import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserService, UserUsageStats, UserQuota, Organization } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { CurrencyService } from '../../../core/services/currency.service';
import { take } from 'rxjs/operators';

@Component({
  selector: 'app-user-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="dashboard-container">
      <h2>📊 Mon Tableau de Bord</h2>

      <!-- Informations utilisateur -->
      <div class="user-info-card">
        <h3>👤 Informations</h3>
        <p><strong>Nom d'utilisateur:</strong> {{ userInfo?.preferred_username || 'N/A' }}</p>
        <p><strong>Email:</strong> {{ userInfo?.email || 'N/A' }}</p>
      </div>

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
        } @else {
          <p class="no-org-message">Vous n'êtes associé à aucune organisation.</p>
        }
      </div>


      <!-- Utilisation Organisation Ce Mois -->
      <div class="quota-card" *ngIf="quota || loadingQuota">
        <h3>📊 Utilisation Organisation Ce Mois</h3>
        @if (loadingQuota) {
          <p>Chargement...</p>
        } @else if (quota) {
          @if (!quota.hasOrganization) {
            <p class="no-org-message">{{ quota.message }}</p>
          } @else {
            <div class="org-usage-details">
              <div class="org-usage-stats">
                <div class="org-usage-item">
                  <span class="org-usage-label">Requêtes ce mois :</span>
                  <span class="org-usage-value">{{ quota.currentUsage || 0 }}</span>
                </div>
                <div class="org-usage-item">
                  <span class="org-usage-label">Quota mensuel :</span>
                  <span class="org-usage-value">
                    @if (quota.isUnlimited) {
                      Illimité
                    } @else {
                      {{ quota.monthlyQuota || 'Non défini' }}
                    }
                  </span>
                </div>
                @if (!quota.isUnlimited && quota.monthlyQuota) {
                  <div class="org-usage-item">
                    <span class="org-usage-label">Requêtes restantes :</span>
                    <span class="org-usage-value" [class.quota-warning]="getOrgPercentage() >= 80" [class.quota-danger]="getOrgPercentage() >= 100">
                      {{ Math.max(0, quota.monthlyQuota - (quota.currentUsage || 0)) }}
                    </span>
                  </div>
                  <div class="org-usage-item">
                    <span class="org-usage-label">Pourcentage utilisé :</span>
                    <span class="org-usage-value" [class.quota-warning]="getOrgPercentage() >= 80" [class.quota-danger]="getOrgPercentage() >= 100">
                      {{ getOrgPercentage().toFixed(1) }}%
                    </span>
                  </div>
                }
              </div>
              @if (!quota.isUnlimited && quota.monthlyQuota && getOrgPercentage() >= 100) {
                <p class="quota-exceeded-message">❌ Le quota mensuel de l'organisation a été dépassé.</p>
              }
            </div>
          }
        }
      </div>

      <!-- Statistiques d'utilisation -->
      <div class="stats-card" *ngIf="stats || loadingStats">
        <h3>📊 Mes Statistiques</h3>
        @if (loadingStats) {
          <p>Chargement...</p>
        } @else if (stats) {
          <div class="stats-grid">
            <div class="stat-item">
              <h4>📈 Total Requêtes</h4>
              <p class="stat-value">{{ stats.totalRequests }}</p>
            </div>
            <div class="stat-item">
              <h4>💰 Coût Total</h4>
              <p class="stat-value">{{ formatCurrency(stats.totalCostUsd) }}</p>
            </div>
            <div class="stat-item">
              <h4>🔢 Tokens Total</h4>
              <p class="stat-value">{{ formatNumber(stats.totalTokens) }}</p>
            </div>
            <div class="stat-item">
              <h4>📅 Requêtes ce mois</h4>
              <p class="stat-value">{{ stats.monthlyRequests }}</p>
            </div>
          </div>

          <!-- Filtres -->
          <div class="filters">
            <div class="filter-group">
              <label for="startDate">Date de début:</label>
              <input type="date" id="startDate" [(ngModel)]="startDate" (change)="loadStats()" />
            </div>
            <div class="filter-group">
              <label for="endDate">Date de fin:</label>
              <input type="date" id="endDate" [(ngModel)]="endDate" (change)="loadStats()" />
            </div>
            <button class="btn btn-secondary" (click)="resetFilters()">Réinitialiser</button>
          </div>

          <!-- Utilisations récentes -->
          @if (stats.recentUsage && stats.recentUsage.length > 0) {
            <div class="recent-usage">
              <h4>🔍 Utilisations Récentes</h4>
              <table class="usage-table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Endpoint</th>
                    <th>Recherche</th>
                    @if (isAdmin()) {
                      <th>Tokens</th>
                    }
                    <th>Coût</th>
                  </tr>
                </thead>
                <tbody>
                  @for (usage of stats.recentUsage; track usage.id) {
                    <tr>
                      <td>{{ formatDate(usage.timestamp) }}</td>
                      <td>{{ usage.endpoint }}</td>
                      <td class="search-term">{{ truncateSearchTerm(usage.searchTerm) }}</td>
                      @if (isAdmin()) {
                        <td>{{ formatNumber(usage.tokensUsed) }}</td>
                      }
                      <td>{{ formatCurrency(usage.costUsd || 0) }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          } @else {
            <p class="empty-message">Aucune utilisation récente.</p>
          }
        }
      </div>

      <!-- Message d'erreur -->
      @if (errorMessage) {
        <div class="error-message">{{ errorMessage }}</div>
      }
    </div>
  `,
  styles: [`
    .dashboard-container {
      padding: 2rem;
      max-width: 1200px;
      margin: 0 auto;
    }

    h2 {
      color: #2c3e50;
      margin-bottom: 2rem;
    }

    h3 {
      color: #2c3e50;
      margin-top: 0;
      margin-bottom: 1rem;
    }

    .user-info-card,
    .organization-card,
    .quota-card,
    .stats-card {
      background: #e0e0e0;
      border-radius: 8px;
      padding: 1.5rem;
      margin-bottom: 1.5rem;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    }

    .org-details p,
    .user-info-card p {
      margin: 0.5rem 0;
      color: #555;
    }

    .no-org-message {
      color: #888;
      font-style: italic;
    }

    .quota-details {
      margin-top: 1rem;
    }

    .quota-unlimited {
      padding: 1rem;
      background: #e8f5e9;
      border-radius: 4px;
    }

    .quota-status {
      font-weight: 600;
      color: #2e7d32;
      margin: 0.5rem 0;
    }

    .quota-limited {
      padding: 1rem;
    }

    .quota-progress {
      margin: 1rem 0;
    }

    .quota-progress-bar {
      width: 100%;
      height: 30px;
      background: #e0e0e0;
      border-radius: 15px;
      overflow: hidden;
      margin-bottom: 0.5rem;
    }

    .quota-progress-fill {
      height: 100%;
      background: linear-gradient(90deg, #4caf50, #8bc34a);
      transition: width 0.3s ease;
    }

    .quota-progress-fill.quota-warning {
      background: linear-gradient(90deg, #ff9800, #ffc107);
    }

    .quota-progress-fill.quota-danger {
      background: linear-gradient(90deg, #f44336, #ef5350);
    }

    .quota-text {
      text-align: center;
      font-weight: 600;
      color: #2c3e50;
      margin: 0.5rem 0;
    }

    .quota-remaining {
      text-align: center;
      margin-top: 0.5rem;
    }

    .quota-remaining-text {
      color: #ff9800;
      font-weight: 600;
    }

    .quota-exceeded {
      color: #f44336;
      font-weight: 600;
    }

    .quota-usage {
      color: #666;
      margin: 0.5rem 0;
    }

    .quota-usage-info {
      text-align: center;
      margin: 0.5rem 0;
      font-size: 0.9rem;
    }

    .quota-usage-text {
      color: #666;
      margin: 0 0.5rem;
    }

    .org-usage-details {
      margin-top: 1rem;
    }

    .org-usage-stats {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .org-usage-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 0.75rem 1rem;
      background: #d0d0d0;
      border-radius: 6px;
    }

    .org-usage-label {
      font-weight: 500;
      color: #555;
    }

    .org-usage-value {
      font-weight: 600;
      font-size: 1.1rem;
      color: #2c3e50;
    }

    .org-usage-value.quota-warning {
      color: #ff9800;
    }

    .org-usage-value.quota-danger {
      color: #f44336;
    }

    .quota-exceeded-message {
      margin-top: 1rem;
      padding: 0.75rem;
      background: #ffebee;
      border-radius: 6px;
      color: #c62828;
      font-weight: 600;
      text-align: center;
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 1rem;
      margin: 1.5rem 0;
    }

    .stat-item {
      background: #d0d0d0;
      padding: 1rem;
      border-radius: 8px;
      text-align: center;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    }

    .stat-item h4 {
      margin: 0 0 0.5rem 0;
      color: #666;
      font-size: 0.9rem;
    }

    .stat-value {
      font-size: 1.5rem;
      font-weight: 600;
      color: #2c3e50;
      margin: 0;
    }

    .filters {
      display: flex;
      gap: 1rem;
      align-items: flex-end;
      margin: 1.5rem 0;
      flex-wrap: wrap;
    }

    .filter-group {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .filter-group label {
      font-weight: 600;
      color: #2c3e50;
      font-size: 0.9rem;
    }

    .filter-group input {
      padding: 0.5rem;
      border: 1px solid #ddd;
      border-radius: 4px;
    }

    .recent-usage {
      margin-top: 2rem;
    }

    .recent-usage h4 {
      margin-bottom: 1rem;
    }

    .usage-table {
      width: 100%;
      border-collapse: collapse;
      background: white;
      border-radius: 4px;
      overflow: hidden;
    }

    .usage-table th,
    .usage-table td {
      padding: 0.75rem;
      text-align: left;
      border-bottom: 1px solid #ddd;
    }

    .usage-table th {
      background: #d5d5d5;
      font-weight: 600;
      color: #2c3e50;
    }

    .search-term {
      max-width: 200px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .btn {
      padding: 0.6rem 1.2rem;
      border: none;
      border-radius: 6px;
      cursor: pointer;
      font-weight: 600;
      transition: all 0.3s ease;
    }

    .btn-secondary {
      background: #95a5a6;
      color: white;
    }

    .btn-secondary:hover {
      background: #7f8c8d;
    }

    .empty-message {
      color: #888;
      font-style: italic;
      text-align: center;
      padding: 2rem;
    }

    .error-message {
      background: #e74c3c;
      color: white;
      padding: 1rem;
      border-radius: 4px;
      margin-top: 1rem;
    }

    .chart-section {
      margin: 2rem 0;
      padding: 1.5rem;
      background: #e0e0e0;
      border-radius: 8px;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    }

    .chart-section h4 {
      margin: 0 0 1rem 0;
      color: #2c3e50;
    }

    .chart-wrapper {
      position: relative;
      height: 300px;
    }

    .quota-chart-wrapper {
      display: flex;
      justify-content: center;
      align-items: center;
      margin: 1rem 0;
      height: 200px;
    }

    .quota-chart-wrapper canvas {
      max-width: 200px;
      max-height: 200px;
    }

    .pricing-plan-card {
      background: white;
      border-radius: 12px;
      padding: 2rem;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      margin-bottom: 2rem;
    }

    .pricing-plan-card h3 {
      margin-top: 0;
      margin-bottom: 1.5rem;
      color: #2c3e50;
    }

    .current-plan {
      margin-bottom: 2rem;
      padding: 1rem;
      background: #f8f9fa;
      border-radius: 8px;
    }

    .plan-info p {
      margin: 0.5rem 0;
      color: #2c3e50;
    }

    .no-plan-message, .no-plans-message {
      color: #7f8c8d;
      font-style: italic;
    }

    .change-plan-section {
      margin-top: 2rem;
      padding-top: 2rem;
      border-top: 2px solid #e1e8ed;
    }

    .change-plan-section h4 {
      margin-bottom: 1rem;
      color: #2c3e50;
    }

    .plan-select {
      width: 100%;
      padding: 0.75rem;
      border: 2px solid #e1e8ed;
      border-radius: 6px;
      font-size: 1rem;
      margin-bottom: 1rem;
      background: white;
    }

    .plan-select:focus {
      outline: none;
      border-color: #3498db;
    }

    .view-all-plans-link {
      display: block;
      margin-top: 1rem;
      text-align: center;
      color: #3498db;
      text-decoration: none;
      font-size: 0.9rem;
    }

    .view-all-plans-link:hover {
      text-decoration: underline;
    }
  `]
})
export class UserDashboardComponent implements OnInit {
  private userService = inject(UserService);
  private authService = inject(AuthService);
  private currencyService = inject(CurrencyService);

  private currentCurrencyCode = 'EUR'; // Par défaut, sera mis à jour dans ngOnInit
  private currentCurrencySymbol = '€'; // Par défaut, sera mis à jour dans ngOnInit

  // Exposer Math pour le template
  Math = Math;

  userInfo: any;
  organization: Organization | null = null;
  quota: UserQuota | null = null;
  stats: UserUsageStats | null = null;

  loadingOrg = false;
  loadingQuota = false;
  loadingStats = false;
  errorMessage = '';

  startDate = '';
  endDate = '';

  ngOnInit() {
    // Charger la devise du marché
    this.currencyService.getCurrencyCode().pipe(take(1)).subscribe({
      next: (code) => {
        this.currentCurrencyCode = code;
        this.currentCurrencySymbol = this.currencyService.getSymbolForCurrency(code);
      },
      error: (err) => {
        console.error('Erreur lors du chargement de la devise:', err);
      }
    });
    
    this.userInfo = this.authService.getUserInfo();
    this.loadOrganization();
    this.loadQuota();
    this.loadStats();
  }

  loadOrganization() {
    this.loadingOrg = true;
    this.errorMessage = '';
    this.userService.getMyOrganization().subscribe({
      next: (org) => {
        this.organization = org;
        this.loadingOrg = false;
      },
      error: (err) => {
        this.errorMessage = 'Erreur lors du chargement de l\'organisation: ' + (err.error?.message || err.message);
        this.loadingOrg = false;
      }
    });
  }

  loadQuota() {
    this.loadingQuota = true;
    this.userService.getMyQuota().subscribe({
      next: (quota) => {
        this.quota = quota;
        this.loadingQuota = false;
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
    const startDate = this.startDate || undefined;
    const endDate = this.endDate || undefined;
    this.userService.getMyUsageStats(startDate, endDate).subscribe({
      next: (stats) => {
        this.stats = stats;
        this.loadingStats = false;
      },
      error: (err) => {
        this.errorMessage = 'Erreur lors du chargement des statistiques: ' + (err.error?.message || err.message);
        this.loadingStats = false;
      }
    });
  }

  resetFilters() {
    this.startDate = '';
    this.endDate = '';
    this.loadStats();
  }

  formatDate(dateString: string): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('fr-FR', { year: 'numeric', month: 'long', day: 'numeric', hour: '2-digit', minute: '2-digit' });
  }

  formatCurrency(amount: number): string {
    if (amount == null || isNaN(amount)) return '0.00';
    
    // Utiliser la devise du marché stockée dans currentCurrencyCode
    // Pour DZD et MAD, le symbole est placé après le montant
    if (this.currentCurrencyCode === 'DZD' || this.currentCurrencyCode === 'MAD') {
      return `${amount.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${this.currentCurrencySymbol}`;
    }
    
    // Pour les autres devises, utiliser Intl.NumberFormat ou le symbole avant
    try {
      return new Intl.NumberFormat('fr-FR', { 
        style: 'currency', 
        currency: this.currentCurrencyCode,
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      }).format(amount);
    } catch (e) {
      // Si la devise n'est pas supportée par Intl, utiliser le symbole manuellement
      return `${this.currentCurrencySymbol}${amount.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
    }
  }

  formatNumber(num: number): string {
    return new Intl.NumberFormat('fr-FR').format(num);
  }

  truncateSearchTerm(term: string): string {
    if (term.length > 50) {
      return term.substring(0, 47) + '...';
    }
    return term;
  }

  getOrgPercentage(): number {
    if (!this.quota || !this.quota.currentUsage || !this.quota.monthlyQuota) {
      return 0;
    }
    return (this.quota.currentUsage / this.quota.monthlyQuota) * 100;
  }

  isAdmin(): boolean {
    return this.authService.hasRole('ADMIN');
  }
}

