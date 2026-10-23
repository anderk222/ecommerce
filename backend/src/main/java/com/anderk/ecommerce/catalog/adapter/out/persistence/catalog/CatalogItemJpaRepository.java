package com.anderk.ecommerce.catalog.adapter.out.persistence.catalog;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import com.anderk.ecommerce.catalog.domain.model.ItemType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * The Spring Data repository of the catalog items.
 *
 * <p>The search is not declared here: {@link JpaSpecificationExecutor} runs the
 * {@code Specification} that {@link CatalogItemSpecifications} builds from the
 * criteria. What stays in this interface are the fixed queries.
 */
public interface CatalogItemJpaRepository
        extends JpaRepository<CatalogItemJpaEntity, Long>, JpaSpecificationExecutor<CatalogItemJpaEntity> {

    /** The item with that slug, whatever its state. */
    Optional<CatalogItemJpaEntity> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /** The slugs of the published items — the sitemap asks for them. */
    @Query("select i.slug from CatalogItemJpaEntity i where i.active = true and i.deletedAt is null order by i.slug")
    List<String> listPublishedSlugs();

    /** The featured, published items, most recent first. */
    @Query("select i from CatalogItemJpaEntity i where i.featured = true and i.active = true "
            + "and i.deletedAt is null order by i.createdAt desc")
    List<CatalogItemJpaEntity> listFeatured(Pageable pageable);

    /** The published items with a discount in force. */
    @Query("select distinct i from CatalogItemJpaEntity i join DiscountJpaEntity d on d.itemId = i.id "
            + "where i.active = true and i.deletedAt is null and d.revokedAt is null "
            + "and (d.startsAt is null or d.startsAt <= :now) and (d.endsAt is null or d.endsAt >= :now) "
            + "order by i.createdAt desc")
    List<CatalogItemJpaEntity> listOnSale(@Param("now") Instant now, Pageable pageable);

    /** The published items with the most confirmed purchases. */
    @Query("select i from CatalogItemJpaEntity i where i.active = true and i.deletedAt is null "
            + "order by i.salesCount desc, i.createdAt desc")
    List<CatalogItemJpaEntity> listBestSelling(Pageable pageable);

    /** The most recent published items of a kind. */
    @Query("select i from CatalogItemJpaEntity i where i.type = :type and i.active = true "
            + "and i.deletedAt is null order by i.createdAt desc, i.id desc")
    List<CatalogItemJpaEntity> listByType(
            @Param("type") ItemType type,
            Pageable pageable);

    /**
     * The item with that identifier, locked for writing.
     *
     * <p>The lock is what makes two concurrent purchases unable to oversell
     * the same item (HU-07). The second transaction waits here until the
     * first one commits.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from CatalogItemJpaEntity i where i.id = :id")
    Optional<CatalogItemJpaEntity> findByIdForUpdate(@Param("id") Long id);
}
