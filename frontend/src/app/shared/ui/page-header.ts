import { Component, input } from '@angular/core';

@Component({
  selector: 'app-page-header',
  template: `<header class="page-header"><div><p class="eyebrow">{{ eyebrow() }}</p><h1 tabindex="-1">{{ title() }}</h1>@if (description()) { <p class="description">{{ description() }}</p> }</div><div class="header-actions"><ng-content /></div></header>`,
  styles: `:host{display:block;margin-bottom:var(--space-6)}.page-header{display:flex;align-items:center;justify-content:space-between;gap:var(--space-5);flex-wrap:wrap}.page-header>div{min-width:0}.eyebrow{color:var(--accent);font-size:.75rem;font-weight:700;letter-spacing:.1em;text-transform:uppercase;margin-bottom:var(--space-2)}h1{margin-bottom:var(--space-2)}.description{color:var(--muted);max-width:64ch;margin:0}.header-actions{display:flex;gap:var(--space-2);flex-wrap:wrap}.header-actions:empty{display:none}@media(max-width:599px){.header-actions{width:100%}}`
})
export class PageHeader {
  readonly title = input.required<string>();
  readonly description = input('');
  readonly eyebrow = input('Espacio de análisis');
}
