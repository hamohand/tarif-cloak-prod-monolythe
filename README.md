# SaaS Qwen - Application Full-Stack avec Authentification Keycloak

Une application SaaS moderne construite avec Angular, Spring Boot, Keycloak et PostgreSQL, entièrement conteneurisée avec Docker.

## 📋 Table des Matières

- [Fonctionnalités](#-fonctionnalités)
- [Architecture](#-architecture)
- [Technologies](#-technologies)
- [Prérequis](#-prérequis)
- [Installation](#-installation)
- [Configuration](#-configuration)
- [Utilisation](#-utilisation)
- [Développement](#-développement)
- [Déploiement](#-déploiement)
- [Documentation](#-documentation)
- [Troubleshooting](#-troubleshooting)

## 🚀 Fonctionnalités

- ✅ **Authentification complète** avec Keycloak (OAuth 2.0 / OpenID Connect)
- ✅ **Gestion des utilisateurs** : Inscription, connexion, profils
- ✅ **Gestion multi-organisations** : Création et gestion d'organisations avec utilisateurs
- ✅ **Plans tarifaires** : Système de facturation complet avec quotas et essai gratuit
  - Plans mensuels avec cycles personnalisés et reconduction tacite automatique
  - Plans Pay-per-Request avec facturation à la requête
  - Affichage de la date de renouvellement automatique pour les plans mensuels
  - Gestion des changements de plan en attente
  - Factures de clôture lors des changements de plan
- ✅ **Recherche de codes HS-code** : Recherche intelligente avec IA (OpenAI, Anthropic, Ollama)
- ✅ **Tracking d'utilisation** : Enregistrement automatique des requêtes et coûts
- ✅ **API REST sécurisée** avec Spring Boot et JWT
- ✅ **Interface moderne** avec Angular 20
- ✅ **Base de données PostgreSQL** persistante
- ✅ **Architecture microservices** entièrement conteneurisée
- ✅ **Configuration centralisée** via variables d'environnement

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      CLIENT (Browser)                        │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                    Traefik (Reverse Proxy)                    │
└────────────┬───────────────────────┬────────────────────────┘
             │                       │
             ▼                       ▼
┌─────────────────────┐   ┌─────────────────────┐
│   Frontend          │   │   Backend           │
│   Angular + Nginx   │   │   Spring Boot       │
│   :4200             │   │   :8081             │
└─────────────────────┘   └───────┬─────────────┘
                                   │
                    ┌─────────────┼─────────────┐
                    ▼             ▼             ▼
        ┌──────────────┐  ┌──────────────┐  ┌──────────────┐
        │  Keycloak    │  │  PostgreSQL  │  │  PostgreSQL  │
        │  (Auth)      │  │  (App DB)    │  │ (Keycloak DB)│
        │  :8080       │  │  :5432       │  │  :5432       │
        └──────────────┘  └──────────────┘  └──────────────┘
```

Pour plus de détails sur l'architecture, consultez [ARCHITECTURE.md](ARCHITECTURE.md).

## 💻 Technologies

### Frontend
- **Angular** 20.3.0
- **TypeScript** 5.9.2
- **angular-oauth2-oidc** 17.0.0
- **RxJS** 7.8.0
- **Karma/Jasmine** pour les tests

### Backend
- **Spring Boot** 3.5.6
- **Java** 21
- **Spring Security OAuth2** Resource Server
- **Spring Data JPA** avec Hibernate
- **Keycloak Admin Client** 26.0.7
- **PostgreSQL** Driver
- **Lombok** pour réduire le boilerplate
- **SpringDoc OpenAPI** 2.8.13 pour la documentation API

### Infrastructure
- **Docker** & Docker Compose
- **Keycloak** 22.0.1
- **PostgreSQL** 16
- **Nginx** (pour servir le frontend)
- **Traefik** (reverse proxy en production)

## 📦 Prérequis

- **Docker** version 20.10 ou supérieure
- **Docker Compose** version 2.0 ou supérieure
- **Git**
- (Optionnel) **Node.js** 18+ et **npm** pour le développement local du frontend
- (Optionnel) **JDK** 21+ et **Maven** pour le développement local du backend

## 🛠️ Installation

### 1. Cloner le Projet

```bash
git clone https://github.com/votre-username/saas-qwen.git
cd saas-qwen
```

### 2. Configuration Initiale

Créez le fichier `.env` à partir de l'exemple :

```bash
cp .env.example .env
```

Éditez `.env` et configurez au minimum :

```env
PROJECT_NAME=mon-projet
POSTGRES_PASSWORD=votre-mot-de-passe-securise
KEYCLOAK_ADMIN_PASSWORD=votre-admin-password
KEYCLOAK_BACKEND_CLIENT_SECRET=votre-client-secret
```

Pour la liste complète des variables d'environnement, consultez [CONFIGURATION.md](CONFIGURATION.md).

### 3. Démarrage des Services

```bash
# Première installation (avec construction des images)
docker compose up -d --build

# Vérifier que tous les services sont démarrés
docker compose ps

# Suivre les logs
docker compose logs -f
```

### 4. Accéder à l'Application

- **Frontend** : http://localhost:4200
- **Backend API** : http://localhost:8081
- **Keycloak Admin** : http://localhost:8080 (admin / admin par défaut)
- **API Documentation** : http://localhost:8081/swagger-ui.html

## ⚙️ Configuration

Toutes les variables de configuration sont centralisées dans le fichier `.env`. 

Consultez [CONFIGURATION.md](CONFIGURATION.md) pour :
- La liste complète des variables d'environnement
- La configuration du thème Keycloak
- Les paramètres de sécurité
- Les exemples de configuration

## 🎯 Utilisation

### Inscription d'un Utilisateur

1. Accédez à http://localhost:4200
2. Cliquez sur "S'inscrire"
3. Remplissez le formulaire
4. L'utilisateur est créé dans Keycloak

### Connexion

1. Cliquez sur "Se connecter"
2. Vous serez redirigé vers Keycloak
3. Entrez vos identifiants
4. Vous serez redirigé vers l'application

### API Backend

L'API backend est documentée avec OpenAPI :

```bash
# Accéder à la documentation interactive
open http://localhost:8081/swagger-ui.html
```

Exemple d'appel API :

```bash
# S'inscrire (endpoint public)
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john",
    "email": "john@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "password": "password123"
  }'
```

## 👨‍💻 Développement

### Structure du Projet

```
saas-qwen/
├── backend/               # API Spring Boot
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/muhend/backend/
│   │   │   │       ├── config/      # Configuration Spring & Keycloak
│   │   │   │       ├── controller/  # Contrôleurs REST
│   │   │   │       ├── dto/         # Data Transfer Objects
│   │   │   │       ├── models/      # Entités JPA
│   │   │   │       └── service/     # Services métier
│   │   │   └── resources/
│   │   │       └── application.yml
│   │   └── test/
│   ├── Dockerfile
│   └── pom.xml
├── frontend/              # Application Angular
│   ├── src/
│   │   ├── app/
│   │   │   ├── core/      # Services, guards, interceptors
│   │   │   ├── features/  # Modules fonctionnels
│   │   │   └── shared/    # Composants partagés
│   │   └── environments/
│   ├── Dockerfile
│   ├── angular.json
│   └── package.json
├── keycloak/              # Configuration Keycloak
│   └── realm-export.json
├── docker-compose.yml     # Orchestration des services
├── .env                   # Variables d'environnement (ne pas commiter)
├── README.md
├── ARCHITECTURE.md        # Documentation de l'architecture
└── CONFIGURATION.md       # Documentation de la configuration
```

### Développement Local du Frontend

```bash
cd frontend
npm install
npm start

# L'application sera disponible sur http://localhost:4200
# Le hot-reload est activé
```

### Développement Local du Backend

```bash
cd backend
./mvnw spring-boot:run

# L'API sera disponible sur http://localhost:8081
# Assurez-vous que PostgreSQL et Keycloak sont démarrés
```

### Tests

```bash
# Tests backend
cd backend
./mvnw test

# Tests frontend
cd frontend
npm test

# Tests e2e frontend
npm run e2e
```

## 🚢 Déploiement

### Production avec Docker Compose

1. Modifiez `.env` pour la production :

```env
SPRING_PROFILES_ACTIVE=prod
KEYCLOAK_HOSTNAME=votre-domaine.com
# Changez tous les mots de passe
```

2. Utilisez HTTPS (configurez un reverse proxy comme Traefik ou Nginx)

3. Démarrez les services :

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

### Variables de Production à Changer

⚠️ **Important** : En production, changez obligatoirement :

- `POSTGRES_PASSWORD`
- `KEYCLOAK_ADMIN_PASSWORD`
- `KEYCLOAK_BACKEND_CLIENT_SECRET`

Générez un nouveau secret pour `backend-client` dans Keycloak.

### Backup de la Base de Données

```bash
# Backup
docker exec saasessai2-db pg_dump -U muhend saasessai2-db > backup.sql

# Restauration
docker exec -i saasessai2-db psql -U muhend saasessai2-db < backup.sql
```

## 📚 Documentation

- **[ARCHITECTURE.md](ARCHITECTURE.md)** : Architecture complète de l'application, modèle de données, flux principaux
- **[CONFIGURATION.md](CONFIGURATION.md)** : Configuration complète, variables d'environnement, thème Keycloak
- **[docs/PLAN_FACTURATION.md](docs/PLAN_FACTURATION.md)** : Système de facturation complet, types de plans tarifaires, règles de changement de plan, cycles mensuels
- **[docs/MARKET_PROFILE.md](docs/MARKET_PROFILE.md)** : Gestion des profils de marché (devises, langues, fuseaux horaires)

## 🔧 Troubleshooting

### Le backend ne démarre pas

```bash
# Vérifier les logs
docker compose logs backend

# Vérifier que Keycloak est démarré
docker compose ps keycloak

# Redémarrer le backend
docker compose restart backend
```

### Erreur 403 lors de l'inscription

Le service account `backend-client` n'a pas les bons rôles :

1. Connectez-vous à Keycloak Admin Console
2. Sélectionnez le realm `hscode-realm`
3. **Clients** → `backend-client` → **Service Account Roles**
4. Ajoutez les rôles : `manage-users`, `view-users`, `query-users`

### Réinitialisation Complète

```bash
# Arrêter et supprimer tous les conteneurs et volumes
docker compose down --volumes --remove-orphans

# Redémarrer
docker compose up -d --build
```

### Voir les Logs en Temps Réel

```bash
# Tous les services
docker compose logs -f

# Un service spécifique
docker compose logs -f backend
docker compose logs -f keycloak
```

## 🤝 Contribution

Les contributions sont les bienvenues ! Veuillez suivre ces étapes :

1. Forkez le projet
2. Créez une branche pour votre fonctionnalité (`git checkout -b feature/AmazingFeature`)
3. Committez vos changements (`git commit -m 'Add some AmazingFeature'`)
4. Poussez vers la branche (`git push origin feature/AmazingFeature`)
5. Ouvrez une Pull Request

### Standards de Code

- **Backend** : Suivez les conventions Java et Spring Boot
- **Frontend** : Suivez le style guide Angular officiel
- **Git** : Utilisez des messages de commit conventionnels

## 📄 Licence

Ce projet est sous licence MIT. Voir le fichier LICENSE pour plus de détails.

## 👥 Auteurs

Muhend - Développeur principal

## 🙏 Remerciements

- Spring Boot
- Angular
- Keycloak
- PostgreSQL
#   t a r i f - c l o a k - p r o d - m o n o l y t h e  
 