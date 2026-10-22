import {
  Component,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { convertToParamMap, ParamMap } from '@angular/router';

import { CatalogApi } from '@features/catalog/catalog-api';
import { Tracker } from '@core/tracking/tracker.service';
import { SeoService } from '@core/seo/seo.service';
import { ItemCard } from '@shared/components/item-card/item-card';
import { Pagination } from '@shared/components/pagination/pagination';
import { Loading } from '@shared/components/loading/loading';
import { CatalogSearchCriteria } from '@shared/models/catalog.models';

/**
 * The listing of the catalog: the products, the services, or the items of a
 * category.
 *
 * <p>It is the same view with a different filter, exactly as HU-03 and HU-04
 * ask: `/productos` is the catalogue with `type = PRODUCT`, `/servicios` is
 * the same view with `type = SERVICE`, and `/categorias/{slug}` filters by
 * category.
 *
 * <p>Every filter is in the URL: a search can be shared, and the crawler
 * sees the same content a visitor sees (HU-02).
 */
@Component({
  selector: 'app-listing',
  imports: [FormsModule, RouterLink, ItemCard, Pagination, Loading],
  templateUrl: './listing.html',
  styleUrl: './listing.scss',
})
export class Listing {
  private readonly catalogApi = inject(CatalogApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly seo = inject(SeoService);
  private readonly tracker = inject(Tracker);

  /** The kind of item the listing shows, when the route fixes it. */
  readonly fixedType = this.route.snapshot.data['type'] as
    | 'PRODUCT'
    | 'SERVICE'
    | undefined;

  /** Whether the listing is the one of a category. */
  readonly byCategory = this.route.snapshot.data['byCategory'] === true;

  /** The category the listing shows, when the route fixes it. */
  readonly fixedCategory = this.route.snapshot.paramMap.get('slug');

  /** The parameters of the URL, as a signal. */
  readonly params = toSignal(this.route.queryParamMap, {
    initialValue: convertToParamMap({}) as ParamMap,
  });

  /** The criteria in force: the URL decides them, and the read follows. */
  private readonly criteria = computed(() => this.criteriaOf(this.params()));

  /** The page of results. */
  readonly result = this.catalogApi.search(this.criteria);

  /** The text of the search box. */
  readonly query = signal('');

  /** The orderings the listing offers. */
  readonly sorts = [
    { value: 'newest', label: 'Más recientes' },
    { value: 'best_selling', label: 'Más comprados' },
    { value: 'price_asc', label: 'Precio: de menor a mayor' },
    { value: 'price_desc', label: 'Precio: de mayor a menor' },
    { value: 'name_asc', label: 'Nombre: A a Z' },
    { value: 'name_desc', label: 'Nombre: Z a A' },
  ];

  /** The ordering in force, which the selector shows. */
  readonly currentSort = computed(() => this.params().get('sort') ?? 'newest');

  /** The name of the category the listing shows, when it shows one. The slug
   *  never moves while the component lives, so the read is a static one. */
  private readonly categoryInfo =
    this.byCategory && this.fixedCategory
      ? this.catalogApi.category(signal(this.fixedCategory))
      : null;

  /** The name of the category the listing shows, when it shows one. */
  readonly categoryName = computed(() => {
    const info = this.categoryInfo;
    return info && info.hasValue() ? info.value().name : null;
  });

  /** The title of the listing. */
  readonly title = computed(() => {
    if (this.byCategory) {
      return this.categoryName() ?? 'Categoría';
    }
    return this.fixedType === 'SERVICE' ? 'Servicios' : 'Productos';
  });

  /** Whether the listing is empty — a true result of the search, or a page
   *  that ran past the end of the list. */
  readonly empty = computed(() => {
    const result = this.result.hasValue() ? this.result.value() : null;
    return result != null && result.content.length === 0;
  });

  /** A page beyond the end of the results: the state must not pretend the
   *  search found nothing (the false-scarcity defect in reverse — a false
   *  emptiness). */
  readonly outOfRangePage = computed(() => {
    const result = this.result.hasValue() ? this.result.value() : null;
    return (
      this.empty() && result != null && result.totalElements > 0
    );
  });

  constructor() {
    effect(() => {
      const params = this.params();
      this.query.set(params.get('q') ?? '');
    });

    effect(() => {
      if (!this.result.hasValue()) {
        return;
      }
      const description = this.fixedType === 'SERVICE'
        ? 'Servicios de Xtrim: instalación, soporte técnico y desarrollo a medida.'
        : 'Productos de Xtrim: tecnología, hogar, ropa y oficina con envío a todo el Ecuador.';
      this.seo.setPage({
        title: `${this.title()} — Xtrim`,
        description,
        path: this.pathOf(this.params()),
      });
    });
  }

  /** Applies a new set of parameters and goes back to the first page. */
  apply(changes: Record<string, string | number | boolean | null>): void {
    const params = new URLSearchParams();
    const current = this.params();
    for (const key of current.keys) {
      params.set(key, current.get(key) ?? '');
    }
    for (const [key, value] of Object.entries(changes)) {
      if (value === null || value === '' || value === false) {
        params.delete(key);
      } else {
        params.set(key, String(value));
      }
    }
    if (!('page' in changes)) {
      params.delete('page');
    }
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: Object.fromEntries(params.entries()),
    });
  }

  /** Sends the search. */
  search(): void {
    const query = this.query().trim();
    this.apply({ q: query || null, page: 0 });
    // The search is recorded when it is sent, not with every keystroke
    // (HU-09).
    if (query) {
      this.tracker.track('SEARCH', 'CATALOG', undefined, { query });
    }
  }

  /** Changes the ordering. */
  sortBy(value: string): void {
    this.apply({ sort: value, page: 0 });
  }

  /** Filters only the items with a discount in force. */
  toggleOnSale(): void {
    const current = this.params().get('onSale') === 'true';
    this.apply({ onSale: current ? null : 'true', page: 0 });
  }

  /** Goes to a page of the listing. */
  goToPage(page: number): void {
    this.apply({ page });
  }

  private criteriaOf(params: ParamMap): CatalogSearchCriteria {
    const criteria: CatalogSearchCriteria = {};
    if (this.fixedType) {
      criteria.type = this.fixedType;
    }
    if (this.fixedCategory) {
      criteria.category = this.fixedCategory;
    }
    const query = params.get('q');
    if (query) {
      criteria.q = query;
    }
    const sort = params.get('sort');
    if (sort) {
      criteria.sort = sort as CatalogSearchCriteria['sort'];
    }
    if (params.get('onSale') === 'true') {
      criteria.onSale = true;
    }
    const page = Number(params.get('page') ?? '0');
    criteria.page = Number.isFinite(page) && page > 0 ? page : 0;
    criteria.size = 12;
    return criteria;
  }

  private pathOf(params: ParamMap): string {
    if (this.byCategory && this.fixedCategory) {
      return `/categorias/${this.fixedCategory}`;
    }
    if (this.fixedType === 'SERVICE') {
      return '/servicios';
    }
    return '/productos';
  }
}
