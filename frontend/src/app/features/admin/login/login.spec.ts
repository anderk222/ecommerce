import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { RouterOutlet } from '@angular/router';
import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';

import { API_BASE_URL } from '@core/api/api-base-url';
import { authInterceptor } from '@core/auth/auth.interceptor';
import { StoragePort } from '@core/storage/storage-port';
import { Login } from './login';

/** An empty shell that holds the outlet where the login is rendered. */
@Component({ imports: [RouterOutlet], template: '<router-outlet />' })
class Shell {}

/** The panel the login walks to: the test only needs the door to exist. */
@Component({ template: '' })
class Panel {}

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

describe('Login', () => {
  let fixture: ReturnType<typeof TestBed.createComponent<Shell>>;
  let backend: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Shell, Login],
      providers: [
        provideRouter([
          { path: 'admin/login', component: Login },
          { path: 'admin', component: Panel },
        ]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: StoragePort, useClass: MemoryStorage },
        { provide: API_BASE_URL, useValue: '/api' },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Shell);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    backend.verify();
  });

  /** Arrives at the login with the query params of the visit. */
  const arrive = async (queryParams?: Record<string, string>): Promise<void> => {
    await TestBed.inject(Router).navigate(['/admin/login'], { queryParams });
    await fixture.whenStable();
  };

  /** The rendered login, inside the shell. */
  const el = () => fixture.nativeElement as HTMLElement;

  it('greets the staff member whose session expired', async () => {
    await arrive({ sesion: 'expirada' });

    const notice = el().querySelector('[role="status"]');
    expect(notice?.textContent).toContain('Tu sesión ha expirado');
    expect(el().querySelector('[role="alert"]')).toBeNull();
  });

  it('says nothing when it arrives without a reason', async () => {
    await arrive();

    expect(el().querySelector('[role="status"]')).toBeNull();
  });

  it('keeps the word of the server when the login fails', async () => {
    await arrive();

    const form = el().querySelector<HTMLFormElement>('form')!;
    const email = form.querySelector<HTMLInputElement>('#email')!;
    const password = form.querySelector<HTMLInputElement>('#password')!;
    email.value = 'admin@xtrim.ec';
    email.dispatchEvent(new Event('input'));
    password.value = 'wrong';
    password.dispatchEvent(new Event('input'));
    form.dispatchEvent(new Event('submit'));

    backend
      .expectOne('/api/auth/login')
      .flush(
        { code: 'UNAUTHORIZED', message: 'Las credenciales no son correctas' },
        { status: 401, statusText: 'Unauthorized' },
      );
    await fixture.whenStable();

    expect(el().querySelector('[role="alert"]')?.textContent).toContain(
      'Las credenciales no son correctas',
    );
  });

  it('arrives quiet: a pristine form shows no error word', async () => {
    await arrive();

    expect(el().querySelector('.xtrim-error')).toBeNull();
    expect(el().querySelector('[role="alert"]')).toBeNull();
  });

  it('says the contract under the input a member touched and left empty', async () => {
    await arrive();

    const email = el().querySelector<HTMLInputElement>('#email')!;
    email.value = '';
    email.dispatchEvent(new Event('input'));
    email.dispatchEvent(new Event('blur'));
    await fixture.whenStable();

    const error = el().querySelector('#email-error');
    expect(error?.textContent).toContain('Este campo es requerido');
    expect(email.getAttribute('aria-invalid')).toBe('true');
  });

  it('walks the visitor to the fix before knocking the door on an empty submit', async () => {
    await arrive();

    el().querySelector<HTMLFormElement>('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();

    const errors = el().querySelectorAll('.xtrim-error');
    expect(errors.length).toBe(2);
    const alert = el().querySelector('[role="alert"]');
    expect(alert?.textContent).toContain('Revisa los campos marcados antes de entrar.');
    // The door was never knocked: an empty pair is no request at all (P38 D1).
    expect(backend.match('/api/auth/login').length).toBe(0);
  });

  it('signs the member in when the pair is complete', async () => {
    await arrive();

    const form = el().querySelector<HTMLFormElement>('form')!;
    const email = form.querySelector<HTMLInputElement>('#email')!;
    const password = form.querySelector<HTMLInputElement>('#password')!;
    email.value = '  admin@xtrim.ec  ';
    email.dispatchEvent(new Event('input'));
    password.value = 'admin123';
    password.dispatchEvent(new Event('input'));
    form.dispatchEvent(new Event('submit'));

    const request = backend.expectOne('/api/auth/login');
    expect(request.request.body).toEqual({ email: 'admin@xtrim.ec', password: 'admin123' });
    request.flush({ token: 't', email: 'admin@xtrim.ec', nombre: 'Ana', role: 'ADMIN' });
    await fixture.whenStable();

    expect(el().querySelector('[role="alert"]')).toBeNull();
  });
});
