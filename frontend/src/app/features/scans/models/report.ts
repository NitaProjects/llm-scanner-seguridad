import { Category, Verdict, Severity, OutcomeReason, LlmErrorReason } from '../../../shared/models/contracts';
import { FindingDto, ScanScoreDto } from './scan';
export const reportTestCodes = ['LLM01-INSTRUCTION-001', 'LLM02-CANARY-001', 'LLM08-CONTEXT-001', 'LLM10-MARKUP-001'] as const;
export type ReportFindingDto = Omit<FindingDto, 'securityTestCode' | 'securityTestRevision'> & { readonly securityTestCode: typeof reportTestCodes[number]; readonly securityTestRevision: 1 };

export interface ReportScopeDto {
  readonly scanCreatedAt: string;
  readonly scanStartedAt: string;
  readonly targetConfigurationId: string;
  readonly targetUpdatedAt: string;
}
export interface ReportVersionsDto {
  readonly engineVersion: string | null;
  readonly policyVersion: string | null;
  readonly confidenceVersion: string | null;
  readonly severityVersion: string | null;
  readonly promptVersion: string | null;
  readonly schemaVersion: string | null;
  readonly llmInputProfile: string | null;
}
export interface ReportEvaluationDto {
  readonly id: string;
  readonly testExecutionId: string;
  readonly securityTestCode: typeof reportTestCodes[number];
  readonly securityTestRevision: 1;
  readonly category: Category;
  readonly status: 'COMPLETED';
  readonly finalVerdict: Verdict;
  readonly confidence: number;
  readonly severity: Severity | null;
  readonly outcomeReason: OutcomeReason | null;
  readonly llmStatus: 'SUCCEEDED' | 'FAILED' | 'SKIPPED';
  readonly llmErrorReason: LlmErrorReason | null;
  readonly degraded: boolean;
  readonly evidenceComplete: boolean;
  readonly criteriaId: string | null;
  readonly criteriaVersion: string | null;
  readonly versions: ReportVersionsDto;
  readonly finishedAt: string;
  readonly findingId: string | null;
}
export interface ReportInterpretationDto {
  readonly scoreMeaning: string;
  readonly confidenceMeaning: string;
  readonly verdictMeaning: string;
  readonly technicalFailureMeaning: string;
}
export const limitationCodes = ['IMPLEMENTED_CATEGORIES_ONLY', 'TARGET_REFERENCE_ONLY', 'POINT_IN_TIME_TESTS', 'LLM10_OUTPUT_ONLY', 'POSSIBLE_FALSE_RESULTS', 'INCONCLUSIVE_RESULTS', 'HEURISTIC_METRICS', 'EXCLUDED_CAPABILITIES', 'SYNTHETIC_SCENARIO_SCOPE'] as const;
export interface ReportLimitationDto { readonly code: typeof limitationCodes[number]; readonly text: string }
export interface SecurityReportDto {
  readonly reportId: string;
  readonly scanId: string;
  readonly projectId: string;
  readonly generatedAt: string;
  readonly reportVersion: 'report-v1';
  readonly locale: 'es';
  readonly scope: ReportScopeDto;
  readonly summary: ScanScoreDto;
  readonly evaluations: readonly ReportEvaluationDto[];
  readonly findings: readonly ReportFindingDto[];
  readonly interpretation: ReportInterpretationDto;
  readonly limitations: readonly ReportLimitationDto[];
}
export type ReportResponse =
  | { readonly availability: 'AVAILABLE'; readonly report: SecurityReportDto }
  | { readonly availability: 'PENDING' | 'UNAVAILABLE' | 'NOT_APPLICABLE'; readonly report: null };
