import { Component, OnInit, inject, OnDestroy } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { Observable, interval, Subscription, combineLatest } from 'rxjs';
import { AuthService } from '../../../core/services/auth.service';
import { AlertService } from '../../../core/services/alert.service';
import { InvoiceService } from '../../../core/services/invoice.service';
import { NotificationService } from '../../../core/services/notification.service';
import { MarketProfileService } from '../../../core/services/market-profile.service';
import { OrganizationAccountService } from '../../../core/services/organization-account.service';
import { environment } from '../../../../environments/environment';
import { AsyncPipe, CommonModule } from '@angular/common';
import { take, map, catchError, switchMap } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, AsyncPipe],
  template: `
    <nav class="navbar">
      <div class="nav-brand">
        <a routerLink="/">
          Enclume-Numérique
          @if (countryCode$ | async; as countryCode) {
            <span class="brand-country-code">{{ countryCode }}</span>
          }
        </a>
      </div>

      <div class="nav-links">
        @if (isAuthenticated$ | async) {
          @if ((isOrganizationAccount$ | async) || (isCollaboratorAccount$ | async)) {
            @if (canMakeRequests$ | async) {
              <a routerLink="/" class="nav-link">HS-code</a>
            } @else {
              <span class="nav-link disabled" title="Le quota de l'essai gratuit de votre organisation a été atteint. Aucune requête HS-code n'est autorisée. Veuillez contacter votre administrateur d'organisation pour choisir un plan tarifaire.">HS-code</span>
            }
          } @else {
            <a routerLink="/" class="nav-link">Accueil</a>
          }
        } @else {
          <a routerLink="/" class="nav-link">Accueil</a>
        }
        @if (!(isCollaboratorAccount$ | async)) {
          <a routerLink="/pricing" class="nav-link pricing-link">
            💳 Tarifs
          </a>
        }
        @if (isAuthenticated$ | async) {
          @if (isOrganizationAccount$ | async) {
            <a routerLink="/organization/account" class="nav-link">Mon organisation</a>
            @if (!isAdmin()) {
              <a routerLink="/dashboard" class="nav-link">Tableau de bord</a>
            }
            @if (alertCount > 0) {
              <a routerLink="/alerts" class="nav-link alerts-link">
                🔔 Alertes
                <span class="alert-badge">{{ alertCount }}</span>
              </a>
            }
          } @else if (isCollaboratorAccount$ | async) {
            @if (!isAdmin()) {
              <a routerLink="/dashboard" class="nav-link">Tableau de bord</a>
            }
            @if (alertCount > 0) {
              <a routerLink="/alerts" class="nav-link alerts-link">
                🔔 Alertes
                <span class="alert-badge">{{ alertCount }}</span>
              </a>
            }
          }
        }
      </div>

      <div class="nav-auth">
        @if (isAuthenticated$ | async) {
          <span class="user-info">
            Bienvenue {{ getUserInfo()?.preferred_username }}
          </span>
          <button (click)="logout()" class="btn btn-outline">Déconnexion</button>
        } @else {
          <button (click)="goToRegister()" class="btn btn-secondary">Créer un compte</button>
          <button (click)="login()" class="btn btn-primary">Connexion</button>
        }
      </div>
    </nav>
    
    @if ((isAuthenticated$ | async) && ((isOrganizationAccount$ | async) || (isCollaboratorAccount$ | async))) {
      @if (!(canMakeRequests$ | async)) {
        <div class="trial-expired-banner">
          <p>
            ⚠️ 
            @if (isOrganizationAccount$ | async) {
              Le quota de votre essai gratuit a été atteint et est maintenant définitivement désactivé pour votre organisation. 
              Aucune requête HS-code n'est autorisée pour tous les collaborateurs. 
              Veuillez <a routerLink="/organization/stats">choisir un plan tarifaire</a> ou <a routerLink="/organization/quote-requests">faire une demande de devis</a> pour continuer à utiliser le service.
            } @else {
              Le quota de l'essai gratuit de votre organisation a été atteint et est maintenant définitivement désactivé. 
              Aucune requête HS-code n'est autorisée. 
              Veuillez contacter votre administrateur d'organisation pour choisir un plan tarifaire ou faire une demande de devis.
            }
          </p>
        </div>
      }
      <nav class="organization-navbar">
        <div class="org-nav-links">
          <a routerLink="/organization/account" routerLinkActive="router-link-active" class="org-nav-link">👥 Collaborateurs</a>
          <a routerLink="/organization/stats" routerLinkActive="router-link-active" class="org-nav-link">💳 Plan tarifaire</a>
          <a routerLink="/organization/stats" routerLinkActive="router-link-active" class="org-nav-link">📊 Statistiques globales</a>
          <a routerLink="/organization/invoices" routerLinkActive="router-link-active" class="org-nav-link invoices-link">
            📄 Factures
            @if (newInvoicesCount > 0 || overdueInvoicesCount > 0) {
              <span class="invoice-badge" [class.overdue-badge]="overdueInvoicesCount > 0">
                @if (overdueInvoicesCount > 0) {
                  ⚠️ {{ overdueInvoicesCount }}
                } @else {
                  {{ newInvoicesCount }}
                }
              </span>
            }
          </a>
          <a routerLink="/organization/quote-requests" routerLinkActive="router-link-active" class="org-nav-link">💼 Demandes de devis</a>
        </div>
      </nav>
    }
    
    @if (isAuthenticated$ | async) {
      @if (isAdmin()) {
        <nav class="admin-navbar">
          <div class="admin-nav-links">
            <a routerLink="/admin/stats" class="admin-nav-link">📊 Stats</a>
            <a routerLink="/admin/organizations" class="admin-nav-link">🏢 Organisations</a>
            <a routerLink="/admin/invoices" class="admin-nav-link">📄 Factures (Admin)</a>
            <a routerLink="/admin/quote-requests" routerLinkActive="router-link-active" class="admin-nav-link">💼 Demandes de devis</a>
            <a routerLink="/admin/pending-registrations" class="admin-nav-link">⏳ Inscriptions en attente</a>
          </div>
        </nav>
      }
    }
  `,
  styles: [`
    .navbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1rem 2rem;
      background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
      color: white;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      position: relative;
      z-index: 1000;
    }

    .navbar::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 3px;
      background: linear-gradient(90deg, #3498db, #2ecc71, #3498db);
      background-size: 200% 100%;
      animation: shimmer 3s linear infinite;
    }

    @keyframes shimmer {
      0% { background-position: -200% 0; }
      100% { background-position: 200% 0; }
    }

    .nav-brand a {
      color: white;
      text-decoration: none;
      font-size: 1.5rem;
      font-weight: 700;
      letter-spacing: 0.5px;
      transition: all 0.3s ease;
      text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.2);
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .nav-brand a:hover {
      transform: translateY(-2px);
      text-shadow: 2px 4px 8px rgba(0, 0, 0, 0.3);
    }

    .brand-country-code {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: white;
      font-weight: 700;
      font-size: 0.75rem;
      color: #1e3c72;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    }

    .nav-links {
      display: flex;
      gap: 1rem;
    }

    .nav-link {
      color: white;
      text-decoration: none;
      padding: 0.6rem 1.2rem;
      border-radius: 6px;
      transition: all 0.3s ease;
      position: relative;
      font-weight: 500;
      overflow: hidden;
    }

    .nav-link::before {
      content: '';
      position: absolute;
      top: 0;
      left: -100%;
      width: 100%;
      height: 100%;
      background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
      transition: left 0.5s ease;
    }

    .nav-link:hover::before {
      left: 100%;
    }

    .nav-link:hover {
      background-color: rgba(255, 255, 255, 0.15);
      transform: translateY(-2px);
      box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
    }

    .nav-link.disabled {
      opacity: 0.5;
      cursor: not-allowed;
      text-decoration: line-through;
      pointer-events: none;
    }

    .nav-link.disabled:hover {
      background-color: transparent;
      transform: none;
      box-shadow: none;
    }

    .nav-auth {
      display: flex;
      align-items: center;
      gap: 1rem;
    }

    .user-info {
      margin-right: 1rem;
      font-weight: 500;
      padding: 0.5rem 1rem;
      background: rgba(255, 255, 255, 0.1);
      border-radius: 20px;
      backdrop-filter: blur(10px);
      border: 1px solid rgba(255, 255, 255, 0.2);
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .user-info:has(.country-code:only-child) {
      padding: 0.5rem;
    }

    .country-code {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: white;
      font-weight: 700;
      font-size: 0.75rem;
      color: #1e3c72;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    }

    .btn {
      padding: 0.6rem 1.4rem;
      border: none;
      border-radius: 6px;
      cursor: pointer;
      transition: all 0.3s ease;
      font-weight: 600;
      letter-spacing: 0.3px;
      position: relative;
      overflow: hidden;
    }

    .btn::before {
      content: '';
      position: absolute;
      top: 50%;
      left: 50%;
      width: 0;
      height: 0;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.3);
      transform: translate(-50%, -50%);
      transition: width 0.6s, height 0.6s;
    }

    .btn:hover::before {
      width: 300px;
      height: 300px;
    }

    .btn-primary {
      background: linear-gradient(135deg, #3498db 0%, #2980b9 100%);
      color: white;
      box-shadow: 0 4px 12px rgba(52, 152, 219, 0.3);
    }

    .btn-primary:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 16px rgba(52, 152, 219, 0.4);
    }

    .btn-outline {
      background-color: transparent;
      border: 2px solid white;
      color: white;
      box-shadow: 0 4px 12px rgba(255, 255, 255, 0.1);
    }

    .btn-outline:hover {
      background-color: rgba(255, 255, 255, 0.15);
      transform: translateY(-2px);
      box-shadow: 0 6px 16px rgba(255, 255, 255, 0.2);
    }

    .btn-secondary {
      background-color: transparent;
      border: 2px solid white;
      color: white;
      box-shadow: 0 4px 12px rgba(255, 255, 255, 0.1);
    }

    .btn-secondary:hover {
      background-color: rgba(255, 255, 255, 0.2);
      transform: translateY(-2px);
      box-shadow: 0 6px 16px rgba(255, 255, 255, 0.25);
    }

    .btn:active {
      transform: translateY(0);
    }

    .alerts-link, .invoices-link, .pricing-link {
      position: relative;
    }

    .pricing-link {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
    }

    .quote-badge {
      font-size: 0.65rem;
      font-weight: 600;
      color: #facc15;
      background: rgba(250, 204, 21, 0.2);
      padding: 0.25rem 0.6rem;
      border-radius: 12px;
      white-space: nowrap;
      border: 1px solid rgba(250, 204, 21, 0.4);
      text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
      box-shadow: 0 2px 4px rgba(250, 204, 21, 0.2);
    }

    .alert-badge, .invoice-badge {
      position: absolute;
      top: -5px;
      right: -5px;
      background: #e74c3c;
      color: white;
      border-radius: 50%;
      width: 20px;
      height: 20px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 0.7rem;
      font-weight: 700;
      border: 2px solid white;
      animation: pulse 2s infinite;
    }

    .invoice-badge {
      background: #3498db;
    }

    .invoice-badge.overdue-badge {
      background: #e74c3c;
      animation: pulse-red 2s infinite;
    }

    .trial-expired-banner {
      background: linear-gradient(135deg, #f39c12 0%, #e67e22 100%);
      color: white;
      padding: 1rem 2rem;
      text-align: center;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
      z-index: 997;
      position: relative;
    }

    .trial-expired-banner p {
      margin: 0;
      font-weight: 600;
      font-size: 0.95rem;
    }

    .trial-expired-banner a {
      color: white;
      text-decoration: underline;
      font-weight: 700;
    }

    .trial-expired-banner a:hover {
      text-decoration: none;
      opacity: 0.9;
    }

    @keyframes pulse-red {
      0%, 100% {
        transform: scale(1);
        box-shadow: 0 0 0 0 rgba(231, 76, 60, 0.7);
      }
      50% {
        transform: scale(1.1);
        box-shadow: 0 0 0 5px rgba(231, 76, 60, 0);
      }
    }
    
    @keyframes pulse {
      0%, 100% {
        transform: scale(1);
      }
      50% {
        transform: scale(1.1);
      }
    }

    .admin-navbar {
      display: flex;
      justify-content: center;
      align-items: center;
      padding: 0.75rem 2rem;
      background: linear-gradient(135deg, #2c3e50 0%, #34495e 100%);
      color: white;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      position: relative;
      z-index: 999;
    }

    .admin-nav-links {
      display: flex;
      gap: 1rem;
      flex-wrap: wrap;
      justify-content: center;
    }

    .admin-nav-link {
      color: white;
      text-decoration: none;
      padding: 0.5rem 1rem;
      border-radius: 6px;
      transition: all 0.3s ease;
      position: relative;
      font-weight: 500;
      font-size: 0.95rem;
    }

    .admin-nav-link:hover {
      background-color: rgba(255, 255, 255, 0.15);
      transform: translateY(-2px);
      box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
    }

    .organization-navbar {
      position: fixed;
      left: 0;
      top: 0;
      width: 220px;
      height: 100vh;
      padding: 1rem 0;
      background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
      color: white;
      box-shadow: 4px 0 12px rgba(0, 0, 0, 0.15);
      z-index: 998;
      overflow-y: auto;
      padding-top: 80px; /* Espace pour la navbar principale */
      box-sizing: border-box;
    }

    .organization-navbar::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 3px;
      background: linear-gradient(90deg, #3498db, #2ecc71, #3498db);
      background-size: 200% 100%;
      animation: shimmer 3s linear infinite;
    }

    .org-nav-links {
      display: flex;
      flex-direction: column;
      gap: 1rem;
      padding: 0 1rem;
    }

    .org-nav-link {
      color: white;
      text-decoration: none;
      padding: 0.6rem 1.2rem;
      border-radius: 6px;
      transition: all 0.3s ease;
      position: relative;
      font-weight: 500;
      overflow: hidden;
      display: flex;
      align-items: center;
      gap: 0.75rem;
      width: 100%;
      box-sizing: border-box;
    }

    .org-nav-link::before {
      content: '';
      position: absolute;
      top: 0;
      left: -100%;
      width: 100%;
      height: 100%;
      background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
      transition: left 0.5s ease;
    }

    .org-nav-link:hover::before {
      left: 100%;
    }

    .org-nav-link:hover {
      background-color: rgba(255, 255, 255, 0.15);
      transform: translateX(4px);
      box-shadow: 0 4px 8px rgba(0, 0, 0, 0.2);
    }

    .org-nav-link:active {
      transform: translateX(2px);
    }

    .org-nav-link.router-link-active {
      background-color: rgba(255, 255, 255, 0.2);
    }

    .org-nav-link.invoices-link {
      position: relative;
    }

    .org-nav-link .invoice-badge {
      position: absolute;
      top: -5px;
      right: -5px;
      background: #3498db;
      color: white;
      border-radius: 50%;
      width: 20px;
      height: 20px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 0.7rem;
      font-weight: 700;
      border: 2px solid rgba(255, 255, 255, 0.3);
      animation: pulse 2s infinite;
    }

    .org-nav-link .invoice-badge.overdue-badge {
      background: #e74c3c;
      animation: pulse-red 2s infinite;
    }

    @media (max-width: 768px) {
      .navbar {
        flex-wrap: wrap;
        padding: 1rem;
      }

      .nav-links {
        flex-wrap: wrap;
        gap: 0.5rem;
      }

      .nav-link {
        padding: 0.5rem 0.8rem;
        font-size: 0.9rem;
      }

      .admin-navbar {
        padding: 0.5rem 1rem;
      }

      .admin-nav-links {
        gap: 0.5rem;
      }

      .admin-nav-link {
        padding: 0.4rem 0.8rem;
        font-size: 0.85rem;
      }

      .organization-navbar {
        width: 180px;
        padding-top: 70px;
      }

      .org-nav-links {
        gap: 0.4rem;
        padding: 0 0.75rem;
      }

      .org-nav-link {
        padding: 0.75rem 1rem;
        font-size: 0.85rem;
      }
    }
  `]
})
export class NavbarComponent implements OnInit, OnDestroy {
  private authService = inject(AuthService);
  private router = inject(Router);
  private alertService = inject(AlertService);
  private invoiceService = inject(InvoiceService);
  private notificationService = inject(NotificationService);
  private marketProfileService = inject(MarketProfileService);
  private organizationAccountService = inject(OrganizationAccountService);

  isAuthenticated$!: Observable<boolean>;
  isOrganizationAccount$!: Observable<boolean>;
  isCollaboratorAccount$!: Observable<boolean>;
  countryCode$!: Observable<string | null>;
  canMakeRequests$!: Observable<boolean>;
  alertCount = 0;
  newInvoicesCount = 0;
  overdueInvoicesCount = 0;
  private previousInvoicesCount: number | null = null;
  private previousOverdueInvoicesCount: number | null = null;
  private refreshSubscription?: Subscription;

  ngOnInit() {
    this.isAuthenticated$ = this.authService.isAuthenticated();
    this.isOrganizationAccount$ = this.authService.isOrganizationAccount();
    this.isCollaboratorAccount$ = this.authService.isCollaboratorAccount();
    
    // Vérifier si l'organisation peut faire des requêtes (essai non expiré)
    // Par défaut, on suppose que les requêtes sont autorisées
    this.canMakeRequests$ = combineLatest([
      this.isOrganizationAccount$,
      this.isCollaboratorAccount$
    ]).pipe(
      switchMap(([isOrg, isCollab]) => {
        const hasOrgOrCollabAccount = isOrg || isCollab;
        if (!hasOrgOrCollabAccount) {
          // Pas d'organisation, donc pas de restriction
          return of(true);
        }
        // Vérifier l'état de l'organisation
        return this.organizationAccountService.getOrganizationStatus().pipe(
          map(status => status.canMakeRequests),
          catchError(err => {
            console.error('Erreur lors de la vérification de l\'état de l\'organisation:', err);
            // En cas d'erreur, autoriser par défaut pour ne pas bloquer l'interface
            return of(true);
          })
        );
      })
    );
    
    // Charger le code pays du profil de marché configuré dans l'environnement
    // Le profil de marché est défini par la variable d'environnement MARKET_VERSION
    if (environment.marketVersion) {
      this.countryCode$ = this.marketProfileService.getMarketProfileByVersion(environment.marketVersion).pipe(
        map(profile => profile.countryCodeIsoAlpha2),
        catchError(err => {
          console.error('Erreur lors du chargement du profil de marché:', err);
          return of(null);
        })
      );
    } else {
      this.countryCode$ = of(null);
    }
    
    this.loadAlertCount();
    this.loadNewInvoicesCount();
    this.loadOverdueInvoicesCount();
    // Rafraîchir les compteurs toutes les 30 secondes
    this.refreshSubscription = interval(30000).subscribe(() => {
      this.loadAlertCount();
      this.loadNewInvoicesCount();
      this.loadOverdueInvoicesCount();
    });
  }

  ngOnDestroy() {
    if (this.refreshSubscription) {
      this.refreshSubscription.unsubscribe();
    }
  }

  loadAlertCount() {
    // Charger les alertes pour les comptes organisation et collaborateur
    this.isOrganizationAccount$.pipe(take(1)).subscribe(isOrganization => {
      if (isOrganization) {
        this.alertService.getMyAlertsCount().subscribe({
          next: (response) => {
            this.alertCount = response.count;
          },
          error: (err) => {
            console.error('Erreur lors du chargement du compteur d\'alertes:', err);
            this.alertCount = 0;
          }
        });
      } else {
        // Vérifier si c'est un collaborateur
        this.isCollaboratorAccount$.pipe(take(1)).subscribe(isCollaborator => {
          if (isCollaborator) {
            this.alertService.getMyAlertsCount().subscribe({
              next: (response) => {
                this.alertCount = response.count;
              },
              error: (err) => {
                console.error('Erreur lors du chargement du compteur d\'alertes:', err);
                this.alertCount = 0;
              }
            });
          } else {
            this.alertCount = 0;
          }
        });
      }
    });
  }

  loadNewInvoicesCount() {
    this.isOrganizationAccount$.pipe(take(1)).subscribe(isOrganization => {
      if (isOrganization) {
        this.invoiceService.getNewInvoicesCount().subscribe({
          next: (response) => {
            const newCount = response.count;
            
            // Afficher une notification si une nouvelle facture est détectée
            // (seulement après la première vérification, pour éviter de notifier au chargement initial)
            if (this.previousInvoicesCount !== null && newCount > this.previousInvoicesCount) {
              const diff = newCount - this.previousInvoicesCount;
              if (diff === 1) {
                this.notificationService.info(`Une nouvelle facture est disponible !`);
              } else {
                this.notificationService.info(`${diff} nouvelles factures sont disponibles !`);
              }
            }
            
            this.newInvoicesCount = newCount;
            // Initialiser ou mettre à jour le compteur précédent
            if (this.previousInvoicesCount === null) {
              // Première vérification : initialiser sans notifier
              this.previousInvoicesCount = newCount;
            } else {
              // Mettre à jour pour les vérifications suivantes
              this.previousInvoicesCount = newCount;
            }
          },
          error: (err) => {
            console.error('Erreur lors du chargement du compteur de factures:', err);
            this.newInvoicesCount = 0;
          }
        });
      } else {
        this.newInvoicesCount = 0;
        this.previousInvoicesCount = null;
      }
    });
  }

  loadOverdueInvoicesCount() {
    this.isOrganizationAccount$.pipe(take(1)).subscribe(isOrganization => {
      if (isOrganization) {
        this.invoiceService.getOverdueInvoicesCount().subscribe({
          next: (response) => {
            const newCount = response.count;
            
            // Afficher une notification si une nouvelle facture en retard est détectée
            if (this.previousOverdueInvoicesCount !== null && newCount > this.previousOverdueInvoicesCount) {
              const diff = newCount - this.previousOverdueInvoicesCount;
              if (diff === 1) {
                this.notificationService.warning(`Une facture est maintenant en retard !`);
              } else {
                this.notificationService.warning(`${diff} factures sont maintenant en retard !`);
              }
            }
            
            this.overdueInvoicesCount = newCount;
            if (this.previousOverdueInvoicesCount === null) {
              this.previousOverdueInvoicesCount = newCount;
            } else {
              this.previousOverdueInvoicesCount = newCount;
            }
          },
          error: (err) => {
            console.error('Erreur lors du chargement du compteur de factures en retard:', err);
            this.overdueInvoicesCount = 0;
          }
        });
      } else {
        this.overdueInvoicesCount = 0;
        this.previousOverdueInvoicesCount = null;
      }
    });
  }

  login() {
    this.router.navigate(['/auth/login']);
  }

  goToRegister() {
    this.router.navigate(['/auth/register']);
  }

  logout() {
    this.authService.logout();
  }

  /*login() {
    this.authService.login();
  }
  logout() {
    this.authService.logout();
    this.router.navigate(['/']);
  }*/

  getUserInfo() {
    return this.authService.getUserInfo();
  }

  isAdmin(): boolean {
    return this.authService.hasRole('ADMIN');
  }
}
