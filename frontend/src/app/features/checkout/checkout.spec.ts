import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { API_BASE_URL } from '@core/api/api-base-url';
import { CartStore } from '@core/cart/cart-store';
import { StoragePort } from '@core/storage/storage-port';
import { Checkout } from './checkout';

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

const A_LINE = {
  itemId: 7,
  slug: 'plan-fibra-300',
  name: 'Plan Fibra 300',
  price: 39.9,
  image: null,
  stock: 5,
};

describe('Checkout', () => {
  let fixture: ReturnType<typeof TestBed.createComponent<Checkout>>;
  let component: Checkout;
  let backend: HttpTestingController;
  let cart: CartStore;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Checkout],
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([])),
        provideHttpClientTesting(),
        { provide: StoragePort, useClass: MemoryStorage },
        { provide: API_BASE_URL, useValue: '/api' },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Checkout);
    component = fixture.componentInstance;
    backend = TestBed.inject(HttpTestingController);
    cart = TestBed.inject(CartStore);
    cart.add(A_LINE, 2);
    fixture.autoDetectChanges();
  });

  afterEach(() => {
    backend.verify();
  });

  /** The rendered checkout. */
  const el = () => fixture.nativeElement as HTMLElement;

  /** Writes into an input the way a visitor would. */
  const write = (input: HTMLInputElement, value: string): void => {
    input.value = value;
    input.dispatchEvent(new Event('input'));
  };

  /** Ticks the consent checkbox the way a visitor would. */
  const consent = (): void => {
    const box = el().querySelector<HTMLInputElement>('#checkout-consent')!;
    box.click();
  };

  /** Leaves an input empty after holding it. */
  const touchEmpty = (selector: string): void => {
    const input = el().querySelector<HTMLInputElement>(selector)!;
    input.value = '';
    input.dispatchEvent(new Event('input'));
    input.dispatchEvent(new Event('blur'));
  };

  it('arrives quiet: a pristine form shows no error word', () => {
    expect(el().querySelectorAll('.xtrim-error').length).toBe(0);
    expect(el().querySelector('[role="alert"]')).toBeNull();
  });

  it('says the contract under an input the visitor touched and left empty', async () => {
    touchEmpty('#checkout-name');
    await fixture.whenStable();

    const error = el().querySelector('#checkout-name-error');
    expect(error?.textContent).toContain('Este campo es requerido');
    expect(el().querySelector<HTMLInputElement>('#checkout-name')?.getAttribute('aria-invalid')).toBe(
      'true',
    );
  });

  it('asks for a real email when the visitor typed a malformed one', async () => {
    write(el().querySelector<HTMLInputElement>('#checkout-email')!, 'not-an-email');
    el().querySelector<HTMLInputElement>('#checkout-email')!.dispatchEvent(new Event('blur'));
    await fixture.whenStable();

    expect(el().querySelector('#checkout-email-error')?.textContent).toContain(
      'Ingresa un correo válido',
    );
  });

  it('walks the visitor to the fix before pricing the order on an invalid submit', async () => {
    consent();
    el().querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    const errors = el().querySelectorAll('.xtrim-error');
    expect(errors.length).toBeGreaterThanOrEqual(2);
    expect(el().querySelector('[role="alert"]')?.textContent).toContain(
      'Revisa los campos marcados antes de confirmar tu compra.',
    );
    // The order was never priced: an unusable form is no request at all (P42 D1).
    expect(backend.match('/api/public/orders').length).toBe(0);
  });

  it('keeps the sentence of the consent when the visitor buys without it', async () => {
    touchEmpty('#checkout-name');
    write(el().querySelector<HTMLInputElement>('#checkout-email')!, 'ana@xtrim.ec');
    await fixture.whenStable();

    // The consent box stays unchecked: the purchase was blocked with verbatim words (P42 D6).
    expect(el().querySelector('.ui.primary[type="submit"]')?.getAttribute('disabled')).toBe('');
    expect(el().querySelector('[role="alert"]')).toBeNull();
  });

  it('records the order with the trimmed name and email, and clears the cart', async () => {
    write(el().querySelector<HTMLInputElement>('#checkout-name')!, '  Ana Torres  ');
    write(el().querySelector<HTMLInputElement>('#checkout-email')!, '  ana@xtrim.ec  ');
    write(el().querySelector<HTMLInputElement>('#checkout-phone')!, ' 0991234567 ');
    consent();
    el().querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));

    const request = backend.expectOne('/api/public/orders');
    expect(request.request.body).toEqual({
      anonymousId: expect.any(String),
      customer: {
        name: 'Ana Torres',
        email: 'ana@xtrim.ec',
        phone: '0991234567',
        province: 'PICHINCHA',
        consent: true,
      },
      items: [{ itemId: 7, quantity: 2 }],
    });
    request.flush({
      orderId: 12,
      leadId: 3,
      total: 79.8,
      currency: 'USD',
      lines: [{ itemName: 'Plan Fibra 300', quantity: 2, unitPrice: 39.9, lineTotal: 79.8 }],
    });
    await fixture.whenStable();

    expect(el().textContent).toContain('¡Gracias por tu compra!');
    expect(el().textContent).toContain('#12');
    expect(cart.lines().length).toBe(0);
  });

  it('keeps the word of the server when the purchase is refused', async () => {
    write(el().querySelector<HTMLInputElement>('#checkout-name')!, 'Ana Torres');
    write(el().querySelector<HTMLInputElement>('#checkout-email')!, 'ana@xtrim.ec');
    consent();
    el().querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));

    backend
      .expectOne('/api/public/orders')
      .flush(
        { code: 'CONFLICT', message: 'Stock insuficiente para Plan Fibra 300' },
        { status: 409, statusText: 'Conflict' },
      );
    await fixture.whenStable();

    expect(el().querySelector('[role="alert"]')?.textContent).toContain(
      'Stock insuficiente para Plan Fibra 300',
    );
    expect(cart.lines().length).toBe(1);
  });
});
