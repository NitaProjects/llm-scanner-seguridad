# Validación 6.3 / D-11 — pospuesta por decisión del operador

**Estado: READY FOR REVIEW documental. 6.3 PARTIAL; 24/25 tareas completas.**

## Decisión vigente

El operador decide no ejecutar la prueba de restore en esta fase final. Queda
explícitamente **PENDIENTE / NO EJECUTADA**, sin autorización para reanudarla ahora.
Informa que producción está estable, accesible y validada; el ejecutor local carece de
acceso administrativo y Session Manager no permite ejecutar cómodamente el procedimiento
guiado. Indica que 6.3 no pertenece a los requisitos oficiales obligatorios de entrega
del TFM y prioriza no arriesgar producción por esta validación operativa adicional.

No se elimina ni se cumple por excepción el criterio de restore del change: se mantiene
6.3 PARTIAL. No se acredita recuperabilidad. Esta actualización es solo documental y
OpenSpec strict; no consulta ni modifica producción, no ejecuta backup/restore, no detiene
servicios ni realiza Scans/OpenAI. No reanudar D-11 sin nueva autorización expresa.

## Antecedente de acceso — histórico

El operador autorizó exclusivamente el backup lógico y restore aislado de D-11 y
aportó el UUID del proyecto testigo. No se reproduce ese identificador en esta evidencia.
Se revisó el procedimiento, pero no se pudo iniciar su preflight en la EC2.

## Bloqueo comprobado

- El ejecutor disponible es local Windows/PowerShell; `aws` y `session-manager-plugin`
  no están disponibles en PATH. No se instalaron herramientas ni se buscaron credenciales.
- El inventario de Computer Use no muestra aplicaciones nativas accesibles ni pestañas
  en las superficies de navegador disponibles; no existe una sesión Session Manager
  accesible desde estas herramientas.
- La evidencia pública omite identificadores operativos de instancia. No se adivinó un
  destino ni se intentó SSH, abrir puertos o cambiar el mecanismo de administración.
- Se solicitó al operador una sesión administrativa accesible o la ejecución supervisada
  de los bloques aprobados con devolución únicamente de evidencia no secreta.

## Resultado de etapas

| Etapa | Resultado |
| --- | --- |
| Preflight D-11 en EC2 | NOT_RUN: falta sesión administrativa |
| Backup custom pg_dump | NOT_RUN |
| Tamaño/SHA256 del backup | NOT_AVAILABLE: no se creó backup |
| Restore transaccional en restore_check | NOT_RUN |
| Flyway V1–V7/checksums, esquema, constraints, índices, catálogo/proyecto | NOT_RUN |
| Disponibilidad antes/durante/después | NOT_VERIFIED: sin muestreo remoto |
| Identidad/estado de servicios productivos | NOT_VERIFIED |
| Cleanup de recursos de esta ejecución | NOT_APPLICABLE: no se creó ningún recurso temporal |
| OpenSpec strict | PASS, exit 0 |

No se ejecutaron comandos contra producción, ni se detuvieron/modificaron sus servicios.
Esto no sustituye la verificación de identidad/disponibilidad exigida por D-11 ni permite
declarar producción intacta PASS. No se crearon contenedor, volumen, dump o diagnósticos
privados; no hay datos del backup ni secretos conservados en el repositorio.

No se cambió código, configuración de despliegue, diseño ni la casilla de 6.3. Sin nuevos
despliegues, Scans, llamadas OpenAI, commits o pushes. La evidencia productiva previa se
conserva como histórica y no se presenta como verificada de nuevo.

## Condiciones para una futura reanudación — no autorizada ahora

Obtener nueva autorización expresa y disponer de una sesión Bash administrativa en la EC2 para ejecutar
D-11.1–D-11.7 en orden. Mantener el proyecto testigo aportado por el operador, todos los
límites/guardas y el monitor de disponibilidad. No ejecutar el restore contra Docker local
ni sustituirlo por una prueba sintética. Conservar solo la evidencia no secreta indicada
por D-11; cerrar 6.3 únicamente si todas sus etapas y cleanup quedan acreditados.
