# Validación de delete-project-and-dependent-data

## Backend y PostgreSQL

- La prueba HTTP verificó 204 sin cuerpo y los códigos 400, 404, 409 y 500.
  Las respuestas comprobadas incluyen `Cache-Control: no-store`. Los errores
  conservan el formato de Project con `fieldErrors` vacío.
- El adaptador borra por conjuntos en una transacción READ COMMITTED, tras
  adquirir el lock de Project y comprobar actividad. Orden de borrado:
  `security_reports`, `scan_scores`, `findings`, `evaluation_results`,
  `test_executions`, `scans`, `target_configurations` y `projects`.
- `ProjectDeletionPostgresIntegrationTest` verificó proyectos vacíos,
  proyectos solo con target y el grafo completo, incluidos scans fallidos
  con hijos parciales e históricos sin score ni informe. Comprobó las FK
  en `pg_catalog`, los recuentos por proyecto y por Scan, la conservación
  del catálogo y la igualdad de las filas del proyecto B.
- Se comprobó rollback exacto tras un error intermedio (500) y tras una FK
  cruzada de otro proyecto (409). La limpieza del corpus se activó tras
  commit, acotada al proyecto eliminado. Una excepción de limpieza dejó
  confirmado el 204.
- Con conexiones independientes y barreras, la prueba verificó dos DELETE
  concurrentes, ambos órdenes frente a admisión y guardado del target,
  protección PENDING/RUNNING, lectura de la versión previa durante los
  cambios, cierre de score/informe y entrega tardía sin trabajo.
- El operador ejecutó `ProjectDeletionPostgresIntegrationTest` en PostgreSQL
  dedicado. Maven registró 6 aprobadas, 0 fallos, 0 errores y 0 omitidas,
  con `BUILD SUCCESS` (13,42 s).

## Frontend y regresiones

- Las pruebas Angular cubrieron nombre, advertencia, foco, cancelación sin
  petición, doble envío, 204 con retirada, 409 con conservación, 404
  reconciliado, error de red y cancelación de una lectura obsoleta.
- Angular `npm test -- --watch=false`: 171 aprobadas, 0 fallos, 0 omitidas.
- Backend offline `.\mvnw.cmd -o test`: 811 registradas, 711 aprobadas,
  0 fallos, 0 errores y 100 omitidas. Las seis pruebas PostgreSQL de borrado
  omitidas en esa suite se acreditaron en la corrida dedicada.
- OpenSpec strict PASS. Tareas completadas 10/10.
