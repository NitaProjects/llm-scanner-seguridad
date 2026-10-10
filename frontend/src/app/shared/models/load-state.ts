import { ApiClientError } from '../../core/api/api-error';
export interface LoadState<T> { readonly status: 'loading' | 'loaded' | 'error'; readonly data: T | null; readonly error: ApiClientError | null }
export function loading<T>(previous: T | null = null): LoadState<T> { return { status: 'loading', data: previous, error: null }; }
export function loaded<T>(data: T): LoadState<T> { return { status: 'loaded', data, error: null }; }
export function failed<T>(error: ApiClientError, previous: T | null = null): LoadState<T> { return { status: 'error', data: previous, error }; }
