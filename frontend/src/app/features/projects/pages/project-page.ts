import { PageHeader } from '../../../shared/ui/page-header';
import { LoadingState } from '../../../shared/ui/loading-state';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription } from 'rxjs';
import { ProjectsApi } from '../data-access/projects-api';
import { TargetApi } from '../../target/data-access/target-api';
import { ScansApi } from '../../scans/data-access/scans-api';
import { ProjectDto } from '../models/project';
import { TargetConfigurationDto } from '../../target/models/target';
import { ScanDto } from '../../scans/models/scan';
import { LoadState, loading, loaded, failed } from '../../../shared/models/load-state';
import { ApiClientError } from '../../../core/api/api-error';
import { FeedbackPanel } from '../../../shared/ui/feedback-panel';
import { ProjectScanList } from '../components/project-scan-list';
import { Icon } from '../../../shared/ui/icon';
@Component({ selector: 'app-project-page', imports: [RouterLink, FeedbackPanel, ProjectScanList, Icon, PageHeader, LoadingState], styleUrl: '../../../shared/ui/page.css', template: `
  <nav aria-label="Ruta"><a routerLink="/projects">Proyectos</a></nav><app-page-header [title]="project().data?.name ?? 'Proyecto'" eyebrow="Proyecto" />
  @if (project().status === 'loading') { <app-loading-state label="Cargando proyecto…" /> }
  @if (project().error; as error) { <app-feedback-panel kind="error" [message]="error.message" [retryable]="true" (retry)="reload()" /> }
  @if (project().data; as data) {
    @if (data.description) { <p class="text project-description">{{ data.description }}</p> }
    <p class="warning">Ejecuta pruebas únicamente sobre sistemas propios o autorizados. Guardar un target no autoriza su origen para la ejecución.</p>
    <section class="card"><h2><app-icon name="settings" /> Target</h2>
      @if (target().status === 'loading') { <app-loading-state label="Cargando configuración…" /> }
      @if (target().error; as error) { <app-feedback-panel kind="error" [message]="error.message" [retryable]="true" (retry)="loadTarget()" /> }
      @if (target().status === 'loaded') {
        @if (target().data; as config) { <p><strong>{{ config.name ?? 'Target configurado' }}</strong></p><p class="text">{{ config.adapterType === 'OPENAI_COMPATIBLE' ? config.endpoint : config.url }}</p><p class="meta">Autenticación: {{ config.authType }} · {{ config.hasCredentials ? 'Credencial configurada' : 'Sin credencial configurada' }}</p><a class="button secondary" [routerLink]="['/projects', projectId(), 'target']"><app-icon name="settings" />Editar target</a> }
        @else { <p>No hay un target configurado.</p><a class="button" [routerLink]="['/projects', projectId(), 'target']"><app-icon name="settings" />Configurar target</a> }
      }
    <div class="launch-panel"><h3>Análisis de seguridad</h3><p class="muted">Plan fijo de cuatro pruebas sobre el target configurado.</p><div class="actions">@if (active(); as scan) { <a class="button" [routerLink]="['/projects', projectId(), 'scans', scan.id]"><app-icon name="play" />Ver análisis en curso</a> }
      @else if (target().data && target().status === 'loaded') { <button (click)="start()" [disabled]="starting() || ambiguous() || scans().status !== 'loaded'"><app-icon name="play" />{{ starting() ? 'Solicitando análisis…' : 'Ejecutar análisis' }}</button> }</div></div>
    </section>
    @if (startError(); as error) { <app-feedback-panel kind="error" [message]="error.message" /> }
    @if (attemptId(); as id) { <p><a [routerLink]="['/projects',projectId(),'scans',id]">Consultar intento registrado</a></p> }
    @if (ambiguous()) { <app-feedback-panel message="El inicio no pudo confirmarse. Consulta el histórico antes de volver a iniciar."><button (click)="reconcile()">Consultar histórico</button></app-feedback-panel> }
    <section><div class="page-heading"><h2>Histórico de análisis</h2><button class="secondary" (click)="loadScans()" [disabled]="scans().status === 'loading'">Actualizar histórico</button></div>
      @if (scans().status === 'loading') { <app-loading-state label="Cargando análisis…" /> }
      @if (scans().error; as error) { <app-feedback-panel kind="error" [message]="error.message" [retryable]="true" (retry)="loadScans()" /> }
      @if (scans().data; as items) { <app-project-scan-list [projectId]="projectId()" [scans]="items" /> }
    </section>
  }` })
export class ProjectPage {
  private readonly projectsApi = inject(ProjectsApi); private readonly targetApi = inject(TargetApi); private readonly scansApi = inject(ScansApi); private readonly router = inject(Router); private readonly destroyRef = inject(DestroyRef);
  private requests = new Subscription(); private targetRead?: Subscription; private scansRead?: Subscription;
  readonly projectId = signal(''); readonly project = signal<LoadState<ProjectDto>>(loading()); readonly target = signal<LoadState<TargetConfigurationDto | null>>(loading()); readonly scans = signal<LoadState<readonly ScanDto[]>>(loading());
  readonly starting = signal(false); readonly startError = signal<ApiClientError | null>(null); readonly ambiguous = signal(false); readonly attemptId = signal<string | null>(null);
  readonly active = computed(() => this.scans().data?.find(scan => scan.status === 'PENDING' || scan.status === 'RUNNING'));
  constructor() {
    inject(ActivatedRoute).paramMap.pipe(takeUntilDestroyed()).subscribe(params => { this.projectId.set(params.get('projectId')!); this.reload(); });
    this.destroyRef.onDestroy(() => this.requests.unsubscribe());
  }
  reload() {
    this.requests.unsubscribe(); this.requests = new Subscription(); this.project.set(loading()); this.target.set(loading()); this.scans.set(loading()); this.starting.set(false); this.startError.set(null); this.ambiguous.set(false); this.attemptId.set(null);
    this.requests.add(this.projectsApi.getProject(this.projectId()).subscribe({ next: data => { this.project.set(loaded(data)); this.loadTarget(); this.loadScans(); }, error: (error: ApiClientError) => this.project.set(failed(error)) }));
  }
  loadTarget() {
    this.targetRead?.unsubscribe(); this.target.set(loading());
    this.targetRead = this.targetApi.getTarget(this.projectId()).subscribe({ next: data => this.target.set(loaded(data)), error: (error: ApiClientError) => this.target.set(error.code === 'TARGET_NOT_CONFIGURED' ? loaded(null) : failed(error)) }); this.requests.add(this.targetRead);
  }
  loadScans() {
    this.scansRead?.unsubscribe(); this.scans.set(loading());
    this.scansRead = this.scansApi.list(this.projectId()).subscribe({ next: data => this.scans.set(loaded(data)), error: (error: ApiClientError) => this.scans.set(failed(error)) }); this.requests.add(this.scansRead);
  }
  reconcile() { this.loadScans(); this.ambiguous.set(false); }
  start() {
    if (this.starting() || this.ambiguous() || this.active() || !this.target().data || this.target().status !== 'loaded' || this.scans().status !== 'loaded') return;
    this.starting.set(true); this.startError.set(null); this.attemptId.set(null);
    this.requests.add(this.scansApi.start(this.projectId()).subscribe({ next: data => { this.starting.set(false); void this.router.navigate(['/projects', this.projectId(), 'scans', data.id]); }, error: (error: ApiClientError) => {
      this.starting.set(false); this.startError.set(error);
      if (error.code === 'SCAN_ALREADY_ACTIVE') this.loadScans();
      if (error.code === 'TARGET_NOT_CONFIGURED') this.loadTarget();
      if (error.code === 'SCAN_DISPATCH_FAILED' || error.code === 'INTERNAL_ERROR') this.attemptId.set(this.scansApi.attemptId(this.projectId(), error.location));
      this.ambiguous.set(error.ambiguousWrite && !this.attemptId());
    } }));
  }
}
