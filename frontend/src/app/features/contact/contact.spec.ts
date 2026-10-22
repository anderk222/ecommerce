import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, RouterOutlet } from '@angular/router';
import { of, throwError } from 'rxjs';

import { LeadApi } from '@features/lead-api';
import { AnonymousId } from '@core/tracking/anonymous-id';
import { Tracker } from '@core/tracking/tracker.service';
import { Contact } from './contact';

/** An empty shell that holds the outlet where the contact is rendered. */
@Component({ imports: [RouterOutlet], template: '<router-outlet />' })
class Shell {}

describe('Contact', () => {
  let fixture: ReturnType<typeof TestBed.createComponent<Shell>>;
  const infoRequest = vi.fn();
  const track = vi.fn();

  beforeEach(async () => {
    infoRequest.mockReset();
    track.mockReset();

    await TestBed.configureTestingModule({
      imports: [Shell, Contact],
      providers: [
        provideRouter([{ path: 'contacto', component: Contact }]),
        { provide: LeadApi, useValue: { infoRequest } },
        { provide: AnonymousId, useValue: { get: () => 'anon-test-id' } },
        { provide: Tracker, useValue: { track } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Shell);
  });

  /** Arrives at the contact page with the query params of the visit. */
  const arrive = async (queryParams?: Record<string, string>): Promise<void> => {
    await TestBed.inject(Router).navigate(['/contacto'], { queryParams });
    await fixture.whenStable();
  };

  /** The rendered page, inside the shell. */
  const el = () => fixture.nativeElement as HTMLElement;

  /** The form of the page. */
  const form = () => el().querySelector<HTMLFormElement>('form')!;

  /** Fills the consent checkbox, so the submit can pass the consent's gate. */
  const consent = (): void => {
    const box = el().querySelector<HTMLInputElement>('#contact-consent')!;
    // The signal-forms native control reads the checkbox on the `input` event
    // (not `change`), so the test marks it and fires what the directive hears.
    box.checked = true;
    box.dispatchEvent(new Event('input', { bubbles: true }));
  };

  it('arrives quiet: a pristine form shows no error word', async () => {
    await arrive();

    expect(el().querySelector('.xtrim-error')).toBeNull();
    expect(el().querySelector('[role="alert"]')).toBeNull();
  });

  it('says the contract under the name a visitor touched and left empty', async () => {
    await arrive();

    const name = el().querySelector<HTMLInputElement>('#contact-name')!;
    name.value = '';
    name.dispatchEvent(new Event('input'));
    name.dispatchEvent(new Event('blur'));
    await fixture.whenStable();

    const error = el().querySelector('#contact-name-error');
    expect(error?.textContent).toContain('Este campo es requerido');
    expect(name.getAttribute('aria-invalid')).toBe('true');
  });

  it('says the email contract under a bad address the visitor touched', async () => {
    await arrive();

    const email = el().querySelector<HTMLInputElement>('#contact-email')!;
    email.value = 'no-es-un-correo';
    email.dispatchEvent(new Event('input'));
    email.dispatchEvent(new Event('blur'));
    await fixture.whenStable();

    const error = el().querySelector('#contact-email-error');
    expect(error?.textContent).toContain('Ingresa un correo válido');
  });

  it('refuses the send before the consent, without a field scold', async () => {
    await arrive();

    form().dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    const alert = el().querySelector('[role="alert"]');
    expect(alert?.textContent).toContain('Necesitamos tu consentimiento');
    expect(el().querySelector('.xtrim-error')).toBeNull();
    expect(infoRequest).not.toHaveBeenCalled();
  });

  it('walks the visitor to the fix before sending an empty form', async () => {
    await arrive();
    consent();
    await fixture.whenStable();

    form().dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    expect(el().querySelectorAll('.xtrim-error').length).toBe(2);
    const alert = el().querySelector('[role="alert"]');
    expect(alert?.textContent).toContain('Revisa los campos marcados antes de enviar.');
    expect(infoRequest).not.toHaveBeenCalled();
  });

  it('sends the trimmed request when the form is complete', async () => {
    infoRequest.mockReturnValue(
      of({ leadId: 7, name: 'Ana', email: 'ana@xtrim.ec', status: 'NUEVO' }),
    );
    await arrive();

    const name = form().querySelector<HTMLInputElement>('#contact-name')!;
    name.value = '  Ana  ';
    name.dispatchEvent(new Event('input'));
    const email = form().querySelector<HTMLInputElement>('#contact-email')!;
    email.value = '  ana@xtrim.ec  ';
    email.dispatchEvent(new Event('input'));
    consent();
    await fixture.whenStable();

    form().dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    expect(infoRequest).toHaveBeenCalledTimes(1);
    const payload = infoRequest.mock.calls[0][0];
    expect(payload.name).toBe('Ana');
    expect(payload.email).toBe('ana@xtrim.ec');
    expect(payload.consent).toBe(true);
    expect(payload.anonymousId).toBe('anon-test-id');
    expect(payload.itemIds).toEqual([]);
    expect(track).toHaveBeenCalledWith('INFO_REQUEST', 'LEAD', undefined);
    expect(el().querySelector('.ui.positive.message')).not.toBeNull();
  });

  it('keeps the word of the server when the request fails', async () => {
    infoRequest.mockReturnValue(
      throwError(() => ({ error: { message: 'Ya te contactamos antes.' } })),
    );
    await arrive();

    const name = form().querySelector<HTMLInputElement>('#contact-name')!;
    name.value = 'Ana';
    name.dispatchEvent(new Event('input'));
    const email = form().querySelector<HTMLInputElement>('#contact-email')!;
    email.value = 'ana@xtrim.ec';
    email.dispatchEvent(new Event('input'));
    consent();
    await fixture.whenStable();

    form().dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    const alert = el().querySelector('[role="alert"]');
    expect(alert?.textContent).toContain('Ya te contactamos antes.');
  });

  it('pre-fills the message when it arrives from a product', async () => {
    await arrive({ item: 'Térmico Alpina' });

    const message = el().querySelector<HTMLTextAreaElement>('#contact-message')!;
    expect(message.value).toContain('Térmico Alpina');
  });
});
