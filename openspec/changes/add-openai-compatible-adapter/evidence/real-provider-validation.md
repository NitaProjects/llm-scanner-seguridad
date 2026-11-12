# Validación real — preflight bloqueado

2026-11-11, Europe/Madrid. Estado: **BLOCKED** para aceptación externa.

- Autorización del operador: un Scan de cuatro pruebas contra Groq mediante el flujo
  normal, sin evaluator externo ni retries. No se ha consumido esta autorización.
- Endpoint previsto documentado en OAC-02:
  `https://api.groq.com/openai/v1/chat/completions`.
- Modelo exacto: NOT_CONFIGURED; no encontrado en la configuración/documentación local
  revisada. No se consultó catálogo externo ni se asumió un identificador.
- GROQ_API_KEY: AUSENTE en el proceso ejecutor. Esto no determina su presencia en
  otras terminales del operador. No se leyó ni imprimió valor alguno.
- Cuota/coste: NOT_INSPECTED; faltan límites operativos acreditados para la cuenta.
- Backend `http://localhost:8080/api/projects` y frontend `http://localhost:4200`:
  no disponibles en las comprobaciones locales.
- Contrato implementado: stream=false; no se ha emitido request real para validarlo.
- 5.1 incompleta; 5.2 NOT_RUN; 5.3 pendiente de evidencia real.
- scanId, duración, HTTP proveedor, parsing, evaluaciones, Findings, score y report:
  NOT_RUN. Cero Scans y cero llamadas al proveedor desde esta actuación.

Reanudación: disponer de backend/frontend locales, modelo disponible confirmado,
clave provisionada únicamente en entorno local y cuota/coste acordados. La clave no
debe compartirse en el chat. Mantener las 16/19 tareas y la evidencia offline previa;
el bloqueo externo no invalida los tests de implementación.
