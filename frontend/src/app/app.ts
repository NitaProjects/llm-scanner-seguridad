import { Component, ElementRef, Signal, computed, inject, signal, viewChild } from '@angular/core';
import { RouterOutlet, RouterLink, Router, NavigationEnd } from '@angular/router';
import { Icon } from './shared/ui/icon';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';
import { version } from '../../package.json';
import { sameId } from './core/api/decode';
import { LoadState } from './shared/models/load-state';
import { ScanDto } from './features/scans/models/scan';

interface ContextPage {
  readonly project?: Signal<LoadState<{ readonly id: string; readonly name: string }>>;
  readonly state?: { readonly scan: Signal<LoadState<ScanDto>> };
}

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, Icon],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef);
  private pageUrl: string | undefined;
  private readonly outlet = viewChild(RouterOutlet);
  readonly currentUrl = signal('');
  readonly frontendVersion = version;
  readonly context = computed(() => {
    this.currentUrl();
    const outlet = this.outlet(); if (!outlet?.isActivated) return null;
    const page = outlet.component as ContextPage;
    const route = outlet.activatedRoute.snapshot;
    const projectId = route.paramMap.get('projectId');
    if (!projectId) return null;
    const project = page.project?.().data;
    const scan = page.state?.scan().data;
    if (!(project && sameId(project.id, projectId)) && !(scan && sameId(scan.projectId, projectId))) return null;
    const target = route.routeConfig?.path?.endsWith('/target') === true;
    return { projectId, name: project?.name ?? null, scanId: scan?.id ?? null, target,
      report: scan?.status === 'COMPLETED' || scan?.status === 'FAILED' };
  });
  readonly projectsActive = computed(() => this.currentUrl().split(/[?#]/)[0] === '/projects');
  readonly reportActive = computed(() => this.context()?.report === true && this.currentUrl().endsWith('#report'));
  constructor() {
    const router = inject(Router);
    router.events.pipe(filter(event => event instanceof NavigationEnd), takeUntilDestroyed()).subscribe(event => {
      this.currentUrl.set(event.urlAfterRedirects);
      const pageUrl = event.urlAfterRedirects.split('#')[0];
      if (pageUrl === this.pageUrl) return;
      const initialNavigation = this.pageUrl === undefined;
      this.pageUrl = pageUrl;
      if (initialNavigation) return;
      requestAnimationFrame(() => {
        if (router.url === event.urlAfterRedirects && !router.parseUrl(router.url).fragment) {
          this.element.nativeElement.querySelector<HTMLElement>('h1')?.focus();
        }
      });
    });
  }
  skipToContent(content: HTMLElement) {
    content.scrollIntoView();
    content.focus({ preventScroll: true });
  }
}
