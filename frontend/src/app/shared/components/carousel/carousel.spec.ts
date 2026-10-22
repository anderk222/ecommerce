import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { CatalogItemSummary } from '@shared/models/catalog.models';
import { Carousel } from './carousel';

describe('Carousel', () => {
  const item = (id: number): CatalogItemSummary => ({
    id,
    slug: `item-${id}`,
    name: `Ítem ${id}`,
    type: 'PRODUCT',
    price: { basePrice: 10, finalPrice: 10, discountPercentage: 0, currency: 'USD' },
    image: null,
    featured: true,
    available: true,
    stock: 1,
    categories: [],
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Carousel],
      providers: [provideRouter([])],
    }).compileComponents();
  });

  it('starts on the first slide', async () => {
    const fixture = TestBed.createComponent(Carousel);
    fixture.componentRef.setInput('items', [item(1), item(2), item(3)]);
    await fixture.whenStable();

    expect(fixture.componentInstance.index()).toBe(0);
    expect(fixture.componentInstance.current()?.id).toBe(1);
  });

  it('advances and comes back around', async () => {
    const fixture = TestBed.createComponent(Carousel);
    fixture.componentRef.setInput('items', [item(1), item(2), item(3)]);
    await fixture.whenStable();

    fixture.componentInstance.next();
    expect(fixture.componentInstance.index()).toBe(1);

    fixture.componentInstance.next();
    fixture.componentInstance.next();
    expect(fixture.componentInstance.index()).toBe(0);
  });

  it('goes back', async () => {
    const fixture = TestBed.createComponent(Carousel);
    fixture.componentRef.setInput('items', [item(1), item(2), item(3)]);
    await fixture.whenStable();

    fixture.componentInstance.previous();
    expect(fixture.componentInstance.index()).toBe(2);
  });

  it('jumps to a slide', async () => {
    const fixture = TestBed.createComponent(Carousel);
    fixture.componentRef.setInput('items', [item(1), item(2), item(3)]);
    await fixture.whenStable();

    fixture.componentInstance.goTo(2);
    expect(fixture.componentInstance.index()).toBe(2);
  });

  it('links a service to its own route and a product to the other one', async () => {
    const fixture = TestBed.createComponent(Carousel);
    fixture.componentRef.setInput('items', [item(1)]);
    await fixture.whenStable();

    expect(fixture.componentInstance.linkOf(item(1))).toBe('/productos/item-1');
    expect(fixture.componentInstance.linkOf({ ...item(2), type: 'SERVICE' }))
      .toBe('/servicios/item-2');
  });

  it('does not rotate when there is a single slide', async () => {
    const fixture = TestBed.createComponent(Carousel);
    fixture.componentRef.setInput('items', [item(1)]);
    await fixture.whenStable();

    expect(fixture.componentInstance.hasSeveral()).toBe(false);
    fixture.componentInstance.next();
    expect(fixture.componentInstance.index()).toBe(0);
  });
});
