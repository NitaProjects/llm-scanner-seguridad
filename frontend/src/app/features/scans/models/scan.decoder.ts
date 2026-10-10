import { object, uuid, text, date, nullable, number, bool, oneOf, array, record } from '../../../core/api/decode';
import { contractError } from '../../../core/api/api-error';
import { categories, statuses, scanErrors, executionErrors, evaluationErrors, verdicts, severities, llmErrors, outcomeReasons, riskLevels } from '../../../shared/models/contracts';
import { ScanDto, TestExecutionMetadataDto, EvaluationMetadataDto, EvaluationListingDto, FindingDto, SeverityCounts, CategoryScoreDto, ScanScoreDto, ScoreResponse } from './scan';
const confidence = number(0, 1, false);
export const decodeScan = object<ScanDto>({ id: uuid, projectId: uuid, status: oneOf(statuses), createdAt: date, startedAt: nullable(date), finishedAt: nullable(date), errorReason: nullable(oneOf(scanErrors)) });
export const decodeExecution = object<TestExecutionMetadataDto>({
  id: uuid, scanId: uuid, securityTestCode: text, securityTestRevision: number(1, Infinity), securityTestName: text,
  category: oneOf(categories), status: oneOf(statuses), startedAt: nullable(date), finishedAt: nullable(date),
  httpStatus: nullable(number(100, 599)), latencyMs: nullable(number(0, Infinity)), errorReason: nullable(oneOf(executionErrors)), requestRedacted: bool, responseRedacted: bool
});
export const decodeEvaluation = object<EvaluationMetadataDto>({
  id: uuid, scanId: uuid, testExecutionId: uuid, securityTestCode: text, securityTestRevision: number(1, Infinity), category: oneOf(categories), status: oneOf(statuses),
  finalVerdict: nullable(oneOf(verdicts)), confidence: nullable(confidence), severity: nullable(oneOf(severities)),
  llmStatus: oneOf(['NOT_REQUESTED', 'SUCCEEDED', 'FAILED', 'SKIPPED']), llmErrorReason: nullable(oneOf(llmErrors)),
  evaluatorProvider: nullable(text), evaluatorModel: nullable(text), criteriaId: nullable(text), criteriaVersion: nullable(text),
  decisionCode: nullable(oneOf(['HYB-01', 'HYB-02', 'HYB-03', 'HYB-04', 'HYB-05', 'HYB-06', 'HYB-07'])), degraded: bool, evidenceComplete: bool,
  outcomeReason: nullable(oneOf(outcomeReasons)), errorReason: nullable(oneOf(evaluationErrors)), reasoningSummary: nullable(text), recommendation: nullable(text),
  startedAt: nullable(date), finishedAt: nullable(date), findingId: nullable(uuid)
});
export const decodeEvaluations = object<EvaluationListingDto>({ evaluationProfile: nullable(oneOf(['evaluation-engine-v1'])), items: array(decodeEvaluation, 0, 4) });
export const decodeFinding = object<FindingDto>({
  id: uuid, evaluationResultId: uuid, scanId: uuid, testExecutionId: uuid, securityTestCode: text, securityTestRevision: number(1, Infinity),
  category: oneOf(categories), verdict: oneOf(['VULNERABLE']), severity: oneOf(severities), confidence, title: text, description: text, evidenceSummary: text, recommendation: text, createdAt: date
});
const count = number(0, 4);
export const decodeSeverityCounts = object<SeverityCounts>({ LOW: count, MEDIUM: count, HIGH: count, CRITICAL: count });
export const decodeCategory = object<CategoryScoreDto>({ category: oneOf(categories), score: number(0, 100), riskLevel: oneOf(riskLevels), evaluationCount: oneOf([1]), findingCount: number(0, 1), notVulnerableCount: number(0, 1), inconclusiveCount: number(0, 1), severityCounts: decodeSeverityCounts });
const scoreShape = object<ScanScoreDto>({
  scanId: uuid, scoringVersion: oneOf(['scoring-v1']), calculatedAt: date, globalScore: number(0, 100), riskLevel: oneOf(riskLevels),
  evaluationCount: oneOf([4]), findingCount: count, notVulnerableCount: count, inconclusiveCount: count, severityCounts: decodeSeverityCounts, categories: array(decodeCategory, 4, 4)
});
export function decodeScore(value: unknown): ScanScoreDto {
  const result = scoreShape(value);
  if (!result.categories.every((item, index) => item.category === categories[index])) throw contractError();
  return result;
}
export function decodeScoreResponse(value: unknown): ScoreResponse {
  const source = record(value);
  const availability = oneOf(['AVAILABLE', 'PENDING', 'UNAVAILABLE', 'NOT_APPLICABLE'])(source['availability']);
  if (availability === 'AVAILABLE') return { availability, score: decodeScore(source['score']) };
  if (source['score'] !== null) throw contractError();
  return { availability, score: null };
}
export function sameScore(left: ScanScoreDto, right: ScanScoreDto): boolean {
  // Los decoders proyectan siempre las mismas claves, sin calcular ni reparar valores.
  return JSON.stringify(left) === JSON.stringify(right);
}
