package com.anderk.ecommerce.catalog.application.usecases;

import java.util.List;

import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;

import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that lists the slugs of the published items.
 */
@Transactional(readOnly = true)
public class ListActiveSlugsUseCaseImpl implements ListActiveSlugsUseCase {

    private final CatalogRepository catalogRepository;

    public ListActiveSlugsUseCaseImpl(CatalogRepository catalogRepository) {
        this.catalogRepository = catalogRepository;
    }

    @Override
    public List<String> listSlugs() {
        return this.catalogRepository.listPublishedSlugs();
    }
}
