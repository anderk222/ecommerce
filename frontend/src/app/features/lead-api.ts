import { HttpClient } from '@angular/common/http';
import { inject, Service } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '@core/api/api-base-url';

/**
 * The API of the public forms that capture a lead.
 */
@Service()
export class LeadApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  /**
   * The request of more information (HU-12).
   */
  infoRequest(payload: {
    name: string;
    email: string;
    phone: string;
    province: string;
    consent: boolean;
    anonymousId: string;
    itemIds: number[];
    message: string;
  }): Observable<{ leadId: number; name: string; email: string; status: string }> {
    return this.http.post<{ leadId: number; name: string; email: string; status: string }>(
      `${this.baseUrl}/public/info-requests`,
      payload,
    );
  }

  /**
   * The purchase (HU-07).
   *
   * <p>The client only sends the item and the quantity: the price is
   * calculated by the server.
   */
  checkout(payload: {
    anonymousId: string;
    customer: {
      name: string;
      email: string;
      phone: string;
      province: string;
      consent: boolean;
    };
    items: { itemId: number; quantity: number }[];
  }): Observable<{
    orderId: number;
    leadId: number;
    total: number;
    currency: string;
    lines: { itemName: string; quantity: number; unitPrice: number; lineTotal: number }[];
  }> {
    return this.http.post<{
      orderId: number;
      leadId: number;
      total: number;
      currency: string;
      lines: { itemName: string; quantity: number; unitPrice: number; lineTotal: number }[];
    }>(`${this.baseUrl}/public/orders`, payload);
  }

  /**
   * The subscription of the stock of an item that is out of stock (HU-08).
   */
  subscribeToStock(payload: {
    email: string;
    name: string;
    province: string;
    consent: boolean;
    anonymousId: string;
    itemId: number;
  }): Observable<{ leadId: number }> {
    return this.http.post<{ leadId: number }>(
      `${this.baseUrl}/public/stock-subscriptions`,
      payload,
    );
  }
}
