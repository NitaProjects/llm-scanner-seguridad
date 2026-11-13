## 1. Implementación tras aprobación del change

- [ ] 1.1 Validar bindings de mismo origen, paso, header y cookie. Verificar expiración, eliminación, path, Secure, Domain y protección contra override de Authorization (CRED-01).
- [ ] 1.2 Materializar handles en el intercambio seguro tras validación del destino. Verificar fixture CSRF/cookie, cambio de origen, rebinding, redirects y valores malformados (CRED-01/02).

## 2. Aceptación

- [ ] 2.1 Verificar redacción de valores reflejados en texto, descarte al cerrar y ausencia de fugas en evidencia/logs. Documentar el contrato operativo afectado (CRED-02).
