import { ProjectDto } from '../app/features/projects/models/project';
import { TargetConfigurationDto, LegacyTargetConfigurationDto, AuthType } from '../app/features/target/models/target';
import { ScanDto, EvaluationMetadataDto, TestExecutionMetadataDto, FindingDto, ScanScoreDto } from '../app/features/scans/models/scan';
import { SecurityReportDto, limitationCodes, reportTestCodes } from '../app/features/scans/models/report';
import { categories, Severity } from '../app/shared/models/contracts';
export const id = (index: number) => `00000000-0000-0000-0000-${index.toString().padStart(12, '0')}`;
export const projectId = id(1); export const scanId = id(2); export const timestamp = '2026-10-10T12:00:00.000000Z';
export const project: ProjectDto = { id: projectId, name: 'Proyecto sintético', description: null, createdAt: timestamp, updatedAt: timestamp };
export function target(authType: AuthType = 'NONE', overrides: Partial<LegacyTargetConfigurationDto> = {}): LegacyTargetConfigurationDto {
  return { id: id(3), projectId, name: null, url: 'https://synthetic.example.test/chat', method: 'POST', contentType: 'application/json', requestTemplate: '{"message":"{{PROMPT}}"}', promptPlaceholder: '{{PROMPT}}', responsePath: '$.text', headers: {}, timeoutMs: 15000, hasCredentials: authType !== 'NONE', createdAt: timestamp, updatedAt: timestamp, authType, authConfig: authType === 'API_KEY_HEADER' ? { headerName: 'X-API-Key' } : null, ...overrides } as LegacyTargetConfigurationDto;
}
export function scan(overrides: Partial<ScanDto> = {}): ScanDto {
  const status = overrides.status ?? 'RUNNING';
  return { id: scanId, projectId, status, createdAt: timestamp, startedAt: status === 'PENDING' ? null : timestamp, finishedAt: status === 'COMPLETED' || status === 'FAILED' ? timestamp : null, errorReason: status === 'FAILED' ? 'SCAN_PROCESSING_FAILED' : null, ...overrides };
}
export const testCodes = ['LLM01-INSTRUCTION-001', 'LLM02-CANARY-001', 'LLM08-CONTEXT-001', 'LLM10-MARKUP-001'] as const;
export function execution(index = 0, overrides: Partial<TestExecutionMetadataDto> = {}): TestExecutionMetadataDto {
  return { id: id(10 + index), scanId, securityTestCode: testCodes[index], securityTestRevision: 1, securityTestName: 'Prueba sintética', category: categories[index], status: 'COMPLETED', startedAt: timestamp, finishedAt: timestamp, httpStatus: 200, latencyMs: 12, errorReason: null, requestRedacted: true, responseRedacted: true, ...overrides };
}
export function evaluation(index = 0, overrides: Partial<EvaluationMetadataDto> = {}): EvaluationMetadataDto {
  return { id: id(20 + index), scanId, testExecutionId: id(10 + index), securityTestCode: testCodes[index], securityTestRevision: 1, category: categories[index], status: 'COMPLETED', finalVerdict: 'NOT_VULNERABLE', confidence: .85, severity: null, llmStatus: 'SUCCEEDED', llmErrorReason: null, evaluatorProvider: 'synthetic-provider', evaluatorModel: 'synthetic-model', criteriaId: 'synthetic-criterion', criteriaVersion: 'v1', decisionCode: 'HYB-01', degraded: false, evidenceComplete: true, outcomeReason: null, errorReason: null, reasoningSummary: 'Internal synthetic value', recommendation: 'Internal synthetic value', startedAt: timestamp, finishedAt: timestamp, findingId: null, ...overrides };
}
export function finding(index = 0, overrides: Partial<FindingDto> = {}): FindingDto {
  return { id: id(30 + index), evaluationResultId: id(20 + index), scanId, testExecutionId: id(10 + index), securityTestCode: testCodes[index], securityTestRevision: 1, category: categories[index], verdict: 'VULNERABLE', severity: 'HIGH', confidence: .85, title: 'Synthetic finding', description: 'Synthetic description', evidenceSummary: 'Synthetic evidence summary', recommendation: 'Review the output boundary.', createdAt: timestamp, ...overrides };
}
export function score(overrides: Partial<ScanScoreDto> = {}): ScanScoreDto {
  return { scanId, scoringVersion: 'scoring-v1', calculatedAt: timestamp, globalScore: 0, riskLevel: 'NO_FINDINGS', evaluationCount: 4, findingCount: 0, notVulnerableCount: 4, inconclusiveCount: 0, severityCounts: { LOW: 0, MEDIUM: 0, HIGH: 0, CRITICAL: 0 }, categories: categories.map(category => ({ category, score: 0, riskLevel: 'NO_FINDINGS', evaluationCount: 1, findingCount: 0, notVulnerableCount: 1, inconclusiveCount: 0, severityCounts: { LOW: 0, MEDIUM: 0, HIGH: 0, CRITICAL: 0 } })), ...overrides };
}
export const limitations = [
  'El análisis se limita a las pruebas implementadas para LLM01, LLM02, LLM08 y LLM10; no cubre por completo esas categorías ni el conjunto de OWASP.',
  'Se identifica la configuración analizada mediante su id y fecha persistidos en las ejecuciones. No se conserva aquí la URL ni una copia histórica recuperable de su configuración; la configuración actual puede haber cambiado.',
  'Los resultados corresponden a pruebas puntuales sobre la configuración referenciada; no constituyen una auditoría exhaustiva ni garantizan resultados futuros.',
  'LLM10 observa únicamente la salida del modelo. No comprueba el tratamiento posterior de esa salida ni la explotación o protección de un consumidor.',
  'Los criterios y la evaluación pueden producir falsos positivos y falsos negativos; los hallazgos requieren revisión en su contexto.',
  'Una evaluación inconclusa no confirma ni descarta vulnerabilidades. Debe interpretarse junto con su motivo y los contadores de resultados.',
  'Score y confidence son métricas heurísticas, no probabilidades calibradas, porcentajes de seguridad o medidas de cobertura.',
  'Este MVP no evalúa RAG avanzado, agentes, herramientas ni streaming, ni capacidades fuera de las pruebas implementadas.',
  'La evaluación del MVP depende de criterios y escenarios sintéticos autorizados. La falta de contexto o permiso de envío puede impedir conclusiones; no se acredita validación sobre datos reales ni un benchmark académico completo.'
] as const;
export function report(summary = score(), evaluations = categories.map((_, index) => evaluation(index)), findings: readonly FindingDto[] = []): SecurityReportDto {
  return { reportId: id(4), scanId, projectId, generatedAt: timestamp, reportVersion: 'report-v1', locale: 'es', scope: { scanCreatedAt: timestamp, scanStartedAt: timestamp, targetConfigurationId: id(3), targetUpdatedAt: timestamp }, summary,
    evaluations: evaluations.map(item => ({ id: item.id, testExecutionId: item.testExecutionId, securityTestCode: reportTestCodes[categories.indexOf(item.category)], securityTestRevision: 1, category: item.category, status: 'COMPLETED', finalVerdict: item.finalVerdict!, confidence: item.confidence!, severity: item.severity, outcomeReason: item.outcomeReason, llmStatus: item.llmStatus === 'NOT_REQUESTED' ? 'SKIPPED' : item.llmStatus, llmErrorReason: item.llmErrorReason, degraded: item.degraded, evidenceComplete: item.evidenceComplete, criteriaId: item.criteriaId, criteriaVersion: item.criteriaVersion, versions: { engineVersion: 'evaluation-engine-v1', policyVersion: 'hybrid-v1', confidenceVersion: 'confidence-v1', severityVersion: 'severity-v1', promptVersion: 'prompt-v1', schemaVersion: 'schema-v1', llmInputProfile: 'synthetic-v1' }, finishedAt: item.finishedAt!, findingId: item.findingId })), findings: findings.map(item => ({ ...item, securityTestCode: reportTestCodes[categories.indexOf(item.category)], securityTestRevision: 1 })),
    interpretation: {
      scoreMeaning: 'La puntuación resume el riesgo detectado mediante una heurística de 0 a 100; no es un porcentaje ni una probabilidad de compromiso. No se detectaron Findings en las pruebas completadas cuando el score es 0. Esto no demuestra que la aplicación sea segura ni que no existan vulnerabilidades.',
      confidenceMeaning: 'Confidence expresa el soporte heurístico de la conclusión y es independiente de la severidad del hallazgo. No es una probabilidad calibrada ni una medida de impacto.',
      verdictMeaning: 'NOT_VULNERABLE indica que esa prueba concreta no detectó vulnerabilidad; no acredita seguridad de la categoría o aplicación. INCONCLUSIVE indica que la evaluación no pudo concluir con soporte suficiente y no equivale a NOT_VULNERABLE.',
      technicalFailureMeaning: 'Una evaluación inconclusa no es un fallo técnico del Scan. Un fallo del componente evaluador puede coexistir con un resultado final consolidado y se identifica por separado. Si un fallo técnico global impide completar el Scan, no se genera este informe completo; los resultados técnicos y hallazgos ya confirmados se consultan en sus recursos.'
    }, limitations: limitationCodes.map((code, index) => ({ code, text: limitations[index] })) };
}
export function scenario(kind: 'negative' | 'inconclusive' | 'mixed' | 'degraded' | 'critical') {
  const evaluations = categories.map((_, index) => evaluation(index));
  const findings: FindingDto[] = [];
  let summary = score();
  if (kind === 'inconclusive') {
    evaluations.forEach((item, index) => evaluations[index] = { ...item, finalVerdict: 'INCONCLUSIVE', confidence: 0, outcomeReason: 'CONTEXT_UNAVAILABLE', llmStatus: 'SKIPPED' });
    summary = score({ notVulnerableCount: 0, inconclusiveCount: 4, categories: score().categories.map(item => ({ ...item, notVulnerableCount: 0, inconclusiveCount: 1 })) });
  }
  if (kind === 'mixed' || kind === 'degraded' || kind === 'critical') {
    const severity: Severity = kind === 'critical' ? 'CRITICAL' : 'HIGH';
    const degraded = kind === 'degraded';
    const confidence = degraded ? .6 : .85;
    findings.push(finding(0, { severity, confidence }));
    evaluations[0] = evaluation(0, { finalVerdict: 'VULNERABLE', severity, confidence, findingId: findings[0].id, degraded, llmStatus: degraded ? 'FAILED' : 'SUCCEEDED', llmErrorReason: degraded ? 'EVALUATOR_TIMEOUT' : null });
    evaluations[2] = evaluation(2, { finalVerdict: 'INCONCLUSIVE', confidence: 0, outcomeReason: 'INSUFFICIENT_EVIDENCE' });
    const globalScore = severity === 'CRITICAL' ? 94 : degraded ? 73 : 74;
    const counts = { LOW: 0, MEDIUM: 0, HIGH: severity === 'HIGH' ? 1 : 0, CRITICAL: severity === 'CRITICAL' ? 1 : 0 };
    summary = score({ globalScore, riskLevel: severity, findingCount: 1, notVulnerableCount: 2, inconclusiveCount: 1, severityCounts: counts,
      categories: score().categories.map((item, index) => index === 0 ? { ...item, score: globalScore, riskLevel: severity, findingCount: 1, notVulnerableCount: 0, severityCounts: counts } : index === 2 ? { ...item, inconclusiveCount: 1, notVulnerableCount: 0 } : item) });
  }
  return { evaluations, findings, score: summary, report: report(summary, evaluations, findings) };
}

// Caso visual exclusivamente sintético: los cuatro niveles, títulos largos y detalle completo.
export function visualFindingsScenario() {
  const levels = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'] as const;
  const findings = levels.map((severity, index) => finding(index, {
    severity, confidence: severity === 'HIGH' ? .1 : .85,
    title: 'Hallazgo sintético de revisión de límites y tratamiento de información en el asistente autorizado',
    description: 'Descripción sintética para revisar la legibilidad del detalle. '.repeat(8),
    evidenceSummary: 'Evidencia pública sintética de la prueba controlada. '.repeat(6),
    recommendation: 'Revisar el límite de confianza y validar la salida antes de utilizarla. '.repeat(5)
  }));
  const evaluations = levels.map((severity, index) => evaluation(index, {
    finalVerdict: 'VULNERABLE', severity, confidence: findings[index].confidence, findingId: findings[index].id
  }));
  const summary = score({
    globalScore: 100, riskLevel: 'CRITICAL', findingCount: 4, notVulnerableCount: 0,
    severityCounts: { LOW: 1, MEDIUM: 1, HIGH: 1, CRITICAL: 1 },
    categories: score().categories.map((item, index) => ({
      ...item, score: [14, 44, 71, 94][index], riskLevel: levels[index], findingCount: 1, notVulnerableCount: 0,
      severityCounts: { LOW: index === 0 ? 1 : 0, MEDIUM: index === 1 ? 1 : 0, HIGH: index === 2 ? 1 : 0, CRITICAL: index === 3 ? 1 : 0 }
    }))
  });
  return { evaluations, findings, score: summary, report: report(summary, evaluations, findings) };
}
