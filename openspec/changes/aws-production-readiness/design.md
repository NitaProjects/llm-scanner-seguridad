## Context

Motivación: [proposal.md](proposal.md). Contrato: [APR-01..APR-12](specs/aws-production-readiness/spec.md). Inspección dirigida del 2026-10-20, sin red ni servicios:

- Angular 21, environment de producción y desarrollo con apiBaseUrl=/api; el dev proxy no sirve en producción. Angular build usa configuration production y salida browser estática; comprobar ruta real dist/frontend/browser al implementar.
- Java21/Boot4.1.1; Maven Wrapper backend y demo fijan Maven3.9.16. frontend packageManager npm10.8.2, lockfile existente; engines Angular permiten Node22>=22.12.0. No actualizar dependencias/lockfiles.
- application.yml activa local por defecto; pro solo toma DB_URL/DB_USERNAME/DB_PASSWORD. JPA ddl-auto=validate y Flyway enabled=true. Executor1/1/queue10 por defecto. No Actuator ni endpoint /health del scanner. GET /api/projects ya existe y consulta persistencia sin writes ni proveedor.
- Migraciones V1..V7 oficiales y recovery single-instance de ADR-003. No Compose de producción, imágenes frontend/backend ni límites de entrada de proxy definidos actualmente. docker/docker-compose.yml se considera solo local; no se abre su material de credenciales ni archivos .env.
- LocalTargetUrlPolicy y TargetDestinationPolicy construyen la excepción privada únicamente si está activo local y su flag/origen exactos. Allowlist pública no evita los bloqueos de IP del transporte. pro no puede apuntar sin cambios a demo privada, aunque el host sea propio.
- ADR-002 deja provisión/custodia AWS pendiente; su AES-GCM usa TARGET_CREDENTIAL_ENCRYPTION_KEY en Base64 que decodifica exactamente32bytes. evaluation config conserva variables/gates de ADR-004 y cuatro llamadas/Scan. La validación real posterior al fix TLS consta en real-evaluator-validation, tasks/evidence: cuatro componentes SUCCEEDED; no se repite aquí.
- README raíz conserva instalación/ejecución incompletas; frontend y demo tienen procedimientos parciales. docs/08-entrega-tfm/checklist-entrega.md revisada: despliegue y guía reproducible siguen pendientes. Este diseño no cambia sus estados ni declara entrega lista.

## Goals / Non-Goals

**Goals:** resolver F1–F4 en el borde/configuración/documentación, con una sola máquina, sin modificar dominio, casos de uso, API o políticas SSRF. Preparación ejecutable y auditable antes del 26-10-2026; despliegue futuro con autorización separada.

**Non-Goals:** proposal. No nuevas dependencias Java/Angular, nuevas migraciones, endpoint health, servicio de usuarios, UI de login, importador de resultados ni cambios visuales. Las decisiones siguientes son la propuesta implementable de este change; su adopción no autoriza despliegue ni cierra ADRs pendientes fuera de este entorno.

## Decisions

### D-01 Topología mínima y servicios concretos

Una EC2 Linux x86_64, una AZ, VPC/subred pública propia con Internet Gateway, una Elastic IP y Security Group que publica únicamente443; restricción IP opcional, desactivada por defecto. Selección final para el despliegue real en eu-north-1: m7i-flex.large, manteniendo Amazon Linux 2023 x86_64 y builds fuera de EC2. Según el operador, el primer intento con t3.medium terminó en rollback por no ser Free Tier eligible en esta cuenta. Su consulta en eu-north-1 devolvió c7i-flex.large, t4g.small, t3.micro, t4g.micro, m7i-flex.large y t3.small. La plantilla permite exactamente m7i-flex.large (default), c7i-flex.large, t3.small y t3.micro; excluye t4g.* por requerir ARM. Esta actualización no consulta AWS, no cambia AMI/arquitectura ni acredita despliegue completado. Confirmar disponibilidad/cuota y elegibilidad por cuenta/región antes de aprovisionar; no se promete gratuidad. Almacenamiento gp3 cifrado, referencia30GiB, con volumen de datos separado/conservado y margen monitorizado. No despliegue blue/green ni réplica adicional.

Servicios de aplicación mediante Docker Engine/Compose v2 en esa EC2, archivo NUEVO docker/production/compose.yml, sin extender/copiar valores secretos del Compose local:

| Servicio | Función | Exposición |
| --- | --- | --- |
| ingress | Nginx + build Angular estático, TLS, Basic Auth, límites y proxy /api | Solo host443 |
| backend | JRE21 y jar Spring Boot, profile pro, réplica1 | 8080 exclusivamente en red app privada, sin ports |
| postgres | PostgreSQL16, rol/schema propios y volumen persistente | 5432 exclusivamente en red data privada, sin ports |

Servicios AWS adicionales mínimos: ECR privado (imágenes ingress/backend por digest), Systems Manager Session Manager para administración sin puerto SSH, Parameter Store SecureString y KMS administrado para secretos pequeños, IAM instance role de mínimo privilegio, Route53 para A hacia EIP si se usa una zona existente bajo control del operador. Certificado TLS en Nginx, no ALB/ACM: no se supone que un certificado ACM administrado sea exportable ni que termine TLS en EC2 automáticamente. Si DNS se gestiona fuera de Route53 se documenta la zona real sin cambiar la topología.

Una plantilla CloudFormation pequeña posterior en docker/production/aws.yml describe VPC/subred/ruta/SG/EIP/IAM/EC2 y volúmenes/repos ECR; solo recibe parámetros no secretos y nombres de parámetros seguros, no secretos ni UserData que los exponga. Operador configura secretos y DNS/certificado por separado antes de abrir el servicio. Sin pipeline CI/CD obligatorio, Terraform, NAT Gateway, balanceador, S3 website ni CDN. Preparar manifiesto de versiones/digests/AMI y comandos de provisión reproducibles, no una plataforma de infraestructura genérica.

```text
Tribunal desde cualquier IP (sin registro previo)
  -> HTTPS443 público / hostname canónico
  -> EC2 única / Security Group / EIP
     -> ingress: Nginx termina TLS + Basic Auth obligatorio + Angular dist
        | /projects/... -> estático + fallback SPA, autenticado
        | /api/...      -> backend:8080, autenticado + límites
        |                  -> postgres:5432 (red data aislada)
        |                  -> HTTPS target público autorizado / OpenAI
        |                     solo si se configura/autoriza posteriormente
     -> EBS cifrado: datos PG, configuración TLS fuera del repo
     -> host bootstrap <- IAM -> SSM SecureString/KMS + ECR
Administración -> Session Manager (sin22 entrante)
Operador -> Certbot/Let's Encrypt DNS-01 en su equipo
         -> instalación TLS fuera del repo + recarga de Nginx
Allowlist IP opcional: desactivada por defecto, no requisito del tribunal

LOCAL APARTE: Angular -> scanner local -> demo VULNERABLE
                                      -> evaluator real autorizado
             evidencia local existente / reproducción opt-in
```

Alternativas: RDS privado simplifica backup/gestión de BD pero añade instancia y coste; ALB+Cognito o CloudFront+S3 separan hosting/auth pero añaden servicios y configuración para /api y acceso privado. No son necesarios para este entorno corto. EC2 con servicios todos expuestos o nginx dev-server no satisface F1/F2/F3. Se elige EC2 + redes aisladas por seguridad suficiente, sencillez y coste relativo bajo; se acepta el punto único de fallo.

### D-02 Red, frontera de confianza y acceso F1

Entrada SG TCP443 pública por IPv4 (0.0.0.0/0) para no depender de conocer la IP del tribunal. Basic Auth obligatorio protege los recursos; puerto público no significa contenido público. Allowlist IP opcional mediante activación explícita, desactivada por defecto y sin lista obligatoria de evaluación. Sin IPv6 inicialmente ni registro AAAA; si se habilita requiere TLS/auth y cierre de puertos equivalentes, con443 público o restringido según el modo opcional elegido. Cerrar22/80/8080/8081/5432; administración SSM por salidaHTTPS. No publicar backend/BD en ports, incluso estando el SG correctamente cerrado. Redes: ingress adjunto a red edge de salida y red app internal; backend adjunto a app, data internal y una red de salida para DNS/HTTPS de targets/proveedor; postgres únicamente data internal. Ingress no pertenece a data y no comparte redes de salida con backend. Solo backend/PG tienen conectividad de aplicación a la BD; el administrador root del host sigue siendo de confianza y puede realizar mantenimiento autorizado.

Nginx exige HTTP Basic Auth a todas las rutas del único hostname, incluyendo assets, SPA, /api y health observable por entrada. Credencial técnica fuerte/hash compatible con Nginx custodiados fuera del repo. Usuario/contraseña se pueden entregar como credenciales de prueba del TFM mediante el canal de entrega reservado, sin versionarlas ni incluirlas en README público, logs o evidencia. No cuenta de la aplicación, autorización por Project ni promesa de aislamiento multiusuario. TLS1.2/1.3 obligatorio, certificado firmado confiable para hostname; acceso usando la IP como hostname/Host distinto se rechaza, lo que no equivale a filtrar la IP de origen. Cabeceras de cache de API no-store y no almacenar respuestas autenticadas en caches compartidas. No exención pública /healthz. No se exponen archivos de configuración/dotfiles/manifests ni listado de directorios.

Origin canónico explícito. En métodos de escritura exigir Origin igual al origen HTTPS; ausente o distinto devuelve403. Las herramientas REST autorizadas deben enviarlo explícitamente. Reduce CSRF con Basic Auth cacheado por navegador sin añadir sesión, JWT o roles. Nginx elimina Authorization de entrada antes de proxy_pass; las credenciales de target en JSON conservan su contrato y OpenAI usa su propia clave solo en backend. Sobrescribir Host/forwarded headers con valores controlados; no confiar en X-Forwarded-For recibido del cliente. CORS_ALLOWED_ORIGINS se fija al origen canónico, sin wildcard ni origen dev; CORS no se usa como autenticación.

**TLS decidido:** HTTPS termina en Nginx del contenedor ingress en EC2:443, con TLS1.2/1.3. El enlace Nginx→backend permanece HTTP en red app privada de la misma instancia, sin publicar8080. No ALB/ACM, certificado autofirmado de producción ni cuarto contenedor.

**Dominio y emisión:** el operador prepara un dominio/subdominio propio y capacidad de editar TXT de _acme-challenge del hostname. Publicar registro A hacia la EIP, sin AAAA mientras no haya IPv6; no es obligatorio usar Route53 ni conocer IPs del tribunal. Emitir certificado públicamente confiable con Let's Encrypt usando Certbot manual, challenge DNS-01, en un equipo seguro del operador. El operador crea el TXT solicitado y comprueba su propagación antes de confirmar la validación. DNS-01 evita abrir80 y no necesita excepción de Basic Auth o ruta pública de challenge en Nginx. Las credenciales DNS no se entregan a EC2/aplicaciones.

**Instalación:** transferir fullchain.pem/privkey.pem mediante el canal administrativo seguro de SSM, sin pegarlos en comandos/logs/UserData ni almacenarlos en repo o imágenes. Guardar en directorio protegido fuera del checkout, sobre EBS cifrado, con clave privada legible solo por el proceso privilegiado necesario. Montar el directorio TLS de solo lectura únicamente en ingress, no archivos individuales que puedan quedar vinculados al inode anterior tras reemplazo. Verificar configuración Nginx, cadena/hostname/caducidad y acceso HTTPS antes de publicar la aplicación.

**Renovación:** manual y responsabilidad del operador para este entorno académico, sin cron/daemon adicional. Comprobar vigencia semanalmente y antes de cada evaluación; con30días o menos restantes repetir emisión/renovación Certbot manual DNS-01 para el mismo hostname y nombre de certificado, instalando el nuevo par/fullchain en un directorio versionado protegido. Cambiar atómicamente el directorio/enlace activo dentro del montaje, ejecutar nginx -t y recargar Nginx; validar por HTTPS el certificado servido y Basic Auth sin reiniciar backend/PG. Conservar el par anterior protegido hasta confirmar la nueva configuración para poder revertir una instalación fallida. Certbot con challenge manual no ofrece renovación desatendida mediante certbot renew sin hooks; no se promete automatización inexistente. No asumir una duración fija del certificado: se usa su caducidad real. Un certificado que expirará durante la ventana de evaluación se renueva antes de entregar la URL.

**Material excluido de versionado:** certificados/cadenas reales, clave privada TLS, clave/cuenta ACME y directorio de estado de Certbot, tokens/credenciales DNS y contraseñas/hashes reales Basic Auth. También quedan fuera de imágenes y evidencia; los ficheros TLS de ejemplo solo muestran rutas, no contenido real. Falta de dominio, control DNS, certificado válido o Basic Auth bloquea publicación; falta de CIDRs no. No degradar a HTTP ni desactivar verificación del navegador para cumplir fecha. Se puede fijar HSTS sin includeSubDomains/preload tras verificar el dominio.

### D-03 Límite de cuerpos y tiempos F2

Nginx aplica globalmente client_max_body_size=1m (1048576bytes), client_header_timeout=15s y client_body_timeout=15s; proxy_request_buffering=on en toda ruta hacia backend. No reenviar body parcial: Content-Length excesivo se rechaza antes y chunked se acumula/limita antes de upstream. Rechazar Content-Encoding de request distinto de identity/ausente con415 para evitar payload comprimido no contemplado. No añadir un filtro de Jackson o refactor de controllers. Conservar formatos/body vacío del POST Scan; comprobar con el mayor TargetConfiguration válido serializado para no introducir un límite funcional accidental.

client_body_buffer_size=16k con temporal acotado por volumen tmpfs64MiB, límite de conexiones8 por IP y32 globales; Nginx limit_conn se configura sin permitir que un encabezado cliente cambie la clave IP. HTTP/2 se omite inicialmente para reducir variaciones; keepalive_timeout=15s, send_timeout=30s. proxy_connect_timeout=5s, proxy_send_timeout/proxy_read_timeout=30s; el Scan es asíncrono, no mantener POST abierto300s ni aumentar timeouts para el evaluator. proxy_next_upstream=off y ninguna repetición automática de escrituras. Límites de cabecera explícitos de16KiB, sin alterar validación de headers target de JSON.

Los timeouts de Nginx son de inactividad entre lecturas, no de duración absoluta. Un emisor periódico puede durar más; autenticación, tamaño máximo, conexiones y memoria acotadas limitan el riesgo académico, sin presumir protección de una allowlist IP que por defecto no existe. No afirmar protección absoluta frente a slowloris ni añadir una capa de servidor nueva para conseguir un deadline total no exigido aquí. Tests de inactividad y de no-forwarding forman parte de aceptación. Si el revisor requiere deadline total posteriormente, debe revisarse este contrato, no simularlo con client_body_timeout.

Rutas /api y /api/ tienen configuración común sin location alternativa menos restrictiva; conservar prefijo completo en proxy_pass. Solo UI usa try_files fallback; API desconocida devuelve su error real. No publicar puerto8080: cerrar el SG sin eliminar ports no basta como única defensa contra bypass accidental futuro.

### D-04 PostgreSQL y Flyway F3

PostgreSQL16 en contenedor propio, volumen en EBS cifrado, sin publicación de puertos y pg_hba restrictivo a red data/BD/rol dedicados, autenticación SCRAM sin trust. BD de referencia llm_scanner_seguridad_pro; no copiar desarrollo ni usar usuario/contraseña de docker/docker-compose.yml. Rol scanner_app no superuser/createdb/createrole, propietario de su BD/esquema de aplicación para DDL Flyway; bootstrap administrativo separado con credencial nueva, no entregada al backend. El proxy no recibe DB_PASSWORD ni acceso data. No Docker privileged ni host networking; contenedores de aplicación sin docker.sock, capabilities innecesarias eliminadas y runtime sin root donde sea viable para el proxy443.

Primera preparación explícita: crear BD vacía y rol/schema, arrancar únicamente backend con pro y variables DB requeridas. Flyway enabled existente ejecuta V1..V7 (o historial oficial vigente al implementar) y ddl-auto=validate comprueba mapping; verificar flyway_schema_history éxito/checksum/orden y cuatro SecurityTests del catálogo, sin baseline-on-migrate ni seed duplicado. No modificar SQL histórico. No ejecutar Flyway durante diseño ni un job automático adicional en paralelo al startup. Entrada permanece cerrada hasta readiness/recovery correctos.

Reinicio mantiene volumen, no limpia ni reenvía Scans; backup antes de cambios que afecten datos. Snapshot cifrado coherente del volumen con PG detenido de forma controlada o pg_dump autorizado protegido fuera de repo; verificar un restore a entorno aislado antes de llamar recuperable a la instalación. No HA, RDS, réplica ni restauración continua. CloudFormation retiene volumen/snapshot en borrado; no down -v ni eliminación automática. La clave AES de targets se respalda/custodia aparte: restaurar solo PG no basta para descifrar. Pérdida de instancia implica indisponibilidad aceptada, no pérdida de datos prometida sin prueba.

### D-05 Secretos, variables y pro

Elegir Parameter Store SecureString estándar con cifrado KMS administrado para secretos pequeños y pocos operadores; IAM instance role limitado a prefijo de este entorno y ECR pull/SSM necesarios, sin claves AWS estáticas ni rol de despliegue en backend. Esto concreta solo la provisión académica pendiente de ADR-002; actualizarla tras aprobación con referencia al change, sin cambiar AES del core. Secrets Manager aporta rotación administrada pero no hace falta para este plazo. KMS cifra custodia, no reemplaza la AES ya implementada.

Bootstrap de host obtiene parámetros sin echo/set-x, shell history ni argumentos con valores, escribe solo contraseñas de BD/clave AES/hash de acceso en tmpfs root-restringido y monta únicamente los ficheros necesarios en cada contenedor. Entrypoint backend mínimo lee sus ficheros con permisos de solo lectura y exporta DB_PASSWORD/TARGET_CREDENTIAL_ENCRYPTION_KEY al proceso Java. Así Spring recibe entorno real, sin dependencias AWS SDK en el core y sin valores en Compose/environment de creación/imagen/docker build args. EVALUATOR_API_KEY no se provisiona en esta instalación AWS ni se escribe en archivos temporales; la reproducción local autorizada la suministra directamente por entorno seguro como hasta ahora. Una activación AWS futura requeriría operación separada manteniendo esa restricción, sin inferir autorización de este bootstrap. No prometer invisibilidad frente a root/proc: host y procesos privilegiados son de confianza. Los ficheros temporales no van a EBS, repo, dumps de entorno o logs. PostgreSQL usa POSTGRES_PASSWORD_FILE para bootstrap, sin reutilizar ese secreto como credencial app. TLS privado queda en archivo protegido de almacenamiento cifrado fuera de repo, montado solo a ingress; htpasswd hash y credenciales de entrega separadas.

| Configuración no secreta | Selección de producción |
| --- | --- |
| SPRING_PROFILES_ACTIVE | pro exclusivamente; no local, ni pro,local |
| SERVER_PORT | 8080 solo red app; sin ports de host |
| DB_URL | jdbc:postgresql://postgres:5432/llm_scanner_seguridad_pro; enlace interno aislado mismo host, sin exposición WAN |
| DB_USERNAME | scanner_app; DB_PASSWORD solo desde secreto nuevo |
| CORS_ALLOWED_ORIGINS | https://hostname canónico |
| TARGET_ALLOWED_ORIGINS | vacío por defecto; ningún target AWS público se autoriza automáticamente |
| TARGET_LOCAL_DEMO_ENABLED / ORIGINS | false / vacío; no excepción privada en pro |
| SCAN_EXECUTOR_CORE_POOL_SIZE / MAX_POOL_SIZE / QUEUE_CAPACITY | 1 / 1 / 10; una sola instancia y cola local existente |
| EVALUATOR_PROVIDER / MODEL | OPENAI / gpt-6-luna actual configurable; no cambiar modelo |
| EVALUATOR_EXPORT_ENABLED / ALLOWED_SCENARIOS | false / vacío por defecto en AWS sin demo |
| EVALUATION_SCENARIO_FILE | omitido en AWS sin escenario autorizado; nunca plantilla con placeholders como instancia válida |
| EVALUATOR_MAX_CALLS / TIMEOUT_MS / INTERVAL_MS / MAX_OUTPUT_TOKENS | 4 / 20000 / 1000 / 1024, sin ampliación |
| TEST_EXECUTION_SCAN_BUDGET_MS | 300000, sin ampliación |
| HOSTNAME / AWS_REGION / IMAGE_DIGESTS | parámetros operativos no secretos obligatorios del despliegue, no nuevas variables Spring |
| IP_ALLOWLIST_ENABLED / REVIEWER_CIDRS | parámetros de infraestructura opcionales: false / vacío por defecto, sin exigir IP del tribunal |

Secretos requeridos: contraseña app BD, bootstrap administrativo separado, TARGET_CREDENTIAL_ENCRYPTION_KEY Base64 de32bytes para soportar target auth existente, credencial/hash de entrada y clave TLS. EVALUATOR_API_KEY es opcional y **no se provisiona en AWS por defecto** al no operar demo/proveedor allí. No generar AES en cada arranque ni rotarla sin plan de descifrado; no compartirla con PostgreSQL/ingress/frontend.

Una futura operación real contra target PÚBLICO autorizado requiere petición específica y configuración existente: origen público autorizado, manifest con project/target/updatedAt exactos y corpus sintético, exportPolicy true/OPENAI, gate global y allowlist, clave solo entorno y política de coste aprobada. No encender esos gates al arrancar el entorno académico vacío. La allowlist de destinos no garantiza exportabilidad de evidencia; mantener ambos controles separados. Si se activa gate monetario, conservar las variables EVALUATOR_MAX_ESTIMATED_COST_USD/ESTIMATED_MAX_CALL_COST_USD y perfil EVALUATOR_PRICING_* más EVALUATOR_ESTIMATE_VERSION existentes; no inventar importes ni consultar tarifas en este diseño.

Spring Boot no carga automáticamente .env. Un archivo .env de Compose normalmente sirve para interpolar su YAML, no se inyecta por sí solo a un proceso Spring ni al jar iniciado en otra terminal. Config no secreta puede entrar por environment explícito; secretos siguen el bootstrap/entorno descritos, no archivos .env. README mostrará nombres y marcadores, nunca valores ni comandos literales con claves. Producción debe fallar preflight si profile efectivo no es exactamente pro o si faltan secretos/configuración, aunque application.yml tenga fallback local.

### D-06 Decisión demo-vulnerable y SSRF

| Opción | Ventaja | Coste/riesgo y decisión |
| --- | --- | --- |
| Demo pública controlada | Escaneable desde pro con IP pública autorizada | Publica deliberadamente un target vulnerable, necesita aislamiento, protección/observación y administración adicionales; descartada para este plazo. |
| Demo separada restringida por red | Podría aceptar solo el egress del scanner manteniendo IP pública | Requiere otro host/entrada pública, excepción de bind demo, origen/binding/corpus nuevos y controles propios. IP privada/localhost seguirían bloqueados. Válida evolución, no elegida. |
| Demo privada en EC2/VPC, túnel o modo local en producción | Parece simple | Incompatible con política pro o exige bypass; rechazada, no se implementa. |
| Demo no desplegada; validación real solo local | Conserva evidencia real existente, coste/ataque mínimos y SSRF intacto | **Elegida.** No hay recorrido live de demo en AWS; se declara expresamente, sin fingir paridad. |

La instalación AWS demuestra UI/API protegidas, configuración y persistencia; no incluye un target por defecto ni importa Findings/BD del equipo local. La defensa académica usa por separado recorrido local reproducible y evidencia del evaluator real validado; vídeo/slides no son funcionalidades de este change. No convertir el despliegue en un importador de históricos ni fabricar datos para llenar la UI. Un backend AWS sin target permitido puede consultar/crear configuración, pero no se acredita un Scan exitoso hasta configurar un target público real autorizado fuera de esta topología.

SSRF tests conservan pro bloqueando loopback, redes privadas y metadata incluso con TARGET_LOCAL_DEMO_ENABLED=true y origen declarado, y bloquean resolución pública que termina en privada. No cambiar TargetDestinationPolicy/LocalTargetUrlPolicy, no usar IP pública que reenvíe a la demo local para evadirlos, no activar local junto a pro. Abrir443 de la aplicación no autoriza usarla como target. IMDSv2 obligatorio y hop-limit1: los contenedores no necesitan credenciales IAM, solo bootstrap host; seguir bloqueando metadata en el core. Egress backend por red de salida es necesario para DNS/HTTPS autorizados, no implica permiso de Scan ni exportación.

### D-07 Healthchecks y single-instance

No Actuator ni endpoint nuevo. En red app, desde ingress/sonda administrativa de la instalación, GET http://backend:8080/api/projects con timeout5s, sin guardar cuerpo; aceptar solo200 y JSON. Esta lectura existente verifica que el contexto arrancó y puede consultar BD; no es prueba de todos los casos de negocio ni carga recomendada alta. Frecuencia30s con margen startup120s, no reinicio agresivo ante una única respuesta fallida. PostgreSQL: pg_isready al proceso local, complementado con Flyway/checksums y lectura backend; pg_isready solo no acredita autenticación/schema. El host puede usar herramientas de los contenedores existentes; no añadir curl/Actuator al core solo por health.

Comprobación de publicación posterior: HTTPS raíz/deep link y GET /api/projects con Basic Auth/Origin desde una IP no registrada, con allowlist desactivada; solo status/tipo de respuesta, sin guardar listas de proyectos ni secreto. Sin auth debe401; con credencial válida debe funcionar sin condición de IP. Solo si se activa la restricción opcional comprobar además que fuera de sus CIDRs falla conexión y que Basic Auth sigue exigido dentro. Una ruta estática200 no acredita backend; /api no tiene fallback SPA. Health protegido no requiere API key OpenAI ni manifest. No whitelist de path que exponga listas públicas por confundir health con read-only.

Mantener una réplica backend, nombre de instancia único y política de actualización stop/start sin solapamiento. Cerrar entrada a nuevas escrituras, esperar Scans activos terminales bajo autorización y parar backend anterior antes de iniciar el nuevo. Startup recovery existente puede interrumpir activos tras caída; no prometer reanudación, retry o distribución. Reverse proxy devuelve error de upstream si backend cae, sin seguir atendiendo escrituras con otro contenedor. Compose scale>1/segundo stack contra la misma BD queda prohibido en el runbook.

Logs de aplicación/PG con rotación Docker explícita, referencia10MiB por fichero/tres ficheros por servicio. Proxy usa access_log con formato cerrado método/status/duración/upstream_status, sin request URI/query/usuario/headers. Deshabilitar el error log crudo derivado de peticiones si no puede garantizar ese saneamiento: no habilitar debug ni confiar en que el log nativo nunca incluya URI; errores de configuración se comprueban con nginx -t en preflight sin datos de requests. Saneamiento probado con marcadores sintéticos en URL/headers/body, sin claves reales. Definir límites de memoria de referencia backend1536MiB, PG768MiB, ingress128MiB y temporal64MiB; validar margen OS/SSM/Docker sobre4GiB antes de abrir. No se optimiza el core ni se cambian sus algoritmos; si la prueba muestra falta de memoria, bloquear apertura y revisar tamaño aprobado, no quitar controles.

### D-08 Builds reproducibles y README F4

Ficheros posteriores mínimos: Dockerfile frontend/proxy y backend, compose de producción separado, nginx.conf, bootstrap/entrypoint pequeños sin lógica de negocio y plantilla AWS única bajo docker/production. Fijar AMI/arquitectura, versiones Docker/Compose y digests de nginx/PostgreSQL/JRE/Node seleccionados al implementar; no latest ni dependencia de un tag mutable como única evidencia. No afirmar patches más recientes sin verificarlos. Construir backend/frontend fuera de EC2 y publicar imágenes por revisión/digest en ECR privado; imagen demo existente se construye solo para reproducción local opcional.

Guía raíz debe poder seguirse desde checkout limpio sin node_modules/target/manifest.local heredados:

1. Requisitos JDK21, Maven3.9.16 vía wrappers, Node22 LTS compatible >=22.12.0 (registrar patch exacto utilizado), npm10.8.2 declarado, PostgreSQL16, Docker/Compose v2 solo si se usa ese camino. La versión exacta del entorno se registra al verificar; no instalar/actualizar ahora.
2. Checkout por ref/revisión conocida; verificar java/javac/node/npm y wrappers sin rutas absolutas del PC actual. Distinguir PowerShell y shell POSIX de AWS para no copiar sintaxis incompatible.
3. PostgreSQL local con el Compose solo local y datos sintéticos; producción con su archivo/rol/secretos nuevos. Inventario de nombres de variables con defaults/obligatorias/secreta y mecanismo de inyección; .env no automático.
4. Frontend desde frontend: npm ci respetando lockfile y npm run build -- --configuration production. Backend desde backend: sh ./mvnw -DskipTests package. Demo desde demo-vulnerable: sh ./mvnw -DskipTests package (mvnw.cmd equivalente en Windows). Los wrappers POSIX están versionados con modo 100644: invocarlos mediante sh, sin exigir chmod. Son comandos de build propuestos; no ejecutados ni sustituto de tests. No npm install que reescriba lockfile; no Maven global requerido. Builds con red solo tras autorización en futura ejecución.
5. Arranque DB→backend (Flyway+JPA+recovery)→ingress/Angular; desde cero no exigir que un jar ya exista. Local puede usar npm start y profile local; AWS jamás Angular dev server ni spring-boot:run, solo artefactos production/JRE21.
6. Local demo con DEMO_PROFILE=VULNERABLE, bind/8081 y permisos SSRF exactos, completar desde plantillas/binding/updatedAt real; exportación false por defecto. Operación evaluator real opt-in independiente con todos los gates, EVALUATOR_API_KEY solo entorno, cuota/coste y cierre del change correspondiente. No repetir validación real para comprobar README offline.
7. Producción pro sin demo ni target/evaluador activos por defecto, /api mismo origen, acceso HTTPS y secretos distribuidos fuera de banda; comandos de health sin credenciales literales.
8. Verificación, error común profile local por fallback, env de proceso antiguo, cap/timeout, migración, certificado y rutas; backup/stop/start/cierre sin borrar volumen. No meter comandos destructivos como parte automática del happy path.

Actualizar solo secciones afectadas de README raíz y enlace específico de README frontend/demo cuando proceda, arquitectura-general y ADR operativa después de aprobación. Registrar pendiente/real en checklist entrega y enlaces; no marcar despliegue/credenciales de prueba COMPLETADO por tener archivos. La credencial de entrada se entrega privadamente; README no incluye contraseña de evaluación. No sincronizar/archivar changes históricos como efecto lateral.

### D-09 Verificación y criterio F1–F4

| Finding | Evidencia para preparación | Comprobación adicional al desplegar |
| --- | --- | --- |
| F1 | Prueba de proxy sin auth401 en UI/assets/API, con auth correcto; Origin ajeno403; fallo cerrado por config ausente; ninguna publicación backend |443 público accesible desde IP no registrada con Basic Auth; sin auth401; certificado/hostname/HTTPS y puertos internos cerrados; CIDRs solo si se activa el modo opcional |
| F2 | Cliente contra proxy+upstream contador sintético: Content-Length/chunked>1MiB413 con cero upstream, idle>15s abortado, compresión415, DTO legítimo aceptado | Misma configuración/digest publicado, sin puerto alternativo8080, control de buffering/temporales |
| F3 | BD aislada vacía, Flyway oficial íntegro y validate; restart conserva datos sin duplicaciones; rol app no superuser; redes sin puertos/data solo backend+PG | EBS cifrado/retención, secretos nuevos, SG cerrado y restauración autorizada con clave custodiada |
| F4 | Reproducción desde checkout limpio con guía y versiones registradas, sin ficheros locales preexistentes ni OpenAI | Pasos de publicación AWS ejecutados con autorización y evidencia limitada; URL/coste/acceso Basic Auth y TLS reales; CIDRs solo si se configuran |

Las pruebas de proxy/Compose/BD implican levantar fixtures y migrar BD de prueba, por tanto necesitan solicitud expresa posterior; no se ejecutan aquí. BD de tests separada, nunca borrar producción para acreditar clean install. Reutilizar tests SSRF pro existentes y añadir solo ausencia específica si hace falta. Las pruebas de red no llaman target/OpenAI ni leen secretos; credenciales de fixture sintéticas. Preparación validada != despliegue validado != integración real AWS; F1–F3 cloud siguen abiertos hasta evidencia adecuada. No ampliar suite por mantenimiento ajeno.

### D-10 Coste, plazo y administración

Coste **bajo relativo** a arquitecturas AWS con ALB/RDS/NAT/HA: una EC2, EBS cifrado, IPv4/EIP, ECR, DNS/dominio y custodia de secretos; transferencia/logs/snapshots también pueden facturarse. Sin estimaciones monetarias, tarifas verificadas o promesa de gratuidad. La selección real m7i-flex.large responde a la elegibilidad comunicada por el operador para esta cuenta en eu-north-1; no presupone elegibilidad universal ni coste cero. Revisar elegibilidad Free Tier, capacidad y consumo antes de usar cualquiera de las alternativas permitidas, sin cambiar arquitectura. Instancia detenida puede seguir generando coste de EBS/IP/dominio; presupuestar ventana y eliminación autorizada posterior preservando datos. No abrir consola ni consultar precios ahora. Antes de crear recursos, operador confirma región/cuenta/cuotas y presupuesto; no tratar AWS Budget como hard cap automático de consumo OpenAI.

Orden objetivo en Europe/Madrid:20 diseño;21 empaquetado/proxy/secretos/guía;22 pruebas locales F1–F4;23 despliegue solo si está autorizado y hay dominio/certificado/Basic Auth;24 verificación de acceso del evaluador sin IP preconocida;25 documentación/evidencia y margen. Fecha límite antes del26-10-2026; no prometer cumplimiento sin recursos/autorizaciones. Si se retrasa dominio/certificado o credenciales, no abrir sin protección para cumplir fecha. Los CIDRs no bloquean el acceso académico. No se crean recordatorios ni automatizaciones.

### D-11 Validación pendiente 6.3: backup lógico y restore aislado

**Estado del diseño: READY FOR IMPLEMENTATION; NO EJECUTADO.** Se concreta únicamente el restore que mantiene 6.3 PARTIAL; no se reabren F1/F2, acceso, TLS, 6.1 ni despliegue. La ejecución requiere autorización posterior. Este procedimiento no modifica scripts ni crea una herramienta permanente.

**Hechos inspeccionados:** compose.yml identifica proyecto Compose llm-scanner-pro, servicio postgres, imagen PostgreSQL16-alpine fijada por digest y PGDATA externo. postgres/init.sh crea scanner_app como propietario de llm_scanner_seguridad_pro y su esquema public, sin privilegios globales. pg_hba.conf permite scanner_app por socket Unix con SCRAM; el secreto app está montado como /run/secrets/app-password, legible por postgres/UID70. scanner_bootstrap es administrador: no hace falta para el backup. La evidencia AWS identifica volumen llm-scanner-pgdata vinculado a /srv/llm-scanner/postgres y un proyecto persistido, pero no aporta su UUID. El operador debe copiar ese UUID de la URL/API ya conocida; no se inventa ni selecciona por nombre.

**Elección mínima:** pg_dump consistente con scanner_app sobre el socket existente, sin parar servicios; pg_restore en otro contenedor de la misma imagen local por Image ID, con red none, sin ports ni secretos productivos, sin backend y volumen local de tipo tmpfs distinto del productivo. Dump y diagnósticos privados en /run tmpfs; nunca copiar PGDATA, montar EBS productivo en el destino, usar snapshot con parada o hacer restore en producción. Se reutilizan Docker, herramientas PG16 de la imagen, Bash/coreutils/util-linux del host y readiness.sh existente. No instalaciones, pulls ni servicios AWS nuevos.

**Precondiciones:** sesión Bash administrativa autorizada en la EC2 ya operativa, con Docker; mínimo1536MiB MemAvailable,64MiB libres en /run y BD<=64MiB para esta prueba académica pequeña. Se limita dump a64MiB, volumen temporal a512MiB y contenedor a768MiB/0.25CPU; si no caben los datos, la prueba queda BLOCKED para rediseñar capacidad, sin usar EBS productivo como espacio temporal ni quitar límites. No despliegues/migraciones durante la ventana y ninguna edición del proyecto de validación mientras se comparan testigos; otras lecturas/servicio continúan. No se detiene la producción ni se fuerza su inactividad. Leer pg_dump implica locks ACCESS SHARE, compatibles con DML pero no con DDL; lock-wait-timeout evita esperar indefinidamente.

Los bloques siguientes son comandos **propuestos, no ejecutados**, para una misma sesión supervisada. No son un script instalado. Antes de cada etapa comprobar exit code: ante cualquier fallo, no continuar hacia PASS, registrar el código de etapa sin salida cruda y ejecutar el cleanup acotado de D-11.7. Si se pierde la sesión, recuperar los nombres del registro no secreto de la ejecución y completar cleanup; nunca deducirlos de un prefijo con borrado masivo.

#### D-11.1 Identificación y recursos permitidos

```bash
set +x
set -euo pipefail
umask 077
# Lo suministra el operador: UUID del proyecto YA persistido, no un nombre.
: "${VALIDATION_PROJECT_ID:?Falta UUID del proyecto existente}"
[[ "$VALIDATION_PROJECT_ID" =~ ^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$ ]]
test "$(id -u)" = 0
test "$(findmnt -n -o FSTYPE -T /run)" = tmpfs
awk '/MemAvailable:/ {ok=($2>=1572864)} END {exit !ok}' /proc/meminfo
test "$(df -Pk /run | awk 'NR==2 {print $4}')" -ge 65536

PROD_PG=$(docker ps -q --filter label=com.docker.compose.project=llm-scanner-pro --filter label=com.docker.compose.service=postgres)
PROD_BACKEND=$(docker ps -q --filter label=com.docker.compose.project=llm-scanner-pro --filter label=com.docker.compose.service=backend)
PROD_INGRESS=$(docker ps -q --filter label=com.docker.compose.project=llm-scanner-pro --filter label=com.docker.compose.service=ingress)
for id in "$PROD_PG" "$PROD_BACKEND" "$PROD_INGRESS"; do
  [[ "$id" =~ ^[0-9a-f]{12,64}$ ]] || { echo BLOCKED_PRODUCTION_IDENTITY; exit 1; }
  test "$(docker inspect -f '{{.State.Running}}' "$id")" = true
done
PROD_VOLUME=llm-scanner-pgdata
test "$(docker inspect -f '{{range .Mounts}}{{if eq .Destination "/var/lib/postgresql/data"}}{{.Name}}{{end}}{{end}}' "$PROD_PG")" = "$PROD_VOLUME"
test "$(docker volume inspect -f '{{index .Options "device"}}' "$PROD_VOLUME")" = /srv/llm-scanner/postgres
PG_IMAGE=$(docker inspect -f '{{.Image}}' "$PROD_PG")
docker image inspect "$PG_IMAGE" --format '{{.Id}}'
docker exec --user postgres "$PROD_PG" sh -c 'command -v pg_dump >/dev/null && command -v psql >/dev/null && command -v timeout >/dev/null'

WORK=$(mktemp -d /run/apr-restore.XXXXXX)
RUN=${WORK##*/}
TEMP_PG="$RUN-pg"
TEMP_VOLUME="$RUN-pgdata"
EVIDENCE=$(mktemp -d /run/apr-restore-evidence.XXXXXX)
printf 'run=%s\ncontainer=%s\nvolume=%s\n' "$RUN" "$TEMP_PG" "$TEMP_VOLUME" > "$EVIDENCE/resources.txt"
test "$TEMP_VOLUME" != "$PROD_VOLUME"
if docker container inspect "$TEMP_PG" >/dev/null 2>&1; then echo BLOCKED_CONTAINER_COLLISION; exit 1; fi
if docker volume inspect "$TEMP_VOLUME" >/dev/null 2>&1; then echo BLOCKED_VOLUME_COLLISION; exit 1; fi
printf '%s\n' "$PG_IMAGE" > "$EVIDENCE/postgres-image-id.txt"

# Solo objetos de esta ejecución, tanto en salida normal como por error.
cleanup_objects() (
  trap - EXIT INT TERM
  docker info --format '{{.ServerVersion}}' >/dev/null 2>&1 || exit 1
  case "$WORK" in /run/apr-restore.??????) ;; *) exit 1;; esac
  test -d "$WORK" && test ! -L "$WORK" && test "$(stat -c %u "$WORK")" = 0 || exit 1
  test "$RUN" = "${WORK##*/}" && test "$TEMP_PG" = "$RUN-pg" &&
    test "$TEMP_VOLUME" = "$RUN-pgdata" && test "$TEMP_VOLUME" != "$PROD_VOLUME" || exit 1
  if docker container inspect "$TEMP_PG" >/dev/null 2>&1; then
    test "$(docker inspect -f '{{index .Config.Labels "apr.restore.run"}}' "$TEMP_PG")" = "$RUN" &&
    test "$(docker inspect -f '{{.HostConfig.NetworkMode}}' "$TEMP_PG")" = none &&
    test "$(docker inspect -f '{{len .Mounts}}' "$TEMP_PG")" = 1 &&
    test "$(docker inspect -f '{{range .Mounts}}{{.Name}}{{end}}' "$TEMP_PG")" = "$TEMP_VOLUME" || exit 1
    docker rm -f "$TEMP_PG" >/dev/null || exit 1
  fi
  if docker volume inspect "$TEMP_VOLUME" >/dev/null 2>&1; then
    test "$(docker volume inspect -f '{{index .Labels "apr.restore.run"}}' "$TEMP_VOLUME")" = "$RUN" &&
    test "$(docker volume inspect -f '{{index .Options "type"}}' "$TEMP_VOLUME")" = tmpfs || exit 1
    docker volume rm "$TEMP_VOLUME" >/dev/null || exit 1
  fi
  remaining_containers=$(docker ps -aq --filter "label=apr.restore.run=$RUN") || exit 1
  remaining_volumes=$(docker volume ls -q --filter "label=apr.restore.run=$RUN") || exit 1
  test -z "$remaining_containers" && test -z "$remaining_volumes" || exit 1
  docker info --format '{{.ServerVersion}}' >/dev/null 2>&1 || exit 1
)
failed_exit() {
  rc=$?
  test "$rc" -ne 0 || return 0
  trap - EXIT
  set +e
  touch "$WORK/monitor.stop"
  test -z "${MONITOR_PID:-}" || wait "$MONITOR_PID"
  cleanup_objects >/dev/null 2>&1
  cleanup_rc=$?
  printf 'FAILED exit=%s cleanup_exit=%s\n' "$rc" "$cleanup_rc" > "$EVIDENCE/failure.txt"
  case "$WORK" in /run/apr-restore.??????)
    if test -d "$WORK" && test ! -L "$WORK" && test "$(stat -c %u "$WORK")" = 0; then
      rm -f -- "$WORK/backup.dump" "$WORK/private.log" "$WORK/check.sql" "$WORK/monitor.stop"
      rmdir -- "$WORK"
    fi;;
  esac
  exit "$rc"
}
trap failed_exit EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
```

Si una comprobación falla, **detener esa etapa**; set -e/pipefail y el trap posterior intentan terminar la prueba con cleanup, no continuar hacia PASS. No imprimir docker inspect completo/config/env ni leer el secreto en el host. El Image ID fija exactamente la imagen local utilizada por producción; comprobar major16 por SQL a continuación. La creación de metadatos Docker usa el disco de sistema normal; el volumen/EBS que contiene PGDATA productivo no se monta/escribe desde ningún recurso temporal. No editar ni reasignar WORK/RUN/TEMP_PG/TEMP_VOLUME/PROD_VOLUME durante la sesión.

#### D-11.2 Lectura de referencia y disponibilidad

```bash
prod_sql() {
  docker exec -i --user postgres "$PROD_PG" sh -ceu '
    set +x
    export PGPASSWORD="$(cat /run/secrets/app-password)"
    export PGOPTIONS="-c default_transaction_read_only=on -c statement_timeout=15000 -c lock_timeout=5000"
    exec psql -X -qAt -w -U scanner_app -d llm_scanner_seguridad_pro \
      -v ON_ERROR_STOP=1 -v validation_id="$1"
  ' -- "$VALIDATION_PROJECT_ID"
}
prod_identity() {
  for id in "$PROD_PG" "$PROD_BACKEND" "$PROD_INGRESS"; do
    docker inspect -f '{{.Id}}|{{.Image}}|{{.State.Running}}|{{.State.StartedAt}}|{{.RestartCount}}' "$id"
  done
  docker inspect -f '{{range .Mounts}}{{if eq .Destination "/var/lib/postgresql/data"}}{{.Name}}|{{.Source}}{{end}}{{end}}' "$PROD_PG"
}
test "$(prod_sql <<< 'SELECT current_setting('\''server_version_num'\'')::int / 10000;')" = 16
test "$(prod_sql <<< 'SELECT pg_database_size(current_database()) <= 67108864;')" = t
prod_identity | sha256sum > "$EVIDENCE/production-identity.before"

# Función existente: GET privado backend/API,200/JSON y cuerpo descartado.
docker exec "$PROD_INGRESS" /opt/readiness.sh
(
  trap - EXIT INT TERM
  while test ! -f "$WORK/monitor.stop"; do
    if docker exec "$PROD_INGRESS" /opt/readiness.sh >/dev/null 2>&1; then result=PASS; else result=FAIL; fi
    printf '%s,%s\n' "$(date -u +%FT%TZ)" "$result" >> "$EVIDENCE/availability.csv"
    sleep 5
  done
) &
MONITOR_PID=$!
```

Crear en WORK el siguiente SQL, que devuelve solo booleanos, conteos y huellas de metadata/proyecto sintético, nunca nombres/descripciones/evidencias libres, blobs cifrados o credenciales. El mismo SQL se usa antes, en el restore y después. Cada lectura es una transacción READ ONLY; las filas del proyecto no deben editarse concurrentemente. No comparar hashes físicos de PGDATA: una BD activa cambia WAL/estadísticas de forma normal.

```bash
cat > "$WORK/check.sql" <<'SQL'
BEGIN ISOLATION LEVEL REPEATABLE READ READ ONLY;
SET LOCAL TIME ZONE 'UTC';
SET LOCAL DateStyle TO 'ISO, YMD';
SELECT 'flyway_valid|' || (count(*)=7 AND bool_and(success)
  AND array_agg(version ORDER BY installed_rank)=ARRAY['1','2','3','4','5','6','7'])::text
FROM public.flyway_schema_history;
SELECT 'flyway_hash|' || md5(string_agg(version || ':' || coalesce(checksum::text,'') || ':' || success::text,'|' ORDER BY installed_rank))
FROM public.flyway_schema_history;
SELECT 'tables_valid|' || (count(*)=10 AND bool_and(tablename=ANY(ARRAY[
 'projects','scans','test_cases','test_executions','findings','target_configurations',
 'evaluation_results','scan_scores','security_reports','flyway_schema_history'])))::text
FROM pg_catalog.pg_tables WHERE schemaname='public';
SELECT 'schema_hash|' || md5(string_agg(table_name || ':' || column_name || ':' || data_type || ':' || is_nullable || ':' || coalesce(column_default,''),'|' ORDER BY table_name,ordinal_position))
FROM information_schema.columns WHERE table_schema='public';
SELECT 'constraints_hash|' || md5(string_agg(conrelid::regclass::text || ':' || conname || ':' || pg_get_constraintdef(oid),'|' ORDER BY conrelid::regclass::text,conname))
FROM pg_constraint WHERE connamespace='public'::regnamespace AND contype IN ('p','u','f','c');
SELECT 'constraints_valid|' || coalesce(bool_and(convalidated),false)::text
FROM pg_constraint WHERE connamespace='public'::regnamespace AND contype IN ('f','c');
SELECT 'indexes_valid|' || bool_and(i.indisvalid AND i.indisready)::text
FROM pg_index i JOIN pg_class c ON c.oid=i.indrelid WHERE c.relnamespace='public'::regnamespace;
SELECT 'catalog_valid|' || (count(*)=4 AND count(DISTINCT category)=4 AND bool_and(revision=1 AND code=ANY(ARRAY[
 'LLM01-INSTRUCTION-001','LLM02-CANARY-001','LLM08-CONTEXT-001','LLM10-MARKUP-001'])))::text FROM public.test_cases;
SELECT 'validation_present|' || (count(*)=1)::text FROM public.projects WHERE id=:'validation_id'::uuid;
SELECT 'validation_hash|' || coalesce(md5(string_agg(row_to_json(p)::text,'|' ORDER BY id)),'ABSENT')
FROM public.projects p WHERE id=:'validation_id'::uuid;
SELECT 'validation_children|' || (SELECT count(*) FROM public.target_configurations WHERE project_id=:'validation_id'::uuid)
 || '|' || (SELECT count(*) FROM public.scans WHERE project_id=:'validation_id'::uuid);
ROLLBACK;
SQL
prod_sql < "$WORK/check.sql" > "$EVIDENCE/production.before" 2> "$WORK/private.log"
for key in flyway_valid tables_valid constraints_valid indexes_valid catalog_valid validation_present; do
  grep -Fxq "$key|true" "$EVIDENCE/production.before" || { echo BLOCKED_REFERENCE; exit 1; }
done
```

Si historial/catálogo no corresponden a V1..V7/cuatro pruebas, detener y revisar contra la revisión desplegada; nunca migrar o corregir producción para ajustar esta prueba. Las huellas sirven para comparar estado lógico, no como prueba criptográfica de anonimización. Son datos sintéticos/metadata del proyecto de validación; no se generaliza la huella a filas sensibles.

#### D-11.3 Backup consistente, protegido y sin credencial en argv

```bash
# Bash ulimit -f usa KiB; solo afecta al subshell del backup.
printf '%s,BACKUP_START\n' "$(date -u +%FT%TZ)" >> "$EVIDENCE/steps.csv"
(
  trap - EXIT INT TERM
  ulimit -f 65536
  docker exec --user postgres "$PROD_PG" sh -ceu '
    set +x
    export PGPASSWORD="$(cat /run/secrets/app-password)"
    export PGOPTIONS="-c default_transaction_read_only=on"
    exec timeout -s TERM -k 5 120 pg_dump -w -U scanner_app \
      -d llm_scanner_seguridad_pro --format=custom --no-acl --lock-wait-timeout=5s
  ' > "$WORK/backup.dump" 2> "$WORK/private.log"
)
test -s "$WORK/backup.dump"
stat -c %s "$WORK/backup.dump" > "$EVIDENCE/backup.bytes"
sha256sum "$WORK/backup.dump" | awk '{print $1}' > "$EVIDENCE/backup.sha256"
printf '%s,BACKUP_EXIT_0\n' "$(date -u +%FT%TZ)" >> "$EVIDENCE/steps.csv"
```

Exigir exit0 del subshell antes de continuar; fichero no vacío por sí solo no acredita backup. pg_dump hace su snapshot lógico consistente sin detener PostgreSQL y sin congelar la aplicación. El timeout se ejecuta dentro del contenedor para terminar pg_dump, no solo el cliente docker del host. No pg_dumpall/globals/roles/password hashes; no URI con password ni docker exec -e PGPASSWORD=valor. PGPASSWORD se resuelve solo dentro del proceso local de PostgreSQL, sin eco/logs; la contraseña no sale al host ni a evidencia. El dump puede contener datos/blobs cifrados del producto y se trata entero como privado aunque no incluya secretos de roles; no mostrarlo, subirlo al repo ni retenerlo como evidencia.

#### D-11.4 Destino temporal estrictamente aislado

```bash
docker volume create --label "apr.restore.run=$RUN" --driver local \
  --opt type=tmpfs --opt device=tmpfs --opt o=size=512m "$TEMP_VOLUME"
docker run -d --pull=never --name "$TEMP_PG" --label "apr.restore.run=$RUN" \
  --network none --restart no --log-driver none --memory 768m --memory-swap 768m \
  --cpus 0.25 --pids-limit 128 --shm-size 64m --security-opt no-new-privileges:true \
  --mount "type=volume,source=$TEMP_VOLUME,target=/var/lib/postgresql/data" \
  -e POSTGRES_USER=restore_bootstrap -e POSTGRES_DB=restore_check \
  -e POSTGRES_HOST_AUTH_METHOD=trust -e POSTGRES_INITDB_ARGS=--auth-local=trust \
  "$PG_IMAGE" postgres -c 'listen_addresses=' -c shared_buffers=32MB \
    -c max_connections=5 -c log_statement=none -c log_min_error_statement=panic

test "$(docker inspect -f '{{.HostConfig.NetworkMode}}' "$TEMP_PG")" = none
test "$(docker inspect -f '{{len .HostConfig.PortBindings}}' "$TEMP_PG")" = 0
test "$(docker inspect -f '{{len .Mounts}}' "$TEMP_PG")" = 1
test "$(docker inspect -f '{{range .Mounts}}{{.Name}}{{end}}' "$TEMP_PG")" = "$TEMP_VOLUME"
test "$(docker volume inspect -f '{{index .Options "type"}}' "$TEMP_VOLUME")" = tmpfs
for n in $(seq 1 30); do
  docker exec --user postgres "$TEMP_PG" pg_isready -q -U restore_bootstrap -d restore_check && break
  sleep 1
done
docker exec --user postgres "$TEMP_PG" pg_isready -q -U restore_bootstrap -d restore_check
```

Trust se limita al cluster desechable sin red, sin escucha TCP y accesible solo por docker exec administrativo/socket local; no cambia pg_hba ni autenticación de producción y evita aprovisionar/copiar secretos en el destino. No se monta init.sh, pg_hba.conf, /run/secrets, TLS, AES, EBS productivo ni redes app/data/egress. No se usa Compose up/down para crear un segundo stack. La imagen ya existe por Image ID: no pull, nuevas dependencias ni acceso ECR/AWS.

#### D-11.5 Restore y verificación mínima

```bash
printf '%s,RESTORE_START\n' "$(date -u +%FT%TZ)" >> "$EVIDENCE/steps.csv"
docker exec -i --user postgres "$TEMP_PG" timeout -s TERM -k 5 120 \
  pg_restore -w -U restore_bootstrap -d restore_check --no-owner --no-acl \
    --exit-on-error --single-transaction \
  < "$WORK/backup.dump" > /dev/null 2> "$WORK/private.log"
printf '%s,RESTORE_EXIT_0\n' "$(date -u +%FT%TZ)" >> "$EVIDENCE/steps.csv"
docker exec -i --user postgres "$TEMP_PG" psql -X -qAt -w \
  -U restore_bootstrap -d restore_check -v ON_ERROR_STOP=1 -v validation_id="$VALIDATION_PROJECT_ID" \
  < "$WORK/check.sql" > "$EVIDENCE/restored.check" 2> "$WORK/private.log"
cmp -s "$EVIDENCE/production.before" "$EVIDENCE/restored.check"
```

Exigir pg_restore exit0 y comparación exit0. --single-transaction/--exit-on-error evita aceptar un restore parcial; PostgreSQL recrea y valida índices/FKs/checks al restaurar. Se comprueban diez tablas incluidas Flyway, versiones/checksums/success, estructura de columnas y constraints, índices válidos, catálogo de cuatro pruebas, proyecto de validación íntegro y sus conteos target/Scan. No arrancar backend/Flyway contra restore_check: se valida lo restaurado, no un esquema regenerado que oculte errores. No --clean, --create ni --dbname productiva. Solo restore_bootstrap escribe y solo en restore_check del contenedor temporal.

--no-owner/--no-acl permite restaurar sin copiar roles/secrets de producción. Esta prueba acredita backup/restauración lógica de esquema y datos, **no** restauración de permisos de todos los roles, clave AES, descifrado de credenciales, TLS, host completo o failover. Se conserva la custodia AES de D-04/D-05 sin leerla/provisionarla para este test. Las referencias se leen antes del dump, no del mismo snapshot exportado: por eso se exige estabilidad del proyecto/metadata durante la ventana y coincidencia posterior; cambios concurrentes relevantes dejan comparación inconclusa, no justifican sobrescribir producción ni fabricar igualdad.

#### D-11.6 Producción intacta y disponible

```bash
prod_sql < "$WORK/check.sql" > "$EVIDENCE/production.after-restore" 2> "$WORK/private.log"
prod_identity | sha256sum > "$EVIDENCE/production-identity.after-restore"
cmp -s "$EVIDENCE/production.before" "$EVIDENCE/production.after-restore"
cmp -s "$EVIDENCE/production-identity.before" "$EVIDENCE/production-identity.after-restore"
docker exec "$PROD_INGRESS" /opt/readiness.sh
```

Se comparan identidad/image/StartedAt/RestartCount/volumen de los tres servicios, historial/schema y testigo del proyecto. Readiness privado200/JSON se muestrea antes/durante/después con código existente y sin Basic Auth de producción. Los intervalos no prueban disponibilidad continua absoluta; registrar frecuencia5s/timeout5s, timestamps, duración y cualquier fallo observado. No afirmar cero cambios físicos de WAL/estadísticas ni igualdad de toda la BD si hubo otros usuarios. La prueba no emite DML/DDL, reinicios o cleanup contra producción; actividad interna normal del servidor no equivale a que el test modificó sus datos.

#### D-11.7 Cleanup exacto, también si falla una etapa

Conservar la sesión/variables o recuperarlas exclusivamente de resources.txt de esta ejecución. cleanup_objects, definida antes del backup, valida identidad, etiquetas y montajes **antes** de cada borrado. failed_exit intenta el mismo cleanup si falla una etapa, también tras SIGINT/SIGTERM; no protege frente a SIGKILL/pérdida del host, casos que requieren verificar recursos al recuperar la sesión. Si una guarda falla, detener cleanup de ese objeto y dejar estado CLEANUP_BLOCKED para revisión; nunca usar prune, comodines, down -v ni nombres de producción como alternativa.

```bash
cleanup_objects

# Verificación final antes de retirar el SQL temporal; si no se creó, registrar NOT_RUN.
prod_sql < "$WORK/check.sql" > "$EVIDENCE/production.after-cleanup" 2> "$WORK/private.log"
prod_identity | sha256sum > "$EVIDENCE/production-identity.after-cleanup"
cmp -s "$EVIDENCE/production.before" "$EVIDENCE/production.after-cleanup"
cmp -s "$EVIDENCE/production-identity.before" "$EVIDENCE/production-identity.after-cleanup"
docker exec "$PROD_INGRESS" /opt/readiness.sh
touch "$WORK/monitor.stop"
wait "$MONITOR_PID"
awk -F, '$2!="PASS" {failed++} END {exit failed>0}' "$EVIDENCE/availability.csv"

# SOLO estos ficheros del directorio tmpfs creado por esta prueba; sin rm -rf.
rm -f -- "$WORK/backup.dump" "$WORK/private.log" "$WORK/check.sql" "$WORK/monitor.stop"
rmdir -- "$WORK"
test ! -e "$WORK"
printf 'PASS backup=0 restore=0 comparisons=0 cleanup=0\n' > "$EVIDENCE/result.txt"
trap - EXIT INT TERM
```

Si la etapa previa falló antes de crear SQL/monitor, omitir únicamente esas lecturas finales inexistentes y marcar NOT_RUN; siempre retirar dump/error privado si existen y completar las guardas del cleanup. Un fallo de disponibilidad/comparación se registra como FAIL, pero no impide limpiar recursos temporales seguros. Un Docker no disponible impide acreditar cleanup hasta verificarlo, sin intentar borrar su almacenamiento manualmente. No shred sobre tmpfs/EBS ni rm del directorio /srv/llm-scanner/postgres: no mejora seguridad y amenaza producción. No se conservan dumps ni errores crudos como evidencia de fallo.

**Evidencia a conservar:** resultado breve de cada etapa/exit code, tiempos, versión PG16 e Image ID usado, tamaño y SHA256 del dump (no el dump), etiquetas/nombres efímeros, guardas de red none/sin puertos/mount tmpfs distinto, hashes y booleanos/conteos de production.before/restored.check/production.after-restore/after-cleanup, identidad productiva comparada, availability.csv (incluidos fallos) y ausencia final de contenedor/volumen/dump. Mantener EVIDENCE solo con esos ficheros no secretos y trasladarlos por el operador al paquete de evidencia del change; después puede retirar ese directorio tmpfs con sus nombres concretos. No incluir UUID real si no es necesario publicarlo: registrar una huella del testigo, no una lista de proyectos. No docker logs/inspect/env completos, pg_dumpall, passwords, blobs, contenido de proyectos o terminal transcrita con secretos.

**Aceptación 6.3:** PASS solo con backup exit0, restore transaccional exit0, equivalencia de testigos/metadata, integridad válida, disponibilidad sin fallos observados, servicios productivos sin reinicios/cambio de volumen y cleanup acreditado. Inconcluso por concurrencia/capacidad/UUID no disponible o fallo mantiene6.3 PARTIAL, con motivo y sin tocar producción. El diseño no marca6.3 completa, no reabre las comprobaciones F1–F3 ya aceptadas y no cierra6.1 ni el change. Las guardas protegen el ámbito de comandos supervisados; no son garantía frente a un operador root malicioso.

## Risks / Trade-offs

- [EC2/PG comparten host y fallo] → volumen cifrado persistente/backup-restauración probado y aceptación de indisponibilidad, sin promesa HA.
- [Admin root/Docker puede leer secretos] → acceso solo SSM/IAM del operador; no contenedores privilegiados/docker.sock ni credenciales IAM para aplicaciones.
- [IP del tribunal desconocida/cambiante] →443 público con Basic Auth, allowlist desactivada por defecto; validar acceso desde IP no registrada antes del25.
- [Mayor exposición de443 a Internet] → autenticación obligatoria sin rutas exentas, límites HTTP existentes y backend/PG/SSH inaccesibles; la restricción IP queda disponible solo como opción explícita.
- [Certificado no disponible/expira] → dominio controlado, Certbot/Let's Encrypt manual DNS-01, revisión semanal y antes de evaluación, renovar con30días o menos y comprobar recarga; publicación bloqueada sin HTTPS válido.
- [Basic Auth cacheada permite CSRF] → Origin obligatorio en escrituras + CORS exacto + TLS; no plataforma multiusuario.
- [Body lento periódico no alcanza timeout inactivo] → tamaño/conexiones/memoria y acceso restringidos; limitación explícita, sin falso deadline total.
- [Memoria/disco compartidos] → build fuera, límites JVM/PG/proxy/logs/temporales y margen; no montar servicios ilimitados.
- [Clave AES perdida tras restore] → custodiar/restaurar la misma versión independientemente de BD; no rotación automática.
- [Demo no vive en AWS] → declararlo en README/defensa y conservar evidencia local; no presentar esta instalación como E2E cloud del evaluator.
- [Documentación histórica con estado anterior] → actualizar solo pasajes afectados con evidencia al implementar; no reescribir registros ni marcar TFM completo.

## Migration Plan

Preparar archivos y documentación → verificar offline autorizado desde checkout limpio/BD de prueba vacía → aprobar parámetros/coste/despliegue → provisionar infraestructura cerrada → secretos/certificado → DB y backend único/Flyway/readiness → ingress protegido → comprobar F1–F4 cloud → registrar URL/evidencia. No tocar datos locales ni importar históricos. Reversión mínima: cerrar entrada, parar backend nuevo y restaurar artefacto anterior compatible; para cambio de esquema incompatible usar backup probado y plan separado, no ejecutar migraciones inversas inventadas. En primera instalación sin datos solo se corrige configuración con entrada cerrada, nunca se borra BD existente automáticamente.

## Open Questions

No quedan decisiones de topología/acceso/TLS necesarias para implementar esta propuesta. Antes de despliegue se requieren cuenta/región/hostname/control de A y TXT DNS/certificado, credenciales Basic Auth de prueba y canal de entrega al tribunal, equipo del operador con Certbot y responsable de renovación, parche de herramientas/digests/AMI exactos verificados y autorización de coste/operación. No se requiere conocer IPs del tribunal; CIDRs solo si el operador pide activar la restricción opcional. Son parámetros o evidencias de ejecución, no permisos para sustituir seguridad ni cambiar la opción demo. Aprobar este change será necesario antes de convertir su selección de secretos/hosting en ADR estable; este diseño no resuelve pendientes ajenos.

## Fuera del MVP / Evolución futura

RDS, ALB/ACM, Cognito/OIDC, S3/CloudFront, WAF, CloudWatch centralizado, Secrets Manager con rotación, HA, backups continuos, workers/SQS y publicación separada de demo pueden considerarse después con cambios propios. Ninguno es requisito para la ventana académica ni se implementa por prevención. Se mantienen separados despliegue, investigación y slides/vídeo/formularios TFM.
