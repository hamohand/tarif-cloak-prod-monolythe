import {HttpErrorResponse, HttpInterceptorFn} from '@angular/common/http';
import { inject } from '@angular/core';
import { OAuthService } from 'angular-oauth2-oidc';
import {catchError, switchMap} from 'rxjs/operators';
import {throwError, from} from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const oauthService = inject(OAuthService);
  const authService = inject(AuthService);
  const token = oauthService.getAccessToken();

  // Vérifier l'expiration du token avant chaque requête
  if (token && !req.url.includes('/realms/')) {
    // Vérifier si le token est valide
    if (!oauthService.hasValidAccessToken()) {
      // Tenter un rafraîchissement silencieux du token
      console.warn('Token invalide détecté, tentative de rafraîchissement...');
      
      return from(oauthService.silentRefresh()).pipe(
        switchMap(() => {
          // Après le rafraîchissement, obtenir le nouveau token
          const newToken = oauthService.getAccessToken();
          if (!newToken || !oauthService.hasValidAccessToken()) {
            console.warn('Impossible de rafraîchir le token. Déconnexion automatique.');
            authService.logout();
            return throwError(() => new Error('Token expiré et impossible de rafraîchir'));
          }
          
          // Envoyer la requête avec le nouveau token
          const cloned = req.clone({
            setHeaders: {
              Authorization: `Bearer ${newToken}`
            }
          });
          return next(cloned);
        }),
        catchError((error: any) => {
          // Si le rafraîchissement échoue, déconnecter
          console.warn('Erreur lors du rafraîchissement du token. Déconnexion automatique.', error);
          authService.logout();
          return throwError(() => error);
        })
      );
    }
    
    // Token valide, envoyer la requête normalement
    const cloned = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    return next(cloned).pipe(
      catchError((error: HttpErrorResponse) => {
        // Si erreur 401 (Unauthorized), déconnecter l'utilisateur
        if (error.status === 401) {
          console.warn('Token expiré ou invalide (401). Déconnexion automatique.');
          authService.logout();
        }
        return throwError(() => error);
      })
    );
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Gérer aussi les erreurs 401 pour les requêtes sans token (token expiré)
      if (error.status === 401) {
        console.warn('Token expiré ou invalide (401). Déconnexion automatique.');
        authService.logout();
      }
      return throwError(() => error);
    })
  );

}
