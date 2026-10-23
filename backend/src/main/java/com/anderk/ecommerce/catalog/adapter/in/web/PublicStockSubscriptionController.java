package com.anderk.ecommerce.catalog.adapter.in.web;

import java.util.List;

import com.anderk.ecommerce.catalog.application.command.SubscribeToStockCommand;
import com.anderk.ecommerce.catalog.application.usecases.SubscribeToStockUseCase;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The subscription to the notice that an item comes back (HU-08).
 *
 * <p>It creates the lead — with the origin `STOCK_NOTIFY` — and the
 * subscription. The notice is only offered when the item has stock 0: an
 * item whose stock is not tracked never needs one.
 */
@RestController
@RequestMapping("/api/public/stock-subscriptions")
public class PublicStockSubscriptionController {

    private final SubscribeToStockUseCase subscribeToStockUseCase;

    public PublicStockSubscriptionController(SubscribeToStockUseCase subscribeToStockUseCase) {
        this.subscribeToStockUseCase = subscribeToStockUseCase;
    }

    /** Subscribes the visitor to the notice. */
    @PostMapping
    public ResponseEntity<Void> subscribe(@Valid @RequestBody SubscriptionRequest request) {
        this.subscribeToStockUseCase.subscribe(new SubscribeToStockCommand(
                request.email(),
                request.name(),
                request.province(),
                request.consent(),
                request.anonymousId(),
                request.itemId(),
                request.itemIds()));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * The body of the subscription.
     *
     * @param email the email of the visitor
     * @param name the name of the visitor
     * @param province the province of Ecuador
     * @param consent whether the visitor agreed to be contacted
     * @param anonymousId the browser of the visitor
     * @param itemId the item that is out of stock
     * @param itemIds the items, when there is more than one
     */
    public record SubscriptionRequest(
            @NotBlank @Email String email,
            String name,
            String province,
            boolean consent,
            String anonymousId,
            @NotNull Long itemId,
            List<Long> itemIds) {
    }
}
