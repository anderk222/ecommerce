package com.anderk.ecommerce.catalog.adapter.out.persistence.category;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, Long> {

    Optional<CategoryJpaEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /** The categories with the number of published items they hold. */
    @Query("""
            select c, (select count(ic) from CatalogItemCategoryJpaEntity ic
                       join CatalogItemJpaEntity i on i = ic.item
                       where ic.category = c and i.active = true and i.deletedAt is null)
            from CategoryJpaEntity c
            order by c.name
            """)
    List<Object[]> listWithItemCounts();

    /** Whether the category holds at least one item. */
    @Query("select count(ic) from CatalogItemCategoryJpaEntity ic where ic.category.id = :categoryId")
    long countItems(@Param("categoryId") Long categoryId);
}
