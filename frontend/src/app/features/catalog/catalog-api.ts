import { HttpParams, httpResource, HttpResourceRef } from '@angular/common/http';
import { inject, Service, Signal } from '@angular/core';

import {
  CatalogItemDetail,
  CatalogSearchCriteria,
  CatalogItemSummary,
  Category,
  HomeContent,
  Page,
} from '@shared/models/catalog.models';
import { API_BASE_URL } from '@core/api/api-base-url';

/**
 * The API of the public catalog.
 *
 * <p>Every HTTP call of the application lives in a `*Api` service; the
 * components never touch `HttpClient` (section 12.6 of the use-case
 * document).
 *
 * <p>The reads answer in signals (P51): every GET of this service *returns*
 * the `HttpResourceRef` it builds — the URL, the params and the type live
 * here, and the caller holds the resource instead of a promise of one. The
 * service is GET-only: the writes of the house live in their own services, so
 * `HttpClient` never enters this file.
 */
@Service()
export class CatalogApi {
  private readonly baseUrl = inject(API_BASE_URL);

  /** The content of the home page. */
  home(): HttpResourceRef<HomeContent | undefined> {
    return httpResource<HomeContent>(() => `${this.baseUrl}/public/home`);
  }

  /** The listing of the catalog, for the criteria in force. */
  search(
    criteria: Signal<CatalogSearchCriteria>,
  ): HttpResourceRef<Page<CatalogItemSummary> | undefined> {
    return httpResource<Page<CatalogItemSummary>>(() => ({
      url: `${this.baseUrl}/public/catalog`,
      params: this.paramsOf(criteria()),
    }));
  }

  /** The detail of an item, by its slug. */
  detail(slug: Signal<string>): HttpResourceRef<CatalogItemDetail | undefined> {
    return httpResource<CatalogItemDetail>(() => `${this.baseUrl}/public/catalog/${slug()}`);
  }

  /** The categories, with the number of items of each one. */
  categories(): HttpResourceRef<Category[] | undefined> {
    return httpResource<Category[]>(() => `${this.baseUrl}/public/categories`);
  }

  /** One category, by its slug. */
  category(slug: Signal<string>): HttpResourceRef<Category | undefined> {
    return httpResource<Category>(() => `${this.baseUrl}/public/categories/${slug()}`);
  }

  /**
   * The serialization of the criteria of the listing.
   *
   * <p>The same keys and the same omissions the API has always answered: an
   * absent, empty or false criterion never flies.
   */
  private paramsOf(criteria: CatalogSearchCriteria): HttpParams {
    let params = new HttpParams();
    if (criteria.type) {
      params = params.set('type', criteria.type);
    }
    if (criteria.q) {
      params = params.set('q', criteria.q);
    }
    if (criteria.category) {
      params = params.set('category', criteria.category);
    }
    if (criteria.sort) {
      params = params.set('sort', criteria.sort);
    }
    if (criteria.onSale) {
      params = params.set('onSale', 'true');
    }
    if (criteria.lowStock) {
      params = params.set('lowStock', 'true');
    }
    if (criteria.page !== undefined) {
      params = params.set('page', String(criteria.page));
    }
    if (criteria.size !== undefined) {
      params = params.set('size', String(criteria.size));
    }
    return params;
  }
}
