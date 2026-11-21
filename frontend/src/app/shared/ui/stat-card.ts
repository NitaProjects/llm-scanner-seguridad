import { Component, input } from '@angular/core';
import { Icon, IconName } from './icon';

@Component({
  selector: 'app-stat-card', imports: [Icon],
  template: `<div class="stat-card"><span class="stat-icon"><app-icon [name]="icon()" /></span><div><strong>{{ value() }}</strong><p>{{ label() }}</p></div></div>`,
  styles: `:host{display:block;min-width:0}.stat-card{display:flex;align-items:center;gap:12px;height:100%;padding:12px 16px;border-left:1px solid var(--border)}p{color:var(--muted);font-size:13px;margin:4px 0 0}strong{font-size:24px;line-height:1.2;font-weight:650;font-variant-numeric:tabular-nums}.stat-icon{display:flex;align-items:center;justify-content:center;color:var(--accent)}:host(.collection-stat) .stat-card{border:0;padding:0}:host(.collection-stat) .stat-icon{width:44px;height:44px;border-radius:8px;background:var(--accent-soft)}:host(.collection-stat) app-icon{width:26px;height:26px}:host(.collection-stat) strong{font-size:32px;line-height:1}:host(.collection-stat) p{font-size:15px;color:var(--text)}`
})
export class StatCard { readonly label = input.required<string>(); readonly value = input.required<number>(); readonly icon = input<IconName>('document'); }
