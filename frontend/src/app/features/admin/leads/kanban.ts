import { Component, computed, inject, signal } from '@angular/core';

import { AdminLeadsApi, KanbanCriteria, Lead } from './admin-leads-api';

/**
 * The board of the pipeline (HU-23).
 *
 * <p>One column per state of the pipeline, with the count and the total of
 * each one. A card is dragged to another column to change the state — and the
 * **backend** decides whether the transition is allowed: when it is not, the
 * card returns to its column and the message of the server is shown.
 *
 * <p>The drag and drop is the native one of the browser: the card is a
 * `draggable` element and the columns are the drop targets. No third-party
 * widget is involved, so the board works in the SSR, adds nothing to the
 * bundle of the public site, and carries no licence obligation.
 *
 * <p>The board answers in signals (P51): the panel holds the resource, and
 * the one box of the screen speaks the refusal of the read too (D4).
 */
@Component({
  selector: 'app-kanban',
  templateUrl: './kanban.html',
  styleUrl: './kanban.scss',
})
export class Kanban {
  private readonly api = inject(AdminLeadsApi);

  /** The criteria of the board: it shows every lead. */
  private readonly criteria = signal<KanbanCriteria>({});

  /** The board of the pipeline, answering in signals. */
  readonly board = this.api.kanban(this.criteria);

  readonly message = signal<string | null>(null);
  readonly failed = signal(false);

  /** The lead that is being dragged. */
  readonly dragging = signal<Lead | null>(null);

  /** The column the card is over. */
  readonly hoveredColumn = signal<string | null>(null);

  /** Whether a change is in flight. */
  readonly busy = signal(false);

  /** The columns of the board, with the label of the state. */
  readonly columns = computed(() => (this.board.hasValue() ? this.board.value().columns : []));

  /** The refusal of the read, in the same voice as the writes (P51 D4). */
  protected readonly readError = computed(() => {
    const error = this.board.error();
    return error ? this.messageOf(error) : null;
  });

  /** The drag of a card starts. */
  onDragStart(event: DragEvent, lead: Lead): void {
    this.dragging.set(lead);
    if (event.dataTransfer) {
      event.dataTransfer.effectAllowed = 'move';
      event.dataTransfer.setData('text/plain', String(lead.id));
    }
  }

  /** The card is over a column. */
  onDragOver(event: DragEvent, status: string): void {
    event.preventDefault();
    if (event.dataTransfer) {
      event.dataTransfer.dropEffect = 'move';
    }
    this.hoveredColumn.set(status);
  }

  /** The card leaves the column. */
  onDragLeave(status: string): void {
    if (this.hoveredColumn() === status) {
      this.hoveredColumn.set(null);
    }
  }

  /** The card is dropped: the state changes, if the backend allows it. */
  onDrop(event: DragEvent, status: string): void {
    event.preventDefault();
    this.hoveredColumn.set(null);

    const lead = this.dragging();
    this.dragging.set(null);
    if (!lead || lead.status === status || this.busy()) {
      return;
    }

    this.busy.set(true);
    this.message.set(null);

    this.api.changeStatus(lead.id, { status }).subscribe({
      next: () => {
        this.busy.set(false);
        this.report(`«${lead.name}» pasó a ${this.label(status)}`, false);
        this.board.reload();
      },
      error: (error) => {
        this.busy.set(false);
        // The transition was refused: the card returns to its place, because
        // the board is read again from the server.
        this.report(error?.error?.message ?? 'El cambio de estado no es válido', true);
        this.board.reload();
      },
    });
  }

  /** The name of a state, in Spanish. */
  label(status: string): string {
    const labels: Record<string, string> = {
      NEW: 'Nuevo',
      IN_PROGRESS: 'En gestión',
      CLOSED: 'Cerrado',
    };
    return labels[status] ?? status;
  }

  private report(text: string, failed: boolean): void {
    this.message.set(text);
    this.failed.set(failed);
  }

  /** The word of a refusal: the one of the server, tal y como llegó (P26). */
  private messageOf(error: unknown): string {
    const body = (error as { error?: { message?: string } } | null)?.error;
    return body?.message ?? 'No se pudo leer el tablero';
  }
}
