import { Component, computed, inject, signal } from '@angular/core';

import { required, form, FormField } from '@angular/forms/signals';

import { AdminCategoriesApi } from './admin-categories-api';
import { Category } from '@shared/models/catalog.models';
import { Loading } from '@shared/components/loading/loading';

/**
 * The administration of the categories (HU-20).
 *
 * <p>It creates, renames and deletes categories. A category that still holds
 * items cannot be deleted, and the server says so — the message is shown as
 * it arrives. The creation is built on the shape the signal forms pass
 * declared (P38): one signal model, the `form()` schema beside it, and the
 * conversation carried by two signals through the single `report(text,
 * failed)`; an attempted empty create never reaches the server — the words
 * walk the staff member to the fix (P40).</p>
 *
 * <p>The ledger answers in signals (P51): the panel holds the resource, and
 * the one box of the screen speaks the refusal of the read too (D4).</p>
 */
@Component({
  selector: 'app-admin-categories',
  imports: [FormField, Loading],
  templateUrl: './categories-admin.html',
})
export class CategoriesAdmin {
  private readonly api = inject(AdminCategoriesApi);

  /** The categories of the catalogue, answering in signals. */
  readonly categories = this.api.list();

  /** The rows of the ledger. */
  readonly rows = computed(() => (this.categories.hasValue() ? this.categories.value() : []));

  private readonly createModel = signal({ name: '' });

  /** The schema of the creation: a required name (P40, the pass contract). */
  readonly createForm = form(this.createModel, (schemaPath) => {
    required(schemaPath.name, { message: 'Este campo es requerido' });
  });

  /**
   * Whether a create was attempted — the contract's second condition of
   * visibility: an untouched input that never tried to create stays quiet.
   */
  readonly attempted = signal(false);

  /** The identifier of the category that is being renamed, if any. */
  readonly editingId = signal<number | null>(null);

  /** The new name of the category that is being renamed. */
  readonly editingName = signal('');

  /** The message of the last operation. */
  readonly message = signal<string | null>(null);

  /** Whether the last operation failed. */
  readonly failed = signal(false);

  /** The refusal of the read, in the same voice as the writes (P51 D4). */
  protected readonly readError = computed(() => {
    const error = this.categories.error();
    return error ? this.messageOf(error) : null;
  });

  /** Creates a category. An empty attempt never reaches the server (P40 D1). */
  create(event: Event): void {
    event.preventDefault();
    this.attempted.set(true);
    if (this.createForm().invalid()) {
      this.report('Revisa los campos marcados antes de crear.', true);
      return;
    }
    this.api.create(this.name.trim()).subscribe({
      next: () => {
        // The reset (the discovery of the phase): touched, dirty, word, paint
        // and the DOM input live again in one breath.
        this.createForm().reset({ name: '' });
        this.report('Categoría creada', false);
        this.categories.reload();
      },
      error: (error) => this.report(this.messageOf(error), true),
    });
  }

  /** Starts the edition of a category. */
  startEditing(category: Category): void {
    this.editingId.set(category.id);
    this.editingName.set(category.name);
  }

  /** Renames a category. */
  save(): void {
    const id = this.editingId();
    const name = this.editingName().trim();
    if (id === null || !name) {
      return;
    }
    this.api.update(id, name).subscribe({
      next: () => {
        this.editingId.set(null);
        this.report('Categoría actualizada', false);
        this.categories.reload();
      },
      error: (error) => this.report(this.messageOf(error), true),
    });
  }

  /** Deletes a category. */
  remove(category: Category): void {
    if (!confirm(`¿Eliminar la categoría «${category.name}»?`)) {
      return;
    }
    this.api.delete(category.id).subscribe({
      next: () => {
        this.report('Categoría eliminada', false);
        this.categories.reload();
      },
      error: (error) => this.report(this.messageOf(error), true),
    });
  }

  /** Writes the message of the conversation, with the voice of its outcome. */
  private report(text: string, failed: boolean): void {
    this.message.set(text);
    this.failed.set(failed);
  }

  /** The word of a refusal: the one of the server, tal y como llegó (P26). */
  private messageOf(error: unknown): string {
    const body = (error as { error?: { message?: string } } | null)?.error;
    return body?.message ?? 'La operación falló';
  }

  get name(): string {
    return this.createModel().name;
  }
}
