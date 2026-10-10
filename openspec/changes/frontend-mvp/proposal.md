## Why

El backend core aprobado permite completar el análisis, pero el frontend actual conserva la bienvenida de Angular y no permite utilizar ese flujo visualmente. El MVP necesita una interfaz clara, segura y demostrable en el TFM que consuma los resultados existentes sin inventar lógica de negocio ni ampliar contratos.

## What Changes

- Proponer cuatro páginas standalone: listado y creación de proyectos, detalle del proyecto con histórico, configuración de su único target y detalle del Scan con progreso y resultados.
- Consumir exclusivamente los contratos REST aprobados de proyectos, targets, scans, ejecuciones, evaluaciones, Findings, scoring-v1 y report-v1. Mantener siete campos y cuatro estados de Scan; inicio sin body y sin selección de pruebas.
- Definir formularios reactivos tipados, validaciones UX básicas, errores sanitizados y credenciales de escritura efímera, con conservación/reemplazo y transiciones NONE/Bearer/API key exactamente según TC-10: eliminación solo al guardar válidamente NONE y credencial nueva al cambiar a otro mecanismo autenticado.
- Definir polling acotado a la página activa, estados confirmados y contadores técnicos, sin porcentajes ni fases supuestas. Tras tres errores consecutivos detenerlo y ofrecer Reintentar mediante GET inmediato; solo un éxito activo reactiva el seguimiento. Diferenciar fallos de lectura, fallo técnico global e incertidumbre semántica.
- Detener inmediatamente el polling al observar COMPLETED y cargar evaluations, findings, score y report una vez, sin polling adicional de disponibilidades: el backend confirma score+report+COMPLETED atómicamente. Tratar NOT_APPLICABLE histórico como normal.
- Presentar arriba score/riesgo/Findings/inconclusos; después categorías, detalles y evaluaciones. El informe prioriza scope, summary, interpretation, limitations y metadatos públicos, sin repetir listas de Findings/evaluaciones. Respetar cero, las cuatro disponibilidades independientes de score e informe, históricos y FAILED.
- Reutilizar Angular 21, RxJS, Vitest y CSS existentes; proponer signals locales, servicios HttpClient y DTOs manuales. Sin librería UI, store global ni dependencias nuevas.
- Definir castellano, responsive, accesibilidad mínima, render de texto inerte, configuración API por entornos y pruebas aisladas del backend real.

## Non-Goals

Esta entrega crea únicamente planificación. No implementa frontend, backend ni demo-vulnerable; no modifica endpoints, OpenAPI, migraciones, scoring, evaluador o reporting. No añade login, dashboard global, métricas históricas, selección de tests, cancelación/reintento de trabajo, SSE/WebSocket, exportación/descarga/PDF/email, i18n, proveedores, AWS ni configuración de despliegue. No acredita experimento académico, operación real de OpenAI ni entrega TFM completados.

## Capabilities

### New Capabilities

- `frontend-mvp`: interfaz Angular para Project → TargetConfiguration → Scan → progreso → Findings → ScanScore → SecurityReport, incluyendo estados UX, seguridad y pruebas de consumo contractual.

### Modified Capabilities

Ninguna. Se consumen los siete contratos aprobados con sus extensiones prospectivas; no se modifica su alcance histórico ni se consolidan specs anteriores.

## Impact

- Implementación futura en `frontend/src/`, configuración frontend mínima de entornos/proxy de desarrollo y tests ya soportados por `frontend/angular.json`. Sin cambios de versiones, dependencias ni lockfile.
- El baseline observado es Angular core/router 21.2.22 y CLI/build 21.2.23 resueltos en lockfile; standalone, TypeScript estricto 5.9 y RxJS 7.8; Vitest/jsdom. Rutas vacías, bienvenida de CLI y sin HttpClient, environments, componentes de producto o design system.
- Fuentes: `AGENTS.md`, `frontend/AGENTS.md`, perfil architecture-agent, `openspec/config.yaml`; specs de project-management, target-configuration, scan-execution, security-test-execution, evaluation-engine, scoring-engine y reporting; `docs/05-api/openapi.yaml`, alcance, arquitectura/decisiones, estrategia de evaluación, modelo de datos y checklist/requisitos TFM. Inventario y referencias precisas en design.
- Documentación futura directamente afectada: arquitectura (sección frontend), alcance (estado real), README frontend y checklist solo si existe evidencia nueva. Este change de planificación no cambia sus estados ni crea ADR aceptadas prematuramente.
- El usuario aprobó explícitamente D-01..D-30 el 2026-10-10 con cuatro aclaraciones incorporadas a requisitos, diseño y tareas: recuperación del polling, carga final, transiciones de autenticación y ausencia de duplicación visual del informe. Estado **READY FOR IMPLEMENTATION**; esta petición actualiza exclusivamente planificación y no solicita implementar. El servidor de publicación deberá servir deep links y enrutar `/api`; esa configuración operativa queda fuera de este change.
