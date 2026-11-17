# Instrucciones del frontend

Estas instrucciones son específicas de `frontend/` y complementan las reglas del `AGENTS.md` situado en la raíz del repositorio.

## Stack

- Angular 21.
- TypeScript.
- Aplicación standalone.
- Routing de Angular.
- Node.js 20.19.4. +
- npm 10.8.2.

El frontend consume exclusivamente la API REST del backend. No debe conectarse directamente a PostgreSQL ni contener lógica de persistencia.

## Fuente de verdad

OpenSpec es la fuente de verdad funcional: `openspec/specs/` contiene los contratos consolidados y `openspec/changes/` los cambios en curso y contratos aún no consolidados. Consulta el [índice técnico](../docs/README.md) para la documentación vigente.

Antes de implementar una pantalla, flujo o servicio:

1. Revisar la especificación o el cambio correspondiente en `openspec/`.
2. Revisar los contratos de API en `docs/05-api/openapi.yaml`.
3. Revisar las decisiones arquitectónicas aplicables en `docs/02-arquitectura/`.

Si el comportamiento solicitado no está definido o contradice la documentación, señalarlo antes de implementarlo.

## Organización del código

Cuando el proyecto crezca, organizar el código por funcionalidades de negocio, evitando una estructura basada únicamente en tipos técnicos globales.

Una estructura orientativa es:

```text
src/app/
├── core/              # Servicios singleton, guards, interceptors y modelos globales
├── shared/            # Componentes, directivas y pipes reutilizables
├── layout/            # Layouts y navegación principal
└── features/          # Funcionalidades de negocio
    ├── projects/
    ├── scans/
    ├── findings/
    └── reports/
```

No crear capas o carpetas adicionales sin necesidad. Mantener los componentes pequeños y con una única responsabilidad.

## Convenciones Angular

- Utilizar componentes standalone.
- Utilizar signals cuando mejoren la claridad del estado local o de la vista.
- Utilizar servicios para la comunicación con la API y la lógica reutilizable.
- Evitar lógica de negocio compleja dentro de templates.
- Mantener tipados los modelos, formularios y respuestas HTTP.
- Validar formularios en el frontend sin sustituir la validación del backend.
- Centralizar la configuración de URLs de API en los entornos de Angular.
- Mantener el tratamiento de errores y estados de carga consistente.
- No introducir librerías de UI o estado global sin autorización.
- No almacenar secretos, tokens sensibles ni credenciales en el frontend.

## API y seguridad

- Respetar el contrato definido en `docs/05-api/openapi.yaml`.
- No inventar endpoints, campos o códigos de error sin documentarlos y autorizarlos.
- Tratar todas las respuestas del backend como datos no confiables.
- Escapar o sanitizar contenido generado por modelos LLM antes de mostrarlo.
- No representar HTML o Markdown generado por un LLM como contenido confiable sin sanitización.
- No mostrar por defecto credenciales, cabeceras de autenticación ni datos sensibles incluidos en evidencias.
- Gestionar errores de red, timeout y respuestas inesperadas sin exponer detalles internos.

## Estilos y accesibilidad

- Respetar los estilos y componentes existentes.
- Evitar estilos globales innecesarios.
- Mantener una interfaz responsive y usable en resoluciones habituales.
- Utilizar HTML semántico, labels asociados y navegación accesible por teclado.
- Los niveles de riesgo deben comunicarse mediante texto además de color.

## Validaciones

No ejecutar por defecto compilaciones, tests, linters, formatters, type-checkers, servidores ni instalaciones de npm. Solo hacerlo cuando el usuario lo solicite expresamente.

Después de modificar el frontend, indicar los comandos que el usuario puede ejecutar para validarlo.
