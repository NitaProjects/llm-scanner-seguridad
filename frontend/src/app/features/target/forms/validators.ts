import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { AuthType, TargetAdapterType, TargetConfigurationDto, UpdateTargetRequest, targetEndpoint } from '../models/target';
const controls = /[\u0000-\u001f\u007f]/;
const token = /^[!#$%&'*+.^_`|~0-9A-Za-z-]+$/;
const reserved = ['authorization', 'proxy-authorization', 'cookie', 'set-cookie', 'host', 'content-length', 'content-type', 'connection', 'transfer-encoding', 'te', 'trailer', 'upgrade', 'keep-alive', 'proxy-connection', 'forwarded'];
export const bytes = (value: string) => new TextEncoder().encode(value).length;
export const points = (value: string) => [...value].length;
export function textValidator(max: number, required = false, trim = true): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const raw = control.value as string;
    const value = trim ? raw.trim() : raw;
    if (raw.includes('\u0000')) return { invalid: 'No se admite el carácter U+0000.' };
    if (required && !value) return { invalid: 'Este campo es obligatorio.' };
    return points(value) > max ? { invalid: `Admite hasta ${max} caracteres Unicode.` } : null;
  };
}
export const urlValidator: ValidatorFn = control => {
  const value = (control.value as string).trim();
  if (points(value) > 2048 || /[\s\u0000-\u001f\u007f]/.test(value) || !/^https?:\/\//i.test(value)) return { invalid: 'Introduce una URL absoluta HTTP/HTTPS de hasta 2048 caracteres, sin espacios ni controles.' };
  try {
    const url = new URL(value);
    const authority = value.match(/^https?:\/\/([^/?#]*)/i)?.[1] ?? '';
    if (!['http:', 'https:'].includes(url.protocol) || !url.hostname || authority.includes('@') || url.username || url.password || value.includes('#')) throw new Error();
    return null;
  } catch { return { invalid: 'Introduce una URL absoluta HTTP/HTTPS sin credenciales ni fragmento.' }; }
};
export const templateValidator: ValidatorFn = control => {
  const value = control.value as string;
  const invalid = { invalid: 'Usa un objeto JSON con un único {{PROMPT}} como valor completo de texto.' };
  if (bytes(value) > 32768 || value.includes('\u0000')) return { invalid: 'La plantilla admite hasta 32768 bytes UTF-8, sin U+0000.' };
  try {
    const parsed: unknown = JSON.parse(value);
    if (parsed === null || typeof parsed !== 'object' || Array.isArray(parsed)) return invalid;
    let count = 0; let misplaced = false;
    const visit = (item: unknown): void => {
      if (typeof item === 'string') {
        if (item.includes('\u0000')) misplaced = true;
        if (item.includes('{{PROMPT}}')) { if (item !== '{{PROMPT}}') misplaced = true; count++; }
      } else if (Array.isArray(item)) item.forEach(visit);
      else if (item !== null && typeof item === 'object') Object.entries(item).forEach(([key, child]) => { if (key.includes('{{PROMPT}}') || key.includes('\u0000')) misplaced = true; visit(child); });
    };
    visit(parsed);
    return count === 1 && !misplaced ? null : invalid;
  } catch { return invalid; }
};
export const timeoutValidator: ValidatorFn = control => Number.isInteger(control.value) && control.value >= 1000 && control.value <= 60000 ? null : { invalid: 'Introduce un entero entre 1000 y 60000 ms.' };
export interface HeaderPair { readonly name: string; readonly value: string }
export function headersError(pairs: readonly HeaderPair[], authType: AuthType, headerName: string): string | null {
  if (pairs.length > 20) return 'Se admiten hasta 20 cabeceras ordinarias.';
  const seen = new Set<string>(); let total = 0;
  for (const pair of pairs) {
    const name = pair.name; const lower = name.toLowerCase();
    if (!token.test(name) || name.length > 128 || reserved.includes(lower) || ['x-api-key', 'api-key'].includes(lower) || lower.startsWith('x-forwarded-')) return 'Hay una cabecera ordinaria no permitida. Usa Autenticación para credenciales.';
    if (seen.has(lower)) return 'No se permiten cabeceras duplicadas, aunque cambie el uso de mayúsculas.';
    if (authType === 'API_KEY_HEADER' && lower === headerName.toLowerCase()) return 'Una cabecera ordinaria coincide con la cabecera de autenticación.';
    if (controls.test(pair.value) || bytes(pair.value) > 4096) return 'Los valores admiten hasta 4096 bytes UTF-8 y no admiten controles.';
    total += bytes(name) + bytes(pair.value); seen.add(lower);
  }
  return total > 16384 ? 'Las cabeceras superan 16384 bytes UTF-8 en total.' : null;
}
export function authHeaderError(name: string): string | null {
  const lower = name.toLowerCase();
  return !token.test(name) || name.length > 128 || reserved.includes(lower) || lower.startsWith('x-forwarded-') ? 'Introduce un nombre de cabecera de autenticación válido y no reservado.' : null;
}
export function secretError(value: string, authType: AuthType): string | null {
  if (!value.trim() || controls.test(value) || bytes(value) > 8192 || (authType === 'BEARER_TOKEN' && /\s/.test(value))) return 'Introduce una credencial válida de hasta 8192 bytes, sin controles; Bearer no admite espacios.';
  for (const character of value) { const code = character.codePointAt(0)!; if (code >= 0xd800 && code <= 0xdfff) return 'La credencial contiene Unicode no válido.'; }
  return null;
}
export function canConserve(original: TargetConfigurationDto | null, authType: AuthType, url: string, headerName: string): boolean {
  if (!original || !original.hasCredentials || authType === 'NONE' || original.authType !== authType) return false;
  try {
    return new URL(targetEndpoint(original)).origin === new URL(url.trim()).origin && (authType !== 'API_KEY_HEADER' || (original.authType === 'API_KEY_HEADER' && original.authConfig.headerName === headerName));
  } catch { return false; }
}
export interface TargetFormValue {
  readonly adapterType?: TargetAdapterType; readonly model?: string; readonly systemPrompt?: string;
  readonly name: string; readonly url: string; readonly requestTemplate: string; readonly responsePath: string; readonly timeoutMs: number;
  readonly headers: readonly HeaderPair[]; readonly authType: AuthType; readonly headerName: string; readonly secretValue: string; readonly credentialMode: 'conserve' | 'replace';
}
export function targetRequest(value: TargetFormValue, conserve: boolean): UpdateTargetRequest {
  if (value.adapterType === 'OPENAI_COMPATIBLE') return {
    adapterType: 'OPENAI_COMPATIBLE', name: value.name.trim() || null, endpoint: value.url.trim(),
    model: (value.model ?? '').trim(), systemPrompt: value.systemPrompt || null, timeoutMs: value.timeoutMs,
    authType: 'BEARER_TOKEN', authConfig: conserve ? {} : { secretValue: value.secretValue }
  };
  const fields = { name: value.name.trim() || null, url: value.url.trim(), requestTemplate: value.requestTemplate, responsePath: value.responsePath.trim(), timeoutMs: value.timeoutMs, headers: Object.fromEntries(value.headers.map(pair => [pair.name, pair.value])) };
  if (value.authType === 'NONE') return { ...fields, authType: 'NONE', authConfig: null };
  const secret = conserve ? {} : { secretValue: value.secretValue };
  if (value.authType === 'BEARER_TOKEN') return { ...fields, authType: 'BEARER_TOKEN', authConfig: secret };
  return { ...fields, authType: 'API_KEY_HEADER', authConfig: { headerName: value.headerName, ...secret } };
}
