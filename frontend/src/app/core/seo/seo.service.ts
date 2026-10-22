import { DOCUMENT } from '@angular/common';
import { inject, Service } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';

import { SITE_BASE_URL } from './site-base-url';

/**
 * The meta tags of a page.
 *
 * @param title the title of the page
 * @param description the description of the page
 * @param path the path of the page, used for the canonical link
 * @param image the absolute URL of the image of the page, for the Open Graph
 * @param jsonLd the structured data of the page, if any
 */
export interface PageMeta {
  title: string;
  description: string;
  path: string;
  image?: string | null;
  jsonLd?: Record<string, unknown> | null;
}

/**
 * The SEO of the public pages.
 *
 * <p>It sets the title, the description, the canonical link, the Open Graph
 * tags and the JSON-LD structured data (HU-27, §12.6).
 *
 * <p>It is called from the container of each public page, once its data has
 * arrived.
 */
@Service()
export class SeoService {
  private readonly title = inject(Title);
  private readonly meta = inject(Meta);
  private readonly document = inject(DOCUMENT);
  private readonly siteBaseUrl = inject(SITE_BASE_URL);

  /** Sets every meta tag of a page. */
  setPage(meta: PageMeta): void {
    const url = this.absoluteUrl(meta.path);
    const image = meta.image ? this.absoluteUrl(meta.image) : null;

    this.title.setTitle(meta.title);
    this.meta.updateTag({ name: 'description', content: meta.description });
    this.meta.updateTag({ property: 'og:title', content: meta.title });
    this.meta.updateTag({ property: 'og:description', content: meta.description });
    this.meta.updateTag({ property: 'og:type', content: 'website' });
    this.meta.updateTag({ property: 'og:url', content: url });
    this.meta.updateTag({ property: 'og:site_name', content: 'Xtrim' });
    this.meta.updateTag({ name: 'twitter:card', content: 'summary_large_image' });
    this.meta.updateTag({ name: 'twitter:title', content: meta.title });
    this.meta.updateTag({ name: 'twitter:description', content: meta.description });
    if (image) {
      this.meta.updateTag({ property: 'og:image', content: image });
      this.meta.updateTag({ name: 'twitter:image', content: image });
    }

    this.setCanonical(url);
    this.setJsonLd(meta.jsonLd);
  }

  /** Marks a page as one that must not be indexed — the administration panel. */
  setNoIndex(): void {
    this.meta.updateTag({ name: 'robots', content: 'noindex, nofollow' });
  }

  private absoluteUrl(path: string): string {
    const base = this.siteBaseUrl.replace(/\/$/, '');
    const suffix = path.startsWith('/') ? path : `/${path}`;
    return `${base}${suffix}`;
  }

  private setCanonical(url: string): void {
    const head = this.document.head;
    let link = head.querySelector<HTMLLinkElement>('link[rel="canonical"]');
    if (!link) {
      link = this.document.createElement('link');
      link.setAttribute('rel', 'canonical');
      head.appendChild(link);
    }
    link.setAttribute('href', url);
  }

  private setJsonLd(data: Record<string, unknown> | null | undefined): void {
    const head = this.document.head;
    let script = head.querySelector<HTMLScriptElement>('script[data-seo="json-ld"]');
    if (!data) {
      script?.remove();
      return;
    }
    if (!script) {
      script = this.document.createElement('script');
      script.setAttribute('data-seo', 'json-ld');
      script.type = 'application/ld+json';
      head.appendChild(script);
    }
    script.textContent = JSON.stringify(data);
  }
}
