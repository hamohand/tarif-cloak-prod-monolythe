import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { map, take } from 'rxjs/operators';

export const collaboratorGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  return authService.isCollaboratorAccount().pipe(
    take(1),
    map(isCollaborator => {
      if (isCollaborator) {
        return true;
      }
      router.navigate(['/organization/account']);
      return false;
    })
  );
};

