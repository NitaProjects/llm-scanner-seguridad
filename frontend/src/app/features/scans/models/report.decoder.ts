import { object, uuid, date, text, nullable, number, bool, oneOf, array, record } from '../../../core/api/decode';
import { contractError } from '../../../core/api/api-error';
import { categories, verdicts, severities, outcomeReasons, llmErrors } from '../../../shared/models/contracts';
import { SecurityReportDto, ReportScopeDto, ReportEvaluationDto, ReportVersionsDto, ReportInterpretationDto, ReportLimitationDto, ReportResponse, limitationCodes, reportTestCodes } from './report';
import { decodeScore, decodeFinding } from './scan.decoder';
const versions = object<ReportVersionsDto>({ engineVersion: nullable(text), policyVersion: nullable(text), confidenceVersion: nullable(text), severityVersion: nullable(text), promptVersion: nullable(text), schemaVersion: nullable(text), llmInputProfile: nullable(text) });
const reportTestCode = oneOf(reportTestCodes);
const evaluation = object<ReportEvaluationDto>({
  id: uuid, testExecutionId: uuid, securityTestCode: reportTestCode, securityTestRevision: oneOf([1]), category: oneOf(categories), status: oneOf(['COMPLETED']), finalVerdict: oneOf(verdicts), confidence: number(0, 1, false), severity: nullable(oneOf(severities)), outcomeReason: nullable(oneOf(outcomeReasons)), llmStatus: oneOf(['SUCCEEDED', 'FAILED', 'SKIPPED']), llmErrorReason: nullable(oneOf(llmErrors)), degraded: bool, evidenceComplete: bool, criteriaId: nullable(text), criteriaVersion: nullable(text), versions, finishedAt: date, findingId: nullable(uuid)
});
const report = object<SecurityReportDto>({
  reportId: uuid, scanId: uuid, projectId: uuid, generatedAt: date, reportVersion: oneOf(['report-v1']), locale: oneOf(['es']),
  scope: object<ReportScopeDto>({ scanCreatedAt: date, scanStartedAt: date, targetConfigurationId: uuid, targetUpdatedAt: date }), summary: decodeScore,
  evaluations: array(evaluation, 4, 4), findings: array(value => { const result = decodeFinding(value); return { ...result, securityTestCode: reportTestCode(result.securityTestCode), securityTestRevision: oneOf([1])(result.securityTestRevision) }; }, 0, 4),
  interpretation: object<ReportInterpretationDto>({ scoreMeaning: text, confidenceMeaning: text, verdictMeaning: text, technicalFailureMeaning: text }),
  limitations: array(object<ReportLimitationDto>({ code: oneOf(limitationCodes), text }), 9, 9)
});
export function decodeReportResponse(value: unknown): ReportResponse {
  const source = record(value);
  const availability = oneOf(['AVAILABLE', 'PENDING', 'UNAVAILABLE', 'NOT_APPLICABLE'])(source['availability']);
  if (availability === 'AVAILABLE') {
    const result = report(source['report']);
    if (!result.limitations.every((item, index) => item.code === limitationCodes[index])) throw contractError();
    return { availability, report: result };
  }
  if (source['report'] !== null) throw contractError();
  return { availability, report: null };
}
