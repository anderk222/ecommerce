package com.anderk.ecommerce.catalog.adapter.config;

import com.anderk.ecommerce.catalog.adapter.out.persistence.catalog.CatalogItemCategoryJpaRepository;
import com.anderk.ecommerce.catalog.adapter.out.persistence.catalog.CatalogItemJpaRepository;
import com.anderk.ecommerce.catalog.adapter.out.persistence.catalog.JpaCatalogRepositoryAdapter;
import com.anderk.ecommerce.catalog.adapter.out.persistence.category.CategoryJpaRepository;
import com.anderk.ecommerce.catalog.adapter.out.persistence.category.JpaCategoryRepositoryAdapter;
import com.anderk.ecommerce.catalog.adapter.out.persistence.discount.DiscountJpaRepository;
import com.anderk.ecommerce.catalog.adapter.out.persistence.discount.JpaDiscountRepositoryAdapter;
import com.anderk.ecommerce.catalog.domain.policy.PercentagePricingPolicy;
import com.anderk.ecommerce.catalog.domain.policy.PricingPolicy;
import com.anderk.ecommerce.catalog.domain.policy.StockPolicy;
import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;
import com.anderk.ecommerce.catalog.domain.port.CategoryRepository;
import com.anderk.ecommerce.catalog.domain.port.DiscountRepository;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The explicit wiring of the catalog hexagon.
 *
 * <p>Tech-stack §2.2.6 asks for one {@code @Configuration} per feature, so
 * that the graph is readable: the adapters are beans, and the use cases
 * depend on the ports, not on the adapters.
 *
 * <p>The use cases are not declared here: they carry the {@code @UseCase}
 * stereotype, which is a {@code @Service}, and Spring finds them by
 * component scanning.
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
}
