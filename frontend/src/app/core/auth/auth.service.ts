import { HttpClient } from '@angular/common/http';
import { Service, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { API_BASE_URL } from '@core/api/api-base-url';
import { StoragePort } from '@core/storage/storage-port';

/**
 * The member of the staff that is signed in.
 */
export interface AuthenticatedUser {
  email: string;
  nombre: string;
  role: 'ADMIN';
}

/**
 * The session of the staff (HU-13).
 *
 * <p>The token lives in the storage of the browser and is sent in the header
 * `Authorization` by the interceptor. The session is **not** a cookie: the
 * panel is an application of the client, and the token never reaches the
 * server-rendered HTML.
 */
@Service()
export class AuthService {
  private static readonly TOKEN_KEY = 'xtrim.token';
  private static readonly USER_KEY = 'xtrim.user';

  private readonly http = inject(HttpClient);
  private readonly storage = inject(StoragePort);
  private readonly baseUrl = inject(API_BASE_URL);

  private readonly tokenState = signal<string | null>(this.storage.get(AuthService.TOKEN_KEY));
  private readonly userState = signal<AuthenticatedUser | null>(this.readUser());

  /** The token of the session, when there is one. */
  readonly token = this.tokenState.asReadonly();

  /** The member of the staff that is signed in, when there is one. */
  readonly user = this.userState.asReadonly();

  /** Whether somebody is signed in. */
  readonly signedIn = computed(() => this.tokenState() !== null);

  /** Whether the signed-in member is an administrator. */
  readonly isAdmin = computed(() => this.userState()?.role === 'ADMIN');

  /** Signs in and keeps the token. */
  login(email: string, password: string): Observable<AuthenticatedUser> {
    return this.http
      .post<{ token: string; email: string; nombre: string; role: 'ADMIN' }>(
        `${this.baseUrl}/auth/login`,
        { email, password },
      )
      .pipe(
        tap((response) => {
          this.storage.set(AuthService.TOKEN_KEY, response.token);
          this.storage.set(AuthService.USER_KEY, JSON.stringify(response));
          this.tokenState.set(response.token);
          this.userState.set({
            email: response.email,
            nombre: response.nombre,
            role: response.role,
          });
        }),
      );
  }

  /** Ends the session. */
  logout(): void {
    this.storage.remove(AuthService.TOKEN_KEY);
    this.storage.remove(AuthService.USER_KEY);
    this.tokenState.set(null);
    this.userState.set(null);
  }

  private readUser(): AuthenticatedUser | null {
    const raw = this.storage.get(AuthService.USER_KEY);
    if (!raw) {
      return null;
    }
    try {
      return JSON.parse(raw) as AuthenticatedUser;
    } catch {
      return null;
    }
  }
}
