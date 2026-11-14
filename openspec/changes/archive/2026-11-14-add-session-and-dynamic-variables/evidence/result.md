# add-session-and-dynamic-variables — validación

2026-11-14. **4/4 verificadas. READY FOR REVIEW.** Aceptación persistida con PostgreSQL **PASS**.

| Ejecución Java 21 | Total | Pasados | Fallos | Errores | Omitidos |
| --- | ---: | ---: | ---: | ---: | ---: |
| Suite backend | 707 | 614 | 0 | 0 | 93 |
| SessionTargetTest dentro de la suite | 15 | 15 | 0 | 0 | 0 |
| SessionPostgresIntegrationTest dentro de la suite | 1 | 0 | 0 | 0 | 1 |
| SessionTargetTest tras añadir el caso de deadline compartido | 16 | 16 | 0 | 0 | 0 |
| SessionPostgresIntegrationTest manual con PostgreSQL | 1 | 1 | 0 | 0 | 0 |

Las filas desglosadas no se suman al total. Los tests omitidos no cuentan como
PASS. Las regresiones offline de REST_LEGACY, OPENAI_COMPATIBLE, SSE/JSONL,
hybrid-v1/v2, scoring-v1 y report-v1 pasan. Las fixtures usan loopback y datos
sintéticos.

- El binding fija dos llamadas como máximo al mismo origen, bootstrap GET/POST
  y POST final. El presupuesto de Scan y el deadline de target cubren ambos
  intercambios y la espera de cuota. Cada intercambio aplica la política de
  origen, DNS/IP fijada y TLS del transporte existente.
  La regresión dirigida confirma que el POST no obtiene un nuevo timeout
  cuando la espera de cuota agota el plazo de la interacción.
- Las fixtures capturan `chatId` y `sessionId` por selector JSON simple y anidado,
  además de un header y una cookie sensibles con sus atributos. Verifican la
  procedencia, la ausencia de reenvío y la redacción en la respuesta final.
- Los slots admiten únicamente un segmento o nodo completo. Se verifica
  codificación UTF-8 una vez, ausencia de sustitución recursiva y bloqueo de
  traversal, delimitadores, variables ausentes, nombres reservados, duplicados,
  tipos inválidos y valores mayores que la cota. Límite de 16 variables y hasta
  4 KiB UTF-8 por valor. Los handles sensibles quedan fuera de los slots.
- Cada prueba crea y cierra un almacén distinto. Se verifica la ausencia de
  reutilización tras éxito, fallo de captura e interrupción. Los errores del
  bootstrap descartan su petición y respuesta. El resultado exitoso conserva
  el HTTP status real y la evidencia saneada del POST final.

PostgreSQL es pertinente porque el worker debe confirmar cuatro resultados
de sesión y cerrar evaluación, score e informe con el esquema actual. La
fixture `SessionPostgresIntegrationTest` comprueba cuatro bootstraps y cuatro
POST finales, redacción antes de persistir y cierre con scoring-v1/report-v1.
El operador confirmó **BUILD SUCCESS** el 2026-11-14 a las 01:11:14 CET.
El test duró **11.38 s** y Maven **13.146 s**. Este resultado cierra la tarea 2.2.
Comando de la integración desde `backend`:

```powershell
.\mvnw.cmd -o '-Dtest=SessionPostgresIntegrationTest' test
```

OpenSpec strict **PASS**.

## Correcciones P2 y P3

**P2.** La sesión reúne la credencial estática y los valores sensibles
capturados, con sus variantes JSON. Busca todas las coincidencias sobre el
texto original y fusiona intervalos solapados o contiguos antes de escribir
`[REDACTED_CREDENTIAL]`. La redacción de una coincidencia no impide encontrar
otra. La regresión cubre ambos órdenes, repeticiones, solapamientos parciales y
texto sin secretos. Una fixture HTTP comprueba el cuerpo saneado, el texto
confirmado y el mensaje preparado por el evaluador sin llamadas externas.

**P3.** La clave de deduplicación incluye la fuente y, para JSON, la lista
inmutable de segmentos ya parseados. Las propiedades son strings y los índices
son enteros. Se rechazan `$.chatId` con `$["chatId"]`, la variante con escape
Unicode y las formas equivalentes que incluyen índices. `$ ."chatId"` sin
el espacio sigue fuera de la gramática, por lo que también se rechaza antes
de red. Las rutas distintas `$.chatId` y `$.sessionId` completan el bootstrap
y el POST. Header y cookie conservan su comparación anterior.

| Validación | Total | Pasados | Fallos | Errores | Omitidos |
| --- | ---: | ---: | ---: | ---: | ---: |
| SessionTargetTest | 19 | 19 | 0 | 0 | 0 |
| Dirigida de sesión, streaming, wiring y evaluación | 71 | 71 | 0 | 0 | 0 |
| Suite backend offline | 711 | 618 | 0 | 0 | 93 |

Las regresiones offline de REST_LEGACY, OPENAI_COMPATIBLE, SSE/JSONL,
hybrid-v1/v2, scoring-v1 y report-v1 pasan. La redacción ocurre antes de entregar
la interacción y la deduplicación antes del bootstrap. Se conserva el contrato
de persistencia y cierre, por lo que no se requiere reejecutar PostgreSQL.
OpenSpec strict **PASS**. **4/4 tareas.**
