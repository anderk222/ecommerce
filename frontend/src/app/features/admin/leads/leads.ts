import { Component, computed, inject, signal } from '@angular/core';
import { form, FormField } from '@angular/forms/signals';

import { AdminLeadsApi, Lead, LeadsCriteria } from '@features/admin/leads/admin-leads-api';
import { Pagination } from '@shared/components/pagination/pagination';
import { LeadDetail } from './lead-detail';


/**
 * The listing of the leads in the CRM (HU-21).
 *
 * <p>It is a list and a Kanban board, one screen or the other (HU-23). The filters
 * row speaks Signal Forms since P45 — the pass's shape with no validating field:
 * a search that may stay empty, two selects that hold nothing but valid answers,
 * a `form()` with no schema rules, and the native submit with `novalidate` (P38 §7).
 *
 * <p>The listing answers in signals (P51): the panel holds the resource, the
 * filters' submit and the page set the criteria in force, and the one box of
 * the screen speaks the refusal of the read too (D4).</p>
 */
@Component({
  selector: 'app-leads',
  imports: [FormField, Pagination, LeadDetail],
  templateUrl: './leads.html',
})
export class Leads {
  private readonly api = inject(AdminLeadsApi);

  /** The filters of the row, held by the signal model of the pass (P45). */
  private readonly filtersModel = signal({ query: '', status: '', source: '' });

  /** No schema rules: nothing in the row is a promise to break (P45 D1). */
  readonly filterForm = form(this.filtersModel);

  /** The criteria in force: the filters' submit and the page set them, and
   *  the resource asks again on its own. */
  private readonly applied = signal(this.criteriaOf(0));

  /** The listing of the leads, answering in signals. */
  readonly leads = this.api.list(this.applied);

  /** The rows of the page in force. */
  readonly rows = computed(() => (this.leads.hasValue() ? this.leads.value().content : []));

  /** The page of the results in force. */
  readonly page = computed(() => (this.leads.hasValue() ? this.leads.value().page : 0));

  /** The number of pages of the results in force. */
  readonly totalPages = computed(() => (this.leads.hasValue() ? this.leads.value().totalPages : 0));

  /** The number of leads the criteria answer for. */
  readonly totalElements = computed(() =>
    this.leads.hasValue() ? this.leads.value().totalElements : 0,
  );

  readonly message = signal<string | null>(null);
  readonly failed = signal(false);

  /** The refusal of the read, in the same voice as the writes (P51 D4). */
  protected readonly readError = computed(() => {
    const error = this.leads.error();
    return error ? this.messageOf(error) : null;
  });

  /** The lead whose detail is open, if any. */
  readonly openLeadId = signal<number | null>(null);

  readonly statuses = [
    { value: '', label: 'Todos los estados' },
    { value: 'NEW', label: 'Nuevo' },
    { value: 'IN_PROGRESS', label: 'En gestión' },
    { value: 'CLOSED', label: 'Cerrado' },
  ];

  readonly sources = [
    { value: '', label: 'Todos los orígenes' },
    { value: 'CHECKOUT', label: 'Compra' },
    { value: 'STOCK_NOTIFY', label: 'Aviso de stock' },
    { value: 'INFO_REQUEST', label: 'Solicitud de información' },
    { value: 'WHATSAPP', label: 'WhatsApp' },
    { value: 'MANUAL', label: 'Manual' },
  ];

  /** Applies the filters. */
  filter(event: Event): void {
    event.preventDefault();
    this.applied.set(this.criteriaOf(0));
  }

  /** Opens the detail of a lead. */
  open(lead: Lead): void {
    this.openLeadId.set(lead.id);
  }

  /** Closes the detail of a lead. */
  close(): void {
    this.openLeadId.set(null);
  }

  /** Goes to a page of the listing. */
  goToPage(page: number): void {
    this.applied.set(this.criteriaOf(page));
  }

  /** The criteria of the listing, for the page in force. */
  private criteriaOf(page: number): LeadsCriteria {
    const filters = this.filtersModel();
    return {
      q: filters.query.trim() || undefined,
      status: filters.status || undefined,
      source: filters.source || undefined,
      page,
      size: 20,
    };
  }

  private report(text: string, failed: boolean): void {
    this.message.set(text);
    this.failed.set(failed);
  }

  /** The word of a refusal: the one of the server, tal y como llegó (P26). */
  private messageOf(error: unknown): string {
    const body = (error as { error?: { message?: string } } | null)?.error;
    return body?.message ?? 'La operación falló';
  }
}
