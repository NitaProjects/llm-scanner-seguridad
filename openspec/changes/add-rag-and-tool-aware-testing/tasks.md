## 1. Implementación tras aprobación del change

- [ ] 1.1 Decodificar eventos de texto/retrieval/tool/final y correlacionarlos bajo los límites del colector. Verificar duplicados, orden inválido y resultado faltante (OBS-01).
- [ ] 1.2 Aplicar comprobaciones locales de aliases y solicitudes con fixtures controladas. Verificar que eventos adversarios no producen callbacks, descargas ni exchanges (OBS-02).

## 2. Aceptación

- [ ] 2.1 Verificar separación de respuesta final, diagnósticos, redacción y exclusión de trazas del evaluador. Comparar Findings, score y report con la misma respuesta final sin trazas (OBS-01/02).
- [ ] 2.2 Documentar las comprobaciones observacionales, la preparación de stubs y cómo interpretar una traza incompleta (OBS-01/02).
