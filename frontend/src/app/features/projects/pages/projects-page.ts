import { Component, DestroyRef, ElementRef, inject, signal, viewChild } from '@angular/core';
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
  styles: ['dialog { max-width: min(32rem, calc(100% - 2rem)); padding: var(--space-4); background: var(--surface); color: var(--text); border: 1px solid var(--border); border-radius: var(--radius-panel); } .delete-confirm:not(:disabled) { background: var(--danger); border-color: var(--danger); }'],
  template: `<div class="page-heading"><div><h1 tabindex="-1">Proyectos</h1><p class="muted">Organiza tus análisis de aplicaciones LLM autorizadas.</p></div><button id="new-project" (click)="showForm.set(true)" [disabled]="showForm()"><app-icon name="plus" />Nuevo proyecto</button></div>
    @if (showForm()) { <app-project-create-form [busy]="creating()" [error]="createError()" (create)="create($event)" (cancel)="cancel()" /> }
    @if (createError(); as error) { <app-feedback-panel kind="error" [message]="error.message" /> }
    @if (ambiguous()) { <app-feedback-panel message="No se pudo confirmar la creación. Consulta el listado antes de volver a crear."><button (click)="reconcile()">Consultar listado</button></app-feedback-panel> }
    @if (deleteNotice(); as notice) { <p role="status">{{ notice }}</p> }
    @if (deleteError(); as error) { <app-feedback-panel kind="error" [message]="error" /> }
    @if (projects().status === 'loading') { <app-feedback-panel message="Cargando proyectos…" /> }
    @if (projects().error; as error) { <app-feedback-panel kind="error" [message]="error.message" [retryable]="true" (retry)="load()" /> }
    @if (projects().status === 'loaded') {
      @if (projects().data?.length === 0 && !showForm()) { <section class="empty-state"><app-icon name="folder" class="empty-icon" /><h2>Todavía no hay proyectos</h2><p>Crea un proyecto para configurar su target y ejecutar el primer análisis.</p><button (click)="showForm.set(true)" [disabled]="showForm()"><app-icon name="plus" />Nuevo proyecto</button></section> }
      <ul class="plain-list project-list">@for (project of projects().data; track project.id) { <li class="card"><h2><a class="row-link" [routerLink]="['/projects', project.id]">{{ project.name }}</a></h2>@if (project.description) { <p class="text muted">{{ project.description }}</p> }<p class="meta">Creado {{ project.createdAt | date:'medium':'UTC' }} UTC</p>
      <button type="button" class="secondary" #opener (click)="confirmDeletion(project, opener)" [disabled]="deleting() || pendingReconciliation() !== null">Eliminar proyecto</button></li> }</ul>
    }
    <dialog #deleteDialog aria-labelledby="delete-title" aria-describedby="delete-description" (cancel)="cancelDeletion($event)" [attr.aria-busy]="deleting()">
      <h2 id="delete-title">Eliminar proyecto</h2>
      <p id="delete-description">¿Eliminar el proyecto “{{ deleteSelection()?.name }}”? Se eliminarán también su target, análisis, hallazgos e informes. Esta acción no se puede deshacer.</p>
      <button type="button" class="secondary" #cancelDelete autofocus (click)="cancelDeletion()" [disabled]="deleting()">Cancelar</button>
      <button type="button" class="delete-confirm" (click)="deleteProject()" [disabled]="deleting()">{{ deleting() ? 'Eliminando…' : 'Eliminar proyecto' }}</button>
    </dialog>`
})
export class ProjectsPage {
  private readonly api = inject(ProjectsApi); private readonly router = inject(Router); private readonly destroyRef = inject(DestroyRef); private readonly element = inject<ElementRef<HTMLElement>>(ElementRef);
  readonly projects = signal<LoadState<readonly ProjectDto[]>>(loading()); readonly showForm = signal(false); readonly creating = signal(false); readonly createError = signal<ApiClientError | null>(null); readonly ambiguous = signal(false);
  private read?: Subscription;
  private readonly deleteDialog = viewChild.required<ElementRef<HTMLDialogElement>>('deleteDialog');
  private readonly cancelDelete = viewChild.required<ElementRef<HTMLButtonElement>>('cancelDelete');
  private deleteOpener?: HTMLButtonElement;
  readonly deleteSelection = signal<ProjectDto | null>(null);
  readonly deleting = signal(false);
  readonly pendingReconciliation = signal<{ id: string; missing: boolean } | null>(null);
  readonly deleteNotice = signal<string | null>(null);
  readonly deleteError = signal<string | null>(null);
  constructor() { this.load(); }
  load() {
    this.read?.unsubscribe();
    // Una lectura anterior al fallo ambiguo no puede reconciliar la escritura.
    const reconcilesAmbiguity = this.ambiguous();
    const deletion = this.pendingReconciliation();
    this.projects.set(loading());
    this.read = this.api.list().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: data => {
        this.projects.set(loaded(data));
        if (reconcilesAmbiguity) this.ambiguous.set(false);
        if (deletion && this.pendingReconciliation() === deletion) {
          this.pendingReconciliation.set(null);
          this.deleteNotice.set(deletion.missing ? 'El proyecto ya no existe.' : data.some(project => project.id === deletion.id)
            ? 'El proyecto sigue en el listado. Puedes volver a solicitar su eliminación.'
            : 'El proyecto ya no figura en el listado.');
        }
      },
      error: (error: ApiClientError) => this.projects.set(failed(error))
    });
  }
  confirmDeletion(project: ProjectDto, opener: HTMLButtonElement) {
    if (this.deleting() || this.pendingReconciliation() || this.deleteSelection()) return;
    this.deleteOpener = opener;
    this.deleteNotice.set(null);
    this.deleteError.set(null);
    this.deleteSelection.set(project);
    this.deleteDialog().nativeElement.showModal();
    this.cancelDelete().nativeElement.focus();
  }
  cancelDeletion(event?: Event) {
    event?.preventDefault();
    if (!this.deleting()) this.closeDeletion(true);
  }
  deleteProject() {
    const project = this.deleteSelection();
    if (!project || this.deleting() || this.pendingReconciliation()) return;
    this.deleting.set(true);
    this.api.deleteProject(project.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.read?.unsubscribe();
        const data = this.projects().data;
        if (data) this.projects.set(loaded(data.filter(entry => entry.id !== project.id)));
        else this.load();
        this.deleteNotice.set('Proyecto eliminado.');
        this.closeDeletion(false);
      },
      error: (error: ApiClientError) => {
        if (error.status === 404 || error.ambiguousWrite) {
          this.pendingReconciliation.set({ id: project.id, missing: error.status === 404 });
          this.deleteNotice.set(error.status === 404 ? 'El proyecto ya no existe.' : 'No se pudo confirmar la eliminación. Consultando el listado…');
          this.closeDeletion(false);
          this.load();
          return;
        }
        this.deleteError.set(error.code === 'SCAN_ALREADY_ACTIVE'
          ? 'El proyecto tiene un análisis pendiente o en ejecución. Espera a que termine para eliminarlo.'
          : error.message);
        this.closeDeletion(true);
      }
    });
  }
  private closeDeletion(restoreOpener: boolean) {
    this.deleting.set(false);
    this.deleteSelection.set(null);
    this.deleteDialog().nativeElement.close();
    const opener = this.deleteOpener;
    this.deleteOpener = undefined;
    setTimeout(() => {
      if (restoreOpener && opener?.isConnected) opener.focus();
      else this.element.nativeElement.querySelector<HTMLElement>('h1')?.focus();
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
