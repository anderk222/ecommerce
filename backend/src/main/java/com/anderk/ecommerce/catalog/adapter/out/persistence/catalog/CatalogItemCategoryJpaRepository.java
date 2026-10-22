package com.anderk.ecommerce.catalog.adapter.out.persistence.catalog;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CatalogItemCategoryJpaRepository
        extends JpaRepository<CatalogItemCategoryJpaEntity, Long> {

    /** The junction rows of an item. */
    List<CatalogItemCategoryJpaEntity> findByItemId(Long itemId);

    /** The junction rows of several items at once — one query for a whole page. */
    @Query("select ic from CatalogItemCategoryJpaEntity ic where ic.item.id in :itemIds")
    List<CatalogItemCategoryJpaEntity> findByItemIds(@Param("itemIds") List<Long> itemIds);

    void deleteByItemId(Long itemId);

    /** Removes every junction row of a category — used when a category is deleted. */
    void deleteByCategoryId(Long categoryId);
}
