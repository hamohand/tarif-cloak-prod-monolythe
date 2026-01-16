import {
  ApplicationConfig,
  importProvidersFrom,
  provideBrowserGlobalErrorListeners,
  provideZoneChangeDetection
} from '@angular/core';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import {provideHttpClient, withInterceptors} from '@angular/common/http';
import {OAuthModule} from 'angular-oauth2-oidc';
import {authInterceptor} from './core/config/auth.interceptor';
import { environment } from '../environments/environment';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(),
    importProvidersFrom(
      OAuthModule.forRoot({
        resourceServer: {
          // Utiliser environment.apiUrl pour la prod, et ajouter localhost pour le dev
          allowedUrls: environment.production 
            ? [environment.apiUrl] 
            : ['http://localhost:8081/api', environment.apiUrl],
          sendAccessToken: true
        }
      })
    ),
    provideHttpClient(
      withInterceptors([authInterceptor])
    )
  ]
};
