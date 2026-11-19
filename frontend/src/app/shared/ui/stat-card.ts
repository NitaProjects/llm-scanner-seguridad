import { Component, input } from '@angular/core';
import { Icon, IconName } from './icon';

@Component({
  selector: 'app-stat-card', imports: [Icon],
  template: `<div class="stat-card"><p><app-icon [name]="icon()" />{{ label() }}</p><strong>{{ value() }}</strong></div>`,
  styles: `:host{display:block;min-width:0}.stat-card{height:100%;padding:var(--space-5);border:1px solid var(--border);border-radius:var(--radius-panel);background:var(--surface-subtle)}p{display:flex;align-items:center;gap:var(--space-2);color:var(--muted);font-size:var(--text-small);margin:0 0 var(--space-3)}strong{font-size:2rem;line-height:1.2;font-variant-numeric:tabular-nums}app-icon{color:var(--accent)}`
})
export class StatCard { readonly label = input.required<string>(); readonly value = input.required<number>(); readonly icon = input<IconName>('document'); }
