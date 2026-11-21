## Why

La redacción previa a persistencia usa matching literal y variantes JSON limitadas, mientras el veto semántico inspecciona escapes y capas adicionales después de guardar TestExecution. Esta divergencia P1 permite almacenar equivalentes reversibles de valores conocidos como secretos o local-only, pese a las garantías de privacidad existentes.

## What Changes

- Una definición compartida y acotada de equivalencia protegida para redacción durable, detección local-only y veto de exportación; cobertura de escapes JSON/Unicode, mezclas parciales y strings JSON anidados hasta cuatro capas.
- Protección antes de cada escritura sensible del pipeline, empezando antes de executions.finish, y comprobación de las representaciones serializadas que realmente se entregan a persistencia.
- Registro efímero de valores no persistibles conocidos, incluyendo credenciales, sesiones/cookies/CSRF, credenciales dinámicas y valores/aliases/IDs local-only RAG/tool; sin nuevo almacén durable de secretos.
- Preservación de redacción por intervalos originales fusionados; para equivalencia no literal se permite suprimir el campo completo en vez de arriesgar reconstrucción parcial.
- Fail closed explícito ante límites, ambigüedad o inspección incompleta, conservando metadata segura y los estados/errores existentes compatibles con PostgreSQL.
- Pruebas unitarias, diferenciales con exportación e integración PostgreSQL tras commit y reapertura, sobre request/response, error paths, copias secundarias y protocolos soportados.

Fuera de alcance: implementación en esta fase, frontend/API pública, migraciones, cambios de protocolo/evaluación/scoring/report-v1, detección universal de secretos desconocidos, decodificadores base64/URL/HTML/compresión/cifrados nuevos, limpieza automática de históricos/backups/WAL, dependencias, despliegue/AWS, commit/push.

## Capabilities

### New Capabilities

- `durable-secret-protection`: contrato transversal prospectivo de equivalencia protegida y escrituras durables seguras, complementando los contratos existentes de integración/evaluación sin relajarlos.

### Modified Capabilities

Ninguna. Se concreta la garantía transversal preexistente de exclusión de secretos/local-only; no se modifica el comportamiento funcional de los adapters ni las reglas híbridas. La divergencia actual con target-retrieval-tool-observations (exclusión de snapshot durable) se registra y corrige, sin reinterpretar el código como requisito.

## Impact

Application/execution y fronteras de persistencia; utilidades de privacidad compartidas; TargetJsonProcessor, InteractionSession, transporte/observaciones y projector semántico; escrituras de evaluación/Findings y guardas de snapshots. Mantener dominio independiente de Spring/Jackson/HTTP. Sin nuevas tablas/columnas/enums REST: usar flags, marcadores y motivos existentes; cualquier imposibilidad de cumplir checks actuales bloquea la implementación afectada, no autoriza migración implícita.

La protección afecta únicamente nuevos writes. Lectura histórica y formatos permanecen compatibles; no se afirma saneamiento retrospectivo ni anonimización de secretos desconocidos. El cifrado autorizado de credential_ciphertext no es una copia de evidencia y permanece intacto.
