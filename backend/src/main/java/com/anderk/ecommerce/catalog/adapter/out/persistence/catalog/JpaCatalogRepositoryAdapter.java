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
import com.anderk.ecommerce.catalog.domain.port.CatalogSearchCriteria;
import com.anderk.ecommerce.catalog.domain.port.CatalogSort;
import com.anderk.ecommerce.catalog.domain.port.ItemStateFilter;
import com.anderk.ecommerce.domain.pagination.DomainPage;
import com.anderk.ecommerce.domain.pagination.PageRequestDto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    private static final int STATE_ALL = 0;
    private static final int STATE_ACTIVE = 1;
    private static final int STATE_INACTIVE = 2;
    private static final int STATE_DELETED = 3;

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
        String search = criteria.hasQuery()
                ? "%" + CatalogItemMapper.searchText(criteria.query(), "") + "%"
                : null;
        Pageable pageable = PageRequest.of(page.page(), page.size(), sortOf(criteria.sort()));
        Page<CatalogItemJpaEntity> found = this.items.search(
                criteria.type(),
                stateOf(criteria.state()),
                search,
                criteria.categorySlug(),
                criteria.onSale(),
                criteria.lowStockOnly(),
                criteria.lowStockThreshold(),
                Instant.now(),
                pageable);
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

    private int stateOf(ItemStateFilter state) {
        if (state == null) {
            return STATE_ALL;
        }
        return switch (state) {
            case ACTIVE -> STATE_ACTIVE;
            case INACTIVE -> STATE_INACTIVE;
            case DELETED -> STATE_DELETED;
            case ALL -> STATE_ALL;
        };
    }

    private Sort sortOf(CatalogSort sort) {
        CatalogSort effective = sort == null ? CatalogSort.NEWEST : sort;
        return switch (effective) {
            case NEWEST -> Sort.by(Sort.Order.desc("createdAt"));
            case OLDEST -> Sort.by(Sort.Order.asc("createdAt"));
            case PRICE_ASC -> Sort.by(Sort.Order.asc("price"), Sort.Order.asc("name"));
            case PRICE_DESC -> Sort.by(Sort.Order.desc("price"), Sort.Order.asc("name"));
            case NAME_ASC -> Sort.by(Sort.Order.asc("name"));
            case NAME_DESC -> Sort.by(Sort.Order.desc("name"));
            case BEST_SELLING -> Sort.by(Sort.Order.desc("salesCount"), Sort.Order.desc("createdAt"));
        };
    }
}
