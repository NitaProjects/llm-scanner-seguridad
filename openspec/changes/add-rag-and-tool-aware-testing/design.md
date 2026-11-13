## Context

El collector recibe eventos declarados por el target. Los cuatro tests y report-v1 mantienen su alcance y sus criterios sintéticos. La observación estructurada depende de lo que el propietario instrumente.

## Goals / Non-Goals

Comprobar trazas declaradas de retrieval/tools en un entorno controlado sin ejecutar acciones desde el scanner. Depende de streaming para este primer formato. Puede usar workflows y bindings cuando el acceso del target los requiera.

## Decisions

Extender el mapping de InteractionEvent con MODEL_TEXT, RETRIEVAL_EVIDENCE, TOOL_REQUEST, TOOL_RESULT y FINAL_ANSWER. Declarar selectores restringidos y aliases sintéticos permitidos de documento y herramienta. Correlacionar con ids opacos de llamada dentro de una interacción y con orden local. Una petición observada no prueba ejecución. Un resultado observado se vincula a una petición previa y se registra como dato declarado por el target, sin afirmar procedencia independiente. Duplicados, ids no correlacionados, faltantes y orden inválido quedan como traza inconclusa. Las trazas carecen de autoridad sobre configuración, destinos o comandos.

Dos comprobaciones locales acotadas de fixture: observar solo aliases de retrieval permitidos y comprobar si el target solicita una herramienta fuera de la lista declarada. El operador configura fixture/stubs, acciones inertes y egress del target. El scanner envía los cuatro prompts actuales a un entorno autorizado con acciones acotadas. Jamás implementa callback de tool, descarga documentos, sigue URLs, procesa código o reenvía argumentos. Si el flujo pide al scanner ejecutar una herramienta, se registra diagnóstico y se aplica la política técnica de respuesta incompleta si falta el final.

Las comprobaciones producen diagnósticos técnicos locales, fuera del scoring-v1, Findings y report-v1. Texto del modelo y observaciones se almacenan separados en memoria. Solo FINAL_ANSWER conforme al contrato del stream pasa como responseText. La evidencia técnica registra clase, ordinal, resultado de la comprobación y códigos permitidos. Omite contenido de documentos, argumentos, resultados de herramientas e identificadores remotos. Los límites de eventos/bytes/deadlines existentes también se aplican a estas observaciones. Cero exchanges nuevos, mismo aislamiento de origen, SSRF/DNS/TLS, redirects desactivados y permisos de evaluación vigentes. Sin cambios DB/API/UI. El operador recibe diagnósticos locales correlacionados con la ejecución. Compatibilidad y configuración según el [roadmap](../generalize-target-integration/roadmap.md).

## Risks / Trade-offs

El target puede omitir, falsificar o reordenar eventos. Los diagnósticos se limitan a lo observado y una traza incompleta permanece inconclusa. El target puede ejecutar acciones propias aunque el scanner no las despache. La prueba requiere un entorno controlado y stubs verificados por su propietario.
