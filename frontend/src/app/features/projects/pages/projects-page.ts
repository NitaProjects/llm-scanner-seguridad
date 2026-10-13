import { Component, DestroyRef, ElementRef, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subscription } from 'rxjs';
import { ProjectsApi } from '../data-access/projects-api';
import { ProjectDto, CreateProjectRequest } from '../models/project';
import { ProjectCreateForm } from '../components/project-create-form';
import { ApiClientError } from '../../../core/api/api-error';
import { LoadState, loading, loaded, failed } from '../../../shared/models/load-state';
import { FeedbackPanel } from '../../../shared/ui/feedback-panel';
import { Icon } from '../../../shared/ui/icon';
@Component({
  selector: 'app-projects-page', imports: [RouterLink, DatePipe, ProjectCreateForm, FeedbackPanel, Icon], styleUrl: '../../../shared/ui/page.css',
  template: `<div class="page-heading"><div><h1 tabindex="-1">Proyectos</h1><p class="muted">Organiza tus análisis de aplicaciones LLM autorizadas.</p></div><button id="new-project" (click)="showForm.set(true)" [disabled]="showForm()"><app-icon name="plus" />Nuevo proyecto</button></div>
    @if (showForm()) { <app-project-create-form [busy]="creating()" [error]="createError()" (create)="create($event)" (cancel)="cancel()" /> }
    @if (createError(); as error) { <app-feedback-panel kind="error" [message]="error.message" /> }
    @if (ambiguous()) { <app-feedback-panel message="No se pudo confirmar la creación. Consulta el listado antes de volver a crear."><button (click)="reconcile()">Consultar listado</button></app-feedback-panel> }
    @if (projects().status === 'loading') { <app-feedback-panel message="Cargando proyectos…" /> }
    @if (projects().error; as error) { <app-feedback-panel kind="error" [message]="error.message" [retryable]="true" (retry)="load()" /> }
    @if (projects().status === 'loaded') {
      @if (projects().data?.length === 0 && !showForm()) { <section class="empty-state"><app-icon name="folder" class="empty-icon" /><h2>Todavía no hay proyectos</h2><p>Crea un proyecto para configurar su target y ejecutar el primer análisis.</p><button (click)="showForm.set(true)" [disabled]="showForm()"><app-icon name="plus" />Nuevo proyecto</button></section> }
      <ul class="plain-list project-list">@for (project of projects().data; track project.id) { <li class="card"><h2><a class="row-link" [routerLink]="['/projects', project.id]">{{ project.name }}</a></h2>@if (project.description) { <p class="text muted">{{ project.description }}</p> }<p class="meta">Creado {{ project.createdAt | date:'medium':'UTC' }} UTC</p></li> }</ul>
    }`
})
export class ProjectsPage {
  private readonly api = inject(ProjectsApi); private readonly router = inject(Router); private readonly destroyRef = inject(DestroyRef); private readonly element = inject<ElementRef<HTMLElement>>(ElementRef);
  readonly projects = signal<LoadState<readonly ProjectDto[]>>(loading()); readonly showForm = signal(false); readonly creating = signal(false); readonly createError = signal<ApiClientError | null>(null); readonly ambiguous = signal(false);
  private read?: Subscription;
  constructor() { this.load(); }
  load() {
    this.read?.unsubscribe();
    // Una lectura anterior al fallo ambiguo no puede reconciliar la escritura.
    const reconcilesAmbiguity = this.ambiguous();
    this.projects.set(loading());
    this.read = this.api.list().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: data => { this.projects.set(loaded(data)); if (reconcilesAmbiguity) this.ambiguous.set(false); },
      error: (error: ApiClientError) => this.projects.set(failed(error))
    });
  }
  reconcile() { this.load(); }
  cancel() { this.showForm.set(false); this.createError.set(null); setTimeout(() => this.element.nativeElement.querySelector<HTMLElement>('#new-project')?.focus()); }
  create(input: CreateProjectRequest) {
    if (this.creating() || this.ambiguous()) return;
    this.creating.set(true); this.createError.set(null);
    this.api.create(input).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: project => { this.creating.set(false); void this.router.navigate(['/projects', project.id]); }, error: (error: ApiClientError) => { this.creating.set(false); this.createError.set(error); this.ambiguous.set(error.ambiguousWrite); } });
  }
}
