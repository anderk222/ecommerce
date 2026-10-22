import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * The footer of the site.
 */
@Component({
  selector: 'app-footer',
  imports: [RouterLink],
  templateUrl: './footer.html',
})
export class Footer {
  readonly year = new Date().getFullYear();
}
