package com.anderk.ecommerce.order.adapter.in.web.dto;

import java.util.List;

import com.anderk.ecommerce.auth.adapter.in.web.validation.ValidEmail;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * The data of the customer.
 *
 * @param name     the name
 * @param email    the email
 * @param phone    the telephone
 * @param province the province of Ecuador
 * @param consent  whether the customer agreed to be contacted
 */
public record CustomerRequest(
        @NotBlank String name,
        @NotBlank @ValidEmail String email,
        String phone,
        String province,
        boolean consent) {

    /**
     * The body of the purchase.
     *
     * @param anonymousId the browser of the customer
     * @param customer    the data of the customer
     * @param items       what the customer wants
     */
    public record CheckoutRequest(
            String anonymousId,
            @Valid @NotNull CustomerRequest customer,
            @NotEmpty List<@Valid ItemRequest> items) {
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