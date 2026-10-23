package com.anderk.ecommerce.catalog.application.usecases;

import java.time.Instant;

import com.anderk.ecommerce.catalog.application.command.ApplyDiscountCommand;
import com.anderk.ecommerce.catalog.domain.model.CatalogItem;
import com.anderk.ecommerce.catalog.domain.model.Discount;
import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;
import com.anderk.ecommerce.catalog.domain.port.DiscountRepository;
import com.anderk.ecommerce.domain.error.NotFoundException;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that applies a discount to an item (HU-19).
 */
@Transactional
public class ApplyDiscountUseCaseImpl implements ApplyDiscountUseCase {

    private final CatalogRepository catalogRepository;

    private final DiscountRepository discountRepository;

    public ApplyDiscountUseCaseImpl(CatalogRepository catalogRepository,
                                    DiscountRepository discountRepository) {
        this.catalogRepository = catalogRepository;
        this.discountRepository = discountRepository;
    }

    @Override
    @CacheEvict(cacheNames = {"catalog-home", "catalog-detail", "catalog-listings"}, allEntries = true)
    public void apply(Long id, ApplyDiscountCommand command) {
        CatalogItem item = this.catalogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe el ítem " + id));

        // One current discount per item (mission 7.7): the new one replaces
        // the previous one, which is revoked the same way.
        this.discountRepository.findEffectiveByItem(item.id(), Instant.now())
                .ifPresent(previous -> this.discountRepository.save(
                        new Discount(previous.id(), previous.itemId(), previous.percentage(),
                                previous.startsAt(), previous.endsAt(), Instant.now())));

        this.discountRepository.save(new Discount(
                null, item.id(), command.percentage(), command.startsAt(), command.endsAt(), null));
    }
}
