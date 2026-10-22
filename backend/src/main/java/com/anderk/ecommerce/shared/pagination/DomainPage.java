package com.anderk.ecommerce.shared.pagination;

import java.util.List;

/**
 * A page of results, as the domain sees it.
 *
 * <p>The outbound ports return this type, not the API envelope: a repository
 * port has no business knowing how the result will be serialised. The
 * application layer converts it into a {@link PageResponse}.
 *
 * <p>It carries no framework import, so the domain can depend on it.
 *
 * @param content the rows of this page
 * @param page the 0-based number of this page
 * @param size the size of a page
 * @param totalElements the total number of rows
 * @param totalPages the total number of pages
 */
public record DomainPage<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public DomainPage {
        content = content == null ? List.of() : List.copyOf(content);
    }

    public static <T> DomainPage<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new DomainPage<>(content, page, size, totalElements, totalPages);
    }

    public static <T> DomainPage<T> empty() {
        return new DomainPage<>(List.of(), 0, 0, 0, 0);
    }

    public <R> DomainPage<R> map(java.util.function.Function<? super T, ? extends R> mapper) {
        return new DomainPage<>(content.stream().map(mapper).map(r -> (R) r).toList(),
                page, size, totalElements, totalPages);
    }
}
