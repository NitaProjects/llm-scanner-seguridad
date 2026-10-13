import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Icon } from './icon';
@Component({ selector: 'app-not-found-page', imports: [RouterLink, Icon], styleUrl: './page.css', template: '<section class="empty-state"><app-icon name="document" class="empty-icon" /><h1 tabindex="-1">Página no encontrada</h1><p>La dirección no corresponde a una página disponible.</p><a class="button secondary" routerLink="/projects">Volver a proyectos</a></section>' })
export class NotFoundPage {}
