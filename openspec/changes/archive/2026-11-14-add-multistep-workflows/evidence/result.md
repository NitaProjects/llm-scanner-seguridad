# Validación de add-multistep-workflows

Suite backend offline con Java 21 y `.\mvnw.cmd -o test`. Maven informó
742 tests, 648 aprobados, 0 fallos, 0 errores y 94 omitidos. Las fixtures
HTTP usaron loopback y el control de origen empleó resolución simulada.

| Grupo | Aprobados | Fallos / errores | Omitidos |
| --- | ---: | ---: | ---: |
| WorkflowTargetTest | 30 | 0 / 0 | 0 |
| LegacyRestTargetAdapterTest | 3 | 0 / 0 | 0 |
| HttpTargetInvocationAdapterTest | 20 | 0 / 0 | 0 |
| OpenAiCompatibleTargetAdapterTest | 30 | 0 / 0 | 0 |
| StreamingTargetTest | 18 | 0 / 0 | 0 |
| SessionTargetTest | 19 | 0 / 0 | 0 |
| HybridV2EvaluationTest (incluye control v1) | 24 | 0 / 0 | 0 |
| ScoringPolicyV1Test | 34 | 0 / 0 | 0 |
| GenerateReportUseCaseTest | 11 | 0 / 0 | 0 |
| TargetAdapterWiringTest | 1 | 0 / 0 | 0 |
| WorkflowPostgresIntegrationTest (suite offline) | 0 | 0 / 0 | 1 |
| WorkflowPostgresIntegrationTest (PostgreSQL dedicado, después de P2/P3) | 1 | 0 / 0 | 0 |

- JSON → JSON → SSE y JSON → JSON → JSONL reconstruyeron el texto terminal
  exacto tras redacción, con bytes UTF-8 fragmentados. JSON terminal también
  pasó. Los IDs de chat/sesión se reutilizaron dentro de cada prueba.
- Con dos orígenes autorizados, la autoridad relativa se rechazó en el plan.
  La comprobación del transporte rechazó URLs resueltas con distinto esquema,
  host o puerto, con cero resoluciones y cero materializaciones de Bearer.
- Se verificaron las cotas de seis pasos, deadline común, bytes acumulados de
  petición/respuesta y unidades de stream. Las unidades y bytes descartados
  consumen cuota. Los fallos, la omisión de un paso requerido y la cancelación
  cortan el flujo sin éxito parcial ni envío al siguiente paso.
- La regresión de latencia comprobó que las dos esperas de cuota quedan
  fuera de `latencyMs` y que un deadline de 1 800 ms impide el tercer
  intercambio. La regresión de captura verificó que `$[0].id` extrae
  un valor de un array raíz y permite reutilizarlo en la ruta dependiente.
- El diagnóstico de paso contiene únicamente metadatos técnicos. Las pruebas
  comprobaron que no contiene los valores sensibles ni las rutas resueltas.
  La evidencia final aplica redacción conjunta a la credencial estática y
  los valores sensibles capturados. El contexto se cierra al salir.
## Aceptación PostgreSQL posterior a P2 y P3

El operador confirmó la corrida posterior a P2/P3 de
`WorkflowPostgresIntegrationTest` en PostgreSQL dedicado. Maven registró
1 aprobado, 0 fallos, 0 errores y 0 omitidos, con `BUILD SUCCESS` y
20,24 s de duración para la clase.

La fixture confirmó que el workflow multi-step captura y
reutiliza las variables en las rutas dependientes y que el Scan queda
`COMPLETED` con cuatro TestExecutions, cuatro evaluaciones completadas,
`scoring-v1` y `report-v1`. Al reabrir la aplicación sin el binding, comprueba
la lectura histórica y que la evidencia persistida contiene el intercambio
terminal saneado, sin los valores efímeros de la fixture. `InteractionSession`
se mantiene en memoria durante la interacción.

La revalidación persistida acredita la tarea 2.2. El checklist registra 6/6.
OpenSpec strict pasó.
