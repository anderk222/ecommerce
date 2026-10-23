import { Service, computed, inject, signal } from '@angular/core';

import { StoragePort } from '@core/storage/storage-port';

/**
 * One line of the cart.
 *
 * @param itemId the identifier of the item
 * @param slug the slug of the item
 * @param name the name of the item, kept so the cart can be shown without
 *        asking the API again
 * @param price the price the customer pays, at the moment it was added
 * @param image the first image of the item
 * @param quantity how many units
 * @param stock the stock of the item when it was added, null when the stock is
 *        not tracked
 */
export interface CartLine {
  itemId: number;
  slug: string;
  name: string;
  price: number;
  image: string | null;
  quantity: number;
  stock: number | null;
}

/**
 * The cart of the visitor.
 *
 * <p>It lives in the browser's storage: the visitor never registers, and the
 * cart survives a reload (HU-06). The price is kept as it was when the item
 * was added — the server owns the price of the purchase, which is calculated
 * again at the checkout.
 */
@Service()
export class CartStore {
  private static readonly KEY = 'xtrim.cart';

  private readonly storage = inject(StoragePort);

  private readonly state = signal<CartLine[]>(this.read());

  /** The lines of the cart. */
  readonly lines = this.state.asReadonly();

  /** How many units the cart holds. */
  readonly count = computed(() =>
    this.state().reduce((total, line) => total + line.quantity, 0),
  );

  /** The sum of the lines, at the prices they were added with. */
  readonly total = computed(() =>
    this.state().reduce((total, line) => total + line.price * line.quantity, 0),
  );

  /**
   * Adds an item to the cart.
   *
   * <p>It never throws: a cart that is full must not break the page. The
   * quantity never goes above the stock that was known when the item was
   * added — the components still speak their own refusal, and this cap keeps
   * the invariant even when a caller forgets to ask.
   */
  add(line: Omit<CartLine, 'quantity'>, quantity = 1): void {
    if (quantity <= 0) {
      return;
    }
    const lines = [...this.state()];
    const existing = lines.find((candidate) => candidate.itemId === line.itemId);
    if (existing) {
      const stock = line.stock ?? existing.stock;
      if (stock !== null && existing.quantity >= stock) {
        return;
      }
      existing.quantity =
        stock === null ? existing.quantity + quantity : Math.min(existing.quantity + quantity, stock);
      existing.stock = stock;
    } else {
      const stock = line.stock;
      if (stock !== null && stock <= 0) {
        return;
      }
      lines.push({
        ...line,
        quantity: stock === null ? quantity : Math.min(quantity, stock),
      });
    }
    this.write(lines);
  }

  /** Changes the quantity of a line. A quantity of zero removes the line. */
  setQuantity(itemId: number, quantity: number): void {
    const lines = this.state()
      .map((line) =>
        line.itemId === itemId ? { ...line, quantity: Math.max(0, quantity) } : line,
      )
      .filter((line) => line.quantity > 0);
    this.write(lines);
  }

  /** Removes a line. */
  remove(itemId: number): void {
    this.write(this.state().filter((line) => line.itemId !== itemId));
  }

  /** Empties the cart. */
  clear(): void {
    this.write([]);
  }

  /** The quantity of an item that is already in the cart. */
  quantityOf(itemId: number): number {
    return this.state().find((line) => line.itemId === itemId)?.quantity ?? 0;
  }

  private read(): CartLine[] {
    const raw = this.storage.get(CartStore.KEY);
    if (!raw) {
      return [];
    }
    try {
      const parsed = JSON.parse(raw) as CartLine[];
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }

  private write(lines: CartLine[]): void {
    this.state.set(lines);
    this.storage.set(CartStore.KEY, JSON.stringify(lines));
  }
}
