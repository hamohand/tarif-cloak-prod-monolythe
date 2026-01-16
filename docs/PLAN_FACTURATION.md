# Plan de Facturation - HS-Code API

## 📋 Vue d'ensemble

Ce document décrit le système de facturation complet pour l'application HS-Code, incluant les différents types de plans tarifaires et leurs règles de facturation.

**Politique de facturation mise à jour** : Février 2025

---

## 💰 Types de Plans Tarifaires

### 1. Plan Essai Gratuit

**Caractéristiques :**
- `trialPeriodDays` > 0 : Nombre de jours d'essai
- `pricePerMonth` : `null`
- `pricePerRequest` : `null`
- `monthlyQuota` : Quota de requêtes non facturées pendant la période d'essai

**Règles :**
- Une organisation ne peut utiliser le plan d'essai qu'**une seule fois**
- Une fois utilisé, le plan d'essai n'est plus disponible pour cette organisation
- À l'issue du plan d'essai, l'organisation doit choisir un plan payant

**Facturation :**
- Aucune facturation pendant la période d'essai
- Les requêtes sont comptabilisées mais non facturées

### 2. Plans Mensuels

**Caractéristiques :**
- `pricePerMonth` : Prix mensuel fixe (dans la devise du marché)
- `monthlyQuota` : Nombre de requêtes autorisées par cycle mensuel (null = illimité)
- `pricePerRequest` : `null`
- `currency` : Devise du marché (ex: EUR, DZD)

**Cycle mensuel :**
- Le cycle mensuel commence le jour J et se termine le jour J-1 du mois suivant (inclus)
- Exemple : Si le cycle commence le 15 janvier, il se termine le 14 février (inclus)
- Le quota est réinitialisé le 15 février (jour suivant la fin du cycle)

**Facturation :**
- **Aucune facturation par requête** pour les plans mensuels
- Facture mensuelle générée à la fin de chaque cycle pour le montant fixe du plan
- Reconduction tacite automatique à la fin de chaque cycle
- Les requêtes hors quota sont facturées au tarif du plan "Paiement à la requête"

### 3. Plans Pay-per-Request (Paiement à la requête)

**Caractéristiques :**
- `pricePerRequest` : Prix par requête (dans la devise du marché)
- `monthlyQuota` : `null` (quota illimité)
- `pricePerMonth` : `null`
- `currency` : Devise du marché

**Facturation :**
- Facturation à chaque requête
- Toutes les requêtes utilisées sont dues
- Facture de clôture générée lors du passage vers un plan mensuel (depuis la dernière facture jusqu'à la date de changement)

---

## 🔄 Règles de Changement de Plan

**Règle fondamentale** : Une organisation ne peut avoir qu'**un seul plan à la fois**.

### Cas 1 : Plan Essai Gratuit → Autre plan

- ✅ **Effet immédiat**
- Le plan d'essai ne peut être utilisé qu'une seule fois
- Si l'organisation a déjà utilisé l'essai, le passage vers un plan d'essai est refusé

### Cas 2 : Plan Mensuel → Plan Mensuel

- ⚠️ **Changement en attente** (prend effet à la fin du cycle en cours)
- Le changement ne prend effet qu'au terme du plan mensuel en cours
- Le nouveau plan est enregistré comme "en attente" (`pendingMonthlyPlanId`)
- Le changement peut être annulé avant la date d'effet
- À la fin du cycle, le scheduler applique automatiquement le changement :
  - Génère une facture de clôture pour l'ancien plan (cycle complet)
  - Applique le nouveau plan et initialise un nouveau cycle

### Cas 3 : Plan Mensuel → Pay-per-Request

**Deux scénarios possibles :**

#### 3a. Changement automatique et provisoire (quota dépassé)
- ⚠️ **Automatique et provisoire** : Si l'utilisateur dépasse son quota mensuel avant la fin du cycle, les requêtes supplémentaires sont facturées au tarif Pay-per-Request
- **Ce n'est PAS un changement de plan** : Le plan reste mensuel, seule la facturation change temporairement
- Le quota du plan mensuel sera réinitialisé au début du nouveau cycle
- Aucune facture de clôture n'est générée dans ce cas

#### 3b. Changement de plan demandé par l'utilisateur
- **Si le quota est déjà dépassé** : ✅ **Effet immédiat**
  - Génère une facture de clôture pour le cycle mensuel en cours
  - Le plan Pay-per-Request prend effet immédiatement
  
- **Si le quota n'est pas dépassé** : ⚠️ **Changement en attente**
  - Le changement est enregistré en attente (`pendingPayPerRequestPlanId`)
  - Le changement prendra effet :
    - **Immédiatement** si le quota est dépassé avant la fin du cycle
    - **À la fin du cycle mensuel** si le quota n'est pas dépassé
  - Le scheduler vérifie quotidiennement si le quota est dépassé pour appliquer le changement

### Cas 4 : Pay-per-Request → Plan Mensuel

- ✅ **Effet immédiat**
- Génère une facture de clôture Pay-per-Request depuis la dernière facture (`lastPayPerRequestInvoiceDate`) jusqu'à aujourd'hui
- Le plan mensuel prend effet immédiatement
- Initialise un nouveau cycle mensuel (du jour J au jour J-1 du mois suivant inclus)

### Cas 5 : Pay-per-Request → Pay-per-Request

- ✅ **Effet immédiat**
- Changement de tarif immédiat (tous les paramètres sont remplacés)

### Règle de remplacement

**Lors d'un changement de plan, TOUS les paramètres de l'ancien plan sont remplacés par ceux du nouveau plan** :
- `pricingPlanId`
- `monthlyQuota`
- `monthlyPlanStartDate` / `monthlyPlanEndDate` (pour plans mensuels)
- Tous les autres paramètres du plan

### Cas 1 : Deux plans mensuels

#### oldPlan.quota < newPlan.quota
- ✅ Changement possible immédiatement
- Nouveau quota = `newPlan.quota - requêtes_déjà_consommées_ce_mois`
- Exemple : 
  - Ancien plan : 300 requêtes/mois
  - Nouveau plan : 500 requêtes/mois
  - Requêtes consommées : 100
  - Nouveau quota : 500 - 100 = **400 requêtes**

#### oldPlan.quota > newPlan.quota
- ⚠️ Changement possible uniquement le 1er du mois
- Si changement le 1er : quota complet du nouveau plan
- Si changement après le 1er : exception levée

#### oldPlan.quota == newPlan.quota
- ✅ Changement autorisé, quota identique

### Cas 2 : Un plan Pay-per-Request impliqué

#### Passage d'un plan mensuel vers Pay-per-Request
- ✅ Changement possible immédiatement
- L'ancien plan mensuel est **entièrement dû** (pas de proratisation)
- Facture mensuelle complète générée pour l'ancien plan
- Pas de facture de démarrage pour Pay-per-Request

#### Passage de Pay-per-Request vers un plan mensuel
- ✅ Changement possible immédiatement
- Le nouveau plan mensuel est **entièrement dû** (pas de proratisation)
- Facture mensuelle complète générée pour le nouveau plan
- Pas de facture de clôture pour Pay-per-Request

### Cas 6 : Quota mensuel dépassé (plans mensuels)

**✅ Implémenté** : Lorsque le quota mensuel d'un plan mensuel est dépassé, les requêtes supplémentaires sont automatiquement facturées au prix du plan Pay-per-Request correspondant au marché de l'organisation.

- Le système recherche automatiquement le plan Pay-per-Request actif pour le marché de l'organisation
- Si un plan Pay-per-Request est trouvé, les requêtes supplémentaires sont facturées à ce prix (dans la devise du marché)
- Les requêtes facturées au prix Pay-per-Request sont enregistrées dans les logs d'utilisation avec le coût correspondant
- Le quota mensuel reste valable pour le cycle en cours, seules les requêtes hors quota sont facturées

---

## 📅 Calendrier de Facturation

### Factures Mensuelles (plans mensuels)

- **Période** : Cycle mensuel (du jour J au jour J-1 du mois suivant inclus)
- **Génération automatique** : À la fin de chaque cycle (par le scheduler quotidien)
- **Montant** : Prix mensuel fixe du plan (dans la devise du marché)
- **Échéance** : 30 jours après la fin du cycle
- **Reconduction** : Tacite et automatique à la fin de chaque cycle

### Factures Pay-per-Request

- **Facturation** : À chaque requête (coût enregistré dans les logs)
- **Facture de clôture** : Générée lors du passage vers un plan mensuel
- **Période de clôture** : Depuis la dernière facture (`lastPayPerRequestInvoiceDate`) jusqu'à la date de changement
- **Devise** : Devise du marché de l'organisation

---

## 🧮 Calcul des Coûts

### Plans Mensuels

**Facture mensuelle :**
```
Total facture = Prix mensuel fixe du plan (dans la devise du marché)
```

**Requêtes hors quota (facturées au tarif Pay-per-Request) :**
- Seules les requêtes dépassant le quota mensuel sont facturées
- Prix : Tarif du plan Pay-per-Request du marché (dans la devise du marché)

### Plans Pay-per-Request

**Coût par requête :**
```
Coût total = Prix par requête du plan (dans la devise du marché)
```

**Facture de clôture :**
```
Total facture = Σ (Coût de chaque requête depuis la dernière facture jusqu'à la date de changement)
```

### Plans Essai Gratuit

- Aucune facturation pendant la période d'essai
- Les requêtes sont comptabilisées mais non facturées

---

## 🔐 Sécurité et Validation

### Vérifications avant génération de facture

1. ✅ L'organisation existe
2. ✅ Aucune facture n'existe déjà pour la période
3. ✅ Au moins une requête a été effectuée pendant la période (pour Pay-per-Request)

### Gestion des erreurs

- Les erreurs lors de la génération de factures n'interrompent pas le processus
- Les erreurs sont loggées mais n'empêchent pas la génération des autres factures
- Les factures déjà existantes sont ignorées silencieusement

---

## 📧 Notifications

### Email de facture

- Envoyé automatiquement lors de la génération d'une facture
- Destinataires :
  - Email de l'organisation
  - Emails de tous les utilisateurs de l'organisation (récupérés depuis Keycloak)

### Email de rappel (facture en retard)

- Envoyé automatiquement pour les factures en retard
- Tâche planifiée : Tous les jours à 9h00
- Une facture est marquée "OVERDUE" si la date d'échéance est dépassée

---

## 🛠️ Configuration Technique

### Schedulers Spring

1. **Traitement des cycles mensuels** (`MonthlyPlanSchedulerService`)
   - Cron : `0 0 0 * * ?` (Tous les jours à minuit)
   - Méthode : `processMonthlyPlanCycles()`
   - Actions :
     - Applique les changements de plan mensuel en attente (dont la date d'effet est arrivée)
     - Reconduit automatiquement les plans mensuels expirés
     - Génère les factures de reconduction

2. **Marquage factures en retard**
   - Cron : `0 0 9 * * ?` (Tous les jours à 9h00)
   - Méthode : `markOverdueInvoices()`

### Variables d'environnement

```env
# Tarif de base par requête (en EUR)
BASE_REQUEST_PRICE_EUR=0.01

# Taux de change USD → EUR (optionnel, défaut: 0.92)
USD_TO_EUR_RATE=0.92
```

---

## 📊 Exemples de Scénarios

### Scénario 1 : Cycle mensuel et reconduction

**Date de début** : 15 janvier  
**Plan** : Plan mensuel 500 requêtes/mois, 50 EUR/mois  
**Cycle** : Du 15 janvier au 14 février (inclus)

**Résultat** :
- Le quota de 500 requêtes est valable du 15 janvier au 14 février
- Le 15 février à minuit, le scheduler :
  - Génère une facture de 50 EUR pour le cycle écoulé
  - Reconduit automatiquement le plan
  - Initialise un nouveau cycle : du 15 février au 14 mars (inclus)
  - Réinitialise le quota à 500 requêtes

### Scénario 2 : Changement de plan mensuel → mensuel (en attente)

**Date** : 20 janvier  
**Cycle actuel** : Du 15 janvier au 14 février (inclus)  
**Ancien plan** : Plan mensuel 300 requêtes/mois, 30 EUR/mois  
**Nouveau plan** : Plan mensuel 500 requêtes/mois, 50 EUR/mois

**Résultat** :
- ✅ Changement enregistré en attente
- Le plan actuel reste actif jusqu'au 14 février
- Le 15 février à minuit, le scheduler :
  - Génère une facture de clôture de 30 EUR pour l'ancien plan (cycle complet)
  - Applique le nouveau plan
  - Initialise un nouveau cycle : du 15 février au 14 mars (inclus)
  - Nouveau quota : 500 requêtes

### Scénario 3 : Passage Pay-per-Request → Plan Mensuel

**Date** : 10 février  
**Ancien plan** : Pay-per-Request, 0.05 EUR/requête  
**Dernière facture** : 1er février  
**Requêtes depuis le 1er février** : 200 requêtes  
**Nouveau plan** : Plan mensuel 500 requêtes/mois, 50 EUR/mois

**Résultat** :
- ✅ Changement immédiat
- Facture de clôture Pay-per-Request : 200 × 0.05 = **10 EUR** (du 1er au 10 février)
- Le plan mensuel prend effet immédiatement
- Nouveau cycle initialisé : du 10 février au 9 mars (inclus)
- Quota : 500 requêtes pour le nouveau cycle

### Scénario 4 : Changement Plan Mensuel → Pay-per-Request (quota non dépassé)

**Date** : 20 janvier  
**Cycle actuel** : Du 15 janvier au 14 février (inclus)  
**Plan actuel** : Plan mensuel 500 requêtes/mois, 50 EUR/mois  
**Requêtes utilisées** : 300 requêtes (quota non dépassé)  
**Nouveau plan** : Pay-per-Request, 0.05 EUR/requête

**Résultat** :
- ⚠️ Changement enregistré en attente
- Le plan mensuel reste actif jusqu'au 14 février
- Le scheduler vérifie quotidiennement si le quota est dépassé
- Si le quota est dépassé avant le 14 février : changement immédiat + facture de clôture
- Si le quota n'est pas dépassé : changement à la fin du cycle (15 février) + facture de clôture

### Scénario 5 : Changement Plan Mensuel → Pay-per-Request (quota dépassé)

**Date** : 20 janvier  
**Cycle actuel** : Du 15 janvier au 14 février (inclus)  
**Plan actuel** : Plan mensuel 500 requêtes/mois, 50 EUR/mois  
**Requêtes utilisées** : 600 requêtes (quota dépassé)  
**Nouveau plan** : Pay-per-Request, 0.05 EUR/requête

**Résultat** :
- ✅ Changement immédiat
- Facture de clôture mensuelle : **50 EUR** (cycle complet du 15 janvier au 14 février)
- Le plan Pay-per-Request prend effet immédiatement
- Les 100 requêtes supplémentaires (600 - 500) sont déjà facturées au tarif Pay-per-Request

### Scénario 6 : Quota mensuel dépassé (facturation automatique provisoire)

**Date** : 25 janvier  
**Cycle** : Du 15 janvier au 14 février (inclus)  
**Plan** : Plan mensuel 500 requêtes/mois, 50 EUR/mois  
**Requêtes utilisées** : 600 requêtes  
**Plan Pay-per-Request du marché** : 0.05 EUR/requête

**Résultat** :
- Les 500 premières requêtes sont incluses dans le plan mensuel (non facturées par requête)
- Les 100 requêtes supplémentaires sont facturées automatiquement : 100 × 0.05 = **5 EUR**
- **Le plan reste mensuel** : Ce n'est pas un changement de plan, seulement une facturation provisoire
- Le quota reste valable jusqu'au 14 février
- À la fin du cycle :
  - Facture mensuelle de **50 EUR** (montant fixe du plan)
  - Les 5 EUR de requêtes hors quota sont déjà facturées dans les logs
  - Le quota est réinitialisé à 500 requêtes pour le nouveau cycle

---

## 📝 Notes Importantes

1. **Cycle mensuel** : Les plans mensuels utilisent un cycle personnalisé (du jour J au jour J-1 du mois suivant inclus), pas le mois calendaire
2. **Reconduction tacite** : Les plans mensuels sont automatiquement reconduits à la fin de chaque cycle
3. **Changement en attente** : Les changements de plan mensuel → mensuel peuvent être annulés avant la date d'effet
4. **Facturation mensuelle** : Les plans mensuels ne sont PAS facturés par requête, seulement le montant fixe mensuel
5. **Requêtes hors quota** : Pour les plans mensuels, les requêtes dépassant le quota sont facturées au tarif Pay-per-Request
6. **Devise** : Toutes les facturations se font dans la devise du marché de l'organisation
7. **Plan d'essai** : Ne peut être utilisé qu'une seule fois par organisation

---

## 🔄 API et Endpoints

### Changement de plan

- **Endpoint** : `PUT /api/organizations/{organizationId}/pricing-plan`
- **Body** : `{ "pricingPlanId": <id> }`
- **Comportement** : Selon les règles de changement de plan décrites ci-dessus
- **Cas particulier** : Pour Plan Mensuel → Pay-per-Request :
  - Si quota dépassé : effet immédiat
  - Si quota non dépassé : changement en attente (effet immédiat si quota dépassé avant la fin du cycle, sinon à la fin du cycle)

### Annulation d'un changement en attente

- **Endpoint pour plan mensuel** : `DELETE /api/organizations/{organizationId}/pending-plan-change`
- **Comportement** : Annule un changement de plan mensuel en attente

- **Endpoint pour Pay-per-Request** : `DELETE /api/organizations/{organizationId}/pending-pay-per-request-change`
- **Comportement** : Annule un changement vers Pay-per-Request en attente

---

**Dernière mise à jour** : Février 2025

