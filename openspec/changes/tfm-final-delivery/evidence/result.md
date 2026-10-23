# Auditoría documental y README — fases A+B

Fecha: **2026-10-23**, Europe/Madrid. Estado: **READY FOR REVIEW de A+B**; auditoría documental **PASS tras corregir los findings siguientes** y README **READY para este alcance**. La entrega completa no es DONE. Revisión examinada: working tree local de `Readme.md`, documentos de `docs/08-entrega-tfm/`, artefactos de este change y fuentes técnicas/evidencias referenciadas; no se ha aportado ni verificado revisión GitHub candidata en esta fase.

Estado posterior vigente de Fase C: **READY FOR REVIEW, 6/18 completas**. Repositorio privado justificado, revisión public-safe local PASS e invitación enviada al evaluador, pendiente de aceptación por tercero. 3.1 completa; 3.3 pendiente exclusivamente de esa aceptación. Los resultados A+B siguientes son históricos; el detalle de C figura al final.

Autorización: solicitud del operador de ejecutar exclusivamente A+B. No se modifica código, infraestructura ni tareas de otros changes. No se ejecutan builds, tests de producto, servidores, Scans, llamadas OpenAI, accesos AWS o comprobaciones externas, ni se crean slides/vídeo. No se revisa el historial para certificar publicación segura: esa aceptación pertenece a C y debe abarcar material nuevo.

## Matriz de obligaciones oficiales

Fuente normativa: [requisitos-oficiales.md](../../../../docs/08-entrega-tfm/requisitos-oficiales.md). Correspondencia con O1–O7 de [D-01](../design.md#d-01--matriz-de-obligaciones-y-evidencia). Los estados siguientes separan contenido documental de aceptación final; no sustituyen los requisitos ni su transcripción.

| Obligación | Fuente y evidencia existente | Estado después de A+B | Pendiente concreto y procedencia |
| --- | --- | --- | --- |
| O1 README detallado | Requisitos: README obligatorio; [Readme.md](../../../../Readme.md), [preparación local 2026-10-20](../../aws-production-readiness/evidence/result.md) | Contenido A+B READY: descripción/objetivo, stack, instalación, ejecución, estructura, funcionalidades y modo privado de acceso cubiertos. Instrucciones revisadas, no reejecutadas. | Las credenciales se facilitarán por canal privado. GitHub privado documentado en C, invitación enviada y aceptación pendiente; slides/vídeo siguen placeholders. Confirmación administrativa de la entrega solo en H; contenido no equivale a paquete final completo. |
| O2 Código en GitHub accesible | Requisitos: código fuente (admite privado justificado); [repositorio privado](https://github.com/NitaProjects/llm-scanner-seguridad), revisión candidata e inventario registrados en C | Acceso preparado: invitación enviada según operador; revisión public-safe working tree/historial PASS. 3.1 completa. | 3.3 pendiente exclusivamente de aceptación por el destinatario; no se exige hacer público el repositorio ni se confunde invitación con acceso efectivo. |
| O3 Despliegue y URL real | Requisitos: despliegue; [AWS 2026-10-20](../../aws-production-readiness/evidence/2026-10-20-aws-deployment.md) | Publicación y preparación técnica acreditadas por observaciones/review del operador. URL canónica documentada. | D: comprobación final autorizada de TLS/Basic Auth/UI/lectura API desde sesión nueva; no realizada ahora. H: confirmación administrativa separada, sin reabrir aceptación técnica histórica. |
| O4 Slides válidas o enlace | Requisitos: presentación; inventario de la carpeta de entrega y [enlaces](../../../../docs/08-entrega-tfm/enlaces-entrega.md) | PENDIENTE; placeholder explícito, no archivo/enlace entregable acreditado. | E: crear, revisar, comprobar apertura y acceso, añadir enlace real a README/directorio/formulario. No se genera material en A+B. |
| O5 Vídeo propio con pantalla y URL | Requisitos: vídeo; inventario de entrega y enlaces | PENDIENTE; placeholder explícito, sin vídeo publicado acreditado. | F–G: guion, grabación del alumno con pantalla, revisión y reproducción del enlace final; rostro opcional. Un guion/slides no completa O5. |
| O6 Directorio con docs, despliegue y slides/enlace | Requisitos: contenido del directorio; [docs](../../../../docs/README.md), README y [guía de producción](../../../../docker/production/README.md) | PARCIAL: documentación e información de despliegue presentes. | E/H: falta presentación o referencia válida y revisión del paquete final. No exigir formatos duplicados ni memoria ampliada no requerida por O1–O7. |
| O7 Formulario final | Requisitos: formulario final; deployment conocido y resto de enlaces pendientes | PENDIENTE. No hay confirmación de formulario o envío administrativo. | H: autor completa nombre/email y URLs reales, entrega credenciales en canal privado y confirma fecha/canal sin valores. Sin datos personales en Git ni envío automático. |

## Findings y correcciones documentales

| Finding | Discrepancia observada | Corrección aplicada / estado |
| --- | --- | --- |
| AB-01 | README presentaba E2E/demo/frontend todavía sin integración; trazabilidad marcaba instalación/ejecución pendientes. | RESOLVED documentalmente: enlazadas evidencias posteriores locales y de instalación. Se conservan los cierres formales históricos y límites de safety/Docker; no se completan tasks ajenos. |
| AB-02 | Enlaces de checklist/trazabilidad usaban `../../README.md`, distinto del nombre real `Readme.md` en GitHub/Linux. | RESOLVED en los documentos afectados; no se duplica ni renombra el archivo. Se conserva `README.md` en la transcripción normativa. |
| AB-03 | README mezclaba emisión TLS fuera de contenedores con la confirmación posterior de Certbot en Docker. | RESOLVED en README: Certbot en Docker con DNS-01 manual, separado de servicios de aplicación; no cambia despliegue/guía ni ejecuta renovación. |
| AB-04 | Acceso repetido con fórmulas ambiguas sobre credenciales «entregadas» y estado administrativo ligado a despliegue. | RESOLVED: antes de H se usa «las credenciales se facilitarán por canal privado», sin valores ni afirmación de entrega efectiva. Preparación técnica y confirmación administrativa separadas. |
| AB-05 | Contenido de evolución futura extenso y acceso duplicado dificultaban localizar funcionalidad actual; enlaces vacíos sin explicación. | RESOLVED: evolución resumida como no implementada, sección de acceso única y placeholders explícitos GitHub/slides/vídeo. No se fabrican URLs. |
| AB-06 | Checklist mezclaba benchmark/memoria ampliada con pendientes necesarios de entrega y podía presentar restore como bloqueo. | RESOLVED: obligaciones O1–O7 separadas de extras; restore 6.3 expresamente pospuesto/no ejecutado y no bloqueante. Sin modificar su task ni afirmar recuperabilidad. |
| AB-07 | README remitía detalles de demo y no explicitaba en el arranque local la clave AES requerida. | RESOLVED: añadidos arranque demo, configuración necesaria, recorrido local y tabla de variables, conservando builds existentes y secretos fuera del repositorio. No se han ejecutado instrucciones. |

No quedan findings documentales abiertos dentro de A+B. Los pendientes O2–O7 son trabajo previsto de C–H, no resultados inventados ni motivo para ejecutar fases no solicitadas. Esta auditoría no certifica ausencia global de secretos ni acceso público a recursos externos.

## Evidencia técnica y límites conservados

- [E2E offline 2026-10-16](../../e2e-integration/evidence/2026-10-16-005150/result.md): integración scanner→demo y API/UI con exportación desactivada. [Cierre operacional](../../e2e-integration/tasks.md#12-cierre-operacional-autorizado--tarea-64-2026-10-16): conserva review formal pendiente; no se altera.
- [UI 2026-10-19](../../real-evaluator-validation/evidence/2026-10-19-ui-real/result.md): recorrido y Finding visible con cuatro componentes LLM fallidos, consolidación determinista degradada. No acredita éxito del proveedor ni cierra frontend 9.4 desde este change.
- [OpenAI local 2026-10-19](../../real-evaluator-validation/evidence/2026-10-19-tls-revalidation/result.md): cuatro respuestas reales aceptadas de gpt-6-luna, híbrido sin degradación; usage 3366/2398 tokens, coste NOT_INSPECTED. No se repite frontend 9.4 en esa ejecución ni se demuestra retrospectivamente la causa del Scan histórico fallido.
- [Demo](../../demo-vulnerable/tasks.md): implementada con QA local; procedimientos de Docker y safety externo preparados, ejecución no acreditada por las evidencias inspeccionadas. No se presenta como target AWS ni como prueba de generalización.
- [AWS 2026-10-20](../../aws-production-readiness/evidence/2026-10-20-aws-deployment.md): publicación y persistencia básica según operador/review; sin Scan/demo/OpenAI en AWS. 24/25, restore aislado pendiente/no ejecutado y pospuesto expresamente. No se reabre ni se convierte en requisito oficial de entrega.

## Comprobaciones y progreso

Lectura documental dirigida, contraste de fuentes fechadas y revisión de **142 referencias locales**: ningún destino ausente ni discrepancia del case del fichero en los ocho documentos de A+B. No se comprueba disponibilidad de URLs externas ni se certifican automáticamente todos los fragmentos de encabezado. La transcripción de obligaciones oficiales se conserva; solo se actualizan sus estados factuales. No se interpreta OpenSpec strict como revisión de seguridad, prueba de producto ni entrega académica realizada.

A: 1.1/1.2 completadas. B: 2.1/2.2 completadas. Total **4/18 tareas** de `tfm-final-delivery`; C–H pendientes. README READY para revisión de contenido A+B, sin ocultar que faltan entregables/enlaces finales. Única validación CLI de aceptación solicitada: `openspec validate tfm-final-delivery --strict`, resultado en [tasks.md](../tasks.md).

OpenSpec strict: **PASS**, exit 0, `Change 'tfm-final-delivery' is valid`. Sin hallazgos documentales abiertos en el alcance solicitado. **READY FOR REVIEW**, sin declarar DONE ni ejecutar C–H.

## Fase C — Revisión pública local y acceso GitHub — 2026-10-23

Autorización: exclusivamente Fase C, revisión local redactada de secretos e historial y lecturas anónimas de GitHub. El autor confirma URL candidata desde `origin` y revisión candidata `HEAD`. No autoriza publicación, push, rotación ni saneamiento histórico. Esta sección actualiza el estado posterior a A+B; no sustituye sus resultados históricos.

### Revisión examinada e inventario

- Rama: `feature/primera-version`. HEAD: `ecee9cf41836b679fc5cfa53865f5615695510d0`.
- Origin SSH sin credenciales embebidas; URL candidata HTTPS: [NitaProjects/llm-scanner-seguridad](https://github.com/NitaProjects/llm-scanner-seguridad).
- Working tree inicial: seis documentos modificados (`Readme.md` y los cinco documentos de entrega) y seis archivos no trackeados bajo este change. 689 archivos tracked y seis no trackeados: **695 archivos publicables inspeccionados**. Ningún cambio de producto o infraestructura durante C.
- Historial: **18 commits alcanzables mediante `git rev-list --all` y 855 blobs únicos**, incluidos estados antiguos de documentos y archivos que ya no estén en el árbol actual; mensajes de los 18 commits revisados también. No incluye objetos inalcanzables, historial remoto no disponible localmente, reflogs ni archivos privados externos al repositorio.
- Binarios del árbol: 43 capturas de UI y un favicon. Inspección visual de capturas, procedencia de fixtures y claves de metadatos; sin credenciales observadas. El inventario histórico no contiene versiones binarias adicionales de imágenes ni archivos PDF, slides, vídeos o archivos comprimidos ausentes del árbol actual. No se usa OCR como garantía de ausencia de secretos.
- `.gitignore` raíz y específicos backend/frontend/demo/producción revisados: `.env`, claves, htpasswd y directorios de secretos/certificados de producción excluidos. `.env` local presente e ignorado: **contenido no abierto ni certificado**, fuera del conjunto publicable. Sin nombres de archivos de secretos trackeados, no trackeados publicables o históricos; sin dumps/backups/keystores auxiliares detectados en el inventario revisado. No se incorporan archivos ignorados a Git.
- Wrappers, lockfile, migraciones V1–V7, manifests/schemas, Dockerfiles y guías necesarios para reproducir el proyecto están presentes. Los documentos A+B y este change aún no forman una revisión entregable publicada; slides/vídeo siguen pendientes de sus fases, no se consideran artefactos perdidos.

### Clasificación de seguridad

Revisión mediante lectura de blobs y búsquedas locales con salida exclusivamente redactada: patrones de claves OpenAI, AWS, GitHub y otros tokens, material privado PEM, hashes htpasswd/bcrypt, Authorization Basic/Bearer, URLs con credenciales, asignaciones de passwords/API keys/AES, campos de secretos y literales codificados. Coincidencias contrastadas por contexto de código/documentación, sin registrar valores, hashes de credenciales ni fragmentos sensibles. No se instala ninguna herramienta adicional ni se ejecutan tests del producto.

| Categoría | Resultado y clasificación |
|---|---|
| Passwords, API keys y tokens reales, incluidos OpenAI/AWS | No detectados en el conjunto publicable ni en los blobs/mensajes históricos revisados. Coincidencias: referencias de entorno, variables y fixtures de tests, no credenciales operativas. |
| AES / SecureString | Nombres de parámetros, rutas y código de carga/generación; no valores persistidos detectados. Configuración de aplicación toma secretos del entorno o de archivos externos. |
| TLS privado / htpasswd | No material privado ni hashes operativos detectados. Los tests generan material efímero fuera del checkout; no se confunde el código generador con un secreto persistido. |
| Basic Auth | Ningún usuario+password ni Authorization real detectado en README/docs/evidencia; referencias a Basic Auth y autenticación de fixtures no son credenciales del deployment. |
| Account ID, Instance ID, IP pública, AMI y ECR URI | Identificadores operativos, no secretos por sí solos; sin riesgo concreto adicional acreditado. No bloquean ni justifican reescritura histórica. |
| Rutas locales | Diez coincidencias bajo OpenSpec histórico/interno; ninguna en `Readme.md` ni `docs/`. No son secretos ni instrucciones portables de instalación. Su minimización es observación no bloqueante; no se exige saneamiento histórico. |
| Deployment y entregables | La URL pública puede permanecer. Credenciales únicamente «se facilitarán por canal privado»; slides/vídeo pendientes y entrega administrativa no realizada. La evidencia local del evaluator no se atribuye a AWS. |

**Working tree public-safe: PASS. Historial local alcanzable public-safe: PASS. Secretos reales encontrados: ninguno.** Resultado limitado al ámbito y métodos descritos, no garantía sobre archivos privados/ignorados ni cambios futuros. No se ha identificado saneamiento adicional necesario por secretos antes de hacer público el contenido revisado. El control debe repetirse sobre material nuevo y la revisión que finalmente se publique.

### GitHub y pendientes reales

Comprobación HTTPS sin autenticación ni credenciales Git locales, autorizada expresamente. El entorno restringido bloqueó inicialmente la red; la lectura se repitió con autorización de acceso. Resultados:

| Recurso | Resultado anónimo |
|---|---|
| `https://github.com/NitaProjects/llm-scanner-seguridad` | HTTP 404 |
| Revisión candidata `ecee9cf41836b679fc5cfa53865f5615695510d0` mediante `/tree/…` | HTTP 404 |
| `Readme.md` de esa revisión mediante raw.githubusercontent.com | HTTP 404 |
| `docs/08-entrega-tfm/checklist-entrega.md` de esa revisión mediante raw.githubusercontent.com | HTTP 404 |

La confirmación posterior del operador permite interpretar esos 404: comportamiento esperado sin autenticación para un repositorio **privado deliberadamente**, no indicio de obligación de hacerlo público. Justificación: proyecto de seguridad con documentación/configuración operativa; se prefiere acceso controlado, opción admitida por el requisito oficial. No se realiza otra consulta GitHub en esta actualización.

El operador confirma que **`mouredev@gmail.com` ya fue añadido como colaborador y la invitación está enviada, pendiente de aceptación en GitHub**. Fuente: confirmación del operador; no comprobación autenticada nueva ni envío de invitación por el agente. Se registra el destinatario expresamente autorizado para trazabilidad de la entrega, sin tokens, credenciales ni enlaces privados de invitación.

- **3.1 COMPLETED:** URL, revisión candidata `ecee9cf41836b679fc5cfa53865f5615695510d0` e inventario de contenido/historial ya conservados; no exige aceptación de un tercero.
- **3.2 COMPLETED / PASS:** revisión public-safe del working tree e historial local alcanzable ya realizada, sin secretos reales detectados dentro del ámbito descrito. No se reejecuta ni se presenta como garantía sobre cambios futuros.
- **3.3 PENDING exclusivamente por aceptación del destinatario:** la preparación técnica exigible al operador está realizada mediante invitación, pero la aceptación y el acceso efectivo dependen del tercero. No se afirma aceptación, lectura de la revisión o acceso ya utilizado, ni se exige visibilidad pública. La comprobación posterior corresponde al acceso autorizado del evaluador.

Progreso vigente: **6/18 tareas completas**. README/enlaces y referencias de Fase C actualizados. D–H no ejecutadas; no se declara publicación de cambios locales ni entrega académica completa. **READY FOR REVIEW**, con dependencia de tercero explícita y sin blocker de secretos detectado. Se conserva el strict PASS anterior; el resultado de esta actualización documental se registra en tasks.md. Sin código, AWS, Scans, cambio de visibilidad, invitaciones nuevas, commit/push ni accesos externos nuevos.
