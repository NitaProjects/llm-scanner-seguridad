import { TestBed } from '@angular/core/testing';
import { vi } from 'vitest';
import { TargetForm } from './target-form';
import { target } from '../../../../testing/fixtures';
import { UpdateTargetRequest } from '../models/target';
import { ApiClientError } from '../../../core/api/api-error';
describe('TargetForm secrets UX', () => {
  beforeEach(() => TestBed.configureTestingModule({ imports: [TargetForm] }));
  function create(type: 'NONE' | 'BEARER_TOKEN' | 'API_KEY_HEADER' = 'BEARER_TOKEN') {
    const fixture = TestBed.createComponent(TargetForm); fixture.componentRef.setInput('original', target(type)); fixture.detectChanges(); return fixture;
  }
  afterEach(() => vi.restoreAllMocks());
  function chat() {
    const legacy = target('BEARER_TOKEN');
    return { adapterType: 'OPENAI_COMPATIBLE' as const, id: legacy.id, projectId: legacy.projectId,
      name: null, endpoint: legacy.url, model: 'fixture-model', systemPrompt: null,
      authType: 'BEARER_TOKEN' as const, authConfig: null, hasCredentials: true,
      timeoutMs: 15000, createdAt: legacy.createdAt, updatedAt: legacy.updatedAt };
  }
  it('loads chat directly and sends only its discriminated fields without a secret on conservation', async () => {
    const fixture = TestBed.createComponent(TargetForm); fixture.componentRef.setInput('original', chat()); fixture.detectChanges();
    await fixture.whenStable(); fixture.detectChanges();
    const component = fixture.componentInstance; const writes: UpdateTargetRequest[] = []; component.save.subscribe(value => writes.push(value));
    expect(fixture.nativeElement.querySelector('#target-template')).toBeNull();
    expect(fixture.nativeElement.querySelector('#target-response-path')).toBeNull();
    expect(fixture.nativeElement.querySelector('#target-auth-type')).toBeNull();
    expect(component.form.controls.model.value).toBe('fixture-model'); component.submit();
    expect(writes).toEqual([{ adapterType: 'OPENAI_COMPATIBLE', name: null, endpoint: chat().endpoint,
      model: 'fixture-model', systemPrompt: null, timeoutMs: 15000, authType: 'BEARER_TOKEN', authConfig: {} }]);
  });
  it('confirms conversion, removes hidden fields and clears tokens on type and origin changes', () => {
    const fixture = create(); const component = fixture.componentInstance; const writes: UpdateTargetRequest[] = [];
    component.save.subscribe(value => writes.push(value)); component.form.controls.secretValue.setValue('synthetic');
    component.form.controls.adapterType.setValue('OPENAI_COMPATIBLE'); component.form.controls.model.setValue('m'); fixture.detectChanges();
    expect(component.form.controls.secretValue.value).toBe('');
    vi.spyOn(window, 'confirm').mockReturnValue(false); component.submit(); expect(writes).toHaveLength(0);
    vi.mocked(window.confirm).mockReturnValue(true); component.submit(); expect(writes[0].adapterType).toBe('OPENAI_COMPATIBLE');
    expect(writes[0]).not.toHaveProperty('requestTemplate'); expect(writes[0]).not.toHaveProperty('responsePath'); expect(writes[0]).not.toHaveProperty('headers');
    component.form.controls.secretValue.setValue('synthetic'); component.form.controls.url.setValue('https://other.example.test/chat');
    expect(component.form.controls.secretValue.value).toBe(''); expect(component.retainAvailable()).toBe(false);
  });
  it('chat validates model, system and required Bearer without submitting invalid settings', () => {
    const fixture = TestBed.createComponent(TargetForm); fixture.detectChanges(); const c = fixture.componentInstance;
    const write = vi.fn(); c.save.subscribe(write); c.form.controls.adapterType.setValue('OPENAI_COMPATIBLE');
    c.form.controls.url.setValue('https://chat.example.test'); c.submit(); expect(write).not.toHaveBeenCalled();
    c.form.controls.model.setValue('m'); c.form.controls.systemPrompt.setValue('   '); c.form.controls.secretValue.setValue('synthetic');
    c.submit(); expect(write).not.toHaveBeenCalled(); c.form.controls.systemPrompt.setValue('synthetic system'); c.submit();
    expect(write).toHaveBeenCalledOnce(); expect(write.mock.calls[0][0].authType).toBe('BEARER_TOKEN'); expect(c.form.controls.secretValue.value).toBe('');
  });
  it('loads only public metadata and retains the original timeout and empty password', () => {
    const fixture = create(); const component = fixture.componentInstance; expect(component.form.controls.timeoutMs.value).toBe(15000); expect(component.form.controls.secretValue.value).toBe(''); expect(component.retainAvailable()).toBe(true); expect(fixture.nativeElement.textContent).toContain('Credencial configurada');
  });
  it('sends conservation explicitly and clears the secret before output', () => {
    const fixture = create(); const component = fixture.componentInstance; let result: UpdateTargetRequest | undefined;
    component.save.subscribe(value => { result = value; expect(component.form.controls.secretValue.value).toBe(''); }); component.submit(); expect(result?.authConfig).toEqual({});
  });
  it('requires a new secret for a mechanism change, and clears it on change and send', () => {
    const fixture = create(); const component = fixture.componentInstance; const writes: UpdateTargetRequest[] = []; component.save.subscribe(value => writes.push(value));
    component.form.controls.secretValue.setValue('synthetic-old'); component.form.controls.authType.setValue('API_KEY_HEADER'); component.form.controls.headerName.setValue('X-API-Key'); expect(component.form.controls.secretValue.value).toBe(''); component.submit(); expect(writes).toHaveLength(0);
    component.form.controls.secretValue.setValue(' synthetic-new '); component.submit(); expect(writes[0].authConfig).toEqual({ headerName: 'X-API-Key', secretValue: ' synthetic-new ' }); expect(component.form.controls.secretValue.value).toBe('');
  });
  it('returning to persisted type before saving re-evaluates against original DTO', () => {
    const component = create().componentInstance; component.form.controls.authType.setValue('NONE'); expect(component.retainAvailable()).toBe(false); component.form.controls.authType.setValue('BEARER_TOKEN'); expect(component.retainAvailable()).toBe(true); expect(component.form.controls.credentialMode.value).toBe('conserve');
  });
  it('NONE only writes removal on submit, not on selection or cancellation', () => {
    const component = create().componentInstance; const write = vi.fn(); component.save.subscribe(write); component.form.controls.secretValue.setValue('synthetic'); component.form.controls.authType.setValue('NONE'); expect(write).not.toHaveBeenCalled(); expect(component.original()?.hasCredentials).toBe(true); component.submit(); expect(write.mock.calls[0][0].authConfig).toBeNull();
  });
  it('clear/error/cancel/destroy never persist a secret in storage or console', () => {
    const storage = vi.spyOn(Storage.prototype, 'setItem'); const log = vi.spyOn(console, 'log'); const debug = vi.spyOn(console, 'debug'); const errorLog = vi.spyOn(console, 'error');
    const fixture = create(); const component = fixture.componentInstance; component.form.controls.credentialMode.setValue('replace'); component.form.controls.secretValue.setValue('synthetic-only');
    fixture.componentRef.setInput('error', new ApiClientError('backend', 'Revisa los campos', 400, 'VALIDATION_ERROR', [{ field: 'authConfig.secretValue', code: 'INVALID_FORMAT', message: 'Formato no válido.' }])); fixture.detectChanges(); expect(component.form.controls.secretValue.value).toBe('');
    const write = vi.fn(); component.save.subscribe(write); component.submit(); expect(write).not.toHaveBeenCalled(); component.form.controls.secretValue.setValue('synthetic-again'); component.cancelForm(); expect(component.form.controls.secretValue.value).toBe(''); component.form.controls.secretValue.setValue('synthetic-last'); fixture.destroy(); expect(component.form.controls.secretValue.value).toBe('');
    expect(storage).not.toHaveBeenCalled(); expect(log).not.toHaveBeenCalled(); expect(debug).not.toHaveBeenCalled(); expect(errorLog).not.toHaveBeenCalled();
  });
  it('clears on cancelled and accepted navigation, and confirms only dirty changes', () => {
    const component = create().componentInstance; const confirm = vi.spyOn(window, 'confirm').mockReturnValue(false); component.form.markAsDirty(); component.form.controls.secretValue.setValue('synthetic'); expect(component.confirmLeave()).toBe(false); expect(component.form.controls.secretValue.value).toBe(''); confirm.mockReturnValue(true); expect(component.confirmLeave()).toBe(true);
  });
  it('changing original project metadata resets the local draft and secret', () => {
    const fixture = create(); fixture.componentInstance.form.controls.secretValue.setValue('synthetic'); fixture.componentRef.setInput('original', target('NONE')); fixture.detectChanges(); expect(fixture.componentInstance.form.controls.secretValue.value).toBe(''); expect(fixture.componentInstance.form.pristine).toBe(true);
  });
  it('blocks duplicate submission and displays backend field errors', () => {
    const fixture = create(); fixture.componentRef.setInput('busy', true); const write = vi.fn(); fixture.componentInstance.save.subscribe(write); fixture.componentInstance.submit(); expect(write).not.toHaveBeenCalled(); fixture.componentRef.setInput('error', new ApiClientError('backend', 'Validación', 400, 'VALIDATION_ERROR', [{ field: 'responsePath', code: 'INVALID_FORMAT', message: 'Ruta no válida.' }])); fixture.detectChanges(); expect(fixture.nativeElement.textContent).toContain('Ruta no válida.');
  });
});
