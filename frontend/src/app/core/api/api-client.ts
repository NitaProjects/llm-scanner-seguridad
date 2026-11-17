import { HttpClient } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, map, Observable, throwError, timeout } from 'rxjs';
import { API_BASE_URL } from './api-config';
import { contractError, mapApiError } from './api-error';
import { Decoder } from './decode';

export abstract class ApiClient {
  protected readonly http = inject(HttpClient);
  protected readonly base = inject(API_BASE_URL);
  protected get<T>(path: string, decode: Decoder<T>): Observable<T> {
    return this.http.get<unknown>(this.base + path, { timeout: 10000 }).pipe(timeout(10000), map(decode), catchError(error => throwError(() => mapApiError(error))));
  }
  protected write<T>(method: 'POST' | 'PUT' | 'DELETE', path: string, body: unknown, status: number, decode: Decoder<T>): Observable<T> {
    return this.http.request<unknown>(method, this.base + path, { body, observe: 'response' }).pipe(
      map(response => { if (response.status !== status) throw contractError(); return decode(response.body); }),
      catchError(error => throwError(() => mapApiError(error)))
    );
  }
}
