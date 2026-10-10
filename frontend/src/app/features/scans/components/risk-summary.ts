import { Component, input } from '@angular/core';
import { ScanScoreDto } from '../models/scan';
import { StatusBadge } from '../../../shared/ui/status-badge';
@Component({ selector: 'app-risk-summary', imports: [StatusBadge], styleUrl: '../../../shared/ui/page.css', styles: '.risk-grid{display:grid;grid-template-columns:1.4fr repeat(2,minmax(0,1fr));gap:1rem;align-items:center}.score{font-size:clamp(2.8rem,7vw,4.5rem);font-weight:750;letter-spacing:-.05em;margin:0}.score span{font-size:1.3rem;letter-spacing:0}.count{display:block;font-size:2.2rem;font-weight:700}.metric{border-left:1px solid var(--border);padding-left:1.2rem}@media(max-width:600px){.risk-grid{grid-template-columns:1fr}.metric{border-left:0;padding-left:0;border-top:1px solid var(--border);padding-top:1rem}}', template: `
  <div class="risk-grid"><div><p class="muted">Puntuación de riesgo</p><p class="score">{{ score().globalScore }}<span>/100</span></p><app-status-badge [badge]="{kind:'risk',value:score().riskLevel}" /></div><div class="metric">Hallazgos<span class="count">{{ score().findingCount }}</span></div><div class="metric">Resultados inconclusos<span class="count">{{ score().inconclusiveCount }}</span></div></div>
  @if (score().globalScore === 0) { <p>No se detectaron Findings en las pruebas completadas. Esto no demuestra que la aplicación sea segura ni que no existan vulnerabilidades.</p> }
  @if (score().inconclusiveCount > 0) { <p class="warning">Hay {{ score().inconclusiveCount }} {{ score().inconclusiveCount === 1 ? 'resultado inconcluso' : 'resultados inconclusos' }}. No permite confirmar ni descartar vulnerabilidades.</p> }
  <p class="meta">{{ score().scoringVersion }} · Puntuación heurística, no probabilidad, porcentaje de seguridad ni cobertura.</p>
  <p>Severidades: Crítica {{ score().severityCounts.CRITICAL }} · Alta {{ score().severityCounts.HIGH }} · Media {{ score().severityCounts.MEDIUM }} · Baja {{ score().severityCounts.LOW }}</p>
` })
export class RiskSummary { readonly score = input.required<ScanScoreDto>(); }
