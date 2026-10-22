import { RenderMode, ServerRoute } from '@angular/ssr';

/**
 * How each route is rendered on the server.
 *
 * <p>The public pages are rendered on request: they depend on the catalog,
 * which changes. The administration panel is rendered in the browser only —
 * it needs the token, which only exists there (tech-stack §3.6).
 */
export const serverRoutes: ServerRoute[] = [
  {
    path: 'admin',
    renderMode: RenderMode.Client,
  },
  {
    path: 'admin/**',
    renderMode: RenderMode.Client,
  },
  {
    path: '**',
    renderMode: RenderMode.Server,
  },
];
