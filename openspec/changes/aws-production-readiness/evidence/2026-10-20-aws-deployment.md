# Despliegue real AWS — 2026-10-20

**Estado: READY FOR REVIEW documental; despliegue operativo observado por el operador, aceptación completa del change pendiente.** Fuente: relato de observaciones reales comunicado por el operador en la solicitud de consolidación. Este registro no es una nueva inspección de AWS ni una transcripción de salidas/capturas conservadas. No se consultaron servicios, secretos ni el dominio durante esta tarea. La [validación local anterior](result.md) se conserva separada y no se presenta como ejecutada en AWS.

El review final comunicado por el operador acredita 1.1 y 6.2 completas y 6.4 completa en preparación técnica. Este registro incorpora esa aceptación sin una nueva inspección ni publicar identificadores internos. Acceso académico preparado no equivale a credenciales entregadas al tribunal.

## Infraestructura y publicación observadas

| Elemento | Observación comunicada |
| --- | --- |
| Región | `eu-north-1`, Europe (Stockholm) |
| URL pública | <https://llmscannerseguridad.com>, requiere credenciales de prueba Basic Auth |
| Host | Una EC2 `m7i-flex.large`, AMI real Amazon Linux 2023 x86_64 acreditada por review; identificadores operativos omitidos |
| Runtime | Docker 25.0.14; Docker Compose v2.40.3 |
| Servicios | Nginx/Angular, Spring Boot y PostgreSQL 16 |
| Administración | Systems Manager; SSH cerrado |
| Exposición | Solo 443 público; backend 8080 y PostgreSQL 5432 no públicos ni publicados al host por sus contenedores |
| Imágenes | Backend e ingress publicados en ECR privado por digest, acreditados por review; URI con Account ID omitidas en documentación pública |
| Estado final | PostgreSQL healthy, backend Up, ingress healthy; ingress expone 443 |
| Exclusiones | Demo vulnerable no desplegada; evaluator OpenAI desactivado por defecto; ningún Scan ejecutado en AWS |

### Incidencia de provisioning

El primer intento CloudFormation con `t3.medium` terminó en rollback porque no era Free Tier eligible para esta cuenta/región. Tras corregir la plantilla, el segundo stack terminó `CREATE_COMPLETE` con la selección `m7i-flex.large`. El review acredita infraestructura provisionada, imágenes publicadas, secretos cargados, Compose arrancado, PostgreSQL healthy, backend operativo, ingress healthy, HTTPS real y persistencia básica. No se publican IDs del stack, instancia o cuenta. La aceptación de 6.2 no acredita restore.

## Contexto operativo aprobado

Confirmación del operador para cierre de 6.1: despliegue AWS autorizado para la entrega académica del TFM, en cuenta AWS Free Plan/créditos, región `eu-north-1`. No se autoriza voluntariamente migrar a Paid Plan durante esta entrega. No se ha medido ni se afirma gasto monetario exacto o coste cero. Ventana operativa: mantener https://llmscannerseguridad.com disponible hasta finalizar entrega/evaluación académica; después podrán detenerse o retirarse los recursos si ya no son necesarios, sin ejecutar esas operaciones en esta tarea. Responsable operativo/TLS: operador del proyecto. Renovación manual mediante Certbot en Docker + DNS-01, procedimiento ya validado según confirmación del operador. Las credenciales Basic Auth se entregarán únicamente por el canal/campo privado de credenciales de la entrega académica, nunca en GitHub ni documentación pública; la entrega efectiva sigue sin acreditarse y es administrativa, no una validación técnica pendiente de 6.4.

## Persistencia y secretos

EBS adicional de **30 GB**, XFS, montado en `/srv/llm-scanner/postgres`; montaje persistente por UUID en `/etc/fstab` y volumen Docker externo `llm-scanner-pgdata`. El UUID literal se omite por innecesario para la documentación pública. Crear y releer un proyecto acredita persistencia funcional, no recuperación tras pérdida del host: restore aislado pendiente.

Parameter Store **SecureString**, con los siguientes nombres no secretos:

- `/llm-scanner/pro/admin-password`
- `/llm-scanner/pro/app-password`
- `/llm-scanner/pro/target-aes`
- `/llm-scanner/pro/htpasswd`

Bootstrap a `/run/llm-scanner` en tmpfs y permisos segregados por UID. No se incluyen valores, hashes htpasswd, credenciales AWS, tokens ECR ni material privado TLS.

## DNS, TLS e incidencia ingress

Cloudflare mantiene el registro A del dominio raíz hacia la IP pública de EC2, en modo **DNS only**, sin publicar aquí la IP directa. Se utilizó TXT ACME temporal para validación manual **DNS-01** de Let's Encrypt. Certificado para `llmscannerseguridad.com`, expiración observada **2027-01-18**. No se registra su private key. La renovación **no es automática** y corresponde al operador del proyecto: confirma disponibilidad y procedimiento validado con Certbot en Docker + DNS-01. Esta confirmación satisface el último pendiente de 6.1; no se reejecutó la renovación ni se accedió a AWS para documentarla.

Ingress reiniciaba inicialmente por falta de permisos de lectura de `privkey.pem`. Se corrigieron ownership/permisos para hacerlos compatibles con **UID 101** y después quedó healthy. Es una incidencia operativa de permisos del montaje TLS resuelta, no un fallo del core. No constan modos numéricos ni comandos exactos; no se inventan.

## Validación externa real comunicada

- `curl -I https://llmscannerseguridad.com` devolvió **401 Unauthorized**, coherente con Basic Auth sin credencial.
- Login desde Chrome confirmado; Angular cargó correctamente y `/api` llegó a Spring Boot.
- Un proyecto creado desde Internet se persistió en PostgreSQL y siguió existiendo al recargar. No se aportaron ID, cuerpo API ni captura; no se inventan.
- `TARGET_NOT_CONFIGURED` observado y esperado al no existir target configurado. No acredita ejecución de pruebas de seguridad.
- No se ejecutó Scan, no se desplegó demo vulnerable ni se llamó a OpenAI en AWS. La [validación real local del evaluator](../../real-evaluator-validation/evidence/2026-10-19-tls-revalidation/result.md) conserva su alcance local.

El review acepta aislamiento, acceso externo y persistencia básica. No se repiten como pendientes ni se atribuyen a AWS las pruebas locales F1–F4. 6.3 queda parcial exclusivamente por restore aislado pendiente: recarga de proyecto, persistencia tras refresh y reinicio simple no constituyen restore.

## Revisión de cierre de tareas

| Tarea | Estado y evidencia restante |
| --- | --- |
| 1.1 | COMPLETED por review: AMI real, AL2023 x86_64, instancia m7i-flex.large, Docker25.0.14, Compose v2.40.3 e imágenes backend/ingress publicadas por digest acreditados. Omitir identificadores operativos no reabre la tarea. |
| 6.1 | COMPLETED: confirmadas autorización académica, Free Plan/créditos sin migración voluntaria a Paid Plan, ventana, responsable y disponibilidad/procedimiento validado de Certbot en Docker + DNS-01. Región, dominio, acceso y demás parámetros ya acreditados por review; canal/campo privado de entrega definido. Se satisface íntegramente el contrato sin afirmar gasto exacto ni credenciales ya entregadas. |
| 6.2 | COMPLETED por review: CREATE_COMPLETE, infraestructura, imágenes, secretos, Compose y tres servicios operativos, HTTPS real y persistencia básica acreditados. |
| 6.3 | PARTIAL exclusivamente por restore aislado pendiente/no ejecutado, pospuesto expresamente por el operador. No reanudar sin nueva autorización. Refresh, persistencia básica o reinicio simple no acreditan restauración. [Decisión y evidencia](2026-10-20-restore-validation.md). |
| 6.4 | COMPLETED en preparación técnica: URL operativa, Basic Auth, acceso externo y documentación reproducible acreditados. Credenciales entregadas al tribunal: no acreditado, acción administrativa separada. |

**24/25 tareas completas.** Solo 6.3 sigue parcial por restore aislado pendiente. No se acreditan gasto exacto, recuperabilidad ni entrega TFM completa. Las credenciales de prueba deben entregarse por canal privado, nunca en el repositorio.

Decisión posterior del operador: posponer esta validación operativa adicional para evitar
riesgos en la producción estable durante la fase final del TFM, ante falta de acceso
administrativo del ejecutor y dificultad del procedimiento guiado por Session Manager.
Según el operador, 6.3 no es un requisito oficial obligatorio de entrega; permanece
pendiente dentro del change, sin excepción de aceptación ni cierre. No se ejecutaron
backup/restore, no se modificó producción ni se detuvieron servicios; sin Scans/OpenAI.

Documentación pública: se conservan dominio, región, tipo de instancia, SO y arquitectura. Cuando un procedimiento necesite identificadores, usar `<aws-account-id>`, `<instance-id>`, `<public-ip>` y `<aws-account-id>.dkr.ecr.eu-north-1.amazonaws.com/<service>@sha256:<digest>`, sin publicar valores reales innecesarios ni secretos.

Esta consolidación no cambia código, configuración desplegada ni requisitos. No ejecuta despliegues, reinicios, tests de producto, Scans, llamadas OpenAI, commits o pushes. OpenSpec strict se registra en [tasks.md](../tasks.md).
