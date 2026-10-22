package com.anderk.ecommerce.catalog.adapter.in.web.dto;

/**
 * One row of the specification table, as the API answers it.
 *
 * @param name the label
 * @param value the value
 */
public record SpecificationResponse(String name, String value) {
}
