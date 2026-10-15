## Why

El scanner y su frontend necesitan un target local autorizado y reproducible para observar las cuatro categorías MVP sin utilizar aplicaciones ajenas, datos reales ni un LLM objetivo de pago. La tarea 9.4 de `frontend-mvp` sigue pendiente de entorno, endpoints y manifiesto; este change prepara ese entorno, sin acreditar todavía la integración ni el experimento académico.

## What Changes

- Incorporar en una implementación posterior `demo-vulnerable/`, benchmark sintético independiente: Java 21, Spring Boot 4.1.1, Maven Wrapper propio, sin dependencia del scanner ni base de datos.
- Definir un único `POST /api/chat`, JSON `{ "message": "..." }` → `{ "response": "..." }`, con respuestas deterministas a los cuatro probes revisión 1 existentes.
- Fijar al arrancar `DEMO_PROFILE=SAFE|VULNERABLE`, SAFE por defecto, sin mutación remota. Cada categoría tendrá un caso vulnerable y su control seguro, con ground truth versionado y señales observables compatibles con las reglas actuales.
- Mantener válidos HTTP/JSON y la extracción textual incluso en LLM10: su incumplimiento se produce en el contrato del texto extraído, con cobertura `OUTPUT_ONLY` y sin sink real.
- Diseñar fixtures/ground truth JSON, manifiesto humano y legible por máquina de autorización/configuración, y plantilla separada del manifiesto que consume `EVALUATION_SCENARIO_FILE`. El evaluador puede recibir referencias sintéticas y criterios técnicos confiables; el oráculo, sus etiquetas/justificación y el resultado esperado del scanner permanecen externos. No se promete evaluación completamente ciega a los valores de referencia.
- Reutilizar exclusivamente la excepción SSRF existente: perfil scanner `local`, flag y origen demo exacto. Definir límites efectivos, logs mínimos, arranque local, Docker opcional y tests futuros.
- Separar el recorrido E2E sin proveedor, esperado `INCONCLUSIVE`, de una ejecución posterior autorizada del evaluador para demostrar veredictos y TP/TN. El usuario confirmó esta separación el 2026-10-13; no se habilita exportación ni se llama a proveedores en este change.

### Non-goals

No implementar ahora; no modificar backend, frontend, catálogo de probes, reglas, scoring, reporting, dependencias existentes ni despliegue compartido. No RAG, agentes, herramientas, streaming, UI propia, multiusuario, autenticación avanzada, BD, datos/secretos reales, comandos, acceso arbitrario a archivos, mutaciones de filesystem, red saliente del target, SSRF real o llamadas LLM del target. LLM real/híbrido y publicación/AWS quedan como evolución futura. No ejecutar E2E completo, cerrar 9.4, fijar el dataset académico definitivo ni declarar el proyecto entregable.

## Capabilities

### New Capabilities

- `demo-vulnerable`: target sintético local, contrato HTTP acotado, perfiles inmutables, cuatro escenarios y controles, ground truth/manifiestos, aislamiento y preparación de validación reproducible.

### Modified Capabilities

Ninguna. Se consumen los contratos aprobados de `target-configuration`, `security-test-execution`, `evaluation-engine`, `scoring-engine`, `reporting`, `frontend-mvp` y `frontend-polish` sin alterarlos.

## Impact

- Esta entrega solo crea `.openspec.yaml`, `proposal.md`, `specs/demo-vulnerable/spec.md`, `design.md` y `tasks.md` dentro de `openspec/changes/demo-vulnerable/`.
- La implementación propuesta quedará contenida en `demo-vulnerable/`, incluido su README, build, fixtures, manifiestos, tests y Dockerfile; no será módulo Maven ni paquete del dominio del scanner. Las dependencias nuevas mínimas de esa app se someterán a aprobación del diseño, sin instalarse ahora.
- Contratos de referencia: ADR-001/006/007, `docs/05-api/openapi.yaml`, `docs/05-api/target-contract.md`, política de autorización, catálogo inmutable V4 y manifiesto D-02 de `evaluation-engine`.
- Operación local prevista: demo `127.0.0.1:8081`, scanner host `8080`, Angular `4200`, PostgreSQL existente `5432`. La demo no accede a ninguno de esos servicios.
- Ground truth esperado describe el comportamiento del benchmark, no garantiza el veredicto híbrido: sin permiso de exportación, el scanner actual conserva `INCONCLUSIVE`, incluso ante señal fuerte; SAFE solo consolida `NOT_VULNERABLE` con LLM válido concordante.
- Corrección F-01 (2026-10-15): distinguir probe del target, contexto confiable del evaluador y oráculo externo. TP/TN/FP/FN se calculan posteriormente comparando ground truth independiente con resultado observado; INCONCLUSIVE se registra aparte. Aceptar referencias conocidas en la evaluación híbrida limita las conclusiones sobre generalización, sin invalidar el benchmark MVP ni cambiar el evaluation-engine.
- Estado: **READY FOR REVIEW** al completar artefactos y validación estricta. La revisión/aprobación de decisiones y una instrucción posterior de implementación siguen siendo necesarias.
