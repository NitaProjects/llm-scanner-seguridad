import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

bootstrapApplication(App, appConfig)
  .catch(() => {
    const host = document.querySelector('app-root');
    if (host) host.textContent = 'No se ha podido iniciar LLM Scanner Seguridad. Recarga la página.';
  });
