import { TestBed } from '@angular/core/testing';
import { App } from './app';
import { provideRouter } from '@angular/router';
import { LOCALE_ID } from '@angular/core';
import { formatDate } from '@angular/common';
import { appConfig } from './app.config';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [...appConfig.providers, provideRouter([])],
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
});
