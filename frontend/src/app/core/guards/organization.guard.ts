import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { map, take } from 'rxjs/operators';

export const organizationGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.isOrganizationAccount().pipe(
    take(1),
    map(isOrg => {
      if (isOrg) {
        return true;
      }
      router.navigate(['/dashboard']);
      return false;
    })
  );
};

