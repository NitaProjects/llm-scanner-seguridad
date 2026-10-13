import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { ScanDto } from '../../scans/models/scan';
import { StatusBadge } from '../../../shared/ui/status-badge';
import { Icon } from '../../../shared/ui/icon';
@Component({ selector: 'app-project-scan-list', imports: [RouterLink, DatePipe, StatusBadge, Icon], styleUrl: '../../../shared/ui/page.css', template: `
  @if (scans().length === 0) { <div class="empty-state"><app-icon name="document" class="empty-icon" /><h3>Sin análisis todavía</h3><p>No hay análisis registrados en este proyecto.</p></div> }
  <ul class="plain-list">@for (scan of scans(); track scan.id) { <li><div class="page-heading"><div><app-status-badge [badge]="{kind:'status',value:scan.status}" /><p class="meta">{{ scan.createdAt | date:'medium':'UTC' }} UTC</p></div><a class="row-link" [routerLink]="['/projects', projectId(), 'scans', scan.id]">Abrir análisis <span class="meta">{{ scan.id }}</span></a></div></li> }</ul>` })
export class ProjectScanList { readonly projectId = input.required<string>(); readonly scans = input.required<readonly ScanDto[]>(); }
