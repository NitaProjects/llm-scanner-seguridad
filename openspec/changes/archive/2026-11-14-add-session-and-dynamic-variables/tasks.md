**4/4 verificadas. READY FOR REVIEW.** La implementación y las regresiones
offline pasan. La ejecución manual de PostgreSQL confirma la aceptación
persistida de cuatro resultados (2.2). [Evidencia](evidence/result.md).

## 1. Implementación tras aprobación del change

- [x] 1.1 Validar las dos llamadas y los tipos/extracciones JSON, header y cookie con límites. Verificar duplicados, variables requeridas y clasificación sensible (SES-01).
- [x] 1.2 Conectar el bootstrap al POST final mediante slots codificados y un contexto por prueba. Verificar dos llamadas como máximo, mismo presupuesto, origen fijo y ausencia de reenvío de cookies (SES-01/02).

## 2. Aceptación

- [x] 2.1 Verificar fallos antes de red dependiente, bloqueo de traversal, redacción, expiración del scope tras error/cancelación y ausencia de fuga a una segunda prueba (SES-02).
- [x] 2.2 Verificar que los cuatro resultados usan el contrato actual y actualizar la descripción de sesión operativa afectada (SES-01/02).
