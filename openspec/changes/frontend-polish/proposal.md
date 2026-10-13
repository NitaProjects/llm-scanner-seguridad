## Why

El frontend funcional de `frontend-mvp` está implementado y aprobado, pero su presentación tiene poca jerarquía, espaciado poco equilibrado y estados vacíos demasiado básicos. Este incremento exclusivamente visual busca una herramienta SaaS de seguridad sobria, reconocible y legible, conservando íntegro el flujo aprobado.

## What Changes

- Refinar shell/header, ProjectsPage, ProjectPage, TargetPage, ScanPage y NotFoundPage sobre los componentes existentes.
- Definir un conjunto pequeño de tokens CSS centrales, tipografía del sistema, superficies, espaciado, botones, badges e iconos SVG inline propios y funcionales mediante un patrón reutilizable ligero, sin duplicación masiva ni librería externa. Mantener LOW azul, visualmente distinto de primary/CTA y sin apariencia interactiva.
- Compactar el listado de proyectos y su creación integrada; trabajar los estados vacíos con explicación y acción existente.
- Reforzar la prioridad visual de score, RiskLevel, hallazgos e inconclusos; diferenciar categorías, detalle, evaluaciones e informe documental.
- Agrupar visualmente el formulario target sin ocultar campos, ayudas, errores ni advertencias contractuales.
- Especificar responsive a 1440/1280/1024/768/360px, accesibilidad y QA visual reproducible de diez estados representativos: validar las 50 combinaciones y registrar todas, conservando capturas representativas de estados/anchos principales, sin exigir una captura por combinación.

## Non-Goals

No implementar código en esta fase. No cambiar comportamiento, rutas/destinos, acciones, estado, condiciones de habilitación, polling, formularios/validadores, secretos, DTOs, modelos, servicios, API, OpenAPI ni backend. No reabrir decisiones funcionales de `frontend-mvp`, migrar estilos a otro framework ni añadir dependencias, fuentes remotas, librerías de iconos, charts, login, dashboard, dark mode, demo-vulnerable o AWS. No modificar los históricos de otros changes ni declarar completada la entrega TFM.

## Capabilities

### New Capabilities

- `frontend-polish`: contrato adicional de presentación visual, responsive y accesibilidad del frontend existente. Es una capacidad de presentación verificable, no una nueva función de negocio.

### Modified Capabilities

Ninguna. FE-01..FE-24 de `frontend-mvp` siguen vigentes; los requisitos FP complementan su presentación sin sustituirlos. Las specs aprobadas siguen en `openspec/changes/`, sin consolidación en este change.

## Impact

- Implementación futura limitada a CSS, templates y metadatos/imports estrictamente necesarios de presentación en `frontend/src/`, con un único componente ligero de iconos propios como excepción acotada de reutilización. Renderizará SVG inline estático, sin estado de negocio; no se crea una nueva capa de componentes ni un design system general.
- Reutilización de `styles.css`, `app.css`, `shared/ui/page.css`, StatusBadge, FeedbackPanel, formularios y componentes de resultados. No cambia ninguna API pública de esos componentes.
- Tests existentes solo se ajustarán si lo exige markup o un contrato accesible afectado. Sin reescritura funcional, infraestructura visual nueva, cambios de configuración, dependencias o lockfiles.
- Documentación de objetivos, tokens y reglas visuales contenida en `design.md`; QA y evidencia futura en `tasks.md`. Tras implementar, solo actualizar la descripción visual directamente afectada de `frontend/README.md`, sin crear manuales adicionales ni una ADR para decisiones cosméticas locales.
- Fuentes: instrucciones de proyecto/rol, configuración OpenSpec, contrato y diseño aprobados de `frontend-mvp`, sección frontend de `docs/02-arquitectura/arquitectura-general.md`, ADR-005 y ADR-008, README frontend y CSS/templates actuales. No se ha identificado una contradicción que obligue a cambiar comportamiento.
- Estado: **READY FOR IMPLEMENTATION**. D-01..D-26 aprobadas explícitamente por el usuario el 2026-10-11, con tres aclaraciones incorporadas: reutilización ligera de SVG propios, LOW azul distinguible de CTA y QA de 50 combinaciones con capturas representativas. El resto de decisiones se mantiene sin cambios. Esta petición actualiza exclusivamente planificación; no solicita implementar.
