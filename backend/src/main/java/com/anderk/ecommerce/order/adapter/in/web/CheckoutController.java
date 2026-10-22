package com.anderk.ecommerce.order.adapter.in.web;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
                        request.customer().email(),
                        request.customer().phone(),
                        request.customer().province(),
                        request.customer().consent()),
                request.items().stream()
                        .map(item -> new CheckoutUseCase.RequestedItem(item.itemId(), item.quantity()))
                        .toList());

        return ResponseEntity.status(HttpStatus.CREATED).body(this.checkoutService.checkout(command));
    }

    /**
     * The body of the purchase.
     *
     * @param anonymousId the browser of the customer
     * @param customer the data of the customer
     * @param items what the customer wants
     */
    public record CheckoutRequest(
            String anonymousId,
            @Valid @NotNull CustomerRequest customer,
            @NotEmpty List<@Valid ItemRequest> items) {
    }

    /**
     * The data of the customer.
     *
     * @param name the name
     * @param email the email
     * @param phone the telephone
     * @param province the province of Ecuador
     * @param consent whether the customer agreed to be contacted
     */
    public record CustomerRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            String phone,
            String province,
            boolean consent) {
    }

    /**
     * One line of the purchase.
     *
     * @param itemId the item
     * @param quantity how many units
     */
    public record ItemRequest(@NotNull Long itemId, @Min(1) int quantity) {
    }
}
