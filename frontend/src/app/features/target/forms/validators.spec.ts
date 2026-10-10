import { FormControl } from '@angular/forms';
import { AuthType } from '../models/target';
import { authHeaderError, bytes, canConserve, headersError, secretError, targetRequest, templateValidator, textValidator, timeoutValidator, urlValidator } from './validators';
import { target } from '../../../../testing/fixtures';
describe('Target and project validation', () => {
  const validate = (validator: ReturnType<typeof textValidator>, value: unknown) => validator(new FormControl(value));
  it('counts trimmed Unicode code points without changing inner spaces', () => {
    expect(validate(textValidator(120, true), '  ' + '😀'.repeat(120) + '  ')).toBeNull(); expect(validate(textValidator(120, true), '😀'.repeat(121))).toBeTruthy();
    expect(validate(textValidator(120, true), '  ')).toBeTruthy(); expect(validate(textValidator(120, true), 'a\u0000b')).toBeTruthy();
    expect(validate(textValidator(2000), '😀'.repeat(2000))).toBeNull(); expect(validate(textValidator(2000), '😀'.repeat(2001))).toBeTruthy();
  });
  it.each(['[]', '{}', '{"x":"prefix {{PROMPT}}"}', '{"{{PROMPT}}":"value"}', '{"x":"{{PROMPT}}","y":"{{PROMPT}}"}', '{bad}', '{"x":"{{PROMPT}}","y":"\\u0000"}'])('rejects template %s', value => expect(validate(templateValidator, value)).toBeTruthy());
  it('counts decoded placeholders and UTF-8 bytes at the exact boundary', () => {
    expect(validate(templateValidator, '{"x":"\\u007b{PROMPT}}"}')).toBeNull();
    const prefix = '{"x":"{{PROMPT}}","pad":"'; const suffix = '"}'; const value = prefix + 'a'.repeat(32768 - bytes(prefix + suffix)) + suffix;
    expect(validate(templateValidator, value)).toBeNull(); expect(validate(templateValidator, value.replace('"}', 'a"}'))).toBeTruthy();
  });
  it.each(['javascript:alert(1)', 'http:example.test', 'https://@example.test', 'https://user:pass@example.test', 'https://example.test/#', 'https://example.test/a b', 'https://example.test/\n'])('rejects URL %s where required', value => {
    // Exterior whitespace is contractually trimmed; interior controls are rejected.
    if (value.endsWith('\n')) expect(validate(urlValidator, value)).toBeNull(); else expect(validate(urlValidator, value)).toBeTruthy();
  });
  it('keeps advanced JSONPath and network authorization in backend', () => { expect(validate(textValidator(1024, true), '$..unsupported')).toBeNull(); expect(validate(urlValidator, 'https://synthetic.example.test/chat')).toBeNull(); });
  it.each([999, 60001, 1000.5, null, '10000'])('rejects timeout %s', value => expect(validate(timeoutValidator, value)).toBeTruthy());
  it.each([1000, 10000, 60000])('accepts timeout %s', value => expect(validate(timeoutValidator, value)).toBeNull());
  it('checks headers before object mapping: duplicates, reserved names, bytes and collisions', () => {
    expect(headersError([{ name: 'Accept', value: 'a' }, { name: 'accept', value: 'b' }], 'NONE', '')).toBeTruthy();
    for (const name of ['Authorization', 'Cookie', 'X-API-Key', 'Content-Type', 'X-Forwarded-Host', 'bad name']) expect(headersError([{ name, value: '' }], 'NONE', '')).toBeTruthy();
    expect(headersError([{ name: 'Accept', value: 'a'.repeat(4096) }], 'NONE', '')).toBeNull(); expect(headersError([{ name: 'Accept', value: 'é'.repeat(2049) }], 'NONE', '')).toBeTruthy();
    expect(headersError([{ name: 'Accept', value: '\n' }], 'NONE', '')).toBeTruthy(); expect(headersError([{ name: 'Custom-Auth', value: 'x' }], 'API_KEY_HEADER', 'custom-auth')).toBeTruthy();
    expect(headersError(Array.from({ length: 21 }, (_, index) => ({ name: 'Header-' + index, value: '' })), 'NONE', '')).toBeTruthy();
    expect(headersError(Array.from({ length: 4 }, (_, index) => ({ name: 'Header-' + index, value: 'a'.repeat(4096) })), 'NONE', '')).toBeTruthy();
    expect(authHeaderError('X-API-Key')).toBeNull(); expect(authHeaderError('Cookie')).toBeTruthy();
  });
  it('never trims secrets; rejects controls, bearer spaces, isolated surrogates and oversized values', () => {
    expect(secretError(' synthetic ', 'API_KEY_HEADER')).toBeNull(); expect(secretError(' synthetic ', 'BEARER_TOKEN')).toBeTruthy();
    for (const value of ['', ' ', 'a\nb', '\ud800', 'x'.repeat(8193)]) expect(secretError(value, 'API_KEY_HEADER')).toBeTruthy();
    expect(secretError('😀'.repeat(2048), 'API_KEY_HEADER')).toBeNull();
  });
  it.each(['NONE', 'BEARER_TOKEN', 'API_KEY_HEADER'] as const)('checks all transitions from %s against original public metadata', originalType => {
    for (const destination of ['NONE', 'BEARER_TOKEN', 'API_KEY_HEADER'] as AuthType[]) expect(canConserve(target(originalType), destination, target().url, 'X-API-Key')).toBe(originalType !== 'NONE' && destination === originalType);
  });
  it('requires exact auth header and normalized same origin; path-only changes are allowed', () => {
    expect(canConserve(target('BEARER_TOKEN'), 'BEARER_TOKEN', 'https://SYNTHETIC.example.test:443/other', '')).toBe(true);
    for (const url of ['http://synthetic.example.test/chat', 'https://synthetic.example.test:444/chat', 'https://other.example.test/chat']) expect(canConserve(target('BEARER_TOKEN'), 'BEARER_TOKEN', url, '')).toBe(false);
    expect(canConserve(target('API_KEY_HEADER'), 'API_KEY_HEADER', target().url, 'x-api-key')).toBe(false); expect(canConserve(target('BEARER_TOKEN', { hasCredentials: false }), 'BEARER_TOKEN', target().url, '')).toBe(false);
  });
  it('serializes explicit optional fields and conservation without secretValue', () => {
    const form = { name: '', url: target().url, requestTemplate: target().requestTemplate, responsePath: '$.text', timeoutMs: 15000, headers: [], authType: 'BEARER_TOKEN' as const, headerName: '', secretValue: 'synthetic', credentialMode: 'conserve' as const };
    expect(targetRequest(form, true)).toEqual({ name: null, url: form.url, requestTemplate: form.requestTemplate, responsePath: '$.text', timeoutMs: 15000, headers: {}, authType: 'BEARER_TOKEN', authConfig: {} });
    expect(targetRequest({ ...form, authType: 'API_KEY_HEADER', headerName: 'X-API-Key', secretValue: ' synthetic ' }, false).authConfig).toEqual({ headerName: 'X-API-Key', secretValue: ' synthetic ' });
    expect(targetRequest({ ...form, authType: 'NONE' }, false).authConfig).toBeNull();
  });
});
