package com.anderk.ecommerce.catalog.application.usecases;

import com.anderk.ecommerce.catalog.application.view.CategoryView;
import com.anderk.ecommerce.catalog.domain.model.Category;
import com.anderk.ecommerce.catalog.domain.port.CategoryRepository;
import com.anderk.ecommerce.domain.error.ErrorDetail;
import com.anderk.ecommerce.domain.error.NotFoundException;
import com.anderk.ecommerce.domain.error.ValidationException;

import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that renames a category (HU-20).
 */
@Transactional
public class UpdateCategoryUseCaseImpl implements UpdateCategoryUseCase {

    private final CategoryRepository categoryRepository;

    private final CatalogViewMapper viewMapper;

    public UpdateCategoryUseCaseImpl(CategoryRepository categoryRepository,
                                     CatalogViewMapper viewMapper) {
        this.categoryRepository = categoryRepository;
        this.viewMapper = viewMapper;
    }

    @Override
    public CategoryView update(Long id, String name) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty()) {
            throw new ValidationException("El nombre de la categoría es obligatorio",
                    ErrorDetail.of("name", "no puede estar vacío"));
        }

        Category existing = this.categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe la categoría " + id));

        Category saved = this.categoryRepository.save(
                new Category(existing.id(), cleanName, existing.slug()));
        long count = this.categoryRepository.listWithCounts().stream()
                .filter(entry -> entry.category().id().equals(saved.id()))
                .mapToLong(CategoryRepository.CategoryWithCount::itemCount)
                .findFirst()
                .orElse(0L);
        return this.viewMapper.toCategoryView(saved, count);
    }
}
