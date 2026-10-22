import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { Admin } from './admin';
import { API_BASE_URL } from '@core/api/api-base-url';
import { AuthService } from '@core/auth/auth.service';
import { authInterceptor } from '@core/auth/auth.interceptor';
import { SITE_BASE_URL } from '@core/seo/site-base-url';
import { StoragePort } from '@core/storage/storage-port';

/** A stub so the router has somewhere to arrive when the session ends. */
@Component({ imports: [], template: '' })
class LoginStub {}

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

/** The member that the fake login issues. */
const SESSION = {
  token: 't1',
  email: 'admin@xtrim.ec',
  nombre: 'Administración',
  role: 'ADMIN' as const,
};

describe('Admin', () => {
  let fixture: ReturnType<typeof TestBed.createComponent<Admin>>;
  let backend: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Admin],
      providers: [
        provideRouter([{ path: 'admin/login', component: LoginStub }]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: StoragePort, useClass: MemoryStorage },
        { provide: API_BASE_URL, useValue: '/api' },
        { provide: SITE_BASE_URL, useValue: 'https://xtrim.example' },
      ],
    }).compileComponents();

    backend = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Admin);

    // Signs in before the panel is read: the identity of the header.
    const auth = TestBed.inject(AuthService);
    auth.login('admin@xtrim.ec', 'admin123').subscribe();
    backend.expectOne('/api/auth/login').flush(SESSION);
    fixture.detectChanges();
  });

  afterEach(() => {
    backend.verify();
  });

  /** The rendered panel. */
  const el = () => fixture.nativeElement as HTMLElement;

  it('names the member of the staff and the role they carry', () => {
    expect(el().querySelector('.xtrim-admin__name')?.textContent).toBe('Administración');
    expect(el().querySelector('.xtrim-chip')?.textContent).toBe('ADMIN');
  });

  it('says what the button is for, with the words and the icon', () => {
    const button = el().querySelector<HTMLButtonElement>('.xtrim-admin__logout')!;
    expect(button.textContent).toContain('Cerrar sesión');
    expect(button.getAttribute('aria-label')).toBe('Cerrar sesión');
    expect(button.getAttribute('title')).toBe('Cerrar la sesión de Administración');
    expect(button.querySelector('.sign.out.alternate.icon')).toBeTruthy();
  });

  it('ends the session and goes back to the login', async () => {
    el().querySelector<HTMLButtonElement>('.xtrim-admin__logout')!.click();

    await new Promise((resolve) => setTimeout(resolve, 0));

    const auth: AuthService = TestBed.inject(AuthService);
    expect(auth.signedIn()).toBeFalsy();
    expect(TestBed.inject(Router).url).toContain('/admin/login');
  });
});
