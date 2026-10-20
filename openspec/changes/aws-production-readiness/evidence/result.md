# Resultado de preparación — 2026-10-20

Registro histórico de preparación local. El despliegue real posterior comunicado por el operador y su aceptación parcial se documentan separadamente en [2026-10-20-aws-deployment.md](2026-10-20-aws-deployment.md). Las referencias siguientes a ausencia de AWS describen esta fase local, no el estado actual de publicación.

Estado vigente tras confirmación del operador: 1.1/6.1/6.2 COMPLETED, 6.4 COMPLETED en preparación
técnica y 24/25 completas. Certbot en Docker + DNS-01 confirmado, procedimiento validado;
6.3 PARTIAL exclusivamente por restore aislado.
La matriz enlazada contiene el estado vigente y sustituye las valoraciones históricas de pendientes
que se conservan abajo. Acceso académico preparado; entrega efectiva de credenciales no acreditada.

**READY FOR REVIEW** para preparación de producción. F1–F4 comprobados localmente.
No acredita despliegue ni acceso público. Sin AWS, OpenAI, Scans, commits o pushes.

## Implementación

- `docker/production/compose.yml`: tres servicios, separado del Compose local; imágenes explícitas, backend único, recursos y logs acotados, volumen externo y redes privadas.
- Nginx sirve Angular production y SPA/deep links. TLS externo y Basic Auth heredada en UI/assets/API; hostname canónico y Origin exacto para escrituras. `/api` conserva prefijo/status/Location y elimina Authorization hacia backend.
- Body máximo 1 MiB, buffering previo, idle de cabeceras/body de 15 s, conexiones y temporales limitados, compresión rechazada, sin retries del proxy. El idle no constituye un deadline total.
- PostgreSQL 16/SCRAM, rol nuevo `scanner_app` sin privilegios globales, contraseña distinta del bootstrap; Flyway oficial sin cambios y JPA validate.
- Profile `pro`, demo y exportación desactivados, sin escenario/allowlists/clave evaluator; SSRF existente intacto. Readiness interna GET `/api/projects` y `pg_isready`, sin Actuator.
- Bootstrap futuro de secretos en tmpfs, segregados por UID/servicio; no se ha ejecutado SSM/KMS. Plantilla CloudFormation preparada con único ingreso 443, IMDSv2/hop 1 y EBS cifrado/Retain.

## Comprobaciones ejecutadas

Detalles reproducibles en [builds.json](builds.json), [local-validation.json](local-validation.json), [clean-backend-build.json](clean-backend-build.json) e [infrastructure.json](infrastructure.json).

| Comprobación | Resultado |
| --- | --- |
| Backend JDK 21 / Maven Wrapper 3.9.16 | package PASS |
| Copia limpia de fuentes backend, sin target/configuración ignorada previa | package PASS |
| Frontend production en etapa Docker limpia, Node 22 / npm 10.8.2, lockfile existente | build PASS |
| Tests frontend sin red | 161 PASS, 10 archivos |
| Tests backend relevantes de destino/SSRF | 35 PASS |
| Entorno completo de tres servicios y fixtures HTTP | 51 PASS |
| Nginx | configuración válida y respuestas comprobadas por HTTPS |
| Flyway con BD nueva | V1–V7 correctas, cuatro test_cases; reinicio conserva proyecto e historial |
| Checksum inválido, solo en BD sintética propia | falla readiness; checksum de fixture restaurado |
| TLS sintético renovado | nuevo serial/caducidad servido; auth conservada, sin reiniciar backend/PG |
| CloudFormation/bootstrap/wrapper/instancia offline | 16 grupos PASS tras actualizar selección x86_64; sin validate-template del servicio AWS |
| OpenSpec strict | PASS |

F1: sin credencial/credencial incorrecta devuelve 401; assets y deep links protegidos.
Host/Origin ajenos rechazados y escrituras denegadas no llegan al contador upstream.
F2: oversized Content-Length/chunked devuelve 413 sin upstream; compresión 415; body
inactivo se aborta y el límite de ocho conexiones por IP opera. El mayor TargetConfiguration
permitido por el contrato se aceptó. Marcadores sintéticos no aparecen en access logs.

Los contextos de build excluyen secretos y artefactos ajenos; el backend incorpora únicamente
el jar construido explícitamente. Se verificaron fuentes actuales del working tree en copia
limpia, no un commit nuevo ni un clone inexistente. Las primeras descargas de imágenes/npm
fueron necesarias para compilar; no hubo tráfico al proveedor ni a servicios AWS.

Dos incidencias de validación se resolvieron sin cambiar el producto: el harness consultaba
inicialmente un nombre de tabla incorrecto (corregido a `test_cases`); el empaquetado de la
copia limpia necesitó repetir el comando con permisos del ejecutor por bloqueo del rename.
La ejecución final de cada comprobación terminó correctamente.

## Puertos y cierre

Configuración de producción: solo host 443 hacia ingress 8443 no root; backend 8080 y
PostgreSQL 5432 sin publicación. Prueba local: únicamente `127.0.0.1:18443`. Ingress no
pertenece a la red de datos y no pudo conectar con PostgreSQL.

Contenedores/red de prueba retirados y archivos secretos/certificados sintéticos eliminados.
Se conserva el volumen externo de la última prueba `apr-data-cd1fe11a70`, con datos sintéticos;
no se eliminaron volúmenes automáticamente. El PostgreSQL de desarrollo preexistente permanece
operativo y publica su propio 5432; no pertenece al stack de producción y no fue modificado.
No se dejó un scanner de prueba activo ni se suministró una clave evaluator a los contenedores.

## Límites y verificaciones pendientes

- Tarea 1.1 parcial: herramientas, arquitectura e imágenes fijadas; el operador ha comunicado Amazon Linux 2023 x86_64, pero el identificador exacto de AMI y su verificación no constan en esta evidencia. Mantener pendiente con 6.1.
- 6.1–6.4 pendientes: completar evidencias de cuenta/coste, AMI/SSM, dominio/DNS, publicación ECR, provisión real, certificado Let's Encrypt, entrega reservada de Basic Auth y aceptación desde Internet. La región y selección comunicadas abajo no acreditan despliegue completado.
- Verificar semántica CloudFormation/IAM/SSM/KMS y conectividad efectiva en la fase de despliegue autorizada; el test offline es estructural.
- Procedimientos de backup/restore y actualización documentados; restore real aislado aún pendiente. Datos EBS/ECR retenidos pueden seguir generando coste.
- Renovación manual DNS-01 exige responsable; no ACME automático ni puerto 80. Solo se probó TLS con material sintético.
- No cambios de arquitectura core, dependencias, lockfiles, migraciones oficiales ni contratos API. El aislamiento de una EC2 no aporta alta disponibilidad.

## Corrección de revisión F1/F2

**F1 CLOSED — bootstrap ECR:** `aws.yml` no exige imágenes antes de crear sus repositorios.
Retirados los dos parámetros/Outputs de digest, que no configuraban la instancia. Los Outputs
de URI ECR siguen disponibles y el IAM de pull no se amplía. La guía detalla obtención de URI,
build/push con rol de operador separado, consulta de digest y exportación de referencias
exactas en el entorno del host antes de `compose pull/up`. CloudFormation no arranca Compose.

**F2 CLOSED — POSIX:** `sh ./mvnw` en instrucciones de instalación/build afectadas (README,
guía y diseño). Wrappers conservan modo Git 100644; sin cambios Maven/dependencias/permisos.

`python docker/production/tests/check_infrastructure.py`: 14 grupos PASS, incluidos bootstrap
sin digests iniciales, Outputs generados, pull acotado, incorporación posterior de digests
sintéticos a Compose y rechazo de cada imagen ausente. La búsqueda documental dirigida no
encontró invocaciones POSIX incompatibles. OpenSpec strict PASS.

No se ejecutó AWS, ni se lanzaron servicios/builds/Maven por estas correcciones. El intento
inicial del test offline fue bloqueado al escribir su temporal en el sandbox; la ejecución
autorizada fuera de él terminó PASS. Los resultados anteriores de producto/HTTP/BD son
históricos y no se atribuyen a esta revalidación. Ninguna tarea de publicación se cierra.

## Selección real de instancia — información del operador

El operador comunica que el primer intento CloudFormation en **eu-north-1** con
**t3.medium** terminó en **rollback** por no ser Free Tier eligible en esta cuenta.
La consulta real comunicada devolvió c7i-flex.large, t4g.small, t3.micro, t4g.micro,
m7i-flex.large y t3.small. No se consultó AWS para verificar estos hechos desde este agente.

Selección final real para el despliegue: **m7i-flex.large**, conservando **Amazon Linux 2023
x86_64**. La plantilla editada previamente por el operador contiene exactamente una lista:
`[m7i-flex.large, c7i-flex.large, t3.small, t3.micro]`, con default `m7i-flex.large`.
No se cambia AMI ni arquitectura. Se excluye `t4g.*` porque requiere ARM.

La elegibilidad Free Tier depende de cuenta/región y debe comprobarse antes de desplegar;
no supone elegibilidad universal ni coste cero de EC2 y demás recursos.
Prueba afectada y validación estructural: **16 grupos PASS**, con comprobaciones nuevas
de default/binding y allowlist exacta x86_64 sin ARM. OpenSpec strict **PASS**.
`infrastructure.json` registra configuración comprobada offline; `NOT_EXECUTED`/`NOT_QUERIED`
se refieren a operaciones AWS de este test, no niegan el intento real informado por el operador.
No se ejecutaron AWS, builds, servicios, cambios de producto, commits ni pushes en esta
actualización. No se acredita un segundo despliegue exitoso ni se cierran tareas operativas.

README raíz, guía de producción, ADR operativa y checklist actualizados únicamente con el
alcance demostrado. URL pública, despliegue y entrega final permanecen pendientes.
