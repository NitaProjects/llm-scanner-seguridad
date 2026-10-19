# ADR-009: preparación académica de una EC2

Estado: aprobada para preparación mediante aws-production-readiness; despliegue y URL
reales pendientes de autorización y evidencia. Fecha: 2026-10-20.

## Contexto

Se necesitan acceso académico, límite previo de recepción HTTP y una instalación
reproducible sin ampliar el core ni publicar el target deliberadamente vulnerable.
Contrato detallado: [design.md](../../../openspec/changes/aws-production-readiness/design.md).

## Decisión

Una EC2 Linux x86_64 y tres contenedores: Nginx/Angular estático, backend Spring Boot
single-instance con profile pro y PostgreSQL16.443 público con TLS/Basic Auth, sin
dependencia de la IP del tribunal; restricción IP opcional desactivada. Sin22/80/8080/5432
públicos ni IPv6 inicialmente. Administración Session Manager, IMDSv2 obligatorio/hop1.

Compose de producción separado del local. app/data internal, ingress fuera de data,
backend sin puertos publicados, PostgreSQL solo data. Origin canónico en escrituras,
Authorization retirado por proxy, cuerpo1MiB/buffering/idle15s y recursos acotados.
Health usa GET /api/projects y pg_isready, sin Actuator ni nuevos endpoints.

Secretos pequeños en Parameter Store SecureString/KMS mediante bootstrap del host a tmpfs,
con permisos por destinatario; la misma AES se custodia independientemente del volumen PG.
TLS externo Let's Encrypt/Certbot manual DNS-01, sin ACME/puerto80/cuarto contenedor.
Volumen PG externo sobre EBS cifrado/retención y rol app sin privilegios globales.

Demo y evaluator real se demuestran localmente; no se despliega demo ni se habilita OpenAI
por defecto. SSRF pro permanece sin excepciones privadas. No se importan históricos locales.

## Consecuencias y validación

Se acepta un único punto de fallo y actualización stop/start, sin solapamiento de backend.
La preparación local no acredita publicación AWS, entrega TFM ni recuperabilidad cloud.
La guía operativa y las evidencias están en [docker/production](../../../docker/production/README.md)
y [tasks.md](../../../openspec/changes/aws-production-readiness/tasks.md).
Antes de desplegar hacen falta región/cuenta/AMI/root device, dominio/DNS, certificado,
digests de aplicación, custodia/transferencia de secretos, presupuesto y prueba de restore.

## Fuera del MVP / Evolución futura

RDS, ALB/ACM, Cognito/OIDC, HA, backups continuos, SQS/workers, WAF, automatización ACME y
publicación de demo requieren cambios propios. Esta decisión no resuelve otros entornos
PRE/PRO ni rotación de secretos o retención científica pendientes.
