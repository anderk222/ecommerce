import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from '@core/auth/auth.service';
import { SeoService } from '@core/seo/seo.service';

/**
 * The panel of the administration.
 *
 * <p>It is rendered in the browser only: it needs the token of the staff
 * member, which only exists there. It carries `noindex` (HU-28).
 */
@Component({
  selector: 'app-admin',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './admin.html',
  styleUrl: './admin.scss',
})
export class Admin {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly seo = inject(SeoService);

  readonly user = this.auth.user;

  constructor() {
    this.seo.setNoIndex();
  }

  /** Ends the session and goes back to the login. */
  logout(): void {
    this.auth.logout();
    void this.router.navigate(['/admin/login']);
  }
}
