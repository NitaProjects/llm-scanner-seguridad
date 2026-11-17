# Documentación de LLM Scanner Seguridad

## Documentación vigente

Para instalar el proyecto y ejecutar el primer análisis, empieza por el [README principal](../Readme.md). Para reproducir casos controlados sin proveedor externo, consulta la [demo local](../demo-vulnerable/README.md).

| Tema | Documentación |
| --- | --- |
| Producto | [Visión](00-contexto/vision.md), [alcance](00-contexto/alcance.md) y [evolución futura](00-contexto/evolucion-futura.md). |
| Diseño | [Arquitectura general](02-arquitectura/arquitectura-general.md) y [decisiones de arquitectura](02-arquitectura/decisiones/). |
| Integración | [Contrato del target](05-api/target-contract.md) y [API OpenAPI](05-api/openapi.yaml). |
| Seguridad | [Política de autorización](03-seguridad/politica-de-autorizacion.md) y [modelo de amenazas](03-seguridad/modelo-de-amenazas.md). |
| Evaluación | [Estrategia](04-ia/estrategia-evaluacion.md), [casos de prueba](04-ia/casos-de-prueba.md) y [referencias](01-investigacion/referencias.md). |
| Operación | [Modelo de datos](06-base-de-datos/modelo-datos.md) y [guía de despliegue](../docker/production/README.md). |

OpenSpec es la fuente de verdad funcional. Las [especificaciones consolidadas](../openspec/specs/) cubren streaming, variables de sesión, workflows, credenciales por paso y observaciones RAG/herramientas. Los [cambios OpenSpec](../openspec/changes/) contienen los contratos en evolución o aún no consolidados. Las evidencias de validación describen la revisión comprobada, no el estado actual de un servicio desplegado.

## Material histórico

La memoria (`07-memoria/`), los materiales de entrega (`08-entrega-tfm/`) y sus evidencias conservan el contexto académico original. Sus checklists, hitos y estados no son obligaciones ni indicadores de estado del producto. Los [cambios archivados](../openspec/changes/archive/) conservan decisiones, contratos y resultados por incremento; no se actualizan retrospectivamente al revisar estas guías.
