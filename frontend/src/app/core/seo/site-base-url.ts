import { InjectionToken } from '@angular/core';

/**
 * The public URL of the site.
 *
 * <p>It is used to build the canonical links and the Open Graph tags
 * (HU-27). On the server it comes from `SITE_BASE_URL`; in the browser it is
 * the origin of the page that is being shown.
 */
export const SITE_BASE_URL = new InjectionToken<string>('SITE_BASE_URL');
