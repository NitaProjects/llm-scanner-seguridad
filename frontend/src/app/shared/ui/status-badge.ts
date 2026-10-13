import { Component, computed, input } from '@angular/core';
import { RiskLevel, Severity, Lifecycle, Verdict } from '../models/contracts';
import { riskLabels, severityLabels, statusLabels, verdictLabels } from '../presentation/labels';
import { Icon } from './icon';
type Badge = { readonly kind: 'risk'; readonly value: RiskLevel } | { readonly kind: 'severity'; readonly value: Severity } | { readonly kind: 'status'; readonly value: Lifecycle } | { readonly kind: 'verdict'; readonly value: Verdict };
@Component({ selector: 'app-status-badge', imports: [Icon], styleUrl: './status-badge.css', template: `<span class="badge" [attr.data-tone]="badge().value">
  @switch (badge().value) {
    @case ('LOW') { <app-icon name="shield" class="badge-icon" /> }
    @case ('MEDIUM') { <app-icon name="warning" class="badge-icon" /> }
    @case ('HIGH') { <app-icon name="warning" class="badge-icon" /> }
    @case ('CRITICAL') { <app-icon name="warning" class="badge-icon" /> }
    @case ('FAILED') { <app-icon name="warning" class="badge-icon" /> }
    @case ('VULNERABLE') { <app-icon name="warning" class="badge-icon" /> }
    @case ('INCONCLUSIVE') { <app-icon name="question" class="badge-icon" /> }
    @case ('COMPLETED') { <app-icon name="check" class="badge-icon" /> }
    @case ('RUNNING') { <app-icon name="play" class="badge-icon" /> }
    @default { <app-icon name="circle" class="badge-icon" /> }
  }{{ label() }}</span>` })
export class StatusBadge {
  readonly badge = input.required<Badge>();
  readonly label = computed(() => {
    const badge = this.badge();
    switch (badge.kind) {
      case 'risk': return badge.value === 'NO_FINDINGS' ? riskLabels[badge.value] : 'Riesgo ' + riskLabels[badge.value].toLowerCase();
      case 'severity': return 'Severidad ' + severityLabels[badge.value].toLowerCase();
      case 'status': return statusLabels[badge.value];
      case 'verdict': return verdictLabels[badge.value];
    }
  });
}
