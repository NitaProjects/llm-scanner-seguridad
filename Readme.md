# LLM Scanner Seguridad

Plataforma defensiva para evaluar aplicaciones que integran modelos de lenguaje. Ejecuta pruebas controladas contra endpoints autorizados y convierte las respuestas en resultados trazables: evaluaciones, hallazgos con recomendaciones, una puntuación de riesgo y un informe estructurado.

Pensado para equipos de desarrollo, QA y seguridad que necesitan comprobar cómo responde una aplicación a entradas adversarias y revisar la evidencia antes de priorizar una corrección.

## Qué permite hacer

1. Crear un proyecto y configurar la API objetivo, su autenticación y el formato de respuesta.
2. Lanzar un análisis asíncrono y seguir su progreso desde Angular. Cada análisis ejecuta cuatro pruebas fijas.
3. Revisar las evaluaciones, los hallazgos y sus recomendaciones, el score (`scoring-v1`) y el informe persistido (`report-v1`).

| Prueba | Qué comprueba |
| --- | --- |
| LLM01 · Prompt Injection | Si la aplicación sigue instrucciones adversarias. |
| LLM02 · Sensitive Information Disclosure | Si devuelve información sintética que debería proteger. |
| LLM08 · Hidden Context Exposure | Si expone contexto interno en la respuesta. |
| LLM10 · Improper Output Handling | Si incumple un contrato observable de salida. No prueba explotación en sistemas que consuman esa salida. |

### Integración con aplicaciones objetivo

Admite [REST POST JSON y Chat Completions compatible con OpenAI](docs/05-api/target-contract.md). Los bindings operativos locales permiten procesar SSE/JSONL y configurar workflows HTTP de varios pasos con variables de sesión, cookies, CSRF y cabeceras dinámicas. El scanner puede observar eventos RAG y de llamadas a herramientas declarados por el target, sin ejecutar esas herramientas.

### Cómo evalúa las respuestas

Combina reglas deterministas, expectativas del escenario y, cuando la exportación de evidencia sintética está autorizada, un modelo evaluador independiente. La evaluación puede concluir `VULNERABLE`, `NOT_VULNERABLE` o `INCONCLUSIVE`. Si el evaluador falla, el resultado conserva esa degradación; las reglas todavía pueden fundamentar un hallazgo. Un score de cero indica que no se detectaron hallazgos en esa ejecución. El score es heurístico y no estima la probabilidad de vulnerabilidad.

## Arquitectura

```text
Angular → API REST Spring Boot → Aplicación y dominio
                                         ↓ puertos y adaptadores
                              PostgreSQL · API objetivo · Evaluador LLM
```

Monolito modular con arquitectura hexagonal. Stack: Angular 21 y TypeScript, Java 21 y Spring Boot 4.1, PostgreSQL 16 con Flyway, Maven Wrapper y Docker Compose. La [guía de despliegue](docker/production/README.md) describe el perfil `pro`, con Nginx como entrada HTTPS y API y base de datos en red privada.

El scanner valida destinos para mitigar SSRF, cifra las credenciales del target con AES-256-GCM y limita tiempos, tamaños y concurrencia. Guardar una configuración no autoriza ni inicia tráfico. **Analiza únicamente sistemas propios o explícitamente autorizados.**

## Inicio rápido local

Requisitos: JDK 21, Node.js 22, npm 10.8.2 y Docker Compose. Los ejemplos usan PowerShell desde la raíz del repositorio. En Linux/macOS usa `sh ./mvnw` en lugar de `mvnw.cmd`.

1. Arranca PostgreSQL local y compila el backend:

   ```powershell
   docker compose -f docker/docker-compose.yml up -d
   Set-Location backend
   .\mvnw.cmd -DskipTests package
   Set-Location ..
   ```

2. En la terminal del backend, configura `DB_USERNAME` y `DB_PASSWORD` con los valores de tu entorno local. Si el target requiere autenticación, proporciona `TARGET_CREDENTIAL_ENCRYPTION_KEY` como clave Base64 de 32 bytes. Spring Boot no carga `.env` automáticamente; suministra secretos por un mecanismo seguro fuera del repositorio.

   ```powershell
   $env:SPRING_PROFILES_ACTIVE = 'local'
   $env:DB_URL = 'jdbc:postgresql://localhost:5432/llm_scanner_seguridad'
   $env:EVALUATOR_EXPORT_ENABLED = 'false'
   java -jar backend/target/llm-scanner-seguridad-0.0.1-SNAPSHOT.jar
   ```

3. En otra terminal, inicia la interfaz y abre [localhost:4200](http://localhost:4200). Angular redirige `/api` al backend local en el puerto 8080.

   ```powershell
   Set-Location frontend
   npm ci
   npm start
   ```

4. Para recorrer un caso controlado sin proveedor externo, sigue la [guía de la demo local](demo-vulnerable/README.md). Incluye perfiles `SAFE` y `VULNERABLE`, configuración del target y manifiestos de evaluación. La excepción SSRF para la demo se limita al origen local exacto autorizado.

## Validación y despliegue

- **Demo local:** [recorrido de extremo a extremo](openspec/changes/e2e-integration/evidence/2026-10-16-005150/result.md) con API e interfaz, sin exportación al evaluador.
- **Evaluador OpenAI:** [revalidación local](openspec/changes/real-evaluator-validation/evidence/2026-10-19-tls-revalidation/result.md) con cuatro respuestas reales aceptadas en un escenario sintético. Es evidencia de integración, no un benchmark.
- **Groq como target:** [validación de Chat Completions](openspec/changes/add-openai-compatible-adapter/evidence/result.md) con un análisis de cuatro pruebas. Las cuatro evaluaciones fueron inconclusas; la conectividad y el procesamiento quedaron acreditados, no la seguridad del target.
- **AWS:** [despliegue registrado](openspec/changes/aws-production-readiness/evidence/2026-10-20-aws-deployment.md) con HTTPS, acceso restringido, interfaz/API y persistencia básica. La evidencia corresponde a la revisión desplegada entonces y no acredita que las extensiones posteriores estén publicadas. La demo y la evaluación local con proveedores no forman parte de esa validación en AWS.

## Documentación

| Ruta | Contenido |
| --- | --- |
| [`frontend/`](frontend/README.md) · `backend/` | Interfaz Angular y API Spring Boot. |
| [`demo-vulnerable/`](demo-vulnerable/README.md) | Target sintético para pruebas controladas. |
| [`docker/production/`](docker/production/README.md) | Guía de despliegue y operación; separada del Compose de desarrollo. |
| [`docs/`](docs/README.md) | Arquitectura, decisiones, contratos, seguridad y metodología de evaluación. |
| `openspec/` | Especificaciones, cambios y evidencias de validación. |
