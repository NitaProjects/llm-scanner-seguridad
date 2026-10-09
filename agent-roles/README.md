# Perfiles de agentes del proyecto

## Estado del mecanismo

El repositorio no contiene un mecanismo nativo de agentes especializados. `AGENTS.md` proporciona instrucciones de proyecto y `.agents/skills/` contiene skills reutilizables, especialmente workflows de OpenSpec. Los archivos de esta carpeta son perfiles de instrucciones reutilizables que deben cargarse explícitamente en una sesión nueva.

Todos los perfiles heredan las reglas de:

- `AGENTS.md`.
- `frontend/AGENTS.md` cuando se trabaje en `frontend/`.
- `backend/AGENTS.md` cuando se trabaje en `backend/`.

La documentación del producto y las especificaciones OpenSpec son fuentes de contexto. Ningún perfil puede modificar requisitos o decisiones importantes por su cuenta.

## Perfiles disponibles

- `documentation-agent.md`: documentación, memoria y consistencia terminológica.
- `architecture-agent.md`: diseño, arquitectura, OpenSpec y ADRs.
- `implementation-agent.md`: implementación controlada en código.
- `review-agent.md`: revisión y findings sin modificar por defecto.

## Cómo activar un perfil

En una sesión nueva, situada en la raíz del proyecto, indica explícitamente el perfil que quieres usar y pídele que lea el archivo correspondiente. Por ejemplo:

```text
Actúa como documentation-agent para LLM Scanner Seguridad.
Lee primero AGENTS.md, agent-roles/README.md y agent-roles/documentation-agent.md.
Después revisa únicamente los archivos relevantes para mi petición.
No implementes código ni modifiques archivos fuera del alcance del perfil.
Mi tarea es: [describir aquí la tarea].
```

Sustituye `documentation-agent` por `architecture-agent`, `implementation-agent` o `review-agent` según corresponda y utiliza la ruta del perfil elegido.

## Regla de selección

- Usar `documentation-agent` para redactar o consolidar documentación.
- Usar `architecture-agent` para analizar diseño, OpenSpec, contratos, dominio o decisiones.
- Usar `implementation-agent` solo cuando exista una especificación o change aprobado para implementar.
- Usar `review-agent` para revisar cambios, riesgos, seguridad o cumplimiento.

No cargar varios perfiles a la vez salvo que la tarea requiera explícitamente coordinar responsabilidades.
