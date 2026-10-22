package com.anderk.ecommerce.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.anderk.ecommerce.TestcontainersConfiguration;
import com.anderk.ecommerce.catalog.application.command.UpdateCatalogItemCommand;
import com.anderk.ecommerce.catalog.domain.model.CatalogItem;
import com.anderk.ecommerce.catalog.domain.model.ItemType;
import com.anderk.ecommerce.catalog.domain.model.StockSubscription;
import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;
import com.anderk.ecommerce.catalog.domain.port.StockSubscriptionRepository;
import com.anderk.ecommerce.catalog.domain.vo.Money;
import com.anderk.ecommerce.catalog.domain.vo.Slug;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/**
 * The subscription to the notice that an item comes back (HU-08).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class StockSubscriptionIntegrationTest {

    @Autowired
    private StockSubscriptionService stockSubscriptionService;

    @Autowired
    private StockSubscriptionRepository subscriptionRepository;

    @Autowired
    private CatalogRepository catalogRepository;

    @Autowired
    private CatalogCommandService catalogCommandService;

    @Test
    @DisplayName("a subscription is created, and it is pending")
    void aSubscriptionIsCreatedAndItIsPending() {
        CatalogItem item = this.createItem("Ítem agotado", 0);

        this.stockSubscriptionService.subscribe(new StockSubscriptionService.SubscribeCommand(
                "ana@mail.com", "Ana", "Pichincha", true, null, item.id(), List.of()));

        List<StockSubscription> pending = this.subscriptionRepository.findPendingByItem(item.id());
        assertThat(pending).hasSize(1);
        assertThat(pending.get(0).pending()).isTrue();
    }

    @Test
    @DisplayName("a replenished item marks its pending subscriptions as notified")
    void aReplenishedItemMarksItsPendingSubscriptionsAsNotified() {
        CatalogItem item = this.createItem("Ítem que vuelve", 0);

        this.stockSubscriptionService.subscribe(new StockSubscriptionService.SubscribeCommand(
                "ana2@mail.com", "Ana", "Pichincha", true, null, item.id(), List.of()));

        assertThat(this.subscriptionRepository.findPendingByItem(item.id())).hasSize(1);

        // The administrator puts the stock back: the event is published and the
        // listener notifies the pending subscriptions.
        this.catalogCommandService.update(item.id(), new UpdateCatalogItemCommand(
                ItemType.PRODUCT,
                "Ítem que vuelve",
                "Un ítem que vuelve.",
                new java.math.BigDecimal("10.00"),
                5,
                List.of(),
                List.of(),
                List.of(),
                false));

        List<StockSubscription> stillPending = this.subscriptionRepository.findPendingByItem(item.id());
        assertThat(stillPending).isEmpty();
    }

    private CatalogItem createItem(String name, int stock) {
        return this.catalogRepository.save(new CatalogItem(
                null, ItemType.PRODUCT, name, Slug.of(name), "Un ítem de prueba.",
                Money.of(new java.math.BigDecimal("10.00")), stock,
                List.of(), List.of(), List.of(),
                false, true, null, java.time.Instant.now()));
    }
}
