import { Injectable } from '@angular/core';

import { StoragePort } from './storage-port';

/**
 * The storage on the server: it remembers nothing.
 *
 * <p>On the server there is no `localStorage`, and the state of a visitor
 * does not belong to the response of a request. The cart and the anonymous
 * identifier are rebuilt in the browser.
 */
@Injectable()
export class ServerStorage extends StoragePort {
  override get(_key: string): string | null {
    return null;
  }

  override set(_key: string, _value: string): void {
    /* Nothing to store on the server. */
  }

  override remove(_key: string): void {
    /* Nothing to remove on the server. */
  }
}
