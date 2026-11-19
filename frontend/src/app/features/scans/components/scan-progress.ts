import { Component, computed, input } from '@angular/core';
import { TestExecutionMetadataDto, EvaluationMetadataDto } from '../models/scan';
import { StatusBadge } from '../../../shared/ui/status-badge';
import { publicReason } from '../../../shared/presentation/labels';
@Component({ selector: 'app-scan-progress', imports: [StatusBadge], styleUrl: '../../../shared/ui/page.css', template: `
  @if (executions().length === 0) { <p>Todavía no hay un plan de pruebas disponible o no hay pruebas registradas para este análisis.</p> }
  @else { <p>{{ finishedExecutions() }} de {{ executions().length }} pruebas finalizadas técnicamente. No indica pruebas satisfactorias ni cobertura.</p> }
  <p>{{ finishedEvaluations() }} de {{ evaluations().length }} evaluaciones finalizadas técnicamente (lectura independiente).</p>
  <ul class="plain-list execution-list">@for (item of executions(); track item.id) { <li><strong>{{ item.category }} · {{ item.securityTestName }}</strong> <app-status-badge [badge]="{kind:'status',value:item.status}" />@if (item.errorReason) { <p>{{ reason(item.errorReason) }}</p> }@if (item.httpStatus !== null) { <p class="meta">HTTP {{ item.httpStatus }} · Latencia {{ item.latencyMs ?? 'desconocida' }} ms</p> }</li> }</ul>` })
export class ScanProgress {
  readonly executions = input.required<readonly TestExecutionMetadataDto[]>(); readonly evaluations = input.required<readonly EvaluationMetadataDto[]>(); readonly reason = publicReason;
  readonly finishedExecutions = computed(() => this.executions().filter(item => item.status === 'COMPLETED' || item.status === 'FAILED').length);
  readonly finishedEvaluations = computed(() => this.evaluations().filter(item => item.status === 'COMPLETED' || item.status === 'FAILED').length);
}
