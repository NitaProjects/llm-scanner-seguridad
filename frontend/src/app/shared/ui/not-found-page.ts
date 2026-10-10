import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
@Component({ selector: 'app-not-found-page', imports: [RouterLink], template: '<h1 tabindex="-1">Página no encontrada</h1><p>La dirección no corresponde a una página disponible.</p><a routerLink="/projects">Volver a proyectos</a>' })
export class NotFoundPage {}
