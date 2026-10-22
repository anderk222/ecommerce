import { Component, effect, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CatalogApi } from '@features/catalog/catalog-api';
import { SeoService } from '@core/seo/seo.service';
import { ItemCard } from '@shared/components/item-card/item-card';
import { Carousel } from '@shared/components/carousel/carousel';
import { Loading } from '@shared/components/loading/loading';

/**
 * The home page.
 *
 * <p>The order is the one of the shop: **first what it sells** —the carousel
 * of the featured items—, then the **services**, then the **products**, and
 * finally the offers and the best sellers.
 *
 * <p>HU-01: the featured items, the most bought ones and the ones with an
 * offer in force.
 *
 * <p>The page is rendered on the server, and the response is transferred to
 * the browser, which never asks the API twice.
 *
 * <p>The content answers in signals (P51): the page holds the resource, and
 * its template speaks the three states — the data, the refusal and the wait.
 */
@Component({
  selector: 'app-home',
  imports: [RouterLink, ItemCard, Carousel, Loading],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home {
  private readonly catalogApi = inject(CatalogApi);
  private readonly seo = inject(SeoService);

  /** The content of the page, once it has arrived. */
  readonly content = this.catalogApi.home();

  constructor() {
    effect(() => {
      this.seo.setPage({
        title: 'Xtrim — Productos y servicios con carrito, compra simulada y CRM de leads',
        description:
          'Catálogo unificado de productos y servicios. Encuentra lo que buscas, agrégalo al ' +
          'carrito y completa tu compra sin registrarte. Cada visita alimenta el CRM de leads.',
        path: '/',
      });
    });
  }
}
