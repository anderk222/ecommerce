package com.anderk.ecommerce.domain.error;

/**
 * One field-level problem inside an error response.
 *
 * @param field the offending field, in dotted notation ({@code items[0].quantity})
 * @param issue what is wrong with it
 */
public record ErrorDetail(String field, String issue) {

    public static ErrorDetail of(String field, String issue) {
        return new ErrorDetail(field, issue);
    }
}
