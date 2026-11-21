import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';
import { ScanPage } from './scan-page';
import { ScanDetailState } from '../state/scan-detail-state';
import { loaded, failed, loading } from '../../../shared/models/load-state';
import { execution, finding, projectId, scanId, scan, scenario } from '../../../../testing/fixtures';
import { ScanDto, ScoreResponse } from '../models/scan';
import { ApiClientError } from '../../../core/api/api-error';

describe('Scan visual states with the existing state contract', () => {
  function setup(status: ScanDto['status']) {
    const data = scenario('mixed');
    const fragment = new BehaviorSubject<string | null>(null);
    const state = {
      scan: signal(loaded(scan({ status }))), paused: signal(false), reading: signal(false), lastUpdated: signal(new Date()),
      executions: signal(loaded([execution()])), evaluations: signal(loaded({ items: data.evaluations, evaluationProfile: 'evaluation-engine-v1' })),
      findings: signal(loaded(data.findings)), score: signal(loaded<ScoreResponse>({ availability: 'AVAILABLE', score: data.score })), report: signal(loaded({ availability: 'AVAILABLE', report: data.report })),
      open: vi.fn(), retry: vi.fn(), retryBlock: vi.fn()
    };
    TestBed.configureTestingModule({ imports: [ScanPage], providers: [provideRouter([]), { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ projectId, scanId })), fragment } }] });
    TestBed.overrideComponent(ScanPage, { set: { providers: [{ provide: ScanDetailState, useValue: state }] } });
    const fixture = TestBed.createComponent(ScanPage); fixture.detectChanges();
    return { state, fixture, fragment, node: fixture.nativeElement as HTMLElement };
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
  it('has unique actual anchors and never serializes null as an id in the selected inspector', () => {
    const { node } = setup('COMPLETED');
    const ids = Array.from(node.querySelectorAll('[id]')).map(item => item.id);
    expect(ids).not.toContain('null'); expect(new Set(ids).size).toBe(ids.length);
  });
  it('keeps confirmed rail activity during an in-flight reread without resetting selection or nodes', () => {
    const { node, state, fixture } = setup('RUNNING');
    state.executions.set(loaded([execution(0, { status: 'RUNNING' })])); fixture.detectChanges();
    const row = node.querySelector('.test-rail.active'); expect(row).not.toBeNull();
    state.executions.set(loading(state.executions().data)); fixture.detectChanges();
    expect(node.querySelector('.test-rail.active')).toBe(row);
    expect(node.textContent).toContain('Inicio'); expect(node.textContent).not.toContain('no iniciado');
    state.executions.set(failed(new ApiClientError('backend', 'Lectura no disponible', 503), state.executions().data)); fixture.detectChanges();
    expect(node.querySelector('.test-rail.active')).toBeNull();
  });
  it('never replaces a loading score with zero and keeps independent results', () => {
    const { node, state, fixture } = setup('COMPLETED'); state.score.set(loading()); fixture.detectChanges();
    expect(node.querySelector('#risk .score')).toBeNull(); expect(node.querySelector('#risk [aria-busy=true]')?.textContent).toContain('Cargando puntuación');
    expect(node.querySelector('app-finding-intelligence-panel')).not.toBeNull();
  });
  it('preserves historical unavailability rather than inventing a score', () => {
    const { node, state, fixture } = setup('COMPLETED');
    state.score.set(loaded({ availability: 'NOT_APPLICABLE', score: null })); fixture.detectChanges();
    expect(node.querySelector('#risk .score')).toBeNull(); expect(node.querySelector('#risk')?.textContent).toContain('histórico');
  });
  it('keeps an explicitly selected test without a Finding when RUNNING closes', () => {
    const { fixture, state, node } = setup('RUNNING');
    fixture.componentInstance.inspectTest('LLM08'); fixture.detectChanges();
    state.scan.set(loaded(scan({ status: 'COMPLETED' }))); fixture.detectChanges();
    expect(fixture.componentInstance.selectedCategory()).toBe('LLM08');
    expect(node.querySelector('#risk .score')?.textContent).toBe('74/100');
    expect(node.querySelector('app-finding-intelligence-panel')).toBeNull();
    expect(node.querySelector<HTMLDetailsElement>('.technical-depth')?.open).toBe(true);
    expect(state.open).toHaveBeenCalledTimes(1);
  });
  it('retains explicit Finding selection when a more severe Finding arrives on reread', () => {
    const { fixture, state, node } = setup('COMPLETED');
    fixture.componentInstance.inspectFinding(finding().id); fixture.detectChanges();
    state.findings.set(loaded([finding(1, { severity: 'CRITICAL', title: 'Nuevo hallazgo crítico' }), finding()])); fixture.detectChanges();
    expect(fixture.componentInstance.selectedFindingId()).toBe(finding().id);
    expect(node.querySelector('app-finding-intelligence-panel')?.textContent).not.toContain('Nuevo hallazgo crítico');
    expect(state.open).toHaveBeenCalledTimes(1);
  });
  it('preserves the score count when the independently loaded Finding list fails', () => {
    const { fixture, state, node } = setup('COMPLETED');
    state.findings.set(failed(new ApiClientError('backend', 'Listado no disponible', 503))); fixture.detectChanges();
    expect(node.querySelector('#risk')?.textContent).toContain('Según resumen del score');
    expect(node.querySelector('#risk app-stat-card strong')?.textContent).toBe('1');
    expect(node.querySelector('#findings [role=alert]')?.textContent).toContain('Listado no disponible');
    expect(node.querySelector('#findings app-empty-state')).toBeNull();
  });
  it('honors a Finding fragment before initial selection without requesting more data', async () => {
    const { fixture, state, node, fragment } = setup('COMPLETED');
    state.findings.set(loaded([finding(), finding(1)])); fixture.detectChanges();
    const row = node.querySelector<HTMLElement>('#finding-' + finding(1).id)!;
    Object.defineProperty(row, 'scrollIntoView', { configurable: true, value: vi.fn() });
    fragment.next('finding-' + finding(1).id); fixture.detectChanges(); await fixture.whenStable();
    expect(fixture.componentInstance.selectedFindingId()).toBe(finding(1).id);
    expect(node.querySelector('app-finding-intelligence-panel')?.textContent).toContain(finding(1).securityTestCode);
    expect(state.open).toHaveBeenCalledTimes(1);
  });
});
