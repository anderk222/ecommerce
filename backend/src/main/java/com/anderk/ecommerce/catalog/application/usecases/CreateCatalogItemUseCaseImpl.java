package com.anderk.ecommerce.catalog.application.usecases;

import com.anderk.ecommerce.catalog.application.command.CreateCatalogItemCommand;
import com.anderk.ecommerce.catalog.application.view.CatalogItemDetail;
import com.anderk.ecommerce.catalog.domain.model.CatalogItem;
import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that creates an item of the catalog (HU-15).
 */
@Transactional
public class CreateCatalogItemUseCaseImpl implements CreateCatalogItemUseCase {

    private final CatalogRepository catalogRepository;

    private final CatalogItemFactory itemFactory;

    private final CatalogViewMapper viewMapper;

    public CreateCatalogItemUseCaseImpl(CatalogRepository catalogRepository,
                                        CatalogItemFactory itemFactory,
                                        CatalogViewMapper viewMapper) {
        this.catalogRepository = catalogRepository;
        this.itemFactory = itemFactory;
        this.viewMapper = viewMapper;
    }

    @Override
    @CacheEvict(cacheNames = {"catalog-home", "catalog-detail", "catalog-listings"}, allEntries = true)
    public CatalogItemDetail create(CreateCatalogItemCommand command) {
        CatalogItem saved = this.catalogRepository.save(this.itemFactory.draft(command));
        return this.viewMapper.toDetail(saved, null);
    }
}
