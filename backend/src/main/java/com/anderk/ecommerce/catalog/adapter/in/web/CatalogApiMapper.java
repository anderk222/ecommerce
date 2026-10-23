package com.anderk.ecommerce.catalog.adapter.in.web;

import java.util.List;

import com.anderk.ecommerce.catalog.application.view.CategoryView;
import com.anderk.ecommerce.catalog.adapter.in.web.dto.catalog.CatalogItemDetailResponse;
import com.anderk.ecommerce.catalog.adapter.in.web.dto.catalog.CatalogItemSummaryResponse;
import com.anderk.ecommerce.catalog.adapter.in.web.dto.CategoryResponse;
import com.anderk.ecommerce.catalog.adapter.in.web.dto.HomeResponse;
import com.anderk.ecommerce.catalog.adapter.in.web.dto.PriceResponse;
import com.anderk.ecommerce.catalog.adapter.in.web.dto.SpecificationResponse;
import com.anderk.ecommerce.catalog.application.view.CatalogItemDetail;
import com.anderk.ecommerce.catalog.application.view.CatalogItemSummary;
import com.anderk.ecommerce.catalog.application.view.HomeContent;
import com.anderk.ecommerce.catalog.application.view.PriceView;

/**
 * The translation between the application views and the API responses.
 *
 * <p>A mapper is a final utility, not an interface (tech-stack §2.2.8).
 */
public final class CatalogApiMapper {

    private CatalogApiMapper() {
    }

    public static CatalogItemSummaryResponse toResponse(CatalogItemSummary view) {
        return new CatalogItemSummaryResponse(
                view.id(),
                view.slug(),
                view.name(),
                view.type().name(),
                toResponse(view.price()),
                view.image(),
                view.featured(),
                view.available(),
                view.stock(),
                view.categories());
    }

    public static CatalogItemDetailResponse toResponse(CatalogItemDetail view) {
        return new CatalogItemDetailResponse(
                view.id(),
                view.slug(),
                view.name(),
                view.type().name(),
                view.description(),
                toResponse(view.price()),
                view.images(),
                view.specifications().stream()
                        .map(specification -> new SpecificationResponse(specification.name(), specification.value()))
                        .toList(),
                view.categories(),
                view.featured(),
                view.available(),
                view.stock(),
                view.stockState().name());
    }

    public static HomeResponse toResponse(HomeContent content) {
        return new HomeResponse(
                content.featured().stream().map(CatalogApiMapper::toResponse).toList(),
                content.services().stream().map(CatalogApiMapper::toResponse).toList(),
                content.products().stream().map(CatalogApiMapper::toResponse).toList(),
                content.bestSelling().stream().map(CatalogApiMapper::toResponse).toList(),
                content.onSale().stream().map(CatalogApiMapper::toResponse).toList());
    }

    public static CategoryResponse toResponse(CategoryView view) {
        return new CategoryResponse(view.id(), view.name(), view.slug(), view.itemCount());
    }

    public static List<CategoryResponse> toResponses(List<CategoryView> views) {
        return views.stream().map(CatalogApiMapper::toResponse).toList();
    }

    private static PriceResponse toResponse(PriceView view) {
        return new PriceResponse(view.basePrice(), view.finalPrice(), view.discountPercentage(), view.currency());
    }
}
