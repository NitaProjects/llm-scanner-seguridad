import { Component, input } from '@angular/core';

const paths = {
  plus: 'M12 5v14M5 12h14',
  open: 'M14 3h7v7M21 3 11 13M10 5H4v15h15v-6',
  trash: 'M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7M14 10v7',
  folder: 'M3 7V5h6l2 2h10v13H3V7Z',
  settings: 'M4 6h16M4 12h16M4 18h16M8 3v6M16 9v6M10 15v6',
  play: 'm8 4 12 8-12 8V4Z',
  shield: 'm12 3 8 3v6c0 4-4 7-8 9-4-2-8-5-8-9V6l8-3ZM8 9l4-2 4 2v4l-4 3-4-3V9Z',
  warning: 'm12 3 10 18H2L12 3ZM12 9v5M12 17v.5',
  question: 'M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0ZM9.5 9a2.5 2.5 0 1 1 4 2c-1 .7-1.5 1-1.5 2.5M12 17v.5',
  check: 'm5 12 4 4L19 6',
  document: 'M14 3H5v18h14V8l-5-5ZM14 3v5h5M8 12h8M8 16h5',
  circle: 'M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0Z'
} as const;
export type IconName = keyof typeof paths;

@Component({
  selector: 'app-icon',
  template: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path [attr.d]="paths[name()]" /></svg>',
  styles: ':host{display:inline-flex;width:1.125rem;height:1.125rem;flex-shrink:0;vertical-align:middle}svg{display:block;width:100%;height:100%}:host(.badge-icon){width:.875rem;height:.875rem}:host(.empty-icon){width:40px;height:40px}:host(.brand-icon){width:28px;height:28px}'
})
export class Icon {
  readonly name = input.required<IconName>();
  readonly paths = paths;
}
