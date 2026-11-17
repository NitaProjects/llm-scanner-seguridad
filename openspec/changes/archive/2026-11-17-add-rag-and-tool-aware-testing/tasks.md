## 1. Extracción y correlación

- [x] 1.1 Mapear selectores de JSON completo y SSE/JSONL al acumulador común. Verificar paridad de consulta, items, call/result y texto final entre dos fixtures (OBS-01).
- [x] 1.2 Aplicar correlación, cotas y manejo de evidencia incompleta. Verificar IDs duplicados, huérfanos, resultado faltante y exceso por registro o agregado. Cubrir JSON malformado/parcial, workflow mixto declarado y discrepancia de formato (OBS-01/03).
- [x] 1.3 Aplicar comprobaciones locales de aliases y solicitudes con fixtures controladas. Verificar cero callbacks, descargas o exchanges disparados por eventos adversarios (OBS-02).

## 2. Contexto de evaluación y privacidad

- [x] 2.1 Transportar la evidencia efímera opcional hasta la preparación del resultado confirmado. Verificar las cinco fuentes tipadas, ausencia de concatenación, vínculo a ejecución y descarte en error, cancelación o fallo de persistencia (OBS-04/06).
- [x] 2.2 Aplicar clasificación, redacción, minimización y permiso explícito por fuente en la proyección semántica existente. Verificar autorizaciones exactas, copia de una fuente bloqueada en otra, límites agregados, local-only, decisión local suficiente y requisito semántico bloqueado con INCONCLUSIVE y cero llamadas/reservas (OBS-05).
- [x] 2.3 Verificar API, lectores históricos y report-v1, y ambas políticas sin contexto adicional. Comprobar que la llamada con fuentes adicionales conserva estados y contadores con proyección/hash nulos y que la histórica conserva su proyección/hash. Cubrir fixtures REST_LEGACY, OPENAI_COMPATIBLE, streaming y workflows con sesiones y credenciales derivadas (OBS-04..06).
- [x] 2.4 Documentar la configuración admitida, las cotas, los stubs y los motivos de traza inconclusa en la documentación técnica afectada (OBS-01..06).

Véase [evidencia](evidence/result.md).
