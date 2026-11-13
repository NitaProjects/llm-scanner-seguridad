**6/6 verificadas. READY FOR FINAL REVIEW.** Implementación y regresiones offline
en PASS. La ejecución manual con PostgreSQL confirma el cierre de la tarea 2.2.
[Evidencia](evidence/result.md).

## 1. Implementación tras aprobación del change

- [x] 1.1 Resolver y fijar el binding operativo del target antes de red. Verificar que perfiles ausentes conservan REST/chat y que configuraciones inválidas no envían peticiones (SSE-01).
- [x] 1.2 Integrar colectores SSE/JSONL y el cierre terminal en el intercambio seguro. Verificar UTF-8 partido, CRLF, multiline data, comentarios, snapshots y deltas (SSE-01).
- [x] 1.3 Fijar el Accept efectivo por binding y rechazar colisiones con credenciales y headers sensibles o reservados antes de red. Verificar una única cabecera para SSE y ambos media types JSONL, y regresión de REST/chat sin binding (SSE-03).

## 2. Aceptación

- [x] 2.1 Verificar EOF, error, flood, timeout, cancelación y redacción de credencial dividida. Confirmar cero evaluación de parciales y cierre de recursos (SSE-02).
- [x] 2.2 Verificar proyección y cierre con un stream válido y fixtures REST/chat. Actualizar solo el contrato técnico de integración afectado (SSE-01/02).
- [x] 2.3 Verificar con un target que negocia por Accept que el binding SSE recibe el stream y reconstruye el texto aun con Accept JSON ordinario configurado. Verificar rechazo de credencial o header reservado Accept con cero DNS, descifrados y requests (SSE-03).
