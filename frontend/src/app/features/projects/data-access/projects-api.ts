import { Injectable } from '@angular/core';
import { ApiClient } from '../../../core/api/api-client';
import { array } from '../../../core/api/decode';
import { CreateProjectRequest } from '../models/project';
import { decodeProject } from '../models/project.decoder';
@Injectable({ providedIn: 'root' })
export class ProjectsApi extends ApiClient {
  list() { return this.get('/projects', array(decodeProject)); }
  getProject(id: string) { return this.get(`/projects/${encodeURIComponent(id)}`, decodeProject); }
  create(input: CreateProjectRequest) { return this.write('POST', '/projects', input, 201, decodeProject); }
  deleteProject(id: string) { return this.write('DELETE', `/projects/${encodeURIComponent(id)}`, undefined, 204, () => undefined); }
}
