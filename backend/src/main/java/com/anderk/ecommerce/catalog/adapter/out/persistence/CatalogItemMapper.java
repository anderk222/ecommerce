package com.anderk.ecommerce.catalog.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import com.anderk.ecommerce.catalog.adapter.out.persistence.catalog.CatalogItemJpaEntity;
import com.anderk.ecommerce.catalog.adapter.out.persistence.category.CategoryJpaEntity;
import com.anderk.ecommerce.catalog.domain.model.CatalogItem;
import com.anderk.ecommerce.catalog.domain.model.Category;
import com.anderk.ecommerce.catalog.domain.model.Specification;
import com.anderk.ecommerce.catalog.domain.vo.Money;
import com.anderk.ecommerce.catalog.domain.vo.Slug;

/**
 * The translation between the catalog domain and its persistence.
 *
 * <p>A mapper is a final utility, not an interface: tech-stack §2.2.8 says
 * interfaces are only created where there is real variation or where they
 * help to test.
 */
public final class CatalogItemMapper {

    private CatalogItemMapper() {
    }

    /** The text the public search matches against: lower case, without accents. */
    public static String searchText(String name, String description) {
        String raw = (name == null ? "" : name) + " " + (description == null ? "" : description);
        return java.text.Normalizer.normalize(raw, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    /** The domain item the entity represents. */
    public static CatalogItem toDomain(CatalogItemJpaEntity entity, List<Category> categories) {
        return new CatalogItem(
                entity.getId(),
                entity.getType(),
                entity.getName(),
                entity.getSlug() == null ? null : Slug.of(entity.getSlug()),
                entity.getDescription(),
                entity.getPrice() == null ? Money.zero() : Money.of(entity.getPrice()),
                entity.getStock(),
                entity.getImages(),
                entity.getSpecifications(),
                categories,
                entity.isFeatured(),
                entity.isActive(),
                entity.getDeletedAt(),
                entity.getCreatedAt());
    }

    /** The entity the domain item describes. The identifier is kept by the adapter. */
    public static void apply(CatalogItem item, CatalogItemJpaEntity entity) {
        entity.setType(item.type());
        entity.setName(item.name());
        entity.setSlug(item.slug() == null ? null : item.slug().value());
        entity.setDescription(item.description());
        entity.setPrice(item.price().amount());
        entity.setStock(item.stock());
        entity.setImages(item.images());
        entity.setSpecifications(item.specifications());
        entity.setFeatured(item.featured());
        entity.setActive(item.active());
        entity.setDeletedAt(item.deletedAt());
        entity.setSearchText(searchText(item.name(), item.description()));
    }

    /** A brand new entity out of a domain item. */
    public static CatalogItemJpaEntity toEntity(CatalogItem item) {
        CatalogItemJpaEntity entity = new CatalogItemJpaEntity();
        entity.setId(item.id());
        apply(item, entity);
        if (item.createdAt() != null) {
            entity.setCreatedAt(item.createdAt());
        }
        return entity;
    }

    /** The domain category the entity represents. */
    public static Category toDomain(CategoryJpaEntity entity) {
        return new Category(entity.getId(), entity.getName(), Slug.of(entity.getSlug()));
    }

    /** The entity the domain category describes. */
    public static CategoryJpaEntity toEntity(Category category) {
        CategoryJpaEntity entity = new CategoryJpaEntity();
        entity.setId(category.id());
        entity.setName(category.name());
        entity.setSlug(category.slug().value());
        return entity;
    }

    /** The domain specification list, defensively copied. */
    public static List<Specification> specifications(List<Specification> specifications) {
        return specifications == null ? List.of() : List.copyOf(specifications);
    }

    /** The instant used when the entity has no timestamp yet. */
    public static Instant nowOr(Instant instant) {
        return instant == null ? Instant.now() : instant;
    }
}
