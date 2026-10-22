import { HttpClient, HttpParams, httpResource, HttpResourceRef } from '@angular/common/http';
import { inject, Service, Signal } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '@core/api/api-base-url';
import { Page } from '@shared/models/catalog.models';

/** A lead, as the CRM sees it. */
export interface Lead {
  id: number;
  name: string;
  email: string;
  phone: string | null;
  province: string | null;
  status: 'NEW' | 'IN_PROGRESS' | 'CLOSED';
  sources: string[];
  saleAmount: number;
  outcome: 'WON' | 'LOST' | null;
  consent: boolean;
  createdAt: string;
  updatedAt: string;
}

/** One entry of the timeline of a lead. */
export interface TimelineEntry {
  type: string;
  entityType: string | null;
  entityId: number | null;
  /** The product the action is about, when the read could name it (P35). */
  productName?: string | null;
  metadata: Record<string, unknown>;
  createdAt: string;
}

/** A note of the team. */
export interface LeadNote {
  id: number;
  leadId: number;
  body: string;
  author: string;
  createdAt: string;
}

/** A change of the state of a lead. */
export interface StatusHistoryEntry {
  id: number;
  leadId: number;
  previousStatus: string | null;
  newStatus: string;
  changedBy: string;
  changedAt: string;
}

/** An order of a lead. */
export interface LeadOrder {
  id: number;
  total: number;
  createdAt: string;
  items: { itemName: string; quantity: number; unitPrice: number; lineTotal: number }[];
}

/** The detail of a lead. */
export interface LeadDetailPayload {
  lead: Lead;
  timeline: TimelineEntry[];
  orders: LeadOrder[];
  notes: LeadNote[];
  statusHistory: StatusHistoryEntry[];
}

/** One column of the board. */
export interface KanbanColumn {
  status: string;
  label: string;
  leads: Lead[];
  count: number;
  total: number;
}

/** The board of the pipeline. */
export interface KanbanBoard {
  columns: KanbanColumn[];
}

/** The indicators of the sales. */
export interface LeadKpis {
  closedSales: number;
  wonCount: number;
  lostCount: number;
  totalCount: number;
  conversionRate: number;
  potentialAmount: number;
  byStatus: Record<string, number>;
}

/** The criteria of the listing of the leads. */
export interface LeadsCriteria {
  q?: string;
  status?: string;
  province?: string;
  source?: string;
  page?: number;
  size?: number;
}

/** The criteria of the board of the pipeline. */
export interface KanbanCriteria {
  q?: string;
  status?: string;
  province?: string;
  source?: string;
}

/** The range of the indicators of the sales. */
export interface KpisRange {
  from?: string;
  to?: string;
}

/**
 * The API of the CRM of the leads.
 *
 * <p>The reads answer in signals (P51): they return the `HttpResourceRef`
 * they build, so the URL, the params and the type stay here. The writes keep
 * `HttpClient` and their `Observable` — a write is an order, not a state.
 */
@Service()
export class AdminLeadsApi {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  /** The listing of the leads. */
  list(criteria: Signal<LeadsCriteria>): HttpResourceRef<Page<Lead> | undefined> {
    return httpResource<Page<Lead>>(() => ({
      url: `${this.baseUrl}/admin/leads`,
      params: this.paramsOf(criteria()),
    }));
  }

  /** The detail of a lead. */
  detail(id: Signal<number>): HttpResourceRef<LeadDetailPayload | undefined> {
    return httpResource<LeadDetailPayload>(() => `${this.baseUrl}/admin/leads/${id()}`);
  }

  /** Creates a lead by hand. */
  create(payload: {
    name: string;
    email: string;
    phone: string;
    province: string;
    saleAmount: number;
    consent: boolean;
  }): Observable<Lead> {
    return this.http.post<Lead>(`${this.baseUrl}/admin/leads`, payload);
  }

  /** Edits a lead. */
  update(
    id: number,
    payload: { name: string; phone: string; province: string; saleAmount: number; consent: boolean },
  ): Observable<Lead> {
    return this.http.put<Lead>(`${this.baseUrl}/admin/leads/${id}`, payload);
  }

  /** Deletes a lead. */
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/admin/leads/${id}`);
  }

  /** Changes the state of a lead. */
  changeStatus(
    id: number,
    payload: { status: string; outcome?: string; saleAmount?: number },
  ): Observable<Lead> {
    return this.http.patch<Lead>(`${this.baseUrl}/admin/leads/${id}/status`, payload);
  }

  /** Writes a note about a lead. */
  addNote(id: number, text: string): Observable<LeadNote> {
    return this.http.post<LeadNote>(`${this.baseUrl}/admin/leads/${id}/notes`, { text });
  }

  /** The board of the pipeline. */
  kanban(criteria: Signal<KanbanCriteria>): HttpResourceRef<KanbanBoard | undefined> {
    return httpResource<KanbanBoard>(() => ({
      url: `${this.baseUrl}/admin/leads/kanban`,
      params: this.paramsOf(criteria()),
    }));
  }

  /** The indicators of the sales. */
  kpis(range: Signal<KpisRange>): HttpResourceRef<LeadKpis | undefined> {
    return httpResource<LeadKpis>(() => {
      const { from, to } = range();
      let params = new HttpParams();
      if (from) {
        params = params.set('from', from);
      }
      if (to) {
        params = params.set('to', to);
      }
      return { url: `${this.baseUrl}/admin/leads/kpis`, params };
    });
  }

  /**
   * The serialization of the criteria of the listing.
   *
   * <p>The same keys and the same omissions the API has always answered: an
   * absent, empty or null criterion never flies.
   */
  private paramsOf(criteria: LeadsCriteria | KanbanCriteria): HttpParams {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(criteria)) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return params;
  }
}
