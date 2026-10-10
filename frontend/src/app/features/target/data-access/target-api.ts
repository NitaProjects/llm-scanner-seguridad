import { Injectable } from '@angular/core';
import { ApiClient } from '../../../core/api/api-client';
import { CreateTargetRequest, UpdateTargetRequest } from '../models/target';
import { decodeTarget } from '../models/target.decoder';
@Injectable({ providedIn: 'root' })
export class TargetApi extends ApiClient {
  private path(projectId: string) { return `/projects/${encodeURIComponent(projectId)}/target`; }
  getTarget(projectId: string) { return this.get(this.path(projectId), decodeTarget); }
  create(projectId: string, input: CreateTargetRequest) { return this.write('POST', this.path(projectId), input, 201, decodeTarget); }
  update(projectId: string, input: UpdateTargetRequest) { return this.write('PUT', this.path(projectId), input, 200, decodeTarget); }
}
