import { describe, expect, it } from 'vitest';
import { decodeTarget } from './target.decoder';
import { target } from '../../../../testing/fixtures';

describe('discriminated target decoder', () => {
  const legacy = target();
  const chat = { adapterType: 'OPENAI_COMPATIBLE', id: legacy.id, projectId: legacy.projectId,
    name: null, endpoint: 'https://chat.example.test/completions', model: 'm', systemPrompt: null,
    authType: 'BEARER_TOKEN', authConfig: null, hasCredentials: true, timeoutMs: 10000,
    createdAt: legacy.createdAt, updatedAt: legacy.updatedAt };
  it('keeps legacy shape and accepts chat columns directly', () => {
    expect(decodeTarget(legacy)).toEqual(legacy); expect(decodeTarget(chat)).toEqual(chat);
  });
  it('rejects unknown types, secret-bearing GETs and mixed branches', () => {
    expect(() => decodeTarget({ ...chat, adapterType: 'UNKNOWN' })).toThrow();
    expect(() => decodeTarget({ ...chat, authConfig: { secretValue: 'synthetic' } })).toThrow();
    expect(() => decodeTarget({ ...chat, requestTemplate: '{}' })).toThrow();
    expect(() => decodeTarget({ ...chat, authType: 'NONE' })).toThrow();
  });
});
