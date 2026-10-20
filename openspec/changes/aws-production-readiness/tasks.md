Estado: **READY FOR REVIEW — restore 6.3 pospuesto expresamente por el operador** (2026-10-20). **24/25 tareas completas**: 1.1, 6.1 y 6.2 COMPLETED; 6.4 COMPLETED en preparación técnica. Solo 6.3 PARTIAL por restore aislado pendiente/no ejecutado; no reanudar sin nueva autorización expresa. Despliegue operativo en eu-north-1 y acceso académico preparado en https://llmscannerseguridad.com; [matriz vigente](evidence/2026-10-20-aws-deployment.md). Credenciales entregadas al tribunal no acreditadas: acción administrativa separada. Sin acceso AWS ni servicios en esta actualización. Los registros de preparación siguientes son históricos: [resultado local](evidence/result.md), [51 comprobaciones del entorno](evidence/local-validation.json), [build backend limpio](evidence/clean-backend-build.json), [infraestructura estática](evidence/infrastructure.json) y [builds/tests](evidence/builds.json).

## 1. Empaquetado y configuración reproducible

- [x] 1.1 Registrar herramientas/AMI/arquitectura e imágenes seleccionadas con versiones/digests exactos, conservando Java21, PostgreSQL16, Maven Wrapper3.9.16, npm10.8.2 y lockfiles; verificar manifiesto sin latest ni valores no comprobados (APR-01/APR-11; D-01/D-08).
- [x] 1.2 Preparar Dockerfiles de producción ingress con build Angular estático y backend con jar/JRE21, con contextos que excluyan .env, claves, manifests locales y artefactos heredados; verificar salida browser, origen /api y ausencia de servidor dev/cambios core/dependencias (APR-03/APR-07/APR-11).
- [x] 1.3 Crear docker/production/compose.yml separado del local: solo ingress443, backend sin ports/réplica1 y PG sin ports/volumen cifrable conservado; verificar redes app/data internal/egress aisladas, proxy fuera de data, sin host network/docker.sock/privileged, caps de recursos/logs/temporales (APR-03/APR-05/APR-10; D-01..D-04).

## 2. Entrada HTTPS y F1/F2

- [x] 2.1 Definir Nginx como terminador TLS443 con hostname/certificado obligatorio, Basic Auth heredada en UI/assets/deep links/API, fallo cerrado y ningún health público; verificar configuración resultante y que auth/cert ausentes impiden apertura, sin usuarios/roles internos ni requisito de IP del tribunal (APR-02; D-02).
- [x] 2.2 Configurar /api y /api/* preservando prefijo/status/Location y sin fallback SPA, autenticación sin reenvío Authorization, Origin obligatorio exacto para escrituras y CORS de producción; verificar rutas/headers y denegación de Host/Origin ajenos sin mutar contratos API (APR-03; D-02/D-03).
- [x] 2.3 Añadir límite1MiB, buffering completo, cabeceras/body idle15s, rechazo de Content-Encoding comprimido, conexiones/temporales acotados, timeout proxy y ausencia de retries; verificar que no hay location/upstream alternativo sin los controles y documentar que idle no es deadline total (APR-04; D-03).
- [x] 2.4 Configurar access logs mínimos y suprimir logs crudos de requests, preservar diagnósticos backend seguros y rotación/tamaños; verificar formato sin URI/query/usuario/credenciales/cuerpos y readiness nginx -t sin secretos (APR-10; D-07).

## 3. PostgreSQL, secretos y profile pro

- [x] 3.1 Preparar bootstrap BD/rol nuevos con PG16/SCRAM y esquema compatible con Flyway; verificar permisos app no superuser/createdb/createrole, diferencia con credenciales locales y conservación de datos/volumen, sin cambiar migraciones oficiales (APR-05/APR-06; D-04).
- [x] 3.2 Preparar bootstrap host/entrypoint mínimo para SecureString/KMS y entorno Spring con secreto BD/AES segregados por servicio, sin secretos en YAML/imagen/argv/logs; verificar falta de parámetros/profile distinto de pro falla cerrado, AES no se regenera y EVALUATOR_API_KEY AWS permanece ausente (APR-07; D-05).
- [x] 3.3 Fijar SSRF pro deny-by-default, TARGET_LOCAL_DEMO_ENABLED=false y listas vacías, sin contenedor demo; verificar invariantes y ausencia de profile local, bypass/túnel/cambios a TargetDestinationPolicy o LocalTargetUrlPolicy (APR-08; D-06).
- [x] 3.4 Preparar sonda operativa interna GET /api/projects con timeout5s/200/JSON y cuerpo descartado, PG pg_isready y comprobación externa autenticada; verificar que no usa Actuator/endpoint nuevo ni invoca target/OpenAI y no confunde SPA200 con readiness (APR-09; D-07).

## 4. Pruebas locales y README desde limpio — ejecución solo solicitada expresamente

- [x] 4.1 Preparar y, con autorización de tests/fixtures, ejecutar pruebas F1 del proxy: sin auth UI/API/assets401, credencial válida desde IP no registrada con allowlist desactivada, Host/Origin ajenos rechazados, Location preservada y API desconocida sin HTML; verificar que upstream contador no recibe peticiones denegadas y no hay puertos backend/PG publicados; probar modo IP opcional solo si se configura, sin sustituir Basic Auth (APR-02/APR-03/APR-12; D-09).
- [x] 4.2 Ejecutar bajo autorización los casos F2 con fixtures sintéticas: Content-Length/chunked>1MiB413 sin upstream, body inactivo>15s abortado, compresión415, límite de conexiones y mayor TargetConfiguration legítimo aceptado; verificar saneamiento de logs con marcadores, sin servicios externos o pruebas DoS (APR-04/APR-10; D-03/D-09).
- [x] 4.3 Sobre BD de prueba vacía autorizada, comprobar aplicación íntegra de Flyway oficial/JPA/recovery, catálogo fijo, checksum inválido falla cerrado y reinicio no duplica/borra; verificar red PG accesible solo por backend y app sin privilegios globales, sin tocar BD local compartida ni producción (APR-05/APR-06; D-04/D-09).
- [x] 4.4 Reutilizar regresiones SSRF pro existentes y añadir únicamente el caso ausente si procede; verificar loopback/privadas/metadata y resolución mixta bloqueados aun con flags de demo, sin targets reales ni nueva llamada evaluator (APR-08; D-06).
- [x] 4.5 Completar README raíz con requisitos/versiones, checkout limpio, comandos frontend/backend/demo, DB local/pro, variables no secretas y carga efectiva de entorno, .env no automático, migraciones/arranque/health, manifest/binding/gates/cierre y demo solo local; verificar enlaces y ausencia de claves/passwords/UUID hardcodeados/archivos locales obligatorios (APR-11; D-08).
- [x] 4.6 Con autorización explícita de builds/instalación reproducible, seguir README en directorio limpio sin node_modules/target/manifests previos y verificar builds/arranque offline de proveedor, /api/SPA y persistencia; registrar revisión/versiones/resultados y pasos pendientes sin usar OpenAI (APR-11/APR-12).

## 5. Infraestructura preparada, sin desplegar por defecto

- [x] 5.1 Escribir plantilla CloudFormation mínima para VPC/subred/IGW/rutas/SG/EIP/EC2/IAM/EBS/ECR, parametrizando región/cuenta/AMI/nombres de secretos y allowlist IP opcional desactivada; incorporar digests después de publicar en la configuración Compose, sin exigir imágenes al crear infraestructura/repositorios; verificar443 público0.0.0.0/0 por defecto, sin22/80/8080/8081/5432/IPv6 público, IMDSv2/hop1 y retención/cifrado de datos, sin secretos en parámetros/UserData; requerir CIDRs solo al activar el modo opcional (APR-01/APR-03/APR-05/APR-07; D-01/D-02/D-06).
- [x] 5.2 Documentar IAM mínimo para host bootstrap/SSM/ECR/SSM SecureString y provisión separada de secretos; fijar certificado Let's Encrypt con Certbot manual DNS-01 en equipo del operador, A a EIP y TXT de validación, instalación de directorio TLS read-only y renovación manual con30días o menos; verificar nginx -t/recarga/serial/caducidad sin reiniciar backend/PG, sin80/cuarto contenedor/permisos DNS en aplicaciones ni secretos/certificados reales versionados (APR-02/APR-07; D-02/D-05).
- [x] 5.3 Documentar actualización de una sola réplica, cierre de entrada, espera de activos, stop/start, backup/restauración autorizados y retención del volumen/clave AES; verificar procedimiento sin scale>1, limpieza automática o migración inversa improvisada (APR-05/APR-06/APR-10; D-04/D-07).
- [x] 5.4 Revisar los artefactos/configuración y ejecutar strict del change; contrastar evidencia local F1–F4 y separar findings de preparación resueltos de verificaciones AWS pendientes, sin llamar validado al despliegue (APR-12).

## 6. Publicación y aceptación académica — autorización independiente obligatoria

- [x] 6.1 Obtener autorización de despliegue/coste y valores reales de cuenta/región/dominio/certificado/AMI/digests, credenciales Basic Auth de prueba y canal de entrega al tribunal; verificar control A/TXT DNS, equipo Certbot/responsable de renovación y acceso desde IP no registrada, registrar coste relativo/ventana sin inventar importes. No exigir CIDRs salvo activación explícita de la restricción opcional (APR-02/APR-07/APR-12; D-10).
- [x] 6.2 Solo tras esa autorización, provisionar recursos inicialmente cerrados, publicar imágenes privadas, suministrar secretos/certificado, arrancar BD limpia/backend pro único y verificar Flyway/readiness antes de abrir ingress protegido; conservar evidencia no secreta y no iniciar Scan/OpenAI (APR-01..APR-09; D-09).
- [ ] 6.3 Verificar F1–F3 con443 público desde IP no registrada: Basic Auth válido permite UI/API, ausente/incorrecto rechaza, certificado/hostname/caducidad correctos; comprobar22/8080/5432 inaccesibles, HTTP/buffering/timeouts vigentes, PG privado y restore aislado autorizado. Verificar bloqueo por CIDRs solo si se activa modo opcional; registrar evidencia sin secretos ni desactivar autenticación (APR-02..APR-06/APR-12).

  Restore pendiente de6.3: procedimiento mínimo y comandos propuestos en [D-11](design.md#d-11-validación-pendiente-63-backup-lógico-y-restore-aislado). Usar scanner_app/pg_dump READ ONLY, imagen PG16 local por ID y un contenedor sin red/puertos con volumen tmpfs propio; no reiniciar ni escribir/restaurar producción. Exigir Flyway/schema/catálogo/proyecto equivalentes, integridad, disponibilidad/identidad productivas preservadas y cleanup con guardas; evidencia solo no secreta. UUID del proyecto existente lo aporta el operador porque no consta en la evidencia consolidada. Diseño listo para implementación, **sin ejecución**: no cambia la casilla, el estado PARTIAL ni el recuento23/25; no reabre las verificaciones F1–F3 ya aceptadas ni6.1. No se ejecutan comandos de BD/Docker/AWS, backup/restore/cleanup ni pruebas/validadores en esta tarea de diseño.
- [x] 6.4 Confirmar acceso académico y reproducción F4, actualizar únicamente README/arquitectura/ADR operativa y checklist/enlaces afectados con URL/estado/evidencia reales; declarar demo/evaluator demostrados localmente y no en AWS, manteniendo investigación/entregables no acreditados pendientes (APR-08/APR-11/APR-12; D-06/D-08/D-10).

Aceptación del review: 6.4 completa en preparación técnica. La entrega efectiva de credenciales al tribunal permanece administrativa; no reabre esta validación técnica. 6.1 completada tras confirmar Certbot en Docker + DNS-01 y decisiones operativas; 6.3 conserva exclusivamente restore aislado pendiente. La matriz enlazada al inicio prevalece sobre las valoraciones históricas siguientes.

## Evidencia de ejecución — 2026-10-20

- **20/25 tareas completas.** 1.1 parcial: herramientas, arquitectura e imágenes registradas por digest en `docker/production/images.json`; durante la validación local original no se había seleccionado ni consultado una AMI real. La selección posterior comunicada por el operador se registra abajo; su identificador exacto sigue sin constar/verificarse en esta evidencia.
- F1–F4: preparación comprobada localmente con tres servicios, TLS sintético, Basic Auth, API y SPA, límites HTTP, aislamiento de puertos, PostgreSQL nuevo, Flyway V1–V7 y persistencia. `verify.py`: **51 PASS**, incluidos rechazo antes de upstream, checksum inválido y recarga TLS sin reiniciar backend/PG.
- Build backend JDK 21/Maven Wrapper 3.9.16 correcto, también desde copia nueva que solo contenía fuentes/pom/wrapper. Build Angular production con Node 22/npm 10.8.2 en etapa Docker limpia, sin `node_modules` heredado. No equivale a un clone de un commit nuevo: se validaron las fuentes del working tree, sin realizar commit.
- Tests frontend: **161 PASS/10 archivos**, sin red en el contenedor de tests. Regresión backend relevante: **35 PASS** (`TargetDestinationPolicyTest`, `TargetConfigurationTest`), sin nuevos endpoints ni llamadas externas.
- CloudFormation: parser YAML y siete invariantes estructurales PASS; validación del servicio AWS **NOT_EXECUTED**. Bootstrap SSM/KMS preparado; no ejecutado contra AWS. Renovación probada con certificado local sintético, no con Let's Encrypt real.
- `openspec validate aws-production-readiness --strict`: **PASS**. No se archiva el change ni se acredita despliegue/URL pública/entrega final.
- Cierre: contenedores y red de prueba retirados, archivos secretos sintéticos eliminados y volumen externo `apr-data-cd1fe11a70` conservado. PostgreSQL de desarrollo preexistente no modificado; su puerto host 5432 no pertenece al Compose de producción.

El calendario orientativo del diseño no autoriza operaciones AWS. Se mantienen pendientes 6.1–6.4 y la comprobación de AMI de 1.1; no se reducen F1–F4 ni SSRF para cumplir fechas.

## Corrección de findings de revisión F1/F2 — 2026-10-20

Estos identificadores corresponden a los findings bootstrap/wrapper de la revisión, no a los controles históricos de acceso/body con los mismos nombres.

- **F1 CLOSED:** retirados parámetros/Outputs BackendImageDigest e IngressImageDigest. CloudFormation crea infraestructura/repositorios vacíos y conserva sus URI Outputs; no configura ni arranca Compose. La guía documenta creación → Outputs → build/push → consulta digest → export BACKEND_IMAGE/INGRESS_IMAGE en el host → pull/up con los prerequisitos externos. IAM sin ampliar: pull limitado a los dos repositorios creados.
- **F2 CLOSED:** README raíz, guía de bootstrap y comandos del diseño usan `sh ./mvnw` para Linux/macOS. Wrappers permanecen 100644; Maven/dependencias intactos.
- `python docker/production/tests/check_infrastructure.py`: **14 grupos PASS**. Comprueba estructura CFN, creación sin digests, URI Outputs, ámbito IAM, inyección posterior de digests sintéticos en Compose, rechazo si falta cualquiera de las imágenes, documentación POSIX y modos Git. Solo `compose config`: no crea contenedores, volúmenes, imágenes ni recursos AWS. El primer intento quedó bloqueado por permisos del directorio temporal del sandbox; ejecución autorizada fuera del sandbox PASS.
- Búsqueda documental dirigida: todas las invocaciones POSIX encontradas en instrucciones vigentes de checkout/build llevan `sh`. `openspec validate aws-production-readiness --strict`: **PASS**.
- No se repitieron builds, Maven, tests de producto ni suite HTTP/BD: los cambios afectan únicamente a plantilla, documentación y validación offline. La evidencia previa de 51 comprobaciones locales se conserva como histórica; no se presenta como reejecutada.
- Permanecen **20/25 tareas completas**; 1.1 parcial y 6.1–6.4 pendientes, sin acreditar despliegue, URL pública o entrega final.

## Selección real de instancia comunicada por el operador

- Primer intento de creación CloudFormation en `eu-north-1` con `t3.medium`: rollback porque ese tipo no era Free Tier eligible en esta cuenta. Fuente: información del operador; no se consultaron eventos AWS en esta actualización.
- Consulta real de elegibilidad comunicada: `c7i-flex.large`, `t4g.small`, `t3.micro`, `t4g.micro`, `m7i-flex.large`, `t3.small`.
- Decisión final para este despliegue: **m7i-flex.large** con **Amazon Linux 2023 x86_64**. Verificada la edición manual del operador en `aws.yml`: una única definición de InstanceType, default `m7i-flex.large`, AllowedValues exactamente `[m7i-flex.large, c7i-flex.large, t3.small, t3.micro]`. No se modifica la plantilla ni la AMI en esta actualización.
- Se excluye `t4g.*` porque requiere ARM. La elegibilidad Free Tier depende de cuenta/región y debe verificarse antes del despliegue; no implica gratuidad total ni confirma un stack/aplicación operativo.
- Prueba afectada `python docker/production/tests/check_infrastructure.py`: **16 grupos PASS** (14 anteriores y comprobaciones de default/binding y lista x86_64 sin ARM). Incluye validación estructural CFN y bootstrap Compose offline. `openspec validate aws-production-readiness --strict`: **PASS**.
- Actualizados guía de producción, diseño y evidencia. Sin AWS real, despliegues, cambios de arquitectura/producto/AMI, commits ni pushes por este agente. No se reejecutan ni se atribuyen a esta actualización los builds/tests HTTP/BD históricos.
- **20/25 tareas completas** sin cambios: 1.1 parcial y 6.1–6.4 pendientes. El intento fallido comunicado no acredita aprovisionamiento completo, aceptación pública o entrega final.

## Consolidación del despliegue real comunicado — 2026-10-20

El estado actual y la procedencia se registran en [evidencia AWS](evidence/2026-10-20-aws-deployment.md); las declaraciones anteriores de ausencia de despliegue corresponden a sus sesiones históricas. Segundo stack `CREATE_COMPLETE`, una EC2 m7i-flex.large/AL2023 x86_64, ECR privado por digest, PostgreSQL16 sobre EBS30GB/XFS/volumen externo, secretos SecureString→tmpfs, HTTPS Let's Encrypt/Basic Auth y persistencia de proyecto desde Internet. Incidencias resueltas: rollback t3.medium por elegibilidad y permisos TLS de ingress para UID101. Sin Scan/demo/OpenAI en AWS.

Revisadas 1.1 y 6.1–6.4 sin cerrar ninguna: faltan AMI/digests exactos (1.1); autorización/coste/ventana y entrega académica/renovación (6.1); evidencia Flyway/readiness/configuración efectiva y secuencia de apertura (6.2); casos externos completos/restore (6.3); acceso académico y reproducción F4 acreditados (6.4). Detalle por tarea en la evidencia enlazada. **20/25 completas**, sin cambiar requisitos ni casillas por observaciones parciales.

Actualizados README raíz, guía de producción, arquitectura/ADR-009 y documentos de entrega directamente afectados. URL publicada con requisito de credenciales de prueba por canal privado; ningún secreto en documentación. Solo se ejecuta OpenSpec strict como validación de esta consolidación; no se reejecutan tests ni operaciones de despliegue.

`openspec validate aws-production-readiness --strict` → `Change 'aws-production-readiness' is valid`, exit0. Estado: **READY FOR REVIEW** documental; aceptación completa pendiente según la matriz de evidencia, sin archivar el change.

## Review final — estado vigente

El operador comunica aprobación de 1.1 (AMI real/herramientas/instancia/imágenes por digest), 6.2 (provisión y servicios/HTTPS/persistencia básica) y 6.4 (preparación técnica, acceso externo y documentación reproducible). Se completan sus casillas: **23/25 completas**. Las valoraciones 20/25 anteriores se conservan como históricas y quedan sustituidas por esta aceptación.

6.1 PARTIAL únicamente por confirmar el equipo con Certbot disponible para renovación manual (requisito expreso de la tarea y D-02). Se documentan despliegue académico autorizado, cuenta Free Plan/créditos sin gasto exacto medido, eu-north-1, ventana hasta evaluación/entrega, operador del proyecto como responsable y canal privado exclusivo para credenciales académicas. 6.3 PARTIAL exclusivamente por **restore aislado pendiente**; refresh/persistencia/reinicio simple no constituyen restore. Entrega administrativa al tribunal no acreditada, sin tratarla como validación técnica pendiente de 6.4.

Documentos consolidados actualizados con este alcance. Identificadores operativos innecesarios omitidos; procedimientos con placeholders, dominio público conservado. Sin código, AWS, despliegues, Scans, tests de producto, commit/push ni cierre del change. Única validación de esta revisión: OpenSpec strict, resultado registrado a continuación.

`openspec validate aws-production-readiness --strict` → `Change 'aws-production-readiness' is valid`, exit0. Recuento documental: **23 casillas completas de 25**. Revisión dirigida sin Account ID, Instance ID, IP pública directa o URI ECR con cuenta real en documentos revisados; solo ejemplos genéricos/loopback y placeholders. **READY FOR REVIEW**.

## Cierre exclusivo de 6.1 — confirmación del operador

**6.1 COMPLETED; 24/25 tareas completas.** El operador confirma despliegue autorizado para entrega académica del TFM, permanencia en AWS Free Plan/créditos sin migración voluntaria autorizada a Paid Plan y sin declarar gasto monetario exacto no medido. Ventana: mantener https://llmscannerseguridad.com hasta finalizar entrega/evaluación; después podrán detenerse o retirarse recursos innecesarios. Responsable operativo/TLS: operador del proyecto. Renovación manual mediante Certbot en Docker + DNS-01, procedimiento ya validado. Credenciales Basic Auth exclusivamente por el canal/campo privado de la entrega académica, nunca GitHub/docs públicas; entrega efectiva todavía no acreditada.

Esta confirmación resuelve la disponibilidad del equipo/procedimiento TLS, único pendiente de 6.1 tras el review. Con los parámetros y controles previamente aceptados se satisface íntegramente su contrato. La [matriz vigente](evidence/2026-10-20-aws-deployment.md) y referencias consolidadas quedan actualizadas; las valoraciones anteriores 23/25 son históricas. El diseño de restore D-11 no se ejecuta ni modifica: solo 6.3 permanece PARTIAL por restore aislado pendiente. Sin cambios de código, AWS, despliegues, Scans, commit/push ni cierre del change.

Validación de este cierre: `openspec validate aws-production-readiness --strict` → `Change 'aws-production-readiness' is valid`, exit0. Recuento confirmado: **24 casillas completas de 25**. **READY FOR REVIEW**.

## Ejecución solicitada de 6.3 / D-11 — bloqueo de acceso

El operador autoriza exclusivamente el restore aislado y aporta el UUID del proyecto
testigo. **BLOCKED antes del preflight remoto**: ejecutor local sin AWS CLI/Session Manager
Plugin en PATH y sin sesión administrativa accesible en el inventario de navegador.
Se solicita la vía de acceso, sin leer credenciales ni modificar producción.

Backup/restore/comparaciones/monitor remoto **NOT_RUN**; no se crearon recursos temporales
ni se requiere cleanup de esta ejecución. No se acredita disponibilidad o identidad
productiva sin medición. **6.3 PARTIAL; 24/25 tareas completas**, sin cambiar su casilla.
[Evidencia de bloqueo y reanudación](evidence/2026-10-20-restore-validation.md).

`openspec validate aws-production-readiness --strict`: **PASS**, exit 0. Sin cambios de
código/diseño/despliegue, reinicios, Scans, OpenAI, commit/push. No se cierra el change.

## Decisión vigente del operador — restore 6.3 pospuesto

Se cancela la ejecución ahora solicitada de D-11: el restore lógico aislado queda
explícitamente **PENDIENTE / NO EJECUTADO**. **6.3 PARTIAL; 24/25 tareas completas**,
sin marcar su casilla ni dar por demostrada la recuperabilidad.

Motivos comunicados por el operador: producción AWS estable, accesible y validada;
ejecutor local sin acceso administrativo necesario; Session Manager no permite ejecutar
cómodamente el procedimiento guiado; esta validación operativa adicional no forma parte
de los requisitos oficiales obligatorios de entrega del TFM y se prioriza evitar riesgos
para producción en esta fase final. Esta decisión no elimina el criterio de restore del
change ni lo considera satisfecho; mantiene su pendiente explícito.

Producción no modificada, ningún backup/restore ejecutado, ningún servicio detenido,
sin recursos temporales creados ni Scans/OpenAI. Solo cambios documentales y
`openspec validate aws-production-readiness --strict`: **PASS**, exit 0.
[Evidencia vigente](evidence/2026-10-20-restore-validation.md).
No reanudar D-11 sin nueva autorización expresa. **READY FOR REVIEW** documental,
sin archivo/cierre del change, código, AWS, deploy, servicios, commit/push.
