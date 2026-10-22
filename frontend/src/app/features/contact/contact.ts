import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { LeadApi } from '@features/lead-api';
import { AnonymousId } from '@core/tracking/anonymous-id';
import { Tracker } from '@core/tracking/tracker.service';
import { PROVINCES } from '@shared/models/provinces';
import { email, form, FormField, required } from '@angular/forms/signals';

/**
 * The form that asks for more information (HU-12).
 *
 * <p>Built on the shape the signal forms pass declared (P41): one signal model,
 * the `form()` schema beside it, no public mutable fields, and the conversation
 * with the visitor carried by two signals through the single `report(text, failed)`
 * — the way `stock-notify` speaks. The consent is explicit and it is refused
 * before anything is sent; the field words appear only touched-invalid or after
 * an attempted submit. An empty submit never knocks the door.</p>
 */
@Component({
  selector: 'app-contact',
  imports: [FormField],
  templateUrl: './contact.html',
  styleUrl: './contact.scss',
})
export class Contact {
  private readonly api = inject(LeadApi);
  private readonly anonymousId = inject(AnonymousId);
  private readonly tracker = inject(Tracker);
  private readonly route = inject(ActivatedRoute);

  readonly provinces = PROVINCES;

  private readonly contactModel = signal({
    name: '',
    email: '',
    phone: '',
    province: 'PICHINCHA',
    message: '',
    consent: false,
  });

  readonly contactForm = form(this.contactModel, (schemaPath) => {
    required(schemaPath.name, { message: 'Este campo es requerido' });
    required(schemaPath.email, { message: 'Este campo es requerido' });
    email(schemaPath.email, { message: 'Ingresa un correo válido' });
  });

  /** The item the page arrived asking about. It stays unresolved today: the
   *  product's name is carried by the message, never by an id. */
  readonly itemId = signal<number | null>(null);

  /**
   * Whether a submit was attempted — the contract's second condition of
   * visibility: an untouched input that never tried to submit stays quiet.
   */
  readonly attempted = signal(false);

  readonly contactMessage = signal<string | null>(null);

  /** Whether the last answer was a refusal or a failure. */
  readonly contactFailed = signal(false);

  readonly done = signal(false);
  readonly busy = signal(false);

  constructor() {
    const item = this.route.snapshot.queryParamMap.get('item');
    if (item) {
      this.contactModel.update((model) => ({
        ...model,
        message: `Me interesa el producto «${item}».`,
      }));
    }
  }

  /** Sends the request. */
  submit(event: Event): void {
    event.preventDefault();
    if (this.busy()) {
      return;
    }
    if (!this.consent) {
      this.report('Necesitamos tu consentimiento para poder contactarte.', true);
      return;
    }
    this.attempted.set(true);
    if (this.contactForm().invalid()) {
      this.report('Revisa los campos marcados antes de enviar.', true);
      return;
    }
    this.busy.set(true);
    this.report(null, false);

    const model = this.contactModel();
    this.api
      .infoRequest({
        name: model.name.trim(),
        email: model.email.trim(),
        phone: model.phone.trim(),
        province: model.province,
        consent: model.consent,
        anonymousId: this.anonymousId.get(),
        itemIds: this.itemId() === null ? [] : [this.itemId() as number],
        message: model.message.trim(),
      })
      .subscribe({
        next: () => {
          this.busy.set(false);
          this.done.set(true);
          this.tracker.track('INFO_REQUEST', 'LEAD', this.itemId() ?? undefined);
        },
        error: (error: { error?: { message?: string } }) => {
          this.busy.set(false);
          this.report(error?.error?.message ?? 'No se pudo enviar la solicitud', true);
        },
      });
  }

  /** Writes the message of the conversation, with the voice of its outcome. */
  private report(text: string | null, failed: boolean): void {
    this.contactMessage.set(text);
    this.contactFailed.set(failed);
  }

  get consent(): boolean {
    return this.contactModel().consent;
  }
}
