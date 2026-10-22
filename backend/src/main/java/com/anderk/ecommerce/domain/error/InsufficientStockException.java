package com.anderk.ecommerce.domain.error;

import java.util.List;

/** The purchase asks for more units than the item has in stock. */
public class InsufficientStockException extends BusinessException {

    public InsufficientStockException(String message, List<ErrorDetail> details) {
        super(ErrorCode.INSUFFICIENT_STOCK, message, details);
    }
}
