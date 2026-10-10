## Why

El ciclo de vida aprobado de Scan todavía termina mediante trabajo no-op. Este cambio define el primer recorrido real y controlado desde una prueba de seguridad hasta una petición al target y evidencia persistida, sin decidir si existe una vulnerabilidad.

## What Changes

- Incorporar un catálogo mínimo e inmutable de `SecurityTest`, persistido en `test_cases` y sembrado mediante V4: cuatro casos sintéticos, uno por cada categoría aprobada LLM01, LLM02, LLM08 y LLM10. Códigos estables, revisión inicial 1 y UUID para conservar las relaciones existentes; sin CRUD ni `enabled`.
- Crear `TestExecution` por prueba seleccionada, con estados PENDING/RUNNING/COMPLETED/FAILED, fechas, metadatos técnicos y evidencia minimizada. Fijar catálogo y configuración una sola vez al comenzar el trabajo RUNNING.
- Sustituir `NoOpScanWorkAdapter` por un caso de uso que implemente `ScanWorkPort`, ejecute secuencialmente POST/JSON, sustituya el placeholder como dato JSON y extraiga exactamente un string mediante el JSONPath restringido aprobado.
- Reutilizar AES-256-GCM/envelope/clave existentes, añadiendo descifrado interno solo en memoria antes del envío. Ningún secreto de autenticación ni material criptográfico forma parte de la evidencia.
- Exigir autorización externa de orígenes, DNS efectivo y conexión a direcciones validadas, bloqueo de redes especiales, TLS verificado y cero redirecciones; conservar exclusivamente la excepción local/demo ya aprobada.
- Aplicar límites técnicos iniciales pequeños y efectivos de tiempo, tamaño, número de pruebas y frecuencia/concurrencia, configurables externamente cuando corresponda dentro de los márgenes aprobados.
- Registrar y continuar ante fallos técnicos individuales; abortar ante fallos de seguridad/configuración compartida, persistencia o presupuesto global. COMPLETED exige que todas las pruebas planificadas terminen técnicamente COMPLETED; cualquier fallo produce FAILED sin borrar los éxitos obtenidos.
- Exponer únicamente GET `/api/projects/{projectId}/scans/{scanId}/executions`, con metadatos sin prompts/respuestas. Mantener los siete campos de Scan y derivar progreso del listado; sin contadores nuevos en Scan. El evaluador posterior podrá recuperar evidencia internamente mediante aplicación/repositorio, sin exponerla públicamente ahora.
- Ejecutar exactamente las cuatro pruebas iniciales sembradas y vigentes, sin selección configurable por usuario. No representan cobertura completa de las categorías OWASP; LLM10 es una observación/proxy del output del endpoint y no valida el consumidor downstream/sink.

## Non-goals

Evaluación determinista de vulnerabilidad, LLM evaluador, verdict, Finding, confidence, severity, scoring, informes, UI, selección configurable de pruebas desde la API, biblioteca de payloads, CRUD de catálogo, AWS, SQS, RAG, agentes, herramientas, streaming, reintentos y cancelación avanzada. No se implementa demo-vulnerable; las pruebas futuras utilizarán un servidor HTTP fixture controlado.

Esta entrega solo actualiza los artefactos OpenSpec existentes. No modifica código, SQL, POM, configuración operativa, ADRs ni documentación consolidada; no ejecuta tests, servidores o migraciones. La validación strict solicitada comprueba únicamente OpenSpec.

## Capabilities

### New Capabilities

- `security-test-execution`: catálogo inicial reproducible, selección interna, invocación segura del target, registro y consulta de ejecuciones técnicas, evidencia y resultado agregado del trabajo real de Scan.

### Modified Capabilities

Ninguna spec consolidada: `openspec/specs/` solo contiene `.gitkeep`. Los changes aprobados se conservan intactos y constituyen la referencia previa. STE-10 concreta la sustitución expresamente prevista por SE-10 y la política individual antes pendiente; no cambia admisión, estados, errores, siete campos, executor ni API de Scan. El alcance temporal no-op de SE-10 permanece como evidencia del incremento anterior, no como comportamiento del nuevo worker. No se sincronizan ni archivan changes en esta tarea.

## Impact

- Backend futuro: dominio de pruebas/ejecuciones, aplicación y puertos de catálogo/invocación/persistencia, adaptadores PostgreSQL/HTTP/crypto y composición del worker existente. Project y el contrato de gestión de TargetConfiguration permanecen intactos.
- V4 evoluciona `test_cases` y `test_executions`; conserva V1/V2/V3, FKs hacia scans y findings, columnas futuras de evaluación sin uso y `global_score` intacto. Falla sin alterar datos si encuentra históricos que no puede convertir de forma verificable.
- Dependencia autorizada por el usuario cuando sea necesaria para cumplir el transporte aprobado: Apache HttpClient 5, encapsulado detrás del puerto de invocación, sin tipos del cliente en dominio/aplicación. Usar versión gestionada por el BOM existente si está disponible; ninguna instalación o cambio de POM en esta entrega.
- La futura implementación actualizará únicamente OpenAPI, modelo de datos, contrato del target, arquitectura/ADR-003/decisiones abiertas, modelo de amenazas/política de autorización y casos-de-prueba directamente afectados, conforme al diseño aprobado. No cambia requisitos administrativos del TFM.
- Documentación y contratos revisados: los tres changes aprobados, docs/00-contexto, arquitectura y ADRs relevantes, riesgos OWASP/modelo de amenazas, casos/estrategia de evaluación, contrato REST, modelo de datos y V1–V3. El documento consolidado de autorización está vacío; este change registra la política ahora aprobada, cuya documentación consolidada se actualizará durante implementación autorizada.

## Review Status

**READY FOR IMPLEMENTATION.** El usuario ha aprobado D-01 a D-11 con las concreciones registradas en Approval Status de design.md, incluida la introducción de HttpClient 5 si resulta necesaria. No quedan decisiones bloqueantes de diseño para este incremento. Catálogo y transporte permanecen en un único change para entregar un recorrido vertical. La aprobación del diseño no autoriza implementar en esta entrega: se requiere una petición posterior explícita; todas las tareas siguen pendientes. No se inicia el siguiente change.
