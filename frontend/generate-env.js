const fs = require('fs');
const path = require('path');

console.log('🔧 Génération de la configuration d\'environnement Angular...\n');

// Récupération des variables d'environnement avec valeurs par défaut
const keycloakRealm = process.env.KEYCLOAK_REALM || 'hscode-realm';
const keycloakClient = process.env.KEYCLOAK_FRONTEND_CLIENT || 'frontend-client';
const keycloakUrl = process.env.KEYCLOAK_EXTERNAL_URL || 'http://localhost:8080';
const apiUrl = process.env.API_URL || '/api';
const marketVersion = process.env.MARKET_VERSION || 'DEFAULT';

// Affichage des valeurs pour debug
console.log('📋 Variables de configuration :');
console.log(`   - KEYCLOAK_REALM: ${keycloakRealm}`);
console.log(`   - KEYCLOAK_FRONTEND_CLIENT: ${keycloakClient}`);
console.log(`   - KEYCLOAK_EXTERNAL_URL: ${keycloakUrl}`);
console.log(`   - API_URL: ${apiUrl}`);
console.log(`   - MARKET_VERSION: ${marketVersion}\n`);

// Validation basique
if (!keycloakRealm || !keycloakClient || !keycloakUrl) {
  console.error('❌ Erreur : Variables Keycloak manquantes !');
  process.exit(1);
}

// Construction du fichier environment.prod.ts
const envConfig = `// ⚠️ FICHIER GÉNÉRÉ AUTOMATIQUEMENT - NE PAS MODIFIER MANUELLEMENT
// Généré le : ${new Date().toISOString()}
// Configuré via : generate-env.js

import { Environment } from './environment.interface';

export const environment: Environment = {
  production: true,
  keycloak: {
    issuer: '${keycloakUrl}/realms/${keycloakRealm}',
    realm: '${keycloakRealm}',
    clientId: '${keycloakClient}',
    redirectUri: window.location.origin + '/'
  },
  apiUrl: '${apiUrl}',
  marketVersion: '${marketVersion}' // DEFAULT, DZ, etc. - Configuré via MARKET_VERSION
} as Environment;
`;

try {
  // Chemin du fichier à créer
  const outputPath = path.join(__dirname, 'src', 'environments', 'environment.prod.ts');

  // Créer le répertoire si nécessaire
  const dir = path.dirname(outputPath);
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true });
    console.log(`📁 Création du répertoire : ${dir}`);
  }

  // Écriture du fichier
  fs.writeFileSync(outputPath, envConfig, 'utf8');

  console.log('✅ Fichier environment.prod.ts généré avec succès !');
  console.log(`📂 Emplacement : ${outputPath}\n`);

  // Affichage du contenu pour debug
  console.log('📄 Contenu du fichier :');
  console.log('─'.repeat(60));
  console.log(envConfig);
  console.log('─'.repeat(60));

} catch (error) {
  console.error('❌ Erreur lors de la génération du fichier :', error.message);
  process.exit(1);
}

console.log('\n🎉 Configuration terminée avec succès !');
