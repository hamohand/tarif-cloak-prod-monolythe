import { Environment } from './environment.interface';

export const environment: Environment = {
  production: false,
  keycloak: {
    issuer: 'http://localhost:8080/realms/hscode-realm',
    realm: 'hscode-realm',
    clientId: 'frontend-client',
    redirectUri: 'http://localhost:4200/'
  },
  apiUrl: 'http://localhost:8081/api',
  marketVersion: 'DEFAULT' // DEFAULT, DZ, etc.
};

