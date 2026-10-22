/**
 * The abstraction of the browser storage.
 *
 * <p>`localStorage` does not exist on the server, so every component that
 * needs it depends on this port instead. The browser gets the real storage;
 * the server gets an implementation that remembers nothing.
 *
 * <p>This is the pattern of section 12.6 of the use-case document: the cart
 * and the anonymous identifier live behind this abstraction, never behind a
 * direct call to `window.localStorage`.
 */
export abstract class StoragePort {
  abstract get(key: string): string | null;

  abstract set(key: string, value: string): void;

  abstract remove(key: string): void;
}
