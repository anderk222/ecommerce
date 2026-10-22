package com.anderk.ecommerce.catalog.domain.port;

import java.util.List;
import java.util.Optional;

import com.anderk.ecommerce.catalog.domain.model.Category;

/**
 * The outbound port of the category aggregate.
 */
public interface CategoryRepository {

    /** Inserts or updates a category. */
    Category save(Category category);

    Optional<Category> findById(Long id);

    Optional<Category> findBySlug(String slug);

    /** Every category, with the number of published items it holds. */
    List<CategoryWithCount> listWithCounts();

    /** Whether a category with that slug already exists. */
    boolean existsBySlug(String slug);

    /** Whether the category holds at least one item. */
    boolean hasItems(Long categoryId);

    /** Removes the category. Callers check {@link #hasItems(Long)} first. */
    void deleteById(Long id);

    /**
     * A category and the number of published items it holds.
     *
     * @param category the category
     * @param itemCount the number of published items
     */
    record CategoryWithCount(Category category, long itemCount) {
    }
}
