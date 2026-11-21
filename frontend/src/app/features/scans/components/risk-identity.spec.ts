import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { CategorySummary } from './category-summary';
import { FindingList } from './finding-list';
import { FindingIntelligencePanel } from './finding-intelligence-panel';
import { ScanProgress } from './scan-progress';
import { RiskSummary } from './risk-summary';
import { execution, evaluation, finding, scan, scanId, scenario, score } from '../../../../testing/fixtures';
import { testRails } from '../presentation/scan-projections';

describe('Risk Intelligence presentation', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideRouter([])] }));
  it('labels the heuristic and the count source while keeping zero neutral and inconclusives visible', () => {
    const fixture = TestBed.createComponent(RiskSummary); fixture.componentRef.setInput('score', scenario('inconclusive').score); fixture.componentRef.setInput('scan', scan({ status: 'COMPLETED' })); fixture.componentRef.setInput('duration', '4.581 s'); fixture.detectChanges();
    const node: HTMLElement = fixture.nativeElement;
    expect(node.querySelector('.score')?.textContent).toBe('0/100'); expect(node.textContent).toContain('Score heurístico. No representa probabilidad ni garantía de seguridad.');
    expect(node.textContent).toContain('Según resumen del score'); expect(node.textContent).toContain('Hay 4 resultados inconclusos'); expect(node.textContent).toContain('4.581 s');
    expect(node.textContent).not.toContain('0%'); expect(node.textContent).toContain('Esto no demuestra');
  });
  it('renders comparable bands with an independent inconclusive outcome at score zero', () => {
    const fixture = TestBed.createComponent(CategorySummary);
    fixture.componentRef.setInput('categories', score().categories);
    fixture.componentRef.setInput('rails', testRails(scanId, [execution()], [evaluation(0, { finalVerdict: 'INCONCLUSIVE', confidence: 0 })], [])); fixture.detectChanges();
    const node: HTMLElement = fixture.nativeElement;
    expect(node.querySelectorAll('.risk-band')).toHaveLength(4); expect(node.querySelector('.risk-band .band-score')?.textContent).toContain('0/100');
    expect(node.querySelector('.risk-band .band-outcome')?.textContent).toContain('Resultado inconcluso'); expect(node.textContent).toContain('Solo salida observable');
    expect(node.querySelectorAll('article.card')).toHaveLength(0);
  });
  it('shows missing category score as absent, keeping received evaluation and literal secondary counts', () => {
    const fixture = TestBed.createComponent(CategorySummary); fixture.componentRef.setInput('rails', testRails(scanId, [execution()], [evaluation()], [])); fixture.detectChanges();
    const node: HTMLElement = fixture.nativeElement;
    expect(node.textContent).toContain('Score no disponible'); expect(node.querySelector('.band-outcome')?.textContent).toContain('No vulnerable en esta prueba');
    expect(node.querySelector('.band-score')?.textContent).not.toContain('/100');
    fixture.componentRef.setInput('categories', score().categories); fixture.detectChanges();
    expect(node.textContent).toContain('No vulnerables en esta prueba: 1'); expect(node.textContent).toContain('Severidades: crítica 0');
  });
  it('selects the most severe Finding initially and keeps all entries accessible without executing markup', () => {
    const fixture = TestBed.createComponent(FindingList); const low = finding(0, { severity: 'LOW' }); const high = finding(1, { severity: 'CRITICAL', evidenceSummary: '<img src=x onerror="attack()">' });
    fixture.componentRef.setInput('findings', [low, high]); fixture.detectChanges();
    const node: HTMLElement = fixture.nativeElement;
    expect(node.querySelector('.finding-row')?.id).toBe('finding-' + high.id); expect(node.querySelector('.finding-row .inspect')?.getAttribute('aria-pressed')).toBe('true');
    const panel = node.querySelector('app-finding-intelligence-panel')!;
    expect(panel.textContent).toContain('Resumen de evidencia'); expect(panel.textContent).toContain('<img src=x'); expect(panel.querySelector('img')).toBeNull();
    expect(Array.from(panel.querySelectorAll('h4')).map(item => item.textContent)).toEqual(['EVIDENCIA', 'DECISIÓN', 'RECOMENDACIÓN', 'REFERENCIAS']);
    node.querySelectorAll<HTMLButtonElement>('.inspect')[1].click(); fixture.detectChanges();
    expect(node.querySelector('.finding-row:last-child .inspect')?.getAttribute('aria-pressed')).toBe('true');
  });
  it('keeps orphan data and explicit public references while warning about correlation', () => {
    const fixture = TestBed.createComponent(FindingIntelligencePanel); const data = finding(); fixture.componentRef.setInput('finding', data); fixture.detectChanges();
    const text = fixture.nativeElement.textContent;
    for (const value of [data.description, data.evidenceSummary, data.recommendation, data.securityTestCode, data.id, data.scanId, data.testExecutionId, data.evaluationResultId]) expect(text).toContain(value);
    expect(text).toContain('No se ha podido verificar la cadena completa'); expect(text).toContain('Confianza heurística: 85 %');
  });
  it('does not fabricate a Finding panel when the selected technical test has no Finding', () => {
    const fixture = TestBed.createComponent(FindingList); fixture.componentRef.setInput('findings', [finding()]); fixture.componentRef.setInput('selectedId', null); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-finding-intelligence-panel')).toBeNull(); expect(fixture.nativeElement.textContent).toContain('selección técnica actual');
  });
  it('retains duplicate Finding contents without duplicate anchors or a verified chain', () => {
    const fixture = TestBed.createComponent(FindingList); fixture.componentRef.setInput('findings', [finding(), finding(0, { description: 'Registro duplicado sintético' })]); fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('[id^=finding-]')).toHaveLength(1); expect(fixture.nativeElement.querySelectorAll('details')).toHaveLength(2);
    expect(fixture.nativeElement.textContent).toContain('Registro duplicado sintético'); expect(fixture.nativeElement.textContent).toContain('ID duplicado');
  });
  it('separates selected rail from actual activity and removes activity on stale reads', () => {
    const fixture = TestBed.createComponent(ScanProgress);
    const executions = [execution(), execution(1, { status: 'RUNNING' })]; const evaluations = [evaluation()];
    fixture.componentRef.setInput('executions', executions); fixture.componentRef.setInput('evaluations', evaluations); fixture.componentRef.setInput('rails', testRails(scanId, executions, evaluations, [])); fixture.componentRef.setInput('selectedCategory', 'LLM01'); fixture.componentRef.setInput('executionLive', true); fixture.detectChanges();
    const node: HTMLElement = fixture.nativeElement;
    expect(node.querySelectorAll('.test-rail')).toHaveLength(4);
    expect(node.querySelector('.test-rail.selected')?.textContent).toContain('LLM01'); expect(node.querySelector('.test-rail.active')?.textContent).toContain('LLM02');
    expect(node.querySelector('.test-rail.active')?.getAttribute('aria-pressed')).toBe('false'); expect(node.textContent).toContain('No recibida');
    fixture.componentRef.setInput('executionLive', false); fixture.detectChanges(); expect(node.querySelector('.test-rail.active')).toBeNull();
    expect(node.querySelector('.test-rail.selected')).not.toBeNull(); expect(node.querySelector('.score')).toBeNull();
  });
});
