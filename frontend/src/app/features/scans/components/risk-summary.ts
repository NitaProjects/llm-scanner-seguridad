import { Component, input } from '@angular/core';
import { ScanScoreDto } from '../models/scan';
import { StatusBadge } from '../../../shared/ui/status-badge';
import { Icon } from '../../../shared/ui/icon';
@Component({ selector: 'app-risk-summary', imports: [StatusBadge, Icon], styleUrls: ['../../../shared/ui/page.css', './risk-summary.css'], template: `
  <div class="risk-grid"><div><p class="muted">Puntuación de riesgo</p><p class="score">{{ score().globalScore }}<span>/100</span></p><app-status-badge [badge]="{kind:'risk',value:score().riskLevel}" /></div><div class="metric"><span class="metric-label"><app-icon name="document" />Hallazgos</span><span class="count">{{ score().findingCount }}</span></div><div class="metric"><span class="metric-label"><app-icon name="question" />Resultados inconclusos</span><span class="count">{{ score().inconclusiveCount }}</span></div></div>
  @if (score().globalScore === 0) { <p class="zero-note">No se detectaron Findings en las pruebas completadas. Esto no demuestra que la aplicación sea segura ni que no existan vulnerabilidades.</p> }
  @if (score().inconclusiveCount > 0) { <p class="warning">Hay {{ score().inconclusiveCount }} {{ score().inconclusiveCount === 1 ? 'resultado inconcluso' : 'resultados inconclusos' }}. No permite confirmar ni descartar vulnerabilidades.</p> }
  <p class="meta">{{ score().scoringVersion }} · Puntuación heurística, no probabilidad, porcentaje de seguridad ni cobertura.</p>
  <p class="meta">Severidades: Crítica {{ score().severityCounts.CRITICAL }} · Alta {{ score().severityCounts.HIGH }} · Media {{ score().severityCounts.MEDIUM }} · Baja {{ score().severityCounts.LOW }}</p>
` })
export class RiskSummary { readonly score = input.required<ScanScoreDto>(); }
