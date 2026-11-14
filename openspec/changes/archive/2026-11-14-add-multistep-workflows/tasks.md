## 1. Implementación tras aprobación del change

- [x] 1.1 Validar definición JSON, dependencias, guardas y terminal. Verificar rechazo de referencias futuras, ciclos, falta de prompt y SSE/JSONL intermedio antes de red (WF-01).
- [x] 1.2 Componer exchanges seguros, variables y collector en el flujo secuencial. Verificar fixture chat JSON → chatId → sesión JSON → sessionId → /answer SSE/JSONL terminal y texto final reconstruido (WF-01).
- [x] 1.3 Validar rutas sin esquema ni autoridad y comprobar esquema, host y puerto efectivo de la URI resuelta antes de red y credenciales. Verificar diferencias de cada componente y conservar la reautorización SSRF por exchange (WF-03).

## 2. Aceptación

- [x] 2.1 Aplicar deadline acumulado, límites, aborto y diagnóstico por paso. Verificar omisión, fallo requerido, cancelación, flood y ausencia de reejecución (WF-02).
- [x] 2.2 Verificar cierre de un scan con fixture y lectura histórica. Actualizar el contrato de workflow afectado con rutas genéricas y su presupuesto (WF-01/02).
- [x] 2.3 Verificar base https://target-a.example y paso //target-b.example/path con ambos orígenes en allowlist. Comprobar workflow bloqueado, cero requests a target-b, cero materialización o envío de Bearer para ese paso y diagnóstico seguro (WF-03).

El operador confirmó la aceptación persistida de la tarea 2.2 en PostgreSQL
dedicado. Véase [evidencia](evidence/result.md).
