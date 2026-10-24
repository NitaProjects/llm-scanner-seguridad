## Purpose

Definir la aceptación verificable de la entrega académica de LLM Scanner Seguridad mediante documentación, acceso y material audiovisual reales. Este contrato del paquete de entrega no añade funcionalidades ni requisitos al dominio del scanner.

## ADDED Requirements

### Requirement: TFD-01 Auditoría trazable de requisitos oficiales

La preparación de la entrega SHALL relacionar cada obligación oficial con evidencia existente, estado y pendiente concreto, distinguiendo evidencia local, producción, confirmación del operador y verificación todavía no realizada. SHALL separar obligaciones, mejoras opcionales y pendientes técnicos no bloqueantes, sin modificar requisitos oficiales ni completar tareas históricas por inferencia.

#### Scenario: Estados documentales antiguos
- **WHEN** la documentación conserva estados que no reflejan evidencia posterior
- **THEN** el autor registra la discrepancia y propone reconciliar únicamente la documentación afectada con referencias fechadas, sin reejecutar validaciones ni alterar silenciosamente contratos o registros históricos

#### Scenario: Restore aplazado
- **WHEN** se revisa la aceptación académica con aws-production-readiness en 24/25 y restore 6.3 pospuesto
- **THEN** ese pendiente permanece identificado y no bloquea la entrega ni se declara ejecutado

### Requirement: TFD-02 README autosuficiente y directorio completo

La entrega SHALL incluir un README raíz detallado con overview, stack, instalación, ejecución, estructura y funcionalidades principales, además de la URL pública y referencias reales a GitHub, slides y vídeo. Antes de H SHALL explicar el acceso Basic Auth únicamente mediante la expresión «las credenciales se facilitarán por canal privado», sin valores ni afirmaciones de entrega ya realizada. Solo en H, tras confirmación efectiva, SHALL registrarse la entrega privada sin valores. El directorio SHALL contener documentación, información de despliegue y slides válidas o su enlace.

#### Scenario: Lectura desde GitHub
- **WHEN** el evaluador abre el README de la revisión entregada
- **THEN** puede comprender el proyecto y sus instrucciones sin depender exclusivamente de docs, distinguir AWS de la demo/validación local y localizar los entregables y el modo privado de obtener acceso

### Requirement: TFD-03 Repositorio accesible al evaluador y contenido seguro

La entrega SHALL disponer de una URL GitHub real accesible para el evaluador y una revisión de seguridad del working tree publicable, historial alcanzable y material de entrega. La revisión SHALL clasificar como bloqueantes los secretos/credenciales reales: passwords, API keys, credenciales AWS, claves AES, claves privadas TLS, tokens incluidos ECR, valores SecureString, htpasswd/hash si expone material sensible y secretos equivalentes, también cuando aparezcan en manifests u otros artefactos. Account ID, Instance ID, IP pública, AMI ID, ECR URI y otros identificadores operativos SHALL considerarse no secretos por sí mismos: se minimizarán o anonimizarán cuando no aporten valor, sin bloquear la entrega ni exigir saneamiento histórico únicamente por su presencia. Solo SHALL elevarse un identificador a bloqueo ante un riesgo concreto documentado. La revisión SHALL usar resultados redactados sin abrir archivos de secretos ni exponer valores. La URL pública del deployment puede documentarse.

#### Scenario: URL no verificada
- **WHEN** solo existe una URL propuesta o un remote local sin comprobación de acceso
- **THEN** GitHub permanece pendiente hasta una comprobación autorizada de acceso del evaluador a la revisión y sus documentos

#### Scenario: Repositorio privado justificado e invitación enviada
- **WHEN** el autor justifica acceso controlado para el proyecto de seguridad y confirma invitación enviada al evaluador designado, pendiente de aceptación
- **THEN** la entrega SHALL admitir el repositorio privado sin exigir visibilidad pública; un 404 sin autenticación SHALL considerarse esperado
- **AND** la URL/revisión e inventario SHALL permitir cerrar 3.1; la invitación acredita acceso preparado, pero 3.3 SHALL permanecer pendiente exclusivamente de la aceptación del destinatario para acreditar acceso efectivo, sin atribuir al operador acciones de un tercero

#### Scenario: Hallazgo sensible
- **WHEN** la revisión detecta un secreto/credencial real o un identificador con riesgo concreto documentado
- **THEN** se registra solo tipo, ubicación, riesgo y estado sanitizados, se bloquea la publicación afectada y se solicita tratamiento específico sin publicar el valor ni reescribir historial, rotar credenciales o modificar producto automáticamente

#### Scenario: Identificadores operativos sin riesgo concreto
- **WHEN** el working tree o historial contienen Account ID, Instance ID, IP pública, AMI ID, ECR URI u otros identificadores operativos sin riesgo concreto documentado
- **THEN** su presencia no bloquea la entrega ni exige saneamiento histórico; se minimizan o anonimizan cuando no aportan valor

### Requirement: TFD-04 Acceso académico verificable sin secretos públicos

La fase D SHALL limitarse a comprobar, bajo autorización de acceso, la URL https://llmscannerseguridad.com con HTTPS válido, funcionamiento de Basic Auth y acceso a UI/API, y preparar usuario/contraseña para entrega privada. SHALL documentar únicamente «las credenciales se facilitarán por canal privado», sin exigir ni afirmar su entrega al tribunal antes de H. La confirmación administrativa efectiva SHALL realizarse en H junto con el formulario final, sin valores en evidencia pública. SHALL preservar evaluator desactivado y demo ausente en AWS, sin activar Scans ni modificar infraestructura para esta comprobación.

#### Scenario: Acceso desde una sesión nueva
- **WHEN** se realiza la comprobación final autorizada sin credencial y después con credencial privada
- **THEN** el acceso sin credencial es rechazado y el autorizado permite la UI y una lectura API, registrándose fecha, resultado y procedencia sin Authorization, cookies ni contraseña

#### Scenario: Preparación técnica sin entrega administrativa
- **WHEN** Basic Auth funciona pero no consta entrega privada al tribunal
- **THEN** la preparación técnica se conserva como acreditada, se puede continuar a E–G y la entrega administrativa queda pendiente para H junto con el formulario final

### Requirement: TFD-05 Slides reales y coherentes

La entrega SHALL incluir una presentación válida mediante archivo o URL pública documentada, con problema, objetivos, alcance, arquitectura, flujo, seguridad, validación, despliegue, limitaciones y conclusiones. SHALL distinguir resultados locales de AWS y validación operativa de experimento académico formal. El formulario SHALL disponer de una URL de slides, que puede ser el enlace accesible al archivo en GitHub.

#### Scenario: Archivo como medio principal
- **WHEN** se elige entregar las slides como documento en el repositorio
- **THEN** el archivo abre correctamente, su enlace público real figura en la documentación y el formulario, y no se exige un segundo servicio de presentación

### Requirement: TFD-06 Vídeo explicado personalmente con pantalla

La entrega SHALL disponer de un vídeo reproducible con explicación propia del alumno y captura de pantalla durante la explicación, publicado con URL pública real documentada. El rostro SHALL considerarse opcional. El vídeo SHALL excluir secretos, credenciales y metadatos operativos innecesarios; SHALL representar resultados ya existentes sin fabricar evidencia ni efectuar nuevas llamadas al evaluator por motivo de grabación.

#### Scenario: Guion sin grabación
- **WHEN** solo existe guion, slides o una URL placeholder
- **THEN** el requisito de vídeo permanece pendiente

#### Scenario: Release candidate antes del vídeo
- **WHEN** código, documentación técnica, slides y controles de seguridad/acceso están validados y el autor congela el repositorio antes de grabar
- **THEN** la preparación y grabación SHALL realizarse fuera del repositorio, sin exigir ni crear un guion versionado
- **AND** los únicos cambios posteriores esperados SHALL ser la URL final del vídeo público/no listado y estados/checklist/tasks/evidence de entrega, incluyendo el cierre administrativo y la aceptación externa de GitHub cuando se produzca; no se declara el vídeo realizado ni se amplía el producto

#### Scenario: Revisión del vídeo publicado
- **WHEN** el reviewer reproduce el vídeo por su enlace final
- **THEN** comprueba voz explicativa del alumno, pantalla visible, contenido coherente y ausencia de datos sensibles; una presentación sin explicación propia o sin pantalla no satisface el requisito

### Requirement: TFD-07 Formulario y definición de DONE

El cierre en H SHALL permitir completar nombre, email de inscripción, URL GitHub, deployment URL, slides URL, video URL y credenciales de prueba, manteniendo datos personales y valores de credenciales en el canal/formulario privado. En H SHALL confirmarse la entrega efectiva de credenciales junto con el formulario final, sin registrar sus valores públicamente. La entrega SHALL declararse DONE únicamente con todas las obligaciones acreditadas, enlaces/archivos accesibles, revisión pública sin hallazgos bloqueantes y confirmación privada de acceso; ni esta planificación ni OpenSpec strict acreditan la entrega material.

#### Scenario: Cierre con enlaces pendientes
- **WHEN** falta un entregable obligatorio, un enlace real, acceso al repositorio o la entrega privada de credenciales
- **THEN** la checklist refleja el pendiente y la entrega no es DONE aunque el producto funcione

#### Scenario: Cierre sin ampliar el MVP
- **WHEN** todas las obligaciones tienen evidencia y solo quedan mejoras opcionales o pendientes técnicos expresamente no bloqueantes
- **THEN** la entrega puede aceptarse sin restore 6.3, PDF reporting, refactors, nuevas features, categorías OWASP, pruebas del evaluator, autenticación interna ni cambios visuales
