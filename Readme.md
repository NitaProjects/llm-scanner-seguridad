# LLM Scanner Seguridad

## Descripción

**LLM Scanner Seguridad** es una plataforma web defensiva orientada a evaluar, de forma controlada y autorizada, la seguridad de aplicaciones que incorporan modelos de lenguaje (LLM).

El proyecto nace como Trabajo Fin de Máster y tiene un doble objetivo: por un lado, aplicar de forma práctica conceptos de desarrollo de software, inteligencia artificial y ciberseguridad; por otro, explorar cómo automatizar parte de la evaluación de seguridad de aplicaciones basadas en LLM mediante pruebas reproducibles y análisis asistido por inteligencia artificial.

El backend permite configurar una aplicación objetivo mediante su API, ejecutar un conjunto controlado de pruebas de seguridad y registrar las respuestas y evidencias obtenidas. Aplica una estrategia de evaluación híbrida que combina reglas deterministas con un modelo de lenguaje evaluador cuando el escenario sintético y su exportación están autorizados, genera findings sustentados, calcula `scoring-v1` y conserva un informe estructurado `report-v1`.

El alcance actual se centra en cuatro categorías inspiradas en OWASP GenAI LLM Top 10:

- **LLM01 – Prompt Injection**
- **LLM02 – Sensitive Information Disclosure**
- **LLM08 – Hidden Context Exposure**
- **LLM10 – Improper Output Handling**

LLM Scanner Seguridad no pretende sustituir herramientas profesionales de auditoría ni afirmar una cobertura completa de todos los riesgos asociados a aplicaciones de inteligencia artificial. Se plantea como un **MVP académico funcional y extensible**, diseñado para demostrar una arquitectura y una metodología de análisis que puedan evolucionar progresivamente.

### ¿Qué tipo de aplicaciones pretende analizar?

El objetivo principal son aplicaciones que incorporan un LLM detrás de una API controlada por su propietario, por ejemplo:

- chatbots internos;
- asistentes empresariales;
- aplicaciones web que integran modelos de lenguaje;
- modelos open source desplegados de forma local o privada;
- servicios propios construidos sobre proveedores externos de IA;
- prototipos y pequeñas aplicaciones que necesitan una primera evaluación de seguridad.

La plataforma no tiene como objetivo auditar directamente servicios generales como ChatGPT, Claude o Gemini sin autorización. El objeto de evaluación es la **aplicación desarrollada alrededor del modelo y el endpoint autorizado que esta expone**.

### Motivación

La incorporación de modelos de lenguaje en aplicaciones introduce una nueva superficie de ataque. Vulnerabilidades como la inyección de prompts, la exposición de información sensible o la revelación de contexto interno no siempre pueden analizarse utilizando únicamente las herramientas tradicionales de seguridad web.

Además, las respuestas de un LLM son probabilísticas y semánticas. Dos respuestas distintas pueden representar el mismo comportamiento inseguro aunque no compartan exactamente las mismas palabras.

Por este motivo, LLM Scanner Seguridad plantea una aproximación híbrida:

1. ejecutar pruebas de seguridad controladas;
2. capturar la evidencia producida por la aplicación objetivo;
3. aplicar comprobaciones deterministas cuando exista una señal objetiva;
4. utilizar un LLM evaluador para analizar semánticamente los casos que requieren interpretación;
5. generar findings sustentados, puntuaciones heurísticas e informes estructurados trazables.

Esta combinación busca conservar la reproducibilidad de las reglas tradicionales sin renunciar a la capacidad de interpretación que ofrece la inteligencia artificial.

### Evolución futura

El proyecto ha sido diseñado para poder crecer más allá del alcance del TFM.

Entre las posibles líneas de evolución se encuentran:

- ampliar el catálogo de pruebas y la cobertura OWASP;
- generar o adaptar dinámicamente pruebas mediante IA;
- incorporar nuevos motores de evaluación;
- comparar distintos modelos evaluadores;
- exportar el informe estructurado a HTML, Markdown o PDF;
- comparar scans y sus históricos;
- integrar el scanner en pipelines CI/CD;
- realizar análisis periódicos;
- incorporar autenticación, organizaciones y gestión de equipos;
- soportar arquitecturas LLM más complejas, como RAG, agentes y uso de herramientas;
- ofrecer integraciones con plataformas de desarrollo y seguridad.

A nivel empresarial, una evolución natural sería convertir el sistema en una herramienta de **LLM Security Testing / AI Security Testing**, orientada a equipos que desarrollan aplicaciones con modelos de lenguaje y necesitan incorporar comprobaciones de seguridad durante el ciclo de desarrollo.

El modelo podría evolucionar hacia un servicio SaaS, una herramienta desplegable dentro de la infraestructura del cliente o una integración de seguridad para pipelines de desarrollo.

El objetivo del TFM no es demostrar la viabilidad comercial de ese producto completo, sino construir y validar técnicamente una base suficientemente sólida sobre la que una solución de este tipo pueda evolucionar.

## Estado del proyecto

En desarrollo. El backend core está aprobado e implementado mediante los changes `project-management`, `target-configuration`, `scan-execution`, `security-test-execution`, `evaluation-engine`, `scoring-engine` y `reporting`.

Los contratos aprobados están actualmente en `openspec/changes/<cambio>/specs/<capacidad>/spec.md`, junto con sus diseños y tareas. `openspec/specs/` todavía no contiene las specs consolidadas; su sincronización y el archivo de changes podrán realizarse posteriormente según el workflow OpenSpec.

Frontend, demo vulnerable, procedimientos completos de instalación/ejecución, experimento académico, operación real de OpenAI, despliegue AWS y entregables finales del TFM siguen en progreso o pendientes. Las validaciones técnicas registradas no completan esos trabajos.

## Funcionalidades

El backend core implementado incluye gestión de proyectos, configuración de targets, ejecución asíncrona de scans, evaluación híbrida, findings, `scoring-v1` e informes estructurados persistidos `report-v1`. Configurar un target no autoriza por sí solo el tráfico; la ejecución exige la política externa de autorización.

El plan MVP ejecuta exactamente cuatro SecurityTests inmutables, uno por categoría LLM01, LLM02, LLM08 y LLM10 del OWASP GenAI LLM Top 10 (2026), sin selección configurable por usuario. No acredita cobertura completa de esas categorías; LLM10 observa únicamente output, sin verificar el consumidor posterior.

Para confirmar `COMPLETED`, los scans nuevos requieren cuatro ejecuciones y cuatro evaluaciones `COMPLETED`, un `ScanScore` y un `SecurityReport` íntegros. Score, informe y transición de Scan a `COMPLETED` se confirman conjuntamente. Un fallo técnico global puede llevar a `FAILED`; un fallo o degradación del componente LLM puede consolidarse como `INCONCLUSIVE` o `VULNERABLE` según las reglas y la evidencia, sin implicar automáticamente `Scan FAILED`. Los históricos conservan su semántica sin recálculo ni backfill.

El score es heurístico, no una probabilidad ni un porcentaje de seguridad. Score 0 significa ningún Finding detectado en las pruebas completadas; no demuestra que la aplicación sea segura. `INCONCLUSIVE` se conserva por separado y no equivale a `NOT_VULNERABLE`.

El dashboard/frontend y la aplicación vulnerable de demostración siguen en progreso. Las exportaciones HTML/Markdown/PDF permanecen como evolución futura.

## Arquitectura

Monolito modular con principios de Clean Architecture / arquitectura hexagonal:

```text
Angular → Spring Boot REST API → aplicación/dominio → puertos/adaptadores
                                      ↓
                    PostgreSQL / target autorizado / evaluador LLM
```

Consulta la [arquitectura conceptual](docs/02-arquitectura/arquitectura-general.md).

## Stack tecnológico

- Angular 21.
- Java 21 LTS.
- Spring Boot 4.1.
- Maven Wrapper.
- PostgreSQL 16.
- Flyway.
- Docker Compose para el entorno local.
- AWS como despliegue final previsto.

## Estructura del repositorio

- `frontend/`: aplicación Angular.
- `backend/`: API Spring Boot.
- `demo-vulnerable/`: aplicación vulnerable para demostraciones.
- `docs/`: documentación técnica, investigación, arquitectura, memoria y entrega del TFM.
- `openspec/`: especificaciones y changes implementables.
- `docker/`: configuración de infraestructura local.
- `.github/workflows/`: automatizaciones CI/CD cuando estén definidas.

## Instalación

El backend requiere **JDK 21** para compilar, ejecutar tests y empaquetar. `JAVA_HOME` debe apuntar al directorio del JDK 21 y su carpeta `bin` debe aparecer en `PATH` antes que instalaciones anteriores de Java.

En Windows, después de actualizar estas variables, reinicia las terminales y el IDE o aplicación desde la que ejecutes Maven: los procesos abiertos conservan el entorno anterior. Desde `backend/`, comprueba que los tres comandos muestran Java 21:

```powershell
java -version
javac -version
.\mvnw.cmd -version
```

**Pendiente de documentación y validación del procedimiento completo.** Se documentará el resto de la preparación de frontend, backend y PostgreSQL.

## Ejecución

**Pendiente de documentación y validación del procedimiento completo.** Se documentará cómo ejecutar la aplicación y sus servicios locales.

## Configuración

El contrato del target y sus controles están documentados en [target-contract.md](docs/05-api/target-contract.md), el executor en [ADR-003](docs/02-arquitectura/decisiones/ADR-003-ejecucion-asincrona-scans.md) y el evaluador en [prompts-evaluadores.md](docs/04-ia/prompts-evaluadores.md). La guía operativa completa sigue pendiente junto con los procedimientos de ejecución y despliegue. No se incluirán secretos, API keys ni credenciales en el repositorio.

## Seguridad y uso autorizado

El scanner debe utilizarse únicamente contra aplicaciones propias, autorizadas o preparadas específicamente para pruebas. La documentación de seguridad se encuentra en [`docs/03-seguridad/`](docs/03-seguridad/).

## Testing

El backend dispone de pruebas de dominio, casos de uso, API, transporte con fixtures/fakes, persistencia y migraciones sobre PostgreSQL de test. Los registros de cada change conservan los comandos, resultados y límites de las validaciones ejecutadas. El [registro de reporting](openspec/changes/reporting/tasks.md#validaciones-ejecutadas) documenta una regresión de 400 pruebas, sin fallos, errores ni omisiones, el 2026-10-10; es evidencia técnica registrada, no una validación del proveedor real.

La operación real de OpenAI y el benchmark académico siguen pendientes. El [experimento académico](docs/01-investigacion/experimento-academico.md) requiere dataset, ground truth, repeticiones y análisis propios; las pruebas técnicas no acreditan calibración estadística ni resultados científicos.

## Despliegue

El despliegue final está previsto en AWS. La URL real es **Pendiente de entrega**.

## Documentación

- [Documentación del proyecto](docs/README.md).
- [Visión](docs/00-contexto/vision.md).
- [Alcance](docs/00-contexto/alcance.md).
- [Arquitectura](docs/02-arquitectura/arquitectura-general.md).
- [Seguridad](docs/03-seguridad/).
- [Evaluación IA](docs/04-ia/).
- [Modelo de datos](docs/06-base-de-datos/modelo-datos.md).
- [Memoria del TFM](docs/07-memoria/indice-memoria.md).
- [Control de entrega del TFM](docs/08-entrega-tfm/README.md).

## TFM

La documentación específica de requisitos, trazabilidad y enlaces de entrega está en [`docs/08-entrega-tfm/`](docs/08-entrega-tfm/README.md).

## Demo / credenciales

La aplicación vulnerable de demostración está en `demo-vulnerable/`. No existe login documentado actualmente, por lo que no hay credenciales de prueba que publicar en esta fase.

## Slides

**Pendiente de entrega.** El enlace se añadirá en [`docs/08-entrega-tfm/enlaces-entrega.md`](docs/08-entrega-tfm/enlaces-entrega.md) cuando exista.

## Vídeo

**Pendiente de entrega.** El enlace se añadirá en [`docs/08-entrega-tfm/enlaces-entrega.md`](docs/08-entrega-tfm/enlaces-entrega.md) cuando exista.
