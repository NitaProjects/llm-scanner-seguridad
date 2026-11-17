import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { API_BASE_URL } from '../../../core/api/api-config';
import { ProjectsPage } from './projects-page';
import { project } from '../../../../testing/fixtures';

describe('ProjectsPage ambiguous creation reconciliation', () => {
  let http: HttpTestingController;
  let page: ProjectsPage;
  let harness: RouterTestingHarness;
  const input = { name: 'Proyecto sintético', description: null };
  const get = { method: 'GET', url: '/api/projects' };
  const post = { method: 'POST', url: '/api/projects' };
  const remove = { method: 'DELETE', url: `/api/projects/${project.id}` };

  beforeEach(async () => {
    TestBed.configureTestingModule({ providers: [
      provideRouter([{ path: 'projects', component: ProjectsPage }]),
      provideHttpClient(), provideHttpClientTesting(), { provide: API_BASE_URL, useValue: '/api' }
    ] });
    http = TestBed.inject(HttpTestingController);
    harness = await RouterTestingHarness.create();
    page = await harness.navigateByUrl('/projects', ProjectsPage);
  });

  afterEach(() => { TestBed.resetTestingModule(); http.verify(); });

  it('does not unlock after a GET started before the ambiguous POST, only after a later reconciliation', () => {
    const initialRead = http.expectOne(get);
    page.create(input);
    http.expectOne(post).error(new ProgressEvent('error'));
    expect(page.ambiguous()).toBe(true);
    initialRead.flush([]);
    harness.detectChanges();
    expect(page.projects().status).toBe('loaded');
    expect(page.ambiguous()).toBe(true);
    expect(harness.routeNativeElement!.textContent).toContain('Consulta el listado');
    page.create(input);
    http.expectNone(post);
    page.reconcile();
    const reconciliation = http.expectOne(get);
    expect(page.ambiguous()).toBe(true);
    reconciliation.flush([project]);
    expect(page.ambiguous()).toBe(false);
    expect(page.projects().data).toEqual([project]);
    http.expectNone(post);
    // Solo una nueva acción explícita puede volver a enviar POST.
    page.create(input);
    http.expectOne(post).flush({ code: 'VALIDATION_ERROR', message: 'Public', fieldErrors: [] }, { status: 400, statusText: 'Bad Request' });
  });

  it('loads normally without an ambiguous write', () => {
    http.expectOne(get).flush([project]);
    harness.detectChanges();
    expect(page.projects().data).toEqual([project]);
    expect(page.ambiguous()).toBe(false);
    expect(harness.routeNativeElement!.textContent).toContain(project.name);
    http.expectNone(post);
  });

  it('opens the existing inline form from empty CTA and returns focus to the original opener on cancel', async () => {
    http.expectOne(get).flush([]); harness.detectChanges();
    const node = harness.routeNativeElement!;
    const original = node.querySelector<HTMLButtonElement>('#new-project')!;
    const empty = node.querySelector<HTMLButtonElement>('.empty-state button')!;
    expect(empty.textContent?.trim()).toBe('Nuevo proyecto');
    expect(empty.querySelector('svg')?.getAttribute('aria-hidden')).toBe('true');
    empty.click(); harness.detectChanges();
    expect(node.querySelector('.empty-state')).toBeNull();
    expect(node.querySelector('#new-project')).toBe(original);
    expect(original.disabled).toBe(true);
    expect(document.activeElement).toBe(node.querySelector('#project-name'));
    node.querySelector<HTMLButtonElement>('form button.secondary')!.click();
    harness.detectChanges();
    await new Promise(resolve => setTimeout(resolve));
    expect(page.showForm()).toBe(false);
    expect(document.activeElement).toBe(original);
    expect(node.querySelector('.empty-state')).not.toBeNull();
    http.expectNone(post);
  });

  it('allows a successful later reconciliation without automatically repeating POST', () => {
    http.expectOne(get).flush([]);
    page.create(input);
    http.expectOne(post).error(new ProgressEvent('error'));
    http.expectNone(post);
    page.reconcile();
    http.expectOne(get).flush([project]);
    expect(page.ambiguous()).toBe(false);
    expect(page.projects().data).toEqual([project]);
    http.expectNone(post);
  });

  it('keeps creation blocked when the later reconciliation fails', () => {
    http.expectOne(get).flush([]);
    page.create(input);
    http.expectOne(post).error(new ProgressEvent('error'));
    page.reconcile();
    http.expectOne(get).flush('Failure', { status: 500, statusText: 'Failure' });
    expect(page.ambiguous()).toBe(true);
    page.create(input);
    http.expectNone(post);
  });

  function openDeletion() {
    harness.detectChanges();
    const node = harness.routeNativeElement!;
    const opener = node.querySelector<HTMLButtonElement>('.project-list button')!;
    const dialog = node.querySelector<HTMLDialogElement>('dialog')!;
    // jsdom no implementa el top layer de HTMLDialogElement.
    dialog.showModal ??= () => dialog.setAttribute('open', '');
    dialog.close ??= () => dialog.removeAttribute('open');
    opener.click();
    harness.detectChanges();
    return { dialog, opener, cancel: dialog.querySelector<HTMLButtonElement>('[autofocus]')!,
      confirm: dialog.querySelector<HTMLButtonElement>('.delete-confirm')! };
  }

  it('names and describes the modal, focuses Cancelar and cancels without DELETE', async () => {
    http.expectOne(get).flush([project]);
    const { dialog, opener, cancel } = openDeletion();
    expect(dialog.open).toBe(true);
    expect(dialog.getAttribute('aria-labelledby')).toBe('delete-title');
    expect(dialog.getAttribute('aria-describedby')).toBe('delete-description');
    expect(dialog.textContent).toContain(project.name);
    expect(dialog.textContent).toContain('no se puede deshacer');
    expect(document.activeElement).toBe(cancel);
    cancel.click();
    expect(dialog.open).toBe(false);
    await new Promise(resolve => setTimeout(resolve));
    expect(document.activeElement).toBe(opener);
    http.expectNone(remove);
  });

  it('blocks duplicate submit and closes and removes the card after 204', async () => {
    http.expectOne(get).flush([project]);
    const { dialog, cancel, confirm } = openDeletion();
    confirm.click();
    page.deleteProject();
    harness.detectChanges();
    expect(confirm.disabled).toBe(true);
    expect(cancel.disabled).toBe(true);
    expect(dialog.getAttribute('aria-busy')).toBe('true');
    const request = http.expectOne(remove);
    expect(request.request.body).toBeNull();
    request.flush(null, { status: 204, statusText: 'No Content' });
    harness.detectChanges();
    expect(dialog.open).toBe(false);
    expect(page.projects().data).toEqual([]);
    expect(page.deleteNotice()).toBe('Proyecto eliminado.');
    await new Promise(resolve => setTimeout(resolve));
    expect(document.activeElement).toBe(harness.routeNativeElement!.querySelector('h1'));
  });

  it('invalidates a stale read on 204 and restarts the list', () => {
    http.expectOne(get).flush([project]);
    openDeletion().confirm.click();
    page.load();
    const stale = http.expectOne(get);
    http.expectOne(remove).flush(null, { status: 204, statusText: 'No Content' });
    expect(stale.cancelled).toBe(true);
    http.expectOne(get).flush([]);
    expect(page.projects().data).toEqual([]);
  });

  it('keeps a project with active scans and reports the conflict', () => {
    http.expectOne(get).flush([project]);
    openDeletion().confirm.click();
    http.expectOne(remove).flush({ code: 'SCAN_ALREADY_ACTIVE', message: 'Conflicto', fieldErrors: [] },
      { status: 409, statusText: 'Conflict' });
    expect(page.projects().data).toEqual([project]);
    expect(page.deleteError()).toContain('pendiente o en ejecución');
  });

  it('reconciles uncertain and missing outcomes before allowing new attempts', () => {
    http.expectOne(get).flush([project]);
    openDeletion().confirm.click();
    http.expectOne(remove).error(new ProgressEvent('error'));
    const fresh = http.expectOne(get);
    expect(page.pendingReconciliation()).not.toBeNull();
    page.deleteProject();
    http.expectNone(remove);
    fresh.flush([project]);
    expect(page.pendingReconciliation()).toBeNull();
    openDeletion().confirm.click();
    http.expectOne(remove).flush({ code: 'PROJECT_NOT_FOUND', message: 'Ausente', fieldErrors: [] },
      { status: 404, statusText: 'Not Found' });
    http.expectOne(get).flush([]);
    expect(page.deleteNotice()).toBe('El proyecto ya no existe.');
    http.expectNone(remove);
  });
});
