import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, Router } from '@angular/router';
import { PricingPlanService, PricingPlan } from '../../core/services/pricing-plan.service';
import { environment } from '../../../environments/environment';
import { QuoteRequestFormComponent } from './quote-request-form.component';
import { AuthService } from '../../core/services/auth.service';
import { CurrencyService } from '../../core/services/currency.service';
import { OrganizationAccountService } from '../../core/services/organization-account.service';
import { AccountContextService } from '../../core/services/account-context.service';
import { AsyncPipe } from '@angular/common';
import { switchMap } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
  selector: 'app-pricing-plans',
  standalone: true,
  imports: [CommonModule, RouterLink, QuoteRequestFormComponent, AsyncPipe],
  template: `
    <div class="pricing-container">
      <div class="pricing-header">
        <h1>Choisissez votre plan tarifaire</h1>
        <p>Sélectionnez le plan qui correspond le mieux à vos besoins
            <span class="quote-badge">💼 Devis personnalisé possible après inscription.</span>
          </p>
        @if (isAuthenticated) {
          <div class="quote-notice">
            💼 <strong>Vous pouvez demander un devis personnalisé</strong> adapté à vos besoins spécifiques.
          </div>
        }
      </div>

      @if (loading) {
        <div class="loading">Chargement des plans tarifaires...</div>
      } @else if (error) {
        <div class="error">{{ error }}</div>
      } @else {
        <div class="pricing-info-bar">
          Possibilité de changer de plan tarifaire et demande de devis personnalisé à tout moment.
        </div>
        <div class="pricing-plans-grid">
          @for (plan of plans; track plan.id) {
            <div class="pricing-plan-card" [class.featured]="plan.displayOrder === 2">
              @if (plan.displayOrder === 2) {
                <div class="popular-badge">Populaire</div>
              }
              <h3>{{ plan.name }}</h3>
              <div class="price">
                @if (plan.pricePerMonth !== null && plan.pricePerMonth !== undefined) {
                  @if (plan.pricePerMonth === 0) {
                    <span class="amount">Gratuit</span>
                  } @else {
                    @if (currencySymbol$ | async; as symbol) {
                      <span class="currency">{{ symbol }}</span>
                    } @else {
                      <span class="currency">{{ getCurrencySymbol(plan.currency) }}</span>
                    }
                    <span class="amount">{{ plan.pricePerMonth }}</span>
                    <span class="period">/mois</span>
                  }
                } @else if (plan.pricePerRequest !== null && plan.pricePerRequest !== undefined) {
                  @if (currencySymbol$ | async; as symbol) {
                    <span class="currency">{{ symbol }}</span>
                  } @else {
                    <span class="currency">{{ getCurrencySymbol(plan.currency) }}</span>
                  }
                  <span class="amount">{{ plan.pricePerRequest }}</span>
                  <span class="period">/requête</span>
                } @else {
                  <span class="amount">Gratuit</span>
                }
              </div>
              @if (plan.description) {
                <p class="description">{{ plan.description }}</p>
              }
              <div class="quota">
                @if (plan.trialPeriodDays) {
                  <strong>Valable {{ plan.trialPeriodDays }} jours</strong>
                } @else if (plan.monthlyQuota) {
                  <strong>{{ plan.monthlyQuota | number }} requêtes/mois</strong>
                } @else if (plan.pricePerRequest !== null && plan.pricePerRequest !== undefined) {
                  <strong>Facturation à la requête</strong>
                } @else {
                  <strong>Quota illimité</strong>
                }
              </div>
              @if (plan.features) {
                <div class="features">
                  <h4>Fonctionnalités :</h4>
                  <ul>
                    @for (feature of parseFeatures(plan.features); track feature) {
                      <li>{{ feature }}</li>
                    }
                  </ul>
                </div>
              }
              <a [routerLink]="['/auth/register']" [queryParams]="{ planId: plan.id }" class="btn btn-primary">
                Choisir ce plan
              </a>
            </div>
          }
        </div>
      }

      <div class="pricing-footer">
        <p>Tarifs de lancement valables jusqu'au 30/06/2026.</p>
        <div class="footer-actions">
          <button class="btn btn-secondary" (click)="openQuoteRequestForm()" *ngIf="isAuthenticated">
            Demander un devis personnalisé
          </button>
          <a routerLink="/auth/login" class="link">Déjà un compte ? Connectez-vous</a>
        </div>
      </div>
    </div>

    <app-quote-request-form
      [showForm]="showQuoteForm"
      (formClosed)="closeQuoteForm()"
      (quoteSubmitted)="onQuoteSubmitted()">
    </app-quote-request-form>
  `,
  styles: [`
    .pricing-container {
      padding: 3rem 2rem;
      max-width: 1200px;
      margin: 0 auto;
      min-height: calc(100vh - 200px);
    }

    .pricing-header {
      text-align: center;
      margin-bottom: 3rem;
    }

    .pricing-header h1 {
      font-size: 2.5rem;
      color: #2c3e50;
      margin-bottom: 1rem;
    }

    .pricing-header p {
      font-size: 1.2rem;
      color: #7f8c8d;
    }

    .quote-notice {
      margin-top: 1.5rem;
      padding: 1rem 1.5rem;
      background: linear-gradient(135deg, rgba(250, 204, 21, 0.15) 0%, rgba(250, 204, 21, 0.05) 100%);
      border: 2px solid rgba(250, 204, 21, 0.4);
      border-radius: 12px;
      color: #1f2937;
      font-size: 1rem;
      display: inline-block;
      box-shadow: 0 4px 12px rgba(250, 204, 21, 0.2);
    }

    .quote-notice strong {
      color: #facc15;
      font-weight: 700;
    }

    .pricing-plans-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 2rem;
      margin-bottom: 3rem;
    }

    .pricing-info-bar {
      text-align: center;
      padding: 0.75rem 1rem;
      margin-bottom: 2rem;
      background: #2c3e50;
      border: 1px solid #e1e8ed;
      border-radius: 6px;
      color:rgb(211, 225, 240);
      font-size: 0.9rem;
      font-style: italic;
      max-width: 800px;
      margin-left: auto;
      margin-right: auto;
    }

    .pricing-plan-card {
      background: #2c3e50;
      border-radius: 12px;
      padding: 1.5rem;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      position: relative;
      transition: transform 0.3s ease, box-shadow 0.3s ease;
      min-height: auto;
      height: auto;
    }

    .pricing-plan-card:hover {
      transform: translateY(-5px);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
    }

    .pricing-plan-card.featured {
      border: 3px solid #facc15;
      transform: scale(1.05);
      background: #34495e;
    }

    .popular-badge {
      position: absolute;
      top: -15px;
      right: 20px;
      background: #facc15;
      color: #1f2937;
      padding: 0.5rem 1rem;
      border-radius: 20px;
      font-size: 0.9rem;
      font-weight: 700;
      box-shadow: 0 2px 8px rgba(250, 204, 21, 0.4);
    }

    .pricing-plan-card h3 {
      font-size: 1.5rem;
      color: #ecf0f1;
      margin-bottom: 0.75rem;
    }

    .price {
      margin: 1rem 0;
      display: flex;
      align-items: baseline;
      justify-content: center;
    }

    .currency {
      font-size: 1.3rem;
      color: #bdc3c7;
      margin-right: 0.25rem;
    }

    .amount {
      font-size: 2.5rem;
      font-weight: 700;
      color: #ecf0f1;
    }

    .period {
      font-size: 1rem;
      color: #bdc3c7;
      margin-left: 0.25rem;
    }

    .description {
      color: #bdc3c7;
      margin-bottom: 0.75rem;
      text-align: center;
      font-size: 0.9rem;
    }

    .quota {
      text-align: center;
      margin: 1rem 0;
      padding: 0.75rem;
      background: #34495e;
      border-radius: 8px;
      color: #ecf0f1;
    }

    .features {
      margin: 1rem 0;
    }

    .features h4 {
      font-size: 1rem;
      color: #ecf0f1;
      margin-bottom: 0.5rem;
    }

    .features ul {
      list-style: none;
      padding: 0;
    }

    .features li {
      padding: 0.4rem 0;
      color: #bdc3c7;
      border-bottom: 1px solid #34495e;
      font-size: 0.9rem;
    }

    .features li:last-child {
      border-bottom: none;
    }

    .features li:before {
      content: "✓ ";
      color: #27ae60;
      font-weight: bold;
      margin-right: 0.5rem;
    }

    .btn {
      width: 100%;
      padding: 0.75rem;
      border: none;
      border-radius: 8px;
      font-size: 1rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.3s ease;
      text-decoration: none;
      display: block;
      text-align: center;
      margin-top: 1rem;
    }

    .btn-primary {
      background: linear-gradient(135deg, #3498db 0%, #2980b9 100%);
      color: white;
    }

    .btn-primary:hover {
      background: linear-gradient(135deg, #2980b9 0%, #1f6391 100%);
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(52, 152, 219, 0.3);
    }

    .pricing-footer {
      text-align: center;
      margin-top: 3rem;
      padding-top: 2rem;
      border-top: 1px solid #ecf0f1;
    }

    .pricing-footer p {
      color: #7f8c8d;
      margin-bottom: 1rem;
    }

    .footer-actions {
      display: flex;
      flex-direction: column;
      gap: 1rem;
      align-items: center;
    }

    .btn-secondary {
      background: linear-gradient(135deg, #95a5a6 0%, #7f8c8d 100%);
      color: white;
      padding: 0.75rem 1.5rem;
      border: none;
      border-radius: 8px;
      font-size: 1rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.3s ease;
    }

    .btn-secondary:hover {
      background: linear-gradient(135deg, #7f8c8d 0%, #6c7a7a 100%);
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(149, 165, 166, 0.3);
    }

    .link {
      color: #3498db;
      text-decoration: none;
      font-weight: 600;
    }

    .link:hover {
      text-decoration: underline;
    }

    .loading, .error {
      text-align: center;
      padding: 2rem;
      font-size: 1.2rem;
    }

    .error {
      color: #e74c3c;
    }

    @media (max-width: 768px) {
      .pricing-plans-grid {
        grid-template-columns: 1fr;
      }

      .pricing-plan-card.featured {
        transform: scale(1);
      }
    }
  `]
})
export class PricingPlansComponent implements OnInit {
  private pricingPlanService = inject(PricingPlanService);
  private authService = inject(AuthService);
  private currencyService = inject(CurrencyService);
  private organizationAccountService = inject(OrganizationAccountService);
  private accountContextService = inject(AccountContextService);
  private router = inject(Router);

  plans: PricingPlan[] = [];
  loading = true;
  error = '';
  showQuoteForm = false;
  isAuthenticated = false;
  currencySymbol$ = this.currencyService.getCurrencySymbol();

  ngOnInit() {
    // Précharger la devise pour qu'elle soit disponible immédiatement
    this.currencySymbol$.subscribe({
      next: (symbol) => {
        console.log('✅ PricingPlansComponent: Symbole de devise chargé:', symbol);
      },
      error: (err) => {
        console.error('❌ PricingPlansComponent: Erreur lors du chargement de la devise:', err);
      }
    });
    
    // Vérifier si l'utilisateur est authentifié
    this.authService.isAuthenticated().subscribe((isAuth: boolean) => {
      this.isAuthenticated = isAuth;
      
      // Si l'utilisateur est authentifié et qu'il s'agit d'une organisation avec un essai terminé, rediriger
      if (isAuth) {
        this.accountContextService.isOrganizationAccount$.pipe(
          switchMap(isOrg => {
            if (isOrg) {
              // Vérifier si l'essai est définitivement terminé
              return this.organizationAccountService.getMyOrganization();
            }
            return of(null);
          })
        ).subscribe({
          next: (org) => {
            if (org?.trialPermanentlyExpired) {
              // Rediriger vers la page de sélection de plan de l'organisation
              this.router.navigate(['/organization/stats']);
            }
          },
          error: (err) => {
            // Ignorer les erreurs (utilisateur peut ne pas être une organisation)
            console.debug('PricingPlansComponent: Pas une organisation ou erreur:', err);
          }
        });
      }
    });
    
    this.loadPricingPlans();
  }

  loadPricingPlans() {
    this.loading = true;
    this.error = '';
    // Le service récupère automatiquement marketVersion depuis l'environnement si non fourni
    this.pricingPlanService.getActivePricingPlans().subscribe({
      next: (plans) => {
        // Si une organisation avec un essai terminé accède à cette page, filtrer les plans gratuits et d'essai
        if (this.isAuthenticated) {
          this.accountContextService.isOrganizationAccount$.pipe(
            switchMap(isOrg => {
              if (isOrg) {
                return this.organizationAccountService.getMyOrganization();
              }
              return of(null);
            })
          ).subscribe({
            next: (org) => {
              if (org?.trialPermanentlyExpired) {
                // Filtrer les plans d'essai et les plans gratuits
                this.plans = plans.filter(plan => {
                  // Exclure les plans d'essai
                  if (plan.trialPeriodDays && plan.trialPeriodDays > 0) {
                    return false;
                  }
                  // Exclure les plans gratuits
                  const isFree = (plan.pricePerMonth === null || plan.pricePerMonth === undefined || plan.pricePerMonth === 0) 
                    && (plan.pricePerRequest === null || plan.pricePerRequest === undefined);
                  return !isFree;
                });
              } else {
                this.plans = plans;
              }
              this.loading = false;
              if (this.plans.length === 0) {
                this.error = 'Aucun plan tarifaire disponible pour le moment.';
              }
            },
            error: () => {
              // Pas une organisation ou erreur, afficher tous les plans
              this.plans = plans;
              this.loading = false;
              if (this.plans.length === 0) {
                this.error = 'Aucun plan tarifaire disponible pour le moment.';
              }
            }
          });
        } else {
          // Utilisateur non authentifié, afficher tous les plans
          this.plans = plans;
          this.loading = false;
          if (this.plans.length === 0) {
            this.error = 'Aucun plan tarifaire disponible pour le moment.';
          }
        }
        console.log('✅ Plans reçus:', this.plans.length, this.plans);
        console.log('✅ Market versions des plans reçus:', this.plans.map(p => ({ name: p.name, marketVersion: p.marketVersion })));
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des plans tarifaires: ' + (err.error?.message || err.message || 'Erreur inconnue');
        this.loading = false;
        console.error('Erreur:', err);
      }
    });
  }

  getCurrencySymbol(currency?: string): string {
    if (!currency) {
      // Si pas de devise fournie, utiliser la devise du marché depuis le service
      // Note: Cette méthode est synchrone, donc on ne peut pas utiliser l'Observable ici
      // Le template utilisera currencySymbol$ qui est asynchrone
      return '€'; // Par défaut EUR (fallback)
    }
    const currencyMap: { [key: string]: string } = {
      'EUR': '€',
      'DZD': 'DA',
      'USD': '$',
      'GBP': '£',
      'JPY': '¥',
      'CNY': '¥',
      'CHF': 'CHF',
      'CAD': 'C$',
      'AUD': 'A$',
      'MAD': 'DH'
    };
    return currencyMap[currency.toUpperCase()] || currency;
  }

  parseFeatures(features: string): string[] {
    try {
      // Essayer de parser comme JSON
      const parsed = JSON.parse(features);
      if (Array.isArray(parsed)) {
        return parsed;
      }
    } catch (e) {
      // Si ce n'est pas du JSON, traiter comme texte séparé par des lignes
      return features.split('\n').filter(f => f.trim().length > 0);
    }
    return [];
  }

  openQuoteRequestForm() {
    if (!this.isAuthenticated) {
      // Rediriger vers la page de connexion si non authentifié
      // L'utilisateur pourra revenir après connexion
      return;
    }
    this.showQuoteForm = true;
  }

  closeQuoteForm() {
    this.showQuoteForm = false;
  }

  onQuoteSubmitted() {
    // Le formulaire se ferme automatiquement après soumission réussie
    // On pourrait aussi recharger les plans ou afficher un message
  }
}

