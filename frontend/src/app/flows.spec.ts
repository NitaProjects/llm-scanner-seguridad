import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, NavigationEnd, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { filter, firstValueFrom } from 'rxjs';
import { vi } from 'vitest';
import { routes } from './app.routes';
import { API_BASE_URL } from './core/api/api-config';
import { ProjectsPage } from './features/projects/pages/projects-page';
import { ProjectPage } from './features/projects/pages/project-page';
import { TargetPage } from './features/target/pages/target-page';
import { ScanPage } from './features/scans/pages/scan-page';
import { NotFoundPage } from './shared/ui/not-found-page';
import { project, projectId, scanId, scan, target, id, scenario, finding, execution, evaluation } from '../testing/fixtures';
describe('Frontend routes and HTTP flows', () => {
  let http: HttpTestingController; let harness: RouterTestingHarness;
  const projectPath = `/api/projects/${projectId}`; const path = `${projectPath}/scans/${scanId}`;
  beforeEach(async () => {
    TestBed.configureTestingModule({ providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting(), { provide: API_BASE_URL, useValue: '/api' }] }); http = TestBed.inject(HttpTestingController); harness = await RouterTestingHarness.create();
  });
  afterEach(() => { TestBed.resetTestingModule(); http.verify(); vi.restoreAllMocks(); });
  function nextNavigation() { return firstValueFrom(TestBed.inject(Router).events.pipe(filter(event => event instanceof NavigationEnd))); }
  function projectLoaded(withTarget = true) { http.expectOne(projectPath).flush(project); const request = http.expectOne(projectPath + '/target'); if (withTarget) request.flush(target()); else request.flush({ code: 'TARGET_NOT_CONFIGURED', message: 'Public', fieldErrors: [] }, { status: 404, statusText: 'Not Found' }); http.expectOne(projectPath + '/scans').flush([]); harness.detectChanges(); }
  function terminal(kind: 'negative' | 'inconclusive' | 'mixed' | 'degraded' | 'critical' = 'mixed') { const data = scenario(kind); http.expectOne(path + '/executions').flush([0,1,2,3].map(index => execution(index))); http.expectOne(path + '/evaluations').flush({ evaluationProfile: 'evaluation-engine-v1', items: data.evaluations }); http.expectOne(path + '/findings').flush(data.findings); http.expectOne(path + '/score').flush({ availability: 'AVAILABLE', score: data.score }); http.expectOne(path + '/report').flush({ availability: 'AVAILABLE', report: data.report }); harness.detectChanges(); }
  it('redirects root, distinguishes loading/empty/list and retains server order', async () => {
    await harness.navigateByUrl('/', ProjectsPage); expect(harness.routeNativeElement?.textContent).toContain('Cargando'); http.expectOne('/api/projects').flush([]); harness.detectChanges(); expect(harness.routeNativeElement?.textContent).toContain('Todavía no hay proyectos');
    const component = harness.routeDebugElement!.componentInstance as ProjectsPage; component.load(); http.expectOne('/api/projects').flush([{ ...project, id: id(100), name: 'Primero' }, project]); harness.detectChanges(); const links = harness.routeNativeElement!.querySelectorAll('li h3'); expect(links[0].textContent).toBe('Primero'); expect(links[1].textContent).toBe(project.name);
  });
  it('shows read errors without pretending the list is empty', async () => {
    await harness.navigateByUrl('/projects', ProjectsPage); http.expectOne('/api/projects').flush('private stack', { status: 500, statusText: 'Failure' }); harness.detectChanges(); const text = harness.routeNativeElement!.textContent; expect(text).toContain('Reintentar'); expect(text).not.toContain('Todavía no hay proyectos'); expect(text).not.toContain('private stack');
  });
  it('creates once through the inline typed form and navigates to the returned project', async () => {
    const component = await harness.navigateByUrl('/projects', ProjectsPage); http.expectOne('/api/projects').flush([]); component.showForm.set(true); harness.detectChanges();
    const name = harness.routeNativeElement!.querySelector<HTMLInputElement>('#project-name')!; expect(document.activeElement).toBe(name); name.value = '  Nombre  interior  '; name.dispatchEvent(new Event('input')); const form = harness.routeNativeElement!.querySelector('form')!; form.dispatchEvent(new Event('submit', { cancelable: true })); form.dispatchEvent(new Event('submit', { cancelable: true }));
    const request = http.expectOne('/api/projects'); expect(request.request.body).toEqual({ name: '  Nombre  interior  ', description: null }); const navigated = nextNavigation(); request.flush(project, { status: 201, statusText: 'Created' }); await navigated; projectLoaded(false); expect(TestBed.inject(Router).url).toBe(`/projects/${projectId}`); expect(harness.routeNativeElement!.textContent).toContain('Configurar target');
  });
  it('ambiguous create offers reconciliation and never repeats POST', async () => {
    const component = await harness.navigateByUrl('/projects', ProjectsPage); http.expectOne('/api/projects').flush([]); component.create({ name: 'Synthetic' }); http.expectOne('/api/projects').error(new ProgressEvent('error')); component.create({ name: 'Synthetic' }); http.expectNone(request => request.method === 'POST'); harness.detectChanges(); expect(harness.routeNativeElement!.textContent).toContain('Consulta el listado'); component.reconcile(); http.expectOne('/api/projects').flush([project]);
  });
  it('a failed reconciliation does not re-enable an ambiguous project write', async () => {
    const page = await harness.navigateByUrl('/projects', ProjectsPage); http.expectOne('/api/projects').flush([]); page.create({ name: 'Synthetic' }); http.expectOne('/api/projects').error(new ProgressEvent('error')); page.reconcile(); http.expectOne('/api/projects').flush('failure', { status: 500, statusText: 'Failure' }); page.create({ name: 'Synthetic' }); http.expectNone(request => request.method === 'POST'); expect(page.ambiguous()).toBe(true);
  });
  it('a concurrent target creation offers public reload without switching silently to PUT', async () => {
    const page = await harness.navigateByUrl(`/projects/${projectId}/target`, TargetPage); http.expectOne(projectPath).flush(project); http.expectOne(projectPath + '/target').flush({ code: 'TARGET_NOT_CONFIGURED', message: 'Public', fieldErrors: [] }, { status: 404, statusText: 'Not Found' }); harness.detectChanges(); const form = page.editor()!; form.form.patchValue({ url: target().url, requestTemplate: target().requestTemplate, responsePath: '$.text' }); form.submit(); http.expectOne(projectPath + '/target').flush({ code: 'TARGET_ALREADY_CONFIGURED', message: 'Public', fieldErrors: [] }, { status: 409, statusText: 'Conflict' }); harness.detectChanges(); expect(page.reconcileRequired()).toBe(true); http.expectNone(request => request.method === 'PUT'); page.load(); http.expectOne(projectPath).flush(project); http.expectOne(projectPath + '/target').flush(target()); harness.detectChanges(); expect(page.target().data).toEqual(target());
  });
  it('Target CanDeactivate preserves the page on cancelled navigation and clears the secret', async () => {
    const page = await harness.navigateByUrl(`/projects/${projectId}/target`, TargetPage); http.expectOne(projectPath).flush(project); http.expectOne(projectPath + '/target').flush(target('BEARER_TOKEN')); harness.detectChanges();
    const dialog = mockLeaveDialog();
    page.editor()!.form.markAsDirty(); page.editor()!.form.controls.secretValue.setValue('synthetic-token');
    const confirm = vi.spyOn(window, 'confirm');
    const navigation = harness.navigateByUrl('/unknown');
    await vi.waitFor(() => expect(dialog.showModal).toHaveBeenCalledTimes(1));
    dialog.querySelector<HTMLButtonElement>('button.secondary')!.click();
    await navigation;
    expect(TestBed.inject(Router).url).toBe(`/projects/${projectId}/target`);
    expect(page.editor()!.form.controls.secretValue.value).toBe(''); expect(confirm).not.toHaveBeenCalled();
    http.expectNone(request => request.method !== 'GET');
  });
  function mockLeaveDialog() {
    const dialog = harness.routeNativeElement!.querySelector<HTMLDialogElement>('.leave-dialog')!;
    Object.defineProperty(dialog, 'showModal', { configurable: true, value: vi.fn(() => dialog.setAttribute('open', '')) });
    Object.defineProperty(dialog, 'close', { configurable: true, value: vi.fn(() => dialog.removeAttribute('open')) });
    return dialog;
  }
  it('Target Cancel uses the same guard once and only leaves after explicit discard', async () => {
    const page = await harness.navigateByUrl(`/projects/${projectId}/target`, TargetPage); http.expectOne(projectPath).flush(project); http.expectOne(projectPath + '/target').flush(target()); harness.detectChanges();
    const dialog = mockLeaveDialog(); page.editor()!.form.markAsDirty();
    page.cancel(); await vi.waitFor(() => expect(dialog.showModal).toHaveBeenCalledTimes(1));
    expect(page.editor()!.form.dirty).toBe(true); http.expectNone(projectPath);
    const navigated = nextNavigation(); dialog.querySelector<HTMLButtonElement>('.discard-confirm')!.click();
    await navigated; projectLoaded();
    expect(TestBed.inject(Router).url).toBe(`/projects/${projectId}`);
    expect(dialog.showModal).toHaveBeenCalledTimes(1); http.expectNone(request => request.method !== 'GET');
  });
  it('keeps history usable when target fails and never enables start', async () => {
    const component = await harness.navigateByUrl(`/projects/${projectId}`, ProjectPage); http.expectOne(projectPath).flush(project); http.expectOne(projectPath + '/target').flush('Failure', { status: 500, statusText: 'Failure' }); http.expectOne(projectPath + '/scans').flush([scan({ status: 'COMPLETED' })]); harness.detectChanges(); expect(harness.routeNativeElement!.textContent).toContain('Abrir análisis'); expect(harness.routeNativeElement!.textContent).not.toContain('Ejecutar análisis'); component.start(); http.expectNone(request => request.method === 'POST');
  });
  it('target deep link creates, then updates with conservation and handles advanced validation', async () => {
    const page = await harness.navigateByUrl(`/projects/${projectId}/target`, TargetPage); http.expectOne(projectPath).flush(project); http.expectOne(projectPath + '/target').flush({ code: 'TARGET_NOT_CONFIGURED', message: 'Public', fieldErrors: [] }, { status: 404, statusText: 'Not Found' }); harness.detectChanges();
    const form = page.editor()!; form.form.patchValue({ url: target().url, requestTemplate: target().requestTemplate, responsePath: '$.text', authType: 'BEARER_TOKEN', secretValue: 'synthetic-token' });
    // La transición limpia cualquier entrada previa; introducir tras elegir el mecanismo.
    form.form.controls.secretValue.setValue('synthetic-token'); form.submit(); const create = http.expectOne(projectPath + '/target'); expect(create.request.method).toBe('POST'); expect(form.form.controls.secretValue.value).toBe(''); create.flush(target('BEARER_TOKEN'), { status: 201, statusText: 'Created' }); harness.detectChanges(); expect(form.form.pristine).toBe(true);
    form.form.controls.responsePath.setValue('$..unsupported'); form.submit(); const update = http.expectOne(projectPath + '/target'); expect(update.request.method).toBe('PUT'); expect(update.request.body.authConfig).toEqual({}); update.flush({ code: 'VALIDATION_ERROR', message: 'Public', fieldErrors: [{ field: 'responsePath', code: 'INVALID_FORMAT', message: 'Public' }] }, { status: 400, statusText: 'Bad Request' }); harness.detectChanges(); expect(harness.routeNativeElement!.textContent).toContain('El formato no es válido'); expect(page.target().data?.authType).toBe('BEARER_TOKEN');
  });
  it('ambiguous target write clears secret, blocks a second write and reloads public metadata', async () => {
    const page = await harness.navigateByUrl(`/projects/${projectId}/target`, TargetPage); http.expectOne(projectPath).flush(project); http.expectOne(projectPath + '/target').flush(target('BEARER_TOKEN')); harness.detectChanges(); const editor = page.editor()!; editor.form.controls.credentialMode.setValue('replace'); editor.form.controls.secretValue.setValue('synthetic-token'); editor.submit(); http.expectOne(projectPath + '/target').error(new ProgressEvent('error')); harness.detectChanges(); expect(editor.form.controls.secretValue.value).toBe(''); expect(page.reconcileRequired()).toBe(true); editor.submit(); http.expectNone(request => request.method === 'PUT'); page.load(); http.expectOne(projectPath).flush(project); http.expectOne(projectPath + '/target').flush(target('BEARER_TOKEN')); harness.detectChanges(); expect(page.reconcileRequired()).toBe(false);
  });
  it('starts without body, navigates and immediately reads a rapidly completed scan', async () => {
    const component = await harness.navigateByUrl(`/projects/${projectId}`, ProjectPage); projectLoaded(); component.start(); component.start(); const request = http.expectOne(projectPath + '/scans'); expect(request.request.serializeBody()).toBeNull(); const navigated = nextNavigation(); request.flush(scan({ status: 'PENDING', startedAt: null }), { status: 202, statusText: 'Accepted' }); await navigated;
    http.expectOne(path).flush(scan({ status: 'COMPLETED', finishedAt: scan().createdAt })); terminal(); expect(harness.routeNativeElement!.textContent).toContain('74/100'); expect(harness.routeNativeElement!.textContent).toContain('Hay 1 resultado inconcluso');
  });
  it('conflict reconciles history, while 503 Location offers only the confirmed scoped attempt', async () => {
    const component = await harness.navigateByUrl(`/projects/${projectId}`, ProjectPage); projectLoaded(); component.start(); http.expectOne(projectPath + '/scans').flush({ code: 'SCAN_ALREADY_ACTIVE', message: 'Public', fieldErrors: [] }, { status: 409, statusText: 'Conflict' }); http.expectOne(projectPath + '/scans').flush([scan()]); harness.detectChanges(); expect(harness.routeNativeElement!.textContent).toContain('Ver análisis en curso');
    component.loadScans(); http.expectOne(projectPath + '/scans').flush([]); component.start(); http.expectOne(projectPath + '/scans').flush({ code: 'SCAN_DISPATCH_FAILED', message: 'Public', fieldErrors: [] }, { status: 503, statusText: 'Unavailable', headers: { Location: path } }); harness.detectChanges(); expect(harness.routeNativeElement!.textContent).toContain('Consultar intento registrado'); http.expectNone(path);
  });
  it('RUNNING deep link uses only progress endpoints, without score, report or Findings', async () => {
    await harness.navigateByUrl(`/projects/${projectId}/scans/${scanId}`, ScanPage); http.expectOne(path).flush(scan()); http.expectOne(path + '/executions').flush([]); http.expectOne(path + '/evaluations').flush({ evaluationProfile: null, items: [] }); harness.detectChanges(); expect(harness.routeNativeElement!.textContent).toContain('Análisis en curso'); http.expectNone(path + '/score'); http.expectNone(path + '/report'); http.expectNone(path + '/findings');
  });
  it.each(['negative', 'inconclusive', 'degraded', 'critical'] as const)('COMPLETED deep link renders %s independently of prior navigation', async kind => {
    await harness.navigateByUrl(`/projects/${projectId}/scans/${scanId}`, ScanPage); http.expectOne(path).flush(scan({ status: 'COMPLETED' })); terminal(kind); expect(harness.routeNativeElement!.querySelector('app-risk-summary')).not.toBeNull(); expect(harness.routeNativeElement!.querySelectorAll('app-report-sections ol>li')).toHaveLength(9);
  });
  it.each(['PENDING', 'UNAVAILABLE', 'NOT_APPLICABLE'] as const)('handles independent score/report %s without fabricating numeric score', async availability => {
    await harness.navigateByUrl(`/projects/${projectId}/scans/${scanId}`, ScanPage); http.expectOne(path).flush(scan({ status: 'COMPLETED' })); http.expectOne(path + '/executions').flush([]); http.expectOne(path + '/evaluations').flush({ evaluationProfile: null, items: [] }); http.expectOne(path + '/findings').flush([]); http.expectOne(path + '/score').flush({ availability, score: null }); http.expectOne(path + '/report').flush({ availability, report: null }); harness.detectChanges(); const text = harness.routeNativeElement!.textContent; expect(text).not.toContain('0/100'); expect(text).toContain(availability === 'PENDING' ? 'incoherente' : availability === 'UNAVAILABLE' ? 'No disponible' : 'histórico');
  });
  it('historical V6 shows score while only report is not applicable', async () => {
    await harness.navigateByUrl(`/projects/${projectId}/scans/${scanId}`, ScanPage); http.expectOne(path).flush(scan({ status: 'COMPLETED' })); http.expectOne(path + '/executions').flush([0,1,2,3].map(index => execution(index))); http.expectOne(path + '/evaluations').flush({ evaluationProfile: 'evaluation-engine-v1', items: scenario('negative').evaluations }); http.expectOne(path + '/findings').flush([]); http.expectOne(path + '/score').flush({ availability: 'AVAILABLE', score: scenario('negative').score }); http.expectOne(path + '/report').flush({ availability: 'NOT_APPLICABLE', report: null }); harness.detectChanges(); expect(harness.routeNativeElement!.textContent).toContain('0/100'); expect(harness.routeNativeElement!.querySelector('#report')!.textContent).toContain('histórico');
  });
  it('FAILED preserves Findings and public reason but creates no partial report', async () => {
    await harness.navigateByUrl(`/projects/${projectId}/scans/${scanId}`, ScanPage); http.expectOne(path).flush(scan({ status: 'FAILED', errorReason: 'SCAN_PROCESSING_FAILED' })); http.expectOne(path + '/executions').flush([execution()]); http.expectOne(path + '/evaluations').flush({ evaluationProfile: 'evaluation-engine-v1', items: [evaluation(0, { finalVerdict: 'VULNERABLE', severity: 'HIGH', findingId: finding().id })] }); http.expectOne(path + '/findings').flush([finding()]); http.expectOne(path + '/score').flush({ availability: 'UNAVAILABLE', score: null }); http.expectOne(path + '/report').flush({ availability: 'UNAVAILABLE', report: null }); harness.detectChanges(); expect(harness.routeNativeElement!.textContent).toContain('No se pudo completar el procesamiento'); expect(harness.routeNativeElement!.textContent).toContain('Synthetic finding'); expect(harness.routeNativeElement!.querySelector('app-report-sections')).toBeNull();
  });
  it('detects summary conflict without choosing a replacement source', async () => {
    const page = await harness.navigateByUrl(`/projects/${projectId}/scans/${scanId}`, ScanPage); http.expectOne(path).flush(scan({ status: 'COMPLETED' })); const data = scenario('mixed'); http.expectOne(path + '/executions').flush([]); http.expectOne(path + '/evaluations').flush({ evaluationProfile: null, items: [] }); http.expectOne(path + '/findings').flush(data.findings); http.expectOne(path + '/score').flush({ availability: 'AVAILABLE', score: data.score }); http.expectOne(path + '/report').flush({ availability: 'AVAILABLE', report: { ...data.report, summary: { ...data.score, globalScore: 75 } } }); harness.detectChanges(); expect(page.conflict()).toBe(true); expect(harness.routeNativeElement!.textContent).toContain('no coinciden'); expect(harness.routeNativeElement!.textContent).toContain('74/100'); expect(harness.routeNativeElement!.textContent).toContain('75/100');
  });
  it('route changes clear data and cancel an old request; unknown route returns to projects', async () => {
    await harness.navigateByUrl(`/projects/${projectId}/scans/${scanId}`, ScanPage); const old = http.expectOne(path); await harness.navigateByUrl(`/projects/${projectId}/scans/${id(99)}`, ScanPage); expect(old.cancelled).toBe(true); const request = http.expectOne(path.replace(scanId, id(99))); await harness.navigateByUrl('/unknown', NotFoundPage); expect(request.cancelled).toBe(true); expect(harness.routeNativeElement!.textContent).toContain('Página no encontrada');
  });
  it('project names and descriptions render as inert text with no unexpected requests', async () => {
    await harness.navigateByUrl('/projects', ProjectsPage); const attack = '<img src="https://external.example.test" onerror="attack()">'; http.expectOne('/api/projects').flush([{ ...project, name: attack, description: attack }]); harness.detectChanges(); expect(harness.routeNativeElement!.querySelector('img')).toBeNull(); expect(harness.routeNativeElement!.textContent).toContain(attack);
  });
});
