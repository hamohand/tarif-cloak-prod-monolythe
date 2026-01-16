# Profil de Marché (Market Profile)

## 📋 Vue d'ensemble

Le profil de marché (`market_profile`) est une entité qui contient toutes les informations nécessaires pour caractériser un pays ou une région dans l'application. Il centralise les données géographiques, monétaires, linguistiques et culturelles d'un marché.

---

## 🗂️ Structure de l'entité

### Champs principaux

| Champ | Type | Description | Exemple |
|-------|------|-------------|---------|
| `market_version` | VARCHAR(10) | Version du marché (unique) | `DEFAULT`, `DZ`, `FR` |
| `country_code_iso_alpha2` | VARCHAR(2) | Code ISO 3166-1 alpha-2 (unique) | `FR`, `DZ`, `US` |
| `country_code_iso_alpha3` | VARCHAR(3) | Code ISO 3166-1 alpha-3 | `FRA`, `DZA`, `USA` |
| `country_name` | VARCHAR(100) | Nom du pays | `France`, `Algérie` |
| `country_name_native` | VARCHAR(100) | Nom du pays dans sa langue native | `الجزائر` |
| `phone_prefix` | VARCHAR(10) | Indicatif téléphonique international | `+33`, `+213` |
| `currency_code` | VARCHAR(3) | Code devise ISO 4217 | `EUR`, `DZD`, `USD` |
| `currency_symbol` | VARCHAR(10) | Symbole de la devise | `€`, `DA`, `$` |
| `timezone` | VARCHAR(50) | Fuseau horaire IANA | `Europe/Paris`, `Africa/Algiers` |
| `locale` | VARCHAR(10) | Locale (langue + pays) | `fr_FR`, `ar_DZ` |
| `language_code` | VARCHAR(5) | Code langue ISO 639-1 | `fr`, `ar`, `en` |
| `is_active` | BOOLEAN | Actif/inactif | `true`, `false` |
| `display_order` | INTEGER | Ordre d'affichage | `1`, `2`, `3` |
| `description` | VARCHAR(500) | Description du marché | Texte libre |

---

## 📁 Structure du code

### Backend

```
backend/src/main/java/com/muhend/backend/market/
├── model/
│   └── MarketProfile.java          # Entité JPA
├── dto/
│   ├── MarketProfileDto.java       # DTO pour les réponses
│   ├── CreateMarketProfileRequest.java  # DTO pour la création
│   └── UpdateMarketProfileRequest.java  # DTO pour la mise à jour
├── repository/
│   └── MarketProfileRepository.java     # Repository JPA
├── service/
│   └── MarketProfileService.java        # Service métier
└── controller/
    └── MarketProfileController.java     # Controller REST
```

### Migrations Flyway

```
backend/src/main/resources/db/migration/
├── V11__create_market_profile_table.sql      # Création de la table
└── V12__insert_default_market_profiles.sql  # Données initiales (DEFAULT, DZ)
```

---

## 🔌 Endpoints API

### Endpoints publics (lecture seule)

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/market-profiles` | Liste tous les profils actifs |
| `GET` | `/market-profiles/{id}` | Détails d'un profil par ID |
| `GET` | `/market-profiles/version/{marketVersion}` | Détails par version (ex: `DEFAULT`, `DZ`) |
| `GET` | `/market-profiles/country/{countryCode}` | Détails par code ISO alpha-2 (ex: `FR`, `DZ`) |

### Endpoints ADMIN (modification)

| Méthode | Endpoint | Description | Authentification |
|---------|----------|-------------|------------------|
| `GET` | `/market-profiles/all` | Liste tous les profils (actifs + inactifs) | ADMIN |
| `POST` | `/market-profiles` | Créer un nouveau profil | ADMIN |
| `PUT` | `/market-profiles/{id}` | Mettre à jour un profil | ADMIN |
| `DELETE` | `/market-profiles/{id}` | Supprimer un profil | ADMIN |

---

## 📊 Exemples de données

### Profil DEFAULT (France)

```json
{
  "id": 1,
  "marketVersion": "DEFAULT",
  "countryCodeIsoAlpha2": "FR",
  "countryCodeIsoAlpha3": "FRA",
  "countryName": "France",
  "countryNameNative": "France",
  "phonePrefix": "+33",
  "currencyCode": "EUR",
  "currencySymbol": "€",
  "timezone": "Europe/Paris",
  "locale": "fr_FR",
  "languageCode": "fr",
  "isActive": true,
  "displayOrder": 1,
  "description": "Profil par défaut pour le marché européen (France)"
}
```

### Profil DZ (Algérie)

```json
{
  "id": 2,
  "marketVersion": "DZ",
  "countryCodeIsoAlpha2": "DZ",
  "countryCodeIsoAlpha3": "DZA",
  "countryName": "Algérie",
  "countryNameNative": "الجزائر",
  "phonePrefix": "+213",
  "currencyCode": "DZD",
  "currencySymbol": "DA",
  "timezone": "Africa/Algiers",
  "locale": "ar_DZ",
  "languageCode": "ar",
  "isActive": true,
  "displayOrder": 2,
  "description": "Profil pour le marché algérien"
}
```

---

## 🔗 Relations avec d'autres entités

### PricingPlan

Le champ `market_version` dans `PricingPlan` fait référence à `MarketProfile.market_version` :

```sql
-- Les plans tarifaires sont filtrés par market_version
SELECT * FROM pricing_plan WHERE market_version = 'DZ';
```

**Utilisation** :
- Les plans tarifaires sont associés à un profil de marché
- Le profil de marché détermine la devise utilisée pour les prix
- Le profil de marché peut être utilisé pour filtrer les plans disponibles

---

## 🛠️ Utilisation

### Récupérer un profil de marché

```bash
# Par version
curl -X GET "https://hscode.enclume-numerique.com/api/market-profiles/version/DZ"

# Par code pays
curl -X GET "https://hscode.enclume-numerique.com/api/market-profiles/country/DZ"
```

### Créer un nouveau profil (ADMIN)

```bash
curl -X POST "https://hscode.enclume-numerique.com/api/market-profiles" \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "marketVersion": "US",
    "countryCodeIsoAlpha2": "US",
    "countryCodeIsoAlpha3": "USA",
    "countryName": "United States",
    "countryNameNative": "United States",
    "phonePrefix": "+1",
    "currencyCode": "USD",
    "currencySymbol": "$",
    "timezone": "America/New_York",
    "locale": "en_US",
    "languageCode": "en",
    "isActive": true,
    "displayOrder": 3,
    "description": "Profil pour le marché américain"
  }'
```

---

## ✅ Validations

### Création

- `marketVersion` : Obligatoire, unique, max 10 caractères
- `countryCodeIsoAlpha2` : Obligatoire, unique, exactement 2 caractères
- `countryName` : Obligatoire, max 100 caractères
- `phonePrefix` : Obligatoire, max 10 caractères
- `currencyCode` : Obligatoire, exactement 3 caractères
- `displayOrder` : Obligatoire

### Mise à jour

- Tous les champs sont optionnels (seuls les champs fournis seront mis à jour)
- Les validations de longueur s'appliquent si le champ est fourni
- Vérification d'unicité si `marketVersion` ou `countryCodeIsoAlpha2` est modifié

---

## 🔍 Recherche et filtrage

### Méthodes du Repository

- `findByMarketVersion(String)` : Trouve par version de marché
- `findByCountryCodeIsoAlpha2(String)` : Trouve par code ISO alpha-2
- `findByIsActiveTrueOrderByDisplayOrderAsc()` : Liste tous les profils actifs triés

### Index de performance

- `idx_market_profile_market_version` : Sur `market_version`
- `idx_market_profile_country_code` : Sur `country_code_iso_alpha2`
- `idx_market_profile_is_active` : Sur `is_active`
- `idx_market_profile_display_order` : Sur `display_order`

---

## 📝 Notes importantes

1. **Unicité** : `market_version` et `country_code_iso_alpha2` sont uniques
2. **Normalisation** : Les codes ISO sont automatiquement convertis en majuscules
3. **Activation** : Seuls les profils actifs sont retournés par défaut
4. **Ordre d'affichage** : Les profils sont triés par `display_order` pour l'affichage

---

## 🔄 Évolutions futures possibles

- [ ] Support de régions multi-pays (ex: UE, Maghreb)
- [ ] Gestion des langues multiples par pays
- [ ] Support des formats de date/heure spécifiques
- [ ] Intégration avec des APIs de données géographiques
- [ ] Cache des profils de marché pour améliorer les performances
- [ ] Support des fuseaux horaires multiples par pays

---

**Dernière mise à jour** : Janvier 2025

