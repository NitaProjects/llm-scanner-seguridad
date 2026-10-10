import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ProjectsApi } from '../../features/projects/data-access/projects-api';
import { TargetApi } from '../../features/target/data-access/target-api';
import { ScansApi } from '../../features/scans/data-access/scans-api';
import { API_BASE_URL } from './api-config';
import { ApiClientError } from './api-error';
import { project, projectId, scanId, scan, target, execution, evaluation, finding, score, report, id } from '../../../testing/fixtures';
describe('API contracts', () => {
  let http: HttpTestingController; let projects: ProjectsApi; let targets: TargetApi; let scans: ScansApi;
  const path = `/api/projects/${projectId}/scans/${scanId}`;
  beforeEach(() => { TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), { provide: API_BASE_URL, useValue: '/api' }] }); http = TestBed.inject(HttpTestingController); projects = TestBed.inject(ProjectsApi); targets = TestBed.inject(TargetApi); scans = TestBed.inject(ScansApi); });
  afterEach(() => http.verify());
  it('lists, gets and creates projects with only the input fields', () => {
    projects.list().subscribe(value => expect(value).toEqual([project])); http.expectOne('/api/projects').flush([project]);
    projects.getProject(projectId).subscribe(value => expect(value.description).toBeNull()); http.expectOne(`/api/projects/${projectId}`).flush(project);
    projects.create({ name: 'Synthetic', description: null }).subscribe(value => expect(value.id).toBe(projectId));
    const request = http.expectOne('/api/projects'); expect(request.request.method).toBe('POST'); expect(request.request.body).toEqual({ name: 'Synthetic', description: null }); request.flush(project, { status: 201, statusText: 'Created' });
  });
  it.each(['NONE', 'BEARER_TOKEN', 'API_KEY_HEADER'] as const)('reads public target %s without propagating unknown fields', type => {
    targets.getTarget(projectId).subscribe(value => { expect(value).toEqual(target(type)); expect(JSON.stringify(value)).not.toContain('secretValue'); });
    http.expectOne(`/api/projects/${projectId}/target`).flush({ ...target(type), secretValue: 'synthetic-unexpected' });
  });
  it('creates then updates a target, omitting secretValue for conservation', () => {
    const input = { name: null, url: target().url, requestTemplate: target().requestTemplate, responsePath: '$.text', headers: {}, timeoutMs: 10000, authType: 'BEARER_TOKEN' as const, authConfig: { secretValue: 'synthetic-token' } };
    targets.create(projectId, input).subscribe(); let request = http.expectOne(`/api/projects/${projectId}/target`); expect(request.request.method).toBe('POST'); expect(request.request.headers.has('Authorization')).toBe(false); request.flush(target('BEARER_TOKEN'), { status: 201, statusText: 'Created' });
    targets.update(projectId, { ...input, authConfig: {} }).subscribe(); request = http.expectOne(`/api/projects/${projectId}/target`); expect(request.request.method).toBe('PUT'); expect(request.request.body.authConfig).toEqual({}); request.flush(target('BEARER_TOKEN'));
  });
  it('starts without serialized body and requires 202', () => {
    scans.start(projectId).subscribe(value => expect(value.id).toBe(scanId)); const request = http.expectOne(`/api/projects/${projectId}/scans`);
    expect(request.request.method).toBe('POST'); expect(request.request.serializeBody()).toBeNull(); expect(request.request.detectContentTypeHeader()).toBeNull(); request.flush(scan(), { status: 202, statusText: 'Accepted' });
  });
  it('rejects an unexpected admission status without retrying', () => {
    scans.start(projectId).subscribe({ error: (error: ApiClientError) => expect(error.kind).toBe('contract') }); http.expectOne(`/api/projects/${projectId}/scans`).flush(scan());
  });
  it('uses all seven scoped read endpoints with unchanged shapes and nullable fields', () => {
    scans.list(projectId).subscribe(value => expect(value).toEqual([scan()])); http.expectOne(`/api/projects/${projectId}/scans`).flush([scan()]);
    scans.getScan(projectId, scanId).subscribe(value => expect(value.finishedAt).toBeNull()); http.expectOne(path).flush(scan());
    scans.executions(projectId, scanId).subscribe(value => expect(value).toEqual([execution()])); http.expectOne(path + '/executions').flush([execution()]);
    scans.evaluations(projectId, scanId).subscribe(value => expect(value.items).toEqual([evaluation()])); http.expectOne(path + '/evaluations').flush({ evaluationProfile: 'evaluation-engine-v1', items: [evaluation()] });
    scans.findings(projectId, scanId).subscribe(value => expect(value).toEqual([finding()])); http.expectOne(path + '/findings').flush([finding()]);
    scans.score(projectId, scanId).subscribe(value => expect(value).toEqual({ availability: 'AVAILABLE', score: score() })); http.expectOne(path + '/score').flush({ availability: 'AVAILABLE', score: score() });
    scans.report(projectId, scanId).subscribe(value => expect(value).toEqual({ availability: 'AVAILABLE', report: report() })); http.expectOne(path + '/report').flush({ availability: 'AVAILABLE', report: report() });
  });
  it.each(['PENDING', 'UNAVAILABLE', 'NOT_APPLICABLE'])('preserves independent %s wrappers', availability => {
    scans.score(projectId, scanId).subscribe(value => expect(value).toEqual({ availability, score: null })); http.expectOne(path + '/score').flush({ availability, score: null });
    scans.report(projectId, scanId).subscribe(value => expect(value).toEqual({ availability, report: null })); http.expectOne(path + '/report').flush({ availability, report: null });
  });
  it.each([
    { availability: 'AVAILABLE', score: null }, { availability: 'NOT_APPLICABLE', score: score() },
    { availability: 'AVAILABLE', score: { ...score(), riskLevel: 'SAFE' } }, { availability: 'AVAILABLE', score: { ...score(), scoringVersion: 'v2' } },
    { availability: 'AVAILABLE', score: { ...score(), categories: [] } }, { availability: 'AVAILABLE', score: { ...score(), globalScore: '0' } }
  ])('rejects incompatible score response %#', body => {
    scans.score(projectId, scanId).subscribe({ error: (error: ApiClientError) => expect(error.kind).toBe('contract') }); http.expectOne(path + '/score').flush(body);
  });
  it('rejects a new report version and a new scan enum safely', () => {
    scans.report(projectId, scanId).subscribe({ error: (error: ApiClientError) => expect(error.kind).toBe('contract') }); http.expectOne(path + '/report').flush({ availability: 'AVAILABLE', report: { ...report(), reportVersion: 'report-v2' } });
    scans.getScan(projectId, scanId).subscribe({ error: (error: ApiClientError) => expect(error.kind).toBe('contract') }); http.expectOne(path).flush({ ...scan(), status: 'SAFE' });
  });
  it('rejects resources from another scope instead of displaying them', () => {
    scans.score(projectId, scanId).subscribe({ error: (error: ApiClientError) => expect(error.kind).toBe('contract') }); http.expectOne(path + '/score').flush({ availability: 'AVAILABLE', score: score({ scanId: id(999) }) });
    scans.findings(projectId, scanId).subscribe({ error: (error: ApiClientError) => expect(error.kind).toBe('contract') }); http.expectOne(path + '/findings').flush([finding(0, { scanId: id(999) })]);
  });
  it('accepts canonical backend IDs when direct links use uppercase UUIDs', () => {
    const upperId = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa';
    scans.getScan(upperId.toUpperCase(), scanId).subscribe(value => expect(value.projectId).toBe(upperId)); http.expectOne(`/api/projects/${upperId.toUpperCase()}/scans/${scanId}`).flush(scan({ projectId: upperId }));
  });
  it('maps only allowed validation fields using safe messages', () => {
    projects.create({ name: '' }).subscribe({ error: (error: ApiClientError) => { expect(error.code).toBe('VALIDATION_ERROR'); expect(error.fieldErrors).toHaveLength(1); expect(error.message + JSON.stringify(error.fieldErrors)).not.toContain('SELECT'); } });
    http.expectOne('/api/projects').flush({ code: 'VALIDATION_ERROR', message: 'SELECT private', fieldErrors: [{ field: 'name', code: 'REQUIRED', message: 'SELECT private' }, { field: 'private', code: 'REQUIRED', message: 'private' }] }, { status: 400, statusText: 'Bad Request' });
  });
  it.each([0, 500, 503])('handles noncontractual status %s without printing body', status => {
    projects.list().subscribe({ error: (error: ApiClientError) => { expect(error.message).not.toContain('SQL'); expect(error.transient).toBe(true); } });
    const request = http.expectOne('/api/projects'); if (status === 0) request.error(new ProgressEvent('error')); else request.flush('<html>SQL stack private</html>', { status, statusText: 'Failure' });
  });
  it('validates Location before offering an attempt, with no external request', () => {
    expect(scans.attemptId(projectId, path)).toBe(scanId);
    for (const location of [null, 'https://evil.example.test' + path, `//evil.example.test${path}`, path + '?secret=value', path.replace(projectId, scanId)]) expect(scans.attemptId(projectId, location)).toBeNull();
    scans.start(projectId).subscribe({ error: (error: ApiClientError) => expect(scans.attemptId(projectId, error.location)).toBe(scanId) });
    http.expectOne(`/api/projects/${projectId}/scans`).flush({ code: 'SCAN_DISPATCH_FAILED', message: 'Public', fieldErrors: [] }, { status: 503, statusText: 'Unavailable', headers: { Location: path } });
  });
});
