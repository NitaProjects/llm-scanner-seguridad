import { HttpErrorResponse } from '@angular/common/http';
import { TimeoutError } from 'rxjs';

export const fieldNames = ['adapterType', 'endpoint', 'model', 'systemPrompt', 'name', 'description', 'url', 'method', 'contentType', 'promptPlaceholder', 'requestTemplate', 'responsePath', 'headers', 'authType', 'authConfig', 'authConfig.headerName', 'authConfig.secretValue', 'timeoutMs'] as const;
export type FieldName = typeof fieldNames[number];
export const fieldCodes = ['REQUIRED', 'INVALID_TYPE', 'TOO_LONG', 'INVALID_FORMAT', 'INVALID_CHARACTER', 'OUT_OF_RANGE', 'NOT_ALLOWED', 'CONFLICT'] as const;
export interface FieldErrorDto { readonly field: FieldName; readonly code: typeof fieldCodes[number]; readonly message: string }
const messages = {
  INVALID_REQUEST: 'La petición no cumple el contrato.', VALIDATION_ERROR: 'Revisa los campos indicados.',
  INVALID_PROJECT_ID: 'El identificador del proyecto no es válido.', INVALID_SCAN_ID: 'El identificador del análisis no es válido.',
  PROJECT_NOT_FOUND: 'El proyecto no existe.', SCAN_NOT_FOUND: 'El análisis no existe en este proyecto.',
  TARGET_NOT_CONFIGURED: 'El proyecto no tiene un target configurado.', TARGET_ALREADY_CONFIGURED: 'El target ya está configurado. Carga la configuración existente.',
  TARGET_TYPE_REQUIRED: 'Consulta el target y selecciona su tipo antes de guardar.', TARGET_TYPE_CHANGED: 'El tipo cambi�. Recarga la configuraci�n antes de guardar.',
  PROJECT_DELETE_CONFLICT: 'No se pudo eliminar el proyecto porque existen referencias a sus datos.',
  SCAN_ALREADY_ACTIVE: 'Ya existe un análisis activo. Consulta el histórico.', SCAN_DISPATCH_FAILED: 'No se pudo iniciar el procesamiento. Puedes consultar el intento registrado.',
  SCAN_EXECUTION_UNAVAILABLE: 'El servicio de ejecución no está disponible temporalmente.', INTERNAL_ERROR: 'No se ha podido completar la operación.'
} as const;
export type ApiErrorCode = keyof typeof messages;
export interface ApiErrorDto { readonly code: ApiErrorCode; readonly message: string; readonly fieldErrors: readonly FieldErrorDto[] }
const fieldMessages: Record<FieldErrorDto['code'], string> = {
  REQUIRED: 'Este campo es obligatorio.', INVALID_TYPE: 'El tipo de dato no es válido.', TOO_LONG: 'Se ha superado el límite del campo.',
  INVALID_FORMAT: 'El formato no es válido.', INVALID_CHARACTER: 'Contiene caracteres no admitidos.', OUT_OF_RANGE: 'El valor está fuera del rango permitido.',
  NOT_ALLOWED: 'El valor no está permitido.', CONFLICT: 'Este campo entra en conflicto con otro valor.'
};
export class ApiClientError extends Error {
  constructor(readonly kind: 'backend' | 'network' | 'timeout' | 'contract' | 'unexpected', message: string,
    readonly status = 0, readonly code: ApiErrorCode | null = null, readonly fieldErrors: readonly FieldErrorDto[] = [], readonly location: string | null = null) { super(message); }
  get transient(): boolean { return this.kind === 'network' || this.kind === 'timeout' || this.status >= 500; }
  get ambiguousWrite(): boolean { return this.transient || this.kind === 'contract' || this.kind === 'unexpected'; }
}
export function contractError(): ApiClientError { return new ApiClientError('contract', 'La respuesta de la API no es compatible. Revisa la conexión y la versión del servicio.'); }
function record(value: unknown): value is Record<string, unknown> { return value !== null && typeof value === 'object' && !Array.isArray(value); }
export function mapApiError(error: unknown): ApiClientError {
  if (error instanceof ApiClientError) return error;
  if (error instanceof TimeoutError) return new ApiClientError('timeout', 'La consulta ha superado el tiempo de espera.');
  if (!(error instanceof HttpErrorResponse)) return new ApiClientError('unexpected', 'No se ha podido completar la operación.');
  if (error.status === 0) return new ApiClientError('network', 'No se pudo confirmar la respuesta. Comprueba la conexión.');
  const body: unknown = error.error;
  if (record(body) && typeof body['code'] === 'string' && Object.hasOwn(messages, body['code']) && typeof body['message'] === 'string' && Array.isArray(body['fieldErrors'])) {
    const code = body['code'] as ApiErrorCode;
    const fields: FieldErrorDto[] = [];
    for (const field of body['fieldErrors']) {
      if (!record(field) || !fieldNames.includes(field['field'] as FieldName) || !fieldCodes.includes(field['code'] as FieldErrorDto['code']) || typeof field['message'] !== 'string') continue;
      const fieldCode = field['code'] as FieldErrorDto['code'];
      fields.push({ field: field['field'] as FieldName, code: fieldCode, message: fieldMessages[fieldCode] });
    }
    // Mensajes locales cerrados: nunca reflejar un body libre ni una entrada rechazada.
    return new ApiClientError('backend', messages[code], error.status, code, fields, error.headers.get('Location'));
  }
  return new ApiClientError('unexpected', 'No se ha podido completar la operación. Inténtalo de nuevo mediante la acción indicada.', error.status);
}
