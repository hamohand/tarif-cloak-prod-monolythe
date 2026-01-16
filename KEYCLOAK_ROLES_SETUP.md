# Configuration des rôles ADMIN et USER dans Keycloak

Ce document explique comment créer et configurer les rôles ADMIN et USER dans Keycloak pour votre application.

## 1. Créer les rôles dans Keycloak

### Via l'interface Keycloak

1. **Connectez-vous à Keycloak Admin Console**
   - URL : `https://auth.hscode.enclume-numerique.com`
   - Connectez-vous avec un compte administrateur

2. **Sélectionnez le realm `hscode-realm`**

3. **Créer les rôles au niveau du Realm**
   - Allez dans **Realm roles** (menu de gauche)
   - Cliquez sur **Create role**
   - Créez deux rôles :
     - **ADMIN** : Rôle pour les administrateurs
     - **USER** : Rôle pour les utilisateurs standards

### Via l'API Keycloak (optionnel)

Vous pouvez aussi créer les rôles via l'API Keycloak Admin Client si vous préférez.

## 2. Assigner les rôles aux utilisateurs

### Via l'interface Keycloak

1. **Allez dans Users** (menu de gauche)
2. **Sélectionnez un utilisateur**
3. **Onglet "Role mapping"**
4. **Cliquez sur "Assign role"**
5. **Sélectionnez "Filter by realm roles"**
6. **Cochez ADMIN ou USER** selon le rôle à assigner
7. **Cliquez sur "Assign"**

### Assigner le rôle ADMIN à un utilisateur

- Pour les administrateurs : assignez le rôle **ADMIN**
- Pour les utilisateurs standards : assignez le rôle **USER**

## 3. Configurer le client frontend-client

### Activer les rôles dans le token

1. **Allez dans Clients** (menu de gauche)
2. **Sélectionnez `frontend-client`**
3. **Onglet "Mappers"**
4. **Vérifiez qu'il existe un mapper pour les rôles du realm**

Si le mapper n'existe pas, créez-en un :

1. **Cliquez sur "Create"**
2. **Sélectionnez "By configuration" → "Realm roles"**
3. **Configurez le mapper :**
   - **Name** : `realm roles`
   - **Token Claim Name** : `realm_access.roles`
   - **Add to ID token** : ON
   - **Add to access token** : ON
   - **Add to userinfo** : ON
4. **Cliquez sur "Save"**

### Activer les rôles du client (optionnel)

Si vous voulez aussi les rôles au niveau du client :

1. **Allez dans Clients → `frontend-client`**
2. **Onglet "Roles"**
3. **Créez les rôles ADMIN et USER au niveau du client** (si nécessaire)
4. **Créez un mapper "Client roles"** pour les inclure dans le token

## 4. Vérifier la configuration

### Tester avec un utilisateur ADMIN

1. **Connectez-vous avec un utilisateur ayant le rôle ADMIN**
2. **Appelez l'endpoint** : `GET /api/admin/endpoints`
3. **Vous devriez recevoir la liste des endpoints**

### Tester avec un utilisateur USER

1. **Connectez-vous avec un utilisateur ayant uniquement le rôle USER**
2. **Appelez l'endpoint** : `GET /api/admin/endpoints`
3. **Vous devriez recevoir une erreur 403 (Forbidden)**

## 5. Endpoints disponibles

### Endpoint protégé par ADMIN

- **GET `/api/admin/endpoints`** : Liste tous les endpoints du backend
  - Nécessite le rôle **ADMIN**
  - Retourne une liste JSON avec tous les endpoints, leurs méthodes HTTP, paramètres, etc.

### Endpoints protégés par USER

Les endpoints suivants sont accessibles aux utilisateurs avec le rôle **USER** ou **ADMIN** :
- **`/api/recherche/**`** : Endpoints de recherche (sections, chapitres, positions4, positions6)
  - Nécessite le rôle **USER** ou **ADMIN**

### Endpoints protégés par authentification (par défaut)

Tous les autres endpoints nécessitent une authentification (rôle USER ou ADMIN) :
- `/api/sections`
- `/api/chapitres`
- `/api/positions6dz`
- etc.

## 6. Structure de la réponse de `/api/admin/endpoints`

```json
{
  "total": 15,
  "endpoints": [
    {
      "method": "GET",
      "path": "/sections",
      "controller": "SectionController",
      "methodName": "getAllSections",
      "parameters": [],
      "returnType": "Iterable"
    },
    {
      "method": "GET",
      "path": "/admin/endpoints",
      "controller": "AdminController",
      "methodName": "listEndpoints",
      "parameters": [],
      "returnType": "ResponseEntity",
      "requiredRole": "hasRole('ADMIN')"
    }
  ]
}
```

## Notes importantes

- Les rôles sont extraits du JWT token Keycloak
- Les rôles peuvent être au niveau du **realm** ou du **client**
- Spring Security convertit automatiquement les rôles en `ROLE_ADMIN` et `ROLE_USER`
- L'annotation `@PreAuthorize("hasRole('ADMIN')")` vérifie que l'utilisateur a le rôle ADMIN

