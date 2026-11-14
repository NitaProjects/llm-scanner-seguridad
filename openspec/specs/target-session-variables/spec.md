# target-session-variables Specification

## Purpose
Resolver un bootstrap y un envío de prompt con identificadores y capturas declaradas dentro de una sola prueba.

## Requirements

### Requirement: SES-01 Bootstrap fijo y captura tipada

La interacción vinculada SHALL ejecutar bootstrap JSON y POST de prompt JSON en un mismo origen bajo un deadline y presupuesto comunes. SHALL extraer escalares por selector cerrado y permitir capturas explícitas por nombre de header o Set-Cookie. SHALL clasificar capturas de headers/cookies como sensibles y limitar cardinalidad y tamaños. Una captura requerida inválida SHALL bloquear el POST dependiente.

#### Scenario: Identificador y cookie de bootstrap
- **WHEN** un bootstrap devuelve un chatId JSON declarado y una cookie nombrada
- **THEN** el POST usa el identificador en su slot permitido y la cookie queda como handle sensible efímero sin reenvío

### Requirement: SES-02 Alcance y uso seguro

Los identificadores JSON no sensibles SHALL reutilizarse únicamente en nodos JSON o segmentos validados y codificados. Una variable SHALL NOT cambiar origen, método ni autoridad. Los valores sensibles SHALL quedar fuera de templates, URLs, logs y evidencia. Cada llamada SHALL aplicar autorización, DNS/IP fijada, TLS, cero redirects y límites. El contexto SHALL descartarse al finalizar y SHALL NOT compartirse entre pruebas ni persistirse.

#### Scenario: Variable intenta cambiar destino
- **WHEN** un identificador contiene traversal o delimitadores de URL
- **THEN** la llamada dependiente no se envía y el resultado conserva solo un diagnóstico seguro
