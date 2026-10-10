# ADR-008: Reporting como snapshot estructurado inmutable

## Estado

Aceptada. D-01..D-24 aprobadas por el usuario el 2026-10-10. Contrato exacto y escenarios: [design de reporting](../../../openspec/changes/reporting/design.md) y [RP-01..RP-14](../../../openspec/changes/reporting/specs/reporting/spec.md).

## Contexto

EvaluationResults, Findings y ScanScore son finales persistidos. Una vista reconstruida en cada consulta cambiaría con nombres/configuración/textos actuales. El informe debe conservar resultados, incertidumbre y alcance sin una segunda evaluación ni fórmula.

## Decisión

SecurityReport es un resultado inmutable identificado por reportId y único por scanId+reportVersion. `report-v1`, locale `es`, fija estructura, textos propios, orden e interpretación; no es configurable. Conserva doce campos: reportId, scanId, projectId, generatedAt, reportVersion, locale, scope, summary, evaluations, findings, interpretation y limitations. Los VOs están dentro del resultado, sin repositorios ni lifecycle por sección.

Scope conserva scanCreatedAt/scanStartedAt y targetConfigurationId/targetUpdatedAt idénticos en las cuatro ejecuciones. No hay URL histórica recuperable, nombres actuales ni copia de configuración. Summary copia los once campos de ScanScore y su breakdown sin construir ese agregado ni invocar scoring. Se verifican contadores y vínculos, no se recalculan riesgo, verdict, severity o confidence.

Evaluations copia solo los diecinueve campos aprobados, incluidos códigos públicos de motivo/componente y siete identificadores de versiones originales o null; excluye assessment, reasoning, señales internas, evidencia y contenidos de prompts/schemas. Findings copia los quince campos públicos. title, description, evidenceSummary y recommendation permanecen literalmente en su idioma original, incluso inglés con locale es. Una recommendation por Finding, sin deduplicación ni narrativa global. Orden: CRITICAL, HIGH, MEDIUM, LOW; categoría; UUID lowercase lexicográfico. Evaluations se ordena por code/revision/id; categorías conservan LLM01/02/08/10.

PresentationPort entrega constantes empaquetadas `reporting/report-v1/es.json`: cuatro interpretaciones, nueve limitaciones y labels propios (HIGH: Alta para severity y Alto para RiskLevel). No executiveSummaryText, traducción de Finding, renderer, LLM ni servicio externo. Los textos se copian al snapshot para preservar históricos. Cambiar presentación propia exige otra versión y conservar reader/recurso v1.

INCONCLUSIVE permanece en contadores, proyección y motivos; no equivale a NOT_VULNERABLE ni a fallo global. Un componente LLM fallido consolidado conserva su código y verdict final. Score 0 significa: «No se detectaron Findings en las pruebas completadas. Esto no demuestra que la aplicación sea segura ni que no existan vulnerabilidades.» Score/confidence son heurísticos, no probabilidades calibradas, porcentajes de seguridad ni cobertura. Las nueve limitaciones literales y ordenadas se especifican en D-09: categorías implementadas, referencia de target, pruebas puntuales, LLM10 OUTPUT_ONLY, falsos resultados, inconclusos, métricas heurísticas, capacidades excluidas y escenarios sintéticos autorizados.

T1 fija report_version junto con scoring/evaluation antes de dispatch. T3 bloquea Scan, valida integridad durable, inserta score, genera/inserta report desde esa fila y confirma COMPLETED conjuntamente. No commit intermedio, nueva fase ni transacción propia del generador. finishedAt >= generatedAt >= calculatedAt y fechas de fuentes. Error revierte T3 y aplica compensación técnica existente; commit ambiguo se resuelve conservando estado durable. Recovery no relanza/regenera; valida antes de admisión, conserva conjunto confirmado e interrumpe activos. Corrupción no se repara silenciosamente.

V7 añade metadata relacional con PK reportId, UNIQUE(scan_id,report_version), FKs a Scan/ScanScore y payload JSONB cerrado validado en dominio/adaptadores/SQL. No tablas hijas, hash, backfill, cambios V1–V6 ni uso de scans.global_score. Máximo cuatro Findings deriva de cuatro tests, una evaluación por categoría y máximo un Finding por evaluación; futuros planes requieren otra versión. Longitudes originales: title150 y otros tres textos1000 puntos de código. Snapshot JSONB textual UTF-8 y objeto API <=262144 bytes, wrapper<=512, sin truncamiento. Unicode inválido/NUL/markup prohibido fallan.

GET scoped `/api/projects/{projectId}/scans/{scanId}/report` es de solo lectura y devuelve `{availability,report}` con no-store en éxito/errores. AVAILABLE exige report/score/COMPLETED coherentes, incluso score0; PENDING activo marcado; UNAVAILABLE FAILED marcado; NOT_APPLICABLE histórico sin marcador aunque tenga score. Ausencias llevan report=null. Integridad inválida produce500 sanitizado, no falsa ausencia. No joins a target/Findings actuales para reconstruir presentación.

## Alternativas descartadas

- Ensamblado/lazy en GET: cambia historia e introduce escrituras/carreras al consultar.
- Report después de otro commit: permite COMPLETED sin informe o score durable en Scan activo.
- Copia de entidades/configuración/evidencia: duplica datos sensibles y rompe minimización.
- Normalizar cada sección: añade tablas/lifecycles para analítica fuera del MVP; JSON libre pierde contrato e integridad.
- Narrativa o traducción LLM: altera fuentes, añade coste/red y no mejora reproducibilidad.

## Consecuencias

Reporting puede impedir completar un análisis con Findings correctos si falla técnicamente; estos se conservan consultables. La duplicación mínima del resumen es intencional, cotejada por igualdad con score persistido. No se promete anonimato arbitrario por regex: la privacidad depende de proyecciones positivas y plantillas upstream. Inmutabilidad no equivale a auditoría criptográfica frente a cambios administrativos. Una versión antigua del worker desconoce el nuevo guard; volver al binario anterior no constituye un rollback operativo seguro. Preferir corrección hacia delante mediante procedimiento autorizado.

## Fuera del MVP / Evolución futura

Frontend, HTML/Markdown/PDF/DOCX, descargas, email, S3/AWS, múltiples idiomas, informes parciales, regeneración histórica, historial seguro de target, retención/borrado y autenticación multiusuario/publicación. Ninguna evolución puede reinterpretar automáticamente resultados históricos. La checklist TFM conserva sus estados hasta contar con evidencia específica de cada entregable académico.
