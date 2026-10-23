package com.anderk.ecommerce.order.application.usecase;

import java.math.BigDecimal;
import java.util.List;

import com.anderk.ecommerce.domain.error.ErrorDetail;
import com.anderk.ecommerce.domain.error.ValidationException;
import com.anderk.ecommerce.domain.model.vo.Email;
import com.anderk.ecommerce.order.domain.model.OrderItem;

/**
 * The port of the purchase (HU-07).
 */
public interface CheckoutUseCase {

        /**
         * Completes a purchase.
         *
         * <p>
         * **Transactional, all or nothing**: the order is created, the stock is
         * discounted, the lead is captured and the purchase is recorded. The
         * price is always the one of the server — the client never sends it.
         *
         * @throws com.anderk.ecommerce.domain.error.InsufficientStockException if an
         *                                                                      item
         *                                                                      does not
         *                                                                      have
         *                                                                      enough
         *                                                                      stock
         * @throws com.anderk.ecommerce.domain.error.NotFoundException          if an
         *                                                                      item is
         *                                                                      inactive
         *                                                                      or
         *                                                                      deleted
         */
        OrderConfirmation checkout(CheckoutCommand command);

        /**
         * The purchase the visitor asks for.
         *
         * @param anonymousId the browser of the customer
         * @param customer    the data of the customer
         * @param items       what the customer wants
         */
        record CheckoutCommand(
                        String anonymousId,
                        Customer customer,
                        List<RequestedItem> items) {

                public CheckoutCommand {

                        if (items == null || items.isEmpty()) {
                                throw new ValidationException("El carrito está vacío",
                                                ErrorDetail.of("items", "no puede estar vacío"));
                        }
                }
        }

        /**
         * The data of the customer (HU-07).
         *
         * @param name     the name
         * @param email    the email — the identity of the lead
         * @param phone    the telephone
         * @param province the province of Ecuador
         * @param consent  whether the customer agreed to be contacted
         */
        public record Customer(String name, Email email, String phone, String province, boolean consent) {
        }

        /**
         * One line of the purchase.
         *
         * @param itemId   the item
         * @param quantity how many units
         */
        public record RequestedItem(Long itemId, int quantity) {
        }

        /**
         * The answer of the purchase.
         *
         * @param orderId  the identifier of the order
         * @param leadId   the lead the customer became
         * @param total    the total that was charged
         * @param currency the currency of the total
         * @param lines    the lines of the order, with the prices that were charged
         */
        public record OrderConfirmation(
                        Long orderId,
                        Long leadId,
                        BigDecimal total,
                        String currency,
                        List<OrderItem> lines) {
        }
}