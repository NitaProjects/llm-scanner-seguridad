import { Component, ElementRef, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { Icon } from './shared/ui/icon';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Icon],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef);
  private pageUrl: string | undefined;
  constructor() {
    const router = inject(Router);
    router.events.pipe(filter(event => event instanceof NavigationEnd), takeUntilDestroyed()).subscribe(event => {
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
