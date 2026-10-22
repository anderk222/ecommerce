import { HttpClient, HttpParams, httpResource, HttpResourceRef } from '@angular/common/http';
import { inject, Service, Signal } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '@core/api/api-base-url';
import {
  CatalogItemDetail,
  Category,
  Page,
  Price,
} from '@shared/models/catalog.models';

/** An item of the catalogue, as the administration sees it. */
export interface CatalogItemAdmin {
  id: number;
  slug: string;
  name: string;
  type: 'PRODUCT' | 'SERVICE';
  price: Price;
  stock: number | null;
  featured: boolean;
  active: boolean;
  deletedAt: string | null;
  createdAt: string;
  categories: string[];
}

/** An item of the catalogue, as the form of the edition reads it back. */
export interface CatalogItemAdminDetail {
  id: number;
  slug: string;
  name: string;
  type: 'PRODUCT' | 'SERVICE';
  description: string;
  basePrice: number;
  stock: number | null;
  images: string[];
  specifications: { name: string; value: string }[];
  categoryIds: number[];
  featured: boolean;
}

/** The body of the creation and of the edition of an item. */
export interface CatalogItemPayload {
  type: 'PRODUCT' | 'SERVICE';
  name: string;
  description: string;
  price: number;
  stock: number | null;
  images: string[];
  specifications: { name: string; value: string }[];
  categoryIds: number[];
  featured: boolean;
}

/** The criteria of the listing of the administration. */
export interface AdminCatalogCriteria {
  type?: string;
  q?: string;
  category?: string;
  sort?: string;
  state?: string;
  page?: number;
  size?: number;
}

/**
 * The API of the administration of the catalogue.
 *
 * <p>The reads answer in signals (P51): they return the `HttpResourceRef`
 * they build, so the URL, the params and the type stay here. The writes keep
 * `HttpClient` and their `Observable` — a write is an order, not a state.
 */
@Service()
export class AdminCatalogApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  /** The unified listing of the administration. */
  list(criteria: Signal<AdminCatalogCriteria>): HttpResourceRef<Page<CatalogItemAdmin> | undefined> {
    return httpResource<Page<CatalogItemAdmin>>(() => ({
      url: `${this.baseUrl}/admin/catalog`,
      params: this.paramsOf(criteria()),
    }));
  }

  /** Reads an item back for its edition (HU-16), whatever its state. The
   *  request waits for an identifier: without one there is nothing to read. */
  get(id: Signal<number | null>): HttpResourceRef<CatalogItemAdminDetail | undefined> {
    return httpResource<CatalogItemAdminDetail>(() => {
      const value = id();
      return value === null ? undefined : `${this.baseUrl}/admin/catalog/${value}`;
    });
  }

  /** Creates an item. */
  create(payload: CatalogItemPayload): Observable<CatalogItemDetail> {
    return this.http.post<CatalogItemDetail>(`${this.baseUrl}/admin/catalog`, payload);
  }

  /** Edits an item. */
  update(id: number, payload: CatalogItemPayload): Observable<CatalogItemDetail> {
    return this.http.put<CatalogItemDetail>(`${this.baseUrl}/admin/catalog/${id}`, payload);
  }

  /** Logically deletes an item. */
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/admin/catalog/${id}`);
  }

  /** Activates or deactivates an item. */
  setActive(id: number, active: boolean): Observable<void> {
    return this.http.patch<void>(`${this.baseUrl}/admin/catalog/${id}/active?active=${active}`, {});
  }

  /** Applies a discount to an item. */
  applyDiscount(id: number, percentage: number, endsAt: string | null): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/admin/catalog/${id}/discount`, {
      percentage,
      startsAt: null,
      endsAt,
    });
  }

  /** Revokes the discount in force of an item. */
  revokeDiscount(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/admin/catalog/${id}/discount`);
  }

  /** The categories, for the form of the item. */
  categories(): HttpResourceRef<Category[] | undefined> {
    return httpResource<Category[]>(() => `${this.baseUrl}/admin/categories`);
  }

  /**
   * The serialization of the criteria of the listing.
   *
   * <p>The same keys and the same omissions the API has always answered: an
   * absent, empty or null criterion never flies.
   */
  private paramsOf(criteria: AdminCatalogCriteria): HttpParams {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(criteria)) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return params;
  }
}
