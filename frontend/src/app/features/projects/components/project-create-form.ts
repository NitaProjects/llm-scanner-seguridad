import { AfterViewInit, Component, ElementRef, inject, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ApiClientError } from '../../../core/api/api-error';
import { textValidator } from '../../target/forms/validators';
import { CreateProjectRequest } from '../models/project';
@Component({
  selector: 'app-project-create-form', imports: [ReactiveFormsModule], styleUrl: '../../../shared/ui/page.css',
  template: `<form [formGroup]="form" (ngSubmit)="submit()" class="card" aria-label="Crear proyecto">
    <h2>Nuevo proyecto</h2><div class="field"><label for="project-name">Nombre</label><input id="project-name" formControlName="name" aria-describedby="project-name-error" [attr.aria-invalid]="!!nameError()" />
    <small id="project-name-error" class="field-error">{{ nameError() }}</small></div>
    <div class="field"><label for="project-description">Descripción (opcional)</label><textarea id="project-description" formControlName="description" aria-describedby="project-description-error" [attr.aria-invalid]="!!descriptionError()"></textarea><small id="project-description-error" class="field-error">{{ descriptionError() }}</small></div>
    <div class="actions"><button type="submit" [disabled]="busy()">{{ busy() ? 'Creando…' : 'Crear proyecto' }}</button><button type="button" class="secondary" [disabled]="busy()" (click)="cancel.emit()">Cancelar</button></div>
  </form>`
})
export class ProjectCreateForm implements AfterViewInit {
  readonly busy = input(false); readonly error = input<ApiClientError | null>(null);
  readonly create = output<CreateProjectRequest>(); readonly cancel = output<void>();
  readonly form = new FormGroup({ name: new FormControl('', { nonNullable: true, validators: textValidator(120, true) }), description: new FormControl('', { nonNullable: true, validators: textValidator(2000) }) });
  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef);
  ngAfterViewInit() { this.element.nativeElement.querySelector<HTMLInputElement>('input')?.focus(); }
  nameError() { return this.error()?.fieldErrors.find(field => field.field === 'name')?.message ?? (this.form.controls.name.touched ? this.form.controls.name.errors?.['invalid'] as string : ''); }
  descriptionError() { return this.error()?.fieldErrors.find(field => field.field === 'description')?.message ?? (this.form.controls.description.touched ? this.form.controls.description.errors?.['invalid'] as string : ''); }
  submit() {
    if (this.busy()) return;
    this.form.markAllAsTouched();
    if (this.form.invalid) { this.element.nativeElement.querySelector<HTMLElement>(this.form.controls.name.invalid ? '#project-name' : '#project-description')?.focus(); return; }
    const value = this.form.getRawValue(); this.create.emit({ name: value.name, description: value.description.trim() || null });
  }
}
