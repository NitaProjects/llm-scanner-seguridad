# Producción académica: guía y despliegue observado

Contrato: [aws-production-readiness](../../openspec/changes/aws-production-readiness/design.md).
Guía de compilación y ejecución local: [README raíz](../../Readme.md).
No ejecutar comandos AWS sin autorización independiente de cuenta/región/coste/dominio.

Despliegue real comunicado por el operador el 2026-10-20: [https://llmscannerseguridad.com](https://llmscannerseguridad.com),
en eu-north-1, EC2 m7i-flex.large/AL2023 x86_64, Docker25.0.14/Compose v2.40.3.
Requiere Basic Auth de prueba, entregable únicamente por canal privado; entrega efectiva al tribunal no acreditada. [Evidencia consolidada](../../openspec/changes/aws-production-readiness/evidence/2026-10-20-aws-deployment.md):
ECR privado por digest, EBS30GB/XFS, volumen `llm-scanner-pgdata`, SecureString→tmpfs,
Systems Manager y HTTPS operativo; sin demo ni evaluator activo por defecto.
Incidencias resueltas: rollback inicial t3.medium y lectura de privkey.pem por UID101.
Certificado Let's Encrypt expira el 2027-01-18; renovación manual DNS-01/Cloudflare,
no automática, responsabilidad del operador del proyecto. Review: 1.1/6.2 completas,
6.4 completa en preparación técnica y 6.1 completada, **24/25 completas**. Solo queda
restore aislado (6.3). Certbot en Docker + DNS-01 disponible y procedimiento validado,
según confirmación del operador. La guía siguiente no es una nueva ejecución.
Despliegue académico autorizado, Free Plan/créditos sin migración voluntaria autorizada
a Paid Plan ni gasto exacto medido. Mantener URL hasta finalizar entrega/evaluación;
después podrán detenerse o retirarse recursos innecesarios. El acceso académico está preparado;
la entrega efectiva de credenciales es administrativa y no reabre 6.4.

En documentación pública usar `<aws-account-id>`, `<instance-id>` y `<public-ip>` cuando
sean necesarios; para ECR, `<aws-account-id>.dkr.ecr.eu-north-1.amazonaws.com/<service>@sha256:<digest>`.
No publicar IDs reales innecesarios ni URI ECR con Account ID real. El dominio público se conserva.

## Tres servicios y acceso

`compose.yml` no extiende el Compose local. Solo ingress publica host443, traducido a8443
sin root dentro de Nginx. Backend8080 no tiene ports, PostgreSQL5432 tampoco. app y data
son redes internal; ingress no está en data. Backend usa su propia red egress; no la
comparte con ingress. Una réplica, container_name fijo; no scale, segundo stack contra
el mismo volumen ni rolling con solapamiento. No puertos22/80/8081 ni contenedor demo.

Basic Auth se hereda en UI, assets, deep links y API. Host debe ser canónico y las
escrituras necesitan Origin exactamente PUBLIC_ORIGIN, incluso desde clientes REST.
Authorization del proxy no se reenvía al backend. TLS1.2/1.3; HTTP/2 no habilitado.
Solo SPA usa index.html; /api conserva prefijo, status y Location, sin fallback HTML.

Nginx limita a1MiB, bufferiza antes de upstream, aplica idle15s en cabeceras/body,
buffer16KiB, tmpfs64MiB, ocho conexiones/IP y32 globales, cabeceras16KiB, keepalive15s,
send30s y upstream connect5s/send30s/read30s. Content-Encoding ajeno a identity se
rechaza415, retries de upstream deshabilitados. Idle no es deadline absoluto: un
emisor periódico puede continuar, dentro de tamaño/conexiones/memoria acotados.
Access log solo método/status/duración/status-upstream; error log crudo suprimido.
Docker rota10MiB x3/servicio; límites128/1536/768MiB para ingress/backend/PG.

## Configuración externa

No usar .env para secretos. Spring Boot NO carga .env automáticamente; Compose solo
interpola su YAML y transmite los campos environment explícitos. Pro usa exclusivamente
DB_URL/DB_USERNAME/DB_PASSWORD, nunca los defaults locales. El entrypoint rechaza otro
profile y configuración de demo/evaluator activa. No montar docker.sock ni credenciales
AWS en contenedores; IMDSv2/hop1 en la plantilla limita acceso al rol del host.

Variables no secretas requeridas por Compose:

| Variable | Valor/selección |
| --- | --- |
| PUBLIC_HOSTNAME | Dominio controlado, sin esquema ni puerto |
| PUBLIC_ORIGIN | https://dominio; origen exacto del navegador |
| SECRETS_DIR | Directorio protegido tmpfs del host, normalmente /run/llm-scanner |
| TLS_DIR | Directorio padre externo con active/fullchain.pem y active/privkey.pem |
| PGDATA_VOLUME | Volumen externo explícito, nuevo en primera instalación |
| BACKEND_IMAGE / INGRESS_IMAGE | En AWS URI ECR@sha256; localmente ID sha256 construido |

INGRESS_BIND_ADDRESS=0.0.0.0 e INGRESS_HTTPS_PORT=443 por defecto. Las pruebas locales
usan127.0.0.1:18443, exclusivamente en su proyecto aislado. Los settings Spring están
explícitos en Compose: pro, DB postgres:5432/llm_scanner_seguridad_pro, scanner_app,
CORS canónico, executor1/1/10, allowlists vacías y evaluator export=false.
EVALUATOR_API_KEY y EVALUATION_SCENARIO_FILE no se provisionan ni se heredan de la consola.
No se abre permiso de Scan por disponer de HTTPS o por guardar un Target.

Ficheros de secreto separados (ningún valor real en Git/imagen/argv/logs):

| Fichero en SECRETS_DIR | Destinatario / propietario Linux |
| --- | --- |
| admin-password | Solo PostgreSQL bootstrap, UID70,0400 |
| app-password-pg | Solo init PostgreSQL, UID70,0400 |
| app-password-backend | Misma contraseña app, solo backend, UID10001,0400 |
| target-aes | Base64 estándar de32bytes, solo backend, UID10001,0400 |
| htpasswd | Hash bcrypt fuerte de acceso técnico, solo ingress, UID101,0400 |

Admin y app deben ser contraseñas nuevas distintas, al menos24 caracteres, sin reutilizar
las del Compose de desarrollo. Mantener la misma clave AES entre reinicios/restauración;
no generarla en cada arranque. No existe autorización por usuario/Project en Spring:
las credenciales Basic Auth son acceso compartido académico y se entregan por canal privado.

## Secretos AWS, IAM y volumen

`aws.yml` prepara VPC/subred/IGW/rutas, SG443 IPv4 público por defecto, EIP, EC2 x86_64
m7i-flex.large por defecto, EBS gp3 cifrado separado30GiB/Retain y dos repositorios ECR privados inmutables.
No UserData con secretos ni comandos de despliegue automático. AMI Linux con SSM agent,
región/cuenta, hostname y nombres de parámetros SecureString son entradas del operador.
Las referencias de imágenes se configuran después en Compose, no en CloudFormation.
Todos esos valores deben verificarse antes de publicar. El operador informa que en el primer
despliegue en `eu-north-1` usó Amazon Linux 2023 x86_64 y eligió `m7i-flex.large` tras el
rollback del intento con `t3.medium`, no elegible para Free Tier en esta cuenta.
La consulta real comunicada por el operador devolvió `c7i-flex.large`, `t4g.small`,
`t3.micro`, `t4g.micro`, `m7i-flex.large` y `t3.small`. La plantilla permite únicamente
`m7i-flex.large`, `c7i-flex.large`, `t3.small` y `t3.micro`: se excluye `t4g.*` porque usa
ARM y no corresponde a la AMI x86_64 actual. No se cambia AMI ni arquitectura.
La elegibilidad Free Tier depende de cuenta/región y debe comprobarse antes de cada
despliegue; no garantiza coste cero del conjunto ni acredita un despliegue completado.
Estos hechos son aportados por el operador, sin consulta AWS en esta actualización.
IP del tribunal no requerida. RestrictIngressIp=true exige ReviewerCidr;
ese modo opcional no sustituye Basic Auth. Sin IPv6/AAAA ni puertos SSH/HTTP/backend/PG.

Rol de instancia: canales ssmmessages y UpdateInstanceInformation explícitos (sin política
administrada que amplíe GetParameter), lectura solo del
prefijo Parameter Store, kms:Decrypt condicionado a SSM y EncryptionContext del prefijo,
ECR token y pull de los dos repositorios. Provisión de secretos/push/CloudFormation con
rol de operador separado, sin claves AWS estáticas. El rol no se entrega a la aplicación.
El root del host sigue siendo de confianza; no se promete invisibilidad frente a /proc.

En el host autorizado, `/run` debe ser tmpfs. Con nombres de parámetros ya aprovisionados,
el bootstrap obtiene valores en memoria y escribe solo ficheros root-restringidos en tmpfs:

```sh
sudo python3 docker/production/bootstrap-secrets.py \
  --region "$AWS_REGION" \
  --admin-password-parameter "$ADMIN_PASSWORD_PARAMETER" \
  --app-password-parameter "$APP_PASSWORD_PARAMETER" \
  --target-aes-parameter "$TARGET_AES_PARAMETER" \
  --htpasswd-parameter "$HTPASSWD_PARAMETER"
```

No ejecutado en las pruebas locales ni autorizado contra AWS en esta tarea. Un error emite
solo código cerrado. Directorio padre0711 y ficheros0400 segregados por UID. En hosts
con remapeo de UID de Docker, comprobar la correspondencia antes de abrir entrada.
Recrear tmpfs al reiniciar y recuperar los mismos secretos custodiados; no cambiar la AES.

Inicializar/montar el EBS de datos mediante procedimiento administrativo autorizado,
identificando primero dispositivo/volumen y comprobando que está vacío antes de cualquier
formato. Nunca formatear automáticamente un volumen existente. Mantener PG y TLS fuera
del checkout, por ejemplo /srv/llm-scanner/postgres y /srv/llm-scanner/tls, en el EBS cifrado.
Tras preparar directorio PG vacío con permisos de PostgreSQL, crear el volumen externo:

```sh
docker volume create --driver local --opt type=none --opt o=bind \
  --opt device=/srv/llm-scanner/postgres "$PGDATA_VOLUME"
```

PG16/SCRAM usa bootstrap administrativo separado y crea scanner_app sin superuser,
createdb, createrole ni replication; propietario de su BD/esquema para Flyway. No baseline
ni migraciones copiadas. Flyway oficial V1..V7 al arrancar backend; JPA validate y recovery
single-instance. Un checksum inválido impide readiness. No copiar dumps de desarrollo.

## TLS externo: Certbot manual DNS-01

El operador confirma renovación manual mediante Certbot en Docker + DNS-01, procedimiento
ya validado y separado de los tres servicios de aplicación; no se añade un servicio ACME
permanente al despliegue. Estado ACME y material TLS fuera de Git. El siguiente ejemplo
es la variante con Certbot instalado en el equipo del operador, no evidencia de una nueva ejecución:

```sh
certbot certonly --manual --preferred-challenges dns -d "$PUBLIC_HOSTNAME" \
  --config-dir "$ACME_STATE/config" --work-dir "$ACME_STATE/work" --logs-dir "$ACME_STATE/logs"
```

El operador crea el TXT solicitado en _acme-challenge y verifica propagación antes de
confirmar. El A apunta a la EIP; no AAAA. No abrir80, no ACME automático, sin cuarto
contenedor ni credenciales DNS en las aplicaciones. No asumir certificados ACM exportables.

Instalar fullchain.pem/privkey.pem por canal administrativo seguro aprobado, fuera de
comandos/logs/UserData/repositorio, en un subdirectorio versionado dentro de TLS_DIR.
Clave legible solo para UID101/root; certificados pueden ser públicos, pero los reales
no se versionan. El canal concreto de transferencia/custodia debe validarse antes de
desplegar; no se copian aquí certificados reales. active debe apuntar dentro del mismo
directorio montado read-only, sin enlaces a rutas externas al montaje.

Revisar vigencia semanalmente y antes de evaluación. Con30días o menos, repetir emisión
manual DNS-01, instalar nuevo par en otro subdirectorio y sustituir active atómicamente.
Después validar y recargar solo Nginx, conservando el par previo para revertir:

```sh
docker compose -f docker/production/compose.yml exec ingress nginx -t -c /tmp/nginx.conf
docker compose -f docker/production/compose.yml exec ingress nginx -c /tmp/nginx.conf -s reload
openssl s_client -connect "$PUBLIC_HOSTNAME:443" -servername "$PUBLIC_HOSTNAME" </dev/null 2>/dev/null |
  openssl x509 -noout -serial -enddate
```

Comprobar cadena/hostname y Basic Auth tras recarga, sin reiniciar backend/PG. Usar caducidad
real, no prometer duración fija. Certbot manual no proporciona renovación desatendida con
certbot renew sin hooks. No publicar si dominio, TXT, certificado o htpasswd no están listos.

## Bootstrap inicial ECR y configuración por digest

Procedimiento futuro, **no ejecutado ni autorizado contra AWS en esta corrección**.
La plantilla crea infraestructura y repositorios vacíos con nombres generados. No recibe
parámetros de imagen, no instala Docker ni ejecuta Compose en la instancia; sus Outputs
de repositorios permiten publicar después. Provisionar el host, secretos, TLS y volumen
según las secciones anteriores sigue siendo un paso operativo independiente.

1. En el equipo del operador autorizado, preparar AWS_REGION, STACK_NAME, AMI_ID,
   ROOT_DEVICE_NAME (coincidente con la AMI), PUBLIC_HOSTNAME y una REVISION única aprobada.
   Con el rol separado de provisión, desde la raíz del checkout:

   ```sh
   aws cloudformation create-stack --region "$AWS_REGION" --stack-name "$STACK_NAME" \
     --template-body file://docker/production/aws.yml --capabilities CAPABILITY_IAM \
     --parameters "ParameterKey=AmiId,ParameterValue=$AMI_ID" \
       "ParameterKey=RootDeviceName,ParameterValue=$ROOT_DEVICE_NAME" \
       "ParameterKey=PublicHostname,ParameterValue=$PUBLIC_HOSTNAME"
   aws cloudformation wait stack-create-complete --region "$AWS_REGION" --stack-name "$STACK_NAME"
   BACKEND_REPOSITORY=$(aws cloudformation describe-stacks --region "$AWS_REGION" --stack-name "$STACK_NAME" \
     --query "Stacks[0].Outputs[?OutputKey=='BackendRepository'].OutputValue | [0]" --output text)
   INGRESS_REPOSITORY=$(aws cloudformation describe-stacks --region "$AWS_REGION" --stack-name "$STACK_NAME" \
     --query "Stacks[0].Outputs[?OutputKey=='IngressRepository'].OutputValue | [0]" --output text)
   ```

2. Construir fuera de EC2 desde esa revisión (JDK 21 comprobado previamente):

   ```sh
   (cd backend && sh ./mvnw -DskipTests package)
   docker build -f docker/production/Dockerfile.backend -t "$BACKEND_REPOSITORY:$REVISION" .
   docker build -f docker/production/Dockerfile.ingress -t "$INGRESS_REPOSITORY:$REVISION" .
   ```

3. Con el rol del operador con permiso de push a esos repositorios, autenticar Docker
   mediante password-stdin. DOCKER_CONFIG debe apuntar a un directorio privado en tmpfs;
   no usar el perfil de instancia para publicar ni imprimir/persistir tokens:

   ```sh
   export DOCKER_CONFIG
   REGISTRY=${BACKEND_REPOSITORY%%/*}
   aws ecr get-login-password --region "$AWS_REGION" |
     docker --config "$DOCKER_CONFIG" login --username AWS --password-stdin "$REGISTRY"
   docker --config "$DOCKER_CONFIG" push "$BACKEND_REPOSITORY:$REVISION"
   docker --config "$DOCKER_CONFIG" push "$INGRESS_REPOSITORY:$REVISION"
   BACKEND_DIGEST=$(aws ecr describe-images --region "$AWS_REGION" \
     --repository-name "${BACKEND_REPOSITORY#*/}" --image-ids "imageTag=$REVISION" \
     --query 'imageDetails[0].imageDigest' --output text)
   INGRESS_DIGEST=$(aws ecr describe-images --region "$AWS_REGION" \
     --repository-name "${INGRESS_REPOSITORY#*/}" --image-ids "imageTag=$REVISION" \
     --query 'imageDetails[0].imageDigest' --output text)
   printf 'BACKEND_IMAGE=%s@%s\nINGRESS_IMAGE=%s@%s\n' \
     "$BACKEND_REPOSITORY" "$BACKEND_DIGEST" "$INGRESS_REPOSITORY" "$INGRESS_DIGEST"
   docker --config "$DOCKER_CONFIG" logout "$REGISTRY"
   ```

   Registrar únicamente las dos referencias no secretas resultantes, verificando formato
   `repo@sha256:<64 hex>` y correspondencia con los repositorios del stack. Retirar la
   configuración temporal de autenticación por el procedimiento seguro del operador.

4. En la sesión administrativa del host EC2 preparado, incorporar esas referencias
   exactas a la configuración real de Compose. Se pueden custodiar en un registro de
   release externo sin secretos; trasladarlas no requiere actualizar CloudFormation.
   Este ejemplo requiere BACKEND_REPOSITORY/INGRESS_REPOSITORY y los dos DIGEST ya
   obtenidos y verificados, además de las variables/rutas/volumen de esta guía:

   ```sh
   export DOCKER_CONFIG
   export BACKEND_IMAGE="$BACKEND_REPOSITORY@$BACKEND_DIGEST"
   export INGRESS_IMAGE="$INGRESS_REPOSITORY@$INGRESS_DIGEST"
   REGISTRY=${BACKEND_REPOSITORY%%/*}
   aws ecr get-login-password --region "$AWS_REGION" |
     docker --config "$DOCKER_CONFIG" login --username AWS --password-stdin "$REGISTRY"
   docker compose -f docker/production/compose.yml config --quiet
   docker compose -f docker/production/compose.yml pull
   docker --config "$DOCKER_CONFIG" logout "$REGISTRY"
   docker compose -f docker/production/compose.yml up -d --no-build
   docker compose -f docker/production/compose.yml exec ingress /opt/readiness.sh
   ```

   En este host DOCKER_CONFIG también debe ser un directorio privado en tmpfs preparado
   por el operador. El login usa aquí el rol de instancia de pull; no copiar la sesión
   del operador ni ampliar IAM. El permiso de pull continúa
   restringido por ARN a BackendRepository e IngressRepository creados por esta plantilla.
   Un Output de CloudFormation no configura por sí solo variables de un proceso/Compose.
   Compose exige ambas imágenes; con variables ausentes falla antes de crear servicios.

## Arranque, comprobación y actualización

Construir fuera de EC2 siguiendo el README raíz, publicar en ECR bajo autorización separada
y registrar digests por revisión. Docker login usa password-stdin y DOCKER_CONFIG en tmpfs;
no escribir tokens en EBS/checkout. En EC2 seleccionar solo las referencias ECR@sha256 aprobadas.

```sh
docker compose -f docker/production/compose.yml config --quiet
docker compose -f docker/production/compose.yml up -d --no-build
docker compose -f docker/production/compose.yml ps
docker compose -f docker/production/compose.yml exec ingress /opt/readiness.sh
```

No volcar config/environment de procesos. Ingress ejecuta nginx -t y espera GET backend:8080/api/projects
200/JSON con timeout5s/cuerpo descartado antes de abrir HTTPS. Health cada30s/startup120s;
PG pg_isready complementa Flyway, no acredita autenticación/esquema por sí solo. No Actuator.
Comprobar desde fuera raíz/deep link/assets y /api/projects con credencial por entrada segura
de cliente (sin literal en historial) y sin ella401. Origin obligatorio en escrituras.
Solo estos tests externos futuros acreditarán publicación real; no se consulta AWS ahora.

Para actualizar: cerrar ingress, esperar Scans activos terminales mediante API autorizada,
detener backend antiguo, respaldar datos/AES, seleccionar digest nuevo y arrancar una única
instancia; comprobar readiness y reabrir ingress. No rolling, scale>1 ni segundo stack con BD
compartida. Recovery tras caída conserva la semántica existente, sin prometer retry de Scans.
No down -v ni limpieza automática. Rollback solo a artefacto compatible; para esquema incompatible
requiere restauración/plan aprobado, nunca migración inversa improvisada.

Backup: con aprobación, snapshot cifrado coherente con PG detenido o pg_dump protegido fuera
de Git; custodiar la misma AES separadamente. Probar restore en volumen/entorno aislado antes de
afirmar recuperabilidad. CloudFormation retiene datos/ECR; conservarlos puede seguir generando coste
con EC2 parada. El despliegue y URL reales comunicados constan en la evidencia enlazada;
AMI real e imágenes por digest aceptadas por review. Contexto económico Free Plan/créditos,
ventana hasta finalizar evaluación/entrega y responsable operativo documentados arriba.
6.1 completada con confirmación de Certbot en Docker + DNS-01; solo queda restore aislado,
recarga de proyecto o reinicio simple no
constituyen restore. Entrega administrativa de credenciales y TFM completo no acreditados.

## Pruebas sin AWS

Tras construir ambas imágenes, ejecutar `python docker/production/tests/verify.py` con
cryptography disponible. Usa certificados autofirmados y contraseñas aleatorias exclusivamente
sintéticas, fuera de Git, HTTPS loopback18443 y proyecto/volumen nuevos. Retira material secreto
de fixture al terminar y conserva el volumen de prueba para inspección; se identifica sin imprimir
secretos en evidence/local-validation.json.
No elimina datos compartidos ni activa evaluator/demo. No usar esas fixtures en AWS.
