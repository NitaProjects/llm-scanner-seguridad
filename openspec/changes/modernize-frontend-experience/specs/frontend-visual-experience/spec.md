## Purpose

Proporcionar una experiencia visual oscura, consistente y accesible que facilite configurar y seguir análisis y comprender resultados sin modificar los contratos funcionales del scanner.

## ADDED Requirements

### Requirement: FVE-01 Sistema visual oscuro coherente

**Historia:** Como operador quiero distinguir información y acciones con claridad. **Prioridad:** Alta. **Dependencias:** frontend-mvp.

La interfaz SHALL presentar fondo azul-negro, superficies diferenciadas, tipografía/espaciado/radios consistentes y acentos violeta/azul. SHALL reservar prioritariamente rojo/ámbar/verde para semántica de riesgo/estado/destrucción; cada significado SHALL tener texto además de tono. Controles nativos, diálogos y estados hover/focus/invalid/disabled/loading SHALL ser legibles en el tema oscuro. SHALL conservar iconografía uniforme sin recursos externos nuevos.

#### Scenario: Recorrido visual completo
- **WHEN** se navega entre proyectos, target y análisis y se abre el diálogo de borrado
- **THEN** todas las superficies y controles comparten el tema oscuro y las acciones destructivas/estados se identifican textualmente
- **AND** no aparecen controles claros ilegibles ni colores de riesgo usados como decoración

### Requirement: FVE-02 Navegación compacta recuperable

**Historia:** Como usuario quiero orientarme sin perder el contexto. **Prioridad:** Alta. **Dependencias:** FE-01, FE-22.

La navegación SHALL usar sidebar compacta en escritorio y navegación compacta en flujo en anchos inferiores a 1024px, con destino Proyectos y estado activo accesible. SHALL preservar rutas, enlaces directos, guard de salida, títulos, fallback, skip-link, foco por navegación y enlaces internos. SHALL NOT añadir destinos sin funcionalidad ni consultas para decorar navegación. El contexto de página SHALL derivarse de datos existentes.

#### Scenario: Acceso directo y fragmento
- **WHEN** se abre directamente un análisis y se activa un enlace a hallazgo/informe o Saltar al contenido
- **THEN** se conserva la ruta del análisis y el destino recibe desplazamiento/foco conforme al comportamiento vigente
- **AND** la navegación principal sigue disponible en móvil y escritorio

### Requirement: FVE-03 Proyectos y formularios preservados

**Historia:** Como operador quiero configurar sin ambigüedad. **Prioridad:** Alta. **Dependencias:** FE-02 a FE-07, contrato target actual.

Proyectos SHALL priorizar nombre, descripción/fecha disponibles y acciones; creación inline, centro de proyecto, target y histórico SHALL conservar sus flujos. El formulario SHALL diferenciar secciones y estados con labels, ayuda y errores asociados al campo, conservando validaciones, REST/chat, auth, limpieza de secretos, guard y reconciliación de escrituras. SHALL NOT añadir edición de Project ni editor de bindings. No se mostrarán métricas globales no provistas: contadores SHALL proceder de datos ya cargados, sin solicitudes adicionales para decoración ni convertir ausencia/error en cero.

#### Scenario: Cambio de tipo y error de guardado
- **WHEN** se usa el formulario REST/chat y falla un guardado con credencial nueva
- **THEN** permanecen confirmaciones, validaciones y comportamiento de limpieza/reconciliación vigentes
- **AND** el error aparece asociado y legible sin exponer la credencial ni reenviar automáticamente

#### Scenario: Listado vacío o fallido
- **WHEN** el listado está vacío o falla su lectura
- **THEN** presenta respectivamente un empty state con acción existente o un error recuperable
- **AND** no sustituye el fallo por cero proyectos ni consulta todos sus scans para fabricar métricas

### Requirement: FVE-04 Eliminación confirmada y feedback coherente

**Historia:** Como usuario quiero distinguir confirmación y resultado. **Prioridad:** Alta. **Dependencias:** PD-04.

El diálogo SHALL mostrar el nombre, alcance e irreversibilidad, Cancelar y acción destructiva explícita, con foco inicial/restauración y teclado accesibles. SHALL impedir duplicados durante envío. Solo tras 204 SHALL retirar el proyecto y anunciar éxito; 409 SHALL conservarlo y explicar actividad/conflicto; 404 o resultado ambiguo SHALL mantener reconciliación vigente sin DELETE automático. Cargas, vacíos, errores y éxitos SHALL tener patrón coherente, texto y acciones existentes. Errores SHALL permanecer disponibles; no depender de toast transitorio.

#### Scenario: Borrado y respuesta ambigua
- **WHEN** el usuario confirma una eliminación y la respuesta no permite confirmar éxito
- **THEN** se envía un único DELETE y se consulta el listado según el flujo existente
- **AND** ninguna animación afirma éxito ni habilita otro intento antes de reconciliación

### Requirement: FVE-05 Seguimiento honesto de análisis

**Historia:** Como operador quiero reconocer actividad y resultados. **Prioridad:** Alta. **Dependencias:** FE-08, FE-09, FE-18.

PENDING y RUNNING SHALL mostrar título/estado textual, información técnica recibida y señal indeterminada moderada. SHALL NOT representar porcentaje, ETA ni fase remota inventada. Contadores SHALL conservar denominadores reales y aclarar finalización técnica frente a éxito/cobertura. Pausa o lectura desactualizada SHALL indicarse explícitamente y detener actividad animada. COMPLETED SHALL priorizar resultados según disponibilidad; FAILED SHALL mostrar error seguro y advertir parcialidad. SHALL conservar polling, cancelación, reintentos y errores independientes vigentes, sin nuevas solicitudes ni anuncios repetitivos por ciclo.

#### Scenario: Seguimiento pausado
- **WHEN** un RUNNING agota los fallos de lectura permitidos
- **THEN** se ve el estado conocido como desactualizado y seguimiento detenido con acción vigente de reintento
- **AND** no continúa la animación sugiriendo actividad remota confirmada

#### Scenario: Transición terminal
- **WHEN** RUNNING cambia a COMPLETED o FAILED
- **THEN** cesa el indicador activo y se presenta la jerarquía terminal sin forzar foco/desplazamiento
- **AND** los bloques conservan cargas/errores propios sin convertirlos en datos ficticios

### Requirement: FVE-06 Resultados de seguridad fieles

**Historia:** Como operador quiero interpretar resultados sin confundirlos con garantías. **Prioridad:** Alta. **Dependencias:** FE-10 a FE-18, scoring-v1, report-v1.

Score SHALL destacar valor 0–100, etiqueta heurística, riesgo y conteos disponibles; SHALL distinguir severidad, confianza, estado y veredicto. Cero SHALL conservar aviso de no garantía y tono neutral. Findings SHALL conservar orden/identidad y ofrecer cabecera escaneable con detalles accesibles de descripción/evidencia/recomendación. Informe SHALL conservar snapshot, interpretación, limitaciones y trazabilidad, sin generar narrativas ni reemplazarlo con datos actuales; divergencias con score SHALL seguir visibles. Evidencia SHALL renderizarse como texto inerte, sin JSON bruto adicional ni HTML/Markdown ejecutable.

#### Scenario: Cero con inconclusos e histórico
- **WHEN** un análisis tiene score cero con inconclusos o un histórico no dispone de score/informe
- **THEN** se distingue cero real de ausencia y no se certifica seguridad
- **AND** el informe existente conserva versiones, limitaciones y su fuente histórica

#### Scenario: Finding con contenido no confiable
- **WHEN** un Finding contiene markup en su evidencia y el usuario abre sus detalles con teclado
- **THEN** lee el texto escapado, severidad/confianza diferenciadas y recomendación sin ejecución ni solicitudes nuevas

### Requirement: FVE-07 Movimiento útil y reducido

**Historia:** Como usuario quiero feedback sin distracción. **Prioridad:** Alta. **Dependencias:** FVE-04 a FVE-06.

Microinteracciones SHALL durar 120–180ms y transiciones ordinarias 180–300ms, con movimiento pequeño y sin retrasar negocio/foco. Entrada de vista/cards/feedback, apertura de modal y cambios de Scan SHALL ser discretos y no repetirse por polling; cierre instantáneo accesible SHALL ser válido. SHALL NOT animar score desde cero, usar fondos animados/partículas/blur costoso ni pulsos decorativos continuos. Con prefers-reduced-motion: reduce SHALL eliminar animaciones no esenciales, transforms, pulse/shimmer y smooth scroll, manteniendo feedback textual estático. Ninguna operación SHALL depender de animationend.

#### Scenario: Preferencia de movimiento reducido
- **WHEN** se activa reduce y se siguen scans, se abre modal o se navega
- **THEN** se mantiene funcionalidad y feedback textual sin movimiento no esencial ni esperas de animación

### Requirement: FVE-08 Accesibilidad y adaptación

**Historia:** Como usuario de teclado o pantalla pequeña quiero completar los mismos flujos. **Prioridad:** Alta. **Dependencias:** FE-21 a FE-23.

La presentación SHALL cumplir contraste WCAG AA aplicable: 4.5:1 texto normal y 3:1 texto grande/controles/foco informativos; SHALL mantener foco visible no oculto, labels/errores asociados, semántica, teclado y targets >=44px. Estados SHALL ser textuales, anuncios limitados a cambios significativos. Layout SHALL adaptarse a 1440/1280/1024/768/390/360px y reflow equivalente 320px, sin pérdida de acciones/contenido ni scroll horizontal de página; SHALL soportar zoom 200%. Métricas/categorías/headers SHALL apilarse cuando sea necesario; modal SHALL caber y permitir scroll interno; valores largos SHALL ser recuperables completos.

#### Scenario: Móvil con datos largos y teclado
- **WHEN** se abre proyecto/target/análisis con nombre, URL y evidencia largos en 360px y después se usa teclado y zoom
- **THEN** las acciones, labels, detalles y confirmación siguen accesibles sin recorte ni dependencia exclusiva del color

### Requirement: FVE-09 Regresión y validación visual trazable

**Historia:** Como mantenedor quiero comprobar la modernización sin cambiar el scanner. **Prioridad:** Alta. **Dependencias:** FVE-01 a FVE-08.

La aceptación SHALL incluir suite Angular existente y cobertura de navegación/fragmentos, formularios, modal/delete, estados Scan/carga/error/vacío, score/Findings/report histórico y ausencia de nuevas solicitudes. SHALL incluir revisión real en navegador de contraste, teclado/modal, responsive/zoom y reduce/no-preference; las pruebas DOM SHALL NOT presentarse como evidencia suficiente de layout o contraste. SHALL usar datos sintéticos sin red externa, respetar autorización de ejecución y preservar API/backend/BD/evaluación/scoring/reporting y protocolos vigentes.

#### Scenario: Evidencia de aceptación
- **WHEN** se propone completar la implementación
- **THEN** se identifican pruebas ejecutadas y pendientes y se registra matriz visual por viewport/movimiento
- **AND** cualquier fallo de compatibilidad o accesibilidad bloquea la aceptación aunque los componentes rendericen
