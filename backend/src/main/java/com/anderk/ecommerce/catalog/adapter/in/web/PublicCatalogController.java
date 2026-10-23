package com.anderk.ecommerce.catalog.adapter.in.web;

import java.util.List;

import com.anderk.ecommerce.catalog.adapter.in.web.dto.catalog.CatalogItemDetailResponse;
import com.anderk.ecommerce.catalog.adapter.in.web.dto.catalog.CatalogItemSummaryResponse;
import com.anderk.ecommerce.catalog.adapter.in.web.dto.catalog.PublicCatalogSearchRequest;
import com.anderk.ecommerce.catalog.application.usecases.GetCatalogItemBySlugUseCase;
import com.anderk.ecommerce.catalog.application.usecases.ListActiveSlugsUseCase;
import com.anderk.ecommerce.catalog.application.usecases.SearchCatalogUseCase;
import com.anderk.ecommerce.domain.pagination.PageResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The public catalog: the listing and the detail of an item.
 *
 * <p>The routes and the query parameters are the ones of §12.5. The controller
 * only translates HTTP into a use case call — no business rule lives here.
 */
@RestController
@RequestMapping("/api/public/catalog")
public class PublicCatalogController {

    private final SearchCatalogUseCase searchCatalogUseCase;

    private final ListActiveSlugsUseCase listActiveSlugsUseCase;

    private final GetCatalogItemBySlugUseCase getCatalogItemBySlugUseCase;

    public PublicCatalogController(SearchCatalogUseCase searchCatalogUseCase,
                                   ListActiveSlugsUseCase listActiveSlugsUseCase,
                                   GetCatalogItemBySlugUseCase getCatalogItemBySlugUseCase) {
        this.searchCatalogUseCase = searchCatalogUseCase;
        this.listActiveSlugsUseCase = listActiveSlugsUseCase;
        this.getCatalogItemBySlugUseCase = getCatalogItemBySlugUseCase;
    }

    /** The listing. The parameters are described in {@link PublicCatalogSearchRequest}. */
    @GetMapping
    public PageResponse<CatalogItemSummaryResponse> search(PublicCatalogSearchRequest request) {
        return this.searchCatalogUseCase
                .search(request.toCriteria(), request.toPageRequest())
                .map(CatalogApiMapper::toResponse);
    }

    /** The slugs of the published items — the sitemap asks for them. */
    @GetMapping("/slugs")
    public List<String> slugs() {
        return this.listActiveSlugsUseCase.listSlugs();
    }

    /** The detail of an item, by its slug. */
    @GetMapping("/{slug}")
    public CatalogItemDetailResponse detail(@PathVariable String slug) {
        return CatalogApiMapper.toResponse(this.getCatalogItemBySlugUseCase.getBySlug(slug));
    }
}
