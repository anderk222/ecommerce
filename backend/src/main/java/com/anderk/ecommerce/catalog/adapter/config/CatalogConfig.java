package com.anderk.ecommerce.catalog.adapter.config;

import com.anderk.ecommerce.catalog.adapter.out.persistence.catalog.CatalogItemCategoryJpaRepository;
import com.anderk.ecommerce.catalog.adapter.out.persistence.catalog.CatalogItemJpaRepository;
import com.anderk.ecommerce.catalog.adapter.out.persistence.catalog.JpaCatalogRepositoryAdapter;
import com.anderk.ecommerce.catalog.adapter.out.persistence.category.CategoryJpaRepository;
import com.anderk.ecommerce.catalog.adapter.out.persistence.category.JpaCategoryRepositoryAdapter;
import com.anderk.ecommerce.catalog.adapter.out.persistence.discount.DiscountJpaRepository;
import com.anderk.ecommerce.catalog.adapter.out.persistence.discount.JpaDiscountRepositoryAdapter;
import com.anderk.ecommerce.catalog.application.usecases.ApplyDiscountUseCase;
import com.anderk.ecommerce.catalog.application.usecases.ApplyDiscountUseCaseImpl;
import com.anderk.ecommerce.catalog.application.usecases.CatalogItemFactory;
import com.anderk.ecommerce.catalog.application.usecases.CatalogViewMapper;
import com.anderk.ecommerce.catalog.application.usecases.CreateCatalogItemUseCase;
import com.anderk.ecommerce.catalog.application.usecases.CreateCatalogItemUseCaseImpl;
import com.anderk.ecommerce.catalog.application.usecases.CreateCategoryUseCase;
import com.anderk.ecommerce.catalog.application.usecases.CreateCategoryUseCaseImpl;
import com.anderk.ecommerce.catalog.application.usecases.DeactivateCatalogItemUseCase;
import com.anderk.ecommerce.catalog.application.usecases.DeactivateCatalogItemUseCaseImpl;
import com.anderk.ecommerce.catalog.application.usecases.DeleteCatalogItemUseCase;
import com.anderk.ecommerce.catalog.application.usecases.DeleteCatalogItemUseCaseImpl;
import com.anderk.ecommerce.catalog.application.usecases.DeleteCategoryUseCase;
import com.anderk.ecommerce.catalog.application.usecases.DeleteCategoryUseCaseImpl;
import com.anderk.ecommerce.catalog.application.usecases.GetAdminCatalogItemUseCase;
import com.anderk.ecommerce.catalog.application.usecases.GetAdminCatalogItemUseCaseImpl;
import com.anderk.ecommerce.catalog.application.usecases.GetCatalogItemBySlugUseCase;
import com.anderk.ecommerce.catalog.application.usecases.GetCatalogItemBySlugUseCaseImpl;
import com.anderk.ecommerce.catalog.application.usecases.GetCategoryBySlugUseCase;
import com.anderk.ecommerce.catalog.application.usecases.GetCategoryBySlugUseCaseImpl;
import com.anderk.ecommerce.catalog.application.usecases.GetHomeContentUseCase;
import com.anderk.ecommerce.catalog.application.usecases.GetHomeContentUseCaseImpl;
import com.anderk.ecommerce.catalog.application.usecases.ListActiveSlugsUseCase;
import com.anderk.ecommerce.catalog.application.usecases.ListActiveSlugsUseCaseImpl;
import com.anderk.ecommerce.catalog.application.usecases.ListCategoriesUseCase;
import com.anderk.ecommerce.catalog.application.usecases.ListCategoriesUseCaseImpl;
import com.anderk.ecommerce.catalog.application.usecases.ReactivateCatalogItemUseCase;
import com.anderk.ecommerce.catalog.application.usecases.ReactivateCatalogItemUseCaseImpl;
import com.anderk.ecommerce.catalog.application.usecases.RevokeDiscountUseCase;
import com.anderk.ecommerce.catalog.application.usecases.RevokeDiscountUseCaseImpl;
import com.anderk.ecommerce.catalog.application.usecases.SearchAdminCatalogUseCase;
import com.anderk.ecommerce.catalog.application.usecases.SearchAdminCatalogUseCaseImpl;
import com.anderk.ecommerce.catalog.application.usecases.SearchCatalogUseCase;
import com.anderk.ecommerce.catalog.application.usecases.SearchCatalogUseCaseImpl;
import com.anderk.ecommerce.catalog.application.usecases.SubscribeToStockUseCase;
import com.anderk.ecommerce.catalog.application.usecases.SubscribeToStockUseCaseImpl;
import com.anderk.ecommerce.catalog.application.usecases.UpdateCatalogItemUseCase;
import com.anderk.ecommerce.catalog.application.usecases.UpdateCatalogItemUseCaseImpl;
import com.anderk.ecommerce.catalog.application.usecases.UpdateCategoryUseCase;
import com.anderk.ecommerce.catalog.application.usecases.UpdateCategoryUseCaseImpl;
import com.anderk.ecommerce.catalog.domain.policy.PercentagePricingPolicy;
import com.anderk.ecommerce.catalog.domain.policy.PricingPolicy;
import com.anderk.ecommerce.catalog.domain.policy.StockPolicy;
import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;
import com.anderk.ecommerce.catalog.domain.port.CategoryRepository;
import com.anderk.ecommerce.catalog.domain.port.DiscountRepository;
import com.anderk.ecommerce.catalog.domain.port.DomainEventPublisher;
import com.anderk.ecommerce.catalog.domain.port.StockSubscriptionRepository;
import com.anderk.ecommerce.lead.application.usecases.CaptureLeadUseCase;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The explicit wiring of the catalog hexagon.
 *
 * <p>Tech-stack §2.2.6 asks for one {@code @Configuration} per feature, so
 * that the graph is readable: the adapters are beans, and the use cases
 * depend on the ports, not on the adapters.
 */
@Configuration
public class CatalogConfig {

    @Bean
    CatalogRepository catalogRepository(CatalogItemJpaRepository items,
                                       CategoryJpaRepository categories,
                                       CatalogItemCategoryJpaRepository itemCategories) {
        return new JpaCatalogRepositoryAdapter(items, categories, itemCategories);
    }

    @Bean
    CategoryRepository categoryRepository(CategoryJpaRepository categories,
                                          CatalogItemCategoryJpaRepository itemCategories) {
        return new JpaCategoryRepositoryAdapter(categories, itemCategories);
    }

    @Bean
    DiscountRepository discountRepository(DiscountJpaRepository discounts) {
        return new JpaDiscountRepositoryAdapter(discounts);
    }

    /** The only pricing rule of the system: a percentage off the base price. */
    @Bean
    PricingPolicy pricingPolicy() {
        return new PercentagePricingPolicy();
    }

    /** The rules about stock: what "out of stock" means, and how much can be sold. */
    @Bean
    StockPolicy stockPolicy() {
        return new StockPolicy();
    }

    @Bean
    CatalogViewMapper catalogViewMapper(PricingPolicy pricingPolicy, StockPolicy stockPolicy) {
        return new CatalogViewMapper(pricingPolicy, stockPolicy);
    }

    @Bean
    CatalogItemFactory catalogItemFactory(CatalogRepository catalogRepository,
                                          CategoryRepository categoryRepository) {
        return new CatalogItemFactory(catalogRepository, categoryRepository);
    }

    // --- the writes of the catalogue ----------------------------------------

    @Bean
    CreateCatalogItemUseCase createCatalogItemUseCase(
            CatalogRepository catalogRepository,
            CatalogItemFactory itemFactory,
            CatalogViewMapper viewMapper) {
        return new CreateCatalogItemUseCaseImpl(catalogRepository, itemFactory, viewMapper);
    }

    @Bean
    UpdateCatalogItemUseCase updateCatalogItemUseCase(
            CatalogRepository catalogRepository,
            CatalogItemFactory itemFactory,
            CatalogViewMapper viewMapper,
            DomainEventPublisher eventPublisher) {
        return new UpdateCatalogItemUseCaseImpl(catalogRepository, itemFactory, viewMapper, eventPublisher);
    }

    @Bean
    DeactivateCatalogItemUseCase deactivateCatalogItemUseCase(CatalogRepository catalogRepository) {
        return new DeactivateCatalogItemUseCaseImpl(catalogRepository);
    }

    @Bean
    ReactivateCatalogItemUseCase reactivateCatalogItemUseCase(CatalogRepository catalogRepository) {
        return new ReactivateCatalogItemUseCaseImpl(catalogRepository);
    }

    @Bean
    DeleteCatalogItemUseCase deleteCatalogItemUseCase(CatalogRepository catalogRepository) {
        return new DeleteCatalogItemUseCaseImpl(catalogRepository);
    }

    @Bean
    ApplyDiscountUseCase applyDiscountUseCase(CatalogRepository catalogRepository,
                                              DiscountRepository discountRepository) {
        return new ApplyDiscountUseCaseImpl(catalogRepository, discountRepository);
    }

    @Bean
    RevokeDiscountUseCase revokeDiscountUseCase(CatalogRepository catalogRepository,
                                                DiscountRepository discountRepository) {
        return new RevokeDiscountUseCaseImpl(catalogRepository, discountRepository);
    }

    // --- the reads of the catalogue -----------------------------------------

    @Bean
    SearchCatalogUseCase searchCatalogUseCase(CatalogRepository catalogRepository,
                                              DiscountRepository discountRepository,
                                              CatalogViewMapper viewMapper) {
        return new SearchCatalogUseCaseImpl(catalogRepository, discountRepository, viewMapper);
    }

    @Bean
    SearchAdminCatalogUseCase searchAdminCatalogUseCase(CatalogRepository catalogRepository,
                                                        DiscountRepository discountRepository,
                                                        CatalogViewMapper viewMapper) {
        return new SearchAdminCatalogUseCaseImpl(catalogRepository, discountRepository, viewMapper);
    }

    @Bean
    GetCatalogItemBySlugUseCase getCatalogItemBySlugUseCase(CatalogRepository catalogRepository,
                                                            DiscountRepository discountRepository,
                                                            CatalogViewMapper viewMapper) {
        return new GetCatalogItemBySlugUseCaseImpl(catalogRepository, discountRepository, viewMapper);
    }

    @Bean
    GetAdminCatalogItemUseCase getAdminCatalogItemUseCase(CatalogRepository catalogRepository,
                                                          CatalogViewMapper viewMapper) {
        return new GetAdminCatalogItemUseCaseImpl(catalogRepository, viewMapper);
    }

    @Bean
    GetHomeContentUseCase getHomeContentUseCase(CatalogRepository catalogRepository,
                                                DiscountRepository discountRepository,
                                                CatalogViewMapper viewMapper) {
        return new GetHomeContentUseCaseImpl(catalogRepository, discountRepository, viewMapper);
    }

    @Bean
    ListActiveSlugsUseCase listActiveSlugsUseCase(CatalogRepository catalogRepository) {
        return new ListActiveSlugsUseCaseImpl(catalogRepository);
    }

    @Bean
    ListCategoriesUseCase listCategoriesUseCase(CategoryRepository categoryRepository,
                                                CatalogViewMapper viewMapper) {
        return new ListCategoriesUseCaseImpl(categoryRepository, viewMapper);
    }

    @Bean
    GetCategoryBySlugUseCase getCategoryBySlugUseCase(CategoryRepository categoryRepository,
                                                      CatalogViewMapper viewMapper) {
        return new GetCategoryBySlugUseCaseImpl(categoryRepository, viewMapper);
    }

    // --- the writes of the categories ---------------------------------------

    @Bean
    CreateCategoryUseCase createCategoryUseCase(CategoryRepository categoryRepository,
                                                CatalogViewMapper viewMapper) {
        return new CreateCategoryUseCaseImpl(categoryRepository, viewMapper);
    }

    @Bean
    UpdateCategoryUseCase updateCategoryUseCase(CategoryRepository categoryRepository,
                                                CatalogViewMapper viewMapper) {
        return new UpdateCategoryUseCaseImpl(categoryRepository, viewMapper);
    }

    @Bean
    DeleteCategoryUseCase deleteCategoryUseCase(CategoryRepository categoryRepository) {
        return new DeleteCategoryUseCaseImpl(categoryRepository);
    }

    // --- the subscriptions of the stock -------------------------------------

    @Bean
    SubscribeToStockUseCase subscribeToStockUseCase(
            StockSubscriptionRepository subscriptionRepository,
            CaptureLeadUseCase leadCaptureService) {
        return new SubscribeToStockUseCaseImpl(subscriptionRepository, leadCaptureService);
    }
}
