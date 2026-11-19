import { Component, DestroyRef, ElementRef, computed, effect, inject, input, output, signal, viewChild } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiClientError, FieldName } from '../../../core/api/api-error';
import { AuthType, TargetAdapterType, TargetConfigurationDto, UpdateTargetRequest, targetEndpoint } from '../models/target';
import { authHeaderError, canConserve, headersError, secretError, targetRequest, templateValidator, textValidator, timeoutValidator, urlValidator } from '../forms/validators';
import { Icon } from '../../../shared/ui/icon';
@Component({ selector: 'app-target-form', imports: [ReactiveFormsModule, Icon], templateUrl: './target-form.html', styleUrls: ['../../../shared/ui/page.css', './target-form.css'] })
export class TargetForm {
  readonly original = input<TargetConfigurationDto | null>(null); readonly busy = input(false); readonly blocked = input(false); readonly error = input<ApiClientError | null>(null);
  readonly save = output<UpdateTargetRequest>(); readonly cancel = output<void>();
  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef); private readonly revision = signal(0); readonly submitted = signal(false);
  private readonly leaveDialog = viewChild.required<ElementRef<HTMLDialogElement>>('leaveDialog');
  private readonly keepEditing = viewChild.required<ElementRef<HTMLButtonElement>>('keepEditing');
  private leaveDecision?: Promise<boolean>;
  private resolveLeave?: (leave: boolean) => void;
  private leaveOpener?: HTMLElement;
  readonly isChat = computed(() => { this.revision(); return this.form.controls.adapterType.value === 'OPENAI_COMPATIBLE'; });
  readonly form = new FormGroup({
    adapterType: new FormControl<TargetAdapterType>('REST_LEGACY', { nonNullable: true }),
    model: new FormControl('', { nonNullable: true }),
    systemPrompt: new FormControl('', { nonNullable: true }),
    name: new FormControl('', { nonNullable: true, validators: textValidator(120) }),
    url: new FormControl('', { nonNullable: true, validators: urlValidator }),
    requestTemplate: new FormControl('', { nonNullable: true, validators: templateValidator }),
    responsePath: new FormControl('', { nonNullable: true, validators: textValidator(1024, true) }),
    timeoutMs: new FormControl(10000, { nonNullable: true, validators: timeoutValidator }),
    headers: new FormArray<FormGroup<{ name: FormControl<string>; value: FormControl<string> }>>([]),
    authType: new FormControl<AuthType>('NONE', { nonNullable: true }), headerName: new FormControl('', { nonNullable: true }),
    secretValue: new FormControl('', { nonNullable: true }), credentialMode: new FormControl<'conserve' | 'replace'>('replace', { nonNullable: true })
  });
  readonly retainAvailable = computed(() => { this.revision(); const value = this.form.getRawValue(); return canConserve(this.original(), value.authType, value.url, value.headerName); });
  readonly extraErrors = computed<Partial<Record<FieldName, string>>>(() => {
    this.revision(); const value = this.form.getRawValue(); const errors: Partial<Record<FieldName, string>> = {};
    if (value.adapterType === 'OPENAI_COMPATIBLE') {
      if (!value.model.trim() || [...value.model.trim()].length > 200 || /[\u0000-\u001f\u007f]/.test(value.model) || value.model.includes('{{PROMPT}}')) errors.model = 'Introduce un modelo válido de hasta 200 caracteres.';
      if (value.systemPrompt && (!value.systemPrompt.trim() || new TextEncoder().encode(value.systemPrompt).length > 16384 || value.systemPrompt.includes('{{PROMPT}}') || value.systemPrompt.includes('\u0000'))) errors.systemPrompt = 'Introduce instrucciones sintéticas de hasta 16 KiB, sin placeholder.';
      const messages = [...(value.systemPrompt ? [{ role: 'system', content: value.systemPrompt }] : []), { role: 'user', content: '{{PROMPT}}' }];
      if (new TextEncoder().encode(JSON.stringify({ model: value.model.trim(), messages, stream: false })).length > 32768) errors.systemPrompt = 'La configuración generada supera 32 KiB.';
    }
    const headerError = value.adapterType === 'OPENAI_COMPATIBLE' ? null : headersError(value.headers, value.authType, value.headerName); if (headerError) errors.headers = headerError;
    if (value.authType === 'API_KEY_HEADER') { const error = authHeaderError(value.headerName); if (error) errors['authConfig.headerName'] = error; }
    if (value.authType !== 'NONE' && !(value.credentialMode === 'conserve' && this.retainAvailable())) { const error = secretError(value.secretValue, value.authType); if (error) errors['authConfig.secretValue'] = error; }
    return errors;
  });
  constructor() {
    let previousType: AuthType = 'NONE';
    let previousAdapter: TargetAdapterType = 'REST_LEGACY';
    let previousOrigin = '';
    const origin = (url: string) => { try { return new URL(url).origin; } catch { return ''; } };
    this.form.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
      const adapter = this.form.controls.adapterType.value;
      if (adapter !== previousAdapter) {
        this.clearSecret(); previousAdapter = adapter;
        if (adapter === 'OPENAI_COMPATIBLE') {
          this.form.controls.authType.setValue('BEARER_TOKEN', { emitEvent: false });
          this.form.controls.headerName.setValue('', { emitEvent: false });
          this.form.controls.headers.clear({ emitEvent: false });
        }
        this.syncAdapterControls();
      }
      const nextOrigin = origin(this.form.controls.url.value);
      if (nextOrigin !== previousOrigin) { this.clearSecret(); previousOrigin = nextOrigin; }
      const type = this.form.controls.authType.value;
      if (type !== previousType) {
        this.clearSecret(); previousType = type;
        const value = this.form.getRawValue();
        this.form.controls.credentialMode.setValue(canConserve(this.original(), type, value.url, value.headerName) ? 'conserve' : 'replace', { emitEvent: false });
      }
      this.revision.update(value => value + 1);
    });
    effect(() => {
      const original = this.original();
      this.form.controls.headers.clear({ emitEvent: false });
      for (const [name, value] of Object.entries(original?.headers ?? {})) this.form.controls.headers.push(this.header(name, value), { emitEvent: false });
      this.form.reset({ adapterType: original?.adapterType ?? 'REST_LEGACY', model: original?.model ?? '', systemPrompt: original?.systemPrompt ?? '', name: original?.name ?? '', url: original ? targetEndpoint(original) : '', requestTemplate: original?.requestTemplate ?? '', responsePath: original?.responsePath ?? '', timeoutMs: original?.timeoutMs ?? 10000,
        authType: original?.authType ?? 'NONE', headerName: original?.authType === 'API_KEY_HEADER' ? original.authConfig.headerName : '', secretValue: '', credentialMode: original?.hasCredentials ? 'conserve' : 'replace' }, { emitEvent: false });
      previousAdapter = original?.adapterType ?? 'REST_LEGACY'; previousOrigin = original ? origin(targetEndpoint(original)) : '';
      this.syncAdapterControls();
      previousType = original?.authType ?? 'NONE'; this.submitted.set(false); this.revision.update(value => value + 1);
    });
    effect(() => { if (this.error()) { this.clearSecret(); this.focusError(); } });
    inject(DestroyRef).onDestroy(() => {
      this.clearSecret();
      this.resolveLeave?.(false);
      this.resolveLeave = undefined;
      this.leaveDecision = undefined;
      this.leaveOpener = undefined;
    });
  }
  private syncAdapterControls() {
    for (const control of [this.form.controls.requestTemplate, this.form.controls.responsePath]) {
      if (this.form.controls.adapterType.value === 'OPENAI_COMPATIBLE') control.disable({ emitEvent: false }); else control.enable({ emitEvent: false });
    }
  }
  private header(name = '', value = '') { return new FormGroup({ name: new FormControl(name, { nonNullable: true }), value: new FormControl(value, { nonNullable: true }) }); }
  addHeader() { if (this.form.controls.headers.length < 20) this.form.controls.headers.push(this.header()); this.form.markAsDirty(); }
  removeHeader(index: number) { this.form.controls.headers.removeAt(index); this.form.markAsDirty(); }
  clearSecret() { this.form.controls.secretValue.setValue('', { emitEvent: false }); this.revision.update(value => value + 1); }
  fieldError(field: FieldName): string {
    const backend = this.error()?.fieldErrors.find(error => error.field === field)?.message;
    if (backend) return backend;
    const local = field === 'authConfig.secretValue' ? this.form.controls.secretValue : field === 'authConfig.headerName' ? this.form.controls.headerName : field === 'endpoint' ? this.form.controls.url : this.form.get(field);
    if (!this.submitted() && !local?.touched) return '';
    return (local?.errors?.['invalid'] as string | undefined) ?? this.extraErrors()[field] ?? '';
  }
  private focusError() {
    setTimeout(() => {
      const fields: readonly FieldName[] = ['adapterType', 'name', 'url', 'endpoint', 'model', 'systemPrompt', 'timeoutMs', 'requestTemplate', 'responsePath', 'headers', 'authType', 'authConfig.headerName', 'authConfig.secretValue', 'authConfig'];
      const field = fields.find(item => this.fieldError(item));
      if (field) this.element.nativeElement.querySelector<HTMLElement>(`[data-field="${field}"]`)?.focus();
    });
  }
  submit() {
    if (this.busy() || this.blocked()) return;
    this.submitted.set(true); this.form.markAllAsTouched();
    if (this.form.invalid || Object.keys(this.extraErrors()).length > 0) { this.focusError(); return; }
    const value = this.form.getRawValue();
    const changedType = this.original() && (this.original()?.adapterType ?? 'REST_LEGACY') !== value.adapterType;
    if (changedType && !window.confirm('Cambiar el tipo sustituirá los campos de configuración anteriores. ¿Confirmas el cambio?')) { this.clearSecret(); return; }
    let request = targetRequest(value, value.credentialMode === 'conserve' && this.retainAvailable());
    if (changedType && value.adapterType === 'REST_LEGACY') request = { ...request, adapterType: 'REST_LEGACY' } as UpdateTargetRequest;
    this.clearSecret(); this.save.emit(request);
  }
  cancelForm() { this.clearSecret(); this.cancel.emit(); }
  confirmLeave(): boolean | Promise<boolean> {
    this.clearSecret();
    if (this.leaveDecision) return this.leaveDecision;
    if (!this.form.dirty) return true;
    const active = this.element.nativeElement.ownerDocument.activeElement;
    this.leaveOpener = active instanceof HTMLElement ? active : undefined;
    this.leaveDecision = new Promise<boolean>(resolve => this.resolveLeave = resolve);
    this.leaveDialog().nativeElement.showModal();
    this.keepEditing().nativeElement.focus();
    return this.leaveDecision;
  }
  finishLeave(leave: boolean, event?: Event) {
    event?.preventDefault();
    const resolve = this.resolveLeave;
    if (!resolve) return;
    const opener = this.leaveOpener;
    this.resolveLeave = undefined;
    this.leaveDecision = undefined;
    this.leaveOpener = undefined;
    this.leaveDialog().nativeElement.close();
    if (!leave && opener?.isConnected) opener.focus({ preventScroll: true });
    resolve(leave);
  }
  onLeaveClosed() {
    if (!this.leaveDialog().nativeElement.open) this.finishLeave(false);
  }
}
