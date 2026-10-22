import { InjectionToken } from '@angular/core';

/**
 * Base URL of the public API.
 *
 * - In the **browser** it is the relative path `/api`: the dev server proxies
 *   it to the backend, and the SSR server proxies it too in production.
 * - On the **server** it is the absolute `PUBLIC_API_URL`, read from the
 *   environment at render time.
 *
 * Nothing in the frontend ever hard-codes the host of the API.
 */
export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL');
