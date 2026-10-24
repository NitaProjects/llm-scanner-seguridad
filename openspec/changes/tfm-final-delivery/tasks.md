Estado: **READY FOR RELEASE CANDIDATE COMMIT — cierre técnico de 2026-10-24**. **10/18 tareas completas**, con [evidencia](evidence/result.md) y [validación de slides](evidence/slides-validation.json). Código/docs/slides congelados. 5.1/5.2 COMPLETED: PPTX/PDF finales revisados, 10 slides y URL oficial Drive accesible sin login. D completada. 3.3 sigue pendiente exclusivamente de aceptación por tercero. F/G se realizan fuera del repositorio, sin crear guion versionado ni marcar vídeo realizado. Después solo URL final del vídeo público/no listado, estados/checklist/tasks/evidence y formulario/entrega privada en H. Sin código, AWS, tests de producto, Scans/OpenAI, commit/push ni cambios de permisos realizados por el agente. Este estado prepara el commit; no afirma que se haya ejecutado.

## 1. A — Auditoría documental final

- [x] 1.1 Contrastar O1–O7 con los requisitos oficiales y evidencia fechada existente; verificar una fila por obligación con fuente, estado, pendiente y procedencia, sin convertir extras o restore 6.3 en bloqueantes (TFD-01; D-01).
- [x] 1.2 Reconciliar exclusivamente estados documentales afectados de checklist/trazabilidad y, si procede, estados factuales de requisitos-oficiales; verificar que instalación, frontend/demo/E2E, OpenAI local y AWS se describen según su evidencia, sin modificar obligaciones ni cerrar tareas de otros changes (TFD-01; D-02).

## 2. B — README final

- [x] 2.1 Consolidar `Readme.md` con overview, stack, instalación/ejecución, estructura y funcionalidades; verificar lectura autosuficiente, versiones/instrucciones existentes, Spring sin carga automática de .env y distinción AWS/local/offline/validación real, sin ejecutar builds o pruebas (TFD-02; D-03).
- [x] 2.2 Preparar secciones GitHub/deployment/slides/vídeo y acceso; verificar URL canónica real y credenciales únicamente como «las credenciales se facilitarán por canal privado», sin afirmar entrega antes de H; mantener explícitamente pendientes los enlaces aún inexistentes hasta E/G/H (TFD-02/TFD-04; D-03/D-05).

## 3. C — GitHub y public-safe

- [x] 3.1 Obtener del autor URL GitHub y revisión candidata e inventariar contenido publicable e historial alcanzable sin abrir archivos de secretos; verificar ámbito que incluye evidencias OpenSpec, manifests, documentos, imágenes y archivos antes ignorados si forman parte de la entrega (TFD-03; D-04).
- [x] 3.2 Realizar revisión pública del working tree e historial con salida redactada y autorización específica para cualquier herramienta que la requiera; verificar ausencia de secretos/credenciales reales según TFD-03, registrando solo alcance/tipo/ruta/riesgo/estado. Minimizar o anonimizar Account ID/Instance ID/IP pública/AMI ID/ECR URI u otros identificadores cuando no aporten valor; no bloquear ni exigir saneamiento histórico solo por su presencia. Bloquear publicación afectada únicamente por secretos reales o riesgo concreto documentado y remitir tratamiento para autorización separada, sin rotación ni reescritura automática (TFD-03; D-04).
- [ ] 3.3 Comprobar URL GitHub y revisión correcta con acceso del evaluador: repositorio privado justificado e invitación enviada; pendiente exclusivamente de aceptación por el destinatario. El 404 anónimo es esperado, no se exige publicar. URL real registrada en enlaces-entrega y README; comprobación autorizada posterior a la aceptación, sin commit/push/cambio de visibilidad implícitos (TFD-03; D-04).

## 4. D — Deployment y credenciales

- [x] 4.1 Con autorización de acceso, comprobar desde sesión nueva https://llmscannerseguridad.com, TLS, rechazo sin credencial y navegación UI/lectura API con credencial privada; verificar resultado fechado no secreto, sin Scans, escrituras de datos, AWS ni cambios en evaluator/demo (TFD-04; D-05).
- [x] 4.2 Preparar usuario/contraseña de Basic Auth para entrega privada en H; verificar preparación sin envío ni confirmación administrativa de entrega, documentando únicamente «las credenciales se facilitarán por canal privado». La falta de entrega al tribunal no bloquea E–G (TFD-04; D-05).

## 5. E — Preparación de slides

- [x] 5.1 Crear con la herramienta elegida por el autor la presentación propuesta en D-06; verificar apertura de todas las slides y cobertura de problema/objetivos/arquitectura/flujo/seguridad/validación/AWS/limitaciones, sin métricas inventadas ni atribuir resultados locales a producción (TFD-05).
- [x] 5.2 Revisar seguridad y metadatos antes de publicación autorizada; entregar archivo válido o enlace público real y comprobar acceso sin solicitud de permisos; verificar URL de slides en README/enlaces y archivo o enlace en el directorio, también utilizable en el formulario (TFD-02/TFD-03/TFD-05).

Preparación de contenido autorizada: [contenido-slides.md](../../../docs/08-entrega-tfm/slides/contenido-slides.md), 10 slides con título/mensaje/texto visible/visual recomendado/notas de 20–40 s. Estimación: 5 min 20 s de exposición, aproximadamente 6 min con transiciones. No se genera PPTX/PDF por instrucción expresa. 5.1 queda parcial: falta crear y abrir la presentación con la herramienta elegida; 5.2 pendiente de artefacto/metadatos/publicación/acceso. Ninguna casilla nueva completa; total 8/18. El Markdown de contenido no se declara presentación final ni URL pública entregable.

Control de contenido: 4/4 limitado a casos sintéticos VULNERABLE locales, no benchmark; AWS sin demo/Scan y evaluator desactivado. Sin secretos ni entrega efectiva de credenciales declarada. `openspec validate tfm-final-delivery --strict` → `Change 'tfm-final-delivery' is valid`, exit0. **READY FOR SLIDE CREATION**; detalle y procedencia en la evidencia de Fase E.

Creación posterior autorizada: [PPTX](../../../docs/08-entrega-tfm/slides/LLM-Scanner-Seguridad-TFM.pptx) y [PDF](../../../docs/08-entrega-tfm/slides/LLM-Scanner-Seguridad-TFM.pdf), sin modificar el contenido fuente aprobado. PPTX reimportado con Artifact Tool y las 10 slides renderizadas, paquete/geometría sin findings; no se afirma apertura en Microsoft PowerPoint. Notas 10/10 iguales al fuente, fuera del texto visible. PDF reabierto con pypdf y sus 10 páginas renderizadas con Poppler, revisión visual individual completa. PDF estático de alta resolución, texto no seleccionable; generado del render del PPTX validado tras fallo del exportador PDF del runtime. Corregidos flechas y margen inferior, sin overflow/texto cortado/problemas de legibilidad pendientes. Ambos archivos copiados sin alterar bytes a sus rutas de entrega; hashes en `evidence/slides-validation.json`. **5.1 COMPLETED; 5.2 parcial; 9/18 completas. READY FOR SLIDE REVIEW**. Sin publicación ni enlace remoto verificado, no se marca 5.2 completa ni se declara disponible una URL pública de slides.

Strict posterior a la validación de ambos artefactos: `openspec validate tfm-final-delivery --strict` → `Change 'tfm-final-delivery' is valid`, exit0. Recuento confirmado 9/18; no acredita publicación ni DONE de entrega.

Retoques posteriores solicitados por el operador: slide 4 con bloque explícito «Puertos» entre Aplicación/Dominio y adaptadores; slide 10 con «score heurístico ≠ probabilidad de vulnerabilidad». Fuente Markdown y PPTX/PDF sincronizados; notas y duración sin cambios. Reimportación del PPTX, render de PDF y revisión visual de 4/10 PASS; comparación de las otras ocho slides idéntica píxel a píxel, incluida la 7. Hashes renovados en la evidencia de validación, sin problemas de overflow/legibilidad pendientes. Total 9/18; 5.2 sigue parcial. **READY FOR SLIDE REVIEW**.

Cierre posterior autorizado de E, 2026-10-24: [URL pública oficial de slides](https://drive.google.com/file/d/15EqoH_KUZoZEyiApI_oVqTwvNv2c7TFE/view?usp=sharing). El operador confirma revisión de PDF/PPTX finales, 10 slides sin secretos/credenciales y Drive «cualquier persona con el enlace / lector». Comprobación directa mediante navegador integrado sin sesión: visor `LLM-Scanner-Seguridad-TFM.pdf`, «Página 1 de 10», documento visible y opción «Iniciar sesión» no obligatoria, sin solicitud de permisos. No se descarga ni se atribuye hash remoto; identidad final publicada confirmada por el operador. Se conserva QA local y hashes de los dos archivos, sin modificarlos ni moverlos. README/enlaces/checklist/trazabilidad/índice y estado factual de requisitos actualizados. **5.2 COMPLETED; E COMPLETED; 10/18. READY FOR PHASE F**. F–H, 3.3 y entrega administrativa sin cierre; ningún cambio de permisos, publicación, código, AWS o commit/push ejecutado por el agente.

`openspec validate tfm-final-delivery --strict` del cierre de E: **PASS**, exit0, `Change 'tfm-final-delivery' is valid`. Recuento confirmado **10/18**, sin declarar DONE de entrega.

## 6. F — Preparación oral y visual fuera del repositorio

- [ ] 6.1 Preparar fuera del repositorio la explicación propia y la secuencia de pantalla con las slides/notas y fuentes existentes de D-07; sin crear `guion-video.md` ni otro archivo de guion. No requiere cambios técnicos/documentales previos a grabar; verificar procedencia de resultados, sin nuevos Scans, servicios ni llamadas OpenAI (TFD-06; cierre técnico autorizado 2026-10-24).
- [ ] 6.2 Preparar el contenido visual ya disponible y revisar encuadre público; verificar que login/credenciales/notificaciones/datos operativos sensibles quedan fuera y que resultados estáticos se identifican como tales, sin fabricar evidencia (TFD-03/TFD-06; D-07).

## 7. G — Grabación y publicación del vídeo

- [ ] 7.1 El alumno graba explicación propia con pantalla siguiendo la preparación oral fuera del repositorio; revisar el vídeo completo y verificar audio, pantalla legible, coherencia, ausencia de secretos y rostro opcional. Las slides sin explicación propia no completan la tarea (TFD-06; D-07).
- [ ] 7.2 Tras revisión pública y autorización de publicación, alojar el vídeo en plataforma elegida; verificar reproducción de URL final accesible sin login/solicitud de permisos y documentarla en README/enlaces, sin guardar credenciales ni exigir binario en Git (TFD-03/TFD-06).

## 8. H — Checklist final y formulario

- [ ] 8.1 Revisar el paquete final completo con el checklist del reviewer D-08; verificar O1–O7, enlaces/archivos finales, case correcto de `Readme.md`, revisión pública del material nuevo y cero hallazgos bloqueantes. Registrar evidencia mínima sanitizada en el change, sin datos de formulario privados (TFD-01–TFD-07).
- [ ] 8.2 Completar con el autor nombre, email de inscripción, GitHub, deployment URL, slides URL, video URL y credenciales en formulario/canal privado; el autor realiza y confirma aquí la entrega efectiva de credenciales junto con el formulario final. Verificar campos reales y confirmación administrativa de fecha/canal sin valores personales o secretos en GitHub ni envío automático del formulario (TFD-04/TFD-07; D-05/D-08).
- [ ] 8.3 Actualizar exclusivamente README/índice afectado, checklist, trazabilidad y enlaces con evidencias reales; verificar DONE según D-08, mantener opcionales separados y restore 6.3 pospuesto, y ejecutar `openspec validate tfm-final-delivery --strict` cuando se solicite la comprobación de cierre. No declarar entrega material completa solo por superar strict (TFD-01/TFD-07).

## Ejecución A+B — 2026-10-23

Autorización explícita del operador limitada a auditoría documental y README. Schema spec-driven, estado apply ready y progreso inicial 0/18. Se completan exclusivamente 1.1, 1.2, 2.1 y 2.2: **4/18 tareas**. Evidencia en `evidence/result.md`: una fila O1–O7 con fuente/procedencia/estado/pendiente y siete findings documentales corregidos. Checklist/trazabilidad reconciliados con instalación local, recorridos frontend/demo/API, OpenAI real local y AWS, sin tocar tasks de otros changes ni reabrir restore 6.3 expresamente pospuesto.

`Readme.md` autosuficiente: objetivo/funcionalidades/stack/arquitectura/estructura, instalación/ejecución/configuración, demo local, evaluador, seguridad, pruebas/limitaciones y URL pública. Acceso únicamente «las credenciales se facilitarán por canal privado». GitHub/slides/vídeo permanecen placeholders explícitos; no se crean materiales ni se afirma entrega efectiva. Corregidos case de referencias al README real, duplicación de acceso, estados antiguos y descripción TLS conforme a evidencia posterior. Revisadas 142 referencias locales sin destino ausente ni discrepancia del case del fichero; no se consultan URLs externas.

`openspec validate tfm-final-delivery --strict` → `Change 'tfm-final-delivery' is valid`, exit0. Auditoría **PASS**, README **READY para A+B**, estado **READY FOR REVIEW**. C–H y entrega material pendientes; no se cierra el change. Sin código, AWS, despliegue, builds/tests de producto, servidores, Scans/OpenAI, slides, vídeo, commit/push ni formulario.

## Ejecución C — 2026-10-23

Revisión autorizada del contenido publicable y del historial local alcanzable: **PASS en 3.2**, sin secretos reales detectados. 689 archivos tracked y seis no trackeados del change; 18 commits y 855 blobs históricos, incluidos mensajes de commit e inventario binario. `.env` local ignorado, no abierto ni incluido; sin archivos de secretos trackeados o históricos detectados. Clasificación redactada, imágenes y límites detallados en `evidence/result.md`.

El autor confirmó usar `origin` y `HEAD`: URL `https://github.com/NitaProjects/llm-scanner-seguridad`, revisión candidata `ecee9cf41836b679fc5cfa53865f5615695510d0`. Las lecturas HTTPS anónimas de repositorio, revisión, README y checklist devolvieron **404**. La confirmación posterior de privacidad deliberada e invitación enviada explica ese resultado: no implica repositorio inexistente ni obligación de publicarlo. La valoración inicial de 5/18 queda sustituida por el registro siguiente. No se presentan como publicados artefactos locales sin evidencia ni se realiza operación Git.

### Confirmación posterior del operador — acceso privado preparado

Repositorio privado deliberadamente por ser un proyecto de seguridad con documentación/configuración operativa y preferencia por acceso controlado, conforme a la opción oficial de privado justificado. URL y revisión candidata/inventario ya registrados: **3.1 COMPLETED**. Working tree e historial local alcanzable mantienen **3.2 PASS**, sin secretos reales detectados en el ámbito revisado; no se repite la revisión ni se amplía su garantía a cambios futuros.

El operador confirma invitación enviada al evaluador exigido por la entrega; destinatario y estado en `evidence/result.md`. GitHub muestra pendiente de aceptación: preparación exigible al operador realizada, pero acceso efectivo no acreditado aún. **3.3 pendiente exclusivamente de aceptación del destinatario**, dependiente de tercero. No se exige hacer público el repositorio ni se envía otra invitación. Total **6/18**. Solo Fase C y sus referencias documentales actualizadas; D–H sin ejecución. **READY FOR REVIEW**, no DONE.

Validación de esta actualización: `openspec validate tfm-final-delivery --strict` → `Change 'tfm-final-delivery' is valid`, exit0. Recuento: **6/18**; eliminada la exigencia incorrecta de visibilidad pública en las referencias afectadas, conservando la aceptación efectiva pendiente del tercero. Spec TFD-03 y diseño D-04 reconciliados con la decisión explícita del operador y la opción oficial de privado justificado.

## Ejecución D — 2026-10-23

**4.1 y 4.2 COMPLETED.** TLS público verificado directamente con validación de cadena y hostname, TLS 1.3; GET sin credenciales a `/` y `/api/projects` → HTTP 401 y desafío Basic Auth. El navegador del agente no dispone de sesión autenticada; el operador confirma en esta ejecución que introduce Basic Auth, carga la UI y ve los proyectos existentes. Esta lectura UI acredita el acceso funcional según confirmación del operador, sin inventar un status API autenticado medido por el agente.

El operador confirma que conserva usuario/contraseña preparados para H y no comparte valores en chat ni documentación. README/enlaces mantienen URL canónica y «las credenciales se facilitarán por canal privado». Once documentos de README/entrega/change revisados sin coincidencias de credenciales reales; se conserva el PASS public-safe de C con sus límites. HEAD `0589a89807d2a6ee8988385a773a27df71088716` coincide con master local; sincronización remota declarada por el operador, no comprobada mediante nueva lectura autenticada de GitHub. No se afirma entrega efectiva ni aceptación de invitación.

Total **8/18**; 3.3 sigue pendiente de tercero, E–H sin ejecutar. **READY FOR PHASE E**. Solo evidence/tasks modificados, sin Scans/OpenAI, escrituras funcionales, AWS, configuración, código, commit/push ni envío de credenciales. Resultado de `openspec validate tfm-final-delivery --strict` registrado en la evidencia de D.

## Cierre técnico final — 2026-10-24

**CODE FREEZE PASS; DOCS FREEZE PASS; SLIDES PASS; GITHUB PENDING EXTERNAL; DEPLOYMENT PASS; PUBLIC-SAFE PASS. READY FOR RELEASE CANDIDATE COMMIT.** Solo correcciones documentales imprescindibles: D ya completada y preparación de vídeo fuera del repositorio, sin crear guion versionado. Se conserva el total **10/18**, sin marcar F/G/H ni aceptación GitHub como realizados. PPTX/PDF coinciden con los hashes finales de E y conservan QA de 10 slides/páginas. Working tree publicable e historial local actual revisados sin secretos reales detectados; alcance en `evidence/result.md`.

Después de este cierre solo se esperan: preparación/grabación/revisión/publicación del vídeo fuera del repositorio; añadir su URL pública/no listada y actualizar estados/checklist/tasks/evidence; registrar aceptación externa de 3.3 cuando exista; completar formulario y entrega privada de credenciales en H. No queda trabajo de código, infraestructura ni documentación técnica necesario antes del vídeo. Restore 6.3 sigue pospuesto/no bloqueante. No se ejecutan tests de producto, AWS, deploy, Scans/OpenAI, staging, commit/push ni cambios de visibilidad. Los artefactos finales no trackeados se incluirán en el commit autorizado posterior.

`openspec validate tfm-final-delivery --strict`: **PASS**, exit0, `Change 'tfm-final-delivery' is valid`. No acredita vídeo publicado ni entrega académica completa; se congela el contenido local revisado, no se afirma commit nuevo.
