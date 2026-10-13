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
});
