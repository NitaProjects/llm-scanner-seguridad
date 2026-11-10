## 1. Revisión y límites de implementación

- [ ] 1.1 Obtener aprobación explícita de las decisiones propuestas y del corte a implementar (D0 recomendado primero); verificar que quede registrado el alcance autorizado, sin asumir que READY FOR REVIEW equivale a aprobación.
- [ ] 1.2 Registrar únicamente decisiones aprobadas en ADR prospectiva vinculada a este change, manteniendo históricos y sección Fuera del MVP / Evolución futura; verificar referencias a ADR-001/002/005/008 sin reescritura retrospectiva.

## 2. D0 / M1a — Primera unidad pequeña: frontera neutral con legacy

- [ ] 2.1 Definir InteractionRequest/NormalizedInteraction y resultado técnico neutral en domain/application, sin clases HTTP/Spring/proveedor (NI-01/02, D-02/03); verificar casos completos/fallidos y campos opcionales mediante fixtures locales.
- [ ] 2.2 Introducir TargetInteractionPort y puente del adapter REST legacy sobre TargetInvocationPort, sin cambiar persistencia, API, UI, prompts ni políticas v1 (TI-01, NI-01); verificar que mismo prompt/JSON/responsePath conserva resultado y fallos originales.
- [ ] 2.3 Mantener limpieza de recursos, presupuesto y controles de red/cifrado existentes al atravesar el puente (IS-01..03); verificar con fake de transporte que denegación no descifra ni envía y no hay retries nuevos.
- [ ] 2.4 Añadir prueba de contrato para la frontera neutral y regresión v1 de ejecución/evaluación/score/report (NI-01/02, RV-02); verificar equivalencia sin red externa y sin migración necesaria. Ejecutar comprobaciones solo por petición explícita del usuario.

## 3. D1a / M1 — Revisiones, configuración REST y seguridad común

- [ ] 3.1 Diseñar e implementar después de autorización nuevas migraciones de revisiones, puntero activo y snapshots de perfil, sin modificar migraciones ejecutadas (TI-01, D-10); verificar checks condicionales legacy/v2 en columnas originalmente NOT NULL, mapper discriminado sin TargetSettings ficticio, defaults legacy y datos sintéticos históricos, sin ejecutar migraciones sin petición explícita.
- [ ] 3.2 Persistir revisión pública inmutable y expectedRevision atómicos con identidad/cardinalidad existentes (TI-01); verificar conflicto de escritura concurrente y estabilidad del snapshot durante Scan.
- [ ] 3.3 Extender render/extracción con JSONPath restringido de entrada/salida, nodo único y errores cerrados (TI-02); verificar arrays, propiedades con corchetes, índice final, Unicode/comillas y rechazo de scripts/tipos ambiguos.
- [ ] 3.4 Extraer solo la parte compartida de exchange seguro para reutilizar DNS/IP fijada, TLS, allowlist y cero redirects, sin relajar excepciones (IS-01/02); verificar DNS mixto, rebinding simulado, metadata, IPv6, Location y cambio de origen con fixtures.
- [ ] 3.5 Definir límites compuestos por interacción/Scan y contabilidad por intento sin red en transacción (IS-03); verificar deadlines monotónicos y agotamiento antes de un envío, con reloj/fakes controlados.
- [ ] 3.6 Añadir API v2 tipada de configuración/lectura, conversión legacy explícita y conflicto v1 seguro sobre targets v2 (TI-01); verificar DTOs/decoders y errores sin pérdida de campos ni secretos; mantener nuevas admisiones v2 desactivadas hasta sección 5.

## 4. D1b / M2 — OpenAI-compatible sin streaming

- [ ] 4.1 Implementar adapter chat con endpoint completo/modelo/system opcional/user/stream=false, una elección y auth protegida (TI-03); verificar request y extracción de choices[0].message.content contra fixture, sin nombres comerciales en dominio.
- [ ] 4.2 Clasificar final por límite, tool calls, null, contenido multimodal, auth/429 y respuesta inválida (TI-03, NI-02); verificar que no hay ejecución de herramientas, éxito por HTTP 200 ni reenvío ambiguo.
- [ ] 4.3 Añadir selector y formularios REST/chat con preview local y credenciales transitorias en Angular (TI-04); verificar guardado sin tráfico remoto, campos aplicables, cambio de origen y limpieza del secreto.

## 5. D1c — Evaluación aplicable, evidencia, reporte y activación

- [ ] 5.1 Añadir AssessmentContext tipado/versionado y panel de criterios por categoría ligado a target/test revision, con consentimiento como referencia a política externa (RV-01, IS-04, D-08/11); verificar que contexto del target, ground truth y permisos de exportación no se autoconceden desde la respuesta o formulario.
- [ ] 5.2 Implementar perfil authorized-app-v2 en preparación/proyección, reutilizando reglas y consolidación con criterios aplicables (RV-01); verificar contexto ausente→INCONCLUSIVE, citas inventadas rechazadas, LLM solo sin Finding y OUTPUT_ONLY preservado.
- [ ] 5.3 Implementar política de exportación v2 por revisión/proveedor/finalidad y límites, sin cambiar corpus exacto v1 (IS-04); verificar target autorizado sin exportación no envía datos y permiso obsoleto se rechaza.
- [ ] 5.4 Separar referencias de evidencia de transporte/normalizada/evaluadora/seguridad y sus transformaciones (NI-03); verificar citas/offsets sobre proyección realmente usada, redacción relevante→INCONCLUSIVE y ausencia de duplicados plaintext en columnas legacy; comprobar disponibilidad EXPIRED tras caducidad sin regenerar informes.
- [ ] 5.5 Implementar solo si se habilita captura el envelope cifrado de evidencia con clave externa separada, AAD scoped, TTL/purga y lectura controlada; verificar rechazo sin clave/política, expiración y ausencia de plaintext en BD/logs/errores (IS-04, D-04/09). Captura queda deshabilitada hasta acordar backups; no habilitar datos sensibles.
- [ ] 5.6 Adaptar entradas estructuradas para fórmula scoring-v1 y añadir fuerza de evidencia/cobertura/perfil sin alterar el cálculo ni históricos (RV-02); verificar ejemplos de score v1 intactos y perfiles distintos sin comparación automática.
- [ ] 5.7 Introducir report-v2 y lector/API/frontend por perfil con limitaciones correctas, manteniendo report-v1 inmutable y cierre conjunto de cuatro ejecuciones/evaluaciones+score+informe (NI-02, RV-02); verificar rollback terminal coherente, idempotencia y lectura histórica sin backfill.
- [ ] 5.8 Añadir correlación y diagnósticos estructurados de adapter y evaluador con labels acotados (NI-04); verificar con fixtures que errores reflejados no filtran cuerpos, tokens, sessionId o URLs libres.
- [ ] 5.9 Actualizar únicamente OpenAPI/target-contract, arquitectura, modelo de datos y política/evaluación afectados por el código efectivamente entregado; verificar correspondencia de perfiles, campos y límites, sin cambiar checklist TFM por una capacidad v2 (TI-01, RV-01/02).
- [ ] 5.10 Con autorización específica de red, seleccionar Groq u OpenRouter y caso con datos sintéticos/no sensibles, fijar contexto/política/límites y ejecutar D1; verificar evidencia de transporte y aplicabilidad separadas, sin métricas de calidad inventadas (RV-04). Gate: no declarar D1 aceptado solo por tests locales.

## 6. M1b — Retries controlados, independientes de D1

- [ ] 6.1 Añadir configuración restringida de retry con cero por defecto, máximo dos e idempotencia remota declarada para POST (IS-03, D-05); verificar timeout ambiguo sin reenvío, auth/parsing sin retry y backoff/Retry-After dentro del presupuesto global.

## 7. D2 / M3 — Streaming SSE y JSONL

- [ ] 7.1 Implementar decoders incrementales SSE/JSONL y eventos normalizados (TS-01); verificar buffers fragmentados, UTF-8, CRLF/multiline, línea acotada y secuencia local sin depender de chunks de red.
- [ ] 7.2 Añadir colector delta/snapshot y condiciones terminal/error configurables (TS-01/02); verificar final válido, prefijo cortado, evento error posterior, EOF no autorizado y ausencia de reconexión automática.
- [ ] 7.3 Aplicar topes de tiempo absoluto/inactividad/bytes/frame/eventos/texto y buffer, compartidos con Scan (TS-03, IS-03); verificar flood/heartbeats y liberación de conexión/worker al primer límite.
- [ ] 7.4 Completar persistencia/proyecciones de interacción neutral para perfil v2 sin httpStatus obligatorio y sin relajar v1 (NI-02, D-10); verificar COMPLETE frente a INCOMPLETE/FAILED y compensación Scan/evaluación.
- [ ] 7.5 Añadir formulario Streaming, mapping, terminal y diagnóstico seguro de prefijo incompleto (TI-04, NI-04); verificar que no se configura tráfico al guardar ni se muestra un parcial como resultado final.
- [ ] 7.6 Probar equivalencia del mismo contenido/criterio/contexto por REST/chat/SSE y actualizar contratos afectados (NI-01, RV-01); verificar misma entrada al motor/veredicto con transportes simulados.
- [ ] 7.7 Con autorización específica, registrar y ejecutar un target SSE real con datos no sensibles y contexto permitido, junto con el endpoint chat de D1 (RV-04); verificar aceptación inicial completa con motor común sin lógica por proveedor. Registrar límites y resultados, sin inferir precision/recall de dos endpoints.

## 8. M4 — Workflows de sesión

- [ ] 8.1 Implementar AST/JSON schema versionado, validación de orden/variables/tipos/guards y compilación sin scripts ni red (TW-01); verificar workflow ejemplo y rechazo de ciclos, referencias futuras, campos desconocidos y métodos no admitidos.
- [ ] 8.2 Añadir intérprete secuencial HTTP/poll/stream con variables tipadas efímeras por ejecución (TW-01/02); verificar sesión independiente por categoría y selector final único sin ColoqIA-specific logic.
- [ ] 8.3 Restringir interpolación a nodos JSON/segmentos/query y bindings de credencial por origen, con revisiones/migraciones aditivas específicas (TW-02, IS-01/02); verificar traversal, URLs retornadas, cambio de host y exfiltración de cabeceras bloqueados.
- [ ] 8.4 Añadir polling con condición de éxito/fallo, límites y cleanup explícito best-effort (TW-03); verificar agotamiento, reserva de presupuesto, warning de cleanup y ausencia de replay tras reinicio.
- [ ] 8.5 Construir editor visual por tarjetas, extracción, selector final y presupuesto total (TI-04, TW-01); verificar configuración de sesión→envío→stream/poll sin JSON crudo obligatorio ni secretos recuperables.
- [ ] 8.6 Ampliar diagnóstico por paso y suite de amenazas/compatibilidad de workflow; verificar que cada request/poll/cleanup aplica autorización DNS/destino y no mantiene transacción de BD durante red (NI-04, IS-01..04).

## 9. M5 — Extensibilidad y validación de producto

- [ ] 9.1 Estabilizar descriptor, registro estático y suite de conformidad de adapters probados, sin cargador de código externo ni WebSockets/tools (NI-01/02, D-01/02); verificar que un adapter fake nuevo no exige cambiar el motor de pruebas/evaluación.
- [ ] 9.2 Preparar corpus autorizado y etiquetado independiente, separando split de calibración y evaluación y truth ambiguo (RV-03); verificar inventario con permisos/versiones y ausencia de ground truth en input evaluador.
- [ ] 9.3 Calcular matriz/precision/recall/FPR/FNR concluyentes, cobertura, abstenciones por truth y recall efectivo, por categoría con soporte/N/A (RV-03); verificar manualmente dataset pequeño adjudicado y no publicar generalizaciones sin soporte suficiente.
- [ ] 9.4 Documentar resultado de aceptación y limitaciones efectivamente observadas, manteniendo demo y evidencia TFM (RV-04); verificar que el scanner funciona sin demo como runtime requerido. El tag/traslado/retiro efectivo queda para autorización y change posterior, no se ejecuta en estas tareas.
