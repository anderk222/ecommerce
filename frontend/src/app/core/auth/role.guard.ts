import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from './auth.service';

/**
 * Lets through only the members of the staff that hold the role the route
 * asks for.
 *
 * <p>The role is read from the data of the route: `data: { role: 'ADMIN' }`.
 */
export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  const required = route.data['role'] as 'ADMIN' | undefined;
  if (!required) {
    return true;
  }

  if (auth.signedIn() && auth.user()?.role === required) {
    return true;
  }

  return router.createUrlTree(['/admin/login']);
};
