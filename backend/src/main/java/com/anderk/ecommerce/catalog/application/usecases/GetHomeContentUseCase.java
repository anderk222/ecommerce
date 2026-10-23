package com.anderk.ecommerce.catalog.application.usecases;

import com.anderk.ecommerce.catalog.application.view.HomeContent;

/**
 * The use case that reads the content of the home page (HU-01).
 */
public interface GetHomeContentUseCase {

    /** The featured, most bought and on-offer items. */
    HomeContent getHome();
}
