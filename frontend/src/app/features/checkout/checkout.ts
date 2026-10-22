import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { LeadApi } from '@features/lead-api';
import { CartStore } from '@core/cart/cart-store';
import { AnonymousId } from '@core/tracking/anonymous-id';
import { PROVINCES } from '@shared/models/provinces';
import { email, form, FormField, required } from '@angular/forms/signals';

/**
 * The checkout: the simulated purchase (HU-07).
 *
 * <p>There is no payment gateway: the order is recorded, the stock is
 * discounted and the customer becomes a lead. The prices are calculated by the
 * server, and the screen of the confirmation shows the ones that were really
 * charged.</p>
 *
 * <p>Built on the shape the signal forms pass declared (P42, after P38): one
 * signal model, the `form()` schema beside it, no public mutable fields, and
 * the conversation with the visitor carried by two signals through the single
 * `report(text, failed)` — the way `stock-notify` speaks. An unusable form
 * never prices an order: the general message walks the visitor to the fix, and
 * the server's word arrives verbatim when there is a real refusal.</p>
 */
@Component({
  selector: 'app-checkout',
  imports: [FormField, RouterLink],
  templateUrl: './checkout.html',
  styleUrl: './checkout.scss',
})
export class Checkout {
  private readonly cart = inject(CartStore);
  private readonly api = inject(LeadApi);
  private readonly anonymousId = inject(AnonymousId);

  readonly provinces = PROVINCES;
  readonly lines = this.cart.lines;
  readonly empty = computed(() => this.lines().length === 0);

  private readonly checkoutModel = signal({
    name: '',
    email: '',
    phone: '',
    province: 'PICHINCHA',
    consent: false,
  });

  readonly checkoutForm = form(this.checkoutModel, (schemaPath) => {
    email(schemaPath.email, { message: 'Ingresa un correo válido' });
    required(schemaPath.name, { message: 'Este campo es requerido' });
    required(schemaPath.email, { message: 'Este campo es requerido' });
  });

  /**
   * Whether a submit was attempted — the contract's second condition of
   * visibility: an untouched input that never tried to submit stays quiet.
   */
  readonly attempted = signal(false);

  readonly message = signal<string | null>(null);

  /** Whether the last answer was a refusal or a failure. */
  readonly failed = signal(false);

  readonly busy = signal(false);

  /** The answer of the server: the order that was created. */
  readonly confirmation = signal<{
    orderId: number;
    leadId: number;
    total: number;
    currency: string;
    lines: { itemName: string; quantity: number; unitPrice: number; lineTotal: number }[];
  } | null>(null);

  /** Completes the purchase. */
  submit(event: Event): void {
    event.preventDefault();
    this.attempted.set(true);
    if (this.busy() || this.empty()) {
      return;
    }
    if (!this.consent) {
      this.report('Necesitamos tu consentimiento para poder contactarte.', true);
      return;
    }
    if (this.checkoutForm().invalid()) {
      this.report('Revisa los campos marcados antes de confirmar tu compra.', true);
      return;
    }

    this.busy.set(true);
    this.report(null, false);

    this.api
      .checkout({
        anonymousId: this.anonymousId.get(),
        customer: {
          name: this.name.trim(),
          email: this.email.trim(),
          phone: this.phone.trim(),
          province: this.province,
          consent: this.consent,
        },
        items: this.lines().map((line) => ({ itemId: line.itemId, quantity: line.quantity })),
      })
      .subscribe({
        next: (confirmation) => {
          this.busy.set(false);
          this.confirmation.set(confirmation);
          this.cart.clear();
        },
        error: (error: { error?: { message?: string } }) => {
          this.busy.set(false);
          this.report(error?.error?.message ?? 'No se pudo completar la compra', true);
        },
      });
  }

  /** Writes the message of the conversation, with the voice of its outcome. */
  private report(text: string | null, failed: boolean): void {
    this.message.set(text);
    this.failed.set(failed);
  }

  get name(): string {
    return this.checkoutModel().name;
  }

  get email(): string {
    return this.checkoutModel().email;
  }

  get phone(): string {
    return this.checkoutModel().phone;
  }

  get province(): string {
    return this.checkoutModel().province;
  }

  get consent(): boolean {
    return this.checkoutModel().consent;
  }
}
