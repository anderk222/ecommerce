package com.anderk.ecommerce.order.application.usecase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.anderk.ecommerce.catalog.application.CatalogQueryService;
import com.anderk.ecommerce.catalog.domain.model.CatalogItem;
import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;
import com.anderk.ecommerce.domain.error.ErrorDetail;
import com.anderk.ecommerce.domain.error.InsufficientStockException;
import com.anderk.ecommerce.domain.error.NotFoundException;
import com.anderk.ecommerce.domain.error.ValidationException;
import com.anderk.ecommerce.lead.application.LeadCaptureService;
import com.anderk.ecommerce.lead.domain.model.LeadSource;
import com.anderk.ecommerce.lead.domain.model.Province;
import com.anderk.ecommerce.order.domain.model.Order;
import com.anderk.ecommerce.order.domain.model.OrderItem;
import com.anderk.ecommerce.order.domain.port.OrderRepository;
import com.anderk.ecommerce.shared.annotation.UseCase;
import com.anderk.ecommerce.tracking.application.ActivityTracker;
import com.anderk.ecommerce.tracking.domain.model.EventType;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

/**
 * The use case of the purchase (HU-07).
 *
 * <p>Everything happens in one transaction: the order, the stock, the lead
 * and the event. If the stock of an item is not enough, **nothing** happens:
 * no order, no stock discounted, no lead.
 */
@UseCase
@Transactional
public class CheckoutUseCaseImpl implements CheckoutUseCase {

    private final CatalogRepository catalogRepository;


    private final CatalogQueryService catalogQueryService;

    private final LeadCaptureService leadCaptureService;

    private final OrderRepository orderRepository;

    private final ActivityTracker activityTracker;

    public CheckoutUseCaseImpl(CatalogRepository catalogRepository,
                                CatalogQueryService catalogQueryService,
                                LeadCaptureService leadCaptureService,
                                OrderRepository orderRepository,
                                ActivityTracker activityTracker) {
        this.catalogRepository = catalogRepository;
        this.catalogQueryService = catalogQueryService;
        this.leadCaptureService = leadCaptureService;
        this.orderRepository = orderRepository;
        this.activityTracker = activityTracker;
    }

    @Override
    public OrderConfirmation checkout(CheckoutCommand command) {
        if (command.items() == null || command.items().isEmpty()) {
            throw new ValidationException("El carrito está vacío",
                    ErrorDetail.of("items", "no puede estar vacío"));
        }

        Instant now = Instant.now();
        List<OrderItem> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (RequestedItem requested : command.items()) {
            // The row is locked: a purchase that arrives at the same time
            // waits here, and the stock cannot go below zero.
            CatalogItem item = this.catalogRepository.findByIdForUpdate(requested.itemId())
                    .orElseThrow(() -> new NotFoundException("No existe el ítem " + requested.itemId()));

            if (!item.isPublished()) {
                throw new NotFoundException("El ítem «" + item.name() + "» ya no está disponible");
            }

            // Discount discount = this.discountRepository.findEffectiveByItem(item.id(), now).orElse(null);
            var breakdown = this.catalogQueryService.getPublicBySlug(item.slug().value()).price();

            if (item.tracksStock() && item.stock() < requested.quantity()) {
                throw new InsufficientStockException(
                        "No hay stock suficiente para «" + item.name() + "»",
                        List.of(ErrorDetail.of(
                                "items[" + lines.size() + "].quantity",
                                "máximo " + item.stock())));
            }

            // The stock is discounted through the entity, so the change is
            // written in the same transaction as the order (tech-stack 2.3).
            if (item.tracksStock()) {
                this.catalogRepository.save(item.withStock(item.stock() - requested.quantity()));
            }

            BigDecimal unitPrice = breakdown.finalPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(requested.quantity()));

            lines.add(new OrderItem(null, item.id(), item.name(), requested.quantity(), unitPrice, lineTotal));
            total = total.add(lineTotal);
        }

        var lead = this.leadCaptureService.capture(new LeadCaptureService.CaptureLeadCommand(
                command.customer().name(),
                command.customer().email(),
                command.customer().phone(),
                parseProvince(command.customer().province()),
                command.customer().consent(),
                LeadSource.CHECKOUT,
                command.anonymousId(),
                command.items().stream().map(RequestedItem::itemId).toList(),
                null,
                total));

        Order order = this.orderRepository.save(
            new Order(null,lead.id(),parseUuid(command.anonymousId()),total, lines, now)
        );

        this.activityTracker.track(
            command.anonymousId(), EventType.PURCHASE, "ORDER", order.id(),
                Map.of("total", total.toPlainString())
        );

        return new OrderConfirmation(order.id(),lead.id(),total,"USD",order.items());
    }

    private Province parseProvince(String province) {
        if (province == null || province.isBlank()) {
            return null;
        }
        try {
            return Province.valueOf(province.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private UUID parseUuid(String anonymousId) {
        if (anonymousId == null || anonymousId.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(anonymousId.trim());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
