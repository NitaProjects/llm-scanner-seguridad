import { TestBed } from '@angular/core/testing';
import { App } from './app';
import { provideRouter, Router } from '@angular/router';
import { Component, LOCALE_ID } from '@angular/core';
import { formatDate } from '@angular/common';
import { appConfig } from './app.config';

@Component({ template: '<h1 tabindex="-1">Synthetic page</h1>' })
class SyntheticPage {}

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [...appConfig.providers, provideRouter([
        { path: 'projects', component: SyntheticPage },
        { path: 'projects/:id', component: SyntheticPage },
        { path: 'projects/:id/target', component: SyntheticPage },
        { path: 'projects/:id/scans/:scanId', component: SyntheticPage },
        { path: '**', component: SyntheticPage }
      ])],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should present dates in Spanish with the configured locale', () => {
    expect(TestBed.inject(LOCALE_ID)).toBe('es');
    expect(formatDate('2026-10-10T12:00:00Z', 'medium', TestBed.inject(LOCALE_ID), 'UTC')).toContain('oct');
  });

  it('should render the application shell and project navigation', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('header')?.textContent).toContain('LLM Scanner');
    expect(compiled.querySelector('nav a')?.getAttribute('href')).toBe('/projects');
    expect(compiled.querySelector('main router-outlet')).not.toBeNull();
  });

  it.each(['/projects', '/projects/test', '/projects/test/target', '/projects/test/scans/test', '/unknown', '/projects/test/unknown'])('marks only project destinations as the current location for %s', async url => {
    const fixture = TestBed.createComponent(App); fixture.detectChanges();
    await TestBed.inject(Router).navigateByUrl(url);
    fixture.detectChanges(); await fixture.whenStable();
    const link: HTMLAnchorElement = fixture.nativeElement.querySelector('nav a');
    expect(link.getAttribute('aria-current')).toBe(url.includes('unknown') ? null : 'location');
    expect(link.classList.contains('active')).toBe(!url.includes('unknown'));
    expect(link.querySelector('svg')?.getAttribute('aria-hidden')).toBe('true');
    expect(link.textContent?.trim()).toBe('Proyectos');
  });
});
