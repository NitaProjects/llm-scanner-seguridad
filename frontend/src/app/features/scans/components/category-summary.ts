import { Component, input } from '@angular/core';
import { CategoryScoreDto } from '../models/scan';
import { StatusBadge } from '../../../shared/ui/status-badge';
import { categoryLabels } from '../../../shared/presentation/labels';
@Component({ selector: 'app-category-summary', imports: [StatusBadge], styleUrls: ['../../../shared/ui/page.css', './category-summary.css'], template: `
  <div class="categories">@for (item of categories(); track item.category) { <article class="card"><h3>{{ item.category }} · {{ labels[item.category] }}</h3><p class="number">{{ item.score }}/100</p><app-status-badge [badge]="{kind:'risk',value:item.riskLevel}" /><p>Hallazgos: {{ item.findingCount }}<br />No vulnerables en esta prueba: {{ item.notVulnerableCount }}<br />Inconclusos: {{ item.inconclusiveCount }}<br />Evaluaciones: {{ item.evaluationCount }}</p><p class="meta">Severidades: crítica {{ item.severityCounts.CRITICAL }}, alta {{ item.severityCounts.HIGH }}, media {{ item.severityCounts.MEDIUM }}, baja {{ item.severityCounts.LOW }}</p>@if (item.category === 'LLM10') { <p class="meta">Solo salida observable. No comprueba el consumidor posterior.</p> }</article> }</div>` })
export class CategorySummary { readonly categories = input.required<readonly CategoryScoreDto[]>(); readonly labels = categoryLabels; }
