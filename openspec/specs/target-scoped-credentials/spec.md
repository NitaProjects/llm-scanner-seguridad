# target-scoped-credentials Specification

## Purpose
Aplicar credenciales efímeras declaradas por paso y confinadas al origen autorizado de captura.

## Requirements

### Requirement: CRED-01 Bindings de mismo origen

Una credencial capturada SHALL materializarse solo mediante un binding explícito de la misma prueba al paso consumidor y mismo scheme/host/puerto. Las cookies SHALL ser host-only, declaradas, vigentes, de path compatible y respetar Secure y eliminación. Los headers dinámicos SHALL estar permitidos por nombre y validar valores. SHALL NOT admitir Authorization derivado, Cookie libre, nombres dinámicos, framing ni overrides de auth.

#### Scenario: Consumo y aislamiento
- **WHEN** un workflow recibe cookie y CSRF y después intenta usarlos en un paso permitido y otro con origen o path incompatible
- **THEN** solo se aplican al paso permitido y la llamada incompatible queda bloqueada antes de enviar

### Requirement: CRED-02 Redacción y ciclo de vida

SHALL resolver el handle tras autorizar y validar destino, DNS/IP fijada y TLS. SHALL mantener cero redirects y no propagar credenciales de forma automática. SHALL aplicar cotas, timeout y redacción de valores conocidos, incluido texto ensamblado. SHALL excluir valores de persistencia, logs, evidencia, URLs y evaluación y descartar el contexto al finalizar.

#### Scenario: Redirect o cookie expirada
- **WHEN** el consumidor requerido tiene una cookie vencida o un exchange devuelve Location
- **THEN** no se reenvía la credencial ni se sigue el redirect, y el diagnóstico excluye valores sensibles
