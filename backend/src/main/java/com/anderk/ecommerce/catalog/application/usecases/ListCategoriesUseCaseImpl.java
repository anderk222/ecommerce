package com.anderk.ecommerce.catalog.application.usecases;

import java.util.List;

import com.anderk.ecommerce.catalog.application.view.CategoryView;
import com.anderk.ecommerce.catalog.domain.port.CategoryRepository;

import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that lists the categories with their number of items (HU-20).
 */
@Transactional(readOnly = true)
public class ListCategoriesUseCaseImpl implements ListCategoriesUseCase {

    private final CategoryRepository categoryRepository;

    private final CatalogViewMapper viewMapper;

    public ListCategoriesUseCaseImpl(CategoryRepository categoryRepository,
                                     CatalogViewMapper viewMapper) {
        this.categoryRepository = categoryRepository;
        this.viewMapper = viewMapper;
    }

    @Override
    public List<CategoryView> list() {
        return this.categoryRepository.listWithCounts().stream()
                .map(entry -> this.viewMapper.toCategoryView(entry.category(), entry.itemCount()))
                .toList();
    }
}
