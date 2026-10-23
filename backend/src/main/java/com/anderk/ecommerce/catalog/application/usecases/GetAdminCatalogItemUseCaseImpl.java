package com.anderk.ecommerce.catalog.application.usecases;

import com.anderk.ecommerce.catalog.application.view.CatalogItemAdminDetailView;
import com.anderk.ecommerce.catalog.domain.model.CatalogItem;
import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;
import com.anderk.ecommerce.domain.error.NotFoundException;

import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that reads an item for the form of the edition (HU-16).
 */
@Transactional(readOnly = true)
public class GetAdminCatalogItemUseCaseImpl implements GetAdminCatalogItemUseCase {

    private final CatalogRepository catalogRepository;

    private final CatalogViewMapper viewMapper;

    public GetAdminCatalogItemUseCaseImpl(CatalogRepository catalogRepository,
                                          CatalogViewMapper viewMapper) {
        this.catalogRepository = catalogRepository;
        this.viewMapper = viewMapper;
    }

    @Override
    public CatalogItemAdminDetailView getById(Long id) {
        CatalogItem item = this.catalogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No existe el ítem " + id));
        return this.viewMapper.toAdminDetail(item);
    }
}
