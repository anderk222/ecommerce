import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { AuthService } from '@core/auth/auth.service';
import { email, form, FormField, required } from '@angular/forms/signals';

/**
 * The login of the panel (HU-13).
 *
 * <p>Built on the shape the signal forms pass declared (P38): one signal model, the
 * `form()` schema beside it, no public mutable fields, and the conversation with the
 * visitor carried by two signals through the single `report(text, failed)` — the way
 * `stock-notify` speaks. An empty submit never knocks the door: the general message
 * walks the member to the fix, and the server's word arrives verbatim when there is
 * a real refusal (P10 D5.1, P26).</p>
 */
@Component({
  selector: 'app-login',
  imports: [FormField],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  private readonly loginModel = signal({
    email: '',
    password: '',
  });

  readonly loginForm = form(this.loginModel, (schemaPath) => {
    required(schemaPath.email, { message: 'Este campo es requerido' });
    email(schemaPath.email, { message: 'De ser un email' })
    required(schemaPath.password, { message: 'Este campo es requerido' });
  });

  /**
   * Whether a submit was attempted — the contract's second condition of visibility:
   * an untouched input that never tried to submit stays quiet.
   */
  readonly attempted = signal(false);

  readonly message = signal<string | null>(null);

  /** Whether the last answer was a refusal or a failure. */
  readonly failed = signal(false);

  readonly busy = signal(false);

  /** The notice that greets the staff member when the session just expired. */
  readonly notice = signal<string | null>(null);

  constructor() {
    // The interceptor sent them here with `sesion=expirada`: the token no
    // longer validates, so the panel says it in the words of the house —
    // not in the words of the HTTP error.
    if (this.route.snapshot.queryParamMap.get('sesion') === 'expirada') {
      this.notice.set('Tu sesión ha expirado. Ingresa de nuevo para continuar.');
    }

    // A session that is already open goes straight to the panel.
    if (this.auth.signedIn()) {
      void this.router.navigate(['/admin']);
    }
  }

  /** Signs in and goes to the panel. */
  submit(event: Event): void {
    event.preventDefault();
    this.attempted.set(true);
    if (this.busy()) {
      return;
    }
    if (this.loginForm().invalid()) {
      this.report('Revisa los campos marcados antes de entrar.', true);
      return;
    }
    this.busy.set(true);
    this.report(null, false);

    this.auth.login(this.email.trim(), this.password).subscribe({
      next: () => {
        this.busy.set(false);
        this.message.set(null);
        const redirect = this.route.snapshot.queryParamMap.get('redirect') ?? '/admin';
        void this.router.navigateByUrl(redirect);
      },
      error: (error: { error?: { message?: string } }) => {
        this.busy.set(false);
        this.report(error?.error?.message ?? 'No se pudo iniciar la sesión', true);
      },
    });
  }

  /** Writes the message of the conversation, with the voice of its outcome. */
  private report(text: string | null, failed: boolean): void {
    this.message.set(text);
    this.failed.set(failed);
  }

  get email(): string {
    return this.loginModel().email;
  }

  get password(): string {
    return this.loginModel().password;
  }
}
