import { Component, computed, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { CatalogApi } from '@features/catalog/catalog-api';
import { CartStore } from '@core/cart/cart-store';
import { WHATSAPP_NUMBER } from '@core/config/whatsapp-number';
import { SeoService } from '@core/seo/seo.service';
import { Tracker } from '@core/tracking/tracker.service';
import { CatalogItemDetail } from '@shared/models/catalog.models';
import { Loading } from '@shared/components/loading/loading';
import { StockNotify } from '@features/catalog/detail/stock-notify/stock-notify';

/**
 * The detail of an item.
 *
 * <p>HU-05: the gallery, the specifications, the price with the original
 * struck through, the categories and the four calls to action — add to the
 * cart, buy now, ask for information and contact by WhatsApp.
 *
 * <p>The page is rendered on the server, so the crawler sees the name, the
 * description and the price of the item in the HTML it receives.
 *
 * <p>The notice of the stock (HU-08) is a lazy chunk: the {@code @defer} in
 * the template loads {@link StockNotify} with a dynamic import only when the
 * item is out of stock, so a page with stock never pays for the form it will
 * never open. It stays declared here — being referenced only inside the
 * {@code @defer} block is exactly what lets the compiler split it out.
 *
 * <p>The item answers in signals (P51): the page holds the resource, the slug
 * of the route is its source, and the template speaks the three states — the
 * data, the refusal and the wait.
 */
@Component({
  selector: 'app-detail',
  imports: [FormsModule, RouterLink, Loading, StockNotify],
  templateUrl: './detail.html',
  styleUrl: './detail.scss',
})
export class Detail {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly catalogApi = inject(CatalogApi);
  private readonly seo = inject(SeoService);
  private readonly cart = inject(CartStore);
  private readonly whatsappNumber = inject(WHATSAPP_NUMBER);
  private readonly tracker = inject(Tracker);

  /** The slug of the item, from the route. */
  private readonly slug = toSignal(this.route.paramMap, {
    initialValue: this.route.snapshot.paramMap,
  });

  /** The item, once it has arrived. */
  readonly item = this.catalogApi.detail(computed(() => this.slug().get('slug') ?? ''));

  /** The message of the last operation (P26): the stock refuses with a word. */
  readonly message = signal<string | null>(null);

  /** Whether the last operation was refused. */
  readonly failed = signal(false);

  /** The refusal of the read, in the same voice as the writes (P51 D4): the
   *  one box of the screen speaks both, and the word of the operation keeps
   *  the priority. */
  protected readonly readError = computed(() => {
    const error = this.item.error();
    return error ? this.messageOf(error) : null;
  });

  /** The page that sent the visitor here: the `from` state of the navigation. */
  readonly returnFrom: string | null =
    (this.router.currentNavigation ()?.extras?.state?.['from'] as string | undefined) ?? null;

  /** The way back: the page that sent the visitor, or the canonical listing
   *  of the item's type. */
  backHref(type: 'PRODUCT' | 'SERVICE'): string {
    return this.returnFrom ?? (type === 'SERVICE' ? '/servicios' : '/productos');
  }

  /** The link's word: where it returns to. */
  backLabel(type: 'PRODUCT' | 'SERVICE'): string {
    if (this.returnFrom) {
      return this.returnFrom === '/carrito' ? 'Volver al carrito' : 'Volver';
    }
    return type === 'SERVICE' ? 'Volver a Servicios' : 'Volver a Productos';
  }

  /** The image that is being shown in the gallery. */
  readonly selectedImage = signal<string | null>(null);

  /** Whether the price is discounted. */
  readonly discounted = computed(() => {
    if (!this.item.hasValue()) {
      return false;
    }
    return this.item.value().price.discountPercentage > 0;
  });

  /** Whether the item can be bought. */
  readonly purchasable = computed(() => {
    if (!this.item.hasValue()) {
      return false;
    }
    return this.item.value().available === true;
  });

  /** The link of WhatsApp, with the message already written. */
  readonly whatsappLink = computed(() => {
    const item = this.item.hasValue() ? this.item.value() : null;
    const text = item
      ? `Hola, me interesa el producto "${item.name}" (${item.price.currency} ${item.price.finalPrice}). ¿Está disponible?`
      : 'Hola, me interesa un producto de su catálogo.';
    return `https://wa.me/${this.whatsappNumber}?text=${encodeURIComponent(text)}`;
  });

  constructor() {
    effect(() => {
      if (!this.item.hasValue()) {
        return;
      }
      const item = this.item.value();
      this.selectedImage.set(item.images[0] ?? null);
      // Opening the detail is an event of the timeline (HU-09).
      this.tracker.track('PRODUCT_VIEW', 'CATALOG_ITEM', item.id, { slug: item.slug });
      this.seo.setPage({
        title: `${item.name} — Xtrim`,
        description: item.description.slice(0, 160),
        path: item.type === 'SERVICE' ? `/servicios/${item.slug}` : `/productos/${item.slug}`,
        image: item.images[0] ?? null,
        jsonLd: this.jsonLdOf(item),
      });
    });
  }

  /** Shows another image of the gallery. */
  select(image: string): void {
    this.selectedImage.set(image);
  }

  /** Adds the item to the cart. */
  addToCart(): void {
    this.addCurrentItemToCart('BUTTON');
  }

  /** Buys now: if the item is not a line of the cart yet, it joins first —
   *  and then the visitor is taken to the cart either way. The decision and
   *  the checkout stay one gesture, and a second click never doubles the
   *  quantity: an item that is already in the cart is met, not added again.
   *  When the stock refuses the line, the cart is not opened on a purchase
   *  that could not happen. */
  buyNow(): void {
    if (!this.item.hasValue()) {
      return;
    }
    const item = this.item.value();
    if (this.cart.quantityOf(item.id) === 0 && !this.addCurrentItemToCart('BUY_NOW')) {
      return;
    }
    void this.router.navigate(['/carrito']);
  }

  /**
   * Adds the item of the page to the cart.
   *
   * <p>The guard is the button's own: an item that is not available, or whose
   * stock is already in the cart, never joins it again. The refusal is said
   * out loud — a button that swallows the click teaches nothing. The event
   * keeps the truth of what happened — the item was added — and the metadata
   * records from which of the two calls it came, so the CRM can tell them
   * apart without a new event type.
   *
   * @returns whether the line was added
   */
  private addCurrentItemToCart(via: 'BUTTON' | 'BUY_NOW'): boolean {
    if (!this.item.hasValue()) {
      return false;
    }
    const item = this.item.value();
    if (item.stock === 0) {
      this.report(`«${item.name}» no tiene stock disponible`, true);
      return false;
    }
    if (!item.available) {
      this.report(`«${item.name}» no está disponible`, true);
      return false;
    }

    const inCart = this.cart.quantityOf(item.id);
    if (item.stock !== null && inCart >= item.stock) {
      this.report(
        `Ya tienes en el carrito todo el stock de «${item.name}»: ${this.stockLabel(item.stock)}`,
        true,
      );
      return false;
    }

    this.cart.add({
      itemId: item.id,
      slug: item.slug,
      name: item.name,
      price: item.price.finalPrice,
      image: item.images[0] ?? null,
      stock: item.stock,
    });
    this.report(null, false);
    this.tracker.track('ADD_TO_CART', 'CATALOG_ITEM', item.id, { quantity: 1, via });
    return true;
  }

  /** The stock written with its noun: one unit, or however many. */
  private stockLabel(stock: number): string {
    return stock === 1 ? '1 unidad' : `${stock} unidades`;
  }

  /** Writes the message of the operation, with the voice of its outcome. */
  private report(text: string | null, failed: boolean): void {
    this.message.set(text);
    this.failed.set(failed);
  }

  /** The word of a failed read: the server's, when it came with one. */
  private messageOf(error: unknown): string {
    const message = (error as { error?: { message?: string } } | null)?.error?.message;
    return message ?? 'No pudimos cargar el ítem.';
  }

  /** The structured data of the item (HU-27). */
  private jsonLdOf(item: CatalogItemDetail): Record<string, unknown> {
    const isService = item.type === 'SERVICE';
    return {
      '@context': 'https://schema.org',
      '@type': isService ? 'Service' : 'Product',
      name: item.name,
      description: item.description,
      image: item.images[0] ?? undefined,
      sku: item.slug,
      category: item.categories.join(', ') || undefined,
      offers: {
        '@type': 'Offer',
        price: item.price.finalPrice,
        priceCurrency: item.price.currency,
        availability:
          item.stock === 0
            ? 'https://schema.org/OutOfStock'
            : 'https://schema.org/InStock',
      },
    };
  }
}
