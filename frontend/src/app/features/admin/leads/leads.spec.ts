import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_BASE_URL } from '@core/api/api-base-url';
import { Lead } from '@features/admin/leads/admin-leads-api';
import { Page } from '@shared/models/catalog.models';
import { Leads } from './leads';

/** A page of the CRM: two leads, one page, one closed door of nothing else. */
const LEADS: Lead[] = [
  {
    id: 1,
    name: 'Ana Pérez',
    email: 'ana@xtrim.ec',
    phone: '0999999999',
    province: 'PICHINCHA',
    status: 'NEW',
    sources: ['CHECKOUT'],
    saleAmount: 120,
    outcome: null,
    consent: true,
    createdAt: '2026-10-20T10:00:00Z',
    updatedAt: '2026-10-20T10:00:00Z',
  },
  {
    id: 2,
    name: 'Luis Mora',
    email: 'luis@xtrim.ec',
    phone: null,
    province: 'GUAYAS',
    status: 'CLOSED',
    sources: ['INFO_REQUEST'],
    saleAmount: 0,
    outcome: 'LOST',
    consent: true,
    createdAt: '2026-10-19T09:00:00Z',
    updatedAt: '2026-10-19T09:00:00Z',
  },
];

const PAGE: Page<Lead> = { content: LEADS, page: 0, size: 20, totalElements: 2, totalPages: 1 };

describe('Leads', () => {
  let fixture: ReturnType<typeof TestBed.createComponent<Leads>>;
  let backend: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Leads],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: '/api' },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Leads);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    backend.verify();
  });

  /** The read of the listing, as the resource asks for it (P51 D7). */
  const listing = () =>
    backend.expectOne((request) => request.url === '/api/admin/leads' && request.method === 'GET');

  /** Arrives at the CRM with its first read answered. */
  const arrive = async (): Promise<void> => {
    fixture.detectChanges();
    listing().flush(PAGE);
    await fixture.whenStable();
    fixture.detectChanges();
  };

  const el = () => fixture.nativeElement as HTMLElement;

  /** Writes a value into a control the way the browser delivers it. */
  const type = (selector: string, value: string): void => {
    const input = el().querySelector<HTMLInputElement | HTMLSelectElement>(selector)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
    input.dispatchEvent(new Event('change'));
  };

  /** The options of a select of the row, as the browser offers them. */
  const optionsOf = (id: string): string[] =>
    Array.from((el().querySelector<HTMLSelectElement>('#' + id) as HTMLSelectElement).options).map(
      (option) => option.value,
    );

  /** Applies the filters the way the browser would: the native submit of the
   *  form. The criteria change and the resource asks again on its own — the
   *  change detection is what fires the read. */
  const apply = (): void => {
    el().querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();
  };

  it('arrives with the whole listing and no filter word anywhere (P45 D1)', async () => {
    fixture.detectChanges();
    const request = listing();
    expect(request.request.params.get('q')).toBeNull();
    expect(request.request.params.get('status')).toBeNull();
    expect(request.request.params.get('source')).toBeNull();
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('20');
    request.flush(PAGE);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(el().querySelector('.xtrim-error')).toBeNull();
    expect(el().querySelectorAll('tbody tr').length).toBe(2);
  });

  it('rides the filters the admin set on the request', async () => {
    await arrive();
    type('#leads-q', '  termo  ');
    type('#leads-status', 'IN_PROGRESS');
    type('#leads-source', 'CHECKOUT');

    apply();

    const request = listing();
    expect(request.request.params.get('q')).toBe('termo');
    expect(request.request.params.get('status')).toBe('IN_PROGRESS');
    expect(request.request.params.get('source')).toBe('CHECKOUT');
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe('20');
    request.flush(PAGE);
    await fixture.whenStable();
  });

  it('lands on page 0 on every apply, whatever the listing was showing', async () => {
    await arrive();
    fixture.componentInstance.goToPage(1);
    fixture.detectChanges();

    const page = listing();
    expect(page.request.params.get('page')).toBe('1');
    page.flush(PAGE);
    await fixture.whenStable();

    apply();

    const request = listing();
    expect(request.request.params.get('page')).toBe('0');
    request.flush(PAGE);
    await fixture.whenStable();
  });

  it('sends no q when the search stays empty', async () => {
    await arrive();
    type('#leads-q', '   ');

    apply();

    const request = listing();
    expect(request.request.params.get('q')).toBeNull();
    request.flush(PAGE);
    await fixture.whenStable();
  });

  it('keeps the server word verbatim through the alert (P26)', async () => {
    await arrive();
    apply();

    listing().flush(
      { message: 'El servidor no alcanzó a ver a tus leads' },
      { status: 500, statusText: 'Server Error' },
    );
    await fixture.whenStable();
    fixture.detectChanges();

    // The refusal of the read speaks in the one box of the screen (P51 D4).
    const alert = el().querySelector('[role="alert"]')!;
    expect(alert.textContent).toContain('El servidor no alcanzó a ver a tus leads');
    expect(alert.classList.contains('is-error')).toBe(true);
  });

  it('offers the statuses and the origins of the house, untouched', async () => {
    await arrive();

    expect(optionsOf('leads-status')).toEqual(['', 'NEW', 'IN_PROGRESS', 'CLOSED']);
    expect(optionsOf('leads-source')).toEqual([
      '',
      'CHECKOUT',
      'STOCK_NOTIFY',
      'INFO_REQUEST',
      'WHATSAPP',
      'MANUAL',
    ]);
  });
});
