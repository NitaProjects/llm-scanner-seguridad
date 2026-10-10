import { Component, input, output } from '@angular/core';
@Component({ selector: 'app-feedback-panel', template: `<div class="feedback" [class.error]="kind() === 'error'" [attr.role]="kind() === 'error' ? 'alert' : 'status'">
  <p>{{ message() }}</p>@if (retryable()) { <button type="button" (click)="retry.emit()" [disabled]="busy()">Reintentar</button> }<ng-content />
</div>`, styles: '.feedback{border:1px solid #b7c8db;border-radius:.6rem;padding:1rem;background:#f5f8fc;margin-block:1rem}.error{border-color:#be6670;background:#fff5f5;color:#8e202a}p{margin:0 0 .6rem}p:last-child{margin:0}' })
export class FeedbackPanel {
  readonly kind = input<'info' | 'error'>('info'); readonly message = input.required<string>(); readonly retryable = input(false); readonly busy = input(false); readonly retry = output<void>();
}
