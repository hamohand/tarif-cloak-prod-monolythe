import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { map, take } from 'rxjs/operators';
import { of } from 'rxjs';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);

  // Vérifier d'abord si le token est valide de manière synchrone (sans effets de bord)
  // Cette vérification ne déclenche pas de redirection et ne modifie pas l'état
  const tokenValid = authService.isTokenValid();
  
  if (!tokenValid) {
    // Si le token n'est pas valide, rediriger vers la connexion
    authService.login();
    return of(false);
  }
  
  // Si le token est valide, vérifier le statut d'authentification via l'Observable
  // Utiliser take(1) pour éviter les problèmes de navigation et prendre seulement la première valeur
  return authService.isAuthenticated().pipe(
    take(1),
    map(isAuthenticated => {
      // Si le token est valide (déjà vérifié), permettre l'accès
      // Le statut peut ne pas être à jour immédiatement, mais le token est valide
      if (isAuthenticated) {
        return true;
      }
      // Si le statut n'est pas à jour mais que le token est valide, permettre quand même l'accès
      // (le statut sera mis à jour par la vérification périodique ou lors de la prochaine requête)
      if (authService.isTokenValid()) {
        return true;
      }
      // Si ni le statut ni le token ne sont valides, rediriger vers la connexion
      authService.login();
      return false;
    })
  );
};
