import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { RiskSummary } from './risk-summary';
import { CategorySummary } from './category-summary';
import { FindingList } from './finding-list';
import { EvaluationList } from './evaluation-list';
import { ReportSections } from './report-sections';
import { ScanProgress } from './scan-progress';
import { confidenceLabel, riskLabels, sortFindings } from '../../../shared/presentation/labels';
import { finding, score, evaluation, execution, scenario, report, id } from '../../../../testing/fixtures';
describe('Scan results presentation', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideRouter([])] }));
  it.each(['negative', 'inconclusive'] as const)('shows real 0 for %s without certifying safety', kind => {
    const fixture = TestBed.createComponent(RiskSummary); fixture.componentRef.setInput('score', scenario(kind).score); fixture.detectChanges(); const text = fixture.nativeElement.textContent;
    expect(text).toContain('0/100'); expect(text).toContain('Sin hallazgos detectados'); expect(text).toContain('Esto no demuestra'); expect(text).not.toContain('0%'); if (kind === 'inconclusive') expect(text).toContain('Hay 4 resultados inconclusos');
  });
  it.each(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'] as const)('presents %s from the API enum without deriving thresholds', riskLevel => {
    const fixture = TestBed.createComponent(RiskSummary); fixture.componentRef.setInput('score', score({ riskLevel })); fixture.detectChanges(); expect(fixture.nativeElement.textContent.toLowerCase()).toContain(riskLabels[riskLevel].toLowerCase());
  });
  it('shows every category unchanged, including zero with inconclusive and output-only LLM10', () => {
    const fixture = TestBed.createComponent(CategorySummary); fixture.componentRef.setInput('categories', scenario('inconclusive').score.categories); fixture.detectChanges(); expect(fixture.nativeElement.querySelectorAll('article')).toHaveLength(4); expect(fixture.nativeElement.textContent).toContain('Solo salida observable'); expect(fixture.nativeElement.textContent).toContain('Inconclusos: 1');
  });
  it('orders by severity then category then lowercase ID without changing source arrays', () => {
    const items = [finding(1, { severity: 'MEDIUM', confidence: 1 }), finding(0, { severity: 'HIGH', confidence: .1 }), finding(3, { severity: 'CRITICAL' }), finding(2, { severity: 'HIGH', category: 'LLM01', id: id(99) })];
    const original = [...items]; expect(sortFindings(items).map(item => item.id)).toEqual([id(33), id(30), id(99), id(31)]); expect(items).toEqual(original); expect(confidenceLabel(.85)).toBe('Confianza heurística: 85 %');
  });
  it('the selected intelligence panel keeps narratives and recommendations, XSS remains inert', () => {
    const fixture = TestBed.createComponent(FindingList); const attack = '<img src=x onerror="window.attack=true"><script>attack()</script>';
    fixture.componentRef.setInput('findings', [finding(0, { title: attack, description: attack, evidenceSummary: attack }), finding(1)]); fixture.detectChanges(); const node: HTMLElement = fixture.nativeElement;
    expect(node.querySelectorAll('.finding-row')).toHaveLength(2); expect(node.querySelectorAll('img,script')).toHaveLength(0); expect(node.textContent).toContain(attack); expect(node.querySelector('app-finding-intelligence-panel')?.textContent).toContain('Review the output boundary.'); expect(node.textContent).toContain('no es una probabilidad calibrada'); const buttons = node.querySelectorAll<HTMLButtonElement>('.inspect'); buttons[1].click(); fixture.detectChanges(); expect(node.querySelector('app-finding-intelligence-panel')?.textContent).toContain('Review the output boundary.');
  });
  it('separates inconclusive, negative, degraded vulnerable and failed evaluation semantics', () => {
    const fixture = TestBed.createComponent(EvaluationList); const mixed = scenario('degraded'); fixture.componentRef.setInput('evaluations', [...mixed.evaluations.slice(0, 3), evaluation(3, { status: 'FAILED', finalVerdict: 'INCONCLUSIVE', confidence: 0, errorReason: 'EVALUATION_ABORTED' })]); fixture.detectChanges(); const text = fixture.nativeElement.textContent;
    expect(text).toContain('Vulnerable'); expect(text).toContain('No vulnerable en esta prueba'); expect(text).toContain('Resultado inconcluso'); expect(text).toContain('Evidencia insuficiente'); expect(text).toContain('Fallo técnico de evaluación'); expect(text).toContain('componente LLM degradado'); expect(text).not.toContain('synthetic-provider'); expect(text).not.toContain('Internal synthetic value'); expect(text).not.toContain('HYB-01');
  });
  it('does not invent a reason for null inconclusive and explains confidence zero', () => {
    const fixture = TestBed.createComponent(EvaluationList); fixture.componentRef.setInput('evaluations', [evaluation(0, { finalVerdict: 'INCONCLUSIVE', confidence: 0, outcomeReason: null })]); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('No hay soporte suficiente'); expect(fixture.nativeElement.textContent).not.toContain('No hay contexto');
  });
  it('reports snapshot scope, four interpretations and nine literal limitations without duplicate narratives', () => {
    const data = scenario('mixed'); const fixture = TestBed.createComponent(ReportSections); fixture.componentRef.setInput('report', data.report); fixture.componentRef.setInput('score', data.score); fixture.detectChanges(); const node: HTMLElement = fixture.nativeElement;
    expect(node.querySelectorAll('ol>li')).toHaveLength(9); for (const item of data.report.limitations) expect(node.textContent).toContain(item.text); for (const value of Object.values(data.report.interpretation)) expect(node.textContent).toContain(value);
    expect(node.textContent).toContain('coincide'); expect(node.textContent).toContain(data.report.scope.targetConfigurationId); expect(node.textContent).not.toContain('Synthetic description'); expect(node.textContent).not.toContain('synthetic.example.test');
  });
  it('can show the labelled report summary when score failed, including no findings', () => {
    const fixture = TestBed.createComponent(ReportSections); fixture.componentRef.setInput('report', report()); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('Resumen propio del informe: 0/100'); expect(fixture.nativeElement.querySelectorAll('ol>li')).toHaveLength(9);
  });
  it('report text is inert even for an adversarial fixture', () => {
    const fixture = TestBed.createComponent(ReportSections); const data = report(); fixture.componentRef.setInput('report', { ...data, interpretation: { ...data.interpretation, scoreMeaning: '<script>syntheticAttack()</script>' } }); fixture.detectChanges(); expect(fixture.nativeElement.querySelector('script')).toBeNull(); expect(fixture.nativeElement.textContent).toContain('<script>');
  });
  it('progress uses observed terminal rows and does not infer a phase or percentage', () => {
    const fixture = TestBed.createComponent(ScanProgress); fixture.componentRef.setInput('executions', [execution(), execution(1, { status: 'FAILED' })]); fixture.componentRef.setInput('evaluations', [evaluation(0, { status: 'RUNNING' })]); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('2 de 2 pruebas finalizadas técnicamente'); expect(fixture.nativeElement.textContent).toContain('0 de 1 evaluaciones'); expect(fixture.nativeElement.textContent).not.toContain('%');
  });
});
