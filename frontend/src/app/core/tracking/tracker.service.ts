import { HttpClient } from '@angular/common/http';
import { isPlatformBrowser } from '@angular/common';
import { inject, PLATFORM_ID, Service } from '@angular/core';
import { EMPTY, catchError } from 'rxjs';

import { API_BASE_URL } from '@core/api/api-base-url';
import { AnonymousId } from './anonymous-id';

/** The events of the visitor that the CRM cares about. */
export type EventType =
  | 'SEARCH'
  | 'PRODUCT_VIEW'
  | 'ADD_TO_CART'
  | 'REMOVE_FROM_CART'
  | 'PURCHASE'
  | 'STOCK_SUBSCRIBE'
  | 'INFO_REQUEST';

/**
 * The tracker of the activity of the visitor (HU-09).
 *
 * <p>It is **fire and forget**: the request is not awaited, the errors are
 * swallowed, and nothing happens on the server. A visitor who cannot be
 * tracked must still be able to browse.
 *
 * <p>It only runs in the browser: on the server there is no visitor to
 * track.
 */
@Service()
export class Tracker {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);
  private readonly anonymousId = inject(AnonymousId);
  private readonly platformId = inject(PLATFORM_ID);

  /**
   * Records an action of the visitor.
   *
   * @param type what the visitor did
   * @param entityType the kind of entity the action is about
   * @param entityId the identifier of that entity
   * @param metadata the details of the action
   */
  track(
    type: EventType,
    entityType?: string,
    entityId?: number,
    metadata?: Record<string, unknown>,
  ): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }
    try {
      this.http
        .post(`${this.baseUrl}/public/events`, {
          anonymousId: this.anonymousId.get(),
          type,
          entityType,
          entityId,
          metadata,
        })
        .pipe(catchError(() => EMPTY))
        .subscribe();
    } catch {
      /* The tracking never breaks the page. */
    }
  }
}
