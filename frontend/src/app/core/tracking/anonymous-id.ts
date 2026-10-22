import { Service, inject } from '@angular/core';

import { StoragePort } from '@core/storage/storage-port';

/**
 * The anonymous identifier of the visitor (HU-09).
 *
 * <p>It is a UUID in the storage of the browser, generated on the first
 * visit. It is the identity of the visitor until they give a contact: the
 * activity is recorded against it, and it is promoted to a `lead_id` at that
 * moment (mission §7.3).
 */
@Service()
export class AnonymousId {
  private static readonly KEY = 'xtrim.anonymousId';

  private readonly storage = inject(StoragePort);

  /** The identifier of the browser, generated the first time it is asked for. */
  get(): string {
    const existing = this.storage.get(AnonymousId.KEY);
    if (existing) {
      return existing;
    }
    const generated = this.generate();
    this.storage.set(AnonymousId.KEY, generated);
    return generated;
  }

  private generate(): string {
    const cryptoApi = globalThis.crypto;
    if (cryptoApi && typeof cryptoApi.randomUUID === 'function') {
      return cryptoApi.randomUUID();
    }
    // A browser without randomUUID still gets a unique enough identifier.
    return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (character) => {
      const random = (Math.random() * 16) | 0;
      const value = character === 'x' ? random : (random & 0x3) | 0x8;
      return value.toString(16);
    });
  }
}
