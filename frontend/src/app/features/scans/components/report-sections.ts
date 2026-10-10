import { Component, computed, input } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { SecurityReportDto } from '../models/report';
import { ScanScoreDto } from '../models/scan';
import { sameScore } from '../models/scan.decoder';
import { riskLabels } from '../../../shared/presentation/labels';
@Component({ selector: 'app-report-sections', imports: [DatePipe, RouterLink], styleUrl: '../../../shared/ui/page.css', template: `
  <h3>Resumen del informe</h3>
  @if (matches()) { <p>El resumen del informe coincide con el <a [routerLink]="[]" fragment="risk">resultado de riesgo mostrado arriba</a>.</p> }
  @else { <p>Resumen propio del informe: {{ report().summary.globalScore }}/100 · {{ riskLabels[report().summary.riskLevel] }} · {{ report().summary.findingCount }} hallazgos · {{ report().summary.inconclusiveCount }} inconclusos.</p> }
  <h3>Alcance</h3><dl><dt>Creación del análisis</dt><dd>{{ report().scope.scanCreatedAt | date:'medium':'UTC' }} UTC</dd><dt>Inicio del análisis</dt><dd>{{ report().scope.scanStartedAt | date:'medium':'UTC' }} UTC</dd><dt>Configuración analizada</dt><dd>{{ report().scope.targetConfigurationId }}</dd><dt>Versión del target</dt><dd>{{ report().scope.targetUpdatedAt }}</dd></dl>
  <p><a [routerLink]="[]" fragment="findings">Consultar hallazgos del análisis</a> · <a [routerLink]="[]" fragment="evaluations">Consultar evaluaciones del análisis</a></p>
  <h3>Interpretación</h3><p class="text">{{ report().interpretation.scoreMeaning }}</p><p class="text">{{ report().interpretation.confidenceMeaning }}</p><p class="text">{{ report().interpretation.verdictMeaning }}</p><p class="text">{{ report().interpretation.technicalFailureMeaning }}</p>
  <h3>Limitaciones</h3><ol>@for (limitation of report().limitations; track limitation.code) { <li><p class="text">{{ limitation.text }}</p></li> }</ol>
  <details><summary>Trazabilidad del informe</summary><dl><dt>Informe</dt><dd>{{ report().reportId }}</dd><dt>Análisis</dt><dd>{{ report().scanId }}</dd><dt>Proyecto</dt><dd>{{ report().projectId }}</dd><dt>Generado</dt><dd>{{ report().generatedAt }}</dd><dt>Versión / idioma</dt><dd>{{ report().reportVersion }} / {{ report().locale }}</dd><dt>Scoring</dt><dd>{{ report().summary.scoringVersion }}</dd></dl>
    @for (item of report().evaluations; track item.id) { <details><summary>Referencias de {{ item.category }}</summary><dl><dt>Evaluación</dt><dd><a [routerLink]="[]" [fragment]="'evaluation-' + item.id">{{ item.id }}</a></dd><dt>Ejecución</dt><dd>{{ item.testExecutionId }}</dd><dt>Criterio / versión</dt><dd>{{ item.criteriaId ?? 'No disponible' }} / {{ item.criteriaVersion ?? 'No disponible' }}</dd><dt>Motor</dt><dd>{{ item.versions.engineVersion ?? 'No disponible' }}</dd><dt>Política</dt><dd>{{ item.versions.policyVersion ?? 'No disponible' }}</dd><dt>Confianza / severidad</dt><dd>{{ item.versions.confidenceVersion ?? 'No disponible' }} / {{ item.versions.severityVersion ?? 'No disponible' }}</dd><dt>Prompt / schema / perfil</dt><dd>{{ item.versions.promptVersion ?? 'No disponible' }} / {{ item.versions.schemaVersion ?? 'No disponible' }} / {{ item.versions.llmInputProfile ?? 'No disponible' }}</dd>@if (item.findingId) { <dt>Hallazgo</dt><dd><a [routerLink]="[]" [fragment]="'finding-' + item.findingId">{{ item.findingId }}</a></dd> }</dl></details> }
  </details>` })
export class ReportSections {
  readonly report = input.required<SecurityReportDto>(); readonly score = input<ScanScoreDto | null>(null); readonly riskLabels = riskLabels;
  readonly matches = computed(() => { const score = this.score(); return score !== null && sameScore(score, this.report().summary); });
}
