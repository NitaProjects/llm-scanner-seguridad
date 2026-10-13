import { Component, DestroyRef, ElementRef, computed, effect, inject, input, output, signal } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiClientError, FieldName } from '../../../core/api/api-error';
import { AuthType, TargetConfigurationDto, UpdateTargetRequest } from '../models/target';
import { authHeaderError, canConserve, headersError, secretError, targetRequest, templateValidator, textValidator, timeoutValidator, urlValidator } from '../forms/validators';
import { Icon } from '../../../shared/ui/icon';
@Component({ selector: 'app-target-form', imports: [ReactiveFormsModule, Icon], templateUrl: './target-form.html', styleUrls: ['../../../shared/ui/page.css', './target-form.css'] })
export class TargetForm {
  readonly original = input<TargetConfigurationDto | null>(null); readonly busy = input(false); readonly blocked = input(false); readonly error = input<ApiClientError | null>(null);
  readonly save = output<UpdateTargetRequest>(); readonly cancel = output<void>();
  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef); private readonly revision = signal(0); readonly submitted = signal(false);
  readonly form = new FormGroup({
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
    const headerError = headersError(value.headers, value.authType, value.headerName); if (headerError) errors.headers = headerError;
    if (value.authType === 'API_KEY_HEADER') { const error = authHeaderError(value.headerName); if (error) errors['authConfig.headerName'] = error; }
    if (value.authType !== 'NONE' && !(value.credentialMode === 'conserve' && this.retainAvailable())) { const error = secretError(value.secretValue, value.authType); if (error) errors['authConfig.secretValue'] = error; }
    return errors;
  });
  constructor() {
    let previousType: AuthType = 'NONE';
    this.form.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => {
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
      this.form.reset({ name: original?.name ?? '', url: original?.url ?? '', requestTemplate: original?.requestTemplate ?? '', responsePath: original?.responsePath ?? '', timeoutMs: original?.timeoutMs ?? 10000,
        authType: original?.authType ?? 'NONE', headerName: original?.authType === 'API_KEY_HEADER' ? original.authConfig.headerName : '', secretValue: '', credentialMode: original?.hasCredentials ? 'conserve' : 'replace' }, { emitEvent: false });
      previousType = original?.authType ?? 'NONE'; this.submitted.set(false); this.revision.update(value => value + 1);
    });
    effect(() => { if (this.error()) { this.clearSecret(); this.focusError(); } });
    inject(DestroyRef).onDestroy(() => this.clearSecret());
  }
  private header(name = '', value = '') { return new FormGroup({ name: new FormControl(name, { nonNullable: true }), value: new FormControl(value, { nonNullable: true }) }); }
  addHeader() { if (this.form.controls.headers.length < 20) this.form.controls.headers.push(this.header()); this.form.markAsDirty(); }
  removeHeader(index: number) { this.form.controls.headers.removeAt(index); this.form.markAsDirty(); }
  clearSecret() { this.form.controls.secretValue.setValue('', { emitEvent: false }); this.revision.update(value => value + 1); }
  fieldError(field: FieldName): string {
    const backend = this.error()?.fieldErrors.find(error => error.field === field)?.message;
    if (backend) return backend;
    const local = field === 'authConfig.secretValue' ? this.form.controls.secretValue : field === 'authConfig.headerName' ? this.form.controls.headerName : this.form.get(field);
    if (!this.submitted() && !local?.touched) return '';
    return (local?.errors?.['invalid'] as string | undefined) ?? this.extraErrors()[field] ?? '';
  }
  private focusError() {
    setTimeout(() => {
      const fields: readonly FieldName[] = ['name', 'url', 'timeoutMs', 'requestTemplate', 'responsePath', 'headers', 'authType', 'authConfig.headerName', 'authConfig.secretValue', 'authConfig'];
      const field = fields.find(item => this.fieldError(item));
      if (field) this.element.nativeElement.querySelector<HTMLElement>(`[data-field="${field}"]`)?.focus();
    });
  }
  submit() {
    if (this.busy() || this.blocked()) return;
    this.submitted.set(true); this.form.markAllAsTouched();
    if (this.form.invalid || Object.keys(this.extraErrors()).length > 0) { this.focusError(); return; }
    const value = this.form.getRawValue();
    const request = targetRequest(value, value.credentialMode === 'conserve' && this.retainAvailable());
    this.clearSecret(); this.save.emit(request);
  }
  cancelForm() { this.clearSecret(); this.cancel.emit(); }
  confirmLeave(): boolean {
    const dirty = this.form.dirty;
    this.clearSecret();
    return !dirty || window.confirm('Hay cambios sin guardar. ¿Quieres salir y descartarlos?');
  }
}
