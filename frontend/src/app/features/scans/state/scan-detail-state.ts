import { DestroyRef, Injectable, WritableSignal, inject, signal } from '@angular/core';
import { EMPTY, Observable, Subscription, catchError, defer, expand, forkJoin, map, of, switchMap, tap, timer } from 'rxjs';
import { ApiClientError, contractError, mapApiError } from '../../../core/api/api-error';
import { LoadState, failed, loaded, loading } from '../../../shared/models/load-state';
import { ScansApi } from '../data-access/scans-api';
import { ScanDto, TestExecutionMetadataDto, EvaluationListingDto, FindingDto, ScoreResponse } from '../models/scan';
import { ReportResponse } from '../models/report';
import { sameId } from '../../../core/api/decode';
export type ScanBlock = 'executions' | 'evaluations' | 'findings' | 'score' | 'report';
@Injectable()
export class ScanDetailState {
  private readonly api = inject(ScansApi); private owner = new Subscription(); private extras = new Subscription();
  private projectId = ''; private scanId = ''; private finalLoaded = false;
  readonly scan = signal<LoadState<ScanDto>>(loading()); readonly executions = signal<LoadState<readonly TestExecutionMetadataDto[]>>(loading());
  readonly evaluations = signal<LoadState<EvaluationListingDto>>(loading()); readonly findings = signal<LoadState<readonly FindingDto[]>>(loading());
  readonly score = signal<LoadState<ScoreResponse>>(loading()); readonly report = signal<LoadState<ReportResponse>>(loading());
  readonly paused = signal(false); readonly reading = signal(false); readonly failures = signal(0); readonly lastUpdated = signal<Date | null>(null);
  constructor() { inject(DestroyRef).onDestroy(() => { this.owner.unsubscribe(); this.extras.unsubscribe(); }); }
  open(projectId: string, scanId: string) {
    this.owner.unsubscribe(); this.extras.unsubscribe(); this.extras = new Subscription(); this.projectId = projectId; this.scanId = scanId; this.finalLoaded = false;
    this.scan.set(loading()); this.executions.set(loading()); this.evaluations.set(loading()); this.findings.set(loading()); this.score.set(loading()); this.report.set(loading());
    this.paused.set(false); this.failures.set(0); this.lastUpdated.set(null); this.reading.set(false); this.follow(false);
  }
  retry() { if (!this.paused() || this.reading()) return; this.follow(true); }
  private follow(manual: boolean) {
    this.owner.unsubscribe();
    this.owner = this.cycle(manual).pipe(expand(again => again ? timer(3000).pipe(switchMap(() => this.cycle(false))) : EMPTY)).subscribe();
  }
  private cycle(manual: boolean): Observable<boolean> {
    return defer(() => {
      this.reading.set(true);
      return this.api.getScan(this.projectId, this.scanId).pipe(
        map(scan => { if (!sameId(scan.projectId, this.projectId) || !sameId(scan.id, this.scanId)) throw contractError(); return { scan, error: null }; }),
        catchError(error => of({ scan: null, error: mapApiError(error) })),
        switchMap(outcome => {
          if (outcome.error) {
            this.failures.update(value => value + 1); this.scan.set(failed(outcome.error, this.scan().data));
            const stop = manual || !outcome.error.transient || this.failures() >= 3;
            this.paused.set(stop); this.reading.set(false); return of(!stop);
          }
          const scan = outcome.scan!;
          this.scan.set(loaded(scan)); this.failures.set(0); this.paused.set(false); this.lastUpdated.set(new Date());
          const terminal = scan.status === 'COMPLETED' || scan.status === 'FAILED';
          if (terminal && this.finalLoaded) { this.reading.set(false); return of(false); }
          if (terminal) this.finalLoaded = true;
          const blocks: ScanBlock[] = terminal ? ['executions', 'evaluations', 'findings', 'score', 'report'] : ['executions', 'evaluations'];
          return forkJoin(blocks.map(block => this.readBlock(block))).pipe(map(() => { this.reading.set(false); return !terminal; }));
        })
      );
    });
  }
  retryBlock(block: ScanBlock) {
    if (!this.finalLoaded) return;
    const state = this.blockState(block);
    if (state.status === 'loading') return;
    this.extras.add(this.readBlock(block).subscribe());
  }
  private blockState(block: ScanBlock): LoadState<unknown> {
    switch (block) { case 'executions': return this.executions(); case 'evaluations': return this.evaluations(); case 'findings': return this.findings(); case 'score': return this.score(); case 'report': return this.report(); }
  }
  private readBlock(block: ScanBlock): Observable<unknown> {
    switch (block) {
      case 'executions': return this.capture(this.executions, this.api.executions(this.projectId, this.scanId));
      case 'evaluations': return this.capture(this.evaluations, this.api.evaluations(this.projectId, this.scanId));
      case 'findings': return this.capture(this.findings, this.api.findings(this.projectId, this.scanId));
      case 'score': return this.capture(this.score, this.api.score(this.projectId, this.scanId));
      case 'report': return this.capture(this.report, this.api.report(this.projectId, this.scanId));
    }
  }
  private capture<T>(state: WritableSignal<LoadState<T>>, request: Observable<T>): Observable<unknown> {
    state.set(loading(state().data));
    return request.pipe(tap(data => state.set(loaded(data))), catchError(error => { state.set(failed(mapApiError(error), state().data)); return of(null); }));
  }
}
