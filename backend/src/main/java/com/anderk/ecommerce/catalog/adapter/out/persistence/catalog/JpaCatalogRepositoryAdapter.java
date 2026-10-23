package com.anderk.ecommerce.catalog.adapter.out.persistence.catalog;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.anderk.ecommerce.catalog.adapter.out.persistence.CatalogItemMapper;
import com.anderk.ecommerce.catalog.adapter.out.persistence.category.CategoryJpaEntity;
import com.anderk.ecommerce.catalog.adapter.out.persistence.category.CategoryJpaRepository;
import com.anderk.ecommerce.catalog.domain.model.CatalogItem;
import com.anderk.ecommerce.catalog.domain.model.Category;
import com.anderk.ecommerce.catalog.domain.model.ItemType;
import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;
import com.anderk.ecommerce.catalog.domain.search.CatalogSearchCriteria;
import com.anderk.ecommerce.domain.error.ErrorDetail;
import com.anderk.ecommerce.domain.error.NotFoundException;
import com.anderk.ecommerce.domain.error.ValidationException;
import com.anderk.ecommerce.domain.pagination.DomainPage;
import com.anderk.ecommerce.domain.pagination.PageRequestDto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

/**
 * The output adapter of the catalog: JPA on one side, the outbound port on
 * the other.
 *
 * <p>Every mutation goes through the entity, never through a JPQL update:
 * the change of the stock is only seen by the database if it travels through
 * the entity (tech-stack §2.3).
 */
@Transactional
public class JpaCatalogRepositoryAdapter implements CatalogRepository {

    private final CatalogItemJpaRepository items;

    private final CategoryJpaRepository categories;

    private final CatalogItemCategoryJpaRepository itemCategories;

    public JpaCatalogRepositoryAdapter(CatalogItemJpaRepository items,
                                       CategoryJpaRepository categories,
                                       CatalogItemCategoryJpaRepository itemCategories) {
        this.items = items;
        this.categories = categories;
        this.itemCategories = itemCategories;
    }

    @Override
    public CatalogItem save(CatalogItem item) {
        CatalogItemJpaEntity entity = item.id() == null
                ? CatalogItemMapper.toEntity(item)
                : this.items.findById(item.id()).orElseGet(() -> CatalogItemMapper.toEntity(item));
        CatalogItemMapper.apply(item, entity);
        CatalogItemJpaEntity saved = this.items.save(entity);
        this.replaceCategories(saved, item.categories());
        return CatalogItemMapper.toDomain(saved, item.categories());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CatalogItem> findById(Long id) {
        return this.items.findById(id).map(entity -> CatalogItemMapper.toDomain(entity, this.categoriesOf(entity)));
    }

    @Override
    @Transactional
    public Optional<CatalogItem> findByIdForUpdate(Long id) {
        return this.items.findByIdForUpdate(id)
                .map(entity -> CatalogItemMapper.toDomain(entity, this.categoriesOf(entity)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CatalogItem> findBySlug(String slug) {
        return this.items.findBySlug(slug)
                .map(entity -> CatalogItemMapper.toDomain(entity, this.categoriesOf(entity)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CatalogItem> findPublishedBySlug(String slug) {
        return this.items.findBySlug(slug)
                .filter(entity -> entity.isActive() && entity.getDeletedAt() == null)
                .map(entity -> CatalogItemMapper.toDomain(entity, this.categoriesOf(entity)));
    }

    @Override
    @Transactional(readOnly = true)
    public DomainPage<CatalogItem> search(CatalogSearchCriteria criteria, PageRequestDto page) {
        Pageable pageable = PageRequest.of(page.page(), page.size(), CatalogItemSorting.of(criteria.sort()));
        Page<CatalogItemJpaEntity> found = this.items.findAll(
                CatalogItemSpecifications.from(criteria, Instant.now()), pageable);
        Map<Long, List<Category>> byItem = this.categoriesOf(found.getContent());
        List<CatalogItem> content = found.getContent().stream()
                .map(entity -> CatalogItemMapper.toDomain(entity, byItem.getOrDefault(entity.getId(), List.of())))
                .toList();
        return DomainPage.of(content, found.getNumber(), found.getSize(), found.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listPublishedSlugs() {
        return this.items.listPublishedSlugs();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogItem> listFeatured(int limit) {
        return this.items.listFeatured(PageRequest.of(0, limit)).stream()
                .map(entity -> CatalogItemMapper.toDomain(entity, this.categoriesOf(entity)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogItem> listOnSale(int limit) {
        return this.items.listOnSale(Instant.now(), PageRequest.of(0, limit)).stream()
                .map(entity -> CatalogItemMapper.toDomain(entity, this.categoriesOf(entity)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogItem> listBestSelling(int limit) {
        return this.items.listBestSelling(PageRequest.of(0, limit)).stream()
                .map(entity -> CatalogItemMapper.toDomain(entity, this.categoriesOf(entity)))
                .toList();
    }

    @Override
    public void registerSale(Long itemId, int units) {
        if (units <= 0) {
            throw new ValidationException("Las unidades vendidas deben ser mayores a cero",
                    ErrorDetail.of("units", "debe ser mayor a cero"));
        }
        CatalogItemJpaEntity entity = this.items.findById(itemId)
                .orElseThrow(() -> new NotFoundException("No existe el ítem " + itemId));
        // The caller has already locked the row. The change travels through the
        // entity, so it is flushed in the same transaction as the sale.
        entity.setSalesCount(entity.getSalesCount() + units);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogItem> listByType(ItemType type, int limit) {
        return this.items.listByType(type, PageRequest.of(0, limit)).stream()
                .map(entity -> CatalogItemMapper.toDomain(entity, this.categoriesOf(entity)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySlug(String slug) {
        return this.items.existsBySlug(slug);
    }

    // --- helpers ------------------------------------------------------------

    private void replaceCategories(CatalogItemJpaEntity entity, List<Category> wanted) {
        List<CategoryJpaEntity> resolved = new ArrayList<>();
        for (Category category : wanted) {
            CategoryJpaEntity categoryEntity = this.categories.findById(category.id())
                    .orElseGet(() -> this.categories.save(CatalogItemMapper.toEntity(category)));
            resolved.add(categoryEntity);
        }
        List<CatalogItemCategoryJpaEntity> current = this.itemCategories.findByItemId(entity.getId());
        List<Long> wantedIds = resolved.stream().map(CategoryJpaEntity::getId).toList();
        for (CatalogItemCategoryJpaEntity row : current) {
            if (!wantedIds.contains(row.getCategory().getId())) {
                this.itemCategories.delete(row);
            }
        }
        for (CategoryJpaEntity categoryEntity : resolved) {
            boolean alreadyLinked = current.stream()
                    .anyMatch(row -> row.getCategory().getId().equals(categoryEntity.getId()));
            if (!alreadyLinked) {
                CatalogItemCategoryJpaEntity row = new CatalogItemCategoryJpaEntity();
                row.setItem(entity);
                row.setCategory(categoryEntity);
                this.itemCategories.save(row);
            }
        }
    }

    private List<Category> categoriesOf(CatalogItemJpaEntity entity) {
        return this.itemCategories.findByItemId(entity.getId()).stream()
                .map(row -> CatalogItemMapper.toDomain(row.getCategory()))
                .toList();
    }

    private Map<Long, List<Category>> categoriesOf(List<CatalogItemJpaEntity> entities) {
        Map<Long, List<Category>> byItem = new LinkedHashMap<>();
        if (entities.isEmpty()) {
            return byItem;
        }
        List<Long> ids = entities.stream().map(CatalogItemJpaEntity::getId).toList();
        for (CatalogItemCategoryJpaEntity row : this.itemCategories.findByItemIds(ids)) {
            byItem.computeIfAbsent(row.getItem().getId(), key -> new ArrayList<>())
                    .add(CatalogItemMapper.toDomain(row.getCategory()));
        }
        return byItem;
    }
}
