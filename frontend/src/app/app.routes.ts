import { Routes } from '@angular/router';

import { authGuard } from '@core/auth/auth.guard';
import { roleGuard } from '@core/auth/role.guard';

/**
 * The routes of the site.
 *
 * <p>The public pages are rendered on the server (tech-stack §3.1); the
 * administration panel is client only and carries `noindex`.
 */
export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () =>
      import('@features/catalog/home/home').then((m) => m.Home),
  },
  {
    path: 'productos',
    loadComponent: () =>
      import('@features/catalog/listing/listing').then((m) => m.Listing),
  },
  {
    path: 'servicios',
    loadComponent: () =>
      import('@features/catalog/listing/listing').then((m) => m.Listing),
    data: { type: 'SERVICE' },
  },
  {
    path: 'productos/:slug',
    loadComponent: () =>
      import('@features/catalog/detail/detail').then((m) => m.Detail),
    data: { type: 'PRODUCT' },
  },
  {
    path: 'servicios/:slug',
    loadComponent: () =>
      import('@features/catalog/detail/detail').then((m) => m.Detail),
    data: { type: 'SERVICE' },
  },
  {
    path: 'categorias',
    loadComponent: () =>
      import('@features/catalog/categories/categories').then((m) => m.Categories),
  },
  {
    path: 'categorias/:slug',
    loadComponent: () =>
      import('@features/catalog/listing/listing').then((m) => m.Listing),
    data: { byCategory: true },
  },
  {
    path: 'contacto',
    loadComponent: () =>
      import('@features/contact/contact').then((m) => m.Contact),
  },
  {
    path: 'carrito',
    loadComponent: () => import('@features/cart/cart').then((m) => m.Cart),
  },
  {
    path: 'checkout',
    loadComponent: () =>
      import('@features/checkout/checkout').then((m) => m.Checkout),
  },
  {
    path: 'admin/login',
    loadComponent: () => import('@features/admin/login/login').then((m) => m.Login),
  },
  {
    path: 'admin',
    canActivate: [authGuard],
    loadComponent: () => import('@features/admin/admin').then((m) => m.Admin),
    children: [
      {
        // The tab «Catálogo» points at /admin: the index of the panel redirects
        // to the listing, so the outlet is never empty.
        path: '',
        pathMatch: 'full',
        redirectTo: 'catalog',
      },
      {
        path: 'catalog',
        loadComponent: () =>
          import('@features/admin/catalog/catalog-admin').then((m) => m.CatalogAdmin),
      },
      {
        path: 'catalog/nuevo',
        loadComponent: () =>
          import('@features/admin/catalog/catalog-item-form').then((m) => m.CatalogItemForm),
      },
      {
        path: 'catalog/:id',
        loadComponent: () =>
          import('@features/admin/catalog/catalog-item-form').then((m) => m.CatalogItemForm),
      },
      {
        path: 'leads',
        loadComponent: () => import('@features/admin/leads/leads').then((m) => m.Leads),
      },
      {
        path: 'leads/tablero',
        loadComponent: () => import('@features/admin/leads/kanban').then((m) => m.Kanban),
      },
      {
        path: 'leads/indicadores',
        loadComponent: () => import('@features/admin/leads/kpis').then((m) => m.Kpis),
      },
      {
        path: 'categorias',
        canActivate: [roleGuard],
        data: { role: 'ADMIN' },
        loadComponent: () =>
          import('@features/admin/categories/categories-admin').then((m) => m.CategoriesAdmin),
      },
    ],
  },
  {
    path: '**',
    loadComponent: () => import('@features/not-found/not-found').then((m) => m.NotFound),
  },
];
