import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { CatalogItemSummary } from '@shared/models/catalog.models';
import { ItemCard } from './item-card';

describe('ItemCard', () => {
  const product: CatalogItemSummary = {
    id: 1,
    slug: 'router-wifi-6',
    name: 'Router WiFi 6',
    type: 'PRODUCT',
    price: { basePrice: 100, finalPrice: 80, discountPercentage: 20, currency: 'USD' },
    image: 'https://example.test/router.png',
    featured: true,
    available: true,
    stock: 4,
    categories: ['Tecnología'],
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ItemCard],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('links to the detail of the item, by its kind', async () => {
    const fixture = TestBed.createComponent(ItemCard);
    fixture.componentRef.setInput('item', product);
    await fixture.whenStable();

    expect(fixture.componentInstance.link()).toBe('/productos/router-wifi-6');
  });

  it('strikes the original price through when there is a discount', async () => {
    const fixture = TestBed.createComponent(ItemCard);
    fixture.componentRef.setInput('item', product);
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('.xtrim-price-original')?.textContent).toContain('100');
    expect(compiled.querySelector('.xtrim-price-final')?.textContent).toContain('80');
  });

  it('marks the items without stock', async () => {
    const fixture = TestBed.createComponent(ItemCard);
    fixture.componentRef.setInput('item', { ...product, stock: 0 });
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('Sin stock');
  });

  it('shows the image with its size and without blocking the page (HU-30)', async () => {
    const fixture = TestBed.createComponent(ItemCard);
    fixture.componentRef.setInput('item', product);
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    const image = compiled.querySelector('img');
    expect(image?.getAttribute('src')).toBe('https://example.test/router.png');
    expect(image?.getAttribute('alt')).toBe('Router WiFi 6');
    // The dimensions avoid the jump of the layout, and the image is lazy.
    expect(image?.getAttribute('width')).toBe('800');
    expect(image?.getAttribute('height')).toBe('600');
    expect(image?.getAttribute('loading')).toBe('lazy');
  });
});
