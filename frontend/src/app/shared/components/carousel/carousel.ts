import {
  Component,
  DestroyRef,
  computed,
  effect,
  inject,
  input,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';

import { CatalogItemSummary } from '@shared/models/catalog.models';

/**
 * The carousel of the items the shop wants to sell.
 *
 * <p>It shows the items the administrator marked as *featured* —the ones
 * that are for sale right now— with their price and their call to action.
 *
 * <p>It is **written here**, not taken from a widget, for three reasons:
 *
 * <ul>
 *   <li>The **SSR** has to show the first slide. A carousel that only exists
 *       once the JavaScript runs would leave the page empty for the crawler
 *       and would paint a flash of every slide at once.
 *   <li>It costs **no bytes**: the styles are a few lines, and the behaviour
 *       is a signal and a timer.
 *   <li>The rotation **pauses** when the visitor hovers it or when the
 *       system asks for less movement.
 * </ul>
 */
@Component({
  selector: 'app-carousel',
  imports: [RouterLink],
  templateUrl: './carousel.html',
  styleUrl: './carousel.scss',
})
export class Carousel {
  private readonly destroyRef = inject(DestroyRef);

  /** The items of the carousel. */
  readonly items = input.required<CatalogItemSummary[]>();

  /** How long a slide stays on the screen, in milliseconds. */
  readonly interval = input(7000);

  /** The slide that is being shown. */
  readonly index = signal(0);

  /** Whether the rotation is paused — the visitor has the mouse encima. */
  readonly paused = signal(false);

  /** Whether there is more than one slide. */
  readonly hasSeveral = computed(() => this.items().length > 1);

  /** The item that is being shown. */
  readonly current = computed(() => this.items()[this.index()] ?? null);

  constructor() {
    let timer: ReturnType<typeof setInterval> | undefined;

    const stop = () => {
      if (timer) {
        clearInterval(timer);
        timer = undefined;
      }
    };

    const start = () => {
      stop();
      if (!this.hasSeveral() || this.paused() || this.reducedMotion()) {
        return;
      }
      timer = setInterval(() => this.next(), this.interval());
    };

    effect(() => {
      // Reading the signals it depends on is what starts and stops the timer.
      this.items();
      this.index();
      this.paused();
      this.interval();
      start();
    });

    this.destroyRef.onDestroy(stop);
  }

  /** Goes to the next slide. */
  next(): void {
    const total = this.items().length;
    if (total === 0) {
      return;
    }
    this.index.update((current) => (current + 1) % total);
  }

  /** Goes to the previous slide. */
  previous(): void {
    const total = this.items().length;
    if (total === 0) {
      return;
    }
    this.index.update((current) => (current - 1 + total) % total);
  }

  /** Goes to a slide by its number. */
  goTo(index: number): void {
    this.index.set(index);
  }

  /** The route of the detail of an item. */
  linkOf(item: CatalogItemSummary): string {
    return item.type === 'SERVICE'
      ? `/servicios/${item.slug}`
      : `/productos/${item.slug}`;
  }

  /** Pauses the rotation while the visitor is over the carousel. */
  setPaused(paused: boolean): void {
    this.paused.set(paused);
  }

  private reducedMotion(): boolean {
    return (
      typeof window !== 'undefined' &&
      window.matchMedia?.('(prefers-reduced-motion: reduce)').matches === true
    );
  }
}
