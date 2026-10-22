import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { App } from './app';
import { StoragePort } from '@core/storage/storage-port';

/** The storage of the tests: an honest memory, nothing more. */
class MemoryStorage extends StoragePort {
  private readonly map = new Map<string, string>();
  override get(key: string): string | null {
    return this.map.get(key) ?? null;
  }
  override set(key: string, value: string): void {
    this.map.set(key, value);
  }
  override remove(key: string): void {
    this.map.delete(key);
  }
}

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), provideHttpClient(), { provide: StoragePort, useClass: MemoryStorage }],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render the layout — the header and the footer', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;

    expect(compiled.querySelector('app-header')).toBeTruthy();
    expect(compiled.querySelector('app-footer')).toBeTruthy();
    expect(compiled.querySelector('router-outlet')).toBeTruthy();
  });

  it('should show the brand and the menu of the header', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    const header = compiled.querySelector('app-header') as HTMLElement;

    expect(header.textContent).toContain('Xtrim');
    expect(header.textContent).toContain('Productos');
    expect(header.textContent).toContain('Servicios');
  });
});
