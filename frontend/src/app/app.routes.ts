import { Routes } from '@angular/router';
import { unsavedTarget } from './features/target/guards/unsaved-target';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'projects' },
  { path: 'projects', title: 'Proyectos · LLM Scanner Seguridad', loadComponent: () => import('./features/projects/pages/projects-page').then(module => module.ProjectsPage) },
  { path: 'projects/:projectId/target', title: 'Target · LLM Scanner Seguridad', canDeactivate: [unsavedTarget], loadComponent: () => import('./features/target/pages/target-page').then(module => module.TargetPage) },
  { path: 'projects/:projectId/scans/:scanId', title: 'Análisis · LLM Scanner Seguridad', loadComponent: () => import('./features/scans/pages/scan-page').then(module => module.ScanPage) },
  { path: 'projects/:projectId', title: 'Proyecto · LLM Scanner Seguridad', loadComponent: () => import('./features/projects/pages/project-page').then(module => module.ProjectPage) },
  { path: '**', title: 'Página no encontrada · LLM Scanner Seguridad', loadComponent: () => import('./shared/ui/not-found-page').then(module => module.NotFoundPage) }
];
