import { Component, computed, input, output } from '@angular/core';

/**
 * The pager of a listing.
 *
 * <p>It shows the pages around the current one, so a catalogue with a
 * thousand pages still shows a pager of a reasonable size.
 */
@Component({
  selector: 'app-pagination',
  templateUrl: './pagination.html',
})
export class Pagination {
  /** The 0-based number of the current page. */
  readonly page = input.required<number>();

  /** The total number of pages. */
  readonly totalPages = input.required<number>();

  /** The page the visitor asked for. */
  readonly pageChange = output<number>();

  /** The pages to show, around the current one. */
  readonly visiblePages = computed(() => {
    const total = this.totalPages();
    const current = this.page();
    if (total <= 7) {
      return Array.from({ length: total }, (_, index) => index);
    }
    const pages = new Set<number>([0, total - 1, current]);
    for (let offset = 1; offset <= 2; offset++) {
      pages.add(current - offset);
      pages.add(current + offset);
    }
    return [...pages].filter((value) => value >= 0 && value < total).sort((a, b) => a - b);
  });

  /** Whether a page is before the current one, so a gap has to be shown. */
  hasGapBefore(page: number): boolean {
    return page > 0 && page - 1 !== this.visiblePages()[this.visiblePages().indexOf(page) - 1];
  }
}
