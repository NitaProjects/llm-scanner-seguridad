# LLM Scanner Seguridad

## Descripción

LLM Scanner Seguridad es una plataforma web defensiva para evaluar, con autorización, la seguridad de aplicaciones basadas en modelos de lenguaje. El proyecto forma parte de un Trabajo Fin de Máster.

## Estado del proyecto

En desarrollo. La documentación, el alcance del MVP y las decisiones arquitectónicas se están consolidando antes de iniciar la implementación guiada por OpenSpec.

## Funcionalidades

El MVP previsto incluye gestión de proyectos, configuración de targets autorizados, ejecución asíncrona de scans, evaluación híbrida, findings, dashboard, informes y una aplicación vulnerable de demostración.

Las categorías iniciales son LLM01, LLM02, LLM08 y LLM10 del OWASP GenAI LLM Top 10 (2026). La implementación está **Pendiente de implementación**.

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

**Pendiente de implementación.** Se documentará el resto del procedimiento validado para preparar frontend, backend y PostgreSQL.

## Ejecución

**Pendiente de implementación.** Se documentará el procedimiento validado para ejecutar la aplicación y sus servicios locales.

## Configuración

La configuración se documentará cuando se definan los procedimientos de ejecución y despliegue. No se incluirán secretos, API keys ni credenciales en el repositorio.

## Seguridad y uso autorizado

El scanner debe utilizarse únicamente contra aplicaciones propias, autorizadas o preparadas específicamente para pruebas. La documentación de seguridad se encuentra en [`docs/03-seguridad/`](docs/03-seguridad/).

## Testing

**Pendiente de implementación.** La estrategia de validación y el experimento académico se documentan en [`docs/01-investigacion/experimento-academico.md`](docs/01-investigacion/experimento-academico.md).

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
