package com.anderk.ecommerce.catalog.application.view;

/**
 * One row of the specification table, as the API answers it.
 *
 * @param name the label
 * @param value the value
 */
public record SpecificationView(String name, String value) {
}
