import { Component, input, output } from '@angular/core';
import { Icon } from './icon';
@Component({ selector: 'app-feedback-panel', imports: [Icon], template: `<div class="feedback" [class.error]="kind() === 'error'" [attr.role]="kind() === 'error' ? 'alert' : 'status'">
  <p><app-icon [name]="kind() === 'error' ? 'warning' : 'circle'" />{{ message() }}</p>@if (retryable()) { <button type="button" class="secondary" (click)="retry.emit()" [disabled]="busy()">Reintentar</button> }<ng-content />
</div>`, styles: ':host{display:block;margin-bottom:var(--space-4)}.feedback{border:1px solid var(--border);border-radius:var(--radius-panel);padding:var(--space-4);background:var(--info-soft)}.error{border-color:var(--danger);background:var(--danger-soft);color:var(--danger)}p{display:flex;align-items:flex-start;gap:var(--space-2);margin:0 0 var(--space-3)}p:last-child{margin:0}app-icon{margin-top:var(--space-1)}@media(prefers-reduced-motion:no-preference){:host{animation:enter var(--duration-normal) ease-out}}' })
export class FeedbackPanel {
  readonly kind = input<'info' | 'error'>('info'); readonly message = input.required<string>(); readonly retryable = input(false); readonly busy = input(false); readonly retry = output<void>();
}
