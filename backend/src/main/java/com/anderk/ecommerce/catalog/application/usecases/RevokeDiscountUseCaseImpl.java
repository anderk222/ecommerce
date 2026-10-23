package com.anderk.ecommerce.catalog.application.usecases;

import java.time.Instant;

import com.anderk.ecommerce.catalog.domain.model.CatalogItem;
import com.anderk.ecommerce.catalog.domain.model.Discount;
import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;
import com.anderk.ecommerce.catalog.domain.port.DiscountRepository;
import com.anderk.ecommerce.domain.error.ConflictException;
import com.anderk.ecommerce.domain.error.NotFoundException;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that revokes the discount in force of an item (HU-19).
 */
@Transactional
public class RevokeDiscountUseCaseImpl implements RevokeDiscountUseCase {

    private final CatalogRepository catalogRepository;

    private final DiscountRepository discountRepository;

    public RevokeDiscountUseCaseImpl(CatalogRepository catalogRepository,
                                     DiscountRepository discountRepository) {
        this.catalogRepository = catalogRepository;
        this.discountRepository = discountRepository;
    }

    @Override
    @CacheEvict(cacheNames = {"catalog-home", "catalog-detail", "catalog-listings"}, allEntries = true)
    public void revoke(Long id) {
        CatalogItem item = this.catalogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe el ítem " + id));
        Discount discount = this.discountRepository.findEffectiveByItem(item.id(), Instant.now())
                .orElseThrow(() -> new ConflictException("El ítem " + id + " no tiene ningún descuento vigente"));

        this.discountRepository.save(new Discount(discount.id(), discount.itemId(), discount.percentage(),
                discount.startsAt(), discount.endsAt(), Instant.now()));
    }
}
