import { Component, computed, input, output, signal } from '@angular/core';
import { sameId } from '../../../core/api/decode';
import { FindingDto } from '../models/scan';
import { StatusBadge } from '../../../shared/ui/status-badge';
import { sortFindings, confidenceLabel, confidenceHelp } from '../../../shared/presentation/labels';
import { EmptyState } from '../../../shared/ui/empty-state';
import { FindingIntelligencePanel } from './finding-intelligence-panel';
import { findingCorrelated, selectedFinding, TestRail } from '../presentation/scan-projections';
@Component({ selector: 'app-finding-list', imports: [StatusBadge, EmptyState, FindingIntelligencePanel], styleUrls: ['../../../shared/ui/page.css', './finding-list.css'], template: `
  @if (findings().length === 0) { <app-empty-state [level]="3" title="Sin hallazgos registrados" message="No hay hallazgos registrados. Esto no demuestra seguridad." /> }
  @else { <p class="meta">{{ help }}</p><div class="finding-workspace" [class.list-only]="externalInspector()"><ol class="finding-index plain-list" aria-label="Hallazgos por severidad">
    @for (finding of ordered(); track $index; let index = $index) { <li class="finding-row" [class.selected]="chosen() === finding" [attr.id]="anchor(index)" tabindex="-1">
      <div class="finding-meta"><app-status-badge [badge]="{kind:'severity',value:finding.severity}" /><span class="technical-ref">{{ finding.category }}</span></div>
      <h3>{{ finding.title }}</h3><p class="meta">{{ confidence(finding.confidence) }}</p><p class="verdict"><app-status-badge [badge]="{kind:'verdict',value:finding.verdict}" /></p>
      <p class="technical-ref">{{ finding.securityTestCode }} · revisión {{ finding.securityTestRevision }}</p>
      @if (duplicate(finding.id)) { <details><summary>Inspeccionar registro duplicado</summary><p class="warning">ID duplicado: no se atribuye este registro a una cadena de evaluación.</p><app-finding-intelligence-panel [finding]="finding" /></details> }
      @else { <button type="button" class="secondary inspect" [attr.aria-pressed]="chosen() === finding" (click)="inspect(finding.id)">{{ chosen() === finding ? 'Inspeccionando' : 'Inspeccionar' }}<span class="sr-only"> {{ finding.title }}</span></button> }
    </li> }
  </ol>@if (!externalInspector()) { <div class="finding-inspector">
    @if (chosen(); as finding) { <app-finding-intelligence-panel [finding]="finding" [correlated]="isCorrelated(finding)" /> }
    @else { <p class="selection-note">Selecciona un hallazgo para inspeccionar su evidencia y recomendación. La selección técnica actual se conserva.</p> }
  </div> }</div> }
` })
export class FindingList {
  readonly findings = input.required<readonly FindingDto[]>(); readonly rails = input<readonly TestRail[]>([]);
  readonly externalInspector = input(false);
  readonly selectedId = input<string | null | undefined>(undefined); readonly selectFinding = output<string>();
  private readonly localId = signal<string | null>(null);
  readonly ordered = computed(() => sortFindings(this.findings()));
  readonly chosen = computed(() => selectedFinding(this.ordered(), this.selectedId() === undefined ? this.localId() ?? this.ordered()[0]?.id ?? null : this.selectedId()!));
  readonly confidence = confidenceLabel; readonly help = confidenceHelp;
  inspect(id: string) { this.localId.set(id); this.selectFinding.emit(id); }
  duplicate(id: string) { return this.findings().filter(item => sameId(item.id, id)).length !== 1; }
  anchor(index: number) { const item = this.ordered()[index]; return this.ordered().findIndex(row => sameId(row.id, item.id)) === index ? 'finding-' + item.id : null; }
  isCorrelated(finding: FindingDto) { return findingCorrelated(finding, this.rails()); }
}
