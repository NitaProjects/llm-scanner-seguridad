import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CategoryScoreDto } from '../models/scan';
import { StatusBadge } from '../../../shared/ui/status-badge';
import { categoryLabels } from '../../../shared/presentation/labels';
import { riskBands, TestRail } from '../presentation/scan-projections';
@Component({ selector: 'app-category-summary', imports: [StatusBadge, RouterLink], styleUrls: ['../../../shared/ui/page.css', './category-summary.css'], template: `
  <div class="risk-bands">@for (band of bands(); track band.category) {
    <article class="risk-band"><div class="band-main"><div class="band-identity"><span class="technical-ref">{{ band.category }}</span><h3>{{ labels[band.category] }}</h3></div>
      <div class="band-score">@if (band.score; as score) { <strong>{{ score.score }}<span>/100</span></strong><app-status-badge [badge]="{kind:'risk',value:score.riskLevel}" /> } @else { <span class="muted">Score no disponible</span> }</div>
      <div class="band-outcome"><span class="column-label">Resultado de evaluación</span>@if (band.rail?.evaluation; as evaluation) { @if (evaluation.status === 'COMPLETED' && evaluation.finalVerdict) { <app-status-badge [badge]="{kind:'verdict',value:evaluation.finalVerdict}" /> } @else { <span>No disponible</span><span class="meta">Evaluación: {{ evaluation.status }}</span> } } @else { <span class="muted">No disponible</span> }</div>
      <div class="band-counts">@if (band.score; as score) { <span>Hallazgos: {{ score.findingCount }}</span><span>Inconclusos: {{ score.inconclusiveCount }}</span><small>Según score</small> } @else { <span class="muted">Conteos no disponibles</span> }</div></div>
      @if (band.category === 'LLM10') { <p class="meta output-only">Solo salida observable. No comprueba el consumidor posterior.</p> }
      <details><summary>Datos y referencias de {{ band.category }}</summary>
        @if (band.score; as score) { <p class="meta">No vulnerables en esta prueba: {{ score.notVulnerableCount }} · Evaluaciones: {{ score.evaluationCount }}</p><p class="meta">Severidades: crítica {{ score.severityCounts.CRITICAL }}, alta {{ score.severityCounts.HIGH }}, media {{ score.severityCounts.MEDIUM }}, baja {{ score.severityCounts.LOW }}</p> }
        @if (band.rail?.execution; as execution) { <p class="technical-ref">{{ execution.securityTestCode }} · revisión {{ execution.securityTestRevision }}</p> }
        @if (band.rail?.warning; as warning) { <p class="warning">{{ warning }}</p> }
        @for (finding of band.rail?.findings ?? []; track finding.id) { <p><a [routerLink]="[]" [fragment]="'finding-' + finding.id">{{ finding.title }}</a></p> }
        <p class="meta">Score, evaluación y listado son fuentes independientes. Una referencia ausente no confirma ausencia de hallazgos.</p>
      </details>
    </article>
  }</div>` })
export class CategorySummary {
  readonly categories = input<readonly CategoryScoreDto[]>([]); readonly rails = input<readonly TestRail[]>([]);
  readonly labels = categoryLabels; readonly bands = computed(() => riskBands(this.categories(), this.rails()));
}
