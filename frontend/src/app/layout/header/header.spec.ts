import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { CartStore } from '@core/cart/cart-store';
import { StoragePort } from '@core/storage/storage-port';
import { Header } from './header';

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

describe('Header', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Header],
      providers: [provideRouter([]), { provide: StoragePort, useClass: MemoryStorage }],
    }).compileComponents();
  });

  it('says nothing about the cart while it is empty', async () => {
    const fixture = TestBed.createComponent(Header);
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.xtrim-cart__count')).toBeNull();
    expect(compiled.querySelector('.xtrim-cart__menu')).toBeNull();
    expect(compiled.querySelector('[aria-label="Carrito"]')).toBeTruthy();
  });

  it('says how many items it holds; the money waits in the dropdown', async () => {
    const cart = TestBed.inject(CartStore);
    cart.add({ itemId: 1, slug: 'p-1', name: 'Taladro', price: 59.99, image: null, stock: 4 });

    const fixture = TestBed.createComponent(Header);
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.xtrim-cart__count')?.textContent).toBe('1');
    const menu = compiled.querySelector('.xtrim-cart__menu');
    expect(menu?.textContent).toContain('$59.99');
    expect(menu?.textContent).toContain('Valor a pagar');
    expect(menu?.textContent).toContain('1 unidad');
  });

  it('grows with the cart, without reloading the page', async () => {
    const fixture = TestBed.createComponent(Header);
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    const count = compiled.querySelector('.xtrim-cart__count');
    expect(count).toBeNull();

    const cart = TestBed.inject(CartStore);
    cart.add({ itemId: 1, slug: 'p-1', name: 'Taladro', price: 59.99, image: null, stock: 4 });
    cart.add({ itemId: 2, slug: 'p-2', name: 'Amoladora', price: 40.0, image: null, stock: null });

    fixture.detectChanges();
    expect(compiled.querySelector('.xtrim-cart__count')?.textContent).toBe('2');
    expect(
      compiled.querySelector('[aria-label="Carrito: 2 unidades, total $99.99"]'),
    ).toBeTruthy();
  });

  it('counts units, not lines', async () => {
    const cart = TestBed.inject(CartStore);
    cart.add({ itemId: 1, slug: 'p-1', name: 'Taladro', price: 59.99, image: null, stock: 4 }, 3);

    const fixture = TestBed.createComponent(Header);
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.xtrim-cart__count')?.textContent).toBe('3');
    expect(compiled.querySelector('[aria-label="Carrito: 3 unidades, total $179.97"]')).toBeTruthy();
  });
});
