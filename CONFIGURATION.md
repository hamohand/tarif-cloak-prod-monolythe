# Configuration de l'Application

## 📋 Vue d'Ensemble

Ce document décrit toutes les configurations nécessaires pour déployer et faire fonctionner l'application.

## 🔧 Variables d'Environnement

### Instructions

1. Créez un fichier `.env` à la racine du projet
2. Copiez les variables ci-dessous et adaptez les valeurs à votre environnement
3. **IMPORTANT** : Ne commitez jamais le fichier `.env` dans Git (il est déjà dans `.gitignore`)

### Configuration Générale

```env
# Nom du projet (utilisé pour les noms de conteneurs et volumes)
PROJECT_NAME=app

# Nom du service frontend
FRONTEND_SERVICE_NAME=frontend
```

### Domaines

```env
# Domaines principaux pour le frontend et le backend
FRONTEND_DOMAIN=hscode.enclume-numerique.com
WWW_FRONTEND_DOMAIN=www.hscode.enclume-numerique.com

# Domaine pour Keycloak (authentification)
KEYCLOAK_DOMAIN=auth.hscode.enclume-numerique.com
KC_HOSTNAME=auth.hscode.enclume-numerique.com
```

### Traefik

```env
# Nom du certificate resolver Traefik pour les certificats Let's Encrypt
TRAEFIK_CERT_RESOLVER=myresolver
```

### Ports Internes

```env
FRONTEND_INTERNAL_PORT=80
BACKEND_INTERNAL_PORT=8081
KEYCLOAK_INTERNAL_PORT=8080
POSTGRES_PORT=5432
```

### PostgreSQL - Base de Données Application

```env
POSTGRES_DB=app-db
POSTGRES_USER=muhend
POSTGRES_PASSWORD=CHANGEZ_MOI
DB_SERVICE_NAME=app-db
POSTGRES_IMAGE_TAG=16
```

### PostgreSQL - Base de Données Keycloak

```env
KEYCLOAK_DB=keycloak
KEYCLOAK_DB_USER=keycloak
KEYCLOAK_DB_PASSWORD=CHANGEZ_MOI
DB_KEYCLOAK_SERVICE_NAME=keycloak-db
```

### Keycloak

```env
# Nom du realm Keycloak
KEYCLOAK_REALM=hscode-realm

# Identifiants administrateur Keycloak
KEYCLOAK_ADMIN_USER=admin
KEYCLOAK_ADMIN_PASSWORD=CHANGEZ_MOI

# Configuration des clients Keycloak
KEYCLOAK_FRONTEND_CLIENT=frontend-client
KEYCLOAK_BACKEND_CLIENT=backend-client
KEYCLOAK_BACKEND_CLIENT_SECRET=CHANGEZ_MOI

# URLs Keycloak
KEYCLOAK_INTERNAL_URL=http://keycloak:8080
KEYCLOAK_EXTERNAL_URL=https://auth.hscode.enclume-numerique.com
KEYCLOAK_IMAGE_TAG=22.0.1

# Timeouts et Retry
KEYCLOAK_CONNECTION_TIMEOUT=10000
KEYCLOAK_READ_TIMEOUT=10000
KEYCLOAK_RETRY_MAX_ATTEMPTS=5
KEYCLOAK_RETRY_WAIT_DURATION=10s
```

### Keycloak - Cache des Thèmes

```env
# Durée de cache pour les fichiers statiques des thèmes (en secondes)
# Recommandé : 2592000 (30 jours) pour la production
KC_THEME_STATIC_MAX_AGE=2592000

# Active le cache des thèmes compilés (true/false)
# Recommandé : true pour la production, false pour le développement
KC_THEME_CACHE_THEMES=true

# Active le cache des templates FreeMarker (true/false)
# Recommandé : true pour la production, false pour le développement
KC_THEME_CACHE_TEMPLATES=true
```

### CORS

```env
# Origines CORS autorisées (séparées par des virgules)
CORS_ALLOWED_ORIGINS=https://hscode.enclume-numerique.com,https://www.hscode.enclume-numerique.com,http://localhost:4200
```

### SMTP (Email)

```env
# Serveur SMTP
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587

# Identifiants SMTP
SMTP_USERNAME=votre_email@gmail.com
SMTP_PASSWORD=votre_mot_de_passe_application

# Configuration SMTP
SMTP_AUTH=true
SMTP_STARTTLS=true
SMTP_STARTTLS_REQUIRED=true
SMTP_CONNECTION_TIMEOUT=5000
SMTP_TIMEOUT=5000
SMTP_WRITE_TIMEOUT=5000

# Expéditeur par défaut
SMTP_FROM=noreply@enclume-numerique.com
SMTP_FROM_NAME=Enclume Numérique

# URL du frontend (pour les liens dans les emails)
FRONTEND_URL=https://hscode.enclume-numerique.com

# Email de l'administrateur pour les notifications
EMAIL_ADMIN_HSCODE=med@forge_numerique.com
```

**Note** : Pour Gmail, utilisez un "Mot de passe d'application" au lieu de votre mot de passe habituel.

### Spring Boot

```env
# Profil Spring Boot (dev ou prod)
SPRING_PROFILES_ACTIVE=prod
```

### Services IA

```env
# OpenAI
OPENAI_API_KEY=CHANGEZ_MOI
OPENAI_MODEL=gpt-4

# Anthropic (Claude)
ANTHROPIC_API_KEY=CHANGEZ_MOI
ANTHROPIC_MODEL=claude-3-sonnet-20240229

# Ollama
OLLAMA_API_KEY=CHANGEZ_MOI
OLLAMA_BASE_URL=http://host.docker.internal:11434
```

### Tarification

```env
# Tarif de base par requête (dans la devise du marché sélectionné)
BASE_REQUEST_PRICE=0.01
```

### Frontend

```env
# URL de l'API backend (utilisée par le frontend)
# En production : /api
# En développement : http://localhost:8081/api
API_URL=/api
```

### Inscription

```env
# Délai d'expiration du token de confirmation d'inscription (en heures)
REGISTRATION_TOKEN_EXPIRATION_HOURS=24
```

## 🎨 Configuration du Thème Keycloak

### Structure des Thèmes

Les thèmes personnalisés sont situés dans `keycloak/themes/` :

```
keycloak/themes/
├── README.md
└── custom-theme/
    ├── theme.properties
    ├── login/
    │   ├── theme.properties
    │   ├── login.ftl
    │   ├── login.css
    │   └── resources/
    │       ├── css/
    │       │   └── custom-login.css
    │       └── img/
    └── account/
```

### Activation du Thème

1. **Redémarrer Keycloak** pour charger le nouveau thème :
   ```bash
   docker compose restart keycloak
   ```

2. **Configurer le thème dans Keycloak Admin Console** :
   - Accéder à `https://auth.hscode.enclume-numerique.com`
   - Se connecter avec les identifiants admin
   - Aller dans **Realm Settings** → **Themes**
   - Sélectionner `custom-theme` pour **Login theme** et **Account theme**
   - Cliquer sur **Save**

3. **Vider le cache du navigateur** si nécessaire

### Variables de Cache

Pour le développement, désactivez le cache des thèmes :

```env
KC_THEME_CACHE_THEMES=false
KC_THEME_CACHE_TEMPLATES=false
KC_THEME_STATIC_MAX_AGE=3600
```

Pour la production, activez le cache :

```env
KC_THEME_CACHE_THEMES=true
KC_THEME_CACHE_TEMPLATES=true
KC_THEME_STATIC_MAX_AGE=2592000
```

## 🔐 Configuration de Sécurité

### Mots de Passe à Changer

⚠️ **IMPORTANT** : En production, changez obligatoirement :

- `POSTGRES_PASSWORD`
- `KEYCLOAK_ADMIN_PASSWORD`
- `KEYCLOAK_BACKEND_CLIENT_SECRET`
- `KEYCLOAK_DB_PASSWORD`

### Génération du Secret Backend Client

1. Connectez-vous à Keycloak Admin Console
2. Allez dans **Clients** → **backend-client**
3. Onglet **Credentials**
4. Copiez le **Secret** et mettez-le dans `.env` comme `KEYCLOAK_BACKEND_CLIENT_SECRET`

### Configuration des Rôles Keycloak

Voir `KEYCLOAK_ROLES_SETUP.md` pour la configuration détaillée des rôles.

## 📝 Exemple de Fichier .env Complet

```env
# ===============================================================
# CONFIGURATION GÉNÉRALE
# ===============================================================
PROJECT_NAME=app
FRONTEND_SERVICE_NAME=frontend

# ===============================================================
# DOMAINES
# ===============================================================
FRONTEND_DOMAIN=hscode.enclume-numerique.com
WWW_FRONTEND_DOMAIN=www.hscode.enclume-numerique.com
KEYCLOAK_DOMAIN=auth.hscode.enclume-numerique.com
KC_HOSTNAME=auth.hscode.enclume-numerique.com

# ===============================================================
# TRAEFIK
# ===============================================================
TRAEFIK_CERT_RESOLVER=myresolver

# ===============================================================
# PORTS
# ===============================================================
FRONTEND_INTERNAL_PORT=80
BACKEND_INTERNAL_PORT=8081
KEYCLOAK_INTERNAL_PORT=8080
POSTGRES_PORT=5432

# ===============================================================
# POSTGRESQL - APPLICATION
# ===============================================================
POSTGRES_DB=app-db
POSTGRES_USER=muhend
POSTGRES_PASSWORD=votre_mot_de_passe_securise
DB_SERVICE_NAME=app-db
POSTGRES_IMAGE_TAG=16

# ===============================================================
# POSTGRESQL - KEYCLOAK
# ===============================================================
KEYCLOAK_DB=keycloak
KEYCLOAK_DB_USER=keycloak
KEYCLOAK_DB_PASSWORD=votre_mot_de_passe_keycloak
DB_KEYCLOAK_SERVICE_NAME=keycloak-db

# ===============================================================
# KEYCLOAK
# ===============================================================
KEYCLOAK_REALM=hscode-realm
KEYCLOAK_ADMIN_USER=admin
KEYCLOAK_ADMIN_PASSWORD=votre_admin_password
KEYCLOAK_FRONTEND_CLIENT=frontend-client
KEYCLOAK_BACKEND_CLIENT=backend-client
KEYCLOAK_BACKEND_CLIENT_SECRET=votre_client_secret
KEYCLOAK_INTERNAL_URL=http://keycloak:8080
KEYCLOAK_EXTERNAL_URL=https://auth.hscode.enclume-numerique.com
KEYCLOAK_IMAGE_TAG=22.0.1
KEYCLOAK_CONNECTION_TIMEOUT=10000
KEYCLOAK_READ_TIMEOUT=10000
KEYCLOAK_RETRY_MAX_ATTEMPTS=5
KEYCLOAK_RETRY_WAIT_DURATION=10s

# ===============================================================
# KEYCLOAK - CACHE DES THÈMES
# ===============================================================
KC_THEME_STATIC_MAX_AGE=2592000
KC_THEME_CACHE_THEMES=true
KC_THEME_CACHE_TEMPLATES=true

# ===============================================================
# CORS
# ===============================================================
CORS_ALLOWED_ORIGINS=https://hscode.enclume-numerique.com,https://www.hscode.enclume-numerique.com,http://localhost:4200

# ===============================================================
# SMTP (EMAIL)
# ===============================================================
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=votre_email@gmail.com
SMTP_PASSWORD=votre_mot_de_passe_application
SMTP_AUTH=true
SMTP_STARTTLS=true
SMTP_STARTTLS_REQUIRED=true
SMTP_CONNECTION_TIMEOUT=5000
SMTP_TIMEOUT=5000
SMTP_WRITE_TIMEOUT=5000
SMTP_FROM=noreply@enclume-numerique.com
SMTP_FROM_NAME=Enclume Numérique
FRONTEND_URL=https://hscode.enclume-numerique.com
EMAIL_ADMIN_HSCODE=med@forge_numerique.com

# ===============================================================
# SPRING BOOT
# ===============================================================
SPRING_PROFILES_ACTIVE=prod

# ===============================================================
# SERVICES IA
# ===============================================================
OPENAI_API_KEY=votre_cle_openai
OPENAI_MODEL=gpt-4
ANTHROPIC_API_KEY=votre_cle_anthropic
ANTHROPIC_MODEL=claude-3-sonnet-20240229
OLLAMA_API_KEY=votre_cle_ollama
OLLAMA_BASE_URL=http://host.docker.internal:11434

# ===============================================================
# TARIFICATION
# ===============================================================
BASE_REQUEST_PRICE=0.01

# ===============================================================
# FRONTEND
# ===============================================================
API_URL=/api

# ===============================================================
# INSCRIPTION
# ===============================================================
REGISTRATION_TOKEN_EXPIRATION_HOURS=24
```

## ⚠️ Notes Importantes

1. **Sécurité** : Remplacez toutes les valeurs `CHANGEZ_MOI` par des valeurs sécurisées
2. **Domaines** : Les domaines doivent pointer vers votre serveur et être configurés dans votre DNS
3. **Certificats SSL** : Assurez-vous que Traefik est configuré pour générer les certificats Let's Encrypt
4. **Développement local** : Pour le développement, certaines valeurs peuvent être différentes (localhost, ports, etc.)
5. **Fichier .env** : Ne commitez JAMAIS le fichier `.env` dans Git

## 🔍 Où sont utilisées ces variables ?

- **docker-compose-prod.yml** : Configuration des services Docker
- **backend/src/main/resources/application.yml** : Configuration Spring Boot
- **frontend/generate-env.js** : Génération de `environment.prod.ts`
- **backend/src/main/java/com/muhend/backend/config/SecurityConfig.java** : Configuration CORS

---

*Dernière mise à jour : Configuration complète*

