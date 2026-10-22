import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { API_BASE_URL } from '@core/api/api-base-url';
import { CatalogItemAdmin } from './admin-catalog-api';
import { CatalogAdmin } from './catalog-admin';

/** A host that holds the listing the panel renders. */
@Component({
  selector: 'admin-host',
  imports: [CatalogAdmin],
  template: '<app-catalog-admin />',
})
class Host {}

/** One item of the listing, as the API presents it. */
const itemOf = (id: number, name: string): CatalogItemAdmin => ({
  id,
  slug: `item-${id}`,
  name,
  type: 'PRODUCT',
  price: { basePrice: 100, finalPrice: 90, discountPercentage: 10, currency: 'USD' },
  stock: 5,
  featured: false,
  active: true,
  deletedAt: null,
  createdAt: '2026-10-21T00:00:00Z',
  categories: [],
});

/** The Page of the listing, answering the panel's first read. */
const pageOf = (items: CatalogItemAdmin[]) => ({
  content: items,
  page: 0,
  size: 12,
  totalElements: items.length,
  totalPages: 1,
});

describe('CatalogAdmin (P44, the forms of the pass)', () => {
  let fixture: ReturnType<typeof TestBed.createComponent<Host>>;
  let backend: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Host],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: API_BASE_URL, useValue: '/api' },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Host);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    backend.verify();
  });

  /** Arrives at the panel, its first listing answered. */
  const arrive = async (items = [itemOf(1, 'Termo')]): Promise<void> => {
    fixture.detectChanges();
    backend
      .expectOne((request) => request.url === '/api/admin/catalog' && request.method === 'GET')
      .flush(pageOf(items));
    await fixture.whenStable();
    fixture.detectChanges();
  };

  /** The rendered container of the panel. */
  const el = () => fixture.nativeElement as HTMLElement;

  /** Opens the discount panel of the first item, by its own button. */
  const buttonNamed = (name: string): HTMLButtonElement =>
    [...el().querySelectorAll<HTMLButtonElement>('button')].find(
      (candidate) => candidate.textContent?.trim() === name,
    )!;

  const openPanel = async (): Promise<void> => {
    buttonNamed('Descuento').click();
    await fixture.whenStable();
    fixture.detectChanges();
  };

  /** The form of the discount row, as the ledger holds it. */
  const discountForm = () => el().querySelector<HTMLFormElement>('.xtrim-ledger__discount form')!;

  it('arrives quiet: a pristine panel shows no error word', async () => {
    await arrive();
    await openPanel();

    expect(el().querySelector('.xtrim-error')).toBeNull();
    expect(el().querySelector('[role="alert"]')).toBeNull();
  });

  it('says the contract under a percentage touched and left empty', async () => {
    await arrive();
    await openPanel();

    const percentage = el().querySelector<HTMLInputElement>('#discount-percentage')!;
    percentage.value = '';
    percentage.dispatchEvent(new Event('input'));
    percentage.dispatchEvent(new Event('blur'));
    await fixture.whenStable();
    fixture.detectChanges();

    const error = el().querySelector('#discount-percentage-error');
    expect(error?.textContent).toContain('Este campo es requerido');
    expect(percentage.getAttribute('aria-invalid')).toBe('true');
  });

  it('holds the door on a negative percentage: the word, the voice, no call', async () => {
    await arrive();
    await openPanel();

    const percentage = el().querySelector<HTMLInputElement>('#discount-percentage')!;
    percentage.value = '-4';
    percentage.dispatchEvent(new Event('input'));
    await fixture.whenStable();

    discountForm().dispatchEvent(new Event('submit'));
    await fixture.whenStable();
    fixture.detectChanges();

    const error = el().querySelector('#discount-percentage-error');
    expect(error?.textContent).toContain('El porcentaje no puede ser negativo');
    expect(percentage.getAttribute('aria-invalid')).toBe('true');
    expect(el().querySelector('[role="alert"]')?.textContent).toContain(
      'Revisa el porcentaje antes de aplicar el descuento',
    );
    expect(backend.match((request) => request.url.includes('/discount')).length).toBe(0);
  });

  it('applies a valid percentage and speaks the house’s word', async () => {
    await arrive();
    await openPanel();

    const percentage = el().querySelector<HTMLInputElement>('#discount-percentage')!;
    percentage.value = '25';
    percentage.dispatchEvent(new Event('input'));
    await fixture.whenStable();

    discountForm().dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    const request = backend.expectOne((r) => r.url.includes('/discount'));
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ percentage: 25, startsAt: null, endsAt: null });
    request.flush(null);
    // The success re-reads the listing: the reload's own read answers too.
    fixture.detectChanges();
    const reload = backend.expectOne((r) => r.url === '/api/admin/catalog' && r.method === 'GET');
    reload.flush(pageOf([itemOf(1, 'Termo')]));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(el().querySelector('.xtrim-message')?.textContent).toContain('Descuento aplicado');
  });

  it('opens every panel pristine, even after a refusal', async () => {
    await arrive();
    await openPanel();

    // The refusal of the first opening: an empty touched percentage.
    const percentage = el().querySelector<HTMLInputElement>('#discount-percentage')!;
    percentage.value = '';
    percentage.dispatchEvent(new Event('input'));
    percentage.dispatchEvent(new Event('blur'));
    await fixture.whenStable();
    fixture.detectChanges();
    expect(el().querySelector('.xtrim-error')).not.toBeNull();

    // The panel closed and opened again: no debt may greet the reader.
    buttonNamed('Cancelar')!.click();
    await fixture.whenStable();
    fixture.detectChanges();
    await openPanel();
    await fixture.whenStable();
    fixture.detectChanges();

    const error = el().querySelector('#discount-percentage-error');
    expect(error).toBeNull();
    // And the reset percentage is the one in force, so the answer is valid.
    expect(el().querySelector<HTMLInputElement>('#discount-percentage')!.value).toBe('10');
  });

  it('re-reads the listing with the criteria the filters speak', async () => {
    await arrive();

    const query = el().querySelector<HTMLInputElement>('#admin-q')!;
    query.value = '  termo  ';
    query.dispatchEvent(new Event('input'));
    await fixture.whenStable();

    el().querySelector<HTMLFormElement>('.xtrim-filters')!.dispatchEvent(new Event('submit'));
    // The criteria change and the resource asks again on its own.
    fixture.detectChanges();

    const request = backend.expectOne((r) => r.url === '/api/admin/catalog');
    expect(request.request.params.get('q')).toBe('termo');
    expect(request.request.params.get('state')).toBe('ALL');
    expect(request.request.params.get('sort')).toBe('newest');
    request.flush(pageOf([]));
    await fixture.whenStable();
  });

  it('worded, never blank: the parse error’s fallback answer exists', async () => {
    // The parse error of type="number" cannot be staged in jsdom; its answer
    // lives in the component's computed, so it is read directly: with no
    // refusal at the door it speaks the house's fallback word (D4).
    const direct = TestBed.createComponent(CatalogAdmin);
    direct.detectChanges();
    // Whatever reads the listing fired while the direct instance booted: all
    // its reads face the same answer, empty.
    backend
      .match((request) => request.url === '/api/admin/catalog' && request.method === 'GET')
      .forEach((request) => request.flush(pageOf([])));
    await direct.whenStable();

    expect(direct.componentInstance.discountPercentageMessage()).toBe('Ingresa un número válido.');
  });

  it('keeps the painted table while the re-read flies (P51 D2)', async () => {
    await arrive();

    // A mutation's success re-reads the listing: the answer that is painted
    // stays painted while the fresh one flies.
    el().querySelector<HTMLButtonElement>('button.ui.small.button')!.click();
    await fixture.whenStable();

    const request = backend.expectOne((r) => r.method === 'PATCH');
    request.flush(null);
    fixture.detectChanges();

    // The table is still there, and the skeleton is not.
    expect(el().querySelectorAll('tbody tr').length).toBe(1);
    expect(el().querySelector('app-loading')).toBeNull();

    const reload = backend.expectOne((r) => r.url === '/api/admin/catalog' && r.method === 'GET');
    reload.flush(pageOf([itemOf(1, 'Termo')]));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(el().querySelectorAll('tbody tr').length).toBe(1);
  });
});
