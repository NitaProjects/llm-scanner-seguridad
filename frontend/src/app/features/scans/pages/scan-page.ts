import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ScanDetailState } from '../state/scan-detail-state';
import { RiskSummary } from '../components/risk-summary';
import { CategorySummary } from '../components/category-summary';
import { FindingList } from '../components/finding-list';
import { EvaluationList } from '../components/evaluation-list';
import { ScanProgress } from '../components/scan-progress';
import { ReportSections } from '../components/report-sections';
import { FeedbackPanel } from '../../../shared/ui/feedback-panel';
import { StatusBadge } from '../../../shared/ui/status-badge';
import { Availability } from '../../../shared/models/contracts';
import { publicReason } from '../../../shared/presentation/labels';
import { sameScore } from '../models/scan.decoder';
@Component({ selector: 'app-scan-page', imports: [RouterLink, DatePipe, RiskSummary, CategorySummary, FindingList, EvaluationList, ScanProgress, ReportSections, FeedbackPanel, StatusBadge], providers: [ScanDetailState], templateUrl: './scan-page.html', styleUrl: '../../../shared/ui/page.css' })
export class ScanPage {
  readonly state = inject(ScanDetailState); readonly projectId = signal(''); readonly reason = publicReason;
  readonly terminal = computed(() => this.state.scan().data?.status === 'COMPLETED' || this.state.scan().data?.status === 'FAILED');
  readonly score = computed(() => { const data = this.state.score().data; return this.state.score().status === 'loaded' && data?.availability === 'AVAILABLE' ? data.score : null; });
  readonly report = computed(() => { const data = this.state.report().data; return this.state.report().status === 'loaded' && data?.availability === 'AVAILABLE' ? data.report : null; });
  readonly conflict = computed(() => { const score = this.score(); const report = this.report(); return score && report ? !sameScore(score, report.summary) : false; });
  readonly duration = computed(() => { const scan = this.state.scan().data; return scan?.startedAt && scan.finishedAt ? `${Math.max(0, Date.parse(scan.finishedAt) - Date.parse(scan.startedAt)) / 1000} s` : 'Desconocida / no iniciado'; });
  constructor() { inject(ActivatedRoute).paramMap.pipe(takeUntilDestroyed()).subscribe(params => { this.projectId.set(params.get('projectId')!); this.state.open(this.projectId(), params.get('scanId')!); }); }
  availabilityText(value: Availability): string {
    switch (value) {
      case 'NOT_APPLICABLE': return 'Esta funcionalidad no aplica a este análisis histórico. Este análisis fue creado antes de esta funcionalidad.';
      case 'UNAVAILABLE': return 'No disponible: el análisis no pudo completarse.';
      case 'PENDING': return 'Resultado pendiente después de un estado terminal: respuesta incoherente. Relee este bloque para confirmar.';
      case 'AVAILABLE': return '';
    }
  }
}
