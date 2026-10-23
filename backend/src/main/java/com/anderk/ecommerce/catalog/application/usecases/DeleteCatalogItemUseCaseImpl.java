package com.anderk.ecommerce.catalog.application.usecases;

import java.time.Instant;

import com.anderk.ecommerce.catalog.domain.model.CatalogItem;
import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;
import com.anderk.ecommerce.domain.error.NotFoundException;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that logically deletes an item (HU-18).
 */
@Transactional
public class DeleteCatalogItemUseCaseImpl implements DeleteCatalogItemUseCase {

    private final CatalogRepository catalogRepository;

    public DeleteCatalogItemUseCaseImpl(CatalogRepository catalogRepository) {
        this.catalogRepository = catalogRepository;
    }

    @Override
    @CacheEvict(cacheNames = {"catalog-home", "catalog-detail", "catalog-listings"}, allEntries = true)
    public void delete(Long id) {
        CatalogItem item = this.catalogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe el ítem " + id));
        this.catalogRepository.save(item.softDeleted(Instant.now()));
    }
}
