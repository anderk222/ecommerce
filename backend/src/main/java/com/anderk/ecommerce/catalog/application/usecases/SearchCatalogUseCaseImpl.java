package com.anderk.ecommerce.catalog.application.usecases;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.anderk.ecommerce.catalog.application.view.CatalogItemSummary;
import com.anderk.ecommerce.catalog.domain.model.CatalogItem;
import com.anderk.ecommerce.catalog.domain.model.Discount;
import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;
import com.anderk.ecommerce.catalog.domain.port.DiscountRepository;
import com.anderk.ecommerce.catalog.domain.search.CatalogSearchCriteria;
import com.anderk.ecommerce.domain.pagination.DomainPage;
import com.anderk.ecommerce.domain.pagination.PageRequestDto;
import com.anderk.ecommerce.domain.pagination.PageResponse;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that searches the public catalog (HU-02 to HU-04).
 */
@Transactional(readOnly = true)
public class SearchCatalogUseCaseImpl implements SearchCatalogUseCase {

    private final CatalogRepository catalogRepository;

    private final DiscountRepository discountRepository;

    private final CatalogViewMapper viewMapper;

    public SearchCatalogUseCaseImpl(CatalogRepository catalogRepository,
                                    DiscountRepository discountRepository,
                                    CatalogViewMapper viewMapper) {
        this.catalogRepository = catalogRepository;
        this.discountRepository = discountRepository;
        this.viewMapper = viewMapper;
    }

    @Override
    @Cacheable(value = "catalog-listings", key = "#criteria + '|' + #page")
    public PageResponse<CatalogItemSummary> search(CatalogSearchCriteria criteria, PageRequestDto page) {
        DomainPage<CatalogItem> found = this.catalogRepository.search(criteria, page);
        Map<Long, Discount> discounts = this.discountRepository.findEffectiveByItems(
                found.content().stream().map(CatalogItem::id).toList(), Instant.now());
        List<CatalogItemSummary> content = found.content().stream()
                .map(item -> this.viewMapper.toSummary(item, discounts.get(item.id())))
                .toList();
        return PageResponse.of(content, found.page(), found.size(), found.totalElements());
    }
}
