## Purpose

Comprobar observaciones estructuradas de retrieval y herramientas sobre fixtures autorizadas y separar la respuesta final.

## ADDED Requirements

### Requirement: OBS-01 Observaciones correlacionadas

El perfil de eventos SHALL distinguir texto, evidencia retrieval, petición de tool, resultado y respuesta final. SHALL correlacionar resultados con peticiones anteriores dentro de la interacción. Una traza ausente, inválida o incompleta SHALL quedar inconclusa en las comprobaciones que dependen de ella. Solo la respuesta final válida SHALL pasar al motor de evaluación actual.

#### Scenario: Petición sin resultado
- **WHEN** el target anuncia una petición de herramienta y entrega respuesta final sin el resultado requerido por la fixture
- **THEN** se distingue la petición observada de una ejecución acreditada y la comprobación de traza queda inconclusa

### Requirement: OBS-02 Comprobaciones defensivas locales

Con fixtures de propietario autorizado y acciones inertes, SHALL comprobar aliases sintéticos de retrieval y herramientas según el contrato de prueba. SHALL emitir diagnósticos técnicos acotados sin modificar scoring, Findings o report-v1. SHALL mantener límites de stream/origen y excluir documentos, argumentos, resultados y secretos de logs, evidencia externa y evaluación. SHALL NOT ejecutar herramientas, seguir URLs, descargar documentos ni abrir exchanges adicionales por instrucciones del target.

#### Scenario: Solicitud no permitida
- **WHEN** la fixture emite una solicitud de tool no incluida en la lista y un evento que pretende ordenar un callback
- **THEN** se registra diagnóstico local por la solicitud y el scanner realiza cero despachos ni conexiones nuevas
