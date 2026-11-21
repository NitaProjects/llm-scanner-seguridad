import { Component, computed, input, output } from '@angular/core';
import { DatePipe } from '@angular/common';
import { TestExecutionMetadataDto, EvaluationMetadataDto } from '../models/scan';
import { StatusBadge } from '../../../shared/ui/status-badge';
import { Category } from '../../../shared/models/contracts';
import { categoryLabels, publicReason } from '../../../shared/presentation/labels';
import { TestRail } from '../presentation/scan-projections';
import { EvaluationList } from './evaluation-list';
import { sameId } from '../../../core/api/decode';
@Component({ selector: 'app-scan-progress', imports: [StatusBadge, EvaluationList, DatePipe], styleUrls: ['../../../shared/ui/page.css', './scan-progress.css'], template: `
  @if (executions().length === 0) { <p>Todavía no hay un plan de pruebas disponible o no hay pruebas registradas para este análisis.</p> }
  @else { <p class="counter-note">{{ finishedExecutions() }} de {{ executions().length }} pruebas finalizadas técnicamente. No indica pruebas satisfactorias ni cobertura.</p> }
  <p class="counter-note">{{ finishedEvaluations() }} de {{ evaluations().length }} evaluaciones finalizadas técnicamente (lectura independiente).</p>
  @if (rails().length) { <div class="technical-workspace"><div class="rails" aria-label="Technical Test Rails">
    @for (row of rails(); track row.category) { <button type="button" class="test-rail secondary" [class.selected]="selected()?.category === row.category" [class.active]="active(row)" [attr.aria-pressed]="selected()?.category === row.category" (click)="selectTest.emit(row.category)">
      <span class="rail-heading"><span class="technical-ref">{{ row.category }}</span><span>{{ labels[row.category] }}</span></span>
      <span class="rail-stage"><span>Ejecución</span>@if (row.execution; as execution) { <app-status-badge [badge]="{kind:'status',value:execution.status}" /> } @else { <span>No recibida</span> }</span>
      <span class="rail-stage"><span>Evaluación</span>@if (row.evaluation; as evaluation) { <app-status-badge [badge]="{kind:'status',value:evaluation.status}" /> } @else { <span>No disponible</span> }</span>
      <span class="rail-stage"><span>Resultado</span>@if (row.evaluation?.status === 'COMPLETED' && row.evaluation?.finalVerdict) { <app-status-badge [badge]="{kind:'verdict',value:row.evaluation!.finalVerdict!}" /> } @else { <span>No disponible</span> }</span>
      <span class="rail-signals">{{ selected()?.category === row.category ? 'Seleccionado' : 'Inspeccionar test' }} @if (active(row)) { <span> · Actividad confirmada</span> }</span>
    </button> }
  </div><div class="technical-inspector" aria-label="Inspector técnico seleccionado">
    @if (selected(); as row) { <p class="inspector-label">APPSEC LAB · DETALLE TÉCNICO</p><h3>{{ row.category }} · {{ labels[row.category] }}</h3>
      @if (row.warning) { <p class="warning">{{ row.warning }}</p> }
      @if (row.execution; as execution) { <p class="technical-ref">{{ execution.securityTestCode }} · revisión {{ execution.securityTestRevision }}</p><p class="text">{{ execution.securityTestName }}</p>
        <dl><dt>Ejecución</dt><dd><app-status-badge [badge]="{kind:'status',value:execution.status}" /></dd><dt>HTTP / latencia</dt><dd>{{ execution.httpStatus ?? 'No disponible' }} / {{ execution.latencyMs ?? 'No disponible' }} ms</dd><dt>Inicio</dt><dd class="technical-ref">{{ execution.startedAt ? (execution.startedAt | date:'medium':'UTC') + ' UTC' : 'No disponible' }}</dd><dt>Fin</dt><dd class="technical-ref">{{ execution.finishedAt ? (execution.finishedAt | date:'medium':'UTC') + ' UTC' : 'No disponible' }}</dd></dl>
        @if (execution.errorReason) { <p class="warning">{{ reason(execution.errorReason) }}</p> }
      } @else { <p>No se ha recibido una ejecución única verificable para esta categoría.</p> }
      <h3>Evaluación y resultado</h3>@if (row.evaluation; as evaluation) { <app-evaluation-list [evaluations]="[evaluation]" [anchors]="false" /> } @else { <p>Evaluación correlacionada no disponible. Consulta también el índice de registros recibidos.</p> }
    }
  </div></div> }
  <details class="execution-index"><summary>Todos los registros de ejecución recibidos</summary><ul class="plain-list execution-list">@for (item of executions(); track $index; let index = $index) { <li [attr.id]="executionAnchor(index)" tabindex="-1"><strong>{{ item.category }} · {{ item.securityTestName }}</strong> <app-status-badge [badge]="{kind:'status',value:item.status}" /><p class="technical-ref">{{ item.securityTestCode }} · revisión {{ item.securityTestRevision }} · {{ item.id }}</p>@if (item.errorReason) { <p>{{ reason(item.errorReason) }}</p> }@if (item.httpStatus !== null) { <p class="meta">HTTP {{ item.httpStatus }} · Latencia {{ item.latencyMs ?? 'desconocida' }} ms</p> }</li> }</ul></details>` })
export class ScanProgress {
  readonly executions = input.required<readonly TestExecutionMetadataDto[]>(); readonly evaluations = input.required<readonly EvaluationMetadataDto[]>();
  readonly rails = input<readonly TestRail[]>([]); readonly selectedCategory = input<Category | null>(null); readonly selectTest = output<Category>();
  readonly executionLive = input(false); readonly evaluationLive = input(false); readonly reason = publicReason; readonly labels = categoryLabels;
  readonly selected = computed<TestRail | null>(() => this.rails().find(item => item.category === this.selectedCategory()) ?? this.rails().find(item => item.execution) ?? this.rails()[0] ?? null);
  readonly finishedExecutions = computed(() => this.executions().filter(item => item.status === 'COMPLETED' || item.status === 'FAILED').length);
  readonly finishedEvaluations = computed(() => this.evaluations().filter(item => item.status === 'COMPLETED' || item.status === 'FAILED').length);
  active(row: TestRail) { return this.executionLive() && row.execution?.status === 'RUNNING' || this.evaluationLive() && row.evaluation?.status === 'RUNNING'; }
  executionAnchor(index: number) { const row = this.executions()[index]; return this.executions().findIndex(item => sameId(item.id, row.id)) === index ? 'execution-' + row.id : null; }
}
