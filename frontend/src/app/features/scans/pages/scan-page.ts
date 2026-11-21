import { PageHeader } from '../../../shared/ui/page-header';
import { LoadingState } from '../../../shared/ui/loading-state';
import { Component, ElementRef, afterRenderEffect, computed, effect, inject, signal, untracked } from '@angular/core';
import { ActivatedRoute, NavigationSkipped, Router, RouterLink } from '@angular/router';
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
import { Category } from '../../../shared/models/contracts';
import { findingCorrelated, selectedFinding, testRails } from '../presentation/scan-projections';
import { sortFindings } from '../../../shared/presentation/labels';
import { FindingIntelligencePanel } from '../components/finding-intelligence-panel';
import { Icon } from '../../../shared/ui/icon';
@Component({ selector: 'app-scan-page', imports: [RouterLink, DatePipe, RiskSummary, CategorySummary, FindingList, EvaluationList, ScanProgress, ReportSections, FeedbackPanel, StatusBadge, PageHeader, LoadingState, FindingIntelligencePanel, Icon], providers: [ScanDetailState], templateUrl: './scan-page.html', styleUrls: ['../../../shared/ui/page.css', './scan-page.css'] })
export class ScanPage {
  readonly state = inject(ScanDetailState); readonly projectId = signal(''); readonly reason = publicReason;
  readonly scanId = signal(''); readonly fragment = signal<string | null>(null);
  readonly selection = signal<{ kind: 'test'; category: Category } | { kind: 'finding'; id: string } | null>(null);
  readonly keepTechnicalOpen = signal(false);
  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef);
  private selectedFragment = ''; private revealedFragment = '';
  private readonly referenceRequest = signal(0);
  readonly rails = computed(() => testRails(this.scanId(), this.state.executions().data ?? [], this.state.evaluations().data?.items ?? [], this.state.findings().data ?? []));
  readonly selectedCategory = computed(() => {
    const selection = this.selection();
    if (selection?.kind === 'test') return selection.category;
    if (selection?.kind === 'finding') return this.rails().find(row => row.findings.some(item => item.id.toLowerCase() === selection.id.toLowerCase()))?.category ?? null;
    return null;
  });
  readonly selectedFindingId = computed(() => {
    const selection = this.selection();
    return selection?.kind === 'finding' ? selection.id : selection?.kind === 'test' ? this.rails().find(row => row.category === selection.category)?.findings[0]?.id ?? null : null;
  });
  readonly findingPanel = computed(() => selectedFinding(this.state.findings().data ?? [], this.selectedFindingId()));
  readonly findingPanelCorrelated = computed(() => { const finding = this.findingPanel(); return finding ? findingCorrelated(finding, this.rails()) : false; });
  readonly executionLive = computed(() => this.tracking() && this.state.executions().data !== null && !this.state.executions().error);
  readonly evaluationLive = computed(() => this.tracking() && this.state.evaluations().data !== null && !this.state.evaluations().error);
  readonly title = computed(() => {
    switch (this.state.scan().data?.status) {
      case 'PENDING': return 'Análisis pendiente';
      case 'RUNNING': return 'Análisis en ejecución';
      case 'COMPLETED': return 'Análisis completado';
      case 'FAILED': return 'Análisis fallido';
      default: return 'Resultado del análisis';
    }
  });
  readonly tracking = computed(() => {
    const status = this.state.scan().data?.status;
    return (status === 'RUNNING' || status === 'PENDING') && this.state.scan().status === 'loaded' && !this.state.scan().error && !this.state.paused();
  });
  readonly terminal = computed(() => this.state.scan().data?.status === 'COMPLETED' || this.state.scan().data?.status === 'FAILED');
  readonly score = computed(() => { const data = this.state.score().data; return this.state.score().status === 'loaded' && data?.availability === 'AVAILABLE' ? data.score : null; });
  readonly report = computed(() => { const data = this.state.report().data; return this.state.report().status === 'loaded' && data?.availability === 'AVAILABLE' ? data.report : null; });
  readonly conflict = computed(() => { const score = this.score(); const report = this.report(); return score && report ? !sameScore(score, report.summary) : false; });
  readonly duration = computed(() => {
    const scan = this.state.scan().data; const start = scan?.startedAt ? Date.parse(scan.startedAt) : NaN; const end = scan?.finishedAt ? Date.parse(scan.finishedAt) : NaN;
    return Number.isFinite(start) && Number.isFinite(end) && end >= start ? `${(end - start) / 1000} s` : null;
  });
  constructor() {
    const route = inject(ActivatedRoute);
    route.paramMap.pipe(takeUntilDestroyed()).subscribe(params => {
      this.projectId.set(params.get('projectId')!); this.scanId.set(params.get('scanId')!);
      this.selection.set(null); this.keepTechnicalOpen.set(false); this.selectedFragment = ''; this.revealedFragment = '';
      this.state.open(this.projectId(), this.scanId());
    });
    route.fragment.pipe(takeUntilDestroyed()).subscribe(fragment => this.fragment.set(fragment));
    const router = inject(Router);
    router.events.pipe(takeUntilDestroyed()).subscribe(event => {
      if (event instanceof NavigationSkipped && event.url === router.url && this.fragment()) {
        this.selectedFragment = ''; this.revealedFragment = ''; this.referenceRequest.update(value => value + 1);
      }
    });
    effect(() => {
      this.referenceRequest();
      const fragment = this.fragment(); const items = this.state.findings().data ?? [];
      const rails = this.rails();
      const key = this.scanId() + '#' + fragment;
      if (fragment?.startsWith('evaluation-') || fragment?.startsWith('execution-')) {
        const evaluation = fragment.startsWith('evaluation-');
        const id = fragment.slice(evaluation ? 'evaluation-'.length : 'execution-'.length);
        const rail = rails.find(row => (evaluation ? row.evaluation?.id : row.execution?.id)?.toLowerCase() === id.toLowerCase());
        if (rail && key !== this.selectedFragment) {
          this.selectedFragment = key; this.selection.set({ kind: 'test', category: rail.category }); this.keepTechnicalOpen.set(true);
        }
        return;
      }
      if (fragment?.startsWith('finding-')) {
        const finding = selectedFinding(items, fragment.slice('finding-'.length));
        if (finding && key !== this.selectedFragment) { this.selectedFragment = key; this.selection.set({ kind: 'finding', id: finding.id }); }
        return;
      }
      this.selectedFragment = '';
      if (!untracked(this.selection) && this.terminal() && this.state.findings().status === 'loaded') {
        const first = sortFindings(items)[0]; if (first) this.selection.set({ kind: 'finding', id: first.id });
      }
    });
    afterRenderEffect(() => {
      this.referenceRequest();
      const fragment = this.fragment(); this.state.findings().data; this.state.evaluations().data; this.state.executions().data; this.report();
      const key = this.scanId() + '#' + fragment;
      if (!fragment || key === this.revealedFragment) return;
      const target = Array.from(this.element.nativeElement.querySelectorAll<HTMLElement>('[id]')).find(item => item.id.toLowerCase() === fragment.toLowerCase());
      if (!target) return;
      let parent = target.parentElement;
      while (parent && parent !== this.element.nativeElement) { if (parent instanceof HTMLDetailsElement) parent.open = true; parent = parent.parentElement; }
      if (fragment === 'evaluations' || fragment === 'executions') { const details = target.querySelector('details'); if (details) details.open = true; }
      this.revealedFragment = key;
      target.tabIndex = -1; target.scrollIntoView({ block: 'start' }); target.focus({ preventScroll: true });
    });
  }
  inspectTest(category: Category) { this.selection.set({ kind: 'test', category }); this.keepTechnicalOpen.set(true); }
  inspectFinding(id: string) { this.selection.set({ kind: 'finding', id }); }
  availabilityText(value: Availability): string {
    switch (value) {
      case 'NOT_APPLICABLE': return 'Esta funcionalidad no aplica a este análisis histórico. Este análisis fue creado antes de esta funcionalidad.';
      case 'UNAVAILABLE': return 'No disponible: el análisis no pudo completarse.';
      case 'PENDING': return 'Resultado pendiente después de un estado terminal: respuesta incoherente. Relee este bloque para confirmar.';
      case 'AVAILABLE': return '';
    }
  }
}
