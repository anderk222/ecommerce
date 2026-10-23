package com.anderk.ecommerce.catalog.adapter.out.persistence.catalog;

import static com.anderk.ecommerce.catalog.adapter.out.persistence.catalog.CatalogItemAttributes.ACTIVE;
import static com.anderk.ecommerce.catalog.adapter.out.persistence.catalog.CatalogItemAttributes.DELETED_AT;
import static com.anderk.ecommerce.catalog.adapter.out.persistence.catalog.CatalogItemAttributes.ID;
import static com.anderk.ecommerce.catalog.adapter.out.persistence.catalog.CatalogItemAttributes.SEARCH_TEXT;
import static com.anderk.ecommerce.catalog.adapter.out.persistence.catalog.CatalogItemAttributes.STOCK;
import static com.anderk.ecommerce.catalog.adapter.out.persistence.catalog.CatalogItemAttributes.TYPE;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.anderk.ecommerce.catalog.adapter.out.persistence.SearchText;
import com.anderk.ecommerce.catalog.adapter.out.persistence.category.CategoryJpaEntity;
import com.anderk.ecommerce.catalog.adapter.out.persistence.discount.DiscountJpaEntity;
import com.anderk.ecommerce.catalog.domain.model.ItemType;
import com.anderk.ecommerce.catalog.domain.search.CatalogSearchCriteria;
import com.anderk.ecommerce.catalog.domain.search.ItemStateFilter;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

import org.springframework.data.jpa.domain.Specification;

/**
 * The filters of the catalog search, one small {@link Specification} each.
 *
 * <p>{@link #from} reads a {@link CatalogSearchCriteria} and joins, with AND,
 * only the filters the criteria actually carry. The query is built when it is
 * run, so there is no JPQL with {@code :param is null or ...} to keep in sync
 * with the criteria.
 *
 * <p>Careful with the name: this is Spring's {@code Specification}, not the
 * domain's {@code Specification} (a name/value row of the specification table
 * of an item).
 */
public final class CatalogItemSpecifications {

    private CatalogItemSpecifications() {
    }

    /**
     * The specification of a whole search.
     *
     * @param now the instant used to decide which discounts are in force
     */
    public static Specification<CatalogItemJpaEntity> from(CatalogSearchCriteria criteria, Instant now) {
        List<Specification<CatalogItemJpaEntity>> filters = new ArrayList<>();

        if (criteria.type() != null) {
            filters.add(ofType(criteria.type()));
        }

        List<String> words = SearchText.words(criteria.text());
        if (!words.isEmpty()) {
            filters.add(containsAllWords(words));
        }

        if (criteria.categorySlug() != null) {
            filters.add(inCategory(criteria.categorySlug()));
        }
        if (criteria.onSaleOnly()) {
            filters.add(withDiscountInForce(now));
        }
        if (criteria.stockAtMost() != null) {
            filters.add(stockAtMost(criteria.stockAtMost()));
        }
        if (criteria.state() != ItemStateFilter.ALL) {
            filters.add(inState(criteria.state()));
        }

        return Specification.allOf(filters);
    }

    // --- the filters --------------------------------------------------------

    /** The items of one kind. */
    static Specification<CatalogItemJpaEntity> ofType(ItemType type) {
        return (root, query, cb) -> cb.equal(root.get(TYPE), type);
    }

    /**
     * The items whose normalised text contains every one of the words.
     *
     * <p>The words come out of {@link SearchText}, which only leaves letters and
     * digits, so none of them can carry a {@code LIKE} wildcard.
     */
    static Specification<CatalogItemJpaEntity> containsAllWords(List<String> words) {
        return (root, query, cb) -> {
            Expression<String> searchText = root.get(SEARCH_TEXT);
            Predicate[] containsWord = words.stream()
                    .map(word -> cb.like(searchText, "%" + word + "%"))
                    .toArray(Predicate[]::new);
            return cb.and(containsWord);
        };
    }

    /** The items linked to the category with that slug. */
    static Specification<CatalogItemJpaEntity> inCategory(String categorySlug) {
        return (root, query, cb) -> {
            Subquery<Long> linked = query.subquery(Long.class);
            Root<CatalogItemCategoryJpaEntity> link = linked.from(CatalogItemCategoryJpaEntity.class);
            Join<CatalogItemCategoryJpaEntity, CategoryJpaEntity> category = link.join("category");

            linked.select(link.get("id")).where(
                    cb.equal(link.get("item").get("id"), root.get(ID)),
                    cb.equal(category.get("slug"), categorySlug));
            return cb.exists(linked);
        };
    }

    /** The items with a discount in force at that instant: not revoked, and inside its dates. */
    static Specification<CatalogItemJpaEntity> withDiscountInForce(Instant now) {
        return (root, query, cb) -> {
            Subquery<Long> inForce = query.subquery(Long.class);
            Root<DiscountJpaEntity> discount = inForce.from(DiscountJpaEntity.class);
            Path<Instant> startsAt = discount.get("startsAt");
            Path<Instant> endsAt = discount.get("endsAt");

            inForce.select(discount.get("id")).where(
                    cb.equal(discount.get("itemId"), root.get(ID)),
                    cb.isNull(discount.get("revokedAt")),
                    cb.or(cb.isNull(startsAt), cb.lessThanOrEqualTo(startsAt, now)),
                    cb.or(cb.isNull(endsAt), cb.greaterThanOrEqualTo(endsAt, now)));
            return cb.exists(inForce);
        };
    }

    /** The items whose stock is tracked and is at or below the threshold. */
    static Specification<CatalogItemJpaEntity> stockAtMost(int threshold) {
        return (root, query, cb) -> {
            Path<Integer> stock = root.get(STOCK);
            return cb.and(cb.isNotNull(stock), cb.lessThanOrEqualTo(stock, threshold));
        };
    }

    /** The items in one lifecycle state. */
    static Specification<CatalogItemJpaEntity> inState(ItemStateFilter state) {
        return (root, query, cb) -> {
            Expression<Boolean> active = root.get(ACTIVE);
            Path<Instant> deletedAt = root.get(DELETED_AT);
            return switch (state) {
                case ACTIVE -> cb.and(cb.isTrue(active), cb.isNull(deletedAt));
                case INACTIVE -> cb.and(cb.isFalse(active), cb.isNull(deletedAt));
                case DELETED -> cb.isNotNull(deletedAt);
                case ALL -> cb.conjunction();
            };
        };
    }
}
