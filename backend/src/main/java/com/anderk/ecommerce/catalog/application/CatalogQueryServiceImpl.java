package com.anderk.ecommerce.catalog.application;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.anderk.ecommerce.catalog.application.view.CategoryView;
import com.anderk.ecommerce.catalog.application.view.CatalogItemAdminDetailView;
import com.anderk.ecommerce.catalog.application.view.CatalogItemAdminView;
import com.anderk.ecommerce.catalog.application.view.CatalogItemDetail;
import com.anderk.ecommerce.catalog.application.view.CatalogItemSummary;
import com.anderk.ecommerce.catalog.application.view.HomeContent;
import com.anderk.ecommerce.catalog.application.view.PriceView;
import com.anderk.ecommerce.catalog.application.view.SpecificationView;
import com.anderk.ecommerce.catalog.domain.model.CatalogItem;
import com.anderk.ecommerce.catalog.domain.model.Category;
import com.anderk.ecommerce.catalog.domain.model.Discount;
import com.anderk.ecommerce.catalog.domain.model.ItemType;
import com.anderk.ecommerce.catalog.domain.policy.PricingPolicy;
import com.anderk.ecommerce.catalog.domain.policy.StockPolicy;
import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;
import com.anderk.ecommerce.catalog.domain.port.CatalogSearchCriteria;
import com.anderk.ecommerce.catalog.domain.port.CategoryRepository;
import com.anderk.ecommerce.catalog.domain.port.DiscountRepository;
import com.anderk.ecommerce.domain.error.NotFoundException;
import com.anderk.ecommerce.domain.pagination.DomainPage;
import com.anderk.ecommerce.domain.pagination.PageRequestDto;
import com.anderk.ecommerce.domain.pagination.PageResponse;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

/**
 * The use cases that read the catalog.
 *
 * <p>It depends only on the outbound ports of the domain and on the pricing
 * policy. It never sees a controller, an HTTP status or a database session.
 */
@Transactional(readOnly = true)
public class CatalogQueryServiceImpl implements CatalogQueryService {

    /** How many items each section of the home page shows. */
    public static final int HOME_SECTION_SIZE = 8;

    private final CatalogRepository catalogRepository;

    private final CategoryRepository categoryRepository;

    private final DiscountRepository discountRepository;

    private final PricingPolicy pricingPolicy;

    private final StockPolicy stockPolicy;

    public CatalogQueryServiceImpl(CatalogRepository catalogRepository,
                                    CategoryRepository categoryRepository,
                                    DiscountRepository discountRepository,
                                    PricingPolicy pricingPolicy,
                                    StockPolicy stockPolicy) {
        this.catalogRepository = catalogRepository;
        this.categoryRepository = categoryRepository;
        this.discountRepository = discountRepository;
        this.pricingPolicy = pricingPolicy;
        this.stockPolicy = stockPolicy;
    }

    @Override
    @Cacheable(value = "catalog-listings", key = "#criteria + '|' + #page")
    public PageResponse<CatalogItemSummary> search(CatalogSearchCriteria criteria, PageRequestDto page) {
        DomainPage<CatalogItem> found = this.catalogRepository.search(criteria, page);
        Map<Long, Discount> discounts = this.discountRepository.findEffectiveByItems(
                found.content().stream().map(CatalogItem::id).toList(), Instant.now());
        List<CatalogItemSummary> content = found.content().stream()
                .map(item -> toSummary(item, discounts.get(item.id())))
                .toList();
        return PageResponse.of(content, found.page(), found.size(), found.totalElements());
    }

    @Override
    public PageResponse<CatalogItemAdminView> searchAdmin(CatalogSearchCriteria criteria, PageRequestDto page) {
        DomainPage<CatalogItem> found = this.catalogRepository.search(criteria, page);
        Map<Long, Discount> discounts = this.discountRepository.findEffectiveByItems(
                found.content().stream().map(CatalogItem::id).toList(), Instant.now());
        List<CatalogItemAdminView> content = found.content().stream()
                .map(item -> toAdminView(item, discounts.get(item.id())))
                .toList();
        return PageResponse.of(content, found.page(), found.size(), found.totalElements());
    }

    @Override
    @Cacheable(value = "catalog-detail", key = "#slug")
    public CatalogItemDetail getPublicBySlug(String slug) {
        CatalogItem item = this.catalogRepository.findPublishedBySlug(slug)
                .orElseThrow(() -> new NotFoundException("No existe un ítem publicado con el slug '" + slug + "'"));
        Discount discount = this.discountRepository.findEffectiveByItem(item.id(), Instant.now()).orElse(null);
        return toDetail(item, discount);
    }

    @Override
    public CatalogItemAdminDetailView getAdminById(Long id) {
        CatalogItem item = this.catalogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe el ítem " + id));
        return toAdminDetail(item);
    }

    @Override
    @Cacheable("catalog-home")
    public HomeContent getHome() {
        Instant now = Instant.now();
        List<CatalogItem> featured = this.catalogRepository.listFeatured(HOME_SECTION_SIZE);
        List<CatalogItem> services = this.catalogRepository.listByType(ItemType.SERVICE, HOME_SECTION_SIZE);
        List<CatalogItem> products = this.catalogRepository.listByType(ItemType.PRODUCT, HOME_SECTION_SIZE);
        List<CatalogItem> bestSelling = this.catalogRepository.listBestSelling(HOME_SECTION_SIZE);
        List<CatalogItem> onSale = this.catalogRepository.listOnSale(HOME_SECTION_SIZE);

        List<Long> ids = java.util.stream.Stream.of(featured, services, products, bestSelling, onSale)
                .flatMap(List::stream)
                .map(CatalogItem::id)
                .distinct()
                .toList();
        Map<Long, Discount> discounts = this.discountRepository.findEffectiveByItems(ids, now);

        return new HomeContent(
                featured.stream().map(item -> toSummary(item, discounts.get(item.id()))).toList(),
                services.stream().map(item -> toSummary(item, discounts.get(item.id()))).toList(),
                products.stream().map(item -> toSummary(item, discounts.get(item.id()))).toList(),
                bestSelling.stream().map(item -> toSummary(item, discounts.get(item.id()))).toList(),
                onSale.stream().map(item -> toSummary(item, discounts.get(item.id()))).toList());
    }

    @Override
    public CategoryView getCategoryBySlug(String slug) {
        Category category = this.categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("No existe la categoría '" + slug + "'"));
        long itemCount = this.categoryRepository.listWithCounts().stream()
                .filter(entry -> entry.category().id().equals(category.id()))
                .mapToLong(CategoryRepository.CategoryWithCount::itemCount)
                .findFirst()
                .orElse(0);
        return new CategoryView(category.id(), category.name(), category.slug().value(), itemCount);
    }

    @Override
    public List<String> listActiveSlugs() {
        return this.catalogRepository.listPublishedSlugs();
    }

    @Override
    public List<CategoryView> listCategories() {
        return this.categoryRepository.listWithCounts().stream()
                .map(entry -> new CategoryView(
                        entry.category().id(),
                        entry.category().name(),
                        entry.category().slug().value(),
                        entry.itemCount()))
                .toList();
    }

    // --- the translation to the views ---------------------------------------

    private CatalogItemSummary toSummary(CatalogItem item, Discount discount) {
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
                item.categories().stream().map(Category::name).toList());
    }

    private CatalogItemAdminView toAdminView(CatalogItem item, Discount discount) {
        var breakdown = this.pricingPolicy.calculate(item.price().amount(), discount, Instant.now());
        return new CatalogItemAdminView(
                item.id(),
                item.slug().value(),
                item.name(),
                item.type(),
                new PriceView(breakdown.basePrice(), breakdown.finalPrice(),
                        breakdown.discountPercentage(), item.price().currency()),
                item.stock(),
                item.featured(),
                item.active(),
                item.deletedAt(),
                item.createdAt(),
                item.categories().stream().map(Category::name).toList());
    }

    /** The translation to the view of the form of the edition (HU-16). */
    private CatalogItemAdminDetailView toAdminDetail(CatalogItem item) {
        return new CatalogItemAdminDetailView(
                item.id(),
                item.slug().value(),
                item.name(),
                item.type(),
                item.description(),
                item.price().amount(),
                item.stock(),
                item.images(),
                item.specifications().stream()
                        .map(specification -> new SpecificationView(
                                specification.name(), specification.value()))
                        .toList(),
                item.categories().stream().map(Category::id).toList(),
                item.featured());
    }

    private CatalogItemDetail toDetail(CatalogItem item, Discount discount) {        return new CatalogItemDetail(
                item.id(),
                item.slug().value(),
                item.name(),
                item.type(),
                item.description(),
                toPrice(item, discount),
                item.images(),
                item.specifications().stream()
                        .map(specification -> new SpecificationView(specification.name(), specification.value()))
                        .toList(),
                item.categories().stream().map(Category::name).toList(),
                item.featured(),
                item.isPurchasable(),
                item.stock(),
                this.stockPolicy.stateOf(item.stock(), StockPolicy.DEFAULT_LOW_STOCK_THRESHOLD));
    }

    private PriceView toPrice(CatalogItem item, Discount discount) {
        var breakdown = this.pricingPolicy.calculate(item.price().amount(), discount, Instant.now());
        return new PriceView(
                breakdown.basePrice(),
                breakdown.finalPrice(),
                breakdown.discountPercentage(),
                item.price().currency());
    }
}
