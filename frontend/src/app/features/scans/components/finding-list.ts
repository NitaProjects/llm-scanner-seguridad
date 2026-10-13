import { Component, computed, input } from '@angular/core';
import { FindingDto } from '../models/scan';
import { StatusBadge } from '../../../shared/ui/status-badge';
import { sortFindings, confidenceLabel, confidenceHelp } from '../../../shared/presentation/labels';
import { Icon } from '../../../shared/ui/icon';
@Component({ selector: 'app-finding-list', imports: [StatusBadge, Icon], styleUrls: ['../../../shared/ui/page.css', './finding-list.css'], template: `
  <p class="meta">{{ help }}</p>@if (findings().length === 0) { <div class="empty-state"><app-icon name="document" class="empty-icon" /><h3>Sin hallazgos registrados</h3><p>No hay hallazgos registrados. Esto no demuestra seguridad.</p></div> }
  @for (finding of ordered(); track finding.id) { <article class="card finding" [attr.data-severity]="finding.severity" [id]="'finding-' + finding.id"><details><summary><span class="finding-summary"><app-status-badge [badge]="{kind:'severity',value:finding.severity}" /><strong>{{ finding.category }} · {{ finding.title }}</strong><span class="meta">{{ confidence(finding.confidence) }}</span></span></summary><div class="finding-detail"><h3>Descripción</h3><p class="text">{{ finding.description }}</p><h3>Resumen de evidencia</h3><p class="text">{{ finding.evidenceSummary }}</p><h3>Recomendación</h3><p class="text recommendation">{{ finding.recommendation }}</p></div></details></article> }
` })
export class FindingList { readonly findings = input.required<readonly FindingDto[]>(); readonly ordered = computed(() => sortFindings(this.findings())); readonly confidence = confidenceLabel; readonly help = confidenceHelp; }
