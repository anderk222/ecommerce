package com.anderk.ecommerce.catalog.application.usecases;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.anderk.ecommerce.catalog.application.view.CatalogItemSummary;
import com.anderk.ecommerce.catalog.application.view.HomeContent;
import com.anderk.ecommerce.catalog.domain.model.CatalogItem;
import com.anderk.ecommerce.catalog.domain.model.Discount;
import com.anderk.ecommerce.catalog.domain.model.ItemType;
import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;
import com.anderk.ecommerce.catalog.domain.port.DiscountRepository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that reads the content of the home page (HU-01).
 */
@Transactional(readOnly = true)
public class GetHomeContentUseCaseImpl implements GetHomeContentUseCase {

    /** How many items each section of the home page shows. */
    public static final int HOME_SECTION_SIZE = 8;

    private final CatalogRepository catalogRepository;

    private final DiscountRepository discountRepository;

    private final CatalogViewMapper viewMapper;

    public GetHomeContentUseCaseImpl(CatalogRepository catalogRepository,
                                     DiscountRepository discountRepository,
                                     CatalogViewMapper viewMapper) {
        this.catalogRepository = catalogRepository;
        this.discountRepository = discountRepository;
        this.viewMapper = viewMapper;
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

        List<Long> ids = Stream.of(featured, services, products, bestSelling, onSale)
                .flatMap(List::stream)
                .map(CatalogItem::id)
                .distinct()
                .toList();
        Map<Long, Discount> discounts = this.discountRepository.findEffectiveByItems(ids, now);

        return new HomeContent(
                summaries(featured, discounts),
                summaries(services, discounts),
                summaries(products, discounts),
                summaries(bestSelling, discounts),
                summaries(onSale, discounts));
    }

    private List<CatalogItemSummary> summaries(List<CatalogItem> items, Map<Long, Discount> discounts) {
        return items.stream()
                .map(item -> this.viewMapper.toSummary(item, discounts.get(item.id())))
                .toList();
    }
}
