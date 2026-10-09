# Instrucciones del backend

Estas instrucciones son específicas de `backend/` y complementan las reglas del `AGENTS.md` situado en la raíz del repositorio.

## Stack

- Java 21 LTS.
- Spring Boot 4.1.
- Maven Wrapper (`mvnw` / `mvnw.cmd`).
- PostgreSQL 16.
- Flyway para migraciones.
- Docker Compose para PostgreSQL en el entorno local inicialmente.
- Arquitectura de monolito modular con principios hexagonales.

## Fuente de verdad

La fuente de verdad funcional del producto es `openspec/specs/` y la de los cambios en curso es `openspec/changes/`. `docs/` contiene la memoria del TFM, investigación, diagramas y documentación consolidada.

Antes de implementar una funcionalidad:

1. Revisar la especificación o el cambio correspondiente en `openspec/`.
2. Revisar la arquitectura y las decisiones en `docs/02-arquitectura/`.
3. Revisar las reglas de seguridad en `docs/03-seguridad/`.
4. Revisar el contrato de API en `docs/05-api/openapi.yaml`.
5. Revisar el modelo de datos en `docs/06-base-de-datos/`.

Si el código, la petición o una decisión nueva contradicen la documentación, señalar la contradicción antes de modificar el comportamiento.

## Arquitectura

El backend es un monolito modular. No crear microservicios ni separar procesos salvo petición explícita.

La organización debe favorecer la arquitectura hexagonal:

```text
src/main/java/com/llmscannerseguridad/
├── domain/          # Entidades, objetos de valor y reglas de negocio
├── application/     # Casos de uso y puertos
├── adapters/
│   ├── in/          # REST y otros puntos de entrada
│   └── out/         # Persistencia, IA, HTTP y otros adaptadores
└── config/          # Configuración de Spring y composición de dependencias
```

La estructura definitiva debe ajustarse a `docs/02-arquitectura/`. No introducir dependencias de infraestructura en el dominio si pueden evitarse.

## Reglas de implementación

- Mantener los casos de uso independientes de los controladores REST.
- Definir puertos para repositorios, aplicaciones objetivo, evaluadores LLM y generadores de informes.
- Mantener separados el endpoint objetivo y el modelo evaluador.
- Validar las entradas en los límites de la aplicación y también proteger las reglas del dominio.
- Utilizar DTOs para los contratos REST; no exponer directamente entidades de persistencia.
- Mantener respuestas y errores HTTP consistentes.
- No incluir secretos, credenciales ni valores sensibles en el código.
- Utilizar configuración externa mediante variables de entorno y perfiles Spring.
- Mantener las operaciones de análisis preparadas para ejecución controlada y limitada.

## Seguridad del scanner

El sistema solo debe analizar endpoints propios o autorizados.

- Validar URLs y limitar protocolos permitidos.
- Mitigar SSRF bloqueando localhost, rangos privados, interfaces locales y endpoints de metadatos cloud cuando corresponda.
- Aplicar timeouts, límites de tamaño, límites de concurrencia y límites de peticiones.
- No permitir pruebas destructivas o de denegación de servicio.
- Tratar las respuestas del endpoint objetivo como datos no confiables.
- Delimitar las respuestas antes de enviarlas al evaluador LLM.
- Redactar secretos y datos sensibles en logs, evidencias e informes.
- No confiar exclusivamente en el evaluador LLM: combinar reglas deterministas y evaluación semántica cuando proceda.

## PostgreSQL y Flyway

Las migraciones oficiales se encuentran en:

```text
src/main/resources/db/migration/
```

Convenciones obligatorias:

- Usar nombres `V<número>__<descripcion>.sql`.
- No modificar migraciones aplicadas.
- Crear una nueva migración para cada cambio del esquema.
- Mantener el esquema documentado en `docs/06-base-de-datos/`.
- No utilizar `ddl-auto=create`, `create-drop` o `update` en entornos compartidos.
- No ejecutar migraciones sin petición explícita del usuario.

PostgreSQL se ejecuta inicialmente mediante `docker/docker-compose.yml`. La aplicación debe conectarse mediante variables de entorno y perfiles Spring, nunca mediante credenciales hardcodeadas para entornos reales.

## Perfiles de configuración

Los perfiles se mantienen en `src/main/resources/`:

- `application.yml`: configuración común.
- `application-local.yml`: desarrollo local con Docker.
- `application-pre.yml`: preproducción.
- `application-pro.yml`: producción.

Los valores específicos de cada entorno deben recibirse mediante variables de entorno. No guardar secretos ni configuraciones sensibles en Git.

## API

- Mantener los endpoints bajo `/api/`.
- Respetar `docs/05-api/openapi.yaml`.
- Documentar cambios de contrato antes o junto con su implementación, cuando estén autorizados.
- Utilizar validación de entrada y respuestas de error previsibles.
- No filtrar stack traces, credenciales, consultas SQL ni detalles internos en respuestas HTTP.

## Validaciones

No ejecutar por defecto Maven, Maven Wrapper, compilaciones, tests, linters, formatters, análisis estáticos, servidores, Docker builds ni migraciones. Solo hacerlo cuando el usuario solicite expresamente la comprobación o el comando.

Después de modificar el backend, indicar los comandos que el usuario puede ejecutar para validarlo.
