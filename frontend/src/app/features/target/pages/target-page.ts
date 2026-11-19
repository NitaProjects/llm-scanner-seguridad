import { PageHeader } from '../../../shared/ui/page-header';
import { LoadingState } from '../../../shared/ui/loading-state';
import { Component, DestroyRef, inject, signal, viewChild } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription } from 'rxjs';
import { TargetApi } from '../data-access/target-api';
import { TargetForm } from '../components/target-form';
import { CreateTargetRequest, TargetConfigurationDto, UpdateTargetRequest } from '../models/target';
import { ApiClientError } from '../../../core/api/api-error';
import { LoadState, loading, loaded, failed } from '../../../shared/models/load-state';
import { FeedbackPanel } from '../../../shared/ui/feedback-panel';
import { ProjectsApi } from '../../projects/data-access/projects-api';
import { ProjectDto } from '../../projects/models/project';
@Component({ selector: 'app-target-page', imports: [RouterLink, TargetForm, FeedbackPanel, PageHeader, LoadingState], styleUrl: '../../../shared/ui/page.css', template: `
  <nav aria-label="Ruta"><a routerLink="/projects">Proyectos</a> / <a [routerLink]="['/projects',projectId()]">Volver al proyecto</a></nav><app-page-header title="Configuración del target" eyebrow="Integración" description="Define la conexión, el formato de intercambio y la autenticación." />
  <p class="warning">Configura únicamente endpoints propios o autorizados. Guardar no comprueba conectividad ni autoriza tráfico.</p>
  @if (project().error; as error) { <app-feedback-panel kind="error" [message]="error.message" [retryable]="true" (retry)="load()" /> }
  @if (target().status === 'loading') { <app-loading-state label="Cargando configuración…" /> }
  @if (project().data && target().error) { <app-feedback-panel kind="error" [message]="target().error!.message" [retryable]="true" (retry)="load()" /> }
  @if (saveError(); as error) { <app-feedback-panel kind="error" [message]="error.message" /> }
  @if (reconcileRequired()) { <app-feedback-panel message="Consulta la configuración pública antes de volver a guardar. La respuesta no permite confirmar el resultado."><button (click)="load()">Cargar configuración existente</button></app-feedback-panel> }
  @if (saved()) { <app-feedback-panel message="Configuración guardada. Esto no acredita conectividad ni autorización del destino." /> }
  @if (target().status === 'loaded') { <app-target-form [original]="target().data" [busy]="saving()" [blocked]="reconcileRequired()" [error]="saveError()" (save)="save($event)" (cancel)="cancel()" /> }
` })
export class TargetPage {
  private readonly api = inject(TargetApi); private readonly projectsApi = inject(ProjectsApi); private readonly router = inject(Router); private readonly destroyRef = inject(DestroyRef); private requests = new Subscription();
  readonly project = signal<LoadState<ProjectDto>>(loading());
  readonly editor = viewChild(TargetForm); readonly projectId = signal(''); readonly target = signal<LoadState<TargetConfigurationDto | null>>(loading()); readonly saving = signal(false); readonly saved = signal(false); readonly saveError = signal<ApiClientError | null>(null); readonly reconcileRequired = signal(false);
  constructor() {
    inject(ActivatedRoute).paramMap.pipe(takeUntilDestroyed()).subscribe(params => { this.editor()?.clearSecret(); this.projectId.set(params.get('projectId')!); this.load(); });
    this.destroyRef.onDestroy(() => this.requests.unsubscribe());
  }
  load() {
    this.requests.unsubscribe(); this.requests = new Subscription(); this.project.set(loading()); this.target.set(loading()); this.saved.set(false); this.saving.set(false); this.saveError.set(null);
    this.requests.add(this.projectsApi.getProject(this.projectId()).subscribe({ next: data => { this.project.set(loaded(data)); this.loadTarget(); }, error: (error: ApiClientError) => { this.project.set(failed(error)); this.target.set(failed(error)); } }));
  }
  private loadTarget() {
    this.requests.add(this.api.getTarget(this.projectId()).subscribe({ next: data => { this.target.set(loaded(data)); this.reconcileRequired.set(false); }, error: (error: ApiClientError) => {
      if (error.code === 'TARGET_NOT_CONFIGURED') { this.target.set(loaded(null)); this.reconcileRequired.set(false); } else this.target.set(failed(error));
    } }));
  }
  save(input: UpdateTargetRequest) {
    if (this.saving() || this.reconcileRequired()) return;
    this.saving.set(true); this.saved.set(false); this.saveError.set(null);
    // El formulario garantiza secretValue en alta autenticada; no se conserva el request en estado.
    const request = this.target().data ? this.api.update(this.projectId(), input) : this.api.create(this.projectId(), input as CreateTargetRequest);
    this.requests.add(request.subscribe({ next: data => { this.saving.set(false); this.target.set(loaded(data)); this.saved.set(true); }, error: (error: ApiClientError) => {
      this.saving.set(false); this.saveError.set(error); this.editor()?.clearSecret(); this.reconcileRequired.set(error.ambiguousWrite || ['TARGET_ALREADY_CONFIGURED', 'TARGET_TYPE_REQUIRED', 'TARGET_TYPE_CHANGED'].includes(error.code ?? ''));
    } }));
  }
  cancel() { void this.router.navigate(['/projects', this.projectId()]); }
  canDeactivate() { return this.editor()?.confirmLeave() ?? true; }
}
