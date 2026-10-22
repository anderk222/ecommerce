import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter, Router, RouterOutlet } from '@angular/router';

import { API_BASE_URL } from '@core/api/api-base-url';
import { authInterceptor } from '@core/auth/auth.interceptor';
import { StoragePort } from '@core/storage/storage-port';
import { CatalogItemForm } from './catalog-item-form';

/** An empty shell that holds the outlet where the form is rendered. */
@Component({ imports: [RouterOutlet], template: '<router-outlet />' })
class Shell {}

/** The destination of a successful save — a stub so the router arrives somewhere. */
@Component({ template: '' })
class CatalogListStub {}

/** The storage of the tests: an honest memory, nothing more. */
class MemoryStorage extends StoragePort {
  private readonly map = new Map<string, string>();
  override get(key: string): string | null {
    return this.map.get(key) ?? null;
  }
  override set(key: string, value: string): void {
    this.map.set(key, value);
  }
  override remove(key: string): void {
    this.map.delete(key);
  }
}

describe('CatalogItemForm', () => {
  let fixture: ReturnType<typeof TestBed.createComponent<Shell>>;
  let backend: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Shell, CatalogItemForm],
      providers: [
        provideRouter([
          { path: 'admin/catalog/nuevo', component: CatalogItemForm },
          { path: 'admin/catalog/:id', component: CatalogItemForm },
          { path: 'admin/catalog', component: CatalogListStub },
        ]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: StoragePort, useClass: MemoryStorage },
        { provide: API_BASE_URL, useValue: '/api' },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Shell);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    backend.verify();
  });

  /** Arrives at the form of a new item, with the categories already answered.
   *  The arrangement of the resource (P51 D7): the change detection fires the
   *  read, the test answers it, and the value lands with the stability. */
  const arrive = async (): Promise<void> => {
    await TestBed.inject(Router).navigate(['/admin/catalog/nuevo']);
    fixture.detectChanges();
    backend.expectOne('/api/admin/categories').flush([]);
    await fixture.whenStable();
    fixture.detectChanges();
  };

  /** The rendered form, inside the shell. */
  const el = () => fixture.nativeElement as HTMLElement;

  /** The form element of the page. */
  const form = () => el().querySelector<HTMLFormElement>('form')!;

  it('arrives quiet: a pristine form shows no error word', async () => {
    await arrive();

    expect(el().querySelector('.xtrim-error')).toBeNull();
    expect(el().querySelector('[role="alert"]')).toBeNull();
  });

  it('says the contract under the name an administrator touched and left empty', async () => {
    await arrive();

    const name = form().querySelector<HTMLInputElement>('#item-name')!;
    name.value = '';
    name.dispatchEvent(new Event('input'));
    name.dispatchEvent(new Event('blur'));
    await fixture.whenStable();

    const error = el().querySelector('#item-name-error');
    expect(error?.textContent).toContain('Este campo es requerido');
    expect(name.getAttribute('aria-invalid')).toBe('true');
  });

  it('guards the numeric bounds: a negative price speaks, an empty stock stays quiet', async () => {
    await arrive();

    const price = form().querySelector<HTMLInputElement>('#item-price')!;
    price.value = '-5';
    price.dispatchEvent(new Event('input'));
    price.dispatchEvent(new Event('blur'));
    await fixture.whenStable();

    expect(el().querySelector('#item-price-error')?.textContent).toContain(
      'El precio no puede ser negativo',
    );
    // The empty stock (no stock control) owes no word.
    expect(el().querySelector('#item-stock-error')).toBeNull();
  });

  it('walks the administrator to the fix before pricing the request on an invalid submit', async () => {
    await arrive();

    form().dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    expect(el().querySelectorAll('.xtrim-error').length).toBeGreaterThan(0);
    const alert = el().querySelector('[role="alert"]');
    expect(alert?.textContent).toContain('Revisa los campos marcados antes de guardar.');
    // The door was never knocked: an empty form is no request at all (D1).
    expect(backend.match('/api/admin/catalog').length).toBe(0);
  });

  it('creates the item with the same payload as before when the form is complete', async () => {
    await arrive();

    const name = form().querySelector<HTMLInputElement>('#item-name')!;
    name.value = 'Router WiFi 6';
    name.dispatchEvent(new Event('input'));
    form().dispatchEvent(new Event('submit'));

    const request = backend.expectOne('/api/admin/catalog');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      type: 'PRODUCT',
      name: 'Router WiFi 6',
      description: '',
      price: 0,
      stock: null,
      images: [],
      specifications: [],
      categoryIds: [],
      featured: false,
    });
    request.flush({ id: 1, slug: 'router-wifi-6', name: 'Router WiFi 6' });
    await fixture.whenStable();

    expect(TestBed.inject(Router).url).toBe('/admin/catalog');
  });

  it('loads the item back into the fields when the form edits', async () => {
    await TestBed.inject(Router).navigate(['/admin/catalog/7']);
    fixture.detectChanges();
    backend.expectOne('/api/admin/categories').flush([]);
    backend.expectOne('/api/admin/catalog/7').flush({
      id: 7,
      slug: 'router-wifi-6',
      name: 'Router WiFi 6',
      type: 'PRODUCT',
      description: 'Un router',
      basePrice: 120.5,
      stock: 5,
      images: ['https://example.com/a.jpg'],
      specifications: [{ name: 'Marca', value: 'X' }],
      categoryIds: [1],
      featured: true,
    });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(form().querySelector<HTMLInputElement>('#item-name')!.value).toBe('Router WiFi 6');
    expect(form().querySelector<HTMLInputElement>('#item-price')!.value).toBe('120.5');
    expect(form().querySelector<HTMLInputElement>('#item-stock')!.value).toBe('5');
    expect(form().querySelector<HTMLTextAreaElement>('#item-images')!.value).toBe(
      'https://example.com/a.jpg',
    );
  });

  it('asks for nothing in the create mode: without an item there is no read (P51 D1)', async () => {
    await arrive();

    // The read of the item waits for an identifier: in the create mode the
    // resource stays idle and no request flies.
    expect(backend.match('/api/admin/catalog/7').length).toBe(0);
    expect(backend.match((request) => request.method === 'GET' && request.url === '/api/admin/catalog').length).toBe(0);
  });

  it('keeps the edits of the administrator when the item is read again (P51 D2)', async () => {
    await TestBed.inject(Router).navigate(['/admin/catalog/7']);
    fixture.detectChanges();
    backend.expectOne('/api/admin/categories').flush([]);
    backend.expectOne('/api/admin/catalog/7').flush({
      id: 7,
      slug: 'router-wifi-6',
      name: 'Router WiFi 6',
      type: 'PRODUCT',
      description: 'Un router',
      basePrice: 120.5,
      stock: 5,
      images: [],
      specifications: [],
      categoryIds: [],
      featured: false,
    });
    await fixture.whenStable();
    fixture.detectChanges();

    // The administrator edits the name…
    const name = form().querySelector<HTMLInputElement>('#item-name')!;
    name.value = 'Router WiFi 7';
    name.dispatchEvent(new Event('input'));
    await fixture.whenStable();

    // …and the re-read does not clobber the edit: the fill happens once.
    const formComponent = fixture.debugElement.query(By.directive(CatalogItemForm))
      .componentInstance as CatalogItemForm;
    formComponent.item.reload();
    fixture.detectChanges();
    backend.expectOne('/api/admin/catalog/7').flush({
      id: 7,
      slug: 'router-wifi-6',
      name: 'Router WiFi 6',
      type: 'PRODUCT',
      description: 'Un router',
      basePrice: 120.5,
      stock: 5,
      images: [],
      specifications: [],
      categoryIds: [],
      featured: false,
    });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(form().querySelector<HTMLInputElement>('#item-name')!.value).toBe('Router WiFi 7');
  });
});
