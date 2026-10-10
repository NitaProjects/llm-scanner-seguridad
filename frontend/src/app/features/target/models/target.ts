export const authTypes = ['NONE', 'BEARER_TOKEN', 'API_KEY_HEADER'] as const;
export type AuthType = typeof authTypes[number];
interface TargetFields {
  readonly name: string | null;
  readonly url: string;
  readonly requestTemplate: string;
  readonly responsePath: string;
  readonly headers: Readonly<Record<string, string>>;
  readonly timeoutMs: number;
}
export type TargetConfigurationDto = TargetFields & {
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
export type CreateTargetRequest = TargetInputFields & Constants & (
  | { readonly authType: 'NONE'; readonly authConfig: null }
  | { readonly authType: 'BEARER_TOKEN'; readonly authConfig: { readonly secretValue: string } }
  | { readonly authType: 'API_KEY_HEADER'; readonly authConfig: { readonly headerName: string; readonly secretValue: string } }
);
export type UpdateTargetRequest = TargetInputFields & Constants & (
  | { readonly authType: 'NONE'; readonly authConfig: null }
  | { readonly authType: 'BEARER_TOKEN'; readonly authConfig: { readonly secretValue?: string } }
  | { readonly authType: 'API_KEY_HEADER'; readonly authConfig: { readonly headerName: string; readonly secretValue?: string } }
);
