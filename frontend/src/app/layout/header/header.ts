import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { CartStore } from '@core/cart/cart-store';

/**
 * The header of the site: the menu, the search bar and the link to the cart.
 */
@Component({
  selector: 'app-header',
  imports: [FormsModule, RouterLink],
  templateUrl: './header.html',
  styleUrl: './header.scss',
})
export class Header {
  private readonly router = inject(Router);

  /** The cart of the visitor: its place in the masthead carries the sum. */
  readonly cart = inject(CartStore);

  /** The text of the search bar. */
  readonly query = signal('');

  /** What the cart says about itself: nothing while it is empty; its units
   *  and its total once the visitor has put something in it. */
  readonly cartLabel = computed(() => {
    const count = this.cart.count();
    if (count === 0) {
      return 'Carrito';
    }
    return `Carrito: ${this.unitsLabel()}, total $${this.cart.total().toFixed(2)}`;
  });

  /** How the cart speaks of its contents: one word per cardinality. */
  readonly unitsLabel = computed(() => {
    const count = this.cart.count();
    return count === 1 ? '1 unidad' : `${count} unidades`;
  });

  /** Sends the visitor to the listing of products with the text they typed. */
  search(): void {
    const query = this.query().trim();
    if (!query) {
      return;
    }
    this.router.navigate(['/productos'], { queryParams: { q: query } });
  }
}
