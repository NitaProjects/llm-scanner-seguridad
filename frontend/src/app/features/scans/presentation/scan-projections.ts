import { sameId } from '../../../core/api/decode';
import { categories, Category } from '../../../shared/models/contracts';
import { CategoryScoreDto, EvaluationMetadataDto, FindingDto, TestExecutionMetadataDto } from '../models/scan';

export interface TestRail {
  readonly category: Category;
  readonly execution: TestExecutionMetadataDto | null;
  readonly evaluation: EvaluationMetadataDto | null;
  readonly findings: readonly FindingDto[];
  readonly warning: string | null;
}

export interface RiskBand {
  readonly category: Category;
  readonly score: CategoryScoreDto | null;
  readonly rail: TestRail | null;
}

function unique<T>(items: readonly T[]): T | null { return items.length === 1 ? items[0] : null; }
function uniqueId<T extends { readonly id: string }>(items: readonly T[], id: string): T | null {
  return unique(items.filter(item => sameId(item.id, id)));
}
function identity(a: TestExecutionMetadataDto | EvaluationMetadataDto | FindingDto, b: TestExecutionMetadataDto | EvaluationMetadataDto | FindingDto) {
  return sameId(a.scanId, b.scanId) && a.category === b.category && a.securityTestCode === b.securityTestCode && a.securityTestRevision === b.securityTestRevision;
}

/** No category-only joins: ambiguous or incompatible records remain in the public indexes. */
export function testRails(scanId: string, executions: readonly TestExecutionMetadataDto[], evaluations: readonly EvaluationMetadataDto[], findings: readonly FindingDto[]): readonly TestRail[] {
  return categories.map(category => {
    const candidates = executions.filter(item => sameId(item.scanId, scanId) && item.category === category);
    const candidate = unique(candidates);
    const execution = candidate && uniqueId(executions, candidate.id) ? candidate : null;
    const matching = execution ? evaluations.filter(item => sameId(item.testExecutionId, execution.id)) : [];
    const evaluationCandidate = unique(matching);
    const evaluation = execution && evaluationCandidate && uniqueId(evaluations, evaluationCandidate.id) && identity(execution, evaluationCandidate) ? evaluationCandidate : null;
    const related = execution && evaluation ? findings.filter(item =>
      uniqueId(findings, item.id) && sameId(item.evaluationResultId, evaluation.id) && sameId(item.testExecutionId, execution.id) &&
      identity(item, execution) && identity(item, evaluation) && evaluation.status === 'COMPLETED' && evaluation.finalVerdict === item.verdict &&
      (!evaluation.findingId || sameId(item.id, evaluation.findingId))
    ) : [];
    const incompatible = candidates.length > 1 || (candidate !== null && execution === null) || matching.length > 0 && !evaluation ||
      evaluations.some(item => item.category === category && (!sameId(item.scanId, scanId) || execution !== null && evaluation === null));
    return { category, execution, evaluation, findings: related, warning: incompatible ? 'Correlación no verificable. Consulta los registros recibidos; no se atribuye evidencia entre referencias incompatibles o duplicadas.' : null };
  });
}

export function riskBands(scores: readonly CategoryScoreDto[], rails: readonly TestRail[]): readonly RiskBand[] {
  return categories.map(category => ({ category, score: unique(scores.filter(item => item.category === category)), rail: unique(rails.filter(item => item.category === category)) }));
}

export function findingCorrelated(finding: FindingDto, rails: readonly TestRail[]): boolean {
  return rails.some(rail => rail.findings.some(item => item === finding));
}

export function selectedFinding(items: readonly FindingDto[], id: string | null): FindingDto | null {
  return id ? uniqueId(items, id) : null;
}
