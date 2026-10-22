import { Component, computed, inject, input, signal } from '@angular/core';

import { LeadApi } from '@features/lead-api';
import { Tracker } from '@core/tracking/tracker.service';
import { AnonymousId } from '@core/tracking/anonymous-id';
import { CatalogItemDetail } from '@shared/models/catalog.models';
import { PROVINCES } from '@shared/models/provinces';
import { email, form, FormField, required } from '@angular/forms/signals';

/**
 * The notice form for an item that is out of stock (HU-08).
 *
 * <p>It only exists when the item is out of stock — for a service it never
 * appears. The visitor gives their email, the shop promises to write back
 * when stock returns, and the promise is recorded as a lead the moment they
 * consent.
 */
@Component({
  selector: 'app-stock-notify',
  imports: [FormField],
  templateUrl: './stock-notify.html',
  styleUrl: './stock-notify.scss',
})
export class StockNotify {

  private readonly leadApi = inject(LeadApi);
  private readonly tracker = inject(Tracker);
  private readonly anonymousId = inject(AnonymousId);

  /** The item the notice is for. */
  readonly item = input<CatalogItemDetail | null>(null);

  /** The provinces of Ecuador, for the form of the notice. */
  readonly provinces = PROVINCES;

  private readonly notifyModel = signal({
    email: '',
    name: '',
    province: 'PICHINCHA',
    consent: false,
  });

  readonly notifyForm = form(this.notifyModel, (schemaPath) => {
    email(schemaPath.email);
    required(schemaPath.email);
    required(schemaPath.name);
  });

  readonly notifyMessage = signal<string | null>(null);

  /** Whether the last answer was a refusal or a failure. */
  readonly notifyFailed = signal(false);

  /** Whether the item is out of stock — the notice is only offered then. */
  readonly outOfStock = computed(() => this.item()?.stockState === 'OUT_OF_STOCK');

  /** Asks for the notice that the item comes back. */
  subscribeToStock(event: Event): void {
    event.preventDefault();
    const item = this.item();
    if (!item || !this.consent) {
      this.report('Necesitamos tu consentimiento para avisarte.', true);
      return;
    }
    this.leadApi
      .subscribeToStock({
        ...this.notifyModel(),
        anonymousId: this.anonymousId.get(),
        itemId: item.id,
      })
      .subscribe({
        next: () => {
          this.report('Te avisamos en cuanto vuelva a haber stock.', false);
          this.tracker.track('STOCK_SUBSCRIBE', 'CATALOG_ITEM', item.id);
        },
        error: (error: { error?: { message?: string } }) => {
          this.report(error?.error?.message ?? 'No se pudo registrar tu aviso', true);
        },
      });
  }

  /** Writes the message of the conversation, with the voice of its outcome. */
  private report(text: string, failed: boolean): void {
    this.notifyMessage.set(text);
    this.notifyFailed.set(failed);
  }

  get email(): string { return this.notifyModel().email }

  get name(): string { return this.notifyModel().name }

  get province(): string { return this.notifyModel().province }

  get consent(): boolean { return this.notifyModel().consent }

}
