## Context

Planificación a 2026-10-20; motivación en [proposal.md](proposal.md). Fuentes inspeccionadas: `docs/08-entrega-tfm/{requisitos-oficiales,checklist-entrega,trazabilidad-requisitos,enlaces-entrega,README}.md`, `Readme.md`, y registros de `aws-production-readiness`, `real-evaluator-validation`, `e2e-integration` y `frontend-mvp`. No se ha comprobado ningún servicio externo, leído secretos ni ejecutado producto.

El operador comunica MVP funcional, frontend integrado, demo local y validación real OpenAI completada. Los registros de revalidación local de 2026-10-19 acreditan cuatro componentes LLM SUCCEEDED y resultados persistidos; no constituyen benchmark. AWS está operativo según evidencia del operador de 2026-10-20: https://llmscannerseguridad.com, TLS/Basic Auth, Angular/backend/PostgreSQL y persistencia. No hay demo desplegada ni evaluator habilitado en AWS. `aws-production-readiness` registra 24/25 y restore aislado pendiente y pospuesto.

Discrepancias documentales observadas: README aún describe E2E/safety de demo pendientes; checklist/trazabilidad conservan instalación/ejecución o frontend/demo en estados antiguos. Los tasks históricos de frontend y E2E también mantienen pendientes de revisión/recorrido; no se cerrarán desde este change. La auditoría citará evidencia posterior y separará aceptación funcional comunicada de cierre formal histórico. Ninguna divergencia autoriza modificar comportamiento del producto.

`requisitos-oficiales.md` separa expresamente entrega y capabilities del producto. La spec solicitada aquí describe aceptación del paquete académico; no se incorpora al dominio ni se propone sincronizar como capability funcional del scanner.

## Goals / Non-Goals

**Goals:** cerrar obligaciones con evidencia verificable, un README autosuficiente, un recorrido de evaluación accesible y material audiovisual sin secretos. Mantener trazabilidad y una ruta de trabajo corta antes del 26 señalado por el operador; el plazo no permite omitir obligaciones.

**Non-Goals:** cambios de código o infraestructura; reapertura de restore 6.3; nuevas ejecuciones OpenAI/Scans, benchmark formal, PDF reporting, autenticación multiusuario, categorías nuevas, refactors o polish visual. Tampoco se exige completar toda la memoria técnica, archivar changes o resolver todo el backlog para satisfacer los siete requisitos enumerados. Si aparece una exigencia oficial adicional, se registra y se revisa su alcance antes de incorporarla.

## Decisions

### D-01 — Matriz de obligaciones y evidencia

Los estados siguientes describen evidencia documental actual, no una verificación de acceso realizada ahora. Se reutiliza evidencia existente; no se repiten builds/pruebas para corregir textos.

| ID / obligación oficial | Evidencia actual inspeccionada | Estado y pendiente de cierre |
|---|---|---|
| O1a README: overview, stack, estructura | `Readme.md`: descripción, stack y árbol; checklist los acredita | Contenido existente; revisar coherencia y navegación final |
| O1b README: instalación/ejecución | README incluye versiones, builds, PostgreSQL/Flyway, entorno, demo/evaluator opt-in y enlace a guía de producción; checklist acredita validación local | Existente; reconciliar trazabilidad antigua. No ejecutar nuevamente comandos |
| O1c README: funcionalidades y acceso | Flujo híbrido/scoring/reporting descrito; Basic Auth privado documentado | Parcial: actualizar estado real FE/demo/E2E sin inventar cierres; antes de H usar «las credenciales se facilitarán por canal privado», sin valores |
| O2 GitHub accesible | URL/revisión/inventario registrados; repositorio privado justificado, revisión public-safe local PASS e invitación enviada según operador | 3.1 completa; 3.3 pendiente exclusivamente de aceptación por el destinatario. 404 anónimo esperado; no exigir visibilidad pública |
| O3 publicación y URL | README/enlaces y `aws-production-readiness/evidence/2026-10-20-aws-deployment.md`; URL real, preparación académica aceptada | Publicación acreditada por operador; comprobación final autorizada y entrega privada de acceso pendientes |
| O4 slides: documento o enlace | Enlaces y requisitos: PENDIENTE; carpeta de entrega no contiene presentación | Pendiente crear, revisar, hacer accesible y enlazar presentación válida |
| O5 vídeo propio con pantalla y URL | Enlaces y requisitos: PENDIENTE | Pendiente guion, grabación por alumno, revisión y publicación/reproducción |
| O6 directorio: docs, despliegue, slides/enlace | `docs/`, README y guía de producción existen | Parcial: añadir slides/enlace y comprobar referencias del paquete final |
| O7 formulario | Campos enumerados en requisitos; deployment conocido | Pendiente nombre/email por canal privado, GitHub/slides/vídeo reales y entrega privada de credenciales; no inferir datos personales |

Obligatorio: O1–O7, revisión pública sin secretos y acceso académico efectivo. Mejoras opcionales: rostro en vídeo, animaciones, gráficos adicionales, formatos duplicados de slides, material complementario y pulido editorial que no impida entender/abrir la entrega. Pendientes técnicos no bloqueantes: restore 6.3 explícitamente pospuesto y consolidación/archivo histórico OpenSpec; quedan visibles sin nuevas tareas de ejecución. Benchmark, PDF y nuevas features quedan fuera, no se presentan como obligaciones.

### D-02 — Orden de cierre y documentos afectados

Orden exacto: **A → B → C → D → E → F → G → H**. Antes de publicar cualquier artefacto posterior a C se aplica de nuevo el control público de C sobre su contenido; H comprueba el conjunto final. Trabajo externo, acceso a producción y publicación requieren autorización específica al ejecutar; aprobar el diseño no publica ni autoriza AWS, comandos o envío de credenciales por herramientas.

| Fase | Trabajo futuro y salida verificable | Archivos que se podrán tocar tras aprobación |
|---|---|---|
| A Auditoría | Contrastar O1–O7; listar contradicciones y evidencia posterior; inventariar pendientes sin alterar requisitos | `docs/08-entrega-tfm/checklist-entrega.md`, `trazabilidad-requisitos.md`; estado factual de `requisitos-oficiales.md` solo si queda desactualizado, nunca su transcripción normativa |
| B README | Consolidar contenido obligatorio, estado real y límites, enlaces finales cuando existan; no delegar toda la instalación a otro documento | `Readme.md` existente, sin duplicar `README.md` ni renombrar por case; `docs/08-entrega-tfm/README.md` solo para índice afectado |
| C GitHub/public-safe | Revisar material publicable e historial con resultados redactados; obtener URL/revisión y verificar acceso público con autorización | Correcciones documentales puntuales detectadas y `enlaces-entrega.md`; evidencia sanitizada del change. No reescritura de historial ni push implícitos |
| D Deployment/acceso | Confirmar lectura UI/API, TLS y funcionamiento de Basic Auth; preparar usuario/contraseña para entrega privada, sin enviarlos ni confirmar entrega | `enlaces-entrega.md`, checklist y evidencia sanitizada del change; documentar únicamente «las credenciales se facilitarán por canal privado». No editar guía ni infraestructura salvo contradicción indispensable revisada aparte |
| E Slides | Crear presentación y verificar archivo/enlace; registrar URL incluso si apunta al documento público en GitHub | Futuro archivo en `docs/08-entrega-tfm/slides/` si se elige archivo; o URL externa real. README/enlaces e índice solo para referencias |
| F Guion | Redactar relato y secuencia visual con fuente de cada resultado | Futuro `docs/08-entrega-tfm/guion-video.md`, sin credenciales ni configuración sensible |
| G Vídeo | Alumno graba voz/pantalla, revisa y publica; comprobar reproducción por enlace final | URL en `enlaces-entrega.md` y README; vídeo alojado externamente, no requiere versionar binario |
| H Checklist/formulario | Revisar paquete completo, enlaces, acceso y evidencia; reunir campos y entregar credenciales en formulario/canal privado, registrar confirmación efectiva de entrega sin valores | Checklist, trazabilidad, enlaces, README si quedan referencias pendientes; evidencia mínima del change |

No se crean ahora esos documentos finales ni carpetas de material. Alternativa descartada: redactar desde cero una segunda memoria/guía duplicada; amplía alcance y crea divergencias. Se actualiza exclusivamente el documento afectado y se enlaza a fuentes existentes.

### D-03 — README final y reproducción

Estructura propuesta sobre `Readme.md`: descripción/valor; estado y límites; stack; funcionalidades y cuatro categorías; arquitectura/estructura; requisitos/versiones; instalación y ejecución; configuración sin secretos; deployment/acceso; seguridad; evidencia/limitaciones; documentación; enlaces GitHub/slides/vídeo.

Conservar instrucciones existentes de checkout limpio, builds FE/backend/demo, PostgreSQL16/Flyway y perfiles, variables sin valores sensibles, manifest/binding y la advertencia de que Spring no carga `.env` automáticamente. Explicar ruta local offline reproducible sin OpenAI y validación real ya realizada, sin pedir que el tribunal la repita. AWS `pro` conserva evaluator apagado y demo ausente. No presentar el score como probabilidad, cobertura OWASP completa o seguridad garantizada.

Antes de H, la frase pública de credenciales será únicamente **«las credenciales se facilitarán por canal privado»**. No se afirmará que ya fueron entregadas. Solo en H, tras confirmar la entrega efectiva mediante el formulario/canal privado, podrá registrarse ese estado sin valores. No copiar usuario/contraseña a README, capturas, slides, guion o enlaces. El texto oficial sobre credenciales se satisface mediante acceso privado para evaluación, conforme a la instrucción explícita de seguridad pública del autor; no se modifica la transcripción del requisito.

### D-04 — GitHub y revisión pública

Decisión confirmada por el operador en Fase C: repositorio **privado deliberadamente**, https://github.com/NitaProjects/llm-scanner-seguridad, por tratarse de un proyecto de seguridad con documentación/configuración operativa y preferencia por acceso controlado. La opción oficial permite privado justificado con acceso al evaluador designado; no es necesario hacerlo público. URL/revisión candidata e inventario ya registrados satisfacen 3.1. La revisión public-safe local de working tree/historial fue PASS, sin secretos reales detectados en su alcance.

El operador confirma invitación enviada y GitHub muestra pendiente de aceptación. Esa evidencia acredita acceso preparado, no aceptación del destinatario ni acceso efectivo a código/README/documentos. Un 404 anónimo es esperado, no bloqueo por visibilidad. 3.3 queda pendiente exclusivamente de aceptación por el destinatario; la comprobación de acceso a la revisión se realizará con el acceso autorizado del evaluador, sin exigir lectura anónima. Destinatario y procedencia en `evidence/result.md`; no se envían invitaciones ni se cambia visibilidad desde este change.

Ámbito: working tree que se publicará, archivos ya tracked, historial alcanzable, Markdown/evidencias JSON, ejemplos/manifests, enlaces, imágenes, propiedades de slides, capturas y audio/vídeo. `.gitignore` por sí solo no demuestra ausencia en el historial. No abrir `.env`, archivos de credenciales, certificados ni claves. El operador realizará, con autorización específica, comprobación de secretos con salida siempre redactada; si necesita herramienta adicional, no se instala automáticamente. La inspección de nombres/rutas sola no permite declarar el repositorio public-safe.

Son bloqueantes los secretos/credenciales reales: passwords, API keys, credenciales AWS, claves AES, claves privadas TLS, tokens, valores SecureString, htpasswd/hash cuando exponga material sensible y secretos equivalentes. El mismo criterio se aplica a manifests y evidencias: se evalúa su contenido y riesgo, no solo su nombre. Ante hallazgo, registrar tipo/ruta/estado sin valor; si ya se publicó un secreto, comunicar al autor la necesidad de revocación y saneamiento del historial bajo autorización separada. El borrado del archivo actual no acredita saneamiento histórico.

Account ID, Instance ID, IP pública, AMI ID, ECR URI y otros identificadores operativos no son secretos por sí mismos. Minimizar o anonimizar cuando no aporten valor, sin bloquear la entrega únicamente por su presencia, tampoco en el historial. Solo elevarlos a bloqueo si existe un riesgo concreto documentado; no exigir saneamiento histórico por identificadores no secretos sin ese riesgo. Las plantillas sintéticas públicas podrán mantenerse si no contienen secretos ni riesgos concretos documentados. Revisar también la propia evidencia OpenSpec antes de publicarla. La URL canónica HTTPS sí es pública.

Alternativa descartada: publicar primero y limpiar después. La revisión previa y final evita difundir secretos en contenido que permanece recuperable. No se promete ausencia de secretos sin evidencia de revisión.

### D-05 — Deployment y credenciales sin ampliar operación

Reutilizar evidencia AWS existente con su procedencia; comprobación final futura desde navegador nuevo: URL canónica, TLS válido, rechazo sin credencial, acceso Basic Auth privado y lectura de UI/API. No usar comandos con contraseña en argv ni capturar login, cabeceras, cookies, devtools sensibles o consola AWS. Preparar la sesión antes de grabar. No crear proyectos/targets/Scans para esta comprobación ni probar exposición mediante escaneos.

La fase D se limita a verificar técnicamente el acceso y Basic Auth y preparar usuario/contraseña para entrega privada, documentando únicamente «las credenciales se facilitarán por canal privado». No requiere entrega al tribunal para avanzar a E–G. En H, junto con el formulario final, el autor entregará las credenciales exclusivamente por el canal/campo privado del TFM y confirmará fecha/canal/receptor sin valores en la evidencia. Nombre completo y email de inscripción también se completarán privadamente en H. El acceso técnico ya aceptado no se vuelve pendiente solo por faltar ese envío administrativo. Mantener la ventana operativa comprometida hasta evaluación; no ejecutar aquí renovación TLS ni acciones AWS.

### D-06 — Slides propuestas

Propuesta de 10 slides, ajustable por claridad; la fuente oficial inspeccionada no fija número ni duración:

1. Título, autor y objetivo defensivo; sin email ni datos personales innecesarios.
2. Problema y valor: seguridad de aplicaciones LLM, autorización y alcance académico.
3. Objetivos/MVP: cuatro categorías y funcionalidades entregadas, límites de cobertura.
4. Arquitectura: Angular → REST Spring Boot → aplicación/dominio → puertos/adaptadores → PostgreSQL/target/evaluator.
5. Flujo: Project/Target → cuatro pruebas → EvaluationResults → Findings → Score → Report.
6. Evaluación híbrida: reglas frente a interpretación LLM, independencia target/oracle/evaluator, INCONCLUSIVE y score heurístico.
7. Seguridad: SSRF, gates de exportación, secretos externos y Basic Auth académico.
8. Validación existente: offline, OpenAI real local y resultados persistidos; evidencia fechada, sin métricas científicas inventadas.
9. AWS: topología mínima y URL canónica; evaluator apagado y demo ausente; resultados locales claramente separados.
10. Limitaciones, conclusiones y futuro; enlaces repositorio/deployment/material y acceso privado.

Recomendada una única presentación con herramienta ya disponible para el autor, entregada como archivo válido o enlace público. Si se usa archivo, su enlace público GitHub sirve para el campo slides URL; no se obliga a contratar hosting ni generar dos formatos. Reviewer abre todas las slides y revisa texto legible, enlaces, alcance y metadatos públicos antes de publicación.

### D-07 — Vídeo propuesto

Guion en seis segmentos, duración orientativa 5–8 minutos, sin presentarla como requisito oficial:

1. Explicación propia del alumno: problema, objetivo y uso autorizado, con pantalla del proyecto/slides.
2. Arquitectura/flujo principal y separación de target, reglas y evaluator.
3. Captura de AWS ya autenticado: navegación UI y persistencia existente; aclarar que no ejecuta evaluator/demo.
4. Captura de resultados locales ya persistidos si están disponibles: Scan terminal, evaluaciones, Finding, score e informe; explicar validación real existente. No lanzar otro Scan ni volver a llamar OpenAI.
5. Evidencia y límites: validación operativa local frente a publicación AWS, sin benchmark/precision/recall ni falsas garantías.
6. Cierre: valor, limitaciones y dónde encontrar repositorio, deployment, slides y vídeo; acceso privado.

Si no está disponible la sesión local con resultados, utilizar capturas no secretas previamente verificadas y referenciar su fecha/procedencia, presentadas como evidencia grabada/estática. No simular un resultado actual ni reactivar servicios para conseguirlo. La grabación debe contener pantalla y explicación propia del alumno; una voz sintética o slides sin narración propia no sustituyen ese requisito. Rostro opcional. Login preparado fuera de captura; ocultar notificaciones, pestañas, historial de terminal y datos privados antes de grabar.

Publicar, con autorización independiente, en plataforma que el alumno controle y permita abrir el enlace sin solicitud de permiso; enlace no listado accesible sin login es admisible si respeta la instrucción oficial de URL pública. Revisar audio, pantalla legible, reproducción y ausencia de datos sensibles antes y después de publicación. No generar el vídeo en esta fase.

### D-08 — Evidencia, reviewer y DONE

Evidencia mínima futura en `openspec/changes/tfm-final-delivery/evidence/result.md`: revisión examinada, fecha, responsable/procedencia, matriz O1–O7, URLs públicas reales, resultado de apertura de slides/vídeo/GitHub, resultado de acceso TLS/Basic Auth sin valores, confirmación administrativa de envío privado y findings resueltos. No guardar dumps, logs de autenticación, cuerpos sensibles, formularios privados ni capturas de AWS. La evidencia no secreta es también parte del contenido que debe revisarse antes de publicación.

El reviewer comprobará:

- Correspondencia O1–O7 → evidencia, sin marcar COMPLETADO por existir placeholders o este plan.
- README autosuficiente y enlaces relativos con case correcto en GitHub/Linux: raíz real `Readme.md`; no se crea un duplicado `README.md` en Windows.
- Reconciliación de estados con fuentes fechadas; ninguna afirmación de experimento formal, Scan AWS, restore o cierre histórico no acreditado.
- GitHub accesible al evaluador/revisión correcta, admitiendo privado justificado; distinguir invitación enviada de aceptación/acceso efectivo. Revisión de seguridad del working tree, historial y material audiovisual, cero hallazgos bloqueantes abiertos según TFD-03; los identificadores operativos por sí solos no bloquean ni exigen saneamiento histórico.
- HTTPS/Basic Auth verificados y credenciales preparadas en D, con entrega administrativa efectiva confirmada exclusivamente en H; antes de H no se afirma que ya se entregaron.
- Slides que abren, vídeo con explicación del alumno y pantalla y enlaces accesibles sin pedir permisos.
- Campos completos en el formulario/canal privado, sin datos personales o credenciales añadidos a GitHub.
- Solo documentación/material autorizado; sin alteraciones del core, infraestructura, gates o SSRF.

**DONE de entrega:** O1–O7 con evidencia real; README/documentos coherentes y paquete accesible; publicación GitHub/material autorizada y revisada; deployment comprobado; credenciales entregadas privadamente; nombre/email y todas las URLs preparados en formulario privado; reviewer sin findings bloqueantes. Si se exige confirmar envío del formulario, registrar únicamente acuse/fecha no sensibles tras confirmación del alumno, nunca declarar envío automático. Opcionales y restore 6.3 no bloquean. **READY FOR REVIEW del change no equivale a DONE de la entrega.**

## Risks / Trade-offs

- Estados históricos contradictorios → citar evidencia posterior y limitar correcciones al estado documental, sin tachar tareas ajenas ni reabrir pruebas.
- Repositorio o material con secretos históricos → revisión redactada previa a publicación; hallazgos bloquean el material afectado hasta tratamiento autorizado.
- Slides/vídeo aún inexistentes o enlaces privados → trabajo obligatorio, apertura desde sesión nueva antes de DONE; un guion no satisface vídeo.
- Tribunal sin credenciales o URL inaccesible → confirmar envío privado y acceso final; no publicar credenciales como solución.
- Expiración/coste/operación AWS durante evaluación → autor conserva responsabilidad operativa ya acordada; no se rediseña AWS ni se inventan importes.
- Poca disponibilidad antes del 26 → priorizar O1–O7 y evidencia existente; prescindir de extras, nuevos experimentos y regrabaciones que exijan consumo LLM.

## Migration Plan

No hay migración, despliegue ni rollback técnico: el producto conserva su arquitectura y configuración. Tras revisión/aprobación, aplicar A–H documentalmente. Cualquier defecto obligatorio imposible de resolver documentalmente se describe con evidencia, impacto y cambio mínimo propuesto; requiere nueva autorización para código/infraestructura. La implementación documental no concede permiso para AWS, commit/push, envío de mensajes o publicación.

## Open Questions

- URL GitHub/revisión candidata ya aportadas; invitación enviada al evaluador, pendiente de aceptación por el destinatario. Nombre y email de inscripción se aportarán en el canal adecuado en H; no se deducen del sistema o Git.
- Autor: elegir herramienta de slides/plataforma de vídeo que ya controle; ambas opciones cumplen la misma aceptación, sin nuevas dependencias del producto.
- Autor: preparar credenciales y canal privado del TFM en D; confirmar entrega efectiva de Basic Auth en H junto al formulario final, sin proporcionar valores en este change.
- Comprobar si existen instrucciones oficiales adicionales de formato/duración/fecha no presentes en la transcripción actual; no se inventan límites. Si alteran obligaciones o alcance, revisar el plan antes de aplicarlas.

Estas entradas pendientes impiden DONE material mientras falten, pero no bloquean revisar el diseño. No hay decisión técnica del core pendiente en este change.
