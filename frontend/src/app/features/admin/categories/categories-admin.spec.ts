import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_BASE_URL } from '@core/api/api-base-url';
import { CategoriesAdmin } from './categories-admin';

/** A ledger to answer the panel with. */
const ledger = [{ id: 1, name: 'Deportes', slug: 'deportes', itemCount: 2 }];

describe('CategoriesAdmin', () => {
  let fixture: ReturnType<typeof TestBed.createComponent<CategoriesAdmin>>;
  let backend: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CategoriesAdmin],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: API_BASE_URL, useValue: '/api' },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CategoriesAdmin);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    backend.verify();
  });

  /** Arrives at the panel and answers the read of the ledger. The
   *  arrangement of the resource (P51 D7): the change detection fires the
   *  read, the test answers it, and the value lands with the stability. */
  const arrive = async (): Promise<void> => {
    fixture.detectChanges();
    backend.expectOne('/api/admin/categories').flush(ledger);
    await fixture.whenStable();
    fixture.detectChanges();
  };

  /** The rendered panel. */
  const el = () => fixture.nativeElement as HTMLElement;

  /** The name input of the creation segment. */
  const nameInput = () => el().querySelector<HTMLInputElement>('#new-category')!;

  /** Writes a value into the input the way a visitor does. */
  const type = (selector: HTMLInputElement, value: string): void => {
    selector.value = value;
    selector.dispatchEvent(new Event('input'));
  };

  it('arrives quiet: a pristine form shows no error word', async () => {
    await arrive();

    expect(el().querySelector('.xtrim-error')).toBeNull();
    expect(el().querySelector('[role="alert"]')).toBeNull();
  });

  it('says the contract under the input that was touched and left empty', async () => {
    await arrive();

    type(nameInput(), '');
    nameInput().dispatchEvent(new Event('blur'));
    await fixture.whenStable();

    const error = el().querySelector('#new-category-error');
    expect(error?.textContent).toContain('Este campo es requerido');
    expect(nameInput().getAttribute('aria-invalid')).toBe('true');
  });

  it('walks the staff member to the fix on an empty create — and never knocks', async () => {
    await arrive();

    el().querySelector<HTMLFormElement>('form.ui.form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    expect(el().querySelectorAll('.xtrim-error').length).toBe(1);
    const alert = el().querySelector('[role="alert"]');
    expect(alert?.textContent).toContain('Revisa los campos marcados antes de crear.');
    // The attempt exists, but no request was made: an empty name is no create at all (P40 D1).
    expect(backend.match((request) => request.method === 'POST' || request.method === 'PUT').length).toBe(0);
  });

  it('creates with the trimmed name, clears the field and reads the ledger again', async () => {
    await arrive();

    type(nameInput(), '  Cables  ');
    el().querySelector<HTMLFormElement>('form.ui.form')!.dispatchEvent(new Event('submit'));

    const request = backend.expectOne('/api/admin/categories');
    expect(request.request.body).toEqual({ name: 'Cables' });
    request.flush({ id: 2, name: 'Cables', slug: 'cables', itemCount: 0 });

    // The success re-reads the ledger: the reload's own read answers too.
    fixture.detectChanges();
    backend.expectOne('/api/admin/categories').flush(ledger);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(nameInput().value).toBe('');
    const status = el().querySelector('.xtrim-message');
    expect(status?.getAttribute('role')).toBe('status');
    expect(status?.textContent).toContain('Categoría creada');
  });

  it('keeps the word of the server when the creation is refused', async () => {
    await arrive();

    type(nameInput(), 'Deportes');
    el().querySelector<HTMLFormElement>('form.ui.form')!.dispatchEvent(new Event('submit'));

    backend
      .expectOne('/api/admin/categories')
      .flush({ code: 'CONFLICT', message: 'La categoría «Deportes» ya existe' }, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();

    const alert = el().querySelector('[role="alert"]');
    expect(alert?.textContent).toContain('La categoría «Deportes» ya existe');
  });

  it('renames from the ledger as it always did', async () => {
    await arrive();

    el().querySelector<HTMLButtonElement>('button.ui.small.button')!.click();
    await fixture.whenStable();

    const input = el().querySelector<HTMLInputElement>('.xtrim-ledger__input')!;
    expect(input.value).toBe('Deportes');
    type(input, 'Deportes exteriores ');
    el().querySelector<HTMLButtonElement>('button.ui.primary.small.button')!.click();

    const request = backend.expectOne('/api/admin/categories/1', 'PUT');
    expect(request.request.body).toEqual({ name: 'Deportes exteriores' });
    request.flush({ id: 1, name: 'Deportes exteriores', slug: 'deportes', itemCount: 2 });

    // The rename re-reads the ledger: the reload's own read answers too.
    fixture.detectChanges();
    backend.expectOne('/api/admin/categories').flush(ledger);
    await fixture.whenStable();

    expect(el().querySelectorAll('.xtrim-ledger__input').length).toBe(0);
  });
});
