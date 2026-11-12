export const authTypes = ['NONE', 'BEARER_TOKEN', 'API_KEY_HEADER'] as const;
export type AuthType = typeof authTypes[number];
export type TargetAdapterType = 'REST_LEGACY' | 'OPENAI_COMPATIBLE';
interface TargetFields {
  readonly adapterType?: 'REST_LEGACY';
  readonly endpoint?: never; readonly model?: never; readonly systemPrompt?: never;
  readonly name: string | null;
  readonly url: string;
  readonly requestTemplate: string;
  readonly responsePath: string;
  readonly headers: Readonly<Record<string, string>>;
  readonly timeoutMs: number;
}
export type LegacyTargetConfigurationDto = TargetFields & {
  readonly id: string;
  readonly projectId: string;
  readonly method: 'POST';
  readonly contentType: 'application/json';
  readonly promptPlaceholder: '{{PROMPT}}';
  readonly hasCredentials: boolean;
  readonly createdAt: string;
  readonly updatedAt: string;
} & (
  | { readonly authType: 'NONE' | 'BEARER_TOKEN'; readonly authConfig: null }
  | { readonly authType: 'API_KEY_HEADER'; readonly authConfig: { readonly headerName: string } }
);
type Constants = { readonly method?: 'POST'; readonly contentType?: 'application/json'; readonly promptPlaceholder?: '{{PROMPT}}' };
type TargetInputFields = Omit<TargetFields, 'name' | 'headers' | 'timeoutMs'> & {
  readonly name?: string | null;
  readonly headers?: Readonly<Record<string, string>>;
  readonly timeoutMs?: number;
};
export type LegacyCreateTargetRequest = TargetInputFields & Constants & (
  | { readonly authType: 'NONE'; readonly authConfig: null }
  | { readonly authType: 'BEARER_TOKEN'; readonly authConfig: { readonly secretValue: string } }
  | { readonly authType: 'API_KEY_HEADER'; readonly authConfig: { readonly headerName: string; readonly secretValue: string } }
);
export type LegacyUpdateTargetRequest = TargetInputFields & Constants & (
  | { readonly authType: 'NONE'; readonly authConfig: null }
  | { readonly authType: 'BEARER_TOKEN'; readonly authConfig: { readonly secretValue?: string } }
  | { readonly authType: 'API_KEY_HEADER'; readonly authConfig: { readonly headerName: string; readonly secretValue?: string } }
);

export interface ChatTargetConfigurationDto {
  readonly adapterType: 'OPENAI_COMPATIBLE';
  readonly id: string; readonly projectId: string; readonly name: string | null;
  readonly endpoint: string; readonly model: string; readonly systemPrompt: string | null;
  readonly authType: 'BEARER_TOKEN'; readonly authConfig: null; readonly hasCredentials: boolean;
  readonly timeoutMs: number; readonly createdAt: string; readonly updatedAt: string;
  readonly url?: never; readonly requestTemplate?: never; readonly responsePath?: never; readonly headers?: never;
}
export interface ChatTargetRequest {
  readonly adapterType: 'OPENAI_COMPATIBLE'; readonly endpoint: string; readonly model: string;
  readonly name?: string | null; readonly systemPrompt?: string | null; readonly timeoutMs?: number;
  readonly authType: 'BEARER_TOKEN'; readonly authConfig: { readonly secretValue?: string };
}
export type TargetConfigurationDto = LegacyTargetConfigurationDto | ChatTargetConfigurationDto;
export type CreateTargetRequest = LegacyCreateTargetRequest | (ChatTargetRequest & { readonly authConfig: { readonly secretValue: string } });
export type UpdateTargetRequest = LegacyUpdateTargetRequest | ChatTargetRequest;
export function targetEndpoint(value: TargetConfigurationDto): string {
  return value.adapterType === 'OPENAI_COMPATIBLE' ? value.endpoint : value.url;
}
