import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { QuoteRequestService, QuoteRequestDto } from '../../core/services/quote-request.service';
import { OrganizationAccountService, OrganizationInfo } from '../../core/services/organization-account.service';
import { QuoteRequestFormComponent } from '../pricing/quote-request-form.component';

@Component({
  selector: 'app-quote-requests',
  standalone: true,
  imports: [CommonModule, QuoteRequestFormComponent],
  template: `
    <div class="quote-requests-container">
      <div class="header">
        <h2>Mes demandes de devis</h2>
        <button class="btn btn-primary" (click)="openNewRequest()" *ngIf="organization">
          Nouvelle demande
        </button>
      </div>

      @if (loading) {
        <div class="loading">Chargement des demandes...</div>
      } @else if (error) {
        <div class="error">{{ error }}</div>
      } @else if (quoteRequests.length === 0) {
        <div class="empty-state">
          <p>Aucune demande de devis pour le moment.</p>
          <button class="btn btn-primary" (click)="openNewRequest()" *ngIf="organization">
            Créer une demande
          </button>
        </div>
      } @else {
        <div class="quote-requests-list">
          @for (request of quoteRequests; track request.id) {
            <div class="quote-request-card" [class]="'status-' + request.status.toLowerCase()">
              <div class="card-header">
                <div>
                  <h3>Demande #{{ request.id }}</h3>
                  <p class="date">Créée le {{ formatDate(request.createdAt) }}</p>
                </div>
                <span class="status-badge" [class]="'status-' + request.status.toLowerCase()">
                  {{ getStatusLabel(request.status) }}
                </span>
              </div>

              <div class="card-body">
                <div class="info-row">
                  <strong>Contact :</strong>
                  <span>{{ request.contactName }} ({{ request.contactEmail }})</span>
                </div>
                @if (request.message) {
                  <div class="message-section">
                    <strong>Message :</strong>
                    <p>{{ request.message }}</p>
                  </div>
                }
                @if (request.adminNotes) {
                  <div class="admin-notes-section">
                    <strong>Notes de l'administrateur :</strong>
                    <p>{{ request.adminNotes }}</p>
                  </div>
                }
                @if (request.respondedAt) {
                  <div class="info-row">
                    <strong>Réponse le :</strong>
                    <span>{{ formatDate(request.respondedAt) }}</span>
                  </div>
                }
              </div>
            </div>
          }
        </div>
      }
    </div>

    <app-quote-request-form
      [showForm]="showQuoteForm"
      (formClosed)="closeQuoteForm()"
      (quoteSubmitted)="onQuoteSubmitted()">
    </app-quote-request-form>
  `,
  styles: [`
    .quote-requests-container {
      padding: 2rem;
      max-width: 1000px;
      margin: 0 auto;
    }

    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 2rem;
    }

    .header h2 {
      color: #2c3e50;
      margin: 0;
    }

    .btn {
      padding: 0.75rem 1.5rem;
      border: none;
      border-radius: 6px;
      font-size: 1rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.3s;
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

    .loading, .error {
      text-align: center;
      padding: 2rem;
      font-size: 1.2rem;
    }

    .error {
      color: #e74c3c;
    }

    .empty-state {
      text-align: center;
      padding: 4rem 2rem;
      background: #f8f9fa;
      border-radius: 12px;
    }

    .empty-state p {
      color: #7f8c8d;
      margin-bottom: 1.5rem;
      font-size: 1.1rem;
    }

    .quote-requests-list {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
    }

    .quote-request-card {
      background: white;
      border-radius: 12px;
      padding: 1.5rem;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      border-left: 4px solid #95a5a6;
      transition: transform 0.3s ease, box-shadow 0.3s ease;
    }

    .quote-request-card:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
    }

    .quote-request-card.status-pending {
      border-left-color: #f39c12;
    }

    .quote-request-card.status-in_review {
      border-left-color: #3498db;
    }

    .quote-request-card.status-responded {
      border-left-color: #27ae60;
    }

    .quote-request-card.status-closed {
      border-left-color: #95a5a6;
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 1rem;
      padding-bottom: 1rem;
      border-bottom: 1px solid #ecf0f1;
    }

    .card-header h3 {
      margin: 0 0 0.5rem 0;
      color: #2c3e50;
      font-size: 1.3rem;
    }

    .date {
      color: #7f8c8d;
      font-size: 0.9rem;
      margin: 0;
    }

    .status-badge {
      padding: 0.5rem 1rem;
      border-radius: 20px;
      font-size: 0.875rem;
      font-weight: 600;
      text-transform: uppercase;
    }

    .status-badge.status-pending {
      background: #fff3cd;
      color: #856404;
    }

    .status-badge.status-in_review {
      background: #d1ecf1;
      color: #0c5460;
    }

    .status-badge.status-responded {
      background: #d4edda;
      color: #155724;
    }

    .status-badge.status-closed {
      background: #e2e3e5;
      color: #383d41;
    }

    .card-body {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .info-row {
      display: flex;
      gap: 1rem;
    }

    .info-row strong {
      color: #2c3e50;
      min-width: 120px;
    }

    .info-row span {
      color: #7f8c8d;
    }

    .message-section, .admin-notes-section {
      padding: 1rem;
      background: #f8f9fa;
      border-radius: 6px;
    }

    .message-section strong, .admin-notes-section strong {
      display: block;
      color: #2c3e50;
      margin-bottom: 0.5rem;
    }

    .message-section p, .admin-notes-section p {
      color: #7f8c8d;
      margin: 0;
      white-space: pre-wrap;
    }

    @media (max-width: 768px) {
      .header {
        flex-direction: column;
        align-items: flex-start;
        gap: 1rem;
      }

      .card-header {
        flex-direction: column;
        gap: 1rem;
      }

      .info-row {
        flex-direction: column;
        gap: 0.5rem;
      }

      .info-row strong {
        min-width: auto;
      }
    }
  `]
})
export class QuoteRequestsComponent implements OnInit {
  private quoteRequestService = inject(QuoteRequestService);
  private organizationAccountService = inject(OrganizationAccountService);

  quoteRequests: QuoteRequestDto[] = [];
  organization: OrganizationInfo | null = null;
  loading = true;
  error = '';
  showQuoteForm = false;

  ngOnInit() {
    this.loadOrganization();
  }

  loadOrganization() {
    this.organizationAccountService.getMyOrganization().subscribe({
      next: (org) => {
        this.organization = org;
        this.loadQuoteRequests();
      },
      error: (err) => {
        console.error('Erreur lors du chargement de l\'organisation:', err);
        this.error = 'Impossible de charger les informations de l\'organisation.';
        this.loading = false;
      }
    });
  }

  loadQuoteRequests() {
    this.loading = true;
    this.error = '';
    // Utiliser le nouvel endpoint qui récupère automatiquement l'organisation depuis le token
    this.quoteRequestService.getMyOrganizationQuoteRequests().subscribe({
      next: (requests) => {
        // Trier par date de création (plus récent en premier)
        this.quoteRequests = requests.sort((a, b) => 
          new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        );
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des demandes de devis: ' + (err.error?.message || err.message || 'Erreur inconnue');
        this.loading = false;
        console.error('Erreur:', err);
      }
    });
  }

  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return new Intl.DateTimeFormat('fr-FR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    }).format(date);
  }

  getStatusLabel(status: string): string {
    const statusMap: { [key: string]: string } = {
      'PENDING': 'En attente',
      'IN_REVIEW': 'En cours d\'examen',
      'RESPONDED': 'Répondu',
      'CLOSED': 'Fermée'
    };
    return statusMap[status] || status;
  }

  openNewRequest() {
    this.showQuoteForm = true;
  }

  closeQuoteForm() {
    this.showQuoteForm = false;
  }

  onQuoteSubmitted() {
    // Recharger les demandes après soumission
    this.loadQuoteRequests();
  }
}

