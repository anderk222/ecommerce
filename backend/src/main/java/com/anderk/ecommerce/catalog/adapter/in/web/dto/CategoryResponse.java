package com.anderk.ecommerce.catalog.adapter.in.web.dto;

/**
 * A category, as the public API answers it.
 *
 * @param id the identifier
 * @param name the display name
 * @param slug the URL fragment
 * @param itemCount the number of published items it holds
 */
public record CategoryResponse(Long id, String name, String slug, long itemCount) {
}
