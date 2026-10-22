import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from './auth.service';

/**
 * Adds the token of the session to the calls of the administration.
 *
 * <p>The public calls do not carry it: the token is only needed by the routes
 * of the panel. When the token no longer validates (a 401 from the API — the
 * token expired, or was signed out on the server), the session ends here: the
 * keys of the storage are removed and the staff member is taken to the login,
 * where the panel explains what happened (HU-13).</p>
 */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.token();
  if (!token || !request.url.includes('/api/admin')) {
    return next(request);
  }

  return next(request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        auth.logout();
        void router.navigate(['/admin/login'], { queryParams: { sesion: 'expirada' } });
      }
      return throwError(() => error);
    }),
  );
};
