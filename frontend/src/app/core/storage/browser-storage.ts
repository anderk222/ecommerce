import { Injectable } from '@angular/core';

import { StoragePort } from './storage-port';

/**
 * The real storage, in the browser.
 *
 * <p>A failure to write (a full disk, a private window) must never break the
 * page: the storage is a convenience, not a guarantee.
 */
@Injectable()
export class BrowserStorage extends StoragePort {
  override get(key: string): string | null {
    try {
      return window.localStorage.getItem(key);
    } catch {
      return null;
    }
  }

  override set(key: string, value: string): void {
    try {
      window.localStorage.setItem(key, value);
    } catch {
      /* The storage is full or blocked: the page keeps working without it. */
    }
  }

  override remove(key: string): void {
    try {
      window.localStorage.removeItem(key);
    } catch {
      /* Nothing to do: the value will be overwritten anyway. */
    }
  }
}
