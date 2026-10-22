package com.anderk.ecommerce.catalog.application;

import java.util.List;

import com.anderk.ecommerce.catalog.application.view.CategoryView;
import com.anderk.ecommerce.catalog.application.view.CatalogItemAdminDetailView;
import com.anderk.ecommerce.catalog.application.view.CatalogItemAdminView;
import com.anderk.ecommerce.catalog.application.view.CatalogItemDetail;
import com.anderk.ecommerce.catalog.application.view.CatalogItemSummary;
import com.anderk.ecommerce.catalog.application.view.HomeContent;
import com.anderk.ecommerce.catalog.domain.port.CatalogSearchCriteria;
import com.anderk.ecommerce.shared.pagination.PageRequestDto;
import com.anderk.ecommerce.shared.pagination.PageResponse;

/**
 * The inbound port of the **reads** of the catalog.
 *
 * <p>It is the contract of §12.4. Everything the public site shows comes
 * through it.
 */
public interface CatalogQueryService {

    /**
     * The published items that match the criteria, paginated.
     *
     * <p>Never null. Only the items that are active and not deleted.
     */
    PageResponse<CatalogItemSummary> search(CatalogSearchCriteria criteria, PageRequestDto page);

    /**
     * The items of the administration: the inactive ones and, when the criteria
     * ask for them, the deleted ones (HU-14).
     */
    PageResponse<CatalogItemAdminView> searchAdmin(CatalogSearchCriteria criteria, PageRequestDto page);

    /**
     * The item with that identifier, whatever its state, as the form of the
     * edition reads it (HU-16): the description, the images, the specification
     * table and the category identifiers included.
     *
     * @throws com.anderk.ecommerce.domain.error.NotFoundException if the item does not exist
     */
    CatalogItemAdminDetailView getAdminById(Long id);

    /**
     * The published item with that slug.
     *
     * @throws com.anderk.ecommerce.domain.error.NotFoundException if the slug does not exist, or the
     *         item is inactive or deleted
     */
    CatalogItemDetail getPublicBySlug(String slug);

    /** The content of the home page: featured, most bought and on offer. */
    HomeContent getHome();

    /** The slugs of the published items — the sitemap asks for them. */
    List<String> listActiveSlugs();

    /** The categories, with the number of published items each one holds. */
    List<CategoryView> listCategories();

    /**
     * The category with that slug.
     *
     * @throws com.anderk.ecommerce.domain.error.NotFoundException if the category does not exist
     */
    CategoryView getCategoryBySlug(String slug);
}
