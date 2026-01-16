import { Component, OnInit, inject } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { AdminService, PendingRegistration } from '../../../core/services/admin.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-pending-registrations',
  standalone: true,
  imports: [CommonModule, DatePipe],
  template: `
    <div class="pending-registrations-container">
      <h2>⏳ Utilisateurs en attente d'inscription</h2>

      @if (loading) {
        <p>Chargement...</p>
      } @else if (pendingRegistrations.length === 0) {
        <p class="empty-message">Aucun utilisateur en attente d'inscription.</p>
      } @else {
        <div class="registrations-table-container">
          <table class="registrations-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Utilisateur</th>
                <th>Email</th>
                <th>Organisation</th>
                <th>Email Organisation</th>
                <th>Pays</th>
                <th>Expire le</th>
                <th>Statut</th>
              </tr>
            </thead>
            <tbody>
              @for (registration of pendingRegistrations; track registration.id) {
                <tr [class.expired]="isExpired(registration)">
                  <td>{{ registration.createdAt | date:'dd/MM/yyyy HH:mm' }}</td>
                  <td>
                    <strong>{{ registration.firstName }} {{ registration.lastName }}</strong><br>
                    <small>@{{ registration.username }}</small>
                  </td>
                  <td>{{ registration.email }}</td>
                  <td>{{ registration.organizationName }}</td>
                  <td>{{ registration.organizationEmail }}</td>
                  <td>{{ registration.organizationCountry }}</td>
                  <td [class.expired-date]="isExpired(registration)">
                    {{ registration.expiresAt | date:'dd/MM/yyyy HH:mm' }}
                  </td>
                  <td>
                    @if (isExpired(registration)) {
                      <span class="badge badge-danger">Expiré</span>
                    } @else {
                      <span class="badge badge-warning">En attente</span>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>
        
        <div class="stats-bar">
          <p>
            <strong>Total :</strong> {{ pendingRegistrations.length }} inscription(s) en attente
            @if (expiredCount > 0) {
              | <span class="expired-count">{{ expiredCount }} expirée(s)</span>
            }
          </p>
        </div>
      }
    </div>
  `,
  styles: [`
    .pending-registrations-container {
      padding: 2rem;
      max-width: 1400px;
      margin: 0 auto;
    }

    h2 {
      color: #2c3e50;
      margin-bottom: 2rem;
      font-size: 1.8rem;
    }

    .empty-message {
      text-align: center;
      padding: 3rem;
      color: #6c757d;
      font-size: 1.1rem;
    }

    .registrations-table-container {
      overflow-x: auto;
      background: white;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
    }

    .registrations-table {
      width: 100%;
      border-collapse: collapse;
    }

    .registrations-table thead {
      background: linear-gradient(135deg, #1e3c72 0%, #2a5298 100%);
      color: white;
    }

    .registrations-table th {
      padding: 1rem;
      text-align: left;
      font-weight: 600;
      font-size: 0.9rem;
    }

    .registrations-table td {
      padding: 1rem;
      border-bottom: 1px solid #e9ecef;
    }

    .registrations-table tbody tr:hover {
      background-color: #f8f9fa;
    }

    .registrations-table tbody tr.expired {
      background-color: #fff5f5;
      opacity: 0.7;
    }

    .badge {
      display: inline-block;
      padding: 0.25rem 0.75rem;
      border-radius: 12px;
      font-size: 0.85rem;
      font-weight: 600;
    }

    .badge-warning {
      background-color: #ffc107;
      color: #000;
    }

    .badge-danger {
      background-color: #dc3545;
      color: white;
    }

    .expired-date {
      color: #dc3545;
      font-weight: 600;
    }

    .stats-bar {
      margin-top: 1.5rem;
      padding: 1rem;
      background: #f8f9fa;
      border-radius: 8px;
      text-align: center;
    }

    .expired-count {
      color: #dc3545;
      font-weight: 600;
    }

    small {
      color: #6c757d;
      font-size: 0.85rem;
    }
  `]
})
export class PendingRegistrationsComponent implements OnInit {
  private adminService = inject(AdminService);
  private notificationService = inject(NotificationService);

  pendingRegistrations: PendingRegistration[] = [];
  loading = false;

  get expiredCount(): number {
    return this.pendingRegistrations.filter(r => this.isExpired(r)).length;
  }

  ngOnInit() {
    this.loadPendingRegistrations();
  }

  loadPendingRegistrations() {
    this.loading = true;
    this.adminService.getPendingRegistrations().subscribe({
      next: (registrations) => {
        this.pendingRegistrations = registrations;
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur lors du chargement des inscriptions en attente:', err);
        this.notificationService.error('Erreur lors du chargement des inscriptions en attente');
        this.loading = false;
      }
    });
  }

  isExpired(registration: PendingRegistration): boolean {
    const expiresAt = new Date(registration.expiresAt);
    return expiresAt < new Date();
  }
}

