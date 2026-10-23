import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { API_BASE_URL } from '@core/api/api-base-url';
import { CartStore } from '@core/cart/cart-store';
import { WHATSAPP_NUMBER } from '@core/config/whatsapp-number';
import { SeoService } from '@core/seo/seo.service';
import { Tracker } from '@core/tracking/tracker.service';
import { AnonymousId } from '@core/tracking/anonymous-id';
import { CatalogItemDetail } from '@shared/models/catalog.models';
import { Detail } from './detail';

/** The product out of stock: the only state the notice exists for. */
const OUT_OF_STOCK: CatalogItemDetail = {
  id: 1,
  slug: 'termo-alpina',
  name: 'Térmico Alpina',
  type: 'PRODUCT',
  description: 'Un termo de acero que guarda el calor del viaje entero.',
  price: { basePrice: 100, finalPrice: 80, discountPercentage: 20, currency: 'USD' },
  images: ['https://example.test/termo.png'],
  specifications: [],
  categories: ['Tecnología'],
  featured: true,
  available: true,
  stock: 0,
  stockState: 'OUT_OF_STOCK',
};

/** The same product with stock: the page that never pays for the notice. */
const withStock = (): CatalogItemDetail => ({
  ...OUT_OF_STOCK,
  stock: 4,
  stockState: 'IN_STOCK',
});

/** The product with a single unit left: the guard of the cart. */
const withOneLeft = (): CatalogItemDetail => ({
  ...OUT_OF_STOCK,
  stock: 1,
  stockState: 'LOW_STOCK',
});

/** The stock of the cart, as the page asks it. */
interface CartLike {
  quantityOf: (itemId: number) => number;
  add: () => void;
}

describe('Detail — the lazy notice of the stock (HU-08)', () => {
  /** Boots the page against the item given, with every service spoken for. */
  const boot = async (
    item: CatalogItemDetail,
    cart: CartLike = { quantityOf: () => 0, add: () => undefined },
  ): Promise<ReturnType<typeof TestBed.createComponent<Detail>>> => {
    const params = convertToParamMap({ slug: item.slug });
    await TestBed.configureTestingModule({
      imports: [Detail],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: { paramMap: of(params), snapshot: { paramMap: params } },
        },
        provideHttpClient(),
        provideHttpClientTesting(),
        // The chunk of the notice is lazy: when it lands, StockNotify makes
        // its own LeadApi, which needs the base url of the API.
        { provide: API_BASE_URL, useValue: 'https://api.test' },
        { provide: SeoService, useValue: { setPage: () => undefined } },
        { provide: Tracker, useValue: { track: () => undefined } },
        { provide: AnonymousId, useValue: { get: () => 'anon-test-id' } },
        {
          provide: CartStore,
          useValue: cart,
        },
        { provide: WHATSAPP_NUMBER, useValue: '593999999999' },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(Detail);
    // The arrangement of the resource (P51 D7): the change detection fires
    // the read, the test answers it, and the value lands with the stability.
    fixture.detectChanges();
    TestBed.inject(HttpTestingController)
      .expectOne((request) => request.url === `https://api.test/public/catalog/${item.slug}`)
      .flush(item);
    await fixture.whenStable();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  };

  it('does not bring the notice in while the item has stock', async () => {
    const fixture = await boot(withStock());

    const band = (fixture.nativeElement as HTMLElement).querySelector('.xtrim-detail__notify-band');
    expect(band).not.toBeNull();
    // The @defer has not fired: the form never joins the page with stock.
    expect(band?.querySelector('app-stock-notify')).toBeNull();
  });

  it('loads the notice through the @defer once the stock is gone', async () => {
    const fixture = await boot(OUT_OF_STOCK);

    const notice = (fixture.nativeElement as HTMLElement).querySelector('app-stock-notify');
    expect(notice).not.toBeNull();
    expect(notice?.querySelector('.xtrim-detail__notify')?.textContent).toContain(
      'Notificarme cuando exista stock',
    );
  });

  it('binds the item to the deferred form so it can pitch the promise', async () => {
    const fixture = await boot(OUT_OF_STOCK);

    const notice = (fixture.nativeElement as HTMLElement).querySelector('app-stock-notify')!;
    expect(notice.querySelector<HTMLInputElement>('input#notify-name')).not.toBeNull();
    expect(notice.querySelector<HTMLInputElement>('input#notify-email')).not.toBeNull();
  });

  /** The primary call of the page: where the stock guard speaks. */
  const addButtonOf = (fixture: Awaited<ReturnType<typeof boot>>): HTMLButtonElement =>
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>(
      '.xtrim-detail__actions .ui.primary.button',
    )!;

  it('adds the last unit and refuses the next one with a word', async () => {
    let inCart = 0;
    const add = vi.fn(() => {
      inCart += 1;
    });
    const fixture = await boot(withOneLeft(), { quantityOf: () => inCart, add });

    addButtonOf(fixture).click();
    fixture.detectChanges();
    expect(add).toHaveBeenCalledTimes(1);

    addButtonOf(fixture).click();
    fixture.detectChanges();
    expect(add).toHaveBeenCalledTimes(1);

    const message = (fixture.nativeElement as HTMLElement).querySelector('.xtrim-message');
    expect(message?.textContent).toContain('todo el stock de «Térmico Alpina»: 1 unidad');
    expect(message?.classList).toContain('is-error');
  });

  it('never adds more than the stock, whatever its size', async () => {
    let inCart = 0;
    const add = vi.fn(() => {
      inCart += 1;
    });
    const fixture = await boot(
      { ...OUT_OF_STOCK, stock: 10, stockState: 'IN_STOCK' },
      { quantityOf: () => inCart, add },
    );

    // The eleventh click meets the same guard as the second one on a stock of one.
    for (let click = 0; click < 11; click += 1) {
      addButtonOf(fixture).click();
      fixture.detectChanges();
    }

    expect(add).toHaveBeenCalledTimes(10);
    const message = (fixture.nativeElement as HTMLElement).querySelector('.xtrim-message');
    expect(message?.textContent).toContain('todo el stock de «Térmico Alpina»: 10 unidades');
    expect(message?.classList).toContain('is-error');
  });

  it('refuses an item without stock and says why', async () => {
    const add = vi.fn();
    const fixture = await boot(OUT_OF_STOCK, { quantityOf: () => 0, add });

    addButtonOf(fixture).click();
    fixture.detectChanges();

    expect(add).not.toHaveBeenCalled();
    const message = (fixture.nativeElement as HTMLElement).querySelector('.xtrim-message');
    expect(message?.textContent).toContain('«Térmico Alpina» no tiene stock disponible');
    expect(message?.getAttribute('role')).toBe('alert');
  });
});
