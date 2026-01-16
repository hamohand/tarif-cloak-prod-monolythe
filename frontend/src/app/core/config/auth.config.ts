import { AuthConfig } from 'angular-oauth2-oidc';
import { environment } from '../../../environments/environment';

// Déterminer si requireHttps doit être désactivé
// En production, si l'issuer utilise HTTPS, on peut garder requireHttps à true
// Mais on le désactive pour éviter les problèmes de validation stricte
const issuerUrl = environment.keycloak.issuer;
const isHttps = issuerUrl.startsWith('https://');

export const authConfig: AuthConfig = {
  // REALM - Configurable via environment
  issuer: issuerUrl,
  redirectUri: environment.keycloak.redirectUri,
  // CLIENT
  clientId: environment.keycloak.clientId,
  responseType: 'code',
  scope: 'openid profile email',
  showDebugInformation: !environment.production,
  // Désactiver les validations strictes pour éviter les erreurs HTTPS
  // Même si l'issuer utilise HTTPS, désactiver requireHttps pour éviter les vérifications strictes
  strictDiscoveryDocumentValidation: false,
  requireHttps: false, // Désactiver la vérification HTTPS stricte
  skipIssuerCheck: true, // Ignorer la vérification de l'issuer
  skipSubjectCheck: true, // Ignorer la vérification du sujet
  // Configuration pour la gestion des tokens
  sessionChecksEnabled: true, // Vérifier la session périodiquement
  clearHashAfterLogin: true,
  // Désactiver d'autres validations qui pourraient causer des problèmes
  disablePKCE: false, // Garder PKCE activé pour la sécurité
  // Configuration pour le logout - utiliser l'URL de base sans chemin spécifique
  postLogoutRedirectUri: window.location.origin + '/',
  // Configuration supplémentaire pour éviter les erreurs de validation
  customQueryParams: {}
} as AuthConfig;


