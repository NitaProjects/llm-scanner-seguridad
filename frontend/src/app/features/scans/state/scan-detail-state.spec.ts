import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { ScanDetailState } from './scan-detail-state';
import { API_BASE_URL } from '../../../core/api/api-config';
import { projectId, scanId, scan, id } from '../../../../testing/fixtures';
describe('ScanDetailState polling', () => {
  let state: ScanDetailState; let http: HttpTestingController;
  const path = `/api/projects/${projectId}/scans/${scanId}`;
  beforeEach(() => { vi.useFakeTimers(); TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), { provide: API_BASE_URL, useValue: '/api' }, ScanDetailState] }); state = TestBed.inject(ScanDetailState); http = TestBed.inject(HttpTestingController); });
  afterEach(() => { TestBed.resetTestingModule(); http.verify(); vi.useRealTimers(); });
  function secondary(base = path) { http.expectOne(base + '/executions').flush([]); http.expectOne(base + '/evaluations').flush({ evaluationProfile: null, items: [] }); }
  function final(base = path) { secondary(base); http.expectOne(base + '/findings').flush([]); http.expectOne(base + '/score').flush({ availability: 'NOT_APPLICABLE', score: null }); http.expectOne(base + '/report').flush({ availability: 'NOT_APPLICABLE', report: null }); }
  function failure() { http.expectOne(path).flush({ code: 'INTERNAL_ERROR', message: 'Public', fieldErrors: [] }, { status: 500, statusText: 'Failure' }); }
  it('gets immediately, waits 3s after the complete cycle and never overlaps', () => {
    state.open(projectId, scanId); const request = http.expectOne(path); vi.advanceTimersByTime(9000); http.expectNone(path);
    request.flush(scan()); http.expectNone(path + '/score'); http.expectNone(path + '/report'); vi.advanceTimersByTime(5000); http.expectNone(path);
    secondary(); vi.advanceTimersByTime(2999); http.expectNone(path); vi.advanceTimersByTime(1); http.expectOne(path).flush(scan()); secondary();
    expect(state.failures()).toBe(0);
  });
  it.each(['COMPLETED', 'FAILED'] as const)('stops on %s and loads each final resource exactly once', status => {
    state.open(projectId, scanId); http.expectOne(path).flush(scan({ status })); final(); vi.advanceTimersByTime(60000); http.expectNone(path); http.expectNone(path + '/score'); http.expectNone(path + '/report');
  });
  it('transitions to terminal without another active cycle or artificial wait', () => {
    state.open(projectId, scanId); http.expectOne(path).flush(scan()); secondary(); vi.advanceTimersByTime(3000); http.expectOne(path).flush(scan({ status: 'COMPLETED' })); final(); vi.advanceTimersByTime(3000); http.expectNone(path);
  });
  it('pauses after three first-read errors without inventing status', () => {
    state.open(projectId, scanId); failure(); expect(state.scan().data).toBeNull(); vi.advanceTimersByTime(3000); failure(); vi.advanceTimersByTime(3000); failure(); expect(state.paused()).toBe(true); expect(state.failures()).toBe(3); vi.advanceTimersByTime(60000); http.expectNone(path);
  });
  it('keeps stale confirmed status and resets errors on a successful read', () => {
    state.open(projectId, scanId); http.expectOne(path).flush(scan()); secondary(); vi.advanceTimersByTime(3000); failure(); expect(state.scan().data?.status).toBe('RUNNING'); expect(state.scan().status).toBe('error'); vi.advanceTimersByTime(3000); http.expectOne(path).flush(scan()); secondary(); expect(state.failures()).toBe(0);
  });
  it('manual retry is immediate, blocks double clicks and resumes only on success', () => {
    state.open(projectId, scanId); failure(); vi.advanceTimersByTime(3000); failure(); vi.advanceTimersByTime(3000); failure();
    state.retry(); state.retry(); http.expectOne(path).flush(scan()); secondary(); expect(state.failures()).toBe(0); expect(state.paused()).toBe(false); vi.advanceTimersByTime(3000); http.expectOne(path).flush(scan()); secondary();
  });
  it.each(['COMPLETED', 'FAILED'] as const)('manual retry respects terminal %s', status => {
    state.open(projectId, scanId); failure(); vi.advanceTimersByTime(3000); failure(); vi.advanceTimersByTime(3000); failure(); state.retry(); http.expectOne(path).flush(scan({ status })); final(); vi.advanceTimersByTime(60000); http.expectNone(path);
  });
  it('a failed manual retry remains paused and does not reset failure count', () => {
    state.open(projectId, scanId); failure(); vi.advanceTimersByTime(3000); failure(); vi.advanceTimersByTime(3000); failure(); state.retry(); failure(); expect(state.paused()).toBe(true); expect(state.failures()).toBe(4); vi.advanceTimersByTime(60000); http.expectNone(path);
  });
  it.each([400, 404, 200])('stops on definitive route/resource/contract error %s', status => {
    state.open(projectId, scanId); const request = http.expectOne(path); request.flush(status === 200 ? { status: 'SAFE' } : { code: 'SCAN_NOT_FOUND', message: 'Public', fieldErrors: [] }, { status, statusText: 'Failure' }); expect(state.paused()).toBe(true); vi.advanceTimersByTime(30000); http.expectNone(path);
  });
  it('times out at 10s and cancels the HTTP read', () => {
    state.open(projectId, scanId); const request = http.expectOne(path); vi.advanceTimersByTime(10000); expect(request.cancelled).toBe(true); expect(state.scan().error?.kind).toBe('timeout'); vi.advanceTimersByTime(3000); http.expectOne(path).flush(scan({ status: 'COMPLETED' })); final();
  });
  it('a secondary error does not stop the next scan read, and block retry is isolated', () => {
    state.open(projectId, scanId); http.expectOne(path).flush(scan()); http.expectOne(path + '/executions').flush('failure', { status: 500, statusText: 'Failure' }); http.expectOne(path + '/evaluations').flush({ evaluationProfile: null, items: [] });
    vi.advanceTimersByTime(3000); http.expectOne(path).flush(scan({ status: 'COMPLETED' })); secondary(); http.expectOne(path + '/findings').flush([]); http.expectOne(path + '/score').flush({ availability: 'NOT_APPLICABLE', score: null }); http.expectOne(path + '/report').flush('failure', { status: 500, statusText: 'Failure' });
    state.retryBlock('report'); http.expectOne(path + '/report').flush({ availability: 'NOT_APPLICABLE', report: null }); http.expectNone(path + '/findings'); http.expectNone(path + '/evaluations');
  });
  it('changing route key cancels in-flight reads and clears previous data', () => {
    state.open(projectId, scanId); const old = http.expectOne(path); const nextId = id(99); state.open(projectId, nextId); expect(old.cancelled).toBe(true); expect(state.scan().data).toBeNull(); const next = path.replace(scanId, nextId); http.expectOne(next).flush(scan({ id: nextId, status: 'COMPLETED' })); final(next);
  });
  it('destroy cancels reads and all timers', () => {
    state.open(projectId, scanId); const request = http.expectOne(path); TestBed.resetTestingModule(); expect(request.cancelled).toBe(true); vi.advanceTimersByTime(60000); http.expectNone(path);
  });
  it('destroy also cancels a scheduled timer after a successful active cycle', () => {
    state.open(projectId, scanId); http.expectOne(path).flush(scan()); secondary(); TestBed.resetTestingModule(); vi.advanceTimersByTime(60000); http.expectNone(path);
  });
  it('changing route cancels the terminal resource load, including late responses', () => {
    state.open(projectId, scanId); http.expectOne(path).flush(scan({ status: 'COMPLETED' }));
    const requests = ['executions', 'evaluations', 'findings', 'score', 'report'].map(block => http.expectOne(path + '/' + block));
    state.open(projectId, id(99)); expect(requests.every(request => request.cancelled)).toBe(true); expect(state.findings().data).toBeNull(); expect(state.score().data).toBeNull();
    const request = http.expectOne(path.replace(scanId, id(99))); TestBed.resetTestingModule(); expect(request.cancelled).toBe(true);
  });
});
