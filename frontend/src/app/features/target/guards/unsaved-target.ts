import { CanDeactivateFn } from '@angular/router';
import type { TargetPage } from '../pages/target-page';
export const unsavedTarget: CanDeactivateFn<TargetPage> = component => component.canDeactivate();
