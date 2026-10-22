import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CatalogItemSummary } from '@shared/models/catalog.models';

/**
 * The card of an item, in a listing.
 *
 * <p>It is presentational: it only takes the item and renders it. The
 * container decides what happens when the visitor clicks.
 */
@Component({
  selector: 'app-item-card',
  imports: [RouterLink],
  templateUrl: './item-card.html',
  styleUrl: './item-card.scss',
})
export class ItemCard {
  /** The item the card shows. */
  readonly item = input.required<CatalogItemSummary>();

  /** Whether the price is discounted. */
  readonly discounted = computed(() => this.item().price.discountPercentage > 0);

  /** The route of the detail of the item. */
  readonly link = computed(() =>
    this.item().type === 'SERVICE'
      ? `/servicios/${this.item().slug}`
      : `/productos/${this.item().slug}`,
  );
}
