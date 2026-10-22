import { Component, computed, inject, signal } from '@angular/core';
import { KeyValuePipe } from '@angular/common';

import { AdminLeadsApi, KpisRange } from '@features/admin/leads/admin-leads-api';

/**
 * The indicators of the sales (HU-24).
 *
 * <p>The indicators answer in signals (P51): the panel holds the resource,
 * and the one box of the screen speaks the refusal of the read too (D4).</p>
 */
@Component({
  selector: 'app-kpis',
  imports: [KeyValuePipe],
  templateUrl: './kpis.html',
})
export class Kpis {
  private readonly api = inject(AdminLeadsApi);

  /** The range of the indicators: the whole history. */
  private readonly range = signal<KpisRange>({});

  /** The indicators of the sales, answering in signals. */
  readonly kpis = this.api.kpis(this.range);

  /** The refusal of the read, in the same voice as the writes (P51 D4). */
  protected readonly readError = computed(() => {
    const error = this.kpis.error();
    return error ? this.messageOf(error) : null;
  });

  /** The percentage, with one decimal. */
  percent(value: number): string {
    return `${(value * 100).toFixed(1)}%`;
  }

  /** The word of a refusal: the one of the server, tal y como llegó (P26). */
  private messageOf(error: unknown): string {
    const body = (error as { error?: { message?: string } } | null)?.error;
    return body?.message ?? 'No se pudieron leer los indicadores';
  }
}
