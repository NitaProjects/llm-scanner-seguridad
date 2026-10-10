import { Component, computed, input } from '@angular/core';
import { FindingDto } from '../models/scan';
import { StatusBadge } from '../../../shared/ui/status-badge';
import { sortFindings, confidenceLabel, confidenceHelp } from '../../../shared/presentation/labels';
@Component({ selector: 'app-finding-list', imports: [StatusBadge], styleUrl: '../../../shared/ui/page.css', template: `
  <p class="meta">{{ help }}</p>@if (findings().length === 0) { <p>No hay hallazgos registrados. Esto no demuestra seguridad.</p> }
  @for (finding of ordered(); track finding.id) { <article class="card" [id]="'finding-' + finding.id"><details><summary><app-status-badge [badge]="{kind:'severity',value:finding.severity}" /> <strong>{{ finding.category }} · {{ finding.title }}</strong><span class="meta"> · {{ confidence(finding.confidence) }}</span></summary><h3>Descripción</h3><p class="text">{{ finding.description }}</p><h3>Resumen de evidencia</h3><p class="text">{{ finding.evidenceSummary }}</p><h3>Recomendación</h3><p class="text">{{ finding.recommendation }}</p></details></article> }
` })
export class FindingList { readonly findings = input.required<readonly FindingDto[]>(); readonly ordered = computed(() => sortFindings(this.findings())); readonly confidence = confidenceLabel; readonly help = confidenceHelp; }
