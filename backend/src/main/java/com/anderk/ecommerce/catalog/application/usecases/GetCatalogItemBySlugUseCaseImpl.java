package com.anderk.ecommerce.catalog.application.usecases;

import java.time.Instant;

import com.anderk.ecommerce.catalog.application.view.CatalogItemDetail;
import com.anderk.ecommerce.catalog.domain.model.CatalogItem;
import com.anderk.ecommerce.catalog.domain.model.Discount;
import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;
import com.anderk.ecommerce.catalog.domain.port.DiscountRepository;
import com.anderk.ecommerce.domain.error.NotFoundException;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that reads the detail page of an item (HU-05).
 */
@Transactional(readOnly = true)
public class GetCatalogItemBySlugUseCaseImpl implements GetCatalogItemBySlugUseCase {

    private final CatalogRepository catalogRepository;

    private final DiscountRepository discountRepository;

    private final CatalogViewMapper viewMapper;

    public GetCatalogItemBySlugUseCaseImpl(CatalogRepository catalogRepository,
                                           DiscountRepository discountRepository,
                                           CatalogViewMapper viewMapper) {
        this.catalogRepository = catalogRepository;
        this.discountRepository = discountRepository;
        this.viewMapper = viewMapper;
    }

    @Override
    @Cacheable(value = "catalog-detail", key = "#slug")
    public CatalogItemDetail getBySlug(String slug) {
        CatalogItem item = this.catalogRepository.findPublishedBySlug(slug)
                .orElseThrow(() -> new NotFoundException("No existe un ítem publicado con el slug '" + slug + "'"));
        Discount discount = this.discountRepository.findEffectiveByItem(item.id(), Instant.now()).orElse(null);
        return this.viewMapper.toDetail(item, discount);
    }
}
