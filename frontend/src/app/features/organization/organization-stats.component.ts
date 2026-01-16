import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, AsyncPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { UserService, UserUsageStats, UserQuota, Organization } from '../../core/services/user.service';
import { AuthService } from '../../core/services/auth.service';
import { PricingPlanService, PricingPlan } from '../../core/services/pricing-plan.service';
import { NotificationService } from '../../core/services/notification.service';
import { OrganizationAccountService, OrganizationUsageLog } from '../../core/services/organization-account.service';
import { CurrencyService } from '../../core/services/currency.service';
import { Chart, ChartConfiguration, registerables } from 'chart.js';
import { take } from 'rxjs/operators';

Chart.register(...registerables);

@Component({
  selector: 'app-organization-stats',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, AsyncPipe],
  styleUrl: './organization-stats.component.css',
  template: `
    <div class="stats-container">
      <h2>📊 Statistiques Globales de l'Organisation</h2>

      <!-- Bannière d'alerte si l'essai est définitivement terminé -->
      @if (organization && organization.trialPermanentlyExpired) {
        <div class="trial-expired-alert">
          <div class="alert-content">
            <h3>⚠️ Action requise : Choisissez un plan tarifaire</h3>
            <p>
              Le quota de votre essai gratuit a été atteint et est maintenant <strong>définitivement désactivé</strong> pour votre organisation. 
              Aucune requête HS-code n'est autorisée pour tous les collaborateurs jusqu'à ce qu'un nouveau plan soit sélectionné.
            </p>
            <p class="alert-action">
              <strong>Veuillez sélectionner un plan tarifaire ci-dessous pour continuer à utiliser le service.</strong>
            </p>
          </div>
        </div>
      }

      <!-- Organisation -->
      <div class="organization-card" *ngIf="organization || loadingOrg">
        <h3>🏢 Mon Organisation</h3>
        @if (loadingOrg) {
          <p>Chargement...</p>
        } @else if (organization) {
          <div class="org-details">
            <p><strong>Nom:</strong> {{ organization.name }}</p>
            @if (organization.email) {
              <p><strong>Email:</strong> {{ organization.email }}</p>
            }
            <p><strong>Créée le:</strong> {{ formatDate(organization.createdAt) }}</p>
          </div>
        }
      </div>

      <!-- Plan tarifaire -->
      @if (organization) {
        <div class="pricing-plan-card">
          <h3>💳 Plan Tarifaire</h3>
          @if (loadingPlans) {
            <p>Chargement...</p>
          } @else if (pricingPlans.length > 0) {
            <div class="current-plan">
              @if (currentPlan) {
                <div class="plan-info">
                  <p><strong>Plan actuel:</strong> {{ currentPlan.name }}</p>
                  <p><strong>Prix:</strong> 
                    @if (currentPlan.pricePerMonth !== null && currentPlan.pricePerMonth !== undefined) {
                      @if (currentPlan.pricePerMonth === 0) {
                        @if (!organization.trialPermanentlyExpired) {
                          Gratuit
                        }
                      } @else {
                        {{ currentPlan.pricePerMonth }} 
                        @if (currencySymbol$ | async; as symbol) {
                          {{ symbol }}
                        } @else {
                          €
                        }/mois
                      }
                    } @else if (currentPlan.pricePerRequest !== null && currentPlan.pricePerRequest !== undefined) {
                      {{ currentPlan.pricePerRequest }} 
                      @if (currencySymbol$ | async; as symbol) {
                        {{ symbol }}
                      } @else {
                        €
                      }/requête
                    } @else {
                      @if (!organization.trialPermanentlyExpired) {
                        Gratuit
                      } @else {
                        <span style="color: #dc3545;">Plan gratuit - Sélection d'un plan payant requise</span>
                      }
                    }
                  </p>
                  @if (currentPlan.trialPeriodDays) {
                    <p><strong>Période d'essai:</strong> Valable {{ currentPlan.trialPeriodDays }} jours</p>
                  } @else if (currentPlan.pricePerMonth !== null && currentPlan.pricePerMonth !== undefined && currentPlan.pricePerMonth > 0) {
                    <!-- Plan mensuel -->
                    @if (currentPlan.monthlyQuota) {
                      <p><strong>Quota:</strong> {{ currentPlan.monthlyQuota | number }} requêtes/mois</p>
                    } @else {
                      <p><strong>Quota:</strong> Illimité</p>
                    }
                    @if (organization.monthlyPlanEndDate) {
                      <p><strong>Prochaine reconduction:</strong> {{ formatRenewalDate(organization.monthlyPlanEndDate) }}</p>
                    }
                  } @else if (currentPlan.pricePerRequest !== null && currentPlan.pricePerRequest !== undefined) {
                    <p><strong>Quota:</strong> Facturation à la requête</p>
                  } @else {
                    <p><strong>Quota:</strong> Illimité</p>
                  }
                </div>
              } @else {
                <p class="no-plan-message">Aucun plan tarifaire sélectionné</p>
              }
            </div>
            <div class="change-plan-section" [class.required-change]="organization.trialPermanentlyExpired">
              @if (organization.trialPermanentlyExpired) {
                <h4 class="required-plan-title">🔴 Sélection obligatoire d'un plan tarifaire</h4>
                <p class="required-plan-message">
                  Votre essai gratuit étant définitivement terminé, vous devez choisir un plan payant pour continuer à utiliser le service. 
                  <strong>Il suffit de valider le plan sélectionné ci-dessous - aucune création de compte supplémentaire n'est nécessaire.</strong>
                </p>
                @if (!selectedPlanId && pricingPlans.length > 0) {
                  <p class="selection-prompt" style="color: #dc3545; font-weight: bold; margin: 1rem 0;">
                    ⚠️ Veuillez sélectionner un plan payant dans la liste ci-dessous.
                  </p>
                } @else if (selectedPlanId) {
                  <p class="selection-prompt" style="color: #28a745; font-weight: bold; margin: 1rem 0;">
                    ✅ Un plan payant est sélectionné. Cliquez sur le bouton ci-dessous pour valider.
                  </p>
                }
              } @else {
                <h4>Changer de plan</h4>
              }
              <select [(ngModel)]="selectedPlanId" (ngModelChange)="onPlanSelectChange()" class="plan-select" [disabled]="loadingPlans" [class.required-select]="organization.trialPermanentlyExpired">
                @if (!organization.trialPermanentlyExpired) {
                  <option [value]="null">Aucun plan (gratuit)</option>
                }
                @if (organization.trialPermanentlyExpired && !selectedPlanId && pricingPlans.length > 0) {
                  <option [value]="null" selected disabled style="color: #dc3545;">⚠️ Sélectionnez un plan payant</option>
                }
                @for (plan of pricingPlans; track plan.id) {
                  <option [value]="plan.id" [selected]="plan.id === organization.pricingPlanId">
                    {{ plan.name }} - 
                    @if (plan.pricePerMonth !== null && plan.pricePerMonth !== undefined && plan.pricePerMonth > 0) {
                      {{ plan.pricePerMonth }} 
                      @if (currencySymbol$ | async; as symbol) {
                        {{ symbol }}
                      } @else {
                        €
                      }/mois
                    } @else if (plan.pricePerRequest !== null && plan.pricePerRequest !== undefined && plan.pricePerRequest > 0) {
                      {{ plan.pricePerRequest }} 
                      @if (currencySymbol$ | async; as symbol) {
                        {{ symbol }}
                      } @else {
                        €
                      }/requête
                    }
                    @if (plan.trialPeriodDays) {
                      ({{ plan.trialPeriodDays }} jours)
                    } @else if (plan.monthlyQuota) {
                      ({{ plan.monthlyQuota | number }} requêtes/mois)
                    } @else if (plan.pricePerRequest !== null && plan.pricePerRequest !== undefined) {
                      (Facturation à la requête)
                    } @else {
                      (Quota illimité)
                    }
                  </option>
                }
              </select>
              <button 
                class="btn btn-primary" 
                [class.btn-required]="organization.trialPermanentlyExpired"
                (click)="openConfirmModal()"
                [disabled]="isChangingPlan || !selectedPlanId"
                [title]="(!selectedPlanId && organization.trialPermanentlyExpired) ? 'Sélectionnez un plan payant pour continuer' : ''">
                @if (isChangingPlan) {
                  <span>Changement en cours...</span>
                } @else if (organization.trialPermanentlyExpired && selectedPlanId) {
                  <span>🔴 Valider le nouveau plan (obligatoire)</span>
                } @else if (organization.trialPermanentlyExpired && !selectedPlanId) {
                  <span>Sélectionnez un plan payant</span>
                } @else {
                  <span>Changer de plan</span>
                }
              </button>
              @if (!organization.trialPermanentlyExpired) {
                <a routerLink="/pricing" class="view-all-plans-link">Voir tous les plans tarifaires</a>
              }
            </div>
          } @else {
            <p class="no-plans-message">Aucun plan tarifaire disponible</p>
          }
        </div>
      }

      <!-- Modal de confirmation pour le changement de plan -->
      @if (showConfirmModal && selectedPlanForConfirmation) {
        <div class="modal-overlay" [class.modal-required]="organization && organization.trialPermanentlyExpired" (click)="!organization?.trialPermanentlyExpired && closeConfirmModal()">
          <div class="modal-content" [class.modal-required-content]="organization && organization.trialPermanentlyExpired" (click)="$event.stopPropagation()">
            <div class="modal-header">
              @if (organization && organization.trialPermanentlyExpired) {
                <h3>🔴 Validation obligatoire d'un plan payant</h3>
              } @else {
                <h3>Confirmer le changement de plan</h3>
              }
              @if (!organization || !organization.trialPermanentlyExpired) {
                <button class="modal-close" (click)="closeConfirmModal()">&times;</button>
              }
            </div>
            <div class="modal-body">
              <p>Vous êtes sur le point de changer votre plan tarifaire vers :</p>
              <div class="selected-plan-info">
                <h4>{{ selectedPlanForConfirmation.name }}</h4>
                @if (selectedPlanForConfirmation.description) {
                  <p class="plan-description">{{ selectedPlanForConfirmation.description }}</p>
                }
                @if (selectedPlanForConfirmation.pricePerMonth !== null && selectedPlanForConfirmation.pricePerMonth !== undefined) {
                  @if (selectedPlanForConfirmation.pricePerMonth === 0) {
                    @if (!organization?.trialPermanentlyExpired) {
                      <p class="plan-price">Gratuit</p>
                    } @else {
                      <p class="plan-price" style="color: #dc3545;">Plan gratuit - Non disponible</p>
                    }
                  } @else {
                    <p class="plan-price">
                      {{ selectedPlanForConfirmation.pricePerMonth }} 
                      @if (currencySymbol$ | async; as symbol) {
                        {{ symbol }}
                      } @else {
                        €
                      }/mois
                    </p>
                  }
                } @else if (selectedPlanForConfirmation.pricePerRequest !== null && selectedPlanForConfirmation.pricePerRequest !== undefined) {
                  <p class="plan-price">
                    {{ selectedPlanForConfirmation.pricePerRequest }} 
                    @if (currencySymbol$ | async; as symbol) {
                      {{ symbol }}
                    } @else {
                      €
                    }/requête
                  </p>
                }
                @if (selectedPlanForConfirmation.monthlyQuota) {
                  <p class="plan-quota">Quota: {{ selectedPlanForConfirmation.monthlyQuota | number }} requêtes/mois</p>
                } @else if (selectedPlanForConfirmation.pricePerRequest !== null && selectedPlanForConfirmation.pricePerRequest !== undefined) {
                  <p class="plan-quota">Facturation à la requête</p>
                } @else {
                  <p class="plan-quota">Quota illimité</p>
                }
              </div>
              @if (organization && organization.trialPermanentlyExpired) {
                <p class="confirmation-warning-urgent">🔴 <strong>Action obligatoire :</strong> Votre essai gratuit étant définitivement terminé, vous devez valider un plan payant pour continuer à utiliser le service.</p>
                <p class="confirmation-info">✅ <strong>Validation simple :</strong> Il suffit de valider ce plan payant. Votre compte organisation existant sera automatiquement mis à jour avec le nouveau plan. <strong>Aucune création de compte supplémentaire n'est nécessaire.</strong></p>
                <p class="confirmation-info">Une fois validé, votre plan sera immédiatement activé et vos collaborateurs pourront à nouveau effectuer des requêtes HS-code.</p>
              } @else {
                <p class="confirmation-warning">⚠️ Êtes-vous sûr de vouloir continuer ?</p>
              }
            </div>
            <div class="modal-footer">
              <button class="btn btn-secondary" (click)="closeConfirmModal()" [disabled]="isChangingPlan">
                Annuler
              </button>
              <button class="btn btn-primary" [class.btn-required]="organization && organization.trialPermanentlyExpired" (click)="changePricingPlan()" [disabled]="isChangingPlan">
                @if (isChangingPlan) {
                  <span>Changement en cours...</span>
                } @else if (organization && organization.trialPermanentlyExpired) {
                  <span>✅ Valider le plan payant (obligatoire)</span>
                } @else {
                  <span>Confirmer le changement</span>
                }
              </button>
            </div>
          </div>
        </div>
      }

      <!-- Quota organisation -->
      <div class="quota-card" *ngIf="quota || loadingQuota">
        <h3>📈 Utilisation Organisation Ce Mois</h3>
        @if (loadingQuota) {
          <p>Chargement...</p>
        } @else if (quota) {
          @if (!quota.hasOrganization) {
            <p class="no-org-message">{{ quota.message }}</p>
          } @else {
            <div class="quota-details">
              @if (quota.isUnlimited) {
                <div class="quota-unlimited">
                  <p class="quota-status">✅ Quota illimité</p>
                  <p class="quota-usage">Utilisation organisation ce mois: {{ quota.currentUsage || 0 }} requêtes</p>
                </div>
              } @else {
                <!-- Graphique circulaire du quota -->
                <div class="quota-chart-wrapper">
                  <canvas #quotaChart></canvas>
                </div>
                <div class="quota-limited">
                  <div class="quota-progress">
                    <div class="quota-progress-bar">
                      <div class="quota-progress-fill" [style.width.%]="quota.percentageUsed || 0" 
                           [class.quota-warning]="(quota.percentageUsed || 0) >= 80"
                           [class.quota-danger]="(quota.percentageUsed || 0) >= 100">
                      </div>
                    </div>
                    <p class="quota-text">
                      {{ quota.currentUsage || 0 }} / {{ quota.monthlyQuota }} requêtes
                      ({{ (quota.percentageUsed || 0).toFixed(1) }}%)
                    </p>
                  </div>
                  <p class="quota-usage-info">
                    <span class="quota-usage-text">Utilisation de l'organisation: {{ quota.currentUsage || 0 }} requêtes</span>
                  </p>
                  <p class="quota-remaining">
                    @if ((quota.remaining || 0) >= 0) {
                      <span class="quota-remaining-text">⚠️ {{ quota.remaining }} requêtes restantes dans le plan de l'organisation</span>
                    } @else {
                      <span class="quota-exceeded">❌ Quota dépassé!</span>
                    }
                  </p>
                </div>
              }
            </div>
          }
        }
      </div>

      <!-- Statistiques d'utilisation -->
      <div class="stats-card" *ngIf="stats || loadingStats">
        <h3>📊 Statistiques Globales</h3>
        @if (loadingStats) {
          <p>Chargement...</p>
        } @else if (stats) {
          <div class="stats-grid">
            <div class="stat-item">
              <h4>📈 Total Requêtes</h4>
              <p class="stat-value">{{ stats.totalRequests }}</p>
            </div>
            <div class="stat-item">
              <h4>💰 Prix Total Requêtes</h4>
              <p class="stat-value">{{ formatCurrency(stats.totalCostUsd) }}</p>
            </div>
            @if (isAdmin()) {
              <div class="stat-item">
                <h4>🔢 Tokens Total</h4>
                <p class="stat-value">{{ formatNumber(stats.totalTokens) }}</p>
              </div>
            }
            <div class="stat-item">
              <h4>📅 Requêtes ce mois</h4>
              <p class="stat-value">{{ stats.monthlyRequests }}</p>
            </div>
            @if (organization?.monthlyPlanEndDate) {
              <div class="stat-item">
                <h4>🔄 Prochaine reconduction</h4>
                <p class="stat-value">{{ formatRenewalDate(organization?.monthlyPlanEndDate) }}</p>
              </div>
            }
          </div>

          <!-- Filtres -->
          <div class="filters">
            <div class="filter-group">
              <label for="startDate">Date de début:</label>
              <input type="date" id="startDate" [(ngModel)]="startDate" (change)="onFilterChange()" />
            </div>
            <div class="filter-group">
              <label for="endDate">Date de fin:</label>
              <input type="date" id="endDate" [(ngModel)]="endDate" (change)="onFilterChange()" />
            </div>
            <button class="btn btn-secondary" (click)="resetFilters()">Réinitialiser</button>
          </div>

        }
      </div>

      <!-- Liste de toutes les requêtes de l'organisation -->
      <div class="usage-logs-card" *ngIf="organizationUsageLogs || loadingUsageLogs">
        <h3>📋 Toutes les Requêtes de l'Organisation</h3>
        @if (loadingUsageLogs) {
          <p>Chargement...</p>
        } @else if (organizationUsageLogs && organizationUsageLogs.usageLogs.length > 0) {
          <div class="usage-logs-info">
            <p><strong>Période:</strong> {{ formatDate(organizationUsageLogs.startDate) }} - {{ formatDate(organizationUsageLogs.endDate) }}</p>
            <p><strong>Total:</strong> {{ organizationUsageLogs.totalRequests }} requête(s)</p>
          </div>
          <table class="usage-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Collaborateur</th>
                <th>Endpoint</th>
                <th>Recherche</th>
                @if (isAdmin()) {
                  <th>Tokens</th>
                  <th>Coût tokens (USD)</th>
                }
                <th>Prix requête</th>
              </tr>
            </thead>
            <tbody>
              @for (log of organizationUsageLogs.usageLogs; track log.id) {
                <tr>
                  <td>{{ formatDate(log.timestamp) }}</td>
                  <td><strong>{{ log.collaboratorName }}</strong></td>
                  <td>{{ log.endpoint }}</td>
                  <td class="search-term">{{ truncateSearchTerm(log.searchTerm) }}</td>
                  @if (isAdmin()) {
                    <td>{{ formatNumber(log.tokensUsed || 0) }}</td>
                    <td>
                      {{ formatCost(log.tokenCostUsd || 0) }} USD
                    </td>
                  }
                  <td>
                    @if (log.totalCostUsd === null || log.totalCostUsd === undefined || log.totalCostUsd === 0) {
                      <span class="forfait-badge">Forfait</span>
                    } @else {
                      <strong>
                        {{ formatCost(log.totalCostUsd) }}
                        @if (currencySymbol$ | async; as symbol) {
                          {{ symbol }}
                        } @else {
                          €
                        }
                      </strong>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        } @else {
          <p class="empty-message">Aucune requête pour cette période.</p>
        }
      </div>

      <!-- Message d'erreur -->
      @if (errorMessage) {
        <div class="error-message">{{ errorMessage }}</div>
      }
    </div>
  `,
})
export class OrganizationStatsComponent implements OnInit {
  private userService = inject(UserService);
  private authService = inject(AuthService);
  private pricingPlanService = inject(PricingPlanService);
  private notificationService = inject(NotificationService);
  private organizationAccountService = inject(OrganizationAccountService);
  private currencyService = inject(CurrencyService);

  organization: Organization | null = null;
  currencySymbol$ = this.currencyService.getCurrencySymbol();
  private currentCurrencyCode = 'EUR'; // Par défaut, sera mis à jour dans ngOnInit
  private currentCurrencySymbol = '€'; // Par défaut, sera mis à jour dans ngOnInit
  quota: UserQuota | null = null;
  stats: UserUsageStats | null = null;
  organizationUsageLogs: any = null;

  loadingOrg = false;
  loadingQuota = false;
  loadingStats = false;
  loadingUsageLogs = false;
  errorMessage = '';

  startDate = '';
  endDate = '';

  // Plans tarifaires
  pricingPlans: PricingPlan[] = [];
  currentPlan: PricingPlan | null = null;
  selectedPlanId: number | null = null;
  loadingPlans = false;
  isChangingPlan = false;
  showConfirmModal = false;
  selectedPlanForConfirmation: PricingPlan | null = null;

  private quotaChart: Chart | null = null;

  ngOnInit() {
    // Précharger la devise pour qu'elle soit disponible immédiatement
    this.currencySymbol$.pipe(take(1)).subscribe({
      next: (symbol) => {
        console.log('✅ OrganizationStatsComponent: Symbole de devise chargé:', symbol);
        this.currentCurrencySymbol = symbol;
      },
      error: (err) => {
        console.error('❌ OrganizationStatsComponent: Erreur lors du chargement de la devise:', err);
      }
    });

    // Charger aussi le code de devise pour formatCurrency
    this.currencyService.getCurrencyCode().pipe(take(1)).subscribe({
      next: (code) => {
        console.log('✅ OrganizationStatsComponent: Code devise chargé:', code);
        this.currentCurrencyCode = code;
        this.currentCurrencySymbol = this.currencyService.getSymbolForCurrency(code);
      },
      error: (err) => {
        console.error('❌ OrganizationStatsComponent: Erreur lors du chargement du code devise:', err);
      }
    });

    this.loadOrganization();
    this.loadQuota();
    // Charger les logs d'utilisation qui recalculeront automatiquement les stats
    this.loadOrganizationUsageLogs();
  }

  loadOrganization() {
    this.loadingOrg = true;
    this.errorMessage = '';

    // Charger d'abord le statut pour avoir l'état réel (met à jour trialPermanentlyExpired si nécessaire)
    this.organizationAccountService.getOrganizationStatus().subscribe({
      next: (status) => {
        console.log('📊 Statut de l\'organisation:', status);
        console.log('🔍 isTrialExpired:', status.isTrialExpired);
        console.log('🔍 trialPermanentlyExpired (status):', status.trialPermanentlyExpired);

        // Charger ensuite les détails de l'organisation
        this.userService.getMyOrganization().subscribe({
          next: (org) => {
            console.log('🏢 Organisation chargée:', org);

            // Mettre à jour trialPermanentlyExpired avec la valeur du statut (plus à jour)
            if (org && status.trialPermanentlyExpired !== undefined && status.trialPermanentlyExpired !== null) {
              org.trialPermanentlyExpired = status.trialPermanentlyExpired;
              console.log('✅ trialPermanentlyExpired mis à jour avec le statut:', org.trialPermanentlyExpired);
            } else if (org) {
              console.log('🔍 trialPermanentlyExpired depuis l\'organisation:', org.trialPermanentlyExpired);
            }

            this.organization = org;
            this.selectedPlanId = org?.pricingPlanId || null;
            this.loadingOrg = false;
            if (org?.pricingPlanId) {
              this.updateCurrentPlan(org.pricingPlanId);
            } else {
              this.currentPlan = null;
            }
            // Recharger les plans tarifaires après avoir chargé l'organisation pour appliquer le bon filtre
            this.loadPricingPlans();
          },
          error: (err) => {
            this.errorMessage = 'Erreur lors du chargement de l\'organisation: ' + (err.error?.message || err.message);
            this.loadingOrg = false;
            // Charger les plans même en cas d'erreur (sans filtre)
            this.loadPricingPlans();
          }
        });
      },
      error: (err) => {
        console.error('❌ Erreur lors du chargement du statut:', err);
        // Si le statut échoue, charger quand même l'organisation
        this.userService.getMyOrganization().subscribe({
          next: (org) => {
            console.log('🏢 Organisation chargée (sans statut):', org);
            this.organization = org;
            this.selectedPlanId = org?.pricingPlanId || null;
            this.loadingOrg = false;
            if (org?.pricingPlanId) {
              this.updateCurrentPlan(org.pricingPlanId);
            } else {
              this.currentPlan = null;
            }
            this.loadPricingPlans();
          },
          error: (err2) => {
            this.errorMessage = 'Erreur lors du chargement de l\'organisation: ' + (err2.error?.message || err2.message);
            this.loadingOrg = false;
            this.loadPricingPlans();
          }
        });
      }
    });
  }

  loadPricingPlans() {
    this.loadingPlans = true;
    const organizationId = this.organization?.id;
    // Convertir null en undefined pour correspondre au type attendu
    const marketVersion = this.organization?.marketVersion ?? undefined;

    // Utiliser le nouvel endpoint qui exclut automatiquement le plan d'essai si déjà utilisé
    this.pricingPlanService.getAvailablePricingPlans(marketVersion, organizationId).subscribe({
      next: (plans) => {
        console.log('📋 Plans disponibles reçus du serveur:', plans.length, plans);
        console.log('🔍 Organization trialPermanentlyExpired:', this.organization?.trialPermanentlyExpired);

        // Le backend exclut déjà le plan d'essai et les plans gratuits si l'organisation les a déjà utilisés
        // Mais on peut ajouter un filtre supplémentaire côté frontend pour s'assurer
        // que seuls les plans payants sont proposés si l'essai est définitivement terminé ou si l'organisation a un plan payant
        const hasUsedTrial = this.organization?.trialPermanentlyExpired || this.organization?.trialExpiresAt;
        const hasPaidPlan = this.currentPlan &&
          ((this.currentPlan.pricePerMonth !== null && this.currentPlan.pricePerMonth !== undefined && this.currentPlan.pricePerMonth > 0) ||
            (this.currentPlan.pricePerRequest !== null && this.currentPlan.pricePerRequest !== undefined && this.currentPlan.pricePerRequest > 0));

        if (hasUsedTrial || hasPaidPlan) {
          // Filtrer pour ne garder que les plans payants (exclure les plans gratuits et d'essai)
          const filteredPlans = plans.filter(plan => {
            // Exclure les plans d'essai
            if (plan.trialPeriodDays && plan.trialPeriodDays > 0) {
              console.log('❌ Plan exclu (essai):', plan.name);
              return false;
            }

            // Exclure les plans gratuits
            const hasPaidMonthlyPrice = plan.pricePerMonth !== null && plan.pricePerMonth !== undefined && plan.pricePerMonth > 0;
            const hasPaidPerRequestPrice = plan.pricePerRequest !== null && plan.pricePerRequest !== undefined && plan.pricePerRequest > 0;
            const isPaidPlan = hasPaidMonthlyPrice || hasPaidPerRequestPrice;

            if (!isPaidPlan) {
              console.log('❌ Plan exclu (gratuit):', plan.name);
            }

            return isPaidPlan;
          });

          console.log('📊 Plans filtrés (payants uniquement):', filteredPlans.length, filteredPlans);
          this.pricingPlans = filteredPlans;
        } else {
          console.log('✅ Tous les plans disponibles (essai non terminé ou pas encore utilisé)');
          this.pricingPlans = plans;
        }
        this.loadingPlans = false;
        if (this.organization?.pricingPlanId) {
          this.updateCurrentPlan(this.organization.pricingPlanId);
        }

        // Si l'essai est terminé, toujours forcer la sélection d'un plan payant
        if (this.organization?.trialPermanentlyExpired) {
          // Vérifier si le plan actuel est un plan payant ET s'il est dans la liste filtrée
          const currentPlanIsPaid = this.currentPlan &&
            ((this.currentPlan.pricePerMonth !== null && this.currentPlan.pricePerMonth !== undefined && this.currentPlan.pricePerMonth > 0) ||
              (this.currentPlan.pricePerRequest !== null && this.currentPlan.pricePerRequest !== undefined && this.currentPlan.pricePerRequest > 0));

          const currentPlanInFilteredList = this.currentPlan && this.currentPlan.id ? this.pricingPlans.some(p => p.id === this.currentPlan!.id) : false;

          // Si le plan actuel n'est pas payant OU n'est pas dans la liste filtrée, sélectionner le premier plan payant disponible
          if ((!currentPlanIsPaid || !currentPlanInFilteredList) && this.pricingPlans.length > 0 && this.pricingPlans[0].id) {
            console.log('🔄 Plan actuel non valide, sélection du premier plan payant:', this.pricingPlans[0].id);
            this.selectedPlanId = this.pricingPlans[0].id;
            this.updateCurrentPlan(this.pricingPlans[0].id);
          } else if (currentPlanIsPaid && currentPlanInFilteredList && this.organization?.pricingPlanId) {
            // Si un plan payant est déjà sélectionné et dans la liste, garder cette sélection
            this.selectedPlanId = this.organization.pricingPlanId;
          } else if (this.pricingPlans.length > 0 && this.pricingPlans[0].id) {
            // Fallback : sélectionner le premier plan payant disponible
            console.log('🔄 Fallback : sélection du premier plan payant:', this.pricingPlans[0].id);
            this.selectedPlanId = this.pricingPlans[0].id;
            this.updateCurrentPlan(this.pricingPlans[0].id);
          } else {
            // Aucun plan payant disponible
            this.selectedPlanId = null;
            this.currentPlan = null;
          }
        } else {
          // Si l'essai n'est pas terminé, garder le plan actuel s'il existe dans la liste
          if (this.organization?.pricingPlanId) {
            const planExists = this.pricingPlans.some(p => p.id === this.organization?.pricingPlanId);
            if (planExists) {
              this.selectedPlanId = this.organization.pricingPlanId;
            } else {
              // Le plan n'existe plus dans la liste, le réinitialiser
              this.selectedPlanId = null;
              this.currentPlan = null;
            }
          }
        }
      },
      error: (err) => {
        console.error('Erreur lors du chargement des plans tarifaires:', err);
        this.loadingPlans = false;
      }
    });
  }

  updateCurrentPlan(planId: number) {
    // S'assurer que planId est un number
    const id = typeof planId === 'string' ? parseInt(planId, 10) : planId;
    const plan = this.pricingPlans.find(p => p.id === id);
    this.currentPlan = plan || null;
  }

  onPlanSelectChange() {
    // Mettre à jour selectedPlanForConfirmation quand le plan sélectionné change
    // Convertir selectedPlanId en number car il peut venir du select HTML comme string
    if (this.selectedPlanId) {
      const planId = typeof this.selectedPlanId === 'string' ? parseInt(this.selectedPlanId, 10) : this.selectedPlanId;
      const selectedPlan = this.pricingPlans.find(p => p.id === planId);
      if (selectedPlan) {
        this.selectedPlanForConfirmation = selectedPlan;
        // S'assurer que selectedPlanId est un number
        this.selectedPlanId = planId;
      }
    } else {
      this.selectedPlanForConfirmation = null;
    }
  }

  openConfirmModal() {
    if (!this.selectedPlanId) {
      if (this.organization?.trialPermanentlyExpired) {
        this.notificationService.error('Veuillez sélectionner un plan payant. Les plans d\'essai ne sont plus disponibles.');
      } else {
        this.notificationService.error('Veuillez sélectionner un plan.');
      }
      return;
    }

    // Convertir selectedPlanId en number car il peut venir du select HTML comme string
    const planId = typeof this.selectedPlanId === 'string' ? parseInt(this.selectedPlanId, 10) : this.selectedPlanId;

    // S'assurer que selectedPlanId est un number
    if (typeof this.selectedPlanId === 'string') {
      this.selectedPlanId = planId;
    }

    console.log('🔍 Recherche du plan avec ID:', planId, 'Type:', typeof planId);
    console.log('📋 Plans disponibles:', this.pricingPlans.map(p => ({ id: p.id, name: p.name, type: typeof p.id })));

    const selectedPlan = this.pricingPlans.find(p => p.id === planId);
    if (!selectedPlan) {
      console.error('❌ Plan sélectionné introuvable dans pricingPlans:', {
        selectedPlanId: this.selectedPlanId,
        planId: planId,
        planIdType: typeof planId,
        plansIds: this.pricingPlans.map(p => ({ id: p.id, type: typeof p.id }))
      });
      // Si le plan n'est pas trouvé, essayer de sélectionner le premier plan payant disponible
      if (this.pricingPlans.length > 0 && this.pricingPlans[0].id) {
        console.log('🔄 Sélection automatique du premier plan payant:', this.pricingPlans[0].id);
        this.selectedPlanId = this.pricingPlans[0].id;
        this.updateCurrentPlan(this.pricingPlans[0].id);
        // Réessayer avec le nouveau plan
        const newSelectedPlan = this.pricingPlans.find(p => p.id === this.selectedPlanId);
        if (newSelectedPlan) {
          this.selectedPlanForConfirmation = newSelectedPlan;
          this.showConfirmModal = true;
          return;
        }
      }
      this.notificationService.error('Plan sélectionné introuvable. Veuillez sélectionner un plan dans la liste.');
      return;
    }

    // Si l'essai est définitivement terminé, forcer le changement même si c'est le même plan (cas où l'ancien plan était gratuit)
    if (this.selectedPlanId === this.organization?.pricingPlanId && !this.organization?.trialPermanentlyExpired) {
      this.notificationService.info('Le plan sélectionné est déjà votre plan actuel.');
      return;
    }

    // Validation : si l'essai est définitivement terminé, vérifier que le plan sélectionné est payant
    if (this.organization?.trialPermanentlyExpired) {
      const isPaidPlan = (selectedPlan.pricePerMonth !== null && selectedPlan.pricePerMonth !== undefined && selectedPlan.pricePerMonth > 0)
        || (selectedPlan.pricePerRequest !== null && selectedPlan.pricePerRequest !== undefined && selectedPlan.pricePerRequest > 0);

      if (!isPaidPlan && (!selectedPlan.trialPeriodDays || selectedPlan.trialPeriodDays <= 0)) {
        // Plan gratuit sans essai - pas autorisé
        this.notificationService.error('Vous devez sélectionner un plan payant. Les plans gratuits ne sont plus disponibles après la fin de l\'essai.');
        return;
      }

      if (selectedPlan.trialPeriodDays && selectedPlan.trialPeriodDays > 0) {
        // Plan d'essai - pas autorisé
        this.notificationService.error('Les plans d\'essai ne sont plus disponibles. Veuillez sélectionner un plan payant.');
        return;
      }
    }

    this.selectedPlanForConfirmation = selectedPlan;
    // S'assurer que selectedPlanId est bien défini
    if (!this.selectedPlanId && selectedPlan.id) {
      this.selectedPlanId = selectedPlan.id;
    }
    console.log('✅ Modal de confirmation ouverte avec plan:', {
      selectedPlanId: this.selectedPlanId,
      selectedPlanForConfirmation: this.selectedPlanForConfirmation
    });
    this.showConfirmModal = true;
  }

  closeConfirmModal() {
    // Toujours permettre de fermer la modal, même si l'essai est terminé
    // L'utilisateur peut toujours revenir en arrière pour choisir un autre plan
    this.showConfirmModal = false;
    // Ne pas réinitialiser selectedPlanForConfirmation pour garder la sélection
  }

  changePricingPlan() {
    console.log('🔄 changePricingPlan() appelé');
    console.log('📋 État actuel:', {
      isChangingPlan: this.isChangingPlan,
      organization: this.organization,
      selectedPlanId: this.selectedPlanId,
      selectedPlanForConfirmation: this.selectedPlanForConfirmation
    });

    if (this.isChangingPlan) {
      console.warn('⚠️ Changement déjà en cours');
      return;
    }

    if (!this.organization) {
      console.error('❌ Aucune organisation trouvée');
      this.notificationService.error('Aucune organisation trouvée. Veuillez rafraîchir la page.');
      return;
    }

    // Utiliser selectedPlanForConfirmation si selectedPlanId n'est pas défini
    let planIdToUse = this.selectedPlanId || this.selectedPlanForConfirmation?.id;

    if (!planIdToUse) {
      console.error('❌ Aucun plan sélectionné');
      this.notificationService.error('Veuillez sélectionner un plan tarifaire.');
      return;
    }

    // Convertir en number si c'est une string (venant du select HTML)
    if (typeof planIdToUse === 'string') {
      planIdToUse = parseInt(planIdToUse, 10);
    }

    // S'assurer que selectedPlanId est défini et est un number
    if (!this.selectedPlanId || typeof this.selectedPlanId === 'string') {
      this.selectedPlanId = planIdToUse;
    }

    this.isChangingPlan = true;
    this.errorMessage = '';
    // Ne pas fermer la modal immédiatement, attendre la réponse
    // this.showConfirmModal = false;

    console.log('📤 Envoi de la requête de changement de plan avec planId:', planIdToUse);

    this.pricingPlanService.changeMyOrganizationPricingPlan(planIdToUse).subscribe({
      next: (updatedOrg) => {
        console.log('✅ Changement de plan réussi:', updatedOrg);
        this.organization = updatedOrg;
        this.updateCurrentPlan(updatedOrg.pricingPlanId || 0);
        this.isChangingPlan = false;
        this.selectedPlanForConfirmation = null;
        this.showConfirmModal = false; // Fermer la modal seulement après succès
        this.notificationService.success('Plan tarifaire changé avec succès');
        this.loadQuota();
        // Recharger les plans pour mettre à jour le filtre si nécessaire
        this.loadPricingPlans();
      },
      error: (err) => {
        console.error('❌ Erreur lors du changement de plan:', err);
        console.error('❌ Détails de l\'erreur:', {
          status: err.status,
          statusText: err.statusText,
          error: err.error,
          message: err.message
        });
        const errorMessage = err.error?.message || err.error?.error || err.message || 'Une erreur est survenue';
        this.errorMessage = 'Erreur lors du changement de plan: ' + errorMessage;
        this.isChangingPlan = false;
        this.notificationService.error('Erreur lors du changement de plan: ' + errorMessage);
        // Garder la modal ouverte en cas d'erreur pour permettre une nouvelle tentative
        // this.showConfirmModal reste true
      }
    });
  }

  loadQuota() {
    this.loadingQuota = true;
    this.userService.getMyQuota().subscribe({
      next: (quota) => {
        this.quota = quota;
        this.loadingQuota = false;
        setTimeout(() => {
          this.updateQuotaChart();
        }, 100);
      },
      error: (err) => {
        this.errorMessage = 'Erreur lors du chargement du quota: ' + (err.error?.message || err.message);
        this.loadingQuota = false;
      }
    });
  }

  loadStats() {
    this.loadingStats = true;
    this.errorMessage = '';
    // Pour les statistiques de l'organisation, on calcule à partir des logs d'utilisation
    // qui sont déjà chargés dans loadOrganizationUsageLogs()
    // On recalcule les stats à partir des logs existants
    this.calculateStatsFromLogs();
    this.loadingStats = false;
  }

  private calculateStatsFromLogs() {
    if (!this.organizationUsageLogs || !this.organizationUsageLogs.usageLogs) {
      this.stats = {
        totalRequests: 0,
        totalCostUsd: 0,
        totalTokens: 0,
        monthlyRequests: 0,
        recentUsage: []
      };
      return;
    }

    const logs = this.organizationUsageLogs.usageLogs;
    const now = new Date();
    const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);
    const monthlyRequests = logs.filter((log: OrganizationUsageLog) => {
      const logDate = new Date(log.timestamp);
      return logDate >= startOfMonth;
    }).length;

    // Calculer le total des requêtes
    const totalRequests = logs.length;

    // Calculer le coût total
    const totalCostUsd = logs.reduce((sum: number, log: OrganizationUsageLog) => {
      return sum + (log.totalCostUsd || 0);
    }, 0);

    // Calculer le total des tokens
    const totalTokens = logs.reduce((sum: number, log: OrganizationUsageLog) => {
      return sum + (log.tokensUsed || 0);
    }, 0);

    // Trier les logs par date (plus récent en premier) et prendre les 10 premiers
    const recentLogs = [...logs]
      .sort((a: OrganizationUsageLog, b: OrganizationUsageLog) => {
        return new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime();
      })
      .slice(0, 10)
      .map((log: OrganizationUsageLog) => ({
        id: log.id,
        endpoint: log.endpoint,
        searchTerm: log.searchTerm || 'N/A',
        tokensUsed: log.tokensUsed || 0,
        costUsd: log.totalCostUsd || null,
        timestamp: log.timestamp
      }));

    this.stats = {
      totalRequests,
      totalCostUsd,
      totalTokens,
      monthlyRequests,
      recentUsage: recentLogs
    };
  }

  loadOrganizationUsageLogs() {
    this.loadingUsageLogs = true;
    const startDate = this.startDate || undefined;
    const endDate = this.endDate || undefined;

    this.organizationAccountService.getOrganizationUsageLogs(startDate, endDate).subscribe({
      next: (logs) => {
        this.organizationUsageLogs = logs;
        this.loadingUsageLogs = false;
        // Recalculer les stats à partir des nouveaux logs
        this.calculateStatsFromLogs();
      },
      error: (err) => {
        this.errorMessage = 'Erreur lors du chargement des logs d\'utilisation: ' + (err.error?.message || err.message);
        this.loadingUsageLogs = false;
      }
    });
  }

  updateQuotaChart() {
    if (!this.quota || !this.quota.hasOrganization || this.quota.isUnlimited) {
      return;
    }

    const used = this.quota.currentUsage || 0;
    const total = this.quota.monthlyQuota || 1;
    const remaining = Math.max(0, total - used);
    const percentage = this.quota.percentageUsed || 0;

    let usedColor = 'rgba(46, 204, 113, 0.8)';
    if (percentage >= 100) {
      usedColor = 'rgba(231, 76, 60, 0.8)';
    } else if (percentage >= 80) {
      usedColor = 'rgba(243, 156, 18, 0.8)';
    }

    if (this.quotaChart) {
      this.quotaChart.destroy();
    }

    const config: ChartConfiguration = {
      type: 'doughnut',
      data: {
        labels: ['Utilisé', 'Restant'],
        datasets: [{
          data: [used, remaining],
          backgroundColor: [usedColor, 'rgba(189, 195, 199, 0.3)'],
          borderColor: [usedColor, 'rgba(189, 195, 199, 0.5)'],
          borderWidth: 2
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: true,
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              padding: 15,
              font: { size: 12 }
            }
          },
          tooltip: {
            callbacks: {
              label: (context: any) => {
                const label = context.label || '';
                const value = context.parsed || 0;
                const total = used + remaining;
                const percentage = total > 0 ? ((value / total) * 100).toFixed(1) : '0';
                return `${label}: ${value} requêtes (${percentage}%)`;
              }
            }
          }
        }
      }
    };

    // Note: quotaChartRef would need to be added with @ViewChild
    // For now, we'll skip the chart rendering
  }

  onFilterChange() {
    // Recharger les logs d'utilisation qui recalculeront automatiquement les stats
    this.loadOrganizationUsageLogs();
  }

  resetFilters() {
    this.startDate = '';
    this.endDate = '';
    // Recharger les logs d'utilisation qui recalculeront automatiquement les stats
    this.loadOrganizationUsageLogs();
  }

  formatDate(dateString: string): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('fr-FR', { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' });
  }

  formatRenewalDate(endDateString: string | null | undefined): string {
    if (!endDateString) return '';
    // La date de fin est incluse, le renouvellement se fait le jour suivant
    const endDate = new Date(endDateString);
    const renewalDate = new Date(endDate);
    renewalDate.setDate(renewalDate.getDate() + 1);
    // Formater seulement le jour et le mois
    return renewalDate.toLocaleDateString('fr-FR', { day: 'numeric', month: 'long' });
  }

  formatCurrency(amount: number): string {
    if (amount == null || isNaN(amount)) return '0.00';

    // Utiliser la devise du marché stockée dans currentCurrencyCode
    // Pour DZD, le symbole est placé après le montant
    if (this.currentCurrencyCode === 'DZD' || this.currentCurrencyCode === 'MAD') {
      return `${amount.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${this.currentCurrencySymbol}`;
    }

    // Pour les autres devises, utiliser Intl.NumberFormat ou le symbole avant
    try {
      return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: this.currentCurrencyCode }).format(amount);
    } catch (e) {
      // Si la devise n'est pas supportée par Intl, utiliser le symbole manuellement
      return `${this.currentCurrencySymbol}${amount.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
    }
  }

  formatCost(amount: number): string {
    if (amount == null || isNaN(amount)) return '0.00000';
    return new Intl.NumberFormat('fr-FR', {
      minimumFractionDigits: 5,
      maximumFractionDigits: 5
    }).format(amount);
  }

  isAdmin(): boolean {
    return this.authService.hasRole('ADMIN');
  }

  formatNumber(num: number): string {
    if (num == null || isNaN(num)) return '0';
    return new Intl.NumberFormat('fr-FR').format(num);
  }

  truncateSearchTerm(term: string): string {
    if (term.length > 50) {
      return term.substring(0, 47) + '...';
    }
    return term;
  }
}

