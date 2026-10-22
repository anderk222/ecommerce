import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';

import { min, max, form, FormField, required } from '@angular/forms/signals';

import {
  AdminCatalogApi,
  AdminCatalogCriteria,
  CatalogItemAdmin,
} from './admin-catalog-api';
import { Loading } from '@shared/components/loading/loading';
import { Pagination } from '@shared/components/pagination/pagination';
import { STATES, TYPES } from '@shared/models/catalog.models';
import { DEFAULT_DISCOUNT, PAGE_SIZE, SORTS } from '@shared/models/pagination';

/** The one voice of the screen: its words and the outcome they carry. */
interface Notice {
  readonly text: string;
  readonly failed: boolean;
}

/**
 * The unified listing of the catalogue in the panel (HU-14).
 *
 * <p>It is the same view of the catalogue with the filters of the
 * administration: the kind of item, the state, the free text and the
 * ordering.</p>
 *
 * <p>Built on the shape the signal forms pass declared (P44, after P38):
 * two forms in one file, each with its own signal model and its `form()`
 * schema — the filters row carries no validator (nothing in it can be
 * invalid, D2), the discount panel guards the numeric bounds the domain
 * declares (0–100) and an unusable percentage never reaches the server (D1).
 * The conversation is carried by the page's single voice, `notice` — the way
 * `stock-notify` speaks.</p>
 *
 * <p>The listing answers in signals (P51): the panel holds the resource, the
 * filters' submit and the page set the criteria in force, and the wait is
 * `isLoading()` behind `hasValue()` — a re-read after a mutation no longer
 * blanks the painted table.</p>
 */
@Component({
  selector: 'app-catalog-admin',
  imports: [FormField, RouterLink, Loading, Pagination],
  templateUrl: './catalog-admin.html',
})
export class CatalogAdmin {
  private readonly api = inject(AdminCatalogApi);

  protected readonly types = TYPES;
  protected readonly states = STATES;
  protected readonly sorts = SORTS;

  // ── The listing ───────────────────────────────────────────────────────

  private readonly filtersModel = signal({
    q: '',
    type: '',
    state: 'ALL',
    sort: 'newest',
  });

  /** The filters speak Signal Forms without validators: nothing in them
   *  can be invalid, the schema lives on Signal Forms all the same (P44 D2). */
  protected readonly filtersForm = form(this.filtersModel);

  // Order matters: `applied` reads `filtersModel` through `criteriaOf`.

  /** The criteria in force: the filters' submit and the page set them, and
   *  the resource asks again on its own. */
  private readonly applied = signal(this.criteriaOf(0));

  /** The page of the catalogue, answering in signals. */
  protected readonly items = this.api.list(this.applied);

  /** The page in force; null while the first answer is pending or after a refusal. */
  protected readonly result = computed(() =>
    this.items.hasValue() ? this.items.value() : null,
  );

  // ── The voice of the screen ───────────────────────────────────────────

  /** The message of the last write (or the refusal of the discount submit). */
  private readonly feedback = signal<Notice | null>(null);

  /** The one box of the screen: the word of the write keeps the priority;
   *  otherwise the refusal of the read, in the same voice (P51 D4). */
  protected readonly notice = computed<Notice | null>(() => {
    const feedback = this.feedback();
    if (feedback) {
      return feedback;
    }
    const error = this.items.error();
    return error ? { text: this.messageOf(error), failed: true } : null;
  });

  // ── The discount panel ────────────────────────────────────────────────

  /** The item whose discount is being edited, if any. */
  protected readonly discountId = signal<number | null>(null);

  private readonly discountModel = signal({
    percentage: DEFAULT_DISCOUNT as number | null,
    endsAt: '',
  });

  /** The numeric bounds the domain declares (P12: 0–100); the words are
   *  born at the validator, the template only prints them (P44 D4). */
  protected readonly discountForm = form(this.discountModel, (schemaPath) => {
    required(schemaPath.percentage, { message: 'Este campo es requerido' });
    min(schemaPath.percentage, 0, { message: 'El porcentaje no puede ser negativo.' });
    max(schemaPath.percentage, 100, { message: 'El porcentaje no puede pasar de 100.' });
  });

  /**
   * Whether a discount submit was attempted — the contract's second condition
   * of visibility: an untouched input that never tried to apply stays quiet.
   */
  private readonly discountAttempted = signal(false);

  /**
   * The word of the percentage, or null while it must stay quiet (valid, or
   * neither touched nor attempted). The browser's parse error arrives without
   * words of its own — the house answers it here (D4).
   */
  protected readonly percentageError = computed(() => {
    const field = this.discountForm.percentage();
    if (!(field.touched() || this.discountAttempted()) || !field.invalid()) {
      return null;
    }
    return field.errors()[0]?.message ?? 'Ingresa un número válido.';
  });

  // ── Listing actions ───────────────────────────────────────────────────

  /** Applies the filters. */
  protected filter(event: Event): void {
    event.preventDefault();
    this.applied.set(this.criteriaOf(0));
  }

  /** Goes to a page of the listing. */
  protected goToPage(page: number): void {
    this.applied.set(this.criteriaOf(page));
  }

  /** Activates or deactivates an item. */
  protected toggleActive(item: CatalogItemAdmin): void {
    const success = item.active ? 'Ítem inactivado' : 'Ítem reactivado';
    this.mutate(this.api.setActive(item.id, !item.active), success);
  }

  /** Deletes an item, after a confirmation. */
  protected remove(item: CatalogItemAdmin): void {
    if (!confirm(`¿Eliminar «${item.name}»? El historial se conserva.`)) {
      return;
    }
    this.mutate(this.api.delete(item.id), 'Ítem eliminado');
  }

  // ── Discount actions ──────────────────────────────────────────────────

  /** Starts the edition of the discount of an item: the panel opens pristine
   *  (D6) — the model fresh, the attempt cleared, no debt of the last refusal. */
  protected startDiscount(item: CatalogItemAdmin): void {
    this.discountId.set(item.id);
    this.discountModel.set({
      percentage: item.price.discountPercentage || DEFAULT_DISCOUNT,
      endsAt: '',
    });
    this.discountAttempted.set(false);
  }

  /** Closes the discount panel without applying anything. */
  protected cancelDiscount(): void {
    this.discountId.set(null);
  }

  /** Applies the discount. An unusable percentage never reaches the server (D1). */
  protected applyDiscount(event: Event): void {
    event.preventDefault();
    const id = this.discountId();
    if (id === null) {
      return;
    }
    this.discountAttempted.set(true);
    const { percentage, endsAt } = this.discountModel();
    // `percentage === null` is already invalid (`required`); the check narrows the type.
    if (this.discountForm().invalid() || percentage === null) {
      this.feedback.set({
        text: 'Revisa el porcentaje antes de aplicar el descuento.',
        failed: true,
      });
      return;
    }
    this.mutate(this.api.applyDiscount(id, percentage, endsAt || null), 'Descuento aplicado', () =>
      this.cancelDiscount(),
    );
  }

  /** Revokes the discount in force. */
  protected revokeDiscount(item: CatalogItemAdmin): void {
    this.mutate(this.api.revokeDiscount(item.id), 'Descuento revocado', () =>
      this.cancelDiscount(),
    );
  }

  // ── Internals ─────────────────────────────────────────────────────────

  /** The shape every write shares: on success, speak and re-read the listing;
   *  on refusal, speak the word of the server. */
  private mutate(request: Observable<unknown>, success: string, onSuccess?: () => void): void {
    request.subscribe({
      next: () => {
        onSuccess?.();
        this.feedback.set({ text: success, failed: false });
        this.items.reload();
      },
      error: (error) => this.feedback.set({ text: this.messageOf(error), failed: true }),
    });
  }

  /** The criteria of the listing, for the page in force. */
  private criteriaOf(page: number): AdminCatalogCriteria {
    const filters = this.filtersModel();
    return {
      q: filters.q.trim() || undefined,
      type: filters.type || undefined,
      state: filters.state,
      sort: filters.sort,
      page,
      size: PAGE_SIZE,
    };
  }

  /** The word of a refusal: the one of the server, tal y como llegó (P26). */
  private messageOf(error: unknown): string {
    const body = (error as { error?: { message?: string } } | null)?.error;
    return body?.message ?? 'La operación falló';
  }
}