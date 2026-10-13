import { Component, computed, input } from '@angular/core';
import { EvaluationMetadataDto } from '../models/scan';
import { StatusBadge } from '../../../shared/ui/status-badge';
import { confidenceLabel, outcomeLabels, publicReason } from '../../../shared/presentation/labels';
import { Icon } from '../../../shared/ui/icon';
@Component({ selector: 'app-evaluation-list', imports: [StatusBadge, Icon], styleUrl: '../../../shared/ui/page.css', template: `
  @if (rows().length === 0) { <div class="empty-state"><app-icon name="document" class="empty-icon" /><h3>Sin evaluaciones registradas</h3><p>No hay evaluaciones registradas.</p></div> }
  <ul class="plain-list">@for (item of rows(); track item.id) { <li [id]="'evaluation-' + item.id"><h3>{{ item.category }} · {{ item.securityTestCode }} (revisión {{ item.securityTestRevision }})</h3>
    @if (item.status === 'COMPLETED' && item.finalVerdict) { <app-status-badge [badge]="{kind:'verdict',value:item.finalVerdict}" />
      @if (item.confidence !== null) { <p class="meta">{{ confidence(item.confidence) }}</p> }
      @if (item.severity) { <app-status-badge [badge]="{kind:'severity',value:item.severity}" /> }
      @if (item.finalVerdict === 'INCONCLUSIVE') { @if (item.outcomeReason) { <p>{{ reasons[item.outcomeReason] }}</p> }@if (item.confidence === 0) { <p>No hay soporte suficiente para concluir. La confianza cero no mide la probabilidad de incertidumbre.</p> } }
    } @else { <app-status-badge [badge]="{kind:'status',value:item.status}" />@if (item.status === 'FAILED') { <p>Fallo técnico de evaluación. No constituye una conclusión semántica.</p><p>{{ publicReason(item.errorReason) }}</p> } }
    @if (item.degraded || item.llmStatus === 'FAILED') { <p class="warning">Evaluación con componente LLM degradado. El resultado final se mantiene.</p>@if (item.llmErrorReason) { <p>{{ publicReason(item.llmErrorReason) }}</p> } }
  </li> }</ul>` })
export class EvaluationList {
  readonly evaluations = input.required<readonly EvaluationMetadataDto[]>(); readonly confidence = confidenceLabel; readonly reasons = outcomeLabels; readonly publicReason = publicReason;
  // Proyección explícita: ningún dato interno del evaluador llega al template.
  readonly rows = computed(() => this.evaluations().map(item => ({ id: item.id, securityTestCode: item.securityTestCode, securityTestRevision: item.securityTestRevision, category: item.category, status: item.status, finalVerdict: item.finalVerdict, confidence: item.confidence, severity: item.severity, outcomeReason: item.outcomeReason, degraded: item.degraded, llmStatus: item.llmStatus, llmErrorReason: item.llmErrorReason, errorReason: item.errorReason })));
}
