import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Component, signal } from '@angular/core';
import { NavigationEnd, provideRouter, Router } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { filter, firstValueFrom } from 'rxjs';

import { API_BASE_URL } from '@core/api/api-base-url';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';
import { StoragePort } from '@core/storage/storage-port';
import { AdminLeadsApi } from '@features/admin/leads/admin-leads-api';
import { CatalogApi } from '@features/catalog/catalog-api';

/** A stub so the router has somewhere to arrive with the expired session. */
@Component({ template: '' })
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

/** The 401 the API answers with when the token no longer validates. */
const UNAUTHORIZED = { code: 'UNAUTHORIZED', message: 'La petición necesita un token válido' };

describe('authInterceptor', () => {
  let backend: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      providers: [
        provideRouter([{ path: 'admin/login', component: LoginStub }]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: StoragePort, useClass: MemoryStorage },
        { provide: API_BASE_URL, useValue: '/api' },
      ],
    }).compileComponents();

    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    backend.verify();
  });

  /** Signs in a member of the staff, through the real service. */
  const signIn = (): AuthService => {
    const auth = TestBed.inject(AuthService);
    auth.login('admin@xtrim.ec', 'admin123').subscribe();
    backend.expectOne('/api/auth/login').flush(SESSION);
    return auth;
  };

  /**
   * The read of the panel's leads, as the house reads it (P51): the request
   * flies through the interceptor because the resource is a client
   * underneath.
   */
  const readLeads = (): void => {
    const api = TestBed.inject(AdminLeadsApi);
    TestBed.runInInjectionContext(() => api.list(signal({})));
    TestBed.tick();
  };

  /** The read of the public catalogue: the call that carries no token. */
  const readHome = (): void => {
    const api = TestBed.inject(CatalogApi);
    TestBed.runInInjectionContext(() => api.home());
    TestBed.tick();
  };

  /** Waits for the next navigation and says where it landed. */
  const nextUrl = (): Promise<string> =>
    firstValueFrom(
      TestBed.inject(Router).events.pipe(
        filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      ),
    ).then((event) => event.urlAfterRedirects);

  it('signs nothing when the session ends on its own', () => {
    const auth = signIn();

    readLeads();
    backend
      .expectOne('/api/admin/leads')
      .flush(UNAUTHORIZED, { status: 401, statusText: 'Unauthorized' });

    expect(auth.signedIn()).toBeFalsy();
    expect(auth.user()).toBeNull();
  });

  it('takes the staff member to the login and says why', async () => {
    signIn();
    const router = TestBed.inject(Router);

    const arrived = nextUrl();

    readLeads();
    backend
      .expectOne('/api/admin/leads')
      .flush(UNAUTHORIZED, { status: 401, statusText: 'Unauthorized' });

    expect(await arrived).toBe('/admin/login?sesion=expirada');
    expect(router.url).toContain('/admin/login');
  });

  it('keeps the session when the failure is not the token', () => {
    const auth = signIn();

    readLeads();
    backend
      .expectOne('/api/admin/leads')
      .flush(
        { code: 'RESOURCE_NOT_FOUND', message: 'No existe' },
        { status: 404, statusText: 'Not Found' },
      );

    expect(auth.signedIn()).toBeTruthy();
  });

  it('does not carry the token to the public API', () => {
    signIn();

    readHome();
    const request = backend.expectOne('/api/public/home');

    expect(request.request.headers.get('Authorization')).toBeNull();
    request.flush({ featured: [], services: [], products: [], onSale: [], bestSelling: [] });
  });
});
