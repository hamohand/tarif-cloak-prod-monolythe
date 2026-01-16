import { Component, OnInit, inject, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AlertService, QuotaAlert } from '../../../core/services/alert.service';
import { interval, Subscription } from 'rxjs';

@Component({
  selector: 'app-alerts',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="alerts-container">
      <div class="alerts-header">
        <h2>🔔 Mes Alertes de Quota</h2>
        @if (alerts.length > 0) {
          <button class="btn btn-secondary" (click)="markAllAsRead()">Marquer tout comme lu</button>
        }
      </div>

      @if (loading) {
        <p>Chargement des alertes...</p>
      } @else if (alerts.length === 0) {
        <div class="no-alerts">
          <p>✅ Aucune alerte active</p>
          <p class="subtitle">La consommation de votre organisation est dans les limites normales par rapport à votre quota.</p>
        </div>
      } @else {
        <div class="alerts-list">
          @for (alert of alerts; track alert.id) {
            <div class="alert-card" [class]="getAlertClass(alert.alertType)" [class.read]="alert.isRead">
              <div class="alert-header">
                <div class="alert-icon">{{ getAlertIcon(alert.alertType) }}</div>
                <div class="alert-title">
                  <h3>{{ alert.organizationName }}</h3>
                  <p class="alert-date">{{ formatDate(alert.createdAt) }}</p>
                </div>
                @if (!alert.isRead) {
                  <button class="btn-mark-read" (click)="markAsRead(alert.id)">✓ Marquer comme lu</button>
                }
              </div>
              <div class="alert-body">
                <p class="alert-message">{{ alert.message }}</p>
                <div class="alert-details">
                  <div class="detail-item">
                    <span class="label">Consommation de l'organisation:</span>
                    <span class="value">{{ alert.currentUsage }} / {{ alert.monthlyQuota || '∞' }} requêtes</span>
                  </div>
                  <div class="detail-item">
                    <span class="label">Pourcentage utilisé:</span>
                    <span class="value" [class]="getPercentageClass(alert.percentageUsed)">
                      {{ alert.percentageUsed.toFixed(1) }}%
                    </span>
                  </div>
                </div>
                <p class="alert-note">💡 Note: Cette alerte concerne la consommation totale de votre organisation (somme de toutes les requêtes de tous les collaborateurs) par rapport au quota défini par votre plan tarifaire.</p>
              </div>
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .alerts-container {
      padding: 2rem;
      background-color: #e8e8e8;
      min-height: calc(100vh - 60px);
    }

    .alerts-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 2rem;
    }

    .alerts-header h2 {
      color: #2c3e50;
      margin: 0;
    }

    .no-alerts {
      text-align: center;
      padding: 4rem 2rem;
      background: white;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    }

    .no-alerts p {
      font-size: 1.2rem;
      color: #2c3e50;
      margin: 0.5rem 0;
    }

    .no-alerts .subtitle {
      color: #666;
      font-size: 1rem;
    }

    .alerts-list {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
    }

    .alert-card {
      background: #e0e0e0;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      padding: 1.5rem;
      border-left: 4px solid;
      transition: transform 0.2s ease-in-out;
    }

    .alert-card:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }

    .alert-card.read {
      opacity: 0.7;
    }

    .alert-card.warning {
      border-left-color: #ff9800;
    }

    .alert-card.critical {
      border-left-color: #f44336;
    }

    .alert-card.exceeded {
      border-left-color: #d32f2f;
    }

    .alert-header {
      display: flex;
      align-items: center;
      gap: 1rem;
      margin-bottom: 1rem;
    }

    .alert-icon {
      font-size: 2rem;
      width: 50px;
      height: 50px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      background: #f5f5f5;
    }

    .alert-title {
      flex: 1;
    }

    .alert-title h3 {
      margin: 0;
      color: #2c3e50;
      font-size: 1.2rem;
    }

    .alert-date {
      margin: 0.25rem 0 0 0;
      color: #666;
      font-size: 0.9rem;
    }

    .alert-body {
      margin-top: 1rem;
    }

    .alert-message {
      font-size: 1rem;
      color: #333;
      margin: 0 0 1rem 0;
      line-height: 1.5;
    }

    .alert-note {
      font-size: 0.85rem;
      color: #666;
      margin: 1rem 0 0 0;
      padding: 0.75rem;
      background: #f8f9fa;
      border-radius: 6px;
      border-left: 3px solid #3498db;
      font-style: italic;
    }

    .alert-details {
      display: flex;
      gap: 2rem;
      flex-wrap: wrap;
    }

    .detail-item {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }

    .detail-item .label {
      font-size: 0.85rem;
      color: #666;
      font-weight: 600;
    }

    .detail-item .value {
      font-size: 1.1rem;
      color: #2c3e50;
      font-weight: 700;
    }

    .detail-item .value.warning {
      color: #ff9800;
    }

    .detail-item .value.critical {
      color: #f44336;
    }

    .detail-item .value.exceeded {
      color: #d32f2f;
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

    .btn-mark-read {
      padding: 0.4rem 0.8rem;
      background: #3498db;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 0.85rem;
      font-weight: 600;
      transition: background 0.3s ease;
    }

    .btn-mark-read:hover {
      background: #2980b9;
    }
  `]
})
export class AlertsComponent implements OnInit, OnDestroy {
  private alertService = inject(AlertService);

  alerts: QuotaAlert[] = [];
  loading = false;
  private refreshSubscription?: Subscription;

  ngOnInit() {
    this.loadAlerts();
    // Rafraîchir les alertes toutes les 30 secondes
    this.refreshSubscription = interval(30000).subscribe(() => {
      this.loadAlerts();
    });
  }

  ngOnDestroy() {
    if (this.refreshSubscription) {
      this.refreshSubscription.unsubscribe();
    }
  }

  loadAlerts() {
    this.loading = true;
    this.alertService.getMyAlerts().subscribe({
      next: (alerts) => {
        this.alerts = alerts;
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur lors du chargement des alertes:', err);
        this.loading = false;
      }
    });
  }

  markAsRead(alertId: number) {
    this.alertService.markAlertAsRead(alertId).subscribe({
      next: () => {
        // Mettre à jour l'alerte localement
        const alert = this.alerts.find(a => a.id === alertId);
        if (alert) {
          alert.isRead = true;
        }
      },
      error: (err) => {
        console.error('Erreur lors du marquage de l\'alerte:', err);
      }
    });
  }

  markAllAsRead() {
    this.alertService.markAllMyAlertsAsRead().subscribe({
      next: () => {
        // Marquer toutes les alertes comme lues localement
        this.alerts.forEach(alert => alert.isRead = true);
      },
      error: (err) => {
        console.error('Erreur lors du marquage de toutes les alertes:', err);
      }
    });
  }

  getAlertClass(alertType: string): string {
    return alertType.toLowerCase();
  }

  getAlertIcon(alertType: string): string {
    switch (alertType) {
      case 'WARNING':
        return '🟡';
      case 'CRITICAL':
        return '🔴';
      case 'EXCEEDED':
        return '⚠️';
      default:
        return 'ℹ️';
    }
  }

  getPercentageClass(percentage: number): string {
    if (percentage >= 100) {
      return 'exceeded';
    } else if (percentage >= 80) {
      return 'warning';
    }
    return '';
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('fr-FR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}

