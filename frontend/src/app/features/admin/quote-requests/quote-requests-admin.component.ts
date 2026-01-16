import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { QuoteRequestService, QuoteRequestDto, UpdateQuoteRequestDto } from '../../../core/services/quote-request.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-quote-requests-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="quote-requests-admin-container">
      <h2>💼 Gestion des Demandes de Devis</h2>

      <!-- Filtres -->
      <div class="filters-bar">
        <select [(ngModel)]="selectedStatus" (change)="loadQuoteRequests()" class="filter-select">
          <option value="">Tous les statuts</option>
          <option value="PENDING">En attente</option>
          <option value="IN_REVIEW">En cours d'examen</option>
          <option value="RESPONDED">Répondu</option>
          <option value="CLOSED">Fermé</option>
        </select>
        <button class="btn btn-secondary" (click)="loadQuoteRequests()">Actualiser</button>
      </div>

      <!-- Liste des demandes -->
      @if (loading) {
        <div class="loading">Chargement des demandes...</div>
      } @else if (error) {
        <div class="error">{{ error }}</div>
      } @else if (quoteRequests.length === 0) {
        <div class="empty-state">Aucune demande de devis trouvée.</div>
      } @else {
        <div class="quote-requests-list">
          @for (request of quoteRequests; track request.id) {
            <div class="quote-request-card" [class]="'status-' + request.status.toLowerCase()">
              <div class="card-header">
                <div>
                  <h3>Demande #{{ request.id }}</h3>
                  <p class="meta-info">
                    Organisation ID: {{ request.organizationId }} | 
                    Créée le {{ formatDate(request.createdAt) }}
                  </p>
                </div>
                <span class="status-badge" [class]="'status-' + request.status.toLowerCase()">
                  {{ getStatusLabel(request.status) }}
                </span>
              </div>

              <div class="card-body">
                <div class="info-section">
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
                  @if (request.respondedAt) {
                    <div class="info-row">
                      <strong>Répondu le :</strong>
                      <span>{{ formatDate(request.respondedAt) }}</span>
                    </div>
                  }
                </div>

                <!-- Formulaire de mise à jour -->
                <div class="update-section">
                  <h4>Mettre à jour la demande</h4>
                  <form (ngSubmit)="updateRequest(request.id)" class="update-form">
                    <div class="form-group">
                      <label for="status-{{ request.id }}">Statut</label>
                      <select 
                        id="status-{{ request.id }}"
                        [(ngModel)]="requestUpdates[request.id].status" 
                        name="status-{{ request.id }}"
                        class="form-control">
                        <option [value]="undefined">-- Ne pas modifier --</option>
                        <option value="PENDING">En attente</option>
                        <option value="IN_REVIEW">En cours d'examen</option>
                        <option value="RESPONDED">Répondu</option>
                        <option value="CLOSED">Fermé</option>
                      </select>
                    </div>
                    <div class="form-group">
                      <label for="notes-{{ request.id }}">Notes administrateur</label>
                      <textarea 
                        id="notes-{{ request.id }}"
                        [(ngModel)]="requestUpdates[request.id].adminNotes" 
                        name="notes-{{ request.id }}"
                        rows="3"
                        class="form-control"
                        placeholder="Ajouter des notes internes..."></textarea>
                    </div>
                    <div class="form-actions">
                      <button 
                        type="submit" 
                        class="btn btn-primary"
                        [disabled]="isUpdating[request.id]">
                        <span *ngIf="isUpdating[request.id]">Mise à jour...</span>
                        <span *ngIf="!isUpdating[request.id]">Mettre à jour</span>
                      </button>
                    </div>
                  </form>
                  
                  @if (request.adminNotes) {
                    <div class="current-notes">
                      <strong>Notes actuelles :</strong>
                      <p>{{ request.adminNotes }}</p>
                    </div>
                  }
                </div>
              </div>
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .quote-requests-admin-container {
      padding: 2rem;
      max-width: 1400px;
      margin: 0 auto;
    }

    h2 {
      color: #2c3e50;
      margin-bottom: 2rem;
    }

    .filters-bar {
      display: flex;
      gap: 1rem;
      margin-bottom: 2rem;
      align-items: center;
    }

    .filter-select {
      padding: 0.5rem 1rem;
      border: 2px solid #e1e8ed;
      border-radius: 6px;
      font-size: 1rem;
    }

    .btn {
      padding: 0.5rem 1rem;
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

    .btn-primary:hover:not(:disabled) {
      background: linear-gradient(135deg, #2980b9 0%, #1f6391 100%);
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(52, 152, 219, 0.3);
    }

    .btn-primary:disabled {
      background: #bdc3c7;
      cursor: not-allowed;
    }

    .btn-secondary {
      background: #ecf0f1;
      color: #2c3e50;
    }

    .btn-secondary:hover {
      background: #d5dbdb;
    }

    .loading, .error, .empty-state {
      text-align: center;
      padding: 2rem;
      font-size: 1.2rem;
    }

    .error {
      color: #e74c3c;
    }

    .empty-state {
      color: #7f8c8d;
    }

    .quote-requests-list {
      display: flex;
      flex-direction: column;
      gap: 2rem;
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

    .meta-info {
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
      gap: 1.5rem;
    }

    .info-section {
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
      min-width: 150px;
    }

    .info-row span {
      color: #7f8c8d;
    }

    .message-section {
      padding: 1rem;
      background: #f8f9fa;
      border-radius: 6px;
    }

    .message-section strong {
      display: block;
      color: #2c3e50;
      margin-bottom: 0.5rem;
    }

    .message-section p {
      color: #7f8c8d;
      margin: 0;
      white-space: pre-wrap;
    }

    .update-section {
      padding: 1.5rem;
      background: #f8f9fa;
      border-radius: 8px;
      border: 1px solid #e1e8ed;
    }

    .update-section h4 {
      margin: 0 0 1rem 0;
      color: #2c3e50;
    }

    .update-form {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .form-group {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .form-group label {
      font-weight: 500;
      color: #2c3e50;
    }

    .form-control {
      padding: 0.75rem;
      border: 2px solid #e1e8ed;
      border-radius: 6px;
      font-size: 1rem;
      transition: border-color 0.3s;
    }

    .form-control:focus {
      outline: none;
      border-color: #3498db;
    }

    textarea.form-control {
      resize: vertical;
      font-family: inherit;
    }

    .form-actions {
      display: flex;
      justify-content: flex-end;
    }

    .current-notes {
      margin-top: 1rem;
      padding: 1rem;
      background: white;
      border-radius: 6px;
      border-left: 3px solid #3498db;
    }

    .current-notes strong {
      display: block;
      color: #2c3e50;
      margin-bottom: 0.5rem;
    }

    .current-notes p {
      color: #7f8c8d;
      margin: 0;
      white-space: pre-wrap;
    }

    @media (max-width: 768px) {
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
export class QuoteRequestsAdminComponent implements OnInit {
  private quoteRequestService = inject(QuoteRequestService);
  private notificationService = inject(NotificationService);

  quoteRequests: QuoteRequestDto[] = [];
  loading = true;
  error = '';
  selectedStatus = '';
  requestUpdates: { [key: number]: UpdateQuoteRequestDto } = {};
  isUpdating: { [key: number]: boolean } = {};

  ngOnInit() {
    this.loadQuoteRequests();
  }

  loadQuoteRequests() {
    this.loading = true;
    this.error = '';
    
    const request = this.selectedStatus 
      ? this.quoteRequestService.getQuoteRequestsByStatus(this.selectedStatus)
      : this.quoteRequestService.getAllQuoteRequests();

    request.subscribe({
      next: (requests) => {
        this.quoteRequests = requests;
        // Initialiser les mises à jour pour chaque demande
        requests.forEach(req => {
          if (!this.requestUpdates[req.id]) {
            this.requestUpdates[req.id] = { status: undefined, adminNotes: '' };
          }
        });
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Erreur lors du chargement des demandes: ' + (err.error?.message || err.message || 'Erreur inconnue');
        this.loading = false;
        console.error('Erreur:', err);
      }
    });
  }

  updateRequest(id: number) {
    const update = this.requestUpdates[id];
    if (!update.status && !update.adminNotes) {
      this.notificationService.error('Veuillez modifier au moins un champ');
      return;
    }

    this.isUpdating[id] = true;
    
    // Préparer le DTO de mise à jour (ne pas envoyer les champs undefined)
    const updateDto: UpdateQuoteRequestDto = {};
    if (update.status) {
      updateDto.status = update.status as any;
    }
    if (update.adminNotes !== undefined && update.adminNotes.trim() !== '') {
      updateDto.adminNotes = update.adminNotes;
    }

    this.quoteRequestService.updateQuoteRequest(id, updateDto).subscribe({
      next: (updated) => {
        // Mettre à jour la demande dans la liste
        const index = this.quoteRequests.findIndex(r => r.id === id);
        if (index !== -1) {
          this.quoteRequests[index] = updated;
        }
        // Réinitialiser les champs de mise à jour
        this.requestUpdates[id] = { status: undefined, adminNotes: '' };
        this.isUpdating[id] = false;
        this.notificationService.success('Demande mise à jour avec succès');
      },
      error: (err) => {
        this.isUpdating[id] = false;
        this.notificationService.error('Erreur lors de la mise à jour: ' + (err.error?.message || err.message || 'Erreur inconnue'));
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
}

