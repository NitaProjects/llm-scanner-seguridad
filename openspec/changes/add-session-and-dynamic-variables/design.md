## Context

El puerto actual representa una interacción de prueba. Una sesión de aplicación requiere dos exchanges, aunque al scanner le baste una respuesta final.

## Goals / Non-Goals

Bootstrap JSON y envío JSON de prompt con un contexto por TestExecution. Las secuencias configurables pertenecen a workflows.

## Decisions

InteractionSession mantiene variables inmutables una vez capturadas. Una declaración fija nombre, fuente, tipo, cardinalidad, longitud máxima y obligatoriedad. Bootstrap usa GET o POST autorizado de ruta relativa. El segundo POST usa el template del target y slots declarados. La extracción JSON selecciona un único escalar por ruta restringida. Un header se captura por nombre permitido, sin duplicados. De Set-Cookie se captura una cookie nombrada con sus atributos, con parser acotado. Variables como chatId/sessionId/conversationId/threadId se usan como identificadores si la declaración y la fuente JSON permiten ese uso. Un identificador de sesión con función de credencial se declara sensible. Los valores de headers/cookies son siempre sensibles y se conservan como handles efímeros con origen y metadatos de cookie. Su reenvío queda en add-dynamic-headers-and-cookies.

Usar identificadores JSON solo como nodo JSON serializado o segmento de ruta validado y codificado una vez. Rechazar slash, backslash, delimitadores de URI, percent encoding y dot segments en slots de ruta. Las variables no modifican origen, método ni nombres de campos de transporte. Las credenciales sensibles no se materializan en templates, guardas o URLs. Presupuesto inicial propuesto de 16 variables y 4 KiB UTF-8 por valor, acotado por los límites de headers y respuesta vigentes. Se rechazan faltantes obligatorios, duplicados y exceso antes de la llamada dependiente.

Exactamente dos intercambios como máximo por prueba. Mismo origen fijo, autorización y DNS/IP/TLS en cada llamada y cero redirects. La cuota y un deadline común cubren ambos. Destruir contexto en cierre, error o cancelación. No compartir sesión entre pruebas ni reanudar tras reinicio. Usar TTL remoto acordado para recursos huérfanos. El resultado representa la llamada final real. El bootstrap aporta solo diagnóstico seguro de paso. Sin cambios SQL/API/UI y sin sesión en EvaluationContext. Configuración y controles según el [roadmap](../generalize-target-integration/roadmap.md).

## Risks / Trade-offs

Un identificador devuelto puede conferir acceso. La declaración sensible, el scope por origen y la redacción protegen su manejo. El plan separa captura de propagación. Cada llamada consume presupuesto y un error del bootstrap impide enviar el prompt.
