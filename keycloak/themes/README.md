# Thème Personnalisé Keycloak

Ce dossier contient le thème personnalisé `custom-theme` pour Keycloak.

## Structure

```
themes/
└── custom-theme/
    ├── theme.properties          # Configuration principale du thème
    ├── login/                    # Thème de connexion
    │   ├── theme.properties      # Configuration du thème de connexion
    │   ├── login.ftl             # Template FreeMarker pour le formulaire de connexion
    │   ├── login.css             # Styles CSS pour le formulaire de connexion
    │   └── resources/            # Ressources statiques (images, CSS additionnels)
    │       ├── css/
    │       └── img/
    └── account/                  # Thème de compte (optionnel)
```

## Activation du thème

1. Redémarrer le conteneur Keycloak :
   ```bash
   docker-compose -f docker-compose-prod.yml restart keycloak
   ```

2. Accéder à l'administration Keycloak :
   - URL : `https://auth.hscode.enclume-numerique.com`
   - Se connecter avec les identifiants admin

3. Configurer le thème :
   - Aller dans **Realm Settings** → **Themes**
   - Sélectionner `custom-theme` dans **Login theme**
   - Optionnel : Sélectionner `custom-theme` dans **Account theme**
   - Cliquer sur **Save**

## Personnalisation

### Modifier les couleurs

Les couleurs principales sont définies dans `login/login.css` :
- Couleur principale : `#1e3c72` (bleu foncé)
- Couleur secondaire : `#2a5298` (bleu moyen)
- Couleur d'accent : `#3498db` (bleu clair)

### Ajouter un logo

1. Placer votre logo dans `login/resources/img/logo.png`
2. Modifier `login/login.ftl` pour ajouter :
   ```html
   <img src="${url.resourcesPath}/img/logo.png" alt="Logo" class="custom-logo" />
   ```
3. Ajouter les styles dans `login/login.css` :
   ```css
   .custom-logo {
       max-width: 200px;
       margin-bottom: 1rem;
   }
   ```

### Modifier le texte

Modifier le fichier `login/login.ftl` pour changer les textes affichés.

## Notes

- Après chaque modification, redémarrer Keycloak pour voir les changements
- Vider le cache du navigateur si les modifications ne s'affichent pas
- Les fichiers `.ftl` utilisent la syntaxe FreeMarker de Keycloak

