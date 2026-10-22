package com.anderk.ecommerce.catalog.adapter.out.persistence.category;

import java.util.List;
import java.util.Optional;

import com.anderk.ecommerce.catalog.adapter.out.persistence.CatalogItemMapper;
import com.anderk.ecommerce.catalog.adapter.out.persistence.catalog.CatalogItemCategoryJpaRepository;
import com.anderk.ecommerce.catalog.domain.model.Category;
import com.anderk.ecommerce.catalog.domain.port.CategoryRepository;

import org.springframework.transaction.annotation.Transactional;

@Transactional
public class JpaCategoryRepositoryAdapter implements CategoryRepository {

    private final CategoryJpaRepository categories;

    private final CatalogItemCategoryJpaRepository itemCategories;

    public JpaCategoryRepositoryAdapter(CategoryJpaRepository categories,
                                        CatalogItemCategoryJpaRepository itemCategories) {
        this.categories = categories;
        this.itemCategories = itemCategories;
    }

    @Override
    public Category save(Category category) {
        CategoryJpaEntity entity = category.id() == null
                ? CatalogItemMapper.toEntity(category)
                : this.categories.findById(category.id()).orElseGet(() -> CatalogItemMapper.toEntity(category));
        entity.setName(category.name());
        entity.setSlug(category.slug().value());
        return CatalogItemMapper.toDomain(this.categories.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Category> findById(Long id) {
        return this.categories.findById(id).map(CatalogItemMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Category> findBySlug(String slug) {
        return this.categories.findBySlug(slug).map(CatalogItemMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryWithCount> listWithCounts() {
        return this.categories.listWithItemCounts().stream()
                .map(row -> new CategoryWithCount(
                        CatalogItemMapper.toDomain((CategoryJpaEntity) row[0]),
                        row[1] == null ? 0L : (Long) row[1]))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySlug(String slug) {
        return this.categories.existsBySlug(slug);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasItems(Long categoryId) {
        return this.categories.countItems(categoryId) > 0;
    }

    @Override
    public void deleteById(Long id) {
        this.itemCategories.deleteByCategoryId(id);
        this.categories.deleteById(id);
    }
}
