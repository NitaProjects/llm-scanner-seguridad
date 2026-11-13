## Context

El collector SSE/JSONL y las variables efímeras proporcionan las primitivas. La fixture de ColoqIA necesita componer más de dos exchanges.

## Goals / Non-Goals

Definir un plan JSON versionado y secuencial para un único origen y una respuesta final. Dependencias de implementación en SSE y sesiones.

## Decisions

WorkflowDefinition v1 contiene identificador/revisión, binding del target, lista de pasos y paso terminal. Cada paso tiene id, método GET/POST, ruta relativa validada, template JSON para POST, dependencia explícita de pasos anteriores y guarda opcional exists/equals sobre valores no sensibles. Validar esquema cerrado y dependencias antes de red, sin referencias futuras ni ciclos. Los pasos intermedios usan request/response HTTP JSON, con body JSON cuando procede y extracción tipada desde un documento de respuesta completo validado. Si se omite un paso, un consumidor obligatorio de su resultado falla antes de enviar. El último paso es un POST JSON obligatorio e incondicional que inserta el prompt una vez. Su respuesta JSON se procesa por selector y sus respuestas SSE/JSONL por el colector del change 1. Solo el terminal produce la NormalizedInteraction final. Esta restricción preserva el body JSON real exigido para TestExecution COMPLETED. La extracción de variables desde SSE/JSONL intermedios queda para una evolución futura. Un plan que declare streaming intermedio se rechaza antes de red. Usar JSON detrás de un futuro editor. YAML añadiría sintaxis y parser y se difiere.

Fijar el origen base del binding al preparar el plan. Rechazar referencias con esquema o autoridad, incluidas las que empiezan por `//`, en la validación del plan. Para cada paso, después de sustituir y codificar variables y resolver la URI final, comparar su esquema, host normalizado y puerto efectivo con el origen base. Aplicar la comprobación antes de cualquier red del paso y antes de materializar Authorization, cookies, secretos o headers sensibles. Un cambio de origen aborta el workflow con diagnóstico seguro de paso, sin DNS, requests ni credenciales para el destino rechazado. La allowlist sigue siendo una comprobación adicional y no puede autorizar un cambio de origen.

WorkflowExecution ejecuta en el mismo worker un máximo inicial propuesto de seis pasos/exchanges por prueba, sin bucles o retries. El menor de timeout del target y ScanBudget restante acota toda la interacción. Cuotas, bytes por exchange y acumulados acotan la multiplicación de trabajo. Proponer como cota acumulada de respuesta por interacción el responseBytes efectivo, incluidos frames descartados. Cada paso revalida destino y aplica las mismas garantías SSRF/DNS/TLS. Fallo de paso requerido, dependencia ausente, cancelación o deadline abortan el flujo. No reejecutar acciones tras error de commit o reinicio. Usar endpoints de prueba de efecto conocido y TTL remoto acordado.

Evidencia técnica por paso limitada a id/ordinal, estado, status, duración, contadores y código seguro. Mantener lista acotada en memoria y emitir diagnósticos seguros correlacionados con TestExecution según el wiring del worker. Persistir únicamente body saneado y texto final por contrato actual. Los bytes de evidencia no crecen con el número de cuerpos recibidos. Sin JSON de configuración en BD, cambios API ni editor UI. Los lectores y report-v1 siguen el [roadmap](../generalize-target-integration/roadmap.md).

Fixture de referencia: POST de chat → JSON completo → chatId; POST de sesión con chatId → JSON completo → sessionId; POST /answer con sessionId y prompt → SSE o JSONL terminal → texto reconstruido. Un único origen, Bearer estático, rutas de bootstrap ilustrativas y una respuesta final. El operador puede describir esta forma sin reglas que dependan de ColoqIA.

Fixture de aislamiento con base `https://target-a.example` y paso `//target-b.example/path`, ambos orígenes en `TARGET_ALLOWED_ORIGINS`. Debe bloquear el workflow antes de red, con cero requests a target-b y cero materializaciones o envíos de Bearer para ese paso. La evidencia se limita al identificador local de paso y al motivo de rechazo, sin URI, headers ni secretos. Comprobar también la defensa sobre la URI resuelta con diferencias de esquema, host o puerto efectivo.

## Risks / Trade-offs

La secuencia puede amplificar coste y dejar recursos remotos. La validación del plan, el límite de pasos, los deadlines y el TTL remoto delimitan la operación. Las guardas nunca interpretan texto como código ni otorgan permisos. Un fallo se informa sin persistir datos de sesión.
