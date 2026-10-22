package com.anderk.ecommerce.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.anderk.ecommerce.TestcontainersConfiguration;
import com.anderk.ecommerce.catalog.domain.model.CatalogItem;
import com.anderk.ecommerce.catalog.domain.model.ItemType;
import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;
import com.anderk.ecommerce.catalog.domain.vo.Money;
import com.anderk.ecommerce.catalog.domain.vo.Slug;
import com.anderk.ecommerce.domain.error.InsufficientStockException;
import com.anderk.ecommerce.lead.domain.port.LeadRepository;
import com.anderk.ecommerce.order.application.usecase.CheckoutUseCase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * The purchase (HU-07).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CheckoutIntegrationTest {

    @Autowired
    private CheckoutUseCase checkoutService;

    @Autowired
    private CatalogRepository catalogRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Test
    @DisplayName("the order is created with the prices of the moment, the stock goes down and the lead exists")
    void theOrderIsCreatedWithThePricesOfTheMoment() {
        CatalogItem item = this.createItem("Router de prueba", 5, "99.00");
        var discount = this.catalogRepository.findById(item.id()).orElseThrow();

        var confirmation = this.checkoutService.checkout(new CheckoutUseCase.CheckoutCommand(
                "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee",
                new CheckoutUseCase.Customer("Ana Pérez", "ana@mail.com", "0999999999", "Pichincha", true),
                List.of(new CheckoutUseCase.RequestedItem(item.id(), 2))));

        assertThat(confirmation.total()).isEqualByComparingTo("198.00");
        assertThat(confirmation.lines()).hasSize(1);
        assertThat(confirmation.leadId()).isNotNull();

        CatalogItem reloaded = this.catalogRepository.findById(item.id()).orElseThrow();
        assertThat(reloaded.stock()).isEqualTo(3);

        var lead = this.leadRepository.findById(confirmation.leadId()).orElseThrow();
        assertThat(lead.sources()).contains(
                com.anderk.ecommerce.lead.domain.model.LeadSource.CHECKOUT);
        assertThat(discount.slug().value()).isEqualTo("router-de-prueba");
    }

    @Test
    @DisplayName("two concurrent purchases cannot oversell the same item")
    void twoConcurrentPurchasesCannotOversellTheSameItem() throws Exception {
        CatalogItem item = this.createItem("Última unidad", 1, "10.00");

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Object> purchase = () -> {
            start.await();
            try {
                return this.checkoutService.checkout(new CheckoutUseCase.CheckoutCommand(
                        "bbbbbbbb-bbbb-4ccc-8ddd-eeeeeeeeeeee",
                        new CheckoutUseCase.Customer("Comprador", "comprador@mail.com", null,
                                "Pichincha", true),
                        List.of(new CheckoutUseCase.RequestedItem(item.id(), 1))));
            } catch (RuntimeException exception) {
                return exception;
            }
        };

        Future<Object> first = pool.submit(purchase);
        Future<Object> second = pool.submit(purchase);
        start.countDown();

        List<Object> outcomes = List.of(first.get(), second.get());
        pool.shutdown();

        long succeeded = outcomes.stream().filter(outcome -> outcome instanceof CheckoutUseCase.OrderConfirmation).count();
        long refused = outcomes.stream().filter(InsufficientStockException.class::isInstance).count();

        assertThat(succeeded).isEqualTo(1);
        assertThat(refused).isEqualTo(1);

        CatalogItem reloaded = this.catalogRepository.findById(item.id()).orElseThrow();
        assertThat(reloaded.stock()).isZero();
    }

    @Test
    @DisplayName("a purchase of more units than there are is refused")
    void aPurchaseOfMoreUnitsThanThereAreIsRefused() {
        CatalogItem item = this.createItem("Pocas unidades", 2, "10.00");

        assertThatThrownBy(() -> this.checkoutService.checkout(new CheckoutUseCase.CheckoutCommand(
                "cccccccc-bbbb-4ccc-8ddd-eeeeeeeeeeee",
                new CheckoutUseCase.Customer("Comprador", "otro@mail.com", null, "Pichincha", true),
                List.of(new CheckoutUseCase.RequestedItem(item.id(), 5)))))
                .isInstanceOf(InsufficientStockException.class);
    }

    private CatalogItem createItem(String name, int stock, String price) {
        return this.catalogRepository.save(new CatalogItem(
                null, ItemType.PRODUCT, name, Slug.of(name), "Un ítem de prueba.",
                Money.of(new java.math.BigDecimal(price)), stock,
                List.of(), List.of(), List.of(),
                false, true, null, java.time.Instant.now()));
    }
}
