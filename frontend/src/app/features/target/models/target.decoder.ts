import { record, uuid, text, date, bool, number, stringMap, oneOf, nullable, object } from '../../../core/api/decode';
import { contractError } from '../../../core/api/api-error';
import { TargetConfigurationDto, authTypes } from './target';
export function decodeTarget(value: unknown): TargetConfigurationDto {
  const source = record(value);
  const fields = {
    id: uuid(source['id']), projectId: uuid(source['projectId']), name: nullable(text)(source['name']), url: text(source['url']),
    method: oneOf(['POST'])(source['method']), contentType: oneOf(['application/json'])(source['contentType']),
    requestTemplate: text(source['requestTemplate']), promptPlaceholder: oneOf(['{{PROMPT}}'])(source['promptPlaceholder']),
    responsePath: text(source['responsePath']), headers: stringMap(source['headers']), timeoutMs: number(1000, 60000)(source['timeoutMs']),
    hasCredentials: bool(source['hasCredentials']), createdAt: date(source['createdAt']), updatedAt: date(source['updatedAt'])
  };
  const authType = oneOf(authTypes)(source['authType']);
  if (authType === 'API_KEY_HEADER') return { ...fields, authType, authConfig: object<{ readonly headerName: string }>({ headerName: text })(source['authConfig']) };
  if (source['authConfig'] !== null) throw contractError();
  return { ...fields, authType, authConfig: null };
}
