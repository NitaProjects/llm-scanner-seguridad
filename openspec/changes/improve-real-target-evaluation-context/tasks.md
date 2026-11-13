**10/10 verificadas. READY FOR FINAL REVIEW.** Las correcciones del span inerte
(P2) y de la proyección sin envío (P3) pasan offline y en PostgreSQL. La tarea
2.2 queda cerrada tras la reejecución del caso de presupuesto.
[Evidencia](evidence/result.md).

## 1. Contexto del escenario

- [x] 1.1 Tras aprobar el change, extender el lector del manifiesto local y su vinculación exacta con un lector v2 de entradas parciales y contexto tipado por prueba (RTEC-01). Verificar v1 intacto, v2 de una a cuatro entradas, denegación de exportación por defecto, rechazo de campos inválidos y ausencia de contexto para el target o la prueba sin vínculo.
- [x] 1.2 Fijar contexto, identidad/hash y policyVersion en la preparación transaccional de evaluación existente (RTEC-01/05). Verificar que un cambio de fichero no modifica un scan preparado y que la ausencia de tres entradas mantiene el plan de cuatro pruebas.

## 2. Decisión local y semántica

- [x] 2.1 Separar en v2 los bloqueos locales de la elegibilidad semántica. Reutilizar HYB-02 con proyección nula y claim sin reserva (RTEC-02/03). Verificar omisión deliberada con policy/code/SKIPPED/0 intentos/errores nulos y degraded=true, además de la regresión v1.
- [x] 2.2 Aplicar el filtro de señales que pueden conducir a HYB-03/04 y las puertas actuales de permiso, corpus y límites. Proyectar únicamente los datos sintéticos pertinentes y conservar validación local de citas (RTEC-03/04). Verificar LLM08 con dos anclas y permiso, denegación por corpus ajeno, expectativa segura corroborada, ausencia de apoyo y aislamiento de datos no confiables.
- [x] 2.3 Evaluar independientemente infracción y expectativa segura en v2 (RTEC-06, P2.1). Verificar con la misma respuesta exacta que ambas señales y referencias llegan a la decisión, que invertir el orden conserva HYB-01/SIGNAL_CONFLICT/INCONCLUSIVE sin Finding y que v1 conserva su comportamiento.
- [x] 2.4 Limitar los positivos v2 de LLM10 a infracciones estructurales del contrato OUTPUT_ONLY (RTEC-07, P2.2). Verificar eco plano y span en JSON permitido sin Finding, incluso añadiendo literales arbitrarios. Con el mismo span y una clave no permitida, comprobar la infracción independiente y HYB-02. Conservar regresión v1.
- [x] 2.5 Reutilizar textBytes para la respuesta saneada completa y separar intervalo de prueba de excerpt de hasta 240 puntos de código (RTEC-08, P2.3). Verificar UTF-8 en y sobre la cota, literal que cruza el borde, literal incompleto o redactado, coincidencia completa larga para LLM02/08 y dos anclas como apoyo. Comprobar que el intervalo completo permite repetir el cotejo con TestExecution y que la proyección excluye su metadata interna.

## 3. Aceptación y compatibilidad

- [x] 3.1 Añadir la fixture con dos scans hybrid-v2 y el mismo texto saneado de LLM02. A vincula un escenario con contexto solo para otra categoría y da CONTEXT_UNAVAILABLE en LLM02. B añade el canario de LLM02 y da HYB-02/Finding si no hay conflicto, con 0 llamadas/reservas. Reutilizar el arnés PostgreSQL para verificar 4/4 y cierre con scoring-v1/report-v1 desde REST y chat, más control con otro valor (RTEC-02/05/08). Groq queda como evidencia complementaria.
- [x] 3.2 Cubrir la regresión del camino v1, de las cuatro categorías y del informe histórico con los arneses existentes (RTEC-01..08). Verificar que no cambian prompts, API, snapshots previos ni versión de score/report. Ejecutar las suites solo cuando se solicite su comprobación y registrar omisiones como pendientes.
- [x] 3.3 Actualizar la estrategia de evaluación existente con la forma de declarar y revisar el escenario v2, procedencia del canario, límites y alcance por categoría (RTEC-01/03/04). Verificar que el ejemplo permite la aceptación local y explica cómo se conserva la evidencia para revisión humana.
