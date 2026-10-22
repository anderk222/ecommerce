import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_BASE_URL } from '@core/api/api-base-url';
import { LeadDetail } from './lead-detail';
import { LeadDetailPayload } from '@features/admin/leads/admin-leads-api';

/** A rich detail: one event of every operation, the product named. */
const PAYLOAD: LeadDetailPayload = {
  lead: {
    id: 5,
    name: 'Ana Pérez',
    email: 'ana@xtrim.ec',
    phone: '0999999999',
    province: 'PICHINCHA',
    status: 'IN_PROGRESS',
    sources: ['INFO_REQUEST'],
    saleAmount: 0,
    outcome: null,
    consent: true,
    createdAt: '2026-10-20T10:00:00Z',
    updatedAt: '2026-10-20T10:00:00Z',
  },
  timeline: [
    {
      type: 'ADD_TO_CART',
      entityType: 'CATALOG_ITEM',
      entityId: 42,
      productName: 'Térmico Alpina P35',
      metadata: { quantity: 2, via: 'BUTTON' },
      createdAt: '2026-10-20T09:00:00Z',
    },
    {
      type: 'SEARCH',
      entityType: 'CATALOG',
      entityId: null,
      metadata: { query: 'termo' },
      createdAt: '2026-10-20T08:30:00Z',
    },
    {
      type: 'REMOVE_FROM_CART',
      entityType: 'CATALOG_ITEM',
      entityId: 42,
      productName: 'Térmico Alpina P35',
      metadata: {},
      createdAt: '2026-10-20T08:10:00Z',
    },
    {
      type: 'PRODUCT_VIEW',
      entityType: 'CATALOG_ITEM',
      entityId: 42,
      productName: 'Térmico Alpina P35',
      metadata: { slug: 'termico-alpina' },
      createdAt: '2026-10-20T08:00:00Z',
    },
  ],
  orders: [],
  notes: [],
  statusHistory: [],
};

describe('LeadDetail', () => {
  let fixture: ReturnType<typeof TestBed.createComponent<LeadDetail>>;
  let backend: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LeadDetail],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: '/api' },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LeadDetail);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    backend.verify();
  });

  /** The read of the detail, as the resource asks for it (P51 D7). */
  const detail = () =>
    backend.expectOne((request) => request.url === '/api/admin/leads/5' && request.method === 'GET');

  /** Arrives at the detail of the lead, with its first read answered. */
  const arrive = async (): Promise<void> => {
    fixture.componentRef.setInput('leadId', 5);
    fixture.detectChanges();
    detail().flush(PAYLOAD);
    await fixture.whenStable();
    fixture.detectChanges();
  };

  const el = () => fixture.nativeElement as HTMLElement;

  const history = () => el().querySelector('.xtrim-history')!;

  const noteForm = () => el().querySelector<HTMLFormElement>('form.ui.form')!;

  const noteBody = () => el().querySelector<HTMLTextAreaElement>('#note-body')!;

  /** Writes a value into the textarea the way a staff member does. */
  const typeNote = (value: string): void => {
    noteBody().value = value;
    noteBody().dispatchEvent(new Event('input'));
  };

  const chips = () => Array.from(el().querySelectorAll<HTMLButtonElement>('.xtrim-filter-chip'));

  const chipOf = (label: string): HTMLButtonElement => {
    const chip = chips().find((candidate) => candidate.textContent?.includes(label));
    if (!chip) {
      throw new Error('no chip says ' + label);
    }
    return chip;
  };

  it('names the product of every event it can', async () => {
    await arrive();

    const text = history().textContent ?? '';
    expect(text).toContain('Vio el producto «Térmico Alpina P35»');
    expect(text).toContain('Agregó 2 × «Térmico Alpina P35» al carrito');
    expect(text).toContain('Sacó «Térmico Alpina P35» del carrito');
    expect(text).toContain('Buscó «termo» en el catálogo');
  });

  it('keeps the whole story while the filter is empty', async () => {
    await arrive();

    expect(chips().length).toBe(4);
    expect(el().querySelectorAll('.xtrim-history__entry').length).toBe(4);
    expect(el().textContent).toContain('4 de 4 eventos');
  });

  it('filters by two operations at once and says it', async () => {
    await arrive();

    chipOf('Vio').click();
    chipOf('Buscó').click();
    fixture.detectChanges();

    expect(el().querySelectorAll('.xtrim-history__entry').length).toBe(2);
    expect(el().textContent).toContain('2 de 4 eventos');
    expect(history().textContent).not.toContain('Sacó «Térmico');

    // The clearing goes back to the whole story.
    el().querySelector<HTMLButtonElement>('.xtrim-filter-chip--clear')!.click();
    fixture.detectChanges();

    expect(el().querySelectorAll('.xtrim-history__entry').length).toBe(4);
  });

  it('shows the details of an entry only once it is opened', async () => {
    await arrive();

    expect(el().querySelector('.xtrim-history__body')).toBeNull();

    el().querySelector<HTMLButtonElement>('.xtrim-history__head')!.click();
    fixture.detectChanges();

    const first = el().querySelector('.xtrim-history__body')!;
    expect(first.textContent).toContain('Producto');
    expect(first.textContent).toContain('Térmico Alpina P35');
    expect(first.textContent).toContain('Botón');

    el().querySelector<HTMLButtonElement>('.xtrim-history__head')!.click();
    fixture.detectChanges();
    expect(el().querySelector('.xtrim-history__body')).toBeNull();
  });

  it('arrives quiet: a pristine note form shows no error word', async () => {
    await arrive();

    expect(el().querySelector('.xtrim-error')).toBeNull();
    expect(el().querySelector('[role="alert"]')).toBeNull();
  });

  it('says the contract under the textarea a staff member touched and left empty', async () => {
    await arrive();

    typeNote('');
    noteBody().dispatchEvent(new Event('blur'));
    fixture.detectChanges();

    const error = el().querySelector('#note-body-error');
    expect(error?.textContent).toContain('Este campo es requerido');
    expect(noteBody().getAttribute('aria-invalid')).toBe('true');
  });

  it('walks the staff member to the fix on an empty note — and never knocks', async () => {
    await arrive();

    noteForm().dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(el().querySelectorAll('.xtrim-error').length).toBe(1);
    const alert = el().querySelector('[role="alert"]');
    expect(alert?.textContent).toContain('Revisa el campo marcado antes de agregar la nota.');
    // The attempt exists, but no request was made: an empty note is no write at all (P46 D1).
    expect(backend.match((request) => request.method === 'POST').length).toBe(0);
  });

  it('writes the trimmed note, clears the room and reads the detail again', async () => {
    await arrive();

    typeNote('  Llamada pendiente  ');
    noteForm().dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    const request = backend.expectOne('/api/admin/leads/5/notes');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ text: 'Llamada pendiente' });
    request.flush({
      id: 1,
      leadId: 5,
      body: 'Llamada pendiente',
      author: 'Admin',
      createdAt: '2026-10-21T10:00:00Z',
    });

    // The note re-reads the detail: the reload's own read answers too.
    fixture.detectChanges();
    detail().flush(PAYLOAD);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(noteBody().value).toBe('');

    const status = el().querySelector('.xtrim-message');
    expect(status?.getAttribute('role')).toBe('status');
    expect(status?.textContent).toContain('Nota agregada');
    // The room is reset: no error word lingers for the next note.
    expect(el().querySelector('.xtrim-error')).toBeNull();
  });

  it('keeps the word of the server when the note is refused', async () => {
    await arrive();

    typeNote('Hola');
    noteForm().dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    backend
      .expectOne('/api/admin/leads/5/notes')
      .flush(
        { message: 'No se pudo agregar la nota.' },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();
    fixture.detectChanges();

    const alert = el().querySelector('[role="alert"]');
    expect(alert?.textContent).toContain('No se pudo agregar la nota.');
  });
});
