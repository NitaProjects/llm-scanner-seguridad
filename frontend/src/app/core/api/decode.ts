import { contractError } from './api-error';
export type Decoder<T> = (value: unknown) => T;
export const text: Decoder<string> = value => { if (typeof value !== 'string') throw contractError(); return value; };
export const bool: Decoder<boolean> = value => { if (typeof value !== 'boolean') throw contractError(); return value; };
export const uuid: Decoder<string> = value => { const result = text(value); if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(result)) throw contractError(); return result; };
export const sameId = (left: string, right: string) => left.toLowerCase() === right.toLowerCase();
export const date: Decoder<string> = value => { const result = text(value); if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(?:\.\d{1,6})?Z$/.test(result) || !Number.isFinite(Date.parse(result))) throw contractError(); return result; };
export function number(min: number, max: number, integer = true): Decoder<number> {
  return value => { if (typeof value !== 'number' || !Number.isFinite(value) || value < min || value > max || (integer && !Number.isInteger(value))) throw contractError(); return value; };
}
export function oneOf<const T extends readonly (string | number)[]>(values: T): Decoder<T[number]> {
  return value => { if (!values.some(item => item === value)) throw contractError(); return value as T[number]; };
}
export function nullable<T>(decoder: Decoder<T>): Decoder<T | null> { return value => value === null ? null : decoder(value); }
export function array<T>(decoder: Decoder<T>, min = 0, max = Infinity): Decoder<readonly T[]> {
  return value => { if (!Array.isArray(value) || value.length < min || value.length > max) throw contractError(); return value.map(item => decoder(item)); };
}
export function record(value: unknown): Record<string, unknown> {
  if (value === null || typeof value !== 'object' || Array.isArray(value)) throw contractError();
  return value as Record<string, unknown>;
}
export function object<T>(shape: { [K in keyof T]-?: Decoder<T[K]> }): Decoder<T> {
  return value => {
    const source = record(value);
    // Solo se proyectan claves declaradas, cada una validada por su decoder.
    return Object.fromEntries(Object.entries(shape).map(([key, decoder]) => [key, (decoder as Decoder<unknown>)(source[key])])) as T;
  };
}
export const stringMap: Decoder<Readonly<Record<string, string>>> = value => Object.fromEntries(Object.entries(record(value)).map(([key, item]) => [key, text(item)]));
