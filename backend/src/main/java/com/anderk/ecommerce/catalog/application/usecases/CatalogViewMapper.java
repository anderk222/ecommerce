package com.anderk.ecommerce.catalog.application.usecases;

import java.time.Instant;
import java.util.List;

import com.anderk.ecommerce.catalog.application.view.CatalogItemAdminDetailView;
import com.anderk.ecommerce.catalog.application.view.CatalogItemAdminView;
import com.anderk.ecommerce.catalog.application.view.CatalogItemDetail;
import com.anderk.ecommerce.catalog.application.view.CatalogItemSummary;
import com.anderk.ecommerce.catalog.application.view.CategoryView;
import com.anderk.ecommerce.catalog.application.view.PriceView;
import com.anderk.ecommerce.catalog.application.view.SpecificationView;
import com.anderk.ecommerce.catalog.domain.model.CatalogItem;
import com.anderk.ecommerce.catalog.domain.model.Category;
import com.anderk.ecommerce.catalog.domain.model.Discount;
import com.anderk.ecommerce.catalog.domain.policy.PricingPolicy;
import com.anderk.ecommerce.catalog.domain.policy.StockPolicy;

/**
 * Translates the aggregate of the catalogue to the views the API answers.
 *
 * <p>It is shared by the read use cases so that the price is calculated in
 * exactly one place, at read time (mission §7.6).
 */
public class CatalogViewMapper {

    private final PricingPolicy pricingPolicy;

    private final StockPolicy stockPolicy;

    public CatalogViewMapper(PricingPolicy pricingPolicy, StockPolicy stockPolicy) {
        this.pricingPolicy = pricingPolicy;
        this.stockPolicy = stockPolicy;
    }

    /** The item as a card of a listing. */
    public CatalogItemSummary toSummary(CatalogItem item, Discount discount) {
        return new CatalogItemSummary(
                item.id(),
                item.slug().value(),
                item.name(),
                item.type(),
                toPrice(item, discount),
                item.images().isEmpty() ? null : item.images().get(0),
                item.featured(),
                item.isPurchasable(),
                item.stock(),
                categoryNames(item));
    }

    /** The item as the listing of the administration shows it (HU-14). */
    public CatalogItemAdminView toAdminView(CatalogItem item, Discount discount) {
        return new CatalogItemAdminView(
                item.id(),
                item.slug().value(),
                item.name(),
                item.type(),
                toPrice(item, discount),
                item.stock(),
                item.featured(),
                item.active(),
                item.deletedAt(),
                item.createdAt(),
                categoryNames(item));
    }

    /** The item as the form of the edition reads it (HU-16). */
    public CatalogItemAdminDetailView toAdminDetail(CatalogItem item) {
        return new CatalogItemAdminDetailView(
                item.id(),
                item.slug().value(),
                item.name(),
                item.type(),
                item.description(),
                item.price().amount(),
                item.stock(),
                item.images(),
                specifications(item),
                item.categories().stream().map(Category::id).toList(),
                item.featured());
    }

    /** The item as its detail page shows it (HU-05). */
    public CatalogItemDetail toDetail(CatalogItem item, Discount discount) {
        return new CatalogItemDetail(
                item.id(),
                item.slug().value(),
                item.name(),
                item.type(),
                item.description(),
                toPrice(item, discount),
                item.images(),
                specifications(item),
                categoryNames(item),
                item.featured(),
                item.isPurchasable(),
                item.stock(),
                this.stockPolicy.stateOf(item.stock(), StockPolicy.DEFAULT_LOW_STOCK_THRESHOLD));
    }

    /** The price of the item with the discount in force, if any. */
    public PriceView toPrice(CatalogItem item, Discount discount) {
        var breakdown = this.pricingPolicy.calculate(item.price().amount(), discount, Instant.now());
        return new PriceView(
                breakdown.basePrice(),
                breakdown.finalPrice(),
                breakdown.discountPercentage(),
                item.price().currency());
    }

    /** A category with the number of published items it holds. */
    public CategoryView toCategoryView(Category category, long itemCount) {
        return new CategoryView(category.id(), category.name(), category.slug().value(), itemCount);
    }

    private static List<String> categoryNames(CatalogItem item) {
        return item.categories().stream().map(Category::name).toList();
    }

    private static List<SpecificationView> specifications(CatalogItem item) {
        return item.specifications().stream()
                .map(specification -> new SpecificationView(specification.name(), specification.value()))
                .toList();
    }
}
