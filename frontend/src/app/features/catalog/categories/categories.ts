import { Component, effect, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CatalogApi } from '@features/catalog/catalog-api';
import { SeoService } from '@core/seo/seo.service';
import { Loading } from '@shared/components/loading/loading';

/**
 * The public page of the categories.
 *
 * <p>Every category links to its own listing, which is the same view of the
 * catalogue filtered by category.
 *
 * <p>The categories answer in signals (P51): the page holds the resource, and
 * its template speaks the three states — the data, the refusal and the wait.
 */
@Component({
  selector: 'app-categories',
  imports: [RouterLink, Loading],
  templateUrl: './categories.html',
})
export class Categories {
  private readonly catalogApi = inject(CatalogApi);
  private readonly seo = inject(SeoService);

  /** The categories, once they have arrived. */
  readonly categories = this.catalogApi.categories();

  constructor() {
    effect(() => {
      this.seo.setPage({
        title: 'Categorías — Xtrim',
        description:
          'Todas las categorías del catálogo de Xtrim: tecnología, hogar, ropa, oficina y ' +
          'servicios.',
        path: '/categorias',
      });
    });
  }
}
