package com.anderk.ecommerce.catalog.domain.model;

import java.time.Instant;
import java.util.List;

import com.anderk.ecommerce.catalog.domain.vo.Money;
import com.anderk.ecommerce.catalog.domain.vo.Slug;
import com.anderk.ecommerce.domain.error.ErrorDetail;
import com.anderk.ecommerce.domain.error.ValidationException;
import com.anderk.ecommerce.domain.error.InsufficientStockException;
import com.anderk.ecommerce.domain.error.NotFoundException;

/**
 * An entry of the unified catalog: a product or a service.
 *
 * <p>
 * Mission section 7.1 makes products and services one entity with a type.
 * This record is the domain of the catalog and it knows nothing about JPA,
 * HTTP or Spring — the persistence mapping lives in
 * {@code catalog/infrastructure/persistence} (tech-stack section 2.2.10).
 *
 * <p>
 * Two rules from mission section 7 are baked in here:
 *
 * <ul>
 * <li>{@code stock == null} means *stock is not tracked*. Only
 * {@code stock == 0}
 * means *out of stock*.
 * <li>Deleting is logical: {@code deletedAt} is set and the record survives,
 * so the order history and the audit history remain readable.
 * </ul>
 *
 * @param id             the identifier, null before it is persisted
 * @param type           whether it is a product or a service
 * @param name           the display name
 * @param slug           the URL fragment, unique across the catalog
 * @param description    the long description
 * @param price          the base price, before any discount
 * @param stock          the units available, null when stock is not tracked
 * @param images         the image URLs
 * @param specifications the specification table
 * @param categories     the categories the item belongs to
 * @param featured       whether the administrator marked it as featured
 * @param active         whether it is published in the public catalog
 * @param deletedAt      when it was logically deleted, null while it lives
 * @param createdAt      when it was created
 */
public record CatalogItem(
        Long id,
        ItemType type,
        String name,
        Slug slug,
        String description,
        Money price,
        Integer stock,
        List<String> images,
        List<Specification> specifications,
        List<Category> categories,
        boolean featured,
        boolean active,
        Instant deletedAt,
        Instant createdAt) {

    public CatalogItem {
        if (type == null) {
            throw new ValidationException("El tipo del ítem es obligatorio");
        }
        if (name == null || name.isBlank()) {
            throw new ValidationException("El nombre del ítem es obligatorio",
                    ErrorDetail.of("name", "no puede estar vacío"));
        }
        if (slug == null) {
            throw new ValidationException("El slug del ítem es obligatorio");
        }
        if (price == null) {
            throw new ValidationException("El precio del ítem es obligatorio");
        }
        if (stock != null && stock < 0) {
            throw new ValidationException("El stock no puede ser negativo: " + stock,
                    ErrorDetail.of("stock", "no puede ser negativo"));
        }
        images = images == null ? List.of() : List.copyOf(images);
        specifications = specifications == null ? List.of() : List.copyOf(specifications);
        categories = categories == null ? List.of() : List.copyOf(categories);
    }

    /** Whether the item is published: active and not logically deleted. */
    public boolean isPublished() {
        return this.active && this.deletedAt == null;
    }

    /** Whether the item tracks stock at all. */
    public boolean tracksStock() {
        return this.stock != null;
    }

    /**
     * Whether the item has zero units. {@code null} stock is *not* out of stock.
     */
    public boolean isOutOfStock() {
        return this.stock != null && this.stock == 0;
    }

    /** Whether the item can be added to a cart right now. */
    public boolean isPurchasable() {
        return this.isPublished() && !this.isOutOfStock();
    }

    /** A copy with the item deactivated. Reversible: nothing is lost. */
    public CatalogItem deactivated() {
        return new CatalogItem(this.id, this.type, this.name, this.slug, this.description, this.price,
                this.stock, this.images, this.specifications, this.categories, this.featured, false,
                this.deletedAt, this.createdAt);
    }

    /** A copy with the item reactivated. */
    public CatalogItem reactivated() {
        return new CatalogItem(this.id, this.type, this.name, this.slug, this.description, this.price,
                this.stock, this.images, this.specifications, this.categories, this.featured, true,
                this.deletedAt, this.createdAt);
    }

    /**
     * A copy with the deletion stamp set. The record survives — the delete is
     * logical.
     */
    public CatalogItem softDeleted(Instant when) {
        return new CatalogItem(this.id, this.type, this.name, this.slug, this.description, this.price,
                this.stock, this.images, this.specifications, this.categories, this.featured, this.active,
                when, this.createdAt);
    }

    /**
     * A copy with a new stock level. The change goes through the entity, so it is
     * written in the same transaction.
     */
    public CatalogItem withStock(int newStock) {
        if (newStock < 0) {
            throw new ValidationException("El stock no puede ser negativo: " + newStock);
        }
        return new CatalogItem(this.id, this.type, this.name, this.slug, this.description, this.price,
                newStock, this.images, this.specifications, this.categories, this.featured, this.active,
                this.deletedAt, this.createdAt);
    }

    /** Un ítem no publicado se comporta como si no existiera. */
    public void ensurePublished() {
        if (!this.isPublished()) {
            throw new NotFoundException("El ítem «" + this.name + "» ya no está disponible");
        }
    }

    /** Descuenta unidades. Si no lleva control de stock, no cambia nada. */
    public CatalogItem discountStock(int quantity) {
        if (quantity <= 0) {
            throw new ValidationException("La cantidad debe ser mayor a cero",
                    ErrorDetail.of("quantity", "debe ser mayor a cero"));
        }
        if (!this.tracksStock()) {
            return this;
        }
        if (this.stock < quantity) {
            throw new InsufficientStockException(
                    "No hay stock suficiente para «" + this.name + "»",
                    List.of(ErrorDetail.of("quantity", "máximo " + this.stock)));
        }
        return this.withStock(this.stock - quantity);
    }

}
