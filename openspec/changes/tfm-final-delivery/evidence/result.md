# Auditoría documental y README — fases A+B

**Estado vigente al 2026-10-24: Fase E COMPLETED, 10/18 tareas completas, READY FOR PHASE F.** PPTX/PDF finales revisados y URL oficial de slides accesible sin login, ver cierre de E y `slides-validation.json`. Las secciones anteriores conservan sus resultados históricos. 3.3 pendiente de aceptación del destinatario; F–H y entrega efectiva de credenciales no completadas.

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
| O4 Slides válidas o enlace | [URL pública oficial](https://drive.google.com/file/d/15EqoH_KUZoZEyiApI_oVqTwvNv2c7TFE/view?usp=sharing), [PPTX](../../../../docs/08-entrega-tfm/slides/LLM-Scanner-Seguridad-TFM.pptx), [PDF](../../../../docs/08-entrega-tfm/slides/LLM-Scanner-Seguridad-TFM.pdf), [QA](slides-validation.json) | COMPLETADO: 10 slides finales revisadas; visor público abierto sin login ni solicitud de permisos el 2026-10-24. 5.1/5.2 completas. | URL utilizable para el formulario de H, todavía no enviado. Permisos lector/cualquiera con enlace e identidad final publicada confirmados por operador; no se cambian permisos. |
| O5 Vídeo propio con pantalla y URL | Requisitos: vídeo; inventario de entrega y enlaces | PENDIENTE; placeholder explícito, sin vídeo publicado acreditado. | F–G: guion, grabación del alumno con pantalla, revisión y reproducción del enlace final; rostro opcional. Un guion/slides no completa O5. |
| O6 Directorio con docs, despliegue y slides/enlace | Requisitos: contenido del directorio; docs, README, guía, archivos locales y enlace público oficial de slides | COMPLETADO para contenido exigido: documentación, información de despliegue y presentación/enlace presentes. | H conserva revisión del paquete completo y acceso a la revisión Git entregada; O6 no acredita formulario ni aceptación GitHub por tercero. |
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

## Fase D — Deployment y acceso preparados — 2026-10-23

Alcance autorizado: comprobación de lectura del deployment, HTTPS, Basic Auth, acceso funcional y preparación de credenciales; solo esta evidencia y tasks actualizados. No se ejecutan Scans/OpenAI, escrituras de datos funcionales, comandos AWS, cambios de configuración, código, publicación, commit/push ni envío de credenciales.

Revisión local: `0589a89807d2a6ee8988385a773a27df71088716`, rama `feature/primera-version`, idéntica a master local y working tree limpio al comenzar. Según el operador, ramas remotas sincronizadas con la versión final; no se realiza otra comprobación autenticada de GitHub ni se atribuye esta nueva revisión al inventario histórico anterior de C.

| Comprobación | Resultado | Fuente y límites |
|---|---|---|
| Deployment `https://llmscannerseguridad.com` | PASS | Responde directamente mediante HTTPS; HTTP 401 sin credencial es el resultado esperado. Carga autenticada confirmada por el operador en esta ejecución. |
| HTTPS | PASS | Handshake TLS 1.3 con contexto de confianza y verificación de hostname habilitados. Cadena y nombre aceptados; no se omiten controles TLS ni se descargan/guardan certificados o claves. |
| Basic Auth UI | PASS | GET `/` sin Authorization → HTTP 401, cabecera de desafío Basic presente. No se guardan cuerpos ni cabeceras de autenticación con credenciales. |
| Basic Auth API | PASS | GET `/api/projects` sin Authorization → HTTP 401 con desafío Basic. Solo lectura; ningún Scan ni escritura. |
| UI autenticada / lectura funcional | PASS, confirmación del operador | El navegador nuevo del agente carece de sesión y devuelve `ERR_INVALID_AUTH_CREDENTIALS`. El operador confirma posteriormente que introduce las credenciales, la UI carga correctamente y puede ver los proyectos existentes. No se afirma observación directa de la UI ni un HTTP 200 API autenticado medido por el agente. |
| URL y tratamiento documental del acceso | PASS | `Readme.md` y `docs/08-entrega-tfm/enlaces-entrega.md` contienen la URL canónica y «las credenciales se facilitarán por canal privado». Sin valores publicados en esos documentos. |
| Seguridad del material versionado | PASS en el ámbito revisado | Reutilización del PASS public-safe C y revisión dirigida de once documentos Markdown: README, entrega y change; sin coincidencias de claves privadas, claves de proveedor, passwords literales, hashes bcrypt ni Authorization Basic codificado. No se abren `.env` ni secretos; no se sustituye C por una garantía sobre otros cambios futuros o contenido remoto no inspeccionado. |
| Credenciales preparadas para H | PASS, confirmación del operador | Confirma que conserva las credenciales y las entregará por canal privado en H, sin compartir usuario ni contraseña en chat/documentación. Preparación comprobada por declaración expresa, sin leer valores ni afirmar entrega efectiva. |

Las primeras solicitudes de red desde el entorno restringido fallaron por resolución/conectividad; las comprobaciones TLS y GET se completaron tras autorización de acceso externo. Ese fallo del entorno de ejecución no se clasifica como fallo del deployment.

La invitación del repositorio privado permanece pendiente de aceptación por el destinatario: no se cierra 3.3 ni se exige visibilidad pública. Slides/vídeo y formulario siguen pendientes. No se atribuye validación OpenAI local a AWS ni se reactiva evaluator/demo. Restore 6.3 sigue pospuesto/no bloqueante.

**4.1 COMPLETED; 4.2 COMPLETED. Deployment PASS; HTTPS PASS; Basic Auth PASS; documentación PASS. Total 8/18 tareas. READY FOR PHASE E**, sin declarar DONE del TFM. `openspec validate tfm-final-delivery --strict`: **PASS**, exit 0, `Change 'tfm-final-delivery' is valid`.

## Fase E — Preparación exclusiva de contenido — 2026-10-23

El operador solicita contenido definitivo de unas 8–10 slides, sin generar todavía PPTX/PDF. Se prepara [contenido-slides.md](../../../../docs/08-entrega-tfm/slides/contenido-slides.md): **10 slides**, cada una con título, mensaje principal, contenido visible breve, visual/diagrama recomendado y notas orales de 20–40 segundos. Secuencia: portada → problema/objetivo → solución/flujo → arquitectura → categorías → motor híbrido → validación local → controles del scanner → deployment AWS → conclusiones/límites. Duración estimada: **5 min 20 s**, aproximadamente **6 min con transiciones**, no un requisito oficial.

Fuentes conservadas: README/arquitectura/metodología y evidencias E2E local, recorrido UI, revalidación OpenAI y deployment/D. El dato «4/4» se limita a cuatro casos sintéticos del perfil VULNERABLE en la validación local: cuatro respuestas LLM aceptadas y cuatro consolidaciones vulnerables sin degradación. No se convierte en precisión/recall ni benchmark. El recorrido UI degradado y la revalidación posterior sin repetición de frontend se distinguen en notas y fuentes. El score permanece heurístico.

Deployment: demo no desplegada y evaluator OpenAI desactivado, sin Scans AWS. Se mantiene URL pública y «las credenciales se facilitarán por canal privado», sin afirmar entrega efectiva. Contenido revisado sin secretos, material privado TLS, credenciales, emails ni identificadores operativos innecesarios; diagramas recomendados en lugar de capturas de consola. No se certifican metadatos/apertura de un deck inexistente. Restore 6.3 no se reabre ni se incorpora como bloqueo de las slides.

**5.1 parcial por preparación de contenido; 5.2 pendiente. Total 8/18**, sin nuevas casillas completas. Falta crear presentación real, abrir todas las slides, revisar el artefacto y entregar/verificar archivo o enlace. No se actualizan README/enlaces como si ya existiese presentación publicada. Sin PPTX/PDF, imágenes, código, AWS, servicios, Scans/OpenAI, vídeo, commit/push o publicación. `openspec validate tfm-final-delivery --strict`: **PASS**, exit 0, `Change 'tfm-final-delivery' is valid`. **READY FOR SLIDE CREATION**.

## Fase E — Creación y validación de artefactos — 2026-10-23

Autorización posterior explícita del operador para generar los dos archivos a partir de contenido-slides.md, sin modificar hechos, métricas o notas aprobadas. Entregables: [PPTX](../../../../docs/08-entrega-tfm/slides/LLM-Scanner-Seguridad-TFM.pptx) y [PDF](../../../../docs/08-entrega-tfm/slides/LLM-Scanner-Seguridad-TFM.pdf). Identidad por tamaño y SHA-256 en [slides-validation.json](slides-validation.json), comprobada también al copiar los archivos finales validados.

| Comprobación | Resultado y límites |
| --- | --- |
| PPTX | PASS: paquete íntegro, 10 slides, formato 16:9, Segoe UI, texto/diagramas editables. Reimportación del archivo final con Artifact Tool y render de las 10 slides; no se afirma ejecución de Microsoft PowerPoint. |
| Notas | PASS: 10 partes de speaker notes, texto de notas aprobado 10/10 coincidente; no aparece el texto completo de notas ni fuentes como contenido visible. |
| PDF | PASS: reabierto con pypdf, 10 páginas 960×540 puntos; render completo con Poppler y revisión individual. PDF estático de alta resolución, generado desde render 2560×1440 del PPTX final; texto no seleccionable. No incluye las notas. |
| Geometría | PASS: validadores de integridad, tamaño de slide, geometría, heading fit y política de fuentes sin findings. No equivalen por sí solos a QA visual. |
| Revisión visual | PASS: revisadas individualmente las 10 slides; reparadas y re-renderizadas 6 y 9. Comparación de las otras ocho páginas sin cambios visuales tras esa revisión. Sin overflow, texto cortado o problemas de legibilidad pendientes. |
| Contenido/seguridad | PASS en el material creado: «Fallo del evaluador ≠ fallo del Scan» y «4 casos controlados · no benchmark» visibles. AWS/laboratorio local separados, dominio aprobado, datos sin alteración; sin credenciales, identificadores operativos innecesarios, logins o material TLS privado añadidos. Metadatos revisados sin datos personales del operador. |

Incidencias de generación resueltas antes de entrega: declaración tipográfica inicial corregida; dirección de flechas corregida; margen inferior de slide 6 ampliado y flechas de slide 9 hechas explícitas. El finalizador necesitó ejecución autorizada fuera del sandbox porque Windows bloqueaba su enlace de archivo. El exportador PDF nativo del runtime falló por resolución de módulos de fuentes; se usó PDF estático a partir del render validado, sin instalar ni alterar dependencias. Estos hechos no son fallos del producto ni operaciones AWS.

**5.1 COMPLETED; 5.2 PARTIAL; total 9/18. READY FOR SLIDE REVIEW.** El resto del contrato de 5.2 exige revisión/entrega y comprobar archivo o enlace final accesible al evaluador; no se ha publicado, actualizado repositorio remoto ni verificado una URL de slides. No se cierran 3.3, F–H, restore ni otros changes. Sin código del proyecto, servicios, nuevas pruebas del producto, AWS, Scans/OpenAI, vídeo, commit/push ni entrega de credenciales. OpenSpec strict se registra al finalizar esta actualización.

`openspec validate tfm-final-delivery --strict`: **PASS**, exit0, `Change 'tfm-final-delivery' is valid`. Ambos artefactos y las 10 páginas están validados antes de actualizar el progreso; no se equipara esta validación con publicación o aceptación académica final.

### Retoques de review — slides 4 y 10

Por solicitud posterior del operador, se hace explícito el bloque «Puertos» entre Aplicación/Dominio y adaptadores en la slide 4. En la 10 se conserva el límite de cuatro escenarios y se precisa el pie: «score heurístico ≠ probabilidad de vulnerabilidad». Solo cambia esa representación y el pie; el Markdown fuente queda sincronizado. Notas 10/10 idénticas a la versión anterior y duración sin cambios.

Ambos artefactos actualizados tras validar la nueva revisión: PPTX reimportado y renderizado, PDF reabierto y renderizado con Poppler. Revisión visual de slides 4/10 PASS; comparación de las otras ocho páginas idéntica píxel a píxel respecto a la versión anterior, incluida la slide 7 solicitada sin cambios. Sin texto cortado ni desbordamientos. Nuevos hashes y detalle en `slides-validation.json`. Se conservan 10 slides 16:9, PDF estático de alta resolución y el límite de no comprobar Microsoft PowerPoint nativo. Progreso 9/18, sin cerrar 5.2 ni publicar. **READY FOR SLIDE REVIEW**.

## Cierre exclusivo de Fase E — 2026-10-24

URL pública oficial comunicada por el operador: [slides finales en Google Drive](https://drive.google.com/file/d/15EqoH_KUZoZEyiApI_oVqTwvNv2c7TFE/view?usp=sharing). Confirma PDF y PPTX finales creados y revisados, 10 slides, sin secretos/credenciales; Drive configurado «cualquier persona con el enlace» y permiso lector. La revisión local de contenido, metadatos, notas y páginas permanece en la evidencia anterior; no se recrean ni modifican los artefactos.

Comprobación directa de lectura mediante navegador integrado sin sesión: el enlace abre `LLM-Scanner-Seguridad-TFM.pdf`, muestra «Página 1 de 10», miniaturas de diez páginas y la portada visible; «Iniciar sesión» es opcional. No aparece solicitud de acceso/permisos ni login obligatorio. No se inicia sesión, descarga el archivo, cambia permisos o publica material desde el agente. Permiso lector/cualquiera con enlace e identidad de la versión final remota proceden de la confirmación del operador; no se afirma cotejo de hash remoto.

PPTX/PDF permanecen en `docs/08-entrega-tfm/slides/`; presencia y SHA-256 locales cotejados con `slides-validation.json`, sin cambios. URL oficial y referencias locales incorporadas a Readme.md, enlaces, checklist, trazabilidad e índice de entrega; estado factual de presentación en requisitos-oficiales actualizado sin alterar obligaciones. URL de slides disponible para el formulario futuro, sin declarar que se haya enviado.

Se satisface el contrato de TFD-05 y 5.2: revisión final y contenido seguro, presentación válida, enlace accesible sin solicitar permisos y referencias en README/directorio/enlaces. **5.1/5.2 COMPLETED; Fase E COMPLETED; 10/18 tareas completas. READY FOR PHASE F.** No se ejecuta F ni se declara vídeo completo, formulario enviado, credenciales entregadas o invitación GitHub aceptada. Restore 6.3 sigue pospuesto/no bloqueante; sin código, AWS, Scans/OpenAI, commit/push o cierre del change. Resultado strict de esta actualización registrado en tasks.md.

OpenSpec strict del cierre E: **PASS**, exit0, `Change 'tfm-final-delivery' is valid`. La validación documental no sustituye los pendientes administrativos de H ni la aceptación de GitHub por el destinatario.

## Cierre técnico final — Release candidate — 2026-10-24

Petición expresa: congelar el repositorio antes de grabar/publicar el vídeo, sin guion dentro del repositorio. Se revisa el working tree actual sobre HEAD `0589a89807d2a6ee8988385a773a27df71088716`. El estado de release candidate corresponde al contenido local revisado, no a un commit nuevo ni a publicación remota. No se ejecutan commit/push, código, builds/tests de producto, servicios, AWS, despliegues, migraciones, Scans ni OpenAI.

| Control | Resultado y evidencia |
|---|---|
| CODE FREEZE | PASS. Los cambios pendientes afectan exclusivamente a README, documentación de entrega, artefactos del change y slides; sin modificaciones de backend/frontend/demo/configuración de producción/dependencias. El MVP conserva las validaciones ya acreditadas; no se identifica trabajo de producto necesario antes del vídeo. |
| DOCS FREEZE | PASS tras correcciones mínimas de coherencia: referencias antiguas a D pendiente actualizadas con su evidencia; retirada la obligación de crear guion versionado y alineados proposal/design/spec/tasks con la instrucción expresa. README contiene descripción, stack, instalación/ejecución, estructura, funcionalidades, deployment, GitHub privado, slides y acceso por canal privado. Vídeo y entrega administrativa permanecen pendientes, sin afirmar DONE. |
| Referencias locales | PASS. 140 enlaces relativos examinados en README y documentos de entrega, sin destinos ausentes. |
| SLIDES | PASS. PPTX y PDF finales presentes y sus tamaños/SHA-256 coinciden exactamente con `slides-validation.json`. PPTX: 10 slides y 10 partes de notas, sin relaciones externas; PDF reabierto: 10 páginas, sin cifrado. Se conserva la revisión visual completa de E porque los bytes no han cambiado. URL oficial Drive y comprobación sin login del 2026-10-24 documentadas; no se repite publicación ni descarga remota. |
| GITHUB | PENDING EXTERNAL. Repositorio privado justificado, URL correcta e invitación a `mouredev@gmail.com` confirmada por operador. 3.3 continúa pendiente exclusivamente de aceptación del destinatario y posterior acceso a la revisión entregada; no exige hacerse público ni otra acción técnica previa al vídeo. |
| DEPLOYMENT | PASS según D de 2026-10-23: cadena/hostname verificados, TLS 1.3, UI/API sin credencial → 401 con desafío Basic Auth. UI autenticada y proyectos observados confirmados por el operador; credenciales preparadas. No se repiten comprobaciones de red ni se infiere nueva ejecución AWS. |
| PUBLIC-SAFE | PASS en el ámbito revisado. Se conserva C y se amplía la búsqueda local redactada al conjunto actual de 699 archivos publicables, 19 commits alcanzables, 871 blobs y mensajes de commit; contenido XML/relaciones del PPTX incluido. Patrones de claves privadas, proveedor/AWS/GitHub, JWT, hashes y asignaciones literales no identifican secretos reales. Coincidencias: contraseña sintética del servidor de tests y referencias de comandos/variables SQL o shell ya clasificadas en C; sin valores persistidos. No hay nombres de archivos secretos en el inventario publicable. `.env` permanece fuera de Git y sin abrir. PDF idéntico al artefacto con contenido/metadatos revisados en E. |

Correcciones imprescindibles realizadas solo en documentación: estados D y límite de freeze. No se crea `docs/08-entrega-tfm/guion-video.md`. 6.1 se concreta como preparación oral fuera del repositorio, sin cambiar el requisito oficial de explicación propia con pantalla ni declarar preparación/grabación realizada. La casilla sigue pendiente; se mantienen **10/18 tareas completas**. La transcripción oficial no se modifica. Los históricos A–E conservan su fecha/procedencia; este cierre establece el estado vigente.

### Pendientes posteriores exactos

1. El alumno prepara/graba fuera del repositorio la explicación propia con pantalla y revisa audio, legibilidad y ausencia de secretos; rostro opcional. Publica el vídeo público/no listado y comprueba reproducción sin solicitar permisos. No requiere nuevos servicios, Scans/OpenAI ni resultados fabricados.
2. Incorporar únicamente la URL final del vídeo a README/enlaces y actualizar estados/checklist/tasks/evidence correspondientes, incluida la revisión final H. No añadir guion, código, infraestructura, nuevas slides ni otros documentos técnicos.
3. Registrar la aceptación externa de la invitación GitHub y el acceso a la revisión cuando existan (3.3); no dar por realizada una acción del destinatario.
4. Completar posteriormente formulario final y entrega privada de credenciales; confirmar fecha/canal sin valores personales o secretos en Git. Las credenciales se facilitarán por canal privado.

Restore aislado 6.3 continúa pospuesto/no ejecutado y no bloqueante. Tampoco bloquean consolidación/archivo histórico OpenSpec, memoria ampliada ni mejoras futuras. No quedan features, refactors, tests, despliegues ni cambios de código/documentación técnica necesarios antes del vídeo. Los PPTX/PDF y `slides-validation.json` aún no trackeados deben incluirse en el futuro commit autorizado de release candidate; no se hace staging ni commit durante este cierre.

**READY FOR RELEASE CANDIDATE COMMIT**, sin afirmar commit ejecutado ni entrega académica completa. `openspec validate tfm-final-delivery --strict`: **PASS**, exit0, `Change 'tfm-final-delivery' is valid`.
