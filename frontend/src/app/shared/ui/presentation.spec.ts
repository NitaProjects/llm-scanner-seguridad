import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { PageHeader } from './page-header';
import { EmptyState } from './empty-state';
import { LoadingState } from './loading-state';
import { StatCard } from './stat-card';
import { FeedbackPanel } from './feedback-panel';

@Component({ imports: [PageHeader, EmptyState], template: `<app-page-header title="Proyectos" description="Solo sistemas autorizados"><button (click)="actions = actions + 1">Crear</button></app-page-header><app-empty-state title="Sin proyectos" message="Configura tu primer análisis"><button (click)="actions = actions + 1">Nuevo proyecto</button></app-empty-state>` })
class PresentationHost { actions = 0; }

describe('Shared presentation patterns', () => {
  it('keeps a focusable page heading, description and projected actions without services', () => {
    const fixture = TestBed.createComponent(PresentationHost); fixture.detectChanges();
    const node: HTMLElement = fixture.nativeElement;
    expect(node.querySelector('h1')?.getAttribute('tabindex')).toBe('-1');
    expect(node.querySelector('h1')?.textContent).toBe('Proyectos');
    expect(node.textContent).toContain('Solo sistemas autorizados');
    node.querySelector<HTMLButtonElement>('app-page-header button')!.click();
    node.querySelector<HTMLButtonElement>('app-empty-state button')!.click();
    expect(fixture.componentInstance.actions).toBe(2);
    expect(node.querySelectorAll('h1')).toHaveLength(1);
  });
  it('labels a loading region and does not invent a progress percentage', () => {
    const fixture = TestBed.createComponent(LoadingState); fixture.componentRef.setInput('label', 'Cargando proyectos…'); fixture.detectChanges();
    const node: HTMLElement = fixture.nativeElement;
    expect(node.querySelector('[role=status][aria-busy=true]')?.textContent).toContain('Cargando proyectos…');
    expect(node.querySelector('progress,[role=progressbar]')).toBeNull();
  });
  it('preserves zero as observed data in stat cards and escapes labels', () => {
    const fixture = TestBed.createComponent(StatCard); fixture.componentRef.setInput('label', '<script>fixture()</script>'); fixture.componentRef.setInput('value', 0); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('strong').textContent).toBe('0');
    expect(fixture.nativeElement.querySelector('script')).toBeNull();
  });
  it('allows subsection empty states without inserting another page heading', () => {
    const fixture = TestBed.createComponent(EmptyState); fixture.componentRef.setInput('title', 'Sin hallazgos'); fixture.componentRef.setInput('message', 'No demuestra seguridad'); fixture.componentRef.setInput('level', 3); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('h3').textContent).toBe('Sin hallazgos');
    expect(fixture.nativeElement.querySelector('h1,h2')).toBeNull();
  });
  it('keeps persistent error feedback and prevents retry while busy', () => {
    const fixture = TestBed.createComponent(FeedbackPanel); fixture.componentRef.setInput('message', 'No disponible'); fixture.componentRef.setInput('kind', 'error'); fixture.componentRef.setInput('retryable', true); fixture.componentRef.setInput('busy', true); fixture.detectChanges();
    const retry = vi.fn(); fixture.componentInstance.retry.subscribe(retry);
    fixture.nativeElement.querySelector('button').click(); expect(retry).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('[role=alert]').textContent).toContain('No disponible');
    fixture.componentRef.setInput('busy', false); fixture.detectChanges(); fixture.nativeElement.querySelector('button').click(); expect(retry).toHaveBeenCalledTimes(1);
  });
});
