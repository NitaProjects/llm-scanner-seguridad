## Purpose

Definir las condiciones mínimas de seguridad y reproducción para desplegar el scanner académico en una única instancia AWS, preservando su core y separando la demostración local autorizada de la exposición cloud.

## ADDED Requirements

### Requirement: APR-01 Alcance académico de una instancia

**Historia:** Como responsable del TFM, quiero un entorno controlado y pequeño que pueda preparar antes del 26 de octubre de 2026.
**Prioridad:** MUST. **Dependencias:** ADR-003; frontend-mvp; backend core; real-evaluator-validation.

La solución SHALL usar una única instancia de aplicación y una única réplica backend, sin workers distribuidos, cola externa, autoscaling ni HA. Esta fase de diseño SHALL modificar solo OpenSpec, sin código, builds/tests de producto, infraestructura, consola AWS o llamadas externas. Preparación implementada, despliegue y aceptación operativa SHALL registrarse por separado; planificar SHALL NOT acreditar cierre de F1–F4 o entrega TFM.

#### Scenario: Entrega del diseño
- **WHEN** se entrega este change
- **THEN** los artefactos están disponibles para implementación y las tareas operativas permanecen pendientes

### Requirement: APR-02 Entrada HTTPS restringida para UI y API F1

**Historia:** Como evaluador autorizado, quiero entrar a la UI y API por un único origen protegido.
**Prioridad:** MUST. **Dependencias:** APR-01; política de autorización.

La única entrada de aplicación SHALL usar HTTPS443 con certificado públicamente confiable para su hostname y TLS 1.2/1.3, terminado en Nginx. El puerto443 SHALL poder ser accesible públicamente sin conocer la IP del tribunal; la autenticación Basic Auth en Nginx SHALL ser obligatoria para UI, recursos estáticos, rutas SPA y todas las rutas /api. La allowlist IP SHALL ser opcional y desactivada por defecto, sin ser precondición de acceso académico. Las credenciales de entrada SHALL poder entregarse como credenciales de prueba del TFM por el canal de entrega previsto, sin publicarlas en el repositorio. SHALL NOT añadir usuarios/roles en la aplicación ni permitir una ruta de health pública sin protección. Acceso usando la IP como hostname o Host desconocido SHALL rechazarse. La falta de certificado/credenciales SHALL bloquear apertura, sin fallback a HTTP o acceso sin autenticación. Backend8080, PostgreSQL5432 y SSH22 SHALL permanecer inaccesibles desde Internet.

#### Scenario: Usuario sin credencial desde Internet
- **WHEN** se solicita la UI, un deep link o /api sin credencial válida
- **THEN** el proxy rechaza antes del backend y no devuelve datos de aplicación

#### Scenario: Tribunal sin IP conocida
- **WHEN** un evaluador con credenciales válidas conecta desde una IP no registrada y la allowlist opcional está desactivada
- **THEN** accede por HTTPS a UI/API sin necesidad de registrar previamente su dirección

#### Scenario: Restricción IP activada opcionalmente
- **WHEN** el operador activa explícitamente la allowlist y un cliente está fuera de los CIDRs declarados
- **THEN** la red bloquea ese acceso sin eliminar Basic Auth para los clientes permitidos

El certificado SHALL emitirse mediante Let's Encrypt/Certbot manual DNS-01 desde un equipo controlado por el operador con capacidad de editar TXT del dominio. El hostname SHALL resolver por A a la EIP antes de publicar; no se requiere puerto80, ACM/ALB ni contenedor de certificados. La renovación SHALL ser manual por el operador, comprobando caducidad semanalmente y antes de evaluación, renovando con30días o menos de vigencia restante. SHALL repetir DNS-01, instalar fullchain/clave por canal administrativo seguro, validar configuración y recargar Nginx sin reiniciar backend/PG. SHALL NOT prometer renovación automática mediante certbot renew sin hooks de autenticación manual. Certificados/cadenas reales, clave privada, clave/cuenta ACME, estado Certbot y credenciales DNS SHALL permanecer fuera del repo, imágenes, logs y evidencia; solo ingress SHALL montar el material TLS necesario en solo lectura.

#### Scenario: Emisión o renovación TLS
- **WHEN** el operador completa DNS-01 y suministra el nuevo certificado/fullchain/clave
- **THEN** Nginx valida configuración y sirve el certificado del hostname tras recarga, conservando Basic Auth y tres contenedores
- **AND** sin certificado vigente confiable no se publica mediante HTTP ni se ignoran errores TLS

### Requirement: APR-03 Routing y ausencia de bypass

**Historia:** Como mantenedor, quiero conservar /api y las rutas Angular sin exponer puertos internos.
**Prioridad:** MUST. **Dependencias:** APR-02; contrato frontend/API.

El proxy SHALL servir el build Angular estático y enrutar /api y /api/* al backend conservando el prefijo, status y Location. Solo las rutas de UI SHALL tener fallback index.html; errores API SHALL NOT convertirse en HTML. Backend y PostgreSQL SHALL carecer de puerto publicado en host/Internet y no usar host networking. La red de BD SHALL conectar solo backend y BD. SHALL NOT existir entrada alternativa vía dominio, IPv6, puerto Docker, SG, health o administración que evite F1/F2. Las operaciones no seguras SHALL rechazar Origin ausente/distinto del origen canónico en el proxy; las credenciales del proxy SHALL NOT reenviarse al backend.

#### Scenario: Petición API inválida
- **WHEN** una petición autorizada alcanza una ruta /api inexistente
- **THEN** conserva la respuesta API real y no recibe index.html

#### Scenario: Petición directa al backend o CSRF
- **WHEN** se intenta acceso externo a 8080/5432 o una escritura desde otro Origin
- **THEN** la red bloquea el acceso directo y el proxy rechaza la escritura ajena sin ejecutarla

### Requirement: APR-04 Límites previos de recepción F2

**Historia:** Como operador, quiero impedir que bodies ilimitados lleguen al backend.
**Prioridad:** MUST. **Dependencias:** APR-02/APR-03; límites funcionales existentes de API.

El punto de entrada SHALL limitar el body completo a 1 MiB y bufferizar completamente antes de reenviar al backend. SHALL cubrir Content-Length, chunked y cualquier método con body, devolviendo 413 al exceder el límite sin invocar upstream. SHALL aplicar timeouts de cabeceras 15 s y de inactividad de body 15 s, límites de conexiones simultáneas y buffering acotado. SHALL rechazar Content-Encoding de request distinto de identity/ausente, sin descomprimir bodies fuera de control. El límite SHALL comprobarse con el mayor DTO válido relevante, manteniendo los contratos funcionales; SHALL NOT cambiar validadores del core para hacer pasar la prueba. Los timeouts de inactividad SHALL NOT presentarse como deadline absoluto contra un emisor que envía periódicamente.

#### Scenario: Body excesivo en chunks
- **WHEN** un cliente autenticado supera 1 MiB mediante chunked
- **THEN** recibe 413 y la aplicación no recibe el body ni inicia procesamiento

#### Scenario: Recepción detenida o codificación comprimida
- **WHEN** el cliente deja de enviar body más de 15 s o declara una codificación comprimida
- **THEN** el proxy termina la recepción por timeout o rechaza con 415, sin reenviar

### Requirement: APR-05 PostgreSQL privado y credenciales propias F3

**Historia:** Como responsable, quiero una BD de producción separada de las credenciales y datos locales.
**Prioridad:** MUST. **Dependencias:** APR-03; modelo de datos; ADR-002.

La instalación SHALL usar PostgreSQL 16 con almacenamiento persistente cifrado y una red privada aislada accesible únicamente desde backend. SHALL NOT publicar 5432 ni reutilizar el Compose local, contraseñas locales, dumps de desarrollo, roles superusuario para la aplicación o secretos en repositorio. La aplicación SHALL usar un rol dedicado sin superuser/createdb/createrole, con los permisos DDL de su esquema necesarios para Flyway. Administración y bootstrap SHALL ser operaciones explícitas separadas. La destrucción del despliegue SHALL NOT borrar automáticamente los datos.

#### Scenario: Acceso desde proxy o Internet
- **WHEN** el proxy de frontend o un cliente externo intenta acceder a la BD
- **THEN** no dispone de conectividad de aplicación a 5432

### Requirement: APR-06 Arranque limpio y migraciones oficiales F3

**Historia:** Como evaluador, quiero poder preparar una BD vacía con el repositorio oficial.
**Prioridad:** MUST. **Dependencias:** APR-05; migraciones V1..V7 vigentes; recovery single-instance.

El procedimiento SHALL crear una BD/esquema vacíos y aplicar Flyway del backend al arrancar la única instancia con profile pro y ddl-auto=validate. SHALL verificar schema history, checksums y catálogo fijo de cuatro SecurityTests antes de declarar listo el backend. SHALL NOT modificar migraciones ejecutadas, hacer baseline para ocultar fallos, usar ddl-auto=create/update ni repetir limpieza de una BD existente. Fallo de migración/recovery SHALL impedir apertura del servicio. Reinicio sobre el mismo volumen SHALL conservar datos y no repetir migraciones ni Scans.

#### Scenario: Primera instalación o checksum incorrecto
- **WHEN** se inicia sobre BD vacía o se detecta una migración inválida
- **THEN** aplica el historial oficial íntegro o falla cerrado, sin servir una instalación parcial

### Requirement: APR-07 Secretos y configuración explícita pro

**Historia:** Como operador, quiero suministrar secretos sin incluirlos en imágenes, frontend o logs.
**Prioridad:** MUST. **Dependencias:** APR-02/APR-05; ADR-002/ADR-004.

La configuración SHALL fijar exclusivamente SPRING_PROFILES_ACTIVE=pro, DB_URL/DB_USERNAME no locales y DB_PASSWORD nueva. Los secretos SHALL provisionarse mediante Parameter Store SecureString/KMS y rol IAM de instancia con permisos limitados, sin claves AWS estáticas. Solo el bootstrap de host y el proceso destinatario SHALL recibir los secretos necesarios; EVALUATOR_API_KEY SHALL estar ausente por defecto en AWS y existir únicamente como variable del proceso backend cuando se autorice su uso. SHALL NOT almacenarse en Compose/.env/imagen/frontend/logs/evidencia ni archivos temporales. TARGET_CREDENTIAL_ENCRYPTION_KEY SHALL conservar formato AES existente, custodia y persistencia independiente del volumen de BD; no se regenera en cada arranque. Configuración/secretos ausentes SHALL fallar cerrado antes de abrir entrada.

#### Scenario: Reemplazo de instancia
- **WHEN** se restaura el volumen y la misma clave custodiada desde su almacén
- **THEN** se conserva capacidad de descifrado sin exponer la clave ni reescribir credenciales cifradas

### Requirement: APR-08 Demo local y SSRF inalterado

**Historia:** Como responsable, quiero demostrar el comportamiento vulnerable sin publicar un target inseguro ni relajar pro.
**Prioridad:** MUST. **Dependencias:** target-configuration/security-test-execution; demo-vulnerable; real-evaluator-validation.

Este entorno AWS SHALL NOT desplegar demo-vulnerable ni abrir 8081. Su demostración y validación real SHALL permanecer locales con profile local y autorización exacta existentes, separadas de la instalación AWS. En pro SHALL permanecer TARGET_LOCAL_DEMO_ENABLED=false, TARGET_LOCAL_DEMO_ORIGINS vacío y TARGET_ALLOWED_ORIGINS vacío por defecto; el bloqueo de IPs privadas/locales/metadata e interfaces SHALL conservarse aun declarando un origen. SHALL NOT usar localhost, IP privada, túnel, proxy o DNS público hacia una IP privada para introducir la demo en pro. La documentación SHALL declarar la ausencia de demo en AWS y no atribuirle evidencia local ni inventar datos importados. Un target público futuro SHALL requerir autorización/configuración específica fuera de esta demostración.

#### Scenario: Intento de usar demo privada en pro
- **WHEN** se configura el destino local/privado de la demo, incluso con flags manipulados
- **THEN** pro conserva el rechazo y no se cambia a local para hacer funcionar AWS

### Requirement: APR-09 Healthchecks sin nueva feature

**Historia:** Como operador, quiero distinguir proxy vivo de backend/BD preparados.
**Prioridad:** MUST. **Dependencias:** APR-02/APR-06; GET /api/projects existente.

El diagnóstico SHALL comprobar frontend estático HTTPS, readiness del backend mediante GET /api/projects con timeout y status 200/JSON descartando el cuerpo, y PostgreSQL mediante pg_isready junto a evidencia Flyway. El backend SHALL comprobarse por red privada y por ruta pública protegida; SHALL NOT usar /actuator/health inexistente, aceptar HTML como health API ni llamar a target/evaluador. Health correcto SHALL NOT acreditar integración de un Scan. Un fallo SHALL dejar entrada de aplicación cerrada o en error de upstream sin fallback estático.

#### Scenario: Proxy responde y backend no
- **WHEN** index.html está disponible pero el backend no responde correctamente
- **THEN** readiness no pasa y /api conserva el fallo en lugar de devolver la SPA

### Requirement: APR-10 Logging mínimo y operación acotada

**Historia:** Como operador, quiero detectar fallos sin registrar secretos ni agotar el disco.
**Prioridad:** MUST. **Dependencias:** APR-04/APR-07; diagnósticos seguros existentes.

El despliegue SHALL preservar logging saneado del core y desactivar wire/body/SQL sensible. Los logs del proxy SHALL limitarse a método, status, duración y status de upstream, sin Authorization, usuario, query, URL completa, bodies, cookies ni secretos. SHALL limitarse rotación/tamaño de logs, espacio temporal del proxy, memoria y CPU de contenedores. SHALL NOT usar Docker privileged, montar docker.sock en aplicaciones o entregar permisos de despliegue al backend. Un único backend SHALL ser invariante en arranque y actualización, sin rolling con solapamiento.

#### Scenario: Actualización de backend
- **WHEN** se reemplaza el artefacto
- **THEN** se cierra entrada, se evita interrumpir Scans activos y no se ejecutan dos réplicas simultáneas

### Requirement: APR-11 README reproducible desde checkout limpio F4

**Historia:** Como evaluador, quiero reproducir instalación, build y arranque sin conocimiento de la sesión original.
**Prioridad:** MUST. **Dependencias:** APR-01..APR-10; README actual.

El README SHALL documentar revisión de checkout, versiones/requisitos y comandos para frontend/backend/demo, PostgreSQL local frente a producción, variables requeridas sin secretos, Flyway/arranque, /api/SPA, perfiles, readiness, permisos y configuración evaluator/manifest. SHALL indicar explícitamente que Spring no carga .env automáticamente y distinguir interpolación Compose de variables efectivamente inyectadas al backend. SHALL permitir reproducción local sin AWS/OpenAI, explicar los gates y bindings reales y separar comandos propuestos de comprobaciones ejecutadas. SHALL NOT requerir ficheros ignorados preexistentes, un jar antiguo, UUID fijo, endpoints de esta sesión o dependencias sin fijación reproducible. Builds y pruebas de reproducción SHALL requerir petición expresa posterior.

#### Scenario: Máquina sin artefactos previos
- **WHEN** un operador sigue el README desde checkout limpio y configura los secretos por la vía indicada
- **THEN** puede construir y arrancar localmente los tres componentes, preparar BD/manifests y distinguir qué pasos no se ejecutan en AWS

### Requirement: APR-12 Evidencia de preparación separada de despliegue

**Historia:** Como responsable del TFM, quiero cerrar findings y requisitos solo con evidencia adecuada.
**Prioridad:** MUST. **Dependencias:** APR-01..APR-11; checklist de entrega.

La implementación SHALL aportar configuración verificable y pruebas autorizadas de F1/F2, BD limpia/reinicio F3 y reproducción F4. El despliegue SHALL requerir autorización adicional de cuenta/región/coste/dominio y entrega de credenciales Basic Auth, sin exigir IP del tribunal; CIDRs solo si se activa la restricción opcional. La evidencia SHALL excluir secretos y distinguir tests locales, inspección de infraestructura y observaciones reales AWS; SHALL NOT marcarse producción desplegada con solo tests locales. README/arquitectura/ADR/checklist/enlaces SHALL actualizarse solo en el alcance afectado y con evidencia, conservando la validación histórica local del evaluator. Esta fase de diseño SHALL ejecutar únicamente OpenSpec strict como validación.

#### Scenario: Preparación sin acceso AWS
- **WHEN** se implementa y verifica localmente pero todavía no se despliega
- **THEN** se puede aceptar la preparación y mantener URL, despliegue y verificaciones externas pendientes
