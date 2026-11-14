# Validación de add-dynamic-headers-and-cookies

## Suite backend offline

`.\mvnw.cmd -o test` con Java 21. Maven registró **775 pruebas, 681
aprobadas, 0 fallos, 0 errores y 94 omitidas**. Las omitidas se contabilizan
aparte. Las 33 pruebas de `WorkflowCredentialsTest` pasaron sin omisiones.
También pasaron las regresiones de workflow, streaming, sesión, REST_LEGACY,
OPENAI_COMPATIBLE, hybrid-v1/v2, scoring-v1 y report-v1.

## Credenciales por paso

- Las fixtures HTTPS locales comprobaron la captura de cookies y CSRF, el uso
  de bindings explícitos y la reconstrucción terminal SSE y JSONL. Los
  consumidores enviaron el ID dinámico, la cookie aplicable y el header CSRF.
  Un valor capturado sin binding quedó fuera de la petición.
- Se verificaron path por segmento, path por defecto, Secure, expiración,
  precedencia de Max-Age, eliminación, múltiples cookies, rutas distintas
  para el mismo nombre y rechazo de cookies ambiguas, malformadas o con Domain.
- El workflow rechazó destinos cross-origin incluso con dos orígenes
  autorizados. El transporte registró cero consultas DNS, cero resoluciones
  de handles y cero descifrados de Bearer para el destino rechazado.
  Una IP bloqueada o un certificado no confiable impidieron resolver los
  handles. El cliente tampoco siguió el redirect same-origin.
- Se comprobaron los rechazos de cabeceras reservadas, colisiones estáticas,
  ausencia, exceso de tamaño y aborto sin enviar el consumidor inválido.
- La revisión de CRED-02 exige que todas las fuentes de
  `credentialBindings` declaren `sensitive: true`. Las regresiones cubren
  una captura JSON no sensible usada como credencial, con y sin reutilización
  en un slot, y una captura sensible usada a la vez como credencial y como
  slot de URL o de body. Los cinco casos se rechazan antes de DNS y descifrado.
  Las fixtures HTTPS mantienen el caso válido de una captura JSON sensible
  enviada exclusivamente mediante su cabecera declarada.
- El texto terminal y la evidencia de petición se redactaron con valores
  solapados. Se comprobó la ausencia de datos de la fixture en los logs
  técnicos y el aislamiento del contexto entre ejecuciones, incluso tras
  cancelar una interacción con credenciales ya capturadas.

## PostgreSQL dedicado

El operador ejecutó `WorkflowPostgresIntegrationTest`. Resultado
**1 aprobado, 0 fallos, 0 errores, 0 omitidos**, con `BUILD SUCCESS` y
19,98 s para la clase. El workflow cerró el Scan con cuatro TestExecutions,
cuatro evaluaciones, scoring-v1 y report-v1. Las consultas comprobaron
la ausencia de la cookie y del token CSRF en ejecuciones, evaluaciones,
hallazgos, score e informe. La prueba también verificó lectura histórica
tras reabrir sin binding. Esta corrida precede al ajuste que elimina el body
de evidencia en errores de pasos con bindings dinámicos, cubierto por la
validación offline del transporte, y a la corrección CRED-02 de la revisión.
La corrección se verifica antes de red. La fixture PostgreSQL ya declara
sensibles todas sus fuentes de credenciales.

## OpenSpec

`openspec validate add-dynamic-headers-and-cookies --strict` pasó.
Las tres tareas están completas.
