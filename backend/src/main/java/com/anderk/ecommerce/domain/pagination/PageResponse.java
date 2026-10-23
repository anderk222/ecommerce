package com.anderk.ecommerce.domain.pagination;

import java.util.List;

/**
 * The pagination envelope of every list in the API.
 *
 * <p>The shape is fixed by the use-case document: {@code { content, page,
 * size, totalElements, totalPages }}.
 *
 * @param content the rows of this page
 * @param page the 0-based number of this page
 * @param size the size of a page
 * @param totalElements the total number of rows
 * @param totalPages the total number of pages
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public PageResponse {
        content = content == null ? List.of() : List.copyOf(content);
    }

    /** Converts the domain page the outbound ports return into the API envelope. */
    public static <T> PageResponse<T> of(DomainPage<T> page) {
        return new PageResponse<>(page.content(), page.page(), page.size(),
                page.totalElements(), page.totalPages());
    }

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }

    /** The same page, with every row translated. */
    public <R> PageResponse<R> map(java.util.function.Function<? super T, ? extends R> mapper) {
        List<R> mapped = this.content.stream().<R>map(mapper).toList();
        return new PageResponse<>(mapped, this.page, this.size, this.totalElements, this.totalPages);
    }

    public static <T> PageResponse<T> empty(PageRequestDto request) {
        return new PageResponse<>(List.of(), request.page(), request.size(), 0, 0);
    }
}
