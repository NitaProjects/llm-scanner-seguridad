## Purpose

Dar al frontend existente una presentación profesional, coherente y accesible que facilite reconocer acciones, resultados e incertidumbre, sin alterar el comportamiento aprobado del MVP.

## Review Status

**READY FOR IMPLEMENTATION.** FP-01..FP-16 tienen prioridad MUST/MVP. Cada requisito incluye historia, dependencias y escenarios verificables. Complementan FE-01..FE-24 de `frontend-mvp`; no los sustituyen. D-01..D-26 fueron aprobadas explícitamente por el usuario el 2026-10-11 con tres aclaraciones incorporadas en FP-05, FP-06 y FP-16: SVG propios reutilizables, LOW diferenciado de CTA y QA completa con capturas representativas. Esta actualización no solicita implementar.

## ADDED Requirements

### Requirement: FP-01 Continuidad funcional y alcance visual

**Historia:** Como usuario, quiero reconocer el mismo flujo con mejor presentación para seguir trabajando sin reaprender acciones.
**Dependencias:** FE-01..FE-24; D-01/D-25.

La interfaz SHALL conservar destinos, acciones, datos, orden contractual de listas, condiciones de disponibilidad/habilitación y estados aprobados. El incremento SHALL limitarse a presentación, agrupación semántica, iconos funcionales y accesibilidad visual. No SHALL introducir nuevas rutas, consultas, persistencia, reglas de negocio ni dependencias externas. No SHALL cambiar textos de resultados, avisos contractuales o semántica para mejorar su apariencia.

#### Scenario: Misma acción con nueva apariencia
- **WHEN** se activa crear, configurar, guardar, ejecutar, cancelar, releer o abrir un análisis
- **THEN** se invoca la acción existente con su habilitación, destino y efectos previos
- **AND** ningún icono, badge o resumen introduce una consulta o acción adicional

#### Scenario: Datos extensos o adversariales
- **WHEN** nombres, URLs o Findings contienen texto largo o marcado adversarial
- **THEN** se muestran como texto inerte y ajustable sin truncar datos necesarios ni activar enlaces externos

### Requirement: FP-02 Identidad y jerarquía visual comunes

**Historia:** Como analista, quiero reconocer un único producto para interpretar cada pantalla con facilidad.
**Dependencias:** FE-21; D-01/D-04..D-08.

Todas las vistas SHALL compartir una dirección sobria azul/gris, superficies neutras, jerarquía tipográfica y ritmo de espaciado coherentes. SHALL distinguir título, descripción, acción principal, contenido y metadatos sin usar igual peso visual para todo. SHALL usar fuentes del sistema sin descargas. No SHALL usar estética hacker, emojis como iconos principales, gradientes decorativos, sombras pesadas ni superficies de riesgo saturadas.

#### Scenario: Recorrido entre páginas
- **WHEN** se recorren listado, proyecto, target, Scan y ruta desconocida
- **THEN** títulos, campos, acciones, bordes y mensajes se perciben como partes del mismo producto
- **AND** las descripciones y metadatos tienen menor énfasis que los títulos sin perder legibilidad

### Requirement: FP-03 Contenedor y densidad de contenido

**Historia:** Como usuario, quiero una composición equilibrada para localizar el siguiente paso sin grandes espacios muertos.
**Dependencias:** FE-21; D-02/D-03/D-04/D-07/D-08.

La interfaz SHALL alinear header, contenido y footer a un contenedor centrado de ancho máximo 1200px y gutters adaptables. Las páginas SHALL tener espaciado por secciones, sin alturas mínimas artificiales de tarjetas o estados vacíos ni márgenes acumulados que separen acciones de su contexto. SHALL permitir lectura cómoda de párrafos extensos mediante columnas de texto acotadas, sin limitar el ancho útil de formularios o resultados.

#### Scenario: Desktop ancho y contenido vacío
- **WHEN** se abre Projects sin datos a 1440 o 1280px
- **THEN** encabezado, CTA y estado vacío forman una composición compacta en el contenedor común
- **AND** la ausencia de datos no se representa mediante una tarjeta que ocupe la altura libre de la pantalla

### Requirement: FP-04 Shell y navegación reconocibles

**Historia:** Como usuario, quiero reconocer la marca y mi contexto para orientarme en el producto.
**Dependencias:** FE-01/FE-22; D-12.

El shell SHALL mostrar `LLM Scanner Seguridad`, un símbolo propio discreto y la navegación existente con alineación y espaciado compactos. La sección Proyectos SHALL identificar visualmente su estado activo en sus rutas y descendientes, con semántica accesible de ubicación; la ruta desconocida no SHALL aparentar sección activa. SHALL conservar salto al contenido, landmarks, retorno y gestión de foco existentes, sin añadir menús, rutas o header fijo.

#### Scenario: Página de proyecto y ruta desconocida
- **WHEN** se abre un proyecto o una ruta desconocida
- **THEN** el primer caso marca Proyectos como sección activa y el segundo no marca una ubicación inexistente
- **AND** marca y navegación conservan sus destinos y acceso por teclado

### Requirement: FP-05 Botones e iconos funcionales

**Historia:** Como usuario, quiero reconocer acciones principales y secundarias para actuar con confianza.
**Dependencias:** FE-21/FE-22; D-09/D-12/D-13/D-16/D-24.

Las acciones SHALL compartir estilos primary, secondary y text/link, con hover, focus y disabled distinguibles. Danger SHALL reservarse para una acción destructiva ya existente, sin introducirla. Los controles interactivos SHALL conservar área de al menos 44px de alto y ancho objetivo, texto explícito y tamaño visual moderado. Los iconos SHALL ser SVG inline propios, simples y coherentes, sin librería externa y evitando duplicación masiva mediante reutilización ligera; acompañarán especialmente crear, configurar, ejecutar, navegación y estados de resultados. SVG decorativo SHALL quedar fuera del árbol accesible y no recibir foco. No SHALL reemplazarse una etiqueta por un icono sin nombre accesible.

#### Scenario: Acción deshabilitada y foco
- **WHEN** una acción existente queda deshabilitada o recibe foco de teclado
- **THEN** su estado se distingue visualmente y el foco es visible
- **AND** el rediseño no cambia las condiciones que permiten activarla

#### Scenario: CTA con icono
- **WHEN** un lector de pantalla encuentra Nuevo proyecto o Ejecutar análisis con SVG
- **THEN** la etiqueta se anuncia una sola vez y el SVG decorativo no añade ruido ni foco

### Requirement: FP-06 Badges y semántica de color

**Historia:** Como analista, quiero distinguir riesgo, severidad, estado e incertidumbre para no confundir conceptos.
**Dependencias:** FE-12/FE-13/FE-15/FE-17/FE-18; D-06/D-10/D-11/D-21.

RiskLevel, Severity, estado técnico y availability SHALL presentarse con texto explícito y familia visual coherente. NO_FINDINGS SHALL ser neutro, LOW azul, MEDIUM ámbar, HIGH naranja moderado y CRITICAL rojo oscuro. INCONCLUSIVE SHALL usar ámbar con etiqueta e indicador propios, sin convertirse en RiskLevel. PENDING SHALL ser neutro, RUNNING azul, COMPLETED verde moderado y FAILED rojo; COMPLETED no SHALL implicar ausencia de vulnerabilidades. AVAILABLE/PENDING/UNAVAILABLE/NOT_APPLICABLE SHALL conservar su significado independiente por recurso, sin transformar ausencia histórica en error. El color SHALL limitarse a badges, bordes, indicadores y avisos locales.

LOW SHALL usar un azul visualmente distinto de primary/CTA. Los badges de riesgo SHALL tener apariencia informativa estática, sin hover de acción, cursor interactivo o foco de control ni semántica de botón/enlace.

#### Scenario: Cero y resultados inconclusos
- **WHEN** se muestra NO_FINDINGS con inconclusiveCount mayor que cero
- **THEN** se distinguen el riesgo neutro y el aviso ámbar de incertidumbre con texto visible
- **AND** no se muestra una certificación verde de seguridad

#### Scenario: Severidad y estado completado
- **WHEN** un Scan COMPLETED contiene un Finding HIGH
- **THEN** estado técnico y severidad mantienen etiquetas y estilos independientes sin recolorear toda la página

#### Scenario: Riesgo LOW junto a una acción principal
- **WHEN** un badge LOW y una acción primary aparecen en la misma vista
- **THEN** LOW conserva un azul desaturado y superficie informativa distintos del azul sólido del CTA
- **AND** el badge no sugiere interacción ni entra en el recorrido de foco como control

### Requirement: FP-07 Projects y primera creación

**Historia:** Como usuario nuevo, quiero entender qué hacer en Projects para comenzar un análisis autorizado.
**Dependencias:** FE-02/FE-21/FE-22; D-13/D-14.

Projects SHALL tener título/descripción compactos y Nuevo proyecto integrado en el encabezado. El estado vacío SHALL mostrar un símbolo ligero, mensaje claro, explicación breve y CTA principal que abra el formulario existente en la misma página. Al mostrarse la creación, las acciones de apertura redundantes SHALL respetar showForm y el formulario SHALL conservar enfoque, cancelación y retorno al control original. El listado con datos SHALL mostrar entradas compactas con nombre, descripción disponible y fecha, preservando orden y contenidos sin ellipsis que los haga inaccesibles.

#### Scenario: Primera apertura sin proyectos
- **WHEN** la lectura válida devuelve una lista vacía
- **THEN** se muestra mensaje, explicación y CTA Nuevo proyecto sin tarjeta de altura exagerada
- **AND** el CTA usa la apertura existente, sin modal ni nueva ruta

#### Scenario: Proyectos con descripciones largas
- **WHEN** se muestran varios proyectos con nombres y descripciones extensos
- **THEN** sus filas se ajustan sin solapamientos y el nombre/enlace tiene prioridad sobre fecha y descripción
- **AND** no se recorta ni reordena la colección

### Requirement: FP-08 Centro de trabajo del proyecto

**Historia:** Como usuario, quiero reconocer preparación, siguiente acción e histórico para trabajar desde el proyecto.
**Dependencias:** FE-03/FE-07; D-15.

Project SHALL priorizar nombre y descripción, target y acción principal contextual, seguidos del histórico. El bloque target SHALL agrupar metadatos públicos y acciones con densidad moderada; el histórico SHALL usar filas legibles sin tarjetas repetidas innecesarias. SHALL preservar los avisos de autorización y el acceso a datos parciales cuando otro bloque falle.

#### Scenario: Target ausente, configurado o Scan activo
- **WHEN** se visualiza cualquiera de esas situaciones confirmadas
- **THEN** Configurar target, Ejecutar análisis o Ver análisis en curso ocupa el lugar principal correspondiente
- **AND** la presentación no modifica las condiciones existentes ni obtiene scores del histórico

### Requirement: FP-09 Formulario target agrupado y completo

**Historia:** Como usuario, quiero comprender el formulario para configurar correctamente el endpoint autorizado.
**Dependencias:** FE-04..FE-06/FE-22/FE-23; D-16.

El formulario SHALL distinguir Conexión, Petición, Respuesta y Autenticación mediante secciones y separadores claros, con labels próximos, ayudas legibles y errores locales. Cabeceras dinámicas SHALL conservar asociación nombre/valor y acciones de añadir/quitar accesibles. JSON, constantes, responsePath, auth y credenciales SHALL permanecer visibles cuando correspondan a las condiciones originales. No SHALL introducir acordeones, wizard, revelado de secretos, agrupaciones que cambien FormGroup/FormArray ni ocultación estética de avisos. Guardar/Cancelar SHALL estar juntos al final, sin barra fija.

#### Scenario: Cabeceras y autenticación en móvil
- **WHEN** se visualizan varias cabeceras y un mecanismo autenticado a 360px
- **THEN** cada nombre/valor/acción se apila de forma reconocible y ningún input, label o ayuda se sale del contenedor
- **AND** el secreto conserva type password y los mismos controles, condiciones y avisos

#### Scenario: Validación y guardado
- **WHEN** existen errores de campo o guardado en curso
- **THEN** labels, ayudas, errores y estado disabled siguen siendo perceptibles sin desplazar la acción fuera de su contexto

### Requirement: FP-10 Jerarquía de Scan y resumen de riesgo

**Historia:** Como analista, quiero reconocer primero detecciones e incertidumbre para interpretar el resultado.
**Dependencias:** FE-09..FE-13/FE-15/FE-18; D-17/D-18.

Scan SHALL situar estado y fechas en una cabecera compacta y score/RiskLevel/findingCount/inconclusiveCount disponibles en su primer bloque de resultados. SHALL diferenciar categorías y Findings de información técnica, evaluaciones y contenido documental usando orden y jerarquía visual. El score SHALL mostrar número legible y `/100`, con explicación heurística visible, sin gauge, donut ni apariencia de probabilidad calibrada. El resumen y los inconclusos no SHALL ocultarse tras interacción. RUNNING SHALL priorizar progreso técnico confirmado, sin cifras de resultado ficticias.

#### Scenario: Scan COMPLETED con Findings
- **WHEN** el Scan tiene score AVAILABLE y hallazgos
- **THEN** score, RiskLevel y ambos contadores aparecen antes de categorías y detalles
- **AND** metadatos, severidades y versiones no compiten visualmente con ese resumen

#### Scenario: Score cero o no disponible
- **WHEN** globalScore es 0 o la puntuación no está disponible
- **THEN** el primer caso muestra 0/100 y su advertencia completa, y el segundo su estado sin número inventado
- **AND** inconclusiveCount y su advertencia se mantienen visibles cuando existan

### Requirement: FP-11 Findings reconocibles y expandibles

**Historia:** Como analista, quiero explorar Findings con claridad para priorizar recomendaciones.
**Dependencias:** FE-14/FE-22/FE-23; D-19.

Cada Finding SHALL mostrar severity badge, categoría y título reconocibles antes de expandir; la confianza SHALL tener jerarquía secundaria. Los hallazgos SHALL separarse con bordes/espaciado, sin anidación repetida de cards ni fondos completos de alarma. El detalle expandible existente SHALL conservar descripción, resumen de evidencia y recomendación literales, orden, IDs, links y manejo nativo por teclado. La gravedad alta SHALL destacar con acento local sin ocultar los demás hallazgos.

#### Scenario: Alta y baja severidad juntas
- **WHEN** hay Findings HIGH y LOW con títulos extensos
- **THEN** se mantiene el orden aprobado, ambos títulos se leen completos y HIGH destaca mediante badge/acento local
- **AND** abrir y cerrar cada detalle permite acceder a sus tres bloques sin cambiar contenido

### Requirement: FP-12 Informe documental sin duplicación

**Historia:** Como lector, quiero leer alcance y limitaciones con calma para entender los límites del análisis.
**Dependencias:** FE-16/FE-17; D-20.

Informe SHALL usar jerarquía documental de alcance, resumen/referencia existente, interpretación, limitaciones y trazabilidad secundaria. SHALL mantener visibles las cuatro interpretaciones y nueve limitaciones literales y ordenadas cuando existe informe. No SHALL reproducir listas de Findings/evaluaciones, esconder limitaciones tras acordeones ni modificar la detección/presentación de discrepancias. Las referencias, IDs y enlaces internos SHALL seguir utilizables con textos largos y en móvil.

#### Scenario: Informe completo y summary coincidente
- **WHEN** report y score están disponibles y sus resúmenes coinciden
- **THEN** Informe referencia el resumen principal, muestra alcance e interpretación y deja las nueve limitaciones legibles
- **AND** la trazabilidad conserva sus detalles existentes sin repetir narrativas de hallazgos

### Requirement: FP-13 Estados vacíos, carga, error y éxito

**Historia:** Como usuario, quiero distinguir actividad, ausencia y fallos para saber qué puedo hacer.
**Dependencias:** FE-17..FE-19/FE-22; D-14/D-21.

Los estados SHALL compartir patrón de icono discreto, texto y acción existente cuando corresponda. Loading SHALL mantener mensaje explícito sin demoras simuladas; empty SHALL explicar ausencia válida; error SHALL conservar mensaje seguro y reintento permitido; éxito SHALL confirmar únicamente lo confirmado por la aplicación. FAILED SHALL destacar como fallo técnico con datos conservados accesibles; NOT_APPLICABLE SHALL ser nota histórica neutra. La ruta desconocida SHALL mostrar título, explicación y retorno a proyectos dentro del lenguaje visual global.

#### Scenario: Error parcial e histórico
- **WHEN** falla un bloque y otro recurso devuelve NOT_APPLICABLE
- **THEN** el error tiene tratamiento local y la ausencia histórica tiene tratamiento neutro independiente
- **AND** no se sustituye el resto de la pantalla por un error global

#### Scenario: Ruta desconocida
- **WHEN** se abre una dirección no reconocida
- **THEN** se muestra una composición compacta Página no encontrada con explicación y enlace Volver a proyectos

### Requirement: FP-14 Responsive sin pérdida de contenido

**Historia:** Como usuario, quiero consultar resultados y configurar targets desde distintos tamaños de pantalla.
**Dependencias:** FE-21; D-02/D-03/D-22.

Todas las vistas SHALL funcionar a 1440, 1280, 1024 y 768px, con uso básico completo a 360px y sin scroll horizontal global. Header, acciones, formulario, resumen y Findings SHALL envolver o apilarse manteniendo orden de lectura y foco. URLs, UUIDs y texto extenso SHALL ajustarse; los bloques de texto técnico no SHALL forzar anchura global. No SHALL ocultarse información o acciones para conseguir caber, ni disimularse overflow con recorte global.

#### Scenario: Tablet y móvil con contenido largo
- **WHEN** se muestran target o Scan con URLs, IDs, JSON y títulos largos a 768 o 360px
- **THEN** se conservan etiquetas, datos y acciones sin solapamiento ni desplazamiento horizontal de toda la página
- **AND** el resumen pasa de columnas a apilado sin cambiar el orden semántico

### Requirement: FP-15 Accesibilidad y movimiento discreto

**Historia:** Como usuario de teclado o lector de pantalla, quiero mantener control y comprensión tras el rediseño.
**Dependencias:** FE-22/FE-23; D-23/D-24.

La presentación SHALL mantener labels, ayudas/errores asociados, landmarks, headings, foco visible, teclado y anuncios existentes sin anunciar cada tick. Texto normal SHALL alcanzar contraste 4.5:1 y texto grande/indicadores esenciales/controles 3:1; disabled SHALL seguir siendo identificable. Color e iconografía SHALL acompañarse de texto. Transiciones SHALL limitarse a color/borde/sombra de hover/focus durante 120–160ms, sin movimiento decorativo, y SHALL desactivarse con prefers-reduced-motion. No SHALL añadirse loaders animados, reordenación por CSS ni controles inaccesibles.

#### Scenario: Recorrido con teclado y SVG
- **WHEN** se recorren header, creación, formulario y detalles de Finding sin ratón
- **THEN** el foco se ve, el orden es lógico, los controles tienen nombres claros y los SVG decorativos no reciben foco

#### Scenario: Preferencia de movimiento reducido
- **WHEN** el sistema solicita prefers-reduced-motion
- **THEN** no hay transiciones nuevas ni desplazamiento suave, y toda la información y acciones siguen disponibles

### Requirement: FP-16 QA visual trazable y regresión acotada

**Historia:** Como mantenedor, quiero evidencia de las vistas y estados críticos para aprobar un acabado coherente.
**Dependencias:** FE-24; FP-01..FP-15; D-26.

La entrega visual SHALL documentar revisión de Projects vacío/con datos, Project detail, Target form, Scan RUNNING, COMPLETED con Findings, score 0, INCONCLUSIVE, FAILED e histórico NOT_APPLICABLE en los cinco anchos acordados. SHALL incluir teclado, contraste, textos largos, estados de interacción y ruta desconocida. La evidencia SHALL usar datos sintéticos públicos sin credenciales ni servicios reales. Los tests funcionales existentes SHALL conservarse; solo SHALL adaptarse/añadirse casos por markup necesario, accesibilidad o estados visuales relevantes. Las comprobaciones de ejecución SHALL requerir solicitud expresa; la planificación no SHALL afirmar QA o suite superados sin evidencia.

La QA SHALL validar y registrar las 50 combinaciones. SHALL conservar capturas representativas de los estados y anchos principales, sin exigir 50 capturas ni una por combinación; un registro textual de cada caso SHALL permitir distinguir validación completa de cobertura fotográfica selectiva.

#### Scenario: Revisión antes de cerrar la implementación
- **WHEN** se solicite y realice QA del frontend implementado
- **THEN** se registran viewport, escenario y resultado para las 50 combinaciones, además de los controles transversales, y se conservan capturas representativas con su correspondencia al registro
- **AND** los fallos se corrigen y las combinaciones afectadas se revisan antes de cerrar

#### Scenario: Planificación sin ejecución frontend
- **WHEN** se entrega este change de diseño
- **THEN** la matriz permanece pendiente, se distingue strict de QA visual y no se ejecutan compilaciones, tests o servidores frontend
