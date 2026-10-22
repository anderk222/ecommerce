import { Component, computed, inject, input, output, signal } from '@angular/core';

import { form, FormField, required } from '@angular/forms/signals';

import { AdminLeadsApi, TimelineEntry } from '@features/admin/leads/admin-leads-api';

/**
 * The detail of a lead: its data, the timeline of the visitor, its orders, its
 * notes and the history of its states (HU-25).
 *
 * <p>The timeline of P35 is a filterable and retractable history: the chips of
 * the operations (several at once, empty = every event), the entries collapsed
 * into their specific summary until the salesperson opens their recorded
 * details, and the product of every action named.
 */
@Component({
  selector: 'app-lead-detail',
  imports: [FormField],
  templateUrl: './lead-detail.html',
})
export class LeadDetail {
  private readonly api = inject(AdminLeadsApi);

  /** The lead whose detail is shown. */
  readonly leadId = input.required<number>();

  /** Emitted when the detail is closed. */
  readonly closed = output<void>();

  /** The detail of the lead, answering in signals (P51): the identifier of
   *  the lead is the source of the read, so a lead that changes re-reads
   *  itself. */
  readonly detail = this.api.detail(this.leadId);

  /** The payload of the lead, once it has arrived. */
  readonly payload = computed(() => (this.detail.hasValue() ? this.detail.value() : null));

  readonly message = signal<string | null>(null);
  readonly failed = signal(false);

  /** The refusal of the read, in the same voice as the writes (P51 D4). */
  protected readonly readError = computed(() => {
    const error = this.detail.error();
    return error ? this.messageOf(error) : null;
  });

  private readonly noteModel = signal({ body: '' });

  /** The schema of the note (P46): a required body, the word in the schema. */
  readonly noteForm = form(this.noteModel, (schemaPath) => {
    required(schemaPath.body, { message: 'Este campo es requerido' });
  });

  /**
   * Whether a note submit was attempted — the contract's second condition of
   * visibility: an untouched textarea that never tried to submit stays quiet.
   */
  readonly attempted = signal(false);

  /** The types of operation the filter holds, in the voice of the timeline.
   *  An empty set means every event: the state the panel opens in. */
  readonly selectedTypes = signal<Set<string>>(new Set());

  /** The entries whose recorded details are open (P35 D3: collapsed truth). */
  readonly expanded = signal<Set<number>>(new Set());

  /** The fixed order of the operations of the timeline (the order of EventType). */
  private static readonly EVENT_TYPES = [
    'SEARCH',
    'PRODUCT_VIEW',
    'ADD_TO_CART',
    'REMOVE_FROM_CART',
    'PURCHASE',
    'STOCK_SUBSCRIBE',
    'INFO_REQUEST',
  ];

  /** One row per operation type that occurred in this lead's timeline. */
  readonly knownTypes = computed(() => {
    const counts = this.typeCounts();
    return LeadDetail.EVENT_TYPES.filter((type) => counts[type]);
  });

  /** How many events of each type the timeline holds: the chip's number. */
  readonly typeCounts = computed(() => {
    const counts: Record<string, number> = {};
    for (const entry of this.payload()?.timeline ?? []) {
      counts[entry.type] = (counts[entry.type] ?? 0) + 1;
    }
    return counts;
  });

  /** Whether a filter narrows the timeline. */
  readonly filtering = computed(() => this.selectedTypes().size > 0);

  /** The events the timeline shows: the union of the chosen types. */
  readonly filteredTimeline = computed(() => {
    const timeline = this.payload()?.timeline ?? [];
    const chosen = this.selectedTypes();
    if (chosen.size === 0) {
      return timeline;
    }
    return timeline.filter((entry) => chosen.has(entry.type));
  });

  /** The moment the clock speaks about, es-EC first. */
  private readonly clock = new Intl.DateTimeFormat('es', {
    dateStyle: 'medium',
    timeStyle: 'short',
  });

  /** Writes a note. An empty attempt never reaches the server (P46 D1). */
  submitNote(event: Event): void {
    event.preventDefault();
    this.attempted.set(true);
    if (this.noteForm().invalid()) {
      this.report('Revisa el campo marcado antes de agregar la nota.', true);
      return;
    }
    const text = this.noteModel().body.trim();
    if (!text) {
      this.report('Escribe la nota antes de agregarla.', true);
      return;
    }
    this.report(null, false);
    this.api.addNote(this.leadId(), text).subscribe({
      next: () => {
        this.noteForm().reset({ body: '' });
        this.attempted.set(false);
        this.report('Nota agregada', false);
        this.detail.reload();
      },
      error: (error) => this.report(error?.error?.message ?? 'No se pudo agregar la nota', true),
    });
  }

  // --- the filter of the operations (P35 D2) ------------------------------

  /** Adds or removes a type of operation from the filter. */
  toggleType(type: string): void {
    const chosen = new Set(this.selectedTypes());
    if (chosen.has(type)) {
      chosen.delete(type);
    } else {
      chosen.add(type);
    }
    this.selectedTypes.set(chosen);
    this.expanded.set(new Set());
  }

  /** Returns the timeline to its whole story. */
  clearFilter(): void {
    this.selectedTypes.set(new Set());
    this.expanded.set(new Set());
  }

  // --- the retract of the entries (P35 D3) --------------------------------

  /** Opens or closes the recorded details of one entry. */
  toggleEntry(index: number): void {
    const open = new Set(this.expanded());
    if (open.has(index)) {
      open.delete(index);
    } else {
      open.add(index);
    }
    this.expanded.set(open);
  }

  /** Whether the details of one entry are open. */
  isOpen(index: number): boolean {
    return this.expanded().has(index);
  }

  // --- the words of the timeline (P35 D4) ---------------------------------

  /** The name of an event of the timeline, in Spanish. */
  label(type: string): string {
    const labels: Record<string, string> = {
      SEARCH: 'Buscó',
      PRODUCT_VIEW: 'Vio',
      ADD_TO_CART: 'Agregó al carrito',
      REMOVE_FROM_CART: 'Sacó del carrito',
      PURCHASE: 'Compró',
      STOCK_SUBSCRIBE: 'Pidió aviso de stock',
      INFO_REQUEST: 'Pidió información',
    };
    return labels[type] ?? type;
  }

  /** The row's own tone: the colour the icon bubble wears for its operation. */
  tone(entry: TimelineEntry): string {
    const tones: Record<string, string> = {
      SEARCH: 'is-tone-grey',
      PRODUCT_VIEW: 'is-tone-blue',
      ADD_TO_CART: 'is-tone-green',
      REMOVE_FROM_CART: 'is-tone-red',
      PURCHASE: 'is-tone-money',
      STOCK_SUBSCRIBE: 'is-tone-yellow',
      INFO_REQUEST: 'is-tone-orange',
    };
    return tones[entry.type] ?? 'is-tone-grey';
  }

  /** The specific sentence of one entry, in the voice of its operation. */
  summary(entry: TimelineEntry): string {
    const name = this.nameOf(entry);
    const metadata = entry.metadata ?? {};
    switch (entry.type) {
      case 'SEARCH': {
        const query = metadata['query'];
        return query ? `Buscó «${query}» en el catálogo` : 'Buscó en el catálogo';
      }
      case 'PRODUCT_VIEW':
        return name ? `Vio el producto «${name}»` : 'Vio un producto';
      case 'ADD_TO_CART': {
        const quantity = Number(metadata['quantity'] ?? 1) || 1;
        const via = metadata['via'] === 'BUY_NOW' ? ' con «Comprar ahora»' : '';
        return name
          ? `Agregó ${quantity} × «${name}» al carrito${via}`
          : 'Agregó un producto al carrito';
      }
      case 'REMOVE_FROM_CART':
        return name ? `Sacó «${name}» del carrito` : 'Sacó un producto del carrito';
      case 'PURCHASE': {
        const total = metadata['total'];
        return total ? `Compró por $${total}` : 'Compró en la tienda';
      }
      case 'STOCK_SUBSCRIBE':
        return name ? `Pidió aviso de stock de «${name}»` : 'Pidió aviso de stock';
      case 'INFO_REQUEST':
        return name ? `Pidió información de «${name}»` : 'Pidió información';
      default:
        return this.label(entry.type);
    }
  }

  /** The recorded details of one entry, for its expanded view. */
  details(entry: TimelineEntry): { label: string; value: string }[] {
    const metadata = entry.metadata ?? {};
    const rows: { label: string; value: string }[] = [];
    const name = this.nameOf(entry);
    if (name) {
      rows.push({ label: 'Producto', value: name });
    }
    if (entry.entityType === 'CATALOG_ITEM' && entry.entityId != null) {
      rows.push({ label: 'Ítem', value: '#' + entry.entityId });
    }
    if (metadata['query']) {
      rows.push({ label: 'Búsqueda', value: String(metadata['query']) });
    }
    if (metadata['slug']) {
      rows.push({ label: 'Ficha', value: '/' + String(metadata['slug']) });
    }
    if (metadata['quantity'] != null) {
      rows.push({ label: 'Cantidad', value: String(metadata['quantity']) });
    }
    if (metadata['via'] != null) {
      rows.push({
        label: 'Botón',
        value: metadata['via'] === 'BUY_NOW' ? 'Comprar ahora' : 'Agregar al carrito',
      });
    }
    if (metadata['total'] != null) {
      rows.push({ label: 'Total', value: '$' + metadata['total'] });
    }
    return rows;
  }

  /** The moment of an entry, as the panel says it: es, medium and short. */
  when(entry: TimelineEntry): string {
    const date = new Date(entry.createdAt);
    return Number.isNaN(date.getTime()) ? entry.createdAt : this.clock.format(date);
  }

  /** The product of an entry, the name the store knows it by. */
  private nameOf(entry: TimelineEntry): string | null {
    if (entry.productName) {
      return entry.productName;
    }
    const recorded = entry.metadata?.['name'];
    return typeof recorded === 'string' && recorded.trim() ? recorded : null;
  }

  /** The link's status history word: the legend the list already speaks. */
  statusLabel(status: string | null): string | null {
    if (!status) {
      return null;
    }
    const labels: Record<string, string> = {
      NEW: 'Nuevo',
      IN_PROGRESS: 'En gestión',
      CLOSED: 'Cerrado',
    };
    return labels[status] ?? status;
  }

  private report(text: string | null, failed: boolean): void {
    this.message.set(text);
    this.failed.set(failed);
  }

  /** The word of a refusal: the one of the server, tal y como llegó (P26). */
  private messageOf(error: unknown): string {
    const body = (error as { error?: { message?: string } } | null)?.error;
    return body?.message ?? 'No se pudo leer el lead';
  }
}
