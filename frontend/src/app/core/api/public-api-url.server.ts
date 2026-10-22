/**
 * Absolute URL of the API, **including** the `/api` prefix.
 *
 * <p>It comes from `PUBLIC_API_URL`, which the deployment platform injects.
 * Render's `RENDER_EXTERNAL_URL` is the origin alone
 * (`https://xtrim-api.onrender.com`), so the path is appended here instead of
 * being written by hand in the Blueprint. A value without a scheme
 * (`xtrim-api.onrender.com`) is completed with `https`, because `fetch()` and
 * the transfer cache need an absolute URL.
 *
 * <p>This module is server-only: it reads `process.env`. It must never be
 * imported from browser code.
 */
const DEFAULT_PUBLIC_API_URL = 'http://localhost:8080/api';

export function resolvePublicApiUrl(raw = process.env['PUBLIC_API_URL']): string {
  const value = (raw ?? '').trim();
  if (value.length === 0) {
    return DEFAULT_PUBLIC_API_URL;
  }

  const url = new URL(/^https?:\/\//i.test(value) ? value : `https://${value}`);
  if (url.pathname === '/' || url.pathname.length === 0) {
    url.pathname = '/api';
  }

  return url.toString().replace(/\/$/, '');
}
