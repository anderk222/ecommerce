import {
  AngularNodeAppEngine,
  createNodeRequestHandler,
  isMainModule,
  writeResponseToNodeResponse,
} from '@angular/ssr/node';
import compression from 'compression';
import express from 'express';
import { join } from 'node:path';
import { resolvePublicApiUrl } from './app/core/api/public-api-url.server';

const browserDistFolder = join(import.meta.dirname, '../browser');

/**
 * Absolute URL of the API, **including** the `/api` prefix.
 *
 * It comes from the environment — Render injects PUBLIC_API_URL when the app
 * is deployed — and never from a hard-coded string. The same value is the
 * base of the calls the SSR engine makes and the target of the proxy below,
 * so the two can never disagree. `resolvePublicApiUrl` completes the origin
 * with the scheme and the `/api` path when the platform hands only the host.
 */
const publicApiUrl = resolvePublicApiUrl();

/**
 * Hostnames allowed to reach the SSR engine.
 *
 * Angular refuses to render a request whose `Host` header is not on this
 * list — the SSRF protection documented in angular.dev/best-practices/security.
 * The list comes from `NG_ALLOWED_HOSTS` (comma-separated), which is what
 * Render injects; locally it falls back to the loopback names.
 */
const allowedHosts = (process.env['NG_ALLOWED_HOSTS'] ?? 'localhost,127.0.0.1')
  .split(',')
  .map((host) => host.trim())
  .filter((host) => host.length > 0);

/**
 * The public URL of the site.
 *
 * It is the base of the canonical links, of the Open Graph tags and of the
 * sitemap.
 */
const siteBaseUrl = (process.env['SITE_BASE_URL'] ?? 'http://localhost:4200').replace(
  /\/$/,
  '',
);

const app = express();
const angularApp = new AngularNodeAppEngine({ allowedHosts });

/**
 * The responses go compressed (HU-30).
 *
 * <p>Without this the stylesheet travels in 1.6 MB and the scripts in 800 KB; with
 * it they travel in a fraction of that. The middleware is registered **before**
 * the static files and the API route, because a response that has already
 * been sent cannot be compressed afterwards.
 */
app.use(compression());

/** The server does not have to say what it is. */
app.disable('x-powered-by');

/**
 * Proxy the API.
 *
 * The browser always calls the API through the relative path `/api`, so the
 * same relative path has to work in production too. In development the dev
 * server proxies it (proxy.conf.json); here it is proxied to the API named by
 * PUBLIC_API_URL.
 */
app.use(
  '/api',
  express.raw({ type: '*/*', limit: '10mb' }),
  async (req, res) => {
    const target = `${publicApiUrl}${req.url}`;
    const hasBody = req.method !== 'GET' && req.method !== 'HEAD';

    try {
      const upstream = await fetch(target, {
        method: req.method,
        headers: {
          accept: req.headers.accept ?? 'application/json',
          ...(req.headers['content-type']
            ? { 'content-type': req.headers['content-type'] }
            : {}),
          // The admin panel calls /api/admin through this proxy; without the
          // bearer token the API answers 401 and the panel is unreachable in
          // production even though the login succeeded.
          ...(req.headers.authorization
            ? { authorization: req.headers.authorization }
            : {}),
        },
        body: hasBody ? req.body : undefined,
      });

      res.status(upstream.status);
      const contentType = upstream.headers.get('content-type');
      if (contentType) {
        res.setHeader('content-type', contentType);
      }
      res.send(Buffer.from(await upstream.arrayBuffer()));
    } catch (error) {
      console.error(`Cannot reach the API at ${target}`, error);
      res.status(502).json({
        code: 'UPSTREAM_UNAVAILABLE',
        message: `The API at ${publicApiUrl} is not reachable`,
        details: [],
        timestamp: new Date().toISOString(),
      });
    }
  },
);

/**
 * The sitemap (HU-28).
 *
 * It lists the home, the two listings, the categories and every published
 * item. The slugs come from the API, so the sitemap is never stale.
 */
app.get('/sitemap.xml', async (_req, res) => {
  const site = siteBaseUrl;
  const urls = [
    { loc: `${site}/`, priority: '1.0' },
    { loc: `${site}/productos`, priority: '0.9' },
    { loc: `${site}/servicios`, priority: '0.9' },
    { loc: `${site}/categorias`, priority: '0.7' },
  ];

  try {
    // The published items, with their kind: a product lives under
    // /productos and a service under /servicios.
    for (const [type, path] of [
      ['PRODUCT', 'productos'],
      ['SERVICE', 'servicios'],
    ] as const) {
      const response = await fetch(
        `${publicApiUrl}/public/catalog?type=${type}&size=50`,
      );
      if (!response.ok) {
        continue;
      }
      const page = (await response.json()) as {
        content: { slug: string; type: string }[];
      };
      for (const item of page.content) {
        urls.push({ loc: `${site}/${path}/${item.slug}`, priority: '0.8' });
      }
    }
  } catch (error) {
    console.error('The slugs of the sitemap could not be read', error);
  }

  const body = urls
    .map(
      (url) =>
        `  <url><loc>${url.loc}</loc><changefreq>daily</changefreq><priority>${url.priority}</priority></url>`,
    )
    .join('\n');

  res
    .status(200)
    .set('Content-Type', 'application/xml; charset=utf-8')
    .send(
      `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${body}\n</urlset>\n`,
    );
});

/**
 * The robots.txt (HU-28).
 *
 * The catalogue is open; the panel and the CRM are not, and the path of the
 * sitemap is declared.
 */
app.get('/robots.txt', (_req, res) => {
  res
    .status(200)
    .set('Content-Type', 'text/plain; charset=utf-8')
    .send(
      [
        'User-agent: *',
        'Allow: /',
        'Disallow: /admin',
        'Disallow: /api/admin',
        `Sitemap: ${siteBaseUrl}/sitemap.xml`,
        '',
      ].join('\n'),
    );
});

/**
 * Serve static files from /browser
 */
app.use(
  express.static(browserDistFolder, {
    maxAge: '1y',
    index: false,
    redirect: false,
  }),
);

/**
 * Handle all other requests by rendering the Angular application.
 */
app.use((req, res, next) => {
  angularApp
    .handle(req)
    .then((response) =>
      response ? writeResponseToNodeResponse(response, res) : next(),
    )
    .catch(next);
});

/**
 * Start the server if this module is the main entry point, or it is ran via PM2.
 * The server listens on the port defined by the `PORT` environment variable, or defaults to 4000.
 */
if (isMainModule(import.meta.url) || process.env['pm_id']) {
  const port = process.env['PORT'] || 4000;
  app.listen(port, (error) => {
    if (error) {
      throw error;
    }

    console.log(`Node Express server listening on http://localhost:${port}`);
    console.log(`Proxying /api to ${publicApiUrl}`);
    console.log(`Allowed hosts: ${allowedHosts.join(', ')}`);
  });
}

/**
 * Request handler used by the Angular CLI (for dev-server and during build) or Firebase Cloud Functions.
 */
export const reqHandler = createNodeRequestHandler(app);
