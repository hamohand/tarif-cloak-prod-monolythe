import { Environment } from './environment.interface';

export const environment: Environment = {
  production: true,
  keycloak: {
    issuer: 'https://auth.hscode.enclume-numerique.com/realms/hscode-realm',
    realm: 'hscode-realm',
    clientId: 'frontend-client',
    redirectUri: window.location.origin + '/'
  },
  apiUrl: '/api',
  marketVersion: 'DZ' // DEFAULT, DZ, etc. - Configuré pour la version DZ
} as Environment;
