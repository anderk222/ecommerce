package com.anderk.ecommerce.order.application.usecase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.anderk.ecommerce.catalog.application.CatalogQueryService;
import com.anderk.ecommerce.catalog.application.view.PriceView;
import com.anderk.ecommerce.catalog.domain.model.CatalogItem;
import com.anderk.ecommerce.catalog.domain.port.CatalogRepository;
import com.anderk.ecommerce.domain.error.NotFoundException;
import com.anderk.ecommerce.lead.application.usecases.CaptureLeadUseCase;
import com.anderk.ecommerce.lead.application.usecases.CaptureLeadUseCase.CaptureLeadUseCaseCommand;
import com.anderk.ecommerce.lead.domain.model.Lead;
import com.anderk.ecommerce.lead.domain.model.Province;
import com.anderk.ecommerce.lead.domain.model.enums.LeadSource;
import com.anderk.ecommerce.order.domain.model.Order;
import com.anderk.ecommerce.order.domain.model.OrderItem;
import com.anderk.ecommerce.order.domain.port.OrderRepository;
import com.anderk.ecommerce.tracking.application.usecase.TrackActivityUseCase;
import com.anderk.ecommerce.tracking.application.usecase.TrackActivityUseCase.TrackActivityUseCaseCommand;
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
@Transactional
public class CheckoutUseCaseImpl implements CheckoutUseCase {

    private final CatalogRepository catalogRepository;

    private final CatalogQueryService catalogQueryService;

    private final CaptureLeadUseCase leadCaptureService;

    private final OrderRepository orderRepository;

    private final TrackActivityUseCase activityTracker;

    public CheckoutUseCaseImpl(CatalogRepository catalogRepository,
                                CatalogQueryService catalogQueryService,
                                CaptureLeadUseCase leadCaptureService,
                                OrderRepository orderRepository,
                                TrackActivityUseCase activityTracker) {
        this.catalogRepository = catalogRepository;
        this.catalogQueryService = catalogQueryService;
        this.leadCaptureService = leadCaptureService;
        this.orderRepository = orderRepository;
        this.activityTracker = activityTracker;
    }

    @Override
    public OrderConfirmation checkout(CheckoutCommand command) {

        Instant now = Instant.now();
        List<OrderItem> lines = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (RequestedItem requested : command.items()) {

            // The row is locked: a purchase that arrives at the same time
            // waits here, and the stock cannot go below zero.
            CatalogItem item = this.catalogRepository.findByIdForUpdate(requested.itemId())
                    .orElseThrow(() -> new NotFoundException("No existe el ítem " + requested.itemId()));

            item.ensurePublished();

            PriceView breakdown = this.catalogQueryService.getPublicBySlug(item.slug().value()).price();

            // The stock is discounted through the entity, so the change is
            // written in the same transaction as the order (tech-stack 2.3).
            this.catalogRepository.save(item.discountStock(requested.quantity()));

            BigDecimal unitPrice = breakdown.finalPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(requested.quantity()));

            lines.add(new OrderItem(null, item.id(), item.name(), requested.quantity(), unitPrice, lineTotal));
            total = total.add(lineTotal);
        }

        Lead lead = this.leadCaptureService.capture(new CaptureLeadUseCaseCommand(
                command.customer().name(),command.customer().email(),command.customer().phone(),
                Province.string(command.customer().province()),command.customer().consent(), LeadSource.CHECKOUT,
                command.anonymousId(), command.items().stream().map(RequestedItem::itemId).toList(),null,total));

        Order order = this.orderRepository.save(
            new Order(null,lead.id(),parseUuid(command.anonymousId()),total, lines, now)
        );

        this.activityTracker.track(
            new TrackActivityUseCaseCommand(command.anonymousId(), EventType.PURCHASE, "ORDER", order.id(),
                Map.of("total", total.toPlainString()))
        );

        return new OrderConfirmation(order.id(),lead.id(),total,"USD",order.items());
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
