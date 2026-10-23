package com.anderk.ecommerce.catalog.application.usecases;

import com.anderk.ecommerce.catalog.domain.model.CatalogItem;
import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;
import com.anderk.ecommerce.domain.error.NotFoundException;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that publishes again an item that was deactivated (HU-17).
 */
@Transactional
public class ReactivateCatalogItemUseCaseImpl implements ReactivateCatalogItemUseCase {

    private final CatalogRepository catalogRepository;

    public ReactivateCatalogItemUseCaseImpl(CatalogRepository catalogRepository) {
        this.catalogRepository = catalogRepository;
    }

    @Override
    @CacheEvict(cacheNames = {"catalog-home", "catalog-detail", "catalog-listings"}, allEntries = true)
    public void reactivate(Long id) {
        CatalogItem item = this.catalogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe el ítem " + id));
        this.catalogRepository.save(item.reactivated());
    }
}
