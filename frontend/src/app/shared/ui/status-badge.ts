import { Component, computed, input } from '@angular/core';
import { RiskLevel, Severity, Lifecycle, Verdict } from '../models/contracts';
import { riskLabels, severityLabels, statusLabels, verdictLabels } from '../presentation/labels';
type Badge = { readonly kind: 'risk'; readonly value: RiskLevel } | { readonly kind: 'severity'; readonly value: Severity } | { readonly kind: 'status'; readonly value: Lifecycle } | { readonly kind: 'verdict'; readonly value: Verdict };
@Component({ selector: 'app-status-badge', template: '<span class="badge" [attr.data-tone]="badge().value">{{ label() }}</span>', styles: '.badge{display:inline-block;border:1px solid currentColor;border-radius:999px;padding:.25rem .65rem;font-size:.85rem;font-weight:650;color:#334155;background:#f1f5f9}.badge[data-tone=LOW]{color:#164a83;background:#eff6ff}.badge[data-tone=MEDIUM],.badge[data-tone=INCONCLUSIVE]{color:#754500;background:#fff8e6}.badge[data-tone=HIGH]{color:#8a330b;background:#fff3ed}.badge[data-tone=CRITICAL],.badge[data-tone=FAILED],.badge[data-tone=VULNERABLE]{color:#991b1b;background:#fff1f2}' })
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
