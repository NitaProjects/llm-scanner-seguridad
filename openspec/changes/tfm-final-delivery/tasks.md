Estado: **READY FOR REVIEW — Fase C con repositorio privado justificado, 2026-10-23**. **6/18 tareas completas**, con [evidencia y acceso preparado](evidence/result.md). A+B aprobadas; 3.1 COMPLETED, revisión local 3.2 PASS. 3.3 permanece pendiente exclusivamente de aceptación de la invitación por el destinatario, tercero; no de hacer público el repositorio. El operador confirma invitación enviada y GitHub la muestra pendiente. El 404 anónimo es esperado. D–H no ejecutadas; no se declara entrega completa ni se altera su orden. Sin accesos externos nuevos, código, AWS, Scans, commit/push ni cambios de visibilidad.

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

- [ ] 4.1 Con autorización de acceso, comprobar desde sesión nueva https://llmscannerseguridad.com, TLS, rechazo sin credencial y navegación UI/lectura API con credencial privada; verificar resultado fechado no secreto, sin Scans, escrituras de datos, AWS ni cambios en evaluator/demo (TFD-04; D-05).
- [ ] 4.2 Preparar usuario/contraseña de Basic Auth para entrega privada en H; verificar preparación sin envío ni confirmación administrativa de entrega, documentando únicamente «las credenciales se facilitarán por canal privado». La falta de entrega al tribunal no bloquea E–G (TFD-04; D-05).

## 5. E — Preparación de slides

- [ ] 5.1 Crear con la herramienta elegida por el autor la presentación propuesta en D-06; verificar apertura de todas las slides y cobertura de problema/objetivos/arquitectura/flujo/seguridad/validación/AWS/limitaciones, sin métricas inventadas ni atribuir resultados locales a producción (TFD-05).
- [ ] 5.2 Revisar seguridad y metadatos antes de publicación autorizada; entregar archivo válido o enlace público real y comprobar acceso sin solicitud de permisos; verificar URL de slides en README/enlaces y archivo o enlace en el directorio, también utilizable en el formulario (TFD-02/TFD-03/TFD-05).

## 6. F — Preparación del guion de vídeo

- [ ] 6.1 Redactar `docs/08-entrega-tfm/guion-video.md` siguiendo D-07 con explicación propia, pantalla y fuentes de cada resultado; verificar secuencia que usa AWS existente y resultados/capturas locales conservados, sin nuevos Scans, servicios ni llamadas OpenAI, y duración orientativa no presentada como requisito oficial (TFD-06).
- [ ] 6.2 Preparar el contenido visual ya disponible y revisar encuadre público; verificar que login/credenciales/notificaciones/datos operativos sensibles quedan fuera y que resultados estáticos se identifican como tales, sin fabricar evidencia (TFD-03/TFD-06; D-07).

## 7. G — Grabación y publicación del vídeo

- [ ] 7.1 El alumno graba explicación propia con pantalla siguiendo el guion; revisar el vídeo completo y verificar audio, pantalla legible, coherencia, ausencia de secretos y rostro opcional. Un guion o slides sin explicación propia no completan la tarea (TFD-06; D-07).
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
