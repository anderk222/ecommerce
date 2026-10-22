package com.anderk.ecommerce.catalog.domain.port;

import java.util.List;
import java.util.Optional;

import com.anderk.ecommerce.catalog.domain.model.CatalogItem;
import com.anderk.ecommerce.catalog.domain.model.ItemType;
import com.anderk.ecommerce.shared.pagination.DomainPage;
import com.anderk.ecommerce.shared.pagination.PageRequestDto;

/**
 * The outbound port of the catalog aggregate: its reads *and* its writes.
 *
 * <p>One port per aggregate (tech-stack section 2.2.3). The read/write split
 * lives one level up, in the service contracts, where the callers really do
 * differ.
 */
public interface CatalogRepository {

    /** Inserts or updates an item. */
    CatalogItem save(CatalogItem item);

    /** The item with that identifier, whatever its state. */
    Optional<CatalogItem> findById(Long id);

    /**
     * The item with that identifier, **locked for writing**.
     *
     * <p>The lock is what makes two concurrent purchases unable to oversell
     * the same item (HU-07): the second transaction waits here until the
     * first one commits.
     */
    Optional<CatalogItem> findByIdForUpdate(Long id);

    /** The item with that slug, whatever its state. */
    Optional<CatalogItem> findBySlug(String slug);

    /** The published item with that slug. Empty when it is inactive or deleted. */
    Optional<CatalogItem> findPublishedBySlug(String slug);

    /** The matching items, paginated. */
    DomainPage<CatalogItem> search(CatalogSearchCriteria criteria, PageRequestDto page);

    /** The slugs of the published items — the sitemap asks for them. */
    List<String> listPublishedSlugs();

    /** The featured, published items, most recent first. */
    List<CatalogItem> listFeatured(int limit);

    /** The published items with a discount in force. */
    List<CatalogItem> listOnSale(int limit);

    /** The published items with the most confirmed purchases. */
    List<CatalogItem> listBestSelling(int limit);

    /**
     * The most recent published items of a kind.
     *
     * <p>It is what the home page shows in its two sections: the services
     * first, and the products after.
     */
    List<CatalogItem> listByType(ItemType type, int limit);

    /** Whether an item with that slug already exists. */
    boolean existsBySlug(String slug);
}
