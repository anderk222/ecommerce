package com.anderk.ecommerce.catalog.application.usecases;

import com.anderk.ecommerce.catalog.application.view.CategoryView;
import com.anderk.ecommerce.catalog.domain.model.Category;
import com.anderk.ecommerce.catalog.domain.port.CategoryRepository;
import com.anderk.ecommerce.domain.error.NotFoundException;

import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that reads a category by its slug.
 */
@Transactional(readOnly = true)
public class GetCategoryBySlugUseCaseImpl implements GetCategoryBySlugUseCase {

    private final CategoryRepository categoryRepository;

    private final CatalogViewMapper viewMapper;

    public GetCategoryBySlugUseCaseImpl(CategoryRepository categoryRepository,
                                        CatalogViewMapper viewMapper) {
        this.categoryRepository = categoryRepository;
        this.viewMapper = viewMapper;
    }

    @Override
    public CategoryView getBySlug(String slug) {
        Category category = this.categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("No existe la categoría '" + slug + "'"));
        long itemCount = this.categoryRepository.listWithCounts().stream()
                .filter(entry -> entry.category().id().equals(category.id()))
                .mapToLong(CategoryRepository.CategoryWithCount::itemCount)
                .findFirst()
                .orElse(0);
        return this.viewMapper.toCategoryView(category, itemCount);
    }
}
