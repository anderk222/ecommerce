package com.anderk.ecommerce.catalog.application.usecases;

import com.anderk.ecommerce.catalog.domain.model.CatalogItem;
import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;
import com.anderk.ecommerce.domain.error.NotFoundException;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that takes an item out of the public catalog (HU-17).
 */
@Transactional
public class DeactivateCatalogItemUseCaseImpl implements DeactivateCatalogItemUseCase {

    private final CatalogRepository catalogRepository;

    public DeactivateCatalogItemUseCaseImpl(CatalogRepository catalogRepository) {
        this.catalogRepository = catalogRepository;
    }

    @Override
    @CacheEvict(cacheNames = {"catalog-home", "catalog-detail", "catalog-listings"}, allEntries = true)
    public void deactivate(Long id) {
        CatalogItem item = this.catalogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe el ítem " + id));
        this.catalogRepository.save(item.deactivated());
    }
}
