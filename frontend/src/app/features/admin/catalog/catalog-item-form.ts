import { Component, computed, effect, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { form, FormField, maxLength, min, required } from '@angular/forms/signals';

import {
  AdminCatalogApi,
  CatalogItemPayload,
} from './admin-catalog-api';

/**
 * The form of the creation and of the edition of an item (HU-15, HU-16).
 *
 * <p>The specifications are pairs of key and value that the administrator
 * adds and removes, and the categories are picked from the ones that exist.
 * Built on the shape the signal forms pass declared (P39): one signal model,
 * the `form()` schema beside it, no public mutable fields, and the
 * conversation with the visitor carried by two signals through the single
 * `report(text, failed)` — the way `stock-notify` and the login speak.</p>
 *
 * <p>The reads answer in signals (P51): the item's read waits for the
 * identifier of the edition — in the create mode there is nothing to read, so
 * no request flies at all — and the categories are a static read. The form is
 * filled once, when the item arrives: a later re-read never clobbers the
 * administrator's edits.</p>
 */
@Component({
  selector: 'app-catalog-item-form',
  imports: [FormField, RouterLink],
  templateUrl: './catalog-item-form.html',
  styleUrl: './catalog-item-form.scss',
})
export class CatalogItemForm implements OnInit {
  private readonly api = inject(AdminCatalogApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  /** The identifier of the item that is being edited, when there is one. */
  readonly editingId = signal<number | null>(null);

  /** The item that is being edited, answering in signals. Without an
   *  identifier there is nothing to read: the resource stays idle. */
  readonly item = this.api.get(this.editingId);

  /** The categories of the catalogue, for the picker of the form. */
  readonly categories = this.api.categories();

  /** The signal model of the item. */
  private readonly itemModel = signal({
    type: 'PRODUCT' as 'PRODUCT' | 'SERVICE',
    name: '',
    description: '',
    price: 0 as number | null,
    stock: null as number | null,
    imagesText: '',
    featured: false,
  });

  /** The schema beside the model, with the words born at the validators. */
  readonly itemForm = form(this.itemModel, (schemaPath) => {
    required(schemaPath.type, { message: 'Este campo es requerido' });
    required(schemaPath.name, { message: 'Este campo es requerido' });
    maxLength(schemaPath.name, 255, {
      message: 'El nombre no puede superar 255 caracteres',
    });
    required(schemaPath.price, { message: 'Este campo es requerido' });
    min(schemaPath.price, 0, { message: 'El precio no puede ser negativo' });
    min(schemaPath.stock, 0, { message: 'El stock no puede ser negativo' });
  });

  /**
   * Whether a submit was attempted — the contract's second condition of visibility:
   * an untouched input that never tried to submit stays quiet.
   */
  readonly attempted = signal(false);

  /** The specifications of the item. */
  readonly specifications = signal<{ name: string; value: string }[]>([]);

  /** The categories the item belongs to. */
  readonly selectedCategories = signal<number[]>([]);

  readonly message = signal<string | null>(null);

  /** Whether the last answer was a refusal or a failure. */
  readonly failed = signal(false);

  readonly busy = signal(false);

  /**
   * Whether the form was already filled with the item that arrived (P51 D2):
   * the fill happens once, so a later re-read never clobbers the edits of
   * the administrator.
   */
  private readonly filled = signal(false);

  /** The refusal of the read, in the same voice as the writes (P51 D4): the
   *  one box of the screen speaks both, and the word of the write keeps the
   *  priority. */
  protected readonly readError = computed(() => {
    const error = this.item.error();
    if (error) {
      return this.messageOf(error, 'No se pudo cargar el ítem');
    }
    const categories = this.categories.error();
    return categories ? this.messageOf(categories, 'No se pudieron cargar las categorías') : null;
  });

  constructor() {
    effect(() => {
      if (this.filled() || !this.item.hasValue()) {
        return;
      }
      const item = this.item.value();
      this.itemModel.set({
        type: item.type,
        name: item.name,
        description: item.description,
        price: item.basePrice,
        stock: item.stock,
        imagesText: item.images.join('\n'),
        featured: item.featured,
      });
      this.specifications.set(
        item.specifications.map((row) => ({ name: row.name, value: row.value })),
      );
      this.selectedCategories.set([...item.categoryIds]);
      this.filled.set(true);
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      return;
    }
    this.editingId.set(Number(id));
  }

  /** Updates one field of one row of specifications. */
  updateSpecification(index: number, field: 'name' | 'value', value: string): void {
    this.specifications.update((rows) =>
      rows.map((row, position) => (position === index ? { ...row, [field]: value } : row)),
    );
  }

  /** Adds a row of specifications. */
  addSpecification(): void {
    this.specifications.update((rows) => [...rows, { name: '', value: '' }]);
  }

  /** Removes a row of specifications. */
  removeSpecification(index: number): void {
    this.specifications.update((rows) => rows.filter((_, position) => position !== index));
  }

  /** Adds or removes a category of the item. */
  toggleCategory(id: number): void {
    this.selectedCategories.update((ids) =>
      ids.includes(id) ? ids.filter((candidate) => candidate !== id) : [...ids, id],
    );
  }

  /** Saves the item. */
  save(event: Event): void {
    event.preventDefault();
    this.attempted.set(true);
    if (this.busy()) {
      return;
    }
    if (this.itemForm().invalid()) {
      this.report('Revisa los campos marcados antes de guardar.', true);
      return;
    }
    this.busy.set(true);
    this.report(null, false);

    const model = this.itemModel();
    const payload: CatalogItemPayload = {
      type: model.type,
      name: model.name,
      description: model.description,
      price: Number(model.price),
      stock: model.stock === null ? null : Number(model.stock),
      images: this.parseImages(model.imagesText),
      specifications: this.specifications().filter((row) => row.name.trim().length > 0),
      categoryIds: this.selectedCategories(),
      featured: model.featured,
    };

    const editingId = this.editingId();
    const request = editingId === null
      ? this.api.create(payload)
      : this.api.update(editingId, payload);

    request.subscribe({
      next: (item) => {
        this.busy.set(false);
        void this.router.navigate(['/admin/catalog'], {
          state: { created: item.name },
        });
      },
      error: (error) => {
        this.busy.set(false);
        this.report(error?.error?.message ?? 'No se pudo guardar el ítem', true);
      },
    });
  }

  /** Reads the value of an input event — the specifications' own binding. */
  valueOf(event: Event): string {
    return (event.target as HTMLInputElement).value;
  }

  /** Writes the message of the conversation, with the voice of its outcome. */
  private report(text: string | null, failed: boolean): void {
    this.message.set(text);
    this.failed.set(failed);
  }

  /** The word of a refusal: the one of the server, tal y como llegó (P26). */
  private messageOf(error: unknown, fallback: string): string {
    const body = (error as { error?: { message?: string } } | null)?.error;
    return body?.message ?? fallback;
  }

  private parseImages(text: string): string[] {
    return text
      .split('\n')
      .map((line) => line.trim())
      .filter((line) => line.length > 0);
  }
}
