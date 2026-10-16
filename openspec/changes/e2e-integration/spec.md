# e2e-integration — índice de especificación

Estado: **READY FOR RE-REVIEW**. Este archivo satisface la entrada `spec.md` solicitada en la raíz del change. La especificación normativa única se encuentra en [specs/e2e-integration/spec.md](specs/e2e-integration/spec.md), conforme al esquema OpenSpec `spec-driven`; no se duplica su contenido para evitar divergencias.

Una capacidad nueva, `e2e-integration`, sin modificaciones a contratos aprobados del producto. [proposal.md](proposal.md) define alcance; [design.md](design.md) resuelve D-01..D-24 para revisión; [tasks.md](tasks.md) ordena la implementación futura.

| Requisito | Decisión | Contrato del recorrido |
| --- | --- | --- |
| E2E-01 | D-01 | OFFLINE obligatorio, autorización, límites MVP |
| E2E-02 | D-02 | Angular 4200, scanner 8080, demo 8081, PostgreSQL 5432 |
| E2E-03 | D-03 | BD manual llm_scanner_seguridad; tests llm_scanner_seguridad_test |
| E2E-04 | D-04 | Arranque manual y preflight explícito, sin operación ahora |
| E2E-05 | D-05 | E2E Demo Vulnerable, ids reales |
| E2E-06 | D-06 | Target exacto aprobado y SSRF local existente |
| E2E-07 | D-07 | Manifest/binding ligados a id/updatedAt reales |
| E2E-08 | D-08 | Demo VULNERABLE estable, SAFE opcional posterior |
| E2E-09 | D-09 | UI → Scan real → cinco endpoints finales → UI |
| E2E-10 | D-10 | Deadline operacional 360 s, HTTP 10 s, ciclos 3 s sin solapar |
| E2E-11 | D-11 | Cuatro I válidos offline, sin forzar VULNERABLE |
| E2E-12 | D-12 | Evidencia funcional API/UI; SQL diagnóstico opcional, internos NOT_INSPECTED si no observados |
| E2E-13 | D-13 | Score/NO_FINDINGS y cuatro I, sin recalcular fuente de verdad |
| E2E-14 | D-14 | Report-v1/es, summary y nueve limitaciones coherentes |
| E2E-15 | D-15 | QA frontend real, sin nuevo polish |
| E2E-16 | D-16 | 9.4 permanece pendiente sin apertura de Finding real |
| E2E-17 | D-17 | Matriz TP/TN/FP/FN/INCONCLUSIVE externa |
| E2E-18 | D-18 | F-01: probe / referencias aprobadas / oráculo separado |
| E2E-19 | D-19 | Procedimiento manual completo; script Prepare/Collect opcional sin proveedor ni procesos |
| E2E-20 | D-20 | Regresión solo por defecto nuevo; BD test aislada |
| E2E-21 | D-21 | Evidencia mínima API/UI; SQL/script no bloquean aceptación |
| E2E-22 | D-22 | Defectos: reproducir, localizar, corregir mínimo autorizado, regresión, reejecutar |
| E2E-23 | D-23 | Historial conservado; reutilización explícita, sin borrados |
| E2E-24 | D-24 | Evaluador real futuro documentado, fuera del primer E2E |

Todas las decisiones están concretadas como diseño propuesto; falta su revisión/aprobación antes de READY FOR IMPLEMENTATION. Este change no acredita E2E ejecutado, proveedor real, cierre 9.4 ni entrega TFM.
