import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CartLine, CartStore } from '@core/cart/cart-store';
import { Tracker } from '@core/tracking/tracker.service';

/**
 * The cart of the visitor (HU-06).
 *
 * <p>It lives in the storage of the browser: the visitor never registers.
 * The quantities are guarded against the stock that was known when the item
 * was added — the server checks it again at the checkout, which is the one
 * that counts.
 */
@Component({
  selector: 'app-cart',
  imports: [RouterLink],
  templateUrl: './cart.html',
  styleUrl: './cart.scss',
})
export class Cart {
  private readonly cart = inject(CartStore);
  private readonly tracker = inject(Tracker);

  /** The lines of the cart. */
  readonly lines = this.cart.lines;

  /** How many units the cart holds. */
  readonly count = this.cart.count;

  /** The sum of the lines. */
  readonly total = this.cart.total;

  /** The message of the last operation. */
  readonly message = signal<string | null>(null);

  /** Whether the last operation was refused (the guard of the stock). */
  readonly messageFailed = signal(false);

  /** Whether the cart is empty. */
  readonly empty = computed(() => this.lines().length === 0);

  /**
   * Changes the quantity of a line.
   *
   * <p>The quantity never goes above the stock that was known when the item
   * was added.
   */
  setQuantity(line: CartLine, quantity: number): void {
    if (line.stock !== null && quantity > line.stock) {
      this.report(`Solo quedan ${line.stock} unidades de «${line.name}»`, true);
      return;
    }
    if (quantity <= 0) {
      this.remove(line);
      return;
    }
    this.report(null, false);
    this.cart.setQuantity(line.itemId, quantity);
  }

  /** Removes a line. */
  remove(line: CartLine): void {
    this.cart.remove(line.itemId);
    this.tracker.track('REMOVE_FROM_CART', 'CATALOG_ITEM', line.itemId);
    this.report(`«${line.name}» salió del carrito`, false);
  }

  /** Empties the cart. */
  clear(): void {
    this.cart.clear();
    this.report('El carrito quedó vacío', false);
  }

  /** Writes the message of an operation, with the voice of its outcome. */
  private report(text: string | null, failed: boolean): void {
    this.message.set(text);
    this.messageFailed.set(failed);
  }

  /** The link of the detail of a line. */
  linkOf(line: CartLine): string {
    return line.slug.startsWith('servicio') ? `/servicios/${line.slug}` : `/productos/${line.slug}`;
  }
}
