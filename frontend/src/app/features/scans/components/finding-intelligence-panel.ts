import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FindingDto } from '../models/scan';
import { StatusBadge } from '../../../shared/ui/status-badge';
import { confidenceHelp, confidenceLabel } from '../../../shared/presentation/labels';
import { Icon } from '../../../shared/ui/icon';

@Component({ selector: 'app-finding-intelligence-panel', imports: [StatusBadge, RouterLink, Icon], styleUrls: ['../../../shared/ui/page.css', './finding-intelligence-panel.css'], template: `
  <article class="intelligence-panel" aria-label="Finding Intelligence Panel">
    <header><p class="eyebrow">Inspección de hallazgo · {{ finding().category }}</p><h3>{{ finding().title }}</h3><p class="technical-ref">{{ finding().securityTestCode }} · revisión {{ finding().securityTestRevision }}</p></header>
    <section><h4><app-icon name="document" />EVIDENCIA</h4><p class="text description">{{ finding().description }}</p><h5>Resumen de evidencia</h5><p class="text evidence-summary">{{ finding().evidenceSummary }}</p></section>
    <section><h4><app-icon name="shield" />DECISIÓN</h4><div class="decision"><app-status-badge [badge]="{kind:'verdict',value:finding().verdict}" /><app-status-badge [badge]="{kind:'severity',value:finding().severity}" /></div><p>{{ confidence(finding().confidence) }}</p><p class="meta">{{ help }}</p></section>
    <section><h4><app-icon name="check" />RECOMENDACIÓN</h4><p class="text">{{ finding().recommendation }}</p></section>
    <section><h4><app-icon name="folder" />REFERENCIAS</h4><p class="technical-ref">{{ finding().securityTestCode }} · revisión {{ finding().securityTestRevision }}</p>
      <p class="correlation-note">{{ correlated() ? 'Referencias de ejecución y evaluación verificadas.' : 'No se ha podido verificar la cadena completa de correlación. Se conserva el contenido propio del hallazgo.' }}</p>
      <dl><dt>Hallazgo</dt><dd class="technical-ref">{{ finding().id }}</dd><dt>Evaluación</dt><dd class="technical-ref"><a [routerLink]="[]" [fragment]="'evaluation-' + finding().evaluationResultId">{{ finding().evaluationResultId }}</a></dd><dt>Ejecución</dt><dd class="technical-ref">{{ finding().testExecutionId }}</dd><dt>Scan</dt><dd class="technical-ref">{{ finding().scanId }}</dd></dl>
    </section>
  </article>` })
export class FindingIntelligencePanel {
  readonly finding = input.required<FindingDto>(); readonly correlated = input(false);
  readonly confidence = confidenceLabel; readonly help = confidenceHelp;
}
