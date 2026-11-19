import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ScanPage } from './scan-page';
import { ScanDetailState } from '../state/scan-detail-state';
import { loaded, failed, loading } from '../../../shared/models/load-state';
import { execution, projectId, scanId, scan, scenario } from '../../../../testing/fixtures';
import { ScanDto, ScoreResponse } from '../models/scan';
import { ApiClientError } from '../../../core/api/api-error';

describe('Scan visual states with the existing state contract', () => {
  function setup(status: ScanDto['status']) {
    const data = scenario('mixed');
    const state = {
      scan: signal(loaded(scan({ status }))), paused: signal(false), reading: signal(false), lastUpdated: signal(new Date()),
      executions: signal(loaded([execution()])), evaluations: signal(loaded({ items: data.evaluations, evaluationProfile: 'evaluation-engine-v1' })),
      findings: signal(loaded(data.findings)), score: signal(loaded<ScoreResponse>({ availability: 'AVAILABLE', score: data.score })), report: signal(loaded({ availability: 'AVAILABLE', report: data.report })),
      open: vi.fn(), retry: vi.fn(), retryBlock: vi.fn()
    };
    TestBed.configureTestingModule({ imports: [ScanPage], providers: [provideRouter([]), { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ projectId, scanId })) } }] });
    TestBed.overrideComponent(ScanPage, { set: { providers: [{ provide: ScanDetailState, useValue: state }] } });
    const fixture = TestBed.createComponent(ScanPage); fixture.detectChanges();
    return { state, fixture, node: fixture.nativeElement as HTMLElement };
  }
  it.each(['PENDING', 'RUNNING'] as const)('shows honest active tracking for %s without fabricated progress', status => {
    const { node, state, fixture } = setup(status);
    expect(node.querySelector('[data-tracking=active]')).not.toBeNull();
    expect(node.querySelector('h1')?.textContent).toContain(status === 'PENDING' ? 'pendiente' : 'ejecución');
    expect(node.querySelector('[role=progressbar],progress')).toBeNull();
    expect(node.querySelector('#risk')).toBeNull();
    fixture.detectChanges(); expect(state.open).toHaveBeenCalledTimes(1);
  });
  it('stops activity immediately when a confirmed running status becomes stale or paused', () => {
    const { node, state, fixture } = setup('RUNNING');
    state.scan.set(failed(new ApiClientError('backend', 'Lectura no disponible', 503), state.scan().data)); fixture.detectChanges();
    expect(node.querySelector('.is-tracking')).toBeNull(); expect(node.textContent).toContain('desactualizado');
    state.scan.set(loaded(scan())); state.paused.set(true); fixture.detectChanges();
    expect(node.querySelector('[data-tracking=stopped]')).not.toBeNull();
  });
  it('keeps one activity indicator across reads and removes it on completion without forcing focus', () => {
    const { node, state, fixture } = setup('RUNNING');
    const indicator = node.querySelector('.activity-line');
    state.scan.set(loaded(scan())); fixture.detectChanges(); expect(node.querySelector('.activity-line')).toBe(indicator);
    state.scan.set(loaded(scan({ status: 'COMPLETED' }))); fixture.detectChanges();
    expect(node.querySelector('.activity-line')).toBeNull(); expect(node.querySelector('#risk .score')?.textContent).toBe('74/100');
    expect(node.querySelector('#report app-report-sections')).not.toBeNull();
  });
  it('retains safe failure, partial-data warning and a route back to the project', () => {
    const { node } = setup('FAILED');
    expect(node.querySelector('h1')?.textContent).toContain('fallido');
    expect(node.querySelector('[role=alert]')).not.toBeNull(); expect(node.textContent).toContain('no constituyen un informe');
    expect(node.querySelector('nav[aria-label=Ruta] a:last-child')?.getAttribute('href')).toBe(`/projects/${projectId}`);
    expect(node.querySelector('.activity-line')).toBeNull();
  });
  it('never replaces a loading score with zero and keeps independent results', () => {
    const { node, state, fixture } = setup('COMPLETED'); state.score.set(loading()); fixture.detectChanges();
    expect(node.querySelector('#risk .score')).toBeNull(); expect(node.querySelector('#risk [aria-busy=true]')?.textContent).toContain('Cargando puntuación');
    expect(node.querySelector('#findings details')).not.toBeNull();
  });
  it('preserves historical unavailability rather than inventing a score', () => {
    const { node, state, fixture } = setup('COMPLETED');
    state.score.set(loaded({ availability: 'NOT_APPLICABLE', score: null })); fixture.detectChanges();
    expect(node.querySelector('#risk .score')).toBeNull(); expect(node.querySelector('#risk')?.textContent).toContain('histórico');
  });
});
