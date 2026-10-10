import { Injectable } from '@angular/core';
import { catchError, map, throwError } from 'rxjs';
import { ApiClient } from '../../../core/api/api-client';
import { contractError, mapApiError } from '../../../core/api/api-error';
import { array, Decoder, sameId } from '../../../core/api/decode';
import { decodeScan, decodeExecution, decodeEvaluations, decodeFinding, decodeScoreResponse } from '../models/scan.decoder';
import { decodeReportResponse } from '../models/report.decoder';
@Injectable({ providedIn: 'root' })
export class ScansApi extends ApiClient {
  private path(projectId: string, scanId?: string) { return `/projects/${encodeURIComponent(projectId)}/scans${scanId ? '/' + encodeURIComponent(scanId) : ''}`; }
  private scoped<T>(decode: Decoder<T>, belongs: (value: T) => boolean): Decoder<T> { return value => { const result = decode(value); if (!belongs(result)) throw contractError(); return result; }; }
  start(projectId: string) {
    // Omitir body genera una petición sin contenido; no se serializa null ni {}.
    return this.http.request<unknown>('POST', this.base + this.path(projectId), { observe: 'response' }).pipe(
      map(response => { if (response.status !== 202) throw contractError(); const scan = decodeScan(response.body); if (!sameId(scan.projectId, projectId)) throw contractError(); return scan; }),
      catchError(error => throwError(() => mapApiError(error)))
    );
  }
  list(projectId: string) { return this.get(this.path(projectId), this.scoped(array(decodeScan), items => items.every(item => sameId(item.projectId, projectId)))); }
  getScan(projectId: string, scanId: string) { return this.get(this.path(projectId, scanId), this.scoped(decodeScan, item => sameId(item.projectId, projectId) && sameId(item.id, scanId))); }
  executions(projectId: string, scanId: string) { return this.get(this.path(projectId, scanId) + '/executions', this.scoped(array(decodeExecution, 0, 4), items => items.every(item => sameId(item.scanId, scanId)))); }
  evaluations(projectId: string, scanId: string) { return this.get(this.path(projectId, scanId) + '/evaluations', this.scoped(decodeEvaluations, listing => listing.items.every(item => sameId(item.scanId, scanId)))); }
  findings(projectId: string, scanId: string) { return this.get(this.path(projectId, scanId) + '/findings', this.scoped(array(decodeFinding, 0, 4), items => items.every(item => sameId(item.scanId, scanId)))); }
  score(projectId: string, scanId: string) { return this.get(this.path(projectId, scanId) + '/score', this.scoped(decodeScoreResponse, item => item.availability !== 'AVAILABLE' || sameId(item.score.scanId, scanId))); }
  report(projectId: string, scanId: string) { return this.get(this.path(projectId, scanId) + '/report', this.scoped(decodeReportResponse, item => item.availability !== 'AVAILABLE' || (sameId(item.report.scanId, scanId) && sameId(item.report.projectId, projectId) && sameId(item.report.summary.scanId, scanId) && item.report.findings.every(finding => sameId(finding.scanId, scanId))))); }
  attemptId(projectId: string, location: string | null): string | null {
    if (!location) return null;
    const prefix = this.base + '/projects/';
    if (!location.startsWith(prefix)) return null;
    const parts = location.slice(prefix.length).split('/');
    if (parts.length !== 3 || !sameId(parts[0], projectId) || parts[1] !== 'scans') return null;
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(parts[2]) ? parts[2] : null;
  }
}
