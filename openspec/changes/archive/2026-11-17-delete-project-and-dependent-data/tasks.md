## 1. Backend tras aprobación

- [x] 1.1 Añadir el caso de uso, adaptador SQL de borrado explícito y transacción de raíz. Verificar PD-01 con el orden D-02 y el borrado íntegro de un proyecto vacío, uno con target y uno con historial.
- [x] 1.2 Coordinar DELETE, admisión T1 y guardado de target con los bloqueos de D-03. Verificar PD-02 con dos transacciones y barreras en ambos órdenes de admisión/DELETE y guardado/DELETE. Cubrir dos DELETE simultáneos, PENDING antes de despacho, RUNNING y entrega tardía sin invocación de trabajo.
- [x] 1.3 Liberar las entradas efímeras del corpus por evaluationId tras commit. Verificar que rollback no dispara limpieza y que la liberación no altera el resultado de un commit durable (PD-02).
- [x] 1.4 Exponer el endpoint y la traducción de errores tras rollback. Verificar con el arnés HTTP de Project los status 204/400/404/409/500, cuerpo vacío del éxito, repetición, ausente, no-store y ausencia de datos internos (PD-03).

## 2. Aceptación PostgreSQL

- [x] 2.1 Reutilizar el arnés PostgreSQL dedicado de integración. Verificar FK y ausencia de cascadas con pg_catalog sobre V1–V8. Incluir proyecto vacío, proyecto solo con target y proyecto con Scan COMPLETED, TestExecutions, evaluaciones, Findings, score e informe. Incluir scan fallido con hijos parciales y terminal histórico sin score/report. Comprobar recuentos por tabla e inexistencia de huérfanos, conservación de test_cases y de un segundo proyecto con datos (PD-01).
- [x] 2.2 Inyectar un fallo tras un borrado intermedio y comprobar 500 y restauración exacta de filas, incluyendo snapshots. Construir por separado una referencia cruzada de B al target de A permitida por el esquema. Verificar 409 PROJECT_DELETE_CONFLICT y rollback íntegro de A y B, sin tocar B (PD-01/03).
- [x] 2.3 Usar barreras, conexiones independientes y ninguna espera basada en sleeps para comprobar lectura concurrente, evaluación/Finding en curso, cierre de score/report y DELETE posterior al cierre. Verificar que no se observan hijos parciales, que 409 mantiene los datos y que una lectura nueva tras 204 devuelve ausencia. Verificar regresión de creación/listado/consulta y persistencia de scans y targets de B (PD-01/02).

## 3. Listado y documentación

- [x] 3.1 Añadir llamada tipada DELETE y diálogo accesible al listado sin rediseño. Verificar texto y foco, cancelación sin petición, bloqueo de doble envío, 204 con retirada y feedback, 409 sin retirada, 404 reconciliado y fallo ambiguo resuelto con GET antes de cualquier nuevo intento. Cubrir la lectura en vuelo que podría reinsertar una fila (PD-04).
- [x] 3.2 Actualizar OpenAPI, modelo de datos y política de autorización únicamente en los puntos afectados. Documentar 204/404 de repetición, pausa por actividad, alcance del perímetro y borrado de evidencia en la base activa. Verificar que no se anuncian permisos por Project ni borrado de copias externas (PD-01..04).
- [x] 3.3 Registrar resultados reales de las pruebas requeridas cuando se solicite su ejecución en entorno autorizado. Validar cobertura de casos PostgreSQL y Angular y distinguir pruebas omitidas de aprobadas (PD-01..04).
