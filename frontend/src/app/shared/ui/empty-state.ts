import { Component, input } from '@angular/core';
import { Icon, IconName } from './icon';

@Component({
  selector: 'app-empty-state', imports: [Icon],
  template: `<section class="empty-state"><app-icon [name]="icon()" class="empty-icon" />@if (level() === 2) { <h2>{{ title() }}</h2> } @else { <h3>{{ title() }}</h3> }<p>{{ message() }}</p><ng-content /></section>`,
  styles: `:host{display:block;margin-bottom:var(--space-5)}.empty-state{padding:var(--space-5);border:1px solid var(--border);border-radius:var(--radius-panel);background:var(--surface-subtle)}app-icon{color:var(--accent);margin-bottom:var(--space-4)}p{color:var(--muted);max-width:56ch}h2{font-size:var(--text-h2)}`
})
export class EmptyState {
  readonly title = input.required<string>(); readonly message = input.required<string>(); readonly icon = input<IconName>('document');
  readonly level = input<2 | 3>(2);
}
