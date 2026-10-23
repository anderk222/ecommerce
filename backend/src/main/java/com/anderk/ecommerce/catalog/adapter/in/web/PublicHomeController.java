package com.anderk.ecommerce.catalog.adapter.in.web;

import com.anderk.ecommerce.catalog.adapter.in.web.dto.HomeResponse;
import com.anderk.ecommerce.catalog.application.usecases.GetHomeContentUseCase;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The content of the home page: the featured items, the most bought ones and
 * the ones with an offer in force (HU-01).
 */
@RestController
@RequestMapping("/api/public/home")
public class PublicHomeController {

    private final GetHomeContentUseCase getHomeContentUseCase;

    public PublicHomeController(GetHomeContentUseCase getHomeContentUseCase) {
        this.getHomeContentUseCase = getHomeContentUseCase;
    }

    @GetMapping
    public HomeResponse home() {
        return CatalogApiMapper.toResponse(this.getHomeContentUseCase.getHome());
    }
}
