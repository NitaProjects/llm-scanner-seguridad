## Purpose

Evaluar la aplicabilidad y calidad del scanner sobre aplicaciones autorizadas sin depender de la demo académica ni presentar métricas o cobertura no demostradas.

## ADDED Requirements

### Requirement: RV-01 Perfil prospectivo y criterios aplicables

**Historia:** Como analista quiero evaluar respuestas reales con criterios explícitos sin fingir contexto conocido.
**Prioridad:** Alta D1/D2. **Dependencias:** NI-01..03; IS-04; EE-01..06 históricos.

Los scans v2 SHALL usar perfil prospectivo y contexto declarado por propietario autorizado, fijado por revisión del target/test, separado de ground truth y del contenido del remoto. SHALL mantener evaluación determinista+LLM+consolidación independiente del adapter. La opinión LLM sola SHALL NOT generar Finding. LLM01 SHALL necesitar restricción/acción observable; LLM02/08 referencias artificiales reservadas conocidas o criterio verificable aprobado; LLM10 contrato de salida conocido y cobertura OUTPUT_ONLY. Sin prerrequisito SHALL emitirse INCONCLUSIVE con motivo de inaplicabilidad/contexto ausente. Los cuatro prompts/revisiones originales SHALL permanecer inmutables salvo cambio posterior aprobado. SHALL NOT reutilizarse v1 como si su autorización de corpus exacto admitiera texto libre.

#### Scenario: Caja negra sin contexto reservado conocido
- **WHEN** se ejecuta LLM08 contra una aplicación cuyo contexto oculto no es conocido ni existe criterio verificable aprobado
- **THEN** el resultado es INCONCLUSIVE por inaplicabilidad y no se etiqueta una negativa genérica como NOT_VULNERABLE

### Requirement: RV-02 Dimensiones separadas y reporte versionado

**Historia:** Como lector quiero distinguir impacto, soporte, incertidumbre y resumen numérico.
**Prioridad:** Alta D1/D2. **Dependencias:** RV-01; ADR-005/008; SC/RP históricos.

Los resultados SHALL distinguir severity, confidence heurística, evidenceStrength, cobertura e inconclusos de scannerScore. D1/D2 SHALL conservar fórmula scoring-v1 cuando se cumplan sus cuatro inputs/invariantes, mostrando perfil de evaluación y plan; SHALL NOT compararse automáticamente scores de perfiles distintos ni interpretar confidence como probabilidad. Cambiar fórmula SHALL requerir nueva versión y aprobación. Los nuevos perfiles SHALL generar report-v2 con alcance, adapter/revisión, aplicabilidad y limitaciones reales, conservando score+informe+cierre atómicos. Report-v1 y scores históricos SHALL permanecer intactos; el nuevo reporte SHALL NOT afirmar restricciones sintéticas o exclusión de SSE que no correspondan al Scan.

#### Scenario: Cero con cuatro inconclusos
- **WHEN** un scan completo obtiene cuatro evaluaciones inconclusas y ningún Finding
- **THEN** el score cero se acompaña de esos contadores y limitaciones, sin presentar la aplicación como segura ni ocultar la falta de cobertura

### Requirement: RV-03 Dataset y medición honesta

**Historia:** Como responsable quiero medir errores de detección sin confundir abstención con acierto.
**Prioridad:** Alta para validación; sin objetivo numérico inicial. **Dependencias:** RV-01; NI-04.

La validación SHALL separar conectividad de calidad de detección. El dataset SHALL registrar autorización, aplicación/versión/protocolo, revisión del test/criterio/contexto, fecha y ground truth adjudicado independientemente del evaluador. SHALL distinguir TP/FP/TN/FN en predicciones concluyentes, INCONCLUSIVE y fallos técnicos aparte; truth ambiguo SHALL excluirse de la matriz binaria y contarse. SHALL publicar precision, recall/FPR/FNR sobre concluyentes con denominadores/soporte explícitos, cobertura, abstenciones por truth y recall efectivo sobre todos los positivos elegibles. Denominador cero SHALL ser N/A. Métricas por categoría SHALL incluir soporte; no SHALL declararse cifras de calidad/calibración antes de datos suficientes ni usar ejemplos de calibración como evaluación independiente.

#### Scenario: Abstenciones sobre positivos
- **WHEN** la mitad de positivos elegibles quedan inconclusos y el resto se detecta
- **THEN** se informa cobertura y recall efectivo además de recall concluyente, sin convertir las abstenciones en TN ni esconderlas del denominador efectivo

### Requirement: RV-04 Aceptación por protocolos y conservación histórica

**Historia:** Como usuario quiero comprobar utilidad fuera de la demo sin perder la evidencia TFM.
**Prioridad:** Alta D1/D2; retirada runtime posterior. **Dependencias:** TI-03; TS-01..03; RV-01..03.

La aceptación inicial completa SHALL requerir un endpoint Groq u OpenRouter compatible autorizado y un target SSE real autorizado, usando el mismo motor y sin código por proveedor en dominio. SHALL registrar configuración/versiones/permiso y diferenciar transporte logrado, criterios aplicables e inconclusos. Automatización SHALL usar fixtures mínimas sintéticas sin llamadas externas. La demo SHALL dejar de ser una dependencia conceptual o prerrequisito de ejecución; su eliminación/traslado y el tag/release `tfm-v1.0` SHALL requerir actuación posterior autorizada e identificación de commit histórico. Esta planificación SHALL NOT crear el tag, borrar demo ni afirmar ejecución de aceptación real.

#### Scenario: Diseño completado antes de disponer de targets
- **WHEN** los contratos y fixtures están preparados pero no hay permisos/dataset externos confirmados
- **THEN** el diseño puede quedar listo para revisión y la aceptación real permanece pendiente, sin métricas inventadas ni uso de la demo como sustituto de esa aceptación
