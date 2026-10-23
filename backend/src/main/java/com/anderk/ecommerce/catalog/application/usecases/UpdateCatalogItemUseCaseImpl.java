package com.anderk.ecommerce.catalog.application.usecases;

import com.anderk.ecommerce.catalog.application.command.UpdateCatalogItemCommand;
import com.anderk.ecommerce.catalog.application.view.CatalogItemDetail;
import com.anderk.ecommerce.catalog.domain.event.StockReplenishedEvent;
import com.anderk.ecommerce.catalog.domain.model.CatalogItem;
import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;
import com.anderk.ecommerce.catalog.domain.port.DomainEventPublisher;
import com.anderk.ecommerce.domain.error.NotFoundException;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that edits an item of the catalog (HU-16).
 */
@Transactional
public class UpdateCatalogItemUseCaseImpl implements UpdateCatalogItemUseCase {

    private final CatalogRepository catalogRepository;

    private final CatalogItemFactory itemFactory;

    private final CatalogViewMapper viewMapper;

    private final DomainEventPublisher eventPublisher;

    public UpdateCatalogItemUseCaseImpl(CatalogRepository catalogRepository,
                                        CatalogItemFactory itemFactory,
                                        CatalogViewMapper viewMapper,
                                        DomainEventPublisher eventPublisher) {
        this.catalogRepository = catalogRepository;
        this.itemFactory = itemFactory;
        this.viewMapper = viewMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @CacheEvict(cacheNames = {"catalog-home", "catalog-detail", "catalog-listings"}, allEntries = true)
    public CatalogItemDetail update(Long id, UpdateCatalogItemCommand command) {
        CatalogItem existing = this.catalogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe el ítem " + id));

        CatalogItem saved = this.catalogRepository.save(this.itemFactory.redraft(existing, command));

        // The stock went from zero to a number above zero: the pending
        // subscriptions of the item have to be notified (HU-08).
        if (isReplenished(existing.stock(), saved.stock())) {
            this.eventPublisher.publish(new StockReplenishedEvent(
                    saved.id(), existing.stock() == null ? 0 : existing.stock(), saved.stock()));
        }

        return this.viewMapper.toDetail(saved, null);
    }

    /** Whether the stock went from zero to a number above zero. */
    private boolean isReplenished(Integer previousStock, Integer newStock) {
        return newStock != null && newStock > 0 && (previousStock == null || previousStock == 0);
    }
}
