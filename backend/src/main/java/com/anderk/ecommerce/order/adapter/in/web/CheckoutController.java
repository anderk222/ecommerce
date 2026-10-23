package com.anderk.ecommerce.order.adapter.in.web;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.anderk.ecommerce.domain.model.vo.Email;
import com.anderk.ecommerce.order.adapter.in.web.dto.CustomerRequest.CheckoutRequest;
import com.anderk.ecommerce.order.application.usecase.CheckoutUseCase;

/**
 * The purchase.
 *
 * <p>The route is open: the visitor never registers.
 */
@RestController
@RequestMapping("/api/public/orders")
public class CheckoutController {

    private final CheckoutUseCase checkoutService;

    public CheckoutController(CheckoutUseCase checkoutService) {
        this.checkoutService = checkoutService;
    }

    /**
     * Completes the purchase.
     *
     * <p>The client only sends the item and the quantity. The price is
     * calculated by the server.
     */
    @PostMapping
    public ResponseEntity<CheckoutUseCase.OrderConfirmation> checkout(
            @Valid @RequestBody CheckoutRequest request) {

        CheckoutUseCase.CheckoutCommand command = new CheckoutUseCase.CheckoutCommand(
                request.anonymousId(),
                new CheckoutUseCase.Customer(
                        request.customer().name(),
                        new Email(request.customer().email()),
                        request.customer().phone(),
                        request.customer().province(),
                        request.customer().consent()),
                request.items().stream()
                        .map(item -> new CheckoutUseCase.RequestedItem(item.itemId(), item.quantity()))
                        .toList());

        return ResponseEntity.status(HttpStatus.CREATED).body(this.checkoutService.checkout(command));
    }
}
