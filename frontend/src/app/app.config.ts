import { DOCUMENT } from '@angular/common';
import {
  ApplicationConfig,
  inject,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideRouter, withComponentInputBinding, withInMemoryScrolling } from '@angular/router';
import { routes } from './app.routes';
import {
  provideClientHydration,
  withHttpTransferCacheOptions,
} from '@angular/platform-browser';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { API_BASE_URL } from '@core/api/api-base-url';
import { BrowserStorage } from '@core/storage/browser-storage';
import { SITE_BASE_URL } from '@core/seo/site-base-url';
import { WHATSAPP_NUMBER } from '@core/config/whatsapp-number';
import { authInterceptor } from '@core/auth/auth.interceptor';
import { StoragePort } from '@core/storage/storage-port';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(
      routes,
      withComponentInputBinding(),
      withInMemoryScrolling({ scrollPositionRestoration: 'enabled' }),
    ),
    // The hydration of the client. The cache of the responses of the server
    // is on by default in Angular 22 (`withHttpTransferCache` became the
    // default and `withNoHttpTransferCache` is the one that turns it off).
    //
    // The option is there because Spring Security puts
    // `Cache-Control: no-cache, no-store` in every response of the API, and
    // Angular does not transfer a response with those headers. The catalogue
    // of the site is a public, read-only thing —the API even keeps it in
    // Redis for five minutes—, so the answer of the server can travel to the
    // browser: the page does not ask again for what it already has, and it
    // does not fall back to the loading skeleton while it waits (HU-30).
    // The calls with a token (the panel) are never transferred: that is the
    // default of Angular and it is not changed here.
    provideClientHydration(withHttpTransferCacheOptions({ includeNonCacheableRequests: true })),
    provideHttpClient(withInterceptors([authInterceptor])),
    // The browser always calls the API through the route /api of its own
    // origin: the dev server proxies it to the backend, and the SSR server
    // proxies it in production. The absolute URL never reaches the browser
    // bundle —but it is the *same* string that the server uses to fetch, so
    // that the cache of the responses of the server (the transfer cache) is
    // a hit in the browser and the page does not fall back to the loading
    // skeleton while it waits (HU-30).
    {
      provide: API_BASE_URL,
      useFactory: () => `${inject(SITE_BASE_URL)}/api`,
    },
    // The storage is the browser's; on the server it is a no-op.
    { provide: StoragePort, useClass: BrowserStorage },
    // The public URL of the site: in the browser it is the origin of the
    // page that is being shown.
    {
      provide: SITE_BASE_URL,
      useFactory: () => inject(DOCUMENT).location?.origin ?? '',
    },
    // The number of WhatsApp of the shop, in international format.
    { provide: WHATSAPP_NUMBER, useValue: '593999999999' },
  ],
};
