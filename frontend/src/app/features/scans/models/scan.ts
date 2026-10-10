import { Category, Lifecycle, Verdict, Severity, OutcomeReason, LlmErrorReason, ExecutionErrorReason, EvaluationErrorReason, ScanErrorReason, RiskLevel } from '../../../shared/models/contracts';

export interface ScanDto {
  readonly id: string;
  readonly projectId: string;
  readonly status: Lifecycle;
  readonly createdAt: string;
  readonly startedAt: string | null;
  readonly finishedAt: string | null;
  readonly errorReason: ScanErrorReason | null;
}
export interface TestExecutionMetadataDto {
  readonly id: string;
  readonly scanId: string;
  readonly securityTestCode: string;
  readonly securityTestRevision: number;
  readonly securityTestName: string;
  readonly category: Category;
  readonly status: Lifecycle;
  readonly startedAt: string | null;
  readonly finishedAt: string | null;
  readonly httpStatus: number | null;
  readonly latencyMs: number | null;
  readonly errorReason: ExecutionErrorReason | null;
  readonly requestRedacted: boolean;
  readonly responseRedacted: boolean;
}
export interface EvaluationMetadataDto {
  readonly id: string;
  readonly scanId: string;
  readonly testExecutionId: string;
  readonly securityTestCode: string;
  readonly securityTestRevision: number;
  readonly category: Category;
  readonly status: Lifecycle;
  readonly finalVerdict: Verdict | null;
  readonly confidence: number | null;
  readonly severity: Severity | null;
  readonly llmStatus: 'NOT_REQUESTED' | 'SUCCEEDED' | 'FAILED' | 'SKIPPED';
  readonly llmErrorReason: LlmErrorReason | null;
  readonly evaluatorProvider: string | null;
  readonly evaluatorModel: string | null;
  readonly criteriaId: string | null;
  readonly criteriaVersion: string | null;
  readonly decisionCode: 'HYB-01' | 'HYB-02' | 'HYB-03' | 'HYB-04' | 'HYB-05' | 'HYB-06' | 'HYB-07' | null;
  readonly degraded: boolean;
  readonly evidenceComplete: boolean;
  readonly outcomeReason: OutcomeReason | null;
  readonly errorReason: EvaluationErrorReason | null;
  readonly reasoningSummary: string | null;
  readonly recommendation: string | null;
  readonly startedAt: string | null;
  readonly finishedAt: string | null;
  readonly findingId: string | null;
}
export interface EvaluationListingDto {
  readonly evaluationProfile: 'evaluation-engine-v1' | null;
  readonly items: readonly EvaluationMetadataDto[];
}
export interface FindingDto {
  readonly id: string;
  readonly evaluationResultId: string;
  readonly scanId: string;
  readonly testExecutionId: string;
  readonly securityTestCode: string;
  readonly securityTestRevision: number;
  readonly category: Category;
  readonly verdict: 'VULNERABLE';
  readonly severity: Severity;
  readonly confidence: number;
  readonly title: string;
  readonly description: string;
  readonly evidenceSummary: string;
  readonly recommendation: string;
  readonly createdAt: string;
}
export type SeverityCounts = Readonly<Record<Severity, number>>;
export interface CategoryScoreDto {
  readonly category: Category;
  readonly score: number;
  readonly riskLevel: RiskLevel;
  readonly evaluationCount: 1;
  readonly findingCount: number;
  readonly notVulnerableCount: number;
  readonly inconclusiveCount: number;
  readonly severityCounts: SeverityCounts;
}
export interface ScanScoreDto {
  readonly scanId: string;
  readonly scoringVersion: 'scoring-v1';
  readonly calculatedAt: string;
  readonly globalScore: number;
  readonly riskLevel: RiskLevel;
  readonly evaluationCount: 4;
  readonly findingCount: number;
  readonly notVulnerableCount: number;
  readonly inconclusiveCount: number;
  readonly severityCounts: SeverityCounts;
  readonly categories: readonly CategoryScoreDto[];
}
export type ScoreResponse =
  | { readonly availability: 'AVAILABLE'; readonly score: ScanScoreDto }
  | { readonly availability: 'PENDING' | 'UNAVAILABLE' | 'NOT_APPLICABLE'; readonly score: null };
