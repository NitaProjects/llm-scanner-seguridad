import { StatCard } from '../../../shared/ui/stat-card';
import { Icon } from '../../../shared/ui/icon';
import { Component, input } from '@angular/core';
import { ScanDto, ScanScoreDto } from '../models/scan';
import { StatusBadge } from '../../../shared/ui/status-badge';
@Component({ selector: 'app-risk-summary', imports: [StatusBadge, StatCard, Icon], styleUrls: ['../../../shared/ui/page.css', './risk-summary.css'], template: `
  <div class="risk-grid"><div class="risk-number"><p class="muted">Puntuación de riesgo</p><p class="score">{{ score().globalScore }}<span>/100</span></p><app-status-badge [badge]="{kind:'risk',value:score().riskLevel}" /></div><div class="risk-context"><p class="source">Según resumen del score</p><div class="risk-counts"><app-stat-card label="Hallazgos" [value]="score().findingCount" /><app-stat-card label="Resultados inconclusos" icon="question" [value]="score().inconclusiveCount" /></div><p class="risk-metadata">@if (scan(); as record) { <app-status-badge [badge]="{kind:'status',value:record.status}" /> } <span><app-icon name="play" />Duración: {{ duration() ?? 'No disponible' }}</span><span class="technical-ref"><app-icon name="document" />{{ score().scoringVersion }}</span></p></div></div>
  <p class="score-meaning">Score heurístico. No representa probabilidad ni garantía de seguridad.</p>
  @if (score().globalScore === 0) { <p class="zero-note">No se detectaron Findings en las pruebas completadas. Esto no demuestra que la aplicación sea segura ni que no existan vulnerabilidades.</p> }
  @if (score().inconclusiveCount > 0) { <p class="warning">Hay {{ score().inconclusiveCount }} {{ score().inconclusiveCount === 1 ? 'resultado inconcluso' : 'resultados inconclusos' }}. No permite confirmar ni descartar vulnerabilidades.</p> }
  <details class="score-notes"><summary>Interpretación y severidades</summary><p class="meta">{{ score().scoringVersion }} · Puntuación heurística, no probabilidad, porcentaje de seguridad ni cobertura.</p>
  <p class="meta">Severidades: Crítica {{ score().severityCounts.CRITICAL }} · Alta {{ score().severityCounts.HIGH }} · Media {{ score().severityCounts.MEDIUM }} · Baja {{ score().severityCounts.LOW }}</p></details>
` })
export class RiskSummary { readonly score = input.required<ScanScoreDto>(); readonly scan = input<ScanDto | null>(null); readonly duration = input<string | null>(null); }
