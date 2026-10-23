package com.anderk.ecommerce.domain.pagination;

/**
 * The pagination request, as it arrives from the API.
 *
 * <p>{@code page} is 0-based and {@code size} is capped at
 * {@link #MAX_SIZE} — a caller asking for a thousand rows gets fifty, not a
 * thousand-row answer.
 *
 * @param page the 0-based page number
 * @param size the page size, at most {@link #MAX_SIZE}
 */
public record PageRequestDto(int page, int size) {

    public static final int MAX_SIZE = 50;

    public static final int DEFAULT_SIZE = 12;

    public PageRequestDto {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = DEFAULT_SIZE;
        }
        if (size > MAX_SIZE) {
            size = MAX_SIZE;
        }
    }

    public static PageRequestDto of(int page, Integer size) {
        return new PageRequestDto(page, size == null ? DEFAULT_SIZE : size);
    }

    public static PageRequestDto first() {
        return new PageRequestDto(0, DEFAULT_SIZE);
    }

    public int offset() {
        return this.page * this.size;
    }
}
