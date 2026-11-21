import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { By } from '@angular/platform-browser';
import { version } from '../../package.json';
import { App } from './app';
import { loaded } from './shared/models/load-state';
import { project, projectId, scan, scanId } from '../testing/fixtures';

@Component({ template: '<h1 tabindex="-1">Colección</h1>' })
class CollectionPage {}
@Component({ template: '<h1 tabindex="-1">Proyecto</h1>' })
class ProjectContextPage { readonly project = signal(loaded(project)); }
@Component({ template: '<h1 tabindex="-1">Scan</h1><section id="report">Informe</section>' })
class ScanContextPage { readonly state = { scan: signal(loaded(scan())) }; }

describe('Risk Intelligence contextual shell', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [App], providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([
    { path: 'projects', component: CollectionPage },
    { path: 'projects/:projectId', component: ProjectContextPage },
    { path: 'projects/:projectId/target', component: ProjectContextPage },
    { path: 'projects/:projectId/scans/:scanId', component: ScanContextPage }
    ])] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => { TestBed.resetTestingModule(); http.verify(); });
  async function open(url: string) {
    const fixture = TestBed.createComponent(App); fixture.detectChanges();
    await TestBed.inject(Router).navigateByUrl(url); fixture.detectChanges(); await fixture.whenStable();
    return { fixture, node: fixture.nativeElement as HTMLElement };
  }
  it('shows two-level branding, package-synchronized version and no fictitious global links', async () => {
    const { node } = await open('/projects');
    expect(node.querySelector('.brand strong')?.textContent).toBe('LLM Scanner Seguridad');
    expect(node.querySelector('.brand-descriptor')?.textContent).toBe('Análisis de seguridad LLM');
    expect(node.textContent).not.toContain('Risk Intelligence');
    expect(node.querySelector('.sidebar-footer')?.textContent).toContain('Frontend v' + version);
    expect(node.querySelectorAll('nav a')).toHaveLength(1); expect(node.querySelector('nav a')?.getAttribute('aria-current')).toBe('page');
    expect(node.textContent).toContain('se consultan dentro de cada proyecto');
    expect(node.querySelector('a[href="/scans"],a[href="/reports"],a[href="/settings"]')).toBeNull();
  });
  it.each(['', '/target'])('shows existing project destinations with exact active state for %s', async suffix => {
    const { node } = await open('/projects/' + projectId + suffix);
    const links = Array.from(node.querySelectorAll<HTMLAnchorElement>('nav a'));
    expect(links.map(item => item.getAttribute('href'))).toEqual(['/projects', '/projects/' + projectId, '/projects/' + projectId + '/target']);
    expect(links.filter(item => item.hasAttribute('aria-current'))).toHaveLength(1);
    expect(links.find(item => item.hasAttribute('aria-current'))?.textContent).toContain(suffix ? 'Configuración del target' : 'Análisis del proyecto');
  });
  it('does not expose project context from incompatible route parameters', async () => {
    const { node } = await open('/projects/not-the-loaded-project'); expect(node.querySelectorAll('nav a')).toHaveLength(1);
  });
  it('only adds the report destination once a terminal section exists and keeps its Scan path', async () => {
    const url = '/projects/' + projectId + '/scans/' + scanId;
    const { fixture, node } = await open(url);
    expect(node.querySelector('nav a[fragment="report"]')).toBeNull();
    const page = fixture.debugElement.query(By.directive(ScanContextPage)).componentInstance as ScanContextPage;
    page.state.scan.set(loaded(scan({ status: 'COMPLETED' }))); fixture.detectChanges();
    expect(node.querySelector('nav a[fragment="report"]')?.getAttribute('href')).toBe(url + '#report');
    await TestBed.inject(Router).navigateByUrl(url + '#report'); fixture.detectChanges();
    expect(node.querySelector('nav a.active')?.textContent).toContain('Informe del análisis');
  });
});
