package com.anderk.ecommerce.catalog.application.usecases;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.anderk.ecommerce.catalog.application.command.CreateCatalogItemCommand;
import com.anderk.ecommerce.catalog.application.command.UpdateCatalogItemCommand;
import com.anderk.ecommerce.catalog.domain.model.CatalogItem;
import com.anderk.ecommerce.catalog.domain.model.Category;
import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;
import com.anderk.ecommerce.catalog.domain.port.CategoryRepository;
import com.anderk.ecommerce.catalog.domain.vo.Money;
import com.anderk.ecommerce.catalog.domain.vo.Slug;
import com.anderk.ecommerce.domain.error.ErrorDetail;
import com.anderk.ecommerce.domain.error.NotFoundException;
import com.anderk.ecommerce.domain.error.ValidationException;

/**
 * Builds the aggregate of the catalogue from the commands of the creation and
 * the edition.
 *
 * <p>It keeps the invariant checks, the unique slug and the resolution of the
 * categories in one place, so both write use cases behave the same.
 */
public class CatalogItemFactory {

    private final CatalogRepository catalogRepository;

    private final CategoryRepository categoryRepository;

    public CatalogItemFactory(CatalogRepository catalogRepository,
                              CategoryRepository categoryRepository) {
        this.catalogRepository = catalogRepository;
        this.categoryRepository = categoryRepository;
    }

    /** A new item, with a slug derived from its name. */
    public CatalogItem draft(CreateCatalogItemCommand command) {
        validate(command.price(), command.stock());
        return new CatalogItem(
                null,
                command.type(),
                command.name().trim(),
                uniqueSlug(command.name()),
                command.description(),
                Money.of(command.price()),
                command.stock(),
                command.images(),
                command.specifications(),
                resolveCategories(command.categoryIds()),
                command.featured(),
                true,
                null,
                Instant.now());
    }

    /** The edition of an existing item. The slug and the lifecycle do not change. */
    public CatalogItem redraft(CatalogItem existing, UpdateCatalogItemCommand command) {
        validate(command.price(), command.stock());
        return new CatalogItem(
                existing.id(),
                command.type(),
                command.name().trim(),
                existing.slug(),
                command.description(),
                Money.of(command.price()),
                command.stock(),
                command.images(),
                command.specifications(),
                resolveCategories(command.categoryIds()),
                command.featured(),
                existing.active(),
                existing.deletedAt(),
                existing.createdAt());
    }

    private void validate(BigDecimal price, Integer stock) {
        if (price == null || price.signum() < 0) {
            throw new ValidationException("El precio no puede ser negativo",
                    ErrorDetail.of("price", "no puede ser negativo"));
        }
        if (stock != null && stock < 0) {
            throw new ValidationException("El stock no puede ser negativo",
                    ErrorDetail.of("stock", "no puede ser negativo"));
        }
    }

    /** A slug that is unique, derived from the name of the item. */
    private Slug uniqueSlug(String name) {
        Slug candidate = Slug.of(name);
        String value = candidate.value();
        int suffix = 2;
        while (this.catalogRepository.existsBySlug(value)) {
            value = candidate.value() + "-" + suffix;
            suffix++;
        }
        return new Slug(value);
    }

    private List<Category> resolveCategories(List<Long> categoryIds) {
        return categoryIds.stream()
                .map(categoryId -> this.categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new NotFoundException("No existe la categoría " + categoryId)))
                .toList();
    }
}
