/**
 * The type of the jQuery global that Fomantic needs.
 *
 * <p>Fomantic is a jQuery plugin: it installs itself on `window.$` when the
 * script is loaded. jQuery itself ships no types here, so this file declares
 * the small surface the application uses — the behaviours of Fomantic and
 * nothing more.
 */
interface FomanticJQuery {
  (selector: string | Element | Document): FomanticJQuery;

  find(selector: string): FomanticJQuery;

  dropdown(...args: unknown[]): FomanticJQuery;

  popup(...args: unknown[]): FomanticJQuery;

  accordion(...args: unknown[]): FomanticJQuery;

  tab(...args: unknown[]): FomanticJQuery;

  on(event: string, handler: (...args: unknown[]) => void): FomanticJQuery;
}

declare global {
  interface Window {
    $?: FomanticJQuery;
  }
}

export {};
