import { object, uuid, text, nullable, date } from '../../../core/api/decode';
import { ProjectDto } from './project';
export const decodeProject = object<ProjectDto>({ id: uuid, name: text, description: nullable(text), createdAt: date, updatedAt: date });
