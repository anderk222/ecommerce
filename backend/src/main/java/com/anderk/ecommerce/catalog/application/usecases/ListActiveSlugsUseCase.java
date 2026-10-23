package com.anderk.ecommerce.catalog.application.usecases;

import java.util.List;

/**
 * The use case that lists the slugs of the published items.
 *
 * <p>The sitemap asks for them.
 */
public interface ListActiveSlugsUseCase {

    /** The slugs of the published items. */
    List<String> listSlugs();
}
