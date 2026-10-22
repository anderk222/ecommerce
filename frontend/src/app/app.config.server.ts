import { HTTP_TRANSFER_CACHE_ORIGIN_MAP } from '@angular/common/http';
import { mergeApplicationConfig, ApplicationConfig } from '@angular/core';
import { provideServerRendering, withRoutes } from '@angular/ssr';
import { appConfig } from './app.config';
import { serverRoutes } from './app.routes.server';
import { API_BASE_URL } from '@core/api/api-base-url';
import { resolvePublicApiUrl } from '@core/api/public-api-url.server';
import { ServerStorage } from '@core/storage/server-storage';
import { SITE_BASE_URL } from '@core/seo/site-base-url';
import { StoragePort } from '@core/storage/storage-port';

const serverConfig: ApplicationConfig = {
  providers: [
    provideServerRendering(withRoutes(serverRoutes)),
    // On the server the API is reached through its absolute URL. It comes
    // from the environment (PUBLIC_API_URL), never from a hard-coded string:
    // Render injects it when the app is deployed.
    {
      provide: API_BASE_URL,
      useFactory: () => resolvePublicApiUrl(),
    },
    /**
     * The map of the origins for the cache of the responses (HU-30).
     *
     * <p>The server asks the API for the data and the browser asks the site
     * for it, through the proxy of `/api`. Those are two different origins,
     * so the cache of the server (the *transfer cache*) would not recognize
     * them as the same request: the browser would ask again, the page would
     * show the loading skeleton for a moment and the whole layout would move
     * (a CLS of 1.6). With this map, Angular computes the key of the cache
     * with the origin of the site, and the browser finds the answer the
     * server already sent.
     *
     * <p>It is only provided here, on the server: Angular fails if it finds
     * it in the browser.
     */
    {
      provide: HTTP_TRANSFER_CACHE_ORIGIN_MAP,
      useFactory: () => {
        const api = resolvePublicApiUrl();
        const site = process.env['SITE_BASE_URL'] ?? 'http://localhost:4200';
        return { [new URL(api).origin]: new URL(site).origin };
      },
    },
    // There is no localStorage on the server: the storage remembers nothing.
    { provide: StoragePort, useClass: ServerStorage },
    // The public URL of the site, for the canonical links and the Open Graph.
    {
      provide: SITE_BASE_URL,
      useValue: process.env['SITE_BASE_URL'] ?? 'http://localhost:4200',
    },
  ],
};

export const config = mergeApplicationConfig(appConfig, serverConfig);
