# Contenido definitivo de slides — LLM Scanner Seguridad

Preparación de contenido de Fase E, 2026-10-23. **READY FOR SLIDE CREATION**: 10 slides; no hay todavía PPTX/PDF ni presentación publicada. Este documento no completa las tareas de creación/apertura/entrega de la presentación. Duración oral estimada: **5 min 20 s**, aproximadamente **6 minutos con transiciones**; es una estimación de ensayo, no un requisito oficial.

Dirección visual para la creación posterior: formato 16:9, fondo claro, texto oscuro y un color de acento; una idea y un visual principal por slide, sin capturas de credenciales ni consola AWS. Diagramas simples editables. Distinguir visualmente «validación local» de «producción AWS». Las fuentes se mantienen en notas/material de apoyo; la slide 7 debe conservar visible el límite «casos sintéticos; no benchmark».

## 1. LLM Scanner Seguridad

**Mensaje principal:** una aplicación defensiva para analizar aplicaciones con LLM de forma controlada.

**Contenido visible:**

- **LLM Scanner Seguridad**
- TFM Máster de Desarrollo con IA
- Evaluación de seguridad controlada y trazable

**Visual recomendado:** composición limpia con el nombre y una línea de tres elementos: aplicación objetivo → análisis → evidencia. Sin logos de proveedores ni autor no confirmado.

**Notas del presentador — 20 s:**

> Presento LLM Scanner Seguridad, el proyecto desarrollado para el TFM del Máster de Desarrollo con IA. Es una aplicación web defensiva para analizar aplicaciones que incorporan modelos de lenguaje. Su objetivo es ejecutar pruebas autorizadas y convertir las respuestas en resultados que puedan revisarse y explicarse.

## 2. El riesgo aparece en la aplicación que integra el LLM

**Mensaje principal:** necesitamos comprobar el comportamiento de la aplicación ante entradas adversarias.

**Contenido visible:**

- Instrucciones manipuladas
- Exposición de información o contexto
- Respuestas que incumplen un contrato de salida
- **Objetivo: automatizar pruebas autorizadas y registrar evidencia**

**Visual recomendado:** usuario → aplicación con LLM → respuesta, con tres señales sobre entrada, contexto y salida. La palabra «autorizadas» junto al objetivo.

**Notas del presentador — 30 s:**

> Integrar un LLM introduce riesgos que afectan al comportamiento de la aplicación: puede seguir instrucciones manipuladas, revelar información o devolver una salida que incumpla el contrato esperado. Una respuesta distinta puede expresar el mismo problema, por lo que buscar palabras concretas no siempre basta. El objetivo es automatizar una evaluación inicial controlada y dejar evidencia revisable, dentro de un alcance autorizado.

## 3. Del endpoint autorizado al informe

**Mensaje principal:** la solución conecta configuración, ejecución y resultados persistidos en un flujo único.

**Contenido visible:**

`Project → Target → Scan → Findings → Score → Report`

- Cuatro pruebas fijas por Scan
- Peticiones, respuestas y evaluaciones trazables
- Resultados consultables desde la web

**Visual recomendado:** seis pasos horizontales con iconos sencillos; bajo Scan, un ramal pequeño «4 pruebas → evaluación». No añadir pantallas o pasos que no existan.

**Notas del presentador — 30 s:**

> El usuario crea un proyecto y configura un endpoint que está autorizado a analizar. Al iniciar un Scan, el sistema ejecuta el plan fijo de cuatro pruebas y evalúa sus respuestas. Los hallazgos sustentados alimentan el score y el informe estructurado. El frontend permite seguir el estado y consultar los resultados persistidos. No todos los análisis tienen que producir hallazgos: la incertidumbre también es un resultado válido.

## 4. Un núcleo independiente de las integraciones

**Mensaje principal:** la arquitectura separa reglas de negocio, interfaz e integraciones externas.

**Contenido visible:**

`Angular → API REST Spring Boot → Aplicación / Dominio → Puertos`

`Adaptadores de salida → PostgreSQL | Target HTTP | Evaluador LLM`

- Java 21 · Angular 21 · PostgreSQL 16
- Arquitectura Clean / Hexagonal

**Visual recomendado:** diagrama de capas con un bloque explícito «Puertos» entre Aplicación/Dominio y los tres adaptadores de salida independientes. Target y evaluador deben ser dos cajas distintas. Etiquetar como arquitectura lógica, no topología AWS.

**Notas del presentador — 35 s:**

> La interfaz está desarrollada en Angular y consume una API REST de Spring Boot. El núcleo separa los casos de uso y las reglas de dominio de los detalles de persistencia y comunicación. PostgreSQL, el endpoint objetivo y el modelo evaluador se conectan mediante puertos y adaptadores. Esta separación evita confundir el sistema que recibe la prueba con el modelo que interpreta su respuesta y permite probar las reglas sin depender del proveedor externo.

## 5. Cuatro categorías, un alcance explícito

**Mensaje principal:** el MVP prueba cuatro escenarios concretos; no ofrece cobertura completa de seguridad LLM.

**Contenido visible:**

| Categoría del proyecto | Prueba orientada a |
| --- | --- |
| LLM01 — Prompt Injection | Alteración de instrucciones |
| LLM02 — Sensitive Information Disclosure | Exposición de información sintética reservada |
| LLM08 — Hidden Context Exposure | Revelación de contexto oculto |
| LLM10 — Improper Output Handling | Incumplimiento observable en la salida |

Pie visible: **LLM10: OUTPUT_ONLY; sin explotación downstream demostrada.**

**Visual recomendado:** cuatro tarjetas con código y concepto; la tabla es el contenido de referencia, puede convertirse en tarjetas sin añadir texto. Sin indicadores de cobertura porcentual.

**Notas del presentador — 30 s:**

> El MVP tiene exactamente cuatro pruebas, una por categoría. Buscan alteración de instrucciones, exposición de información sintética, revelación de contexto e incumplimientos observables en la salida. Son escenarios concretos, no una auditoría exhaustiva de cada categoría. En particular, LLM10 analiza únicamente lo que devuelve el target: no demuestra que otro sistema ejecute ese contenido ni que exista una explotación posterior.

## 6. Evaluación híbrida y degradación controlada

**Mensaje principal:** el veredicto final combina evidencia verificable e interpretación LLM, con incertidumbre explícita.

**Contenido visible:**

`Respuesta → Reglas deterministas + Evaluador LLM → Consolidación híbrida`

- VULNERABLE · NOT_VULNERABLE · INCONCLUSIVE
- Fallo del evaluador ≠ fallo del Scan
- Señal fuerte: posible VULNERABLE degradado; sin soporte suficiente: INCONCLUSIVE

**Visual recomendado:** dos ramas que confluyen en la política híbrida. Rama LLM con un indicador de fallo que desemboca en consolidación degradada, sin flecha a otro proveedor.

**Notas del presentador — 40 s:**

> Las reglas deterministas aportan señales verificables y el evaluador LLM añade interpretación cuando la evidencia sintética está autorizada para exportación. La política híbrida toma la decisión final. Si falla el LLM, no se cambia automáticamente de proveedor ni se inventa su voto: se conserva el fallo. Una señal determinista fuerte puede justificar un resultado vulnerable degradado; sin soporte suficiente, el resultado es inconcluso. Por eso un Scan completado no significa necesariamente que el proveedor haya respondido correctamente.

## 7. Validación local: cuatro casos vulnerables detectados

**Mensaje principal:** la cadena funcional y el proveedor real se comprobaron en un entorno local controlado.

**Contenido visible:**

- Demo local sintética · E2E API/UI
- OpenAI real: **4/4 respuestas LLM aceptadas**
- Híbrido: **4/4 casos VULNERABLE detectados**, sin degradación
- **4 Findings → Score → Report persistidos**

Pie visible: **Perfil VULNERABLE · validación local 19/10/2026 · 4 casos controlados · no benchmark**

**Visual recomendado:** cuatro tarjetas LLM01/02/08/10 marcadas como detectadas y una banda inferior Findings → Score → Report. Pequeña etiqueta «LOCAL». Evitar un gráfico «100 % de precisión».

**Notas del presentador — 40 s:**

> La validación utiliza una demo determinista con datos sintéticos. Además del recorrido E2E con exportación desactivada, se realizó una validación real local con OpenAI y gpt-6-luna. Se aceptaron cuatro respuestas LLM y la consolidación híbrida detectó los cuatro casos del perfil vulnerable, sin degradación, conservando hallazgos, score e informe. Son cuatro casos controlados: no permiten afirmar precisión general, ausencia de falsos positivos ni cobertura completa. Esta revalidación del proveedor no repitió el recorrido frontend.

## 8. El scanner también necesita controles de seguridad

**Mensaje principal:** analizar endpoints exige limitar destinos, datos exportados y recursos utilizados.

**Contenido visible:**

- SSRF: política de destinos y bloqueo de red privada por defecto
- Secretos: AES-256-GCM y configuración externa
- Recursos: límites, timeouts y concurrencia acotada
- Evaluator: exportación opt-in y gates de escenario/corpus

**Visual recomendado:** cuatro barreras alrededor del núcleo del scanner: destino, secretos, recursos y exportación. Sin ejemplos de claves, cabeceras o payloads explotables.

**Notas del presentador — 30 s:**

> Un scanner que realiza peticiones HTTP debe protegerse también a sí mismo. El proyecto aplica políticas SSRF, cifra las credenciales del target y limita tiempo, tamaño y concurrencia. El envío al evaluador requiere permisos conjuntos y evidencia sintética autorizada; guardar un target no autoriza por sí solo el tráfico. La demo utiliza una excepción local explícita que no se traslada a producción. Estos controles reducen el riesgo dentro del alcance definido, sin prometer seguridad absoluta.

## 9. AWS publica la aplicación; la validación LLM es local

**Mensaje principal:** producción permite acceso académico protegido a UI/API y persistencia, sin reproducir el laboratorio vulnerable.

**Contenido visible:**

`Internet · HTTPS + Basic Auth → EC2: Nginx / Angular → Spring Boot → PostgreSQL 16 / EBS`

- Solo 443 público · backend y PostgreSQL privados
- Demo: **no desplegada** · Evaluator OpenAI: **desactivado**
- [https://llmscannerseguridad.com](https://llmscannerseguridad.com)

Pie de acceso: **Las credenciales se facilitarán por canal privado.**

**Visual recomendado:** una sola caja EC2 con tres servicios y disco EBS; candado en 443. Un recuadro separado «Laboratorio local: demo + evaluator» sin conexiones que sugieran ejecución desde AWS. Sin IP directa, IDs ni consola AWS.

**Notas del presentador — 35 s:**

> La instalación académica está desplegada en una EC2 de AWS. Nginx sirve Angular y protege UI y API con HTTPS y Basic Auth; Spring Boot y PostgreSQL no publican sus puertos. El despliegue acredita acceso a la aplicación y persistencia básica. La demo vulnerable no está desplegada y el evaluador permanece desactivado: los resultados de OpenAI que he mostrado proceden del entorno local. Las credenciales se facilitarán por canal privado. No se ha ejecutado un Scan en AWS.

## 10. Resultado, aprendizaje y límites

**Mensaje principal:** el resultado es un MVP trazable; ampliar su validez exige nuevos escenarios y evaluación experimental.

**Contenido visible:**

- **Resultado:** flujo completo y evidencia persistida
- **Aprendizaje:** separar prueba, evaluación y despliegue
- **Límites:** cuatro escenarios sintéticos; score heurístico ≠ probabilidad de vulnerabilidad
- **Evolución:** ampliar dataset, calibrar y comparar evaluadores

**Visual recomendado:** tres zonas sencillas «Construido → Aprendido → Siguiente paso»; colocar los límites en una banda visible, sin porcentajes ni promesas comerciales.

**Notas del presentador — 30 s:**

> El resultado es un MVP que conecta pruebas controladas con evaluaciones, hallazgos e informes trazables. El aprendizaje principal es separar qué se observa, cómo se interpreta y qué puede afirmarse a partir de la evidencia. El score es heurístico y cuatro escenarios no constituyen un benchmark. La evolución razonable es ampliar el dataset y evaluar calibración y generalización antes de ampliar las conclusiones. Un score cero tampoco acredita que una aplicación sea segura.

## Fuentes y control de afirmaciones

- Slides 1–6 y 8: [README principal](../../../Readme.md), [arquitectura](../../02-arquitectura/arquitectura-general.md), [estrategia de evaluación](../../04-ia/estrategia-evaluacion.md) y [política de autorización](../../03-seguridad/politica-de-autorizacion.md).
- Slide 7: [E2E offline 2026-10-16](../../../openspec/changes/e2e-integration/evidence/2026-10-16-005150/result.md), [recorrido UI 2026-10-19](../../../openspec/changes/real-evaluator-validation/evidence/2026-10-19-ui-real/result.md) y [revalidación OpenAI local](../../../openspec/changes/real-evaluator-validation/evidence/2026-10-19-tls-revalidation/result.md). El recorrido UI con Findings tuvo degradación LLM; la revalidación posterior sin degradación no repitió frontend 9.4. No se presentan como un mismo Scan.
- Slide 9: [despliegue AWS](../../../openspec/changes/aws-production-readiness/evidence/2026-10-20-aws-deployment.md) y [Fase D de entrega](../../../openspec/changes/tfm-final-delivery/evidence/result.md). No se confunde la confirmación del operador con observación directa de UI autenticada. No se atribuye el PASS OpenAI local a AWS.
- Slide 10: limitaciones documentadas en las fuentes anteriores. No se afirma benchmark, probabilidad calibrada, cobertura completa, restauración ejecutada, entrega efectiva de credenciales ni causa histórica del fallo LLM demostrada. Restore 6.3 permanece pospuesto y no se convierte en requisito de las slides.

No se usan secretos, capturas de login, emails, IDs de cuenta/instancia, IP pública directa, URI ECR identificativas ni material TLS privado. Este control de contenido no sustituye la revisión de seguridad/metadatos y apertura del artefacto que se cree después. No se crean PPTX/PDF, capturas, servicios, Scans ni llamadas externas para preparar estas notas.
