import { TestBed } from '@angular/core/testing';
import { Icon, IconName } from './icon';
import { StatusBadge } from './status-badge';

describe('Visual icon contract', () => {
  it.each<IconName>(['plus', 'folder', 'settings', 'play', 'shield', 'warning', 'question', 'check', 'document', 'circle', 'open', 'trash'])('renders static decorative %s without an accessible name or focus', name => {
    const fixture = TestBed.createComponent(Icon);
    fixture.componentRef.setInput('name', name);
    fixture.detectChanges();
    const svg: SVGElement = fixture.nativeElement.querySelector('svg');
    expect(svg.getAttribute('viewBox')).toBe('0 0 24 24');
    expect(svg.getAttribute('aria-hidden')).toBe('true');
    expect(svg.getAttribute('focusable')).toBe('false');
    expect(svg.hasAttribute('tabindex')).toBe(false);
    expect(svg.querySelector('path')?.getAttribute('d')).toBeTruthy();
    expect(svg.querySelector('title,image,use,script,foreignObject')).toBeNull();
  });

  it('updates a visual path and permits a sizing class without adding business state', () => {
    const fixture = TestBed.createComponent(Icon);
    fixture.componentRef.setInput('name', 'plus'); fixture.detectChanges();
    const first = fixture.nativeElement.querySelector('path').getAttribute('d');
    fixture.nativeElement.classList.add('empty-icon');
    fixture.componentRef.setInput('name', 'folder'); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('path').getAttribute('d')).not.toBe(first);
    expect(fixture.nativeElement.classList.contains('empty-icon')).toBe(true);
  });

  it('keeps LOW informative and risk distinct from severity, uncertainty and technical completion', () => {
    const fixture = TestBed.createComponent(StatusBadge);
    for (const [kind, value, label] of [
      ['risk', 'LOW', 'Riesgo bajo'], ['severity', 'LOW', 'Severidad baja'],
      ['verdict', 'INCONCLUSIVE', 'Resultado inconcluso'], ['status', 'COMPLETED', 'Completado']
    ]) {
      fixture.componentRef.setInput('badge', { kind, value }); fixture.detectChanges();
      const node: HTMLElement = fixture.nativeElement;
      expect(node.textContent?.trim()).toBe(label);
      expect(node.querySelector('button,a,[tabindex],[role=button]')).toBeNull();
      expect(node.querySelector('svg')?.getAttribute('aria-hidden')).toBe('true');
    }
  });
});
