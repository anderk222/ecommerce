package com.anderk.ecommerce.order.adapter.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.anderk.ecommerce.catalog.application.usecases.GetCatalogItemBySlugUseCase;
import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;
import com.anderk.ecommerce.lead.application.usecases.CaptureLeadUseCase;
import com.anderk.ecommerce.order.application.usecase.CheckoutUseCase;
import com.anderk.ecommerce.order.application.usecase.CheckoutUseCaseImpl;
import com.anderk.ecommerce.order.domain.port.OrderRepository;
import com.anderk.ecommerce.tracking.application.usecase.TrackActivityUseCase;

@Configuration
public class OrderUseCasesConfig {

    @Bean
    CheckoutUseCase checkoutUseCase(
            @Autowired CatalogRepository catalogRepository,
            @Autowired GetCatalogItemBySlugUseCase getCatalogItemBySlugUseCase,
            @Autowired CaptureLeadUseCase leadCaptureService,
            @Autowired OrderRepository orderRepository,
            @Autowired TrackActivityUseCase activityTracker
    ){
        return new CheckoutUseCaseImpl(catalogRepository, getCatalogItemBySlugUseCase, leadCaptureService, orderRepository, activityTracker);
    }
}