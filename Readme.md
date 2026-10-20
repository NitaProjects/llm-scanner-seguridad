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

La [demo sintética local](demo-vulnerable/README.md) implementa un target independiente y determinista con cuatro escenarios y sus controles seguros, sin LLM real ni BD. Su integración E2E, Docker y la comprobación de safety con aislamiento externo siguen pendientes.

La instalación/ejecución y preparación de producción se documentan abajo, con validación local
en aws-production-readiness. La operación real local de OpenAI está acreditada en
[real-evaluator-validation](openspec/changes/real-evaluator-validation/tasks.md). El despliegue AWS
está operativo según la [evidencia del operador](openspec/changes/aws-production-readiness/evidence/2026-10-20-aws-deployment.md), con aceptación completa pendiente. Experimento académico y entregables finales siguen pendientes; no se atribuye a AWS evidencia local.

## Funcionalidades

El backend core implementado incluye gestión de proyectos, configuración de targets, ejecución asíncrona de scans, evaluación híbrida, findings, `scoring-v1` e informes estructurados persistidos `report-v1`. Configurar un target no autoriza por sí solo el tráfico; la ejecución exige la política externa de autorización.

El plan MVP ejecuta exactamente cuatro SecurityTests inmutables, uno por categoría LLM01, LLM02, LLM08 y LLM10 del OWASP GenAI LLM Top 10 (2026), sin selección configurable por usuario. No acredita cobertura completa de esas categorías; LLM10 observa únicamente output, sin verificar el consumidor posterior.

Para confirmar `COMPLETED`, los scans nuevos requieren cuatro ejecuciones y cuatro evaluaciones `COMPLETED`, un `ScanScore` y un `SecurityReport` íntegros. Score, informe y transición de Scan a `COMPLETED` se confirman conjuntamente. Un fallo técnico global puede llevar a `FAILED`; un fallo o degradación del componente LLM puede consolidarse como `INCONCLUSIVE` o `VULNERABLE` según las reglas y la evidencia, sin implicar automáticamente `Scan FAILED`. Los históricos conservan su semántica sin recálculo ni backfill.

El score es heurístico, no una probabilidad ni un porcentaje de seguridad. Score 0 significa ningún Finding detectado en las pruebas completadas; no demuestra que la aplicación sea segura. `INCONCLUSIVE` se conserva por separado y no equivale a `NOT_VULNERABLE`.

El dashboard/frontend y la integración real con la demo siguen en progreso. Las exportaciones HTML/Markdown/PDF permanecen como evolución futura.

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
- AWS: despliegue académico en eu-north-1, con aceptación completa pendiente.

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

Desde un checkout de la revisión aprobada (sin necesitar `target/`, `node_modules/` ni
manifests locales previos), se requieren JDK21, Maven3.9.16 mediante el wrapper,
Node22 LTS compatible con Angular21 y npm10.8.2, PostgreSQL16 y Docker/Compose si se usa
el entorno contenerizado. Las versiones y digests comprobados están en
[images.json](docker/production/images.json). No cambies package-lock.json ni instales
versiones nuevas para reproducir. Las primeras descargas de dependencias/imágenes requieren red;
las pruebas no requieren OpenAI ni AWS. En Windows usa `mvnw.cmd`; en Linux/macOS usa
`sh ./mvnw`: los wrappers están versionados sin permiso ejecutable y no necesitan chmod.

Builds desde la raíz (PowerShell):

```powershell
Set-Location frontend
npm ci
npm run build -- --configuration production
Set-Location ../backend
.\mvnw.cmd -DskipTests package
Set-Location ../demo-vulnerable
.\mvnw.cmd -DskipTests package
Set-Location ..
```

Angular genera `frontend/dist/frontend/browser`; producción consume `/api` del mismo
origen, sin dev-server. El backend genera su jar en `backend/target/`. La demo se construye
solo para pruebas locales. Maven admite `-o` si la caché está completa. Los builds no
sustituyen tests; su ejecución se registra en tasks.md del change correspondiente.

Alternativa reproducible de frontend sin Node instalado en el host: el Dockerfile de ingress
usa Node22 y npm10.8.2 exactos, ejecuta npm ci desde el lockfile en una capa limpia y copia
únicamente el build estático a Nginx. Después de construir el jar del backend:

```powershell
docker build -f docker/production/Dockerfile.backend -t llm-scanner-backend:production .
docker build -f docker/production/Dockerfile.ingress -t llm-scanner-ingress:production .
```

Las imágenes base se fijan por digest y los contextos excluyen secretos/artefactos ajenos.
El jar es un artefacto explícito del build anterior: no reutilices un jar de otra revisión.

## Ejecución

### Desarrollo local

`docker/docker-compose.yml` es **exclusivamente local**, con sus propios datos y credenciales
de desarrollo; no se convierte en producción ni se reutiliza su contraseña/dump. Un operador
prepara la BD local `llm_scanner_seguridad` y suministra DB_USERNAME/DB_PASSWORD por entrada
segura. No uses valores locales como secretos AWS.

```powershell
docker compose -f docker/docker-compose.yml up -d
$env:SPRING_PROFILES_ACTIVE = 'local'
$env:DB_URL = 'jdbc:postgresql://localhost:5432/llm_scanner_seguridad'
# DB_USERNAME y DB_PASSWORD ya preparados de forma segura en esta terminal.
java -jar backend/target/llm-scanner-seguridad-0.0.1-SNAPSHOT.jar
```

En otra terminal, desde frontend, `npm start` sirve localhost4200 y proxy `/api` hacia
localhost8080. `GET http://localhost:8080/api/projects` debe devolver200/JSON. No existe
Actuator; no usar su /health como comprobación. PostgreSQL usa pg_isready, complementado
con Flyway. Al arrancar, Flyway aplica las migraciones oficiales
`backend/src/main/resources/db/migration/` y JPA valida el esquema. No baseline para ocultar
errores ni ddl-auto=create/update. Recovery y ejecución son single-instance.

### Demo y evaluator: opt-in local

Desde una terminal sin secretos del scanner, configurar DEMO_PROFILE=VULNERABLE y arrancar
el jar demo en127.0.0.1:8081. Ver [demo-vulnerable/README.md](demo-vulnerable/README.md) para
health, contrato, plantillas y binding. El scanner local requiere además
TARGET_LOCAL_DEMO_ENABLED=true y TARGET_LOCAL_DEMO_ORIGINS=http://127.0.0.1:8081.

Crear Project/Target reales por la API, releer IDs/updatedAt exactos y completar la plantilla
del perfil elegido y bindings.local.json según sus schemas; nunca copiar UUIDs de evidencia
ni usar placeholders como instancia válida. EVALUATION_SCENARIO_FILE apunta al JSON local
completado. Mantener EVALUATOR_EXPORT_ENABLED=false por defecto: la demo funciona sin LLM real.

OpenAI es otra autorización: manifest enabled=true/provider OPENAI, binding/corpus exactos,
gate global true, EVALUATOR_ALLOWED_SCENARIOS con solo el escenario aprobado y
EVALUATOR_API_KEY únicamente en el entorno del backend. Modelo aprobado gpt-6-luna, hasta4
llamadas/Scan/20s/intervalo1s/output1024tokens; coste/tarifa deben aprobarse aparte. No pedir
claves por chat, escribirlas en archivos ni copiarlas a demo/frontend. Tras operación,
detener scanner o reiniciarlo sin clave/exportación, deshabilitar manifest y retirar la
variable de su terminal. No repetir esta operación para validar instalación offline.

### Producción preparada

La [guía de producción](docker/production/README.md) detalla el Compose separado, secretos,
volumen nuevo, TLS, Basic Auth, arranque, health y actualización. Se requieren PUBLIC_HOSTNAME,
PUBLIC_ORIGIN, SECRETS_DIR, TLS_DIR, PGDATA_VOLUME e imágenes explícitas BACKEND_IMAGE/INGRESS_IMAGE.
Localmente pueden usarse IDs de imágenes construidas; en AWS deben ser referencias privadas ECR@sha256.
La primera instalación crea antes infraestructura/repositorios y después publica las imágenes;
CloudFormation no recibe digests ni arranca Compose. El [bootstrap ECR de la guía](docker/production/README.md#bootstrap-inicial-ecr-y-configuración-por-digest)
explica cómo obtener las URIs y exportar BACKEND_IMAGE/INGRESS_IMAGE en el host de Compose.
Los ficheros secretos se obtienen mediante bootstrap de host, no valores en environment de Compose.

```powershell
# Solo configuración no secreta; suministrar también las rutas/volumen/hostname de la guía.
$env:BACKEND_IMAGE = docker image inspect llm-scanner-backend:production --format '{{.Id}}'
$env:INGRESS_IMAGE = docker image inspect llm-scanner-ingress:production --format '{{.Id}}'
docker compose -f docker/production/compose.yml config --quiet
docker compose -f docker/production/compose.yml up -d --no-build
docker compose -f docker/production/compose.yml exec ingress /opt/readiness.sh
```

No activar pro con secretos ausentes: el entrypoint falla cerrado. Se crea BD de producción
nueva `llm_scanner_seguridad_pro`, rol scanner_app sin privilegios globales y se aplica Flyway
oficial. El volumen es externo, persistente y separado del local; AWS lo sitúa en EBS cifrado.
Solo443 se publica;8080/5432 son internos. La sonda descarta el cuerpo y exige200/JSON.
No usar `down -v`, limpiar datos ni ejecutar dos backends contra la misma BD.

## Configuración

El contrato del target y sus controles están documentados en [target-contract.md](docs/05-api/target-contract.md), el executor en [ADR-003](docs/02-arquitectura/decisiones/ADR-003-ejecucion-asincrona-scans.md) y el evaluador en [prompts-evaluadores.md](docs/04-ia/prompts-evaluadores.md).

**Spring Boot NO carga `.env` automáticamente.** La interpolación `.env` de Compose tampoco
inyecta automáticamente variables al proceso Java; deben existir en environment explícito o
en el bootstrap autorizado. No guardar secretos en .env. Un proceso abierto conserva su
entorno anterior: cambiar otra terminal no cambia su configuración efectiva.

Producción requiere DB_URL/DB_USERNAME y DB_PASSWORD nueva, TARGET_CREDENTIAL_ENCRYPTION_KEY
Base64 de32bytes conservada, htpasswd bcrypt externo y certificados TLS externos.
El entrypoint lee únicamente los ficheros segregados necesarios y exporta DB_PASSWORD/AES
al Java, sin valores en YAML/argv/imagen. EVALUATOR_API_KEY es opcional y ausente en AWS,
evaluator export false, scenario ausente y allowlists vacías. CORS_ALLOWED_ORIGINS es el
origen HTTPS canónico. La demo no se despliega; no relajar SSRF ni activar local en AWS.

## Seguridad y uso autorizado

El scanner debe utilizarse únicamente contra aplicaciones propias, autorizadas o preparadas específicamente para pruebas. La documentación de seguridad se encuentra en [`docs/03-seguridad/`](docs/03-seguridad/).

## Testing

El backend dispone de pruebas de dominio, casos de uso, API, transporte con fixtures/fakes, persistencia y migraciones sobre PostgreSQL de test. Los registros de cada change conservan los comandos, resultados y límites de las validaciones ejecutadas. El [registro de reporting](openspec/changes/reporting/tasks.md#validaciones-ejecutadas) documenta una regresión de 400 pruebas, sin fallos, errores ni omisiones, el 2026-10-10; es evidencia técnica registrada, no una validación del proveedor real.

La validación real local de OpenAI se registra en su change; no se ejecuta al instalar AWS.
El [experimento académico](docs/01-investigacion/experimento-academico.md) requiere dataset,
ground truth, repeticiones y análisis propios; las pruebas técnicas no acreditan calibración
estadística ni resultados científicos.

## Despliegue

Aplicación publicada en **[https://llmscannerseguridad.com](https://llmscannerseguridad.com)**,
con Basic Auth: requiere credenciales de prueba entregadas por canal privado; nunca incluir
la contraseña real en el repositorio. La entrega al tribunal todavía no está acreditada.
[Evidencia real comunicada por el operador, 2026-10-20](openspec/changes/aws-production-readiness/evidence/2026-10-20-aws-deployment.md).
Guía en [docker/production](docker/production/README.md) y plantilla
[aws.yml](docker/production/aws.yml). En eu-north-1 (Stockholm), una EC2 m7i-flex.large,
Amazon Linux 2023 x86_64, Docker25.0.14/Compose v2.40.3 y tres
contenedores: Nginx/Angular→backend→PostgreSQL16.443 público con TLS y Basic Auth,
22/80/8080/5432 cerrados; sin conocer IP del tribunal. Allowlist IP opcional, desactivada.

TLS se emite fuera de contenedores con Let's Encrypt/Certbot **manual DNS-01**; A hacia EIP
y control TXT del dominio. Sin ACME automático ni puerto80/cuarto contenedor. Certificados,
private keys y estado ACME quedan fuera de Git. Instalar directorio TLS read-only, validar
nginx -t y renovar manualmente con30días o menos, comprobando certificado servido tras reload.

Secretos suministrados mediante Parameter Store SecureString y bootstrap a `/run/llm-scanner`
tmpfs, con permisos por UID. PostgreSQL16 usa EBS30GB/XFS en `/srv/llm-scanner/postgres` y
volumen externo `llm-scanner-pgdata`. Certificado Let's Encrypt con expiración observada
2027-01-18, DNS-01 manual mediante Cloudflare; renovación no automática. Acceso administrativo
mediante Systems Manager. Se observaron 401 sin credencial, login Chrome, Angular/API y
persistencia de un proyecto tras recargar. No se ejecutó Scan ni se desplegó demo; OpenAI
permanece desactivado por defecto en AWS. El review acredita AMI real, herramientas,
imágenes por digest, provisión y acceso académico preparado: **24/25 tareas completas**.
6.1 completada; solo queda restore aislado (6.3). Renovación manual mediante Certbot en Docker
y DNS-01, procedimiento validado confirmado por el operador. Despliegue académico autorizado
en Free Plan/créditos, sin migración voluntaria autorizada a Paid Plan ni gasto exacto medido.
La URL se mantendrá hasta finalizar entrega/evaluación; después podrán detenerse o retirarse
recursos innecesarios. Responsable operativo/TLS: operador del proyecto. La entrega efectiva
de credenciales al tribunal es una acción administrativa por canal privado aún no acreditada;
el despliegue no acredita el TFM completo. No publicar Account ID, Instance ID, IP directa ni
URI ECR con Account ID; usar placeholders en procedimientos y conservar el dominio público.

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

La demo está en `demo-vulnerable/` y se ejecuta solo localmente. La instalación académica
preparada exige Basic Auth en Nginx para UI y API, sin usuarios/roles Spring. El operador
entregará la credencial técnica por canal reservado; ninguna contraseña de evaluación se
publica en este README. Esa entrega no se acredita hasta desplegar y verificar acceso real.

## Slides

**Pendiente de entrega.** El enlace se añadirá en [`docs/08-entrega-tfm/enlaces-entrega.md`](docs/08-entrega-tfm/enlaces-entrega.md) cuando exista.

## Vídeo

**Pendiente de entrega.** El enlace se añadirá en [`docs/08-entrega-tfm/enlaces-entrega.md`](docs/08-entrega-tfm/enlaces-entrega.md) cuando exista.
