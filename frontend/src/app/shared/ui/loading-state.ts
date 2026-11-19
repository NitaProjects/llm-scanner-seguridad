import { Component, input } from '@angular/core';
import { Icon } from './icon';

@Component({
  selector: 'app-loading-state', imports: [Icon],
  template: `<div class="loading-state" role="status" aria-busy="true"><app-icon name="circle" /><span>{{ label() }}</span></div>`,
  styles: `:host{display:block;margin-bottom:var(--space-4)}.loading-state{display:flex;align-items:center;gap:var(--space-3);padding:var(--space-5);border:1px solid var(--border);border-radius:var(--radius-panel);background:var(--surface-subtle);color:var(--muted)}app-icon{color:var(--accent)}`
})
export class LoadingState { readonly label = input.required<string>(); }
