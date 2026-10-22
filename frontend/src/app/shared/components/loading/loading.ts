import { Component, computed, input } from '@angular/core';

/**
 * The skeleton of a page that is still loading.
 *
 * <p>It replaces the ad-hoc markup of Fomantic's placeholder, which had two
 * problems: the `inverted` variant paints it **black** (`#1b1c1d`) on a white
 * page, and the placeholder is capped at `max-width: 30rem`, so it looked like
 * a small box in the middle of the screen instead of a page that is arriving.
 *
 * <p>Here the skeleton takes the width of its container and keeps the shimmer
 * of Fomantic, which is the part that reads well.
 */
@Component({
  selector: 'app-loading',
  template: `
    <div class="ui placeholder xtrim-placeholder" [attr.aria-label]="label()" role="status">
      @for (bar of bars(); track bar) {
        <div class="ui line"></div>
      }
    </div>
  `,
  styles: `
    :host {
      display: block;
      width: 100%;
    }

    .xtrim-placeholder {
      width: 100%;
      max-width: none;
      margin: 0;
    }
  `,
})
export class Loading {
  /** How many bars the skeleton has. */
  readonly lines = input(4);

  /** What the skeleton is standing in for, for the screen readers. */
  readonly label = input('Cargando…');

  /** The bars, ready to be iterated. */
  protected readonly bars = computed(() =>
    Array.from({ length: this.lines() }, (_, index) => index),
  );
}
