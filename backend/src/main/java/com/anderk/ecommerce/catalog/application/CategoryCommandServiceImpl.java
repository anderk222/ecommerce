package com.anderk.ecommerce.catalog.application;

import com.anderk.ecommerce.catalog.application.view.CategoryView;
import com.anderk.ecommerce.catalog.domain.model.Category;
import com.anderk.ecommerce.catalog.domain.port.CategoryRepository;
import com.anderk.ecommerce.catalog.domain.vo.Slug;
import com.anderk.ecommerce.domain.error.ConflictException;
import com.anderk.ecommerce.domain.error.NotFoundException;
import com.anderk.ecommerce.domain.error.ValidationException;

import org.springframework.transaction.annotation.Transactional;

/**
 * The use cases that write the categories.
 */
@Transactional
public class CategoryCommandServiceImpl implements CategoryCommandService {

    private final CategoryRepository categoryRepository;

    public CategoryCommandServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public CategoryView create(String name) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty()) {
            throw new ValidationException("El nombre de la categoría es obligatorio",
                    com.anderk.ecommerce.domain.error.ErrorDetail.of("name", "no puede estar vacío"));
        }

        Category category = new Category(null, cleanName, uniqueSlug(cleanName));
        Category saved = this.categoryRepository.save(category);
        return new CategoryView(saved.id(), saved.name(), saved.slug().value(), 0);
    }

    @Override
    public CategoryView update(Long id, String name) {
        String cleanName = name == null ? "" : name.trim();
        if (cleanName.isEmpty()) {
            throw new ValidationException("El nombre de la categoría es obligatorio",
                    com.anderk.ecommerce.domain.error.ErrorDetail.of("name", "no puede estar vacío"));
        }

        Category existing = this.categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe la categoría " + id));

        Category renamed = new Category(existing.id(), cleanName, existing.slug());
        Category saved = this.categoryRepository.save(renamed);
        long count = this.categoryRepository.listWithCounts().stream()
                .filter(entry -> entry.category().id().equals(saved.id()))
                .map(CategoryRepository.CategoryWithCount::itemCount)
                .findFirst()
                .orElse(0L);
        return new CategoryView(saved.id(), saved.name(), saved.slug().value(), count);
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
