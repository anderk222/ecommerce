package com.anderk.ecommerce.catalog.application.usecases;

import com.anderk.ecommerce.catalog.application.view.CategoryView;
import com.anderk.ecommerce.catalog.domain.model.Category;
import com.anderk.ecommerce.catalog.domain.port.CategoryRepository;
import com.anderk.ecommerce.catalog.domain.vo.Slug;
import com.anderk.ecommerce.domain.error.ErrorDetail;
import com.anderk.ecommerce.domain.error.ValidationException;

import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that creates a category (HU-20).
 */
@Transactional
public class CreateCategoryUseCaseImpl implements CreateCategoryUseCase {

    private final CategoryRepository categoryRepository;

    private final CatalogViewMapper viewMapper;

    public CreateCategoryUseCaseImpl(CategoryRepository categoryRepository,
                                     CatalogViewMapper viewMapper) {
        this.categoryRepository = categoryRepository;
        this.viewMapper = viewMapper;
    }

    @Override
    public CategoryView create(String name) {
        String cleanName = cleanName(name);

        Category saved = this.categoryRepository.save(
                new Category(null, cleanName, uniqueSlug(cleanName)));
        return this.viewMapper.toCategoryView(saved, 0);
    }

    private static String cleanName(String name) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty()) {
            throw new ValidationException("El nombre de la categoría es obligatorio",
                    ErrorDetail.of("name", "no puede estar vacío"));
        }
        return cleanName;
    }

    /** A slug that is unique, derived from the name of the category. */
    private Slug uniqueSlug(String name) {
        Slug candidate = Slug.of(name);
        String value = candidate.value();
        int suffix = 2;
        while (this.categoryRepository.existsBySlug(value)) {
            value = candidate.value() + "-" + suffix;
            suffix++;
        }
        return new Slug(value);
    }
}
