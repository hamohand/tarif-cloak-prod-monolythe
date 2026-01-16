import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { InvoiceService, Invoice, UpdateInvoiceStatusRequest } from '../../../core/services/invoice.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-invoice-detail-admin',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="invoice-detail-container">
      <div class="invoice-header">
        <button class="btn btn-back" (click)="goBack()">← Retour</button>
        <h2>Facture {{ invoice?.invoiceNumber }} (Admin)</h2>
      </div>

      @if (loading) {
        <div class="loading">
          <p>Chargement de la facture...</p>
        </div>
      } @else if (error) {
        <div class="error">
          <p>❌ {{ error }}</p>
        </div>
      } @else if (invoice) {
        <div class="invoice-content">
          <!-- Informations de la facture -->
          <div class="invoice-info-card">
            <h3>Informations de la facture</h3>
            <div class="info-grid">
              <div class="info-item">
                <label>Numéro de facture :</label>
                <span>{{ invoice.invoiceNumber }}</span>
              </div>
              <div class="info-item">
                <label>Organisation :</label>
                <span>{{ invoice.organizationName }}</span>
              </div>
              @if (invoice.organizationEmail) {
                <div class="info-item">
                  <label>Email :</label>
                  <span>{{ invoice.organizationEmail }}</span>
                </div>
              }
              <div class="info-item">
                <label>Période :</label>
                <span>{{ formatDate(invoice.periodStart) }} - {{ formatDate(invoice.periodEnd) }}</span>
              </div>
              <div class="info-item">
                <label>Date de facturation :</label>
                <span>{{ formatDateTime(invoice.createdAt) }}</span>
              </div>
              <div class="info-item">
                <label>Date d'échéance :</label>
                <span>{{ formatDate(invoice.dueDate) }}</span>
              </div>
              <div class="info-item">
                <label>Statut :</label>
                <select [(ngModel)]="invoice.status" (change)="updateStatus()" class="status-select">
                  <option value="DRAFT">Brouillon</option>
                  <option value="PENDING">En attente</option>
                  <option value="PAID">Payée</option>
                  <option value="OVERDUE">En retard</option>
                  <option value="CANCELLED">Annulée</option>
                </select>
              </div>
              @if (invoice.paidAt) {
                <div class="info-item">
                  <label>Date de paiement :</label>
                  <span>{{ formatDateTime(invoice.paidAt) }}</span>
                </div>
              }
            </div>
          </div>

          <!-- Lignes de facture -->
          <div class="invoice-items-card">
            <h3>Détails de la facture</h3>
            @if (invoice.items && invoice.items.length > 0) {
              <table class="items-table">
                <thead>
                  <tr>
                    <th>Description</th>
                    <th>Quantité</th>
                    <th>Prix unitaire</th>
                    <th>Total</th>
                  </tr>
                </thead>
                <tbody>
                  @for (item of invoice.items; track item.id) {
                    <tr>
                      <td>{{ item.description }}</td>
                      <td class="text-center">{{ item.quantity }}</td>
                      <td class="text-right">{{ formatCurrency(item.unitPrice) }}</td>
                      <td class="text-right"><strong>{{ formatCurrency(item.totalPrice) }}</strong></td>
                    </tr>
                  }
                </tbody>
              </table>
            } @else {
              <p>Aucune ligne de facture disponible.</p>
            }
          </div>

          <!-- Statistiques d'utilisation -->
          @if (invoice.totalRequests !== undefined || invoice.totalTokens !== undefined) {
            <div class="invoice-stats-card">
              <h3>Statistiques d'utilisation</h3>
              <div class="stats-grid">
                @if (invoice.totalRequests !== undefined) {
                  <div class="stat-item">
                    <label>Nombre de requêtes :</label>
                    <span>{{ formatNumber(invoice.totalRequests) }}</span>
                  </div>
                }
                @if (invoice.totalTokens !== undefined) {
                  <div class="stat-item">
                    <label>Tokens utilisés :</label>
                    <span>{{ formatNumber(invoice.totalTokens) }}</span>
                  </div>
                }
                @if (invoice.totalCostUsd !== undefined) {
                  <div class="stat-item">
                    <label>Coût total :</label>
                    <span>{{ formatCurrency(invoice.totalCostUsd) }}</span>
                  </div>
                }
              </div>
            </div>
          }

          <!-- Total -->
          <div class="invoice-total-card">
            <div class="total-row">
              <label>Total HT :</label>
              <span>{{ formatCurrency(invoice.totalAmount) }}</span>
            </div>
            <div class="total-row">
              <label>TVA (0%) :</label>
              <span>{{ formatCurrency(0) }}</span>
            </div>
            <div class="total-row total-final">
              <label>Total TTC :</label>
              <span>{{ formatCurrency(invoice.totalAmount) }}</span>
            </div>
          </div>

          <!-- Notes -->
          <div class="invoice-notes-card">
            <h3>Notes</h3>
            <textarea [(ngModel)]="invoice.notes" (blur)="updateStatus()" 
                      placeholder="Ajouter des notes..." class="notes-textarea"></textarea>
          </div>

          <!-- Actions -->
          <div class="invoice-actions">
            <button class="btn btn-primary" (click)="downloadPdf()">
              📥 Télécharger le PDF
            </button>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .invoice-detail-container {
      padding: 2rem;
      max-width: 1200px;
      margin: 0 auto;
      background: #e8e8e8;
      min-height: 100vh;
    }

    .invoice-header {
      display: flex;
      align-items: center;
      gap: 1rem;
      margin-bottom: 2rem;
      background: #e0e0e0;
      padding: 1.5rem;
      border-radius: 8px;
    }

    .invoice-header h2 {
      margin: 0;
      color: #2c3e50;
    }

    .btn-back {
      background: #6c757d;
      color: white;
      border: none;
      padding: 0.5rem 1rem;
      border-radius: 4px;
      cursor: pointer;
      font-size: 0.875rem;
    }

    .btn-back:hover {
      background: #545b62;
    }

    .loading, .error {
      text-align: center;
      padding: 3rem;
      background: #e0e0e0;
      border-radius: 8px;
      margin-top: 2rem;
    }

    .error {
      background: #ffe0e0;
      color: #d32f2f;
    }

    .invoice-content {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
    }

    .invoice-info-card, .invoice-items-card, .invoice-stats-card, .invoice-total-card, .invoice-notes-card {
      background: #e0e0e0;
      padding: 1.5rem;
      border-radius: 8px;
    }

    .invoice-info-card h3, .invoice-items-card h3, .invoice-stats-card h3, .invoice-notes-card h3 {
      margin-top: 0;
      color: #2c3e50;
    }

    .info-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 1rem;
    }

    .info-item {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }

    .info-item label {
      font-weight: 600;
      color: #666;
      font-size: 0.875rem;
    }

    .info-item span {
      color: #2c3e50;
    }

    .status-select {
      padding: 0.5rem;
      border: 1px solid #ccc;
      border-radius: 4px;
      font-size: 1rem;
      cursor: pointer;
    }

    .items-table {
      width: 100%;
      border-collapse: collapse;
      margin-top: 1rem;
    }

    .items-table th {
      background: #d5d5d5;
      padding: 1rem;
      text-align: left;
      font-weight: 600;
      color: #2c3e50;
      border-bottom: 2px solid #bbb;
    }

    .items-table td {
      padding: 1rem;
      border-bottom: 1px solid #ccc;
      background: #e0e0e0;
    }

    .text-center {
      text-align: center;
    }

    .text-right {
      text-align: right;
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 1rem;
      margin-top: 1rem;
    }

    .stat-item {
      display: flex;
      flex-direction: column;
      gap: 0.25rem;
    }

    .stat-item label {
      font-weight: 600;
      color: #666;
      font-size: 0.875rem;
    }

    .stat-item span {
      color: #2c3e50;
      font-size: 1.25rem;
    }

    .invoice-total-card {
      margin-top: 1rem;
    }

    .total-row {
      display: flex;
      justify-content: space-between;
      padding: 0.75rem 0;
      border-bottom: 1px solid #ccc;
    }

    .total-row label {
      font-weight: 600;
      color: #2c3e50;
    }

    .total-row span {
      color: #2c3e50;
    }

    .total-final {
      border-bottom: 2px solid #2c3e50;
      font-size: 1.25rem;
      font-weight: 600;
      margin-top: 0.5rem;
    }

    .notes-textarea {
      width: 100%;
      min-height: 100px;
      padding: 0.5rem;
      border: 1px solid #ccc;
      border-radius: 4px;
      font-size: 1rem;
      font-family: inherit;
      resize: vertical;
    }

    .invoice-actions {
      display: flex;
      gap: 1rem;
      margin-top: 1rem;
    }

    .btn {
      padding: 0.75rem 1.5rem;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 1rem;
      transition: background-color 0.2s;
    }

    .btn-primary {
      background: #007bff;
      color: white;
    }

    .btn-primary:hover {
      background: #0056b3;
    }
  `]
})
export class InvoiceDetailAdminComponent implements OnInit {
  invoiceService = inject(InvoiceService);
  notificationService = inject(NotificationService);
  route = inject(ActivatedRoute);
  router = inject(Router);

  invoice: Invoice | null = null;
  loading = false;
  error: string | null = null;

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadInvoice(parseInt(id, 10));
    }
  }

  loadInvoice(id: number) {
    this.loading = true;
    this.error = null;

    this.invoiceService.getInvoice(id).subscribe({
      next: (invoice) => {
        this.invoice = invoice;
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur lors du chargement de la facture:', err);
        this.error = 'Erreur lors du chargement de la facture. Veuillez réessayer.';
        this.loading = false;
        this.notificationService.error('Erreur lors du chargement de la facture');
      }
    });
  }

  updateStatus() {
    if (!this.invoice) return;

    const request: UpdateInvoiceStatusRequest = {
      status: this.invoice.status,
      notes: this.invoice.notes || undefined
    };

    this.invoiceService.updateInvoiceStatus(this.invoice.id, request).subscribe({
      next: (updatedInvoice) => {
        this.invoice = updatedInvoice;
        this.notificationService.success('Facture mise à jour avec succès');
      },
      error: (err) => {
        console.error('Erreur lors de la mise à jour de la facture:', err);
        this.notificationService.error('Erreur lors de la mise à jour de la facture');
        // Recharger la facture pour restaurer l'état précédent
        this.loadInvoice(this.invoice!.id);
      }
    });
  }

  downloadPdf() {
    if (!this.invoice) return;

    this.invoiceService.downloadInvoicePdfAdmin(this.invoice.id).subscribe({
      next: (blob) => {
        const filename = `facture_${this.invoice!.invoiceNumber}.pdf`;
        this.invoiceService.downloadFile(blob, filename);
        this.notificationService.success('Facture téléchargée avec succès');
      },
      error: (err) => {
        console.error('Erreur lors du téléchargement du PDF:', err);
        this.notificationService.error('Erreur lors du téléchargement du PDF');
      }
    });
  }

  goBack() {
    this.router.navigate(['/admin/invoices']);
  }

  formatDate(dateString: string): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('fr-FR');
  }

  formatDateTime(dateString: string): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleString('fr-FR');
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR'
    }).format(amount);
  }

  formatNumber(value: number): string {
    return new Intl.NumberFormat('fr-FR').format(value);
  }
}

