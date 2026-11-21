import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NavigationEnd, NavigationSkipped, Router } from '@angular/router';
import { filter, firstValueFrom } from 'rxjs';
import { App } from './app';
import { appConfig } from './app.config';
import { execution, projectId, scanId, scan, scenario } from '../testing/fixtures';

describe('Fragment navigation with a root base and an internal Scan route', () => {
  const scanRoute = `/projects/${projectId}/scans/${scanId}`;
  const apiPath = `/api${scanRoute}`;
  const data = scenario('mixed');
  let fixture: ComponentFixture<App>;
  let http: HttpTestingController;
  let router: Router;
  let base: HTMLBaseElement;
  let previousBase: string | null;
  let addedBase: boolean;
  let previousScroll: PropertyDescriptor | undefined;
  const scrollIntoView = vi.fn();

  beforeEach(async () => {
    const existingBase = document.querySelector('base');
    addedBase = existingBase === null;
    base = existingBase ?? document.createElement('base');
    previousBase = base.getAttribute('href');
    base.setAttribute('href', '/');
    if (addedBase) document.head.append(base);
    previousScroll = Object.getOwnPropertyDescriptor(HTMLElement.prototype, 'scrollIntoView');
    scrollIntoView.mockClear();
    Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', { configurable: true, value: scrollIntoView });
    vi.spyOn(window, 'scrollTo').mockImplementation(() => {});
    TestBed.configureTestingModule({ imports: [App], providers: [...appConfig.providers, provideHttpClientTesting()] });
    fixture = TestBed.createComponent(App);
    router = TestBed.inject(Router);
    http = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    await router.navigateByUrl(scanRoute);
    http.expectOne(apiPath).flush(scan({ status: 'COMPLETED' }));
    http.expectOne(apiPath + '/executions').flush([0, 1, 2, 3].map(index => execution(index)));
    http.expectOne(apiPath + '/evaluations').flush({ evaluationProfile: 'evaluation-engine-v1', items: data.evaluations });
    http.expectOne(apiPath + '/findings').flush(data.findings);
    http.expectOne(apiPath + '/score').flush({ availability: 'AVAILABLE', score: data.score });
    http.expectOne(apiPath + '/report').flush({ availability: 'AVAILABLE', report: data.report });
    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    http.verify();
    vi.restoreAllMocks();
    if (previousScroll) Object.defineProperty(HTMLElement.prototype, 'scrollIntoView', previousScroll);
    else Reflect.deleteProperty(HTMLElement.prototype, 'scrollIntoView');
    if (addedBase) base.remove();
    else if (previousBase === null) base.removeAttribute('href');
    else base.setAttribute('href', previousBase);
  });

  async function follow(selector: string, fragment: string) {
    const link = (fixture.nativeElement as HTMLElement).querySelector<HTMLAnchorElement>(selector)!;
    expect(link).not.toBeNull();
    expect(document.querySelector('base')?.getAttribute('href')).toBe('/');
    expect(new URL(link.href).pathname).toBe(scanRoute);
    expect(new URL(link.href).hash).toBe(`#${fragment}`);
    const navigation = firstValueFrom(router.events.pipe(filter(event => event instanceof NavigationEnd)));
    link.click();
    await navigation;
    fixture.detectChanges();
    await fixture.whenStable();
    expect(router.url).toBe(`${scanRoute}#${fragment}`);
    expect((fixture.nativeElement as HTMLElement).querySelector('app-scan-page')).not.toBeNull();
    http.expectNone('/api/projects');
  }

  it('keeps the Scan route for the risk summary link', async () => {
    await follow('app-report-sections a[fragment="risk"]', 'risk');
  });

  it.each(['categories', 'findings', 'evaluations', 'report'])('keeps the Scan route for the %s section', async fragment => {
    await follow(`.anchors a[fragment="${fragment}"]`, fragment);
  });

  it.each(['findings', 'evaluations'])('keeps the Scan route for the report %s reference', async fragment => {
    await follow(`app-report-sections a[fragment="${fragment}"]`, fragment);
  });

  it.each([
    `evaluation-${data.evaluations[0].id}`,
    `finding-${data.findings[0].id}`
  ])('keeps the Scan route for report item reference %s', async fragment => {
    const details = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLDetailsElement>('app-report-sections details');
    details.forEach(item => item.open = true);
    await follow(`app-report-sections a[href$="#${fragment}"]`, fragment);
  });

  it('keeps the Scan route and scrolls/focuses content when skipping navigation', async () => {
    const content = (fixture.nativeElement as HTMLElement).querySelector<HTMLElement>('#content')!;
    await follow('.skip-link', 'content');
    expect(scrollIntoView).toHaveBeenCalled();
    expect(scrollIntoView.mock.contexts).toContain(content);
    expect(document.activeElement).toBe(content);
    // El mismo fragmento puede activarse de nuevo sin NavigationEnd.
    (fixture.nativeElement as HTMLElement).querySelector<HTMLAnchorElement>('.skip-link')!.focus();
    (fixture.nativeElement as HTMLElement).querySelector<HTMLAnchorElement>('.skip-link')!.click();
    await fixture.whenStable();
    expect(router.url).toBe(`${scanRoute}#content`);
    expect(document.activeElement).toBe(content);
  });
  it('reveals an evaluation again when its current-fragment link is reactivated after collapsing the detail', async () => {
    const fragment = 'evaluation-' + data.evaluations[0].id;
    const node = fixture.nativeElement as HTMLElement;
    node.querySelectorAll<HTMLDetailsElement>('app-report-sections details').forEach(item => item.open = true);
    await follow(`app-report-sections a[href$="#${fragment}"]`, fragment);
    const technical = node.querySelector<HTMLDetailsElement>('.technical-depth')!;
    technical.open = false;
    const skipped = firstValueFrom(router.events.pipe(filter(event => event instanceof NavigationSkipped)));
    node.querySelector<HTMLAnchorElement>(`app-report-sections a[href$="#${fragment}"]`)!.click();
    await skipped; fixture.detectChanges(); await fixture.whenStable();
    expect(technical.open).toBe(true);
    expect(document.activeElement?.id).toBe(fragment);
    http.expectNone('/api/projects');
  });
});
