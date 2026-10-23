package com.anderk.ecommerce.catalog.application.usecases;

import com.anderk.ecommerce.catalog.domain.model.Category;
import com.anderk.ecommerce.catalog.domain.port.CategoryRepository;
import com.anderk.ecommerce.domain.error.ConflictException;
import com.anderk.ecommerce.domain.error.NotFoundException;

import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that deletes a category (HU-20).
 */
@Transactional
public class DeleteCategoryUseCaseImpl implements DeleteCategoryUseCase {

    private final CategoryRepository categoryRepository;

    public DeleteCategoryUseCaseImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void delete(Long id) {
        Category existing = this.categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe la categoría " + id));

        if (this.categoryRepository.hasItems(id)) {
            throw new ConflictException(
                    "La categoría «" + existing.name() + "» tiene ítems asignados: quite los ítems antes de eliminarla");
        }

        this.categoryRepository.deleteById(id);
    }
}
