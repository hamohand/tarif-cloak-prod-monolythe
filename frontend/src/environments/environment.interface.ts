export interface Environment {
  production: boolean;
  keycloak: {
    issuer: string;
    realm: string;
    clientId: string;
    redirectUri: string;
  };
  apiUrl: string;
  marketVersion: string; // DEFAULT, DZ, etc.
}

