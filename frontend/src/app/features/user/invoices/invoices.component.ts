import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { InvoiceService, Invoice } from '../../../core/services/invoice.service';
import { NotificationService } from '../../../core/services/notification.service';

@Component({
  selector: 'app-invoices',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  template: `
    <div class="invoices-container">
      <div class="invoices-header">
        <h2>Mes Factures</h2>
        <p class="subtitle">Consultez vos factures et téléchargez-les en PDF</p>
      </div>

      <!-- Filtres et recherche -->
      @if (invoices.length > 0) {
        <div class="filters-bar">
          <div class="search-group">
            <input type="text" 
                   [(ngModel)]="searchTerm" 
                   (input)="applyFilters()" 
                   placeholder="Rechercher par numéro..." 
                   class="search-input" />
          </div>
          <div class="filter-group">
            <label for="statusFilter">Statut :</label>
            <select id="statusFilter" [(ngModel)]="statusFilter" (change)="applyFilters()" class="filter-select">
              <option [ngValue]="null">Tous</option>
              <option value="DRAFT">Brouillon</option>
              <option value="PENDING">En attente</option>
              <option value="PAID">Payée</option>
              <option value="OVERDUE">En retard</option>
              <option value="CANCELLED">Annulée</option>
            </select>
          </div>
          <div class="filter-group">
            <label for="dateFrom">Du :</label>
            <input type="date" id="dateFrom" [(ngModel)]="dateFrom" (change)="applyFilters()" class="filter-input" />
          </div>
          <div class="filter-group">
            <label for="dateTo">Au :</label>
            <input type="date" id="dateTo" [(ngModel)]="dateTo" (change)="applyFilters()" class="filter-input" />
          </div>
          <div class="actions-group">
            <button class="btn btn-secondary" (click)="resetFilters()">Réinitialiser</button>
            <button class="btn btn-secondary" (click)="exportToCsv()">📊 Export CSV</button>
          </div>
        </div>
      }

      @if (loading) {
        <div class="loading">
          <p>Chargement des factures...</p>
        </div>
      } @else if (error) {
        <div class="error">
          <p>❌ {{ error }}</p>
        </div>
      } @else if (filteredInvoices.length === 0) {
        <div class="no-invoices">
          <p>📄 Aucune facture disponible pour le moment.</p>
        </div>
      } @else {
        <!-- Informations de pagination -->
        <div class="pagination-info">
          <p>Affichage de {{ getStartIndex() }} à {{ getEndIndex() }} sur {{ filteredInvoices.length }} facture(s)</p>
        </div>

        <div class="invoices-table-wrapper">
          <table class="invoices-table">
            <thead>
              <tr>
                <th>Numéro</th>
                <th>Période</th>
                <th>Montant</th>
                <th>Statut</th>
                <th>Date d'échéance</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              @for (invoice of paginatedInvoices; track invoice.id) {
                <tr [class.new-invoice]="!invoice.viewedAt">
                  <td>
                    <strong>{{ invoice.invoiceNumber }}</strong>
                    @if (!invoice.viewedAt) {
                      <span class="new-badge">Nouveau</span>
                    }
                  </td>
                  <td>
                    {{ formatDate(invoice.periodStart) }} - {{ formatDate(invoice.periodEnd) }}
                  </td>
                  <td>
                    <strong>{{ formatCurrency(invoice.totalAmount) }}</strong>
                  </td>
                  <td>
                    <span class="status-badge" [class]="invoiceService.getStatusClass(invoice.status)">
                      {{ invoiceService.getStatusText(invoice.status) }}
                    </span>
                  </td>
                  <td>
                    {{ formatDate(invoice.dueDate) }}
                  </td>
                  <td>
                    <button class="btn btn-primary" (click)="viewInvoice(invoice.id)">
                      Voir
                    </button>
                    <button class="btn btn-secondary" (click)="downloadPdf(invoice)">
                      📥 PDF
                    </button>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>

        <!-- Pagination -->
        <div class="pagination">
          <button class="btn btn-secondary" (click)="previousPage()" [disabled]="currentPage === 1">
            ← Précédent
          </button>
          <span class="page-info">
            Page {{ currentPage }} sur {{ totalPages }}
          </span>
          <button class="btn btn-secondary" (click)="nextPage()" [disabled]="currentPage === totalPages">
            Suivant →
          </button>
        </div>
      }
    </div>
  `,
  styles: [`
    .invoices-container {
      padding: 2rem;
      max-width: 1400px;
      margin: 0 auto;
      background: #e8e8e8;
      min-height: 100vh;
    }

    .invoices-header {
      margin-bottom: 2rem;
      background: #e0e0e0;
      padding: 1.5rem;
      border-radius: 8px;
    }

    .invoices-header h2 {
      margin: 0 0 0.5rem 0;
      color: #2c3e50;
    }

    .subtitle {
      color: #666;
      margin: 0;
    }

    .loading, .error, .no-invoices {
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

    .no-invoices {
      background: #e0e0e0;
      color: #666;
    }

    .invoices-table-wrapper {
      background: #e0e0e0;
      border-radius: 8px;
      overflow: hidden;
      margin-top: 2rem;
    }

    .invoices-table {
      width: 100%;
      border-collapse: collapse;
    }

    .invoices-table th {
      background: #d5d5d5;
      padding: 1rem;
      text-align: left;
      font-weight: 600;
      color: #2c3e50;
      border-bottom: 2px solid #bbb;
    }

    .invoices-table td {
      padding: 1rem;
      border-bottom: 1px solid #ccc;
      background: #e0e0e0;
    }

    .invoices-table tr:hover td {
      background: #d5d5d5;
    }

    .status-badge {
      display: inline-block;
      padding: 0.25rem 0.75rem;
      border-radius: 12px;
      font-size: 0.875rem;
      font-weight: 500;
    }

    .status-draft {
      background: #e0e0e0;
      color: #666;
    }

    .status-pending {
      background: #fff3cd;
      color: #856404;
    }

    .status-paid {
      background: #d4edda;
      color: #155724;
    }

    .status-overdue {
      background: #f8d7da;
      color: #721c24;
    }

    .status-cancelled {
      background: #e0e0e0;
      color: #666;
    }

    .new-badge {
      display: inline-block;
      margin-left: 0.5rem;
      padding: 0.15rem 0.5rem;
      background: #3498db;
      color: white;
      border-radius: 10px;
      font-size: 0.7rem;
      font-weight: 600;
      animation: pulse 2s infinite;
    }

    .new-invoice {
      background: #e8f4f8 !important;
    }

    .new-invoice:hover {
      background: #d0e8f0 !important;
    }

    @keyframes pulse {
      0%, 100% {
        opacity: 1;
      }
      50% {
        opacity: 0.7;
      }
    }

    .btn {
      padding: 0.5rem 1rem;
      border: none;
      border-radius: 4px;
      cursor: pointer;
      font-size: 0.875rem;
      margin-right: 0.5rem;
      transition: background-color 0.2s;
    }

    .btn-primary {
      background: #007bff;
      color: white;
    }

    .btn-primary:hover {
      background: #0056b3;
    }

    .btn-secondary {
      background: #6c757d;
      color: white;
    }

    .btn-secondary:hover {
      background: #545b62;
    }

    .filters-bar {
      background: #e0e0e0;
      padding: 1.5rem;
      border-radius: 8px;
      margin-bottom: 2rem;
      display: flex;
      flex-wrap: wrap;
      gap: 1rem;
      align-items: flex-end;
    }

    .search-group {
      flex: 1;
      min-width: 200px;
    }

    .search-input {
      width: 100%;
      padding: 0.5rem;
      border: 1px solid #ccc;
      border-radius: 4px;
      font-size: 1rem;
    }

    .filter-group {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .filter-group label {
      font-weight: 600;
      color: #2c3e50;
      font-size: 0.875rem;
    }

    .filter-select,
    .filter-input {
      padding: 0.5rem;
      border: 1px solid #ccc;
      border-radius: 4px;
      font-size: 1rem;
    }

    .actions-group {
      display: flex;
      gap: 0.5rem;
    }

    .pagination-info {
      margin-bottom: 1rem;
      color: #666;
      font-size: 0.875rem;
    }

    .pagination {
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 1rem;
      margin-top: 2rem;
    }

    .page-info {
      font-weight: 600;
      color: #2c3e50;
    }

    .btn:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }
  `]
})
export class InvoicesComponent implements OnInit {
  invoiceService = inject(InvoiceService);
  private notificationService = inject(NotificationService);
  private router = inject(Router);

  invoices: Invoice[] = [];
  filteredInvoices: Invoice[] = [];
  paginatedInvoices: Invoice[] = [];
  loading = false;
  error: string | null = null;

  // Filtres
  searchTerm = '';
  statusFilter: string | null = null;
  dateFrom: string | null = null;
  dateTo: string | null = null;

  // Pagination
  currentPage = 1;
  pageSize = 10;
  totalPages = 1;

  ngOnInit() {
    this.loadInvoices();
  }

  loadInvoices() {
    this.loading = true;
    this.error = null;

    this.invoiceService.getMyInvoices().subscribe({
      next: (invoices) => {
        this.invoices = invoices || [];
        this.applyFilters();
        this.loading = false;
      },
      error: (err) => {
        console.error('Erreur lors du chargement des factures:', err);
        this.error = 'Erreur lors du chargement des factures. Veuillez réessayer.';
        this.loading = false;
        this.notificationService.error('Erreur lors du chargement des factures');
      }
    });
  }

  applyFilters() {
    this.filteredInvoices = this.invoices.filter(invoice => {
      // Recherche
      if (this.searchTerm) {
        const searchLower = this.searchTerm.toLowerCase();
        if (!invoice.invoiceNumber.toLowerCase().includes(searchLower)) {
          return false;
        }
      }

      // Filtre par statut
      if (this.statusFilter && invoice.status !== this.statusFilter) {
        return false;
      }

      // Filtre par date
      if (this.dateFrom) {
        const invoiceDate = new Date(invoice.createdAt);
        const fromDate = new Date(this.dateFrom);
        if (invoiceDate < fromDate) {
          return false;
        }
      }

      if (this.dateTo) {
        const invoiceDate = new Date(invoice.createdAt);
        const toDate = new Date(this.dateTo);
        toDate.setHours(23, 59, 59, 999);
        if (invoiceDate > toDate) {
          return false;
        }
      }

      return true;
    });

    this.currentPage = 1;
    this.updatePagination();
  }

  resetFilters() {
    this.searchTerm = '';
    this.statusFilter = null;
    this.dateFrom = null;
    this.dateTo = null;
    this.applyFilters();
  }

  updatePagination() {
    this.totalPages = Math.ceil(this.filteredInvoices.length / this.pageSize);
    const startIndex = (this.currentPage - 1) * this.pageSize;
    const endIndex = startIndex + this.pageSize;
    this.paginatedInvoices = this.filteredInvoices.slice(startIndex, endIndex);
  }

  previousPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.updatePagination();
    }
  }

  nextPage() {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.updatePagination();
    }
  }

  exportToCsv() {
    const csv = this.convertToCsv(this.filteredInvoices);
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const filename = `mes_factures_${new Date().toISOString().split('T')[0]}.csv`;
    this.invoiceService.downloadFile(blob, filename);
    this.notificationService.success('Export CSV réussi');
  }

  private convertToCsv(invoices: Invoice[]): string {
    const headers = ['Numéro', 'Période Début', 'Période Fin', 'Montant', 'Statut', 'Date de création', 'Date d\'échéance'];
    const rows = invoices.map(invoice => [
      invoice.invoiceNumber,
      this.formatDate(invoice.periodStart),
      this.formatDate(invoice.periodEnd),
      invoice.totalAmount.toString(),
      this.invoiceService.getStatusText(invoice.status),
      this.formatDateTime(invoice.createdAt),
      this.formatDate(invoice.dueDate)
    ]);

    const csvContent = [
      headers.join(','),
      ...rows.map(row => row.map(cell => `"${cell}"`).join(','))
    ].join('\n');

    // Ajouter BOM pour UTF-8
    return '\uFEFF' + csvContent;
  }

  formatDateTime(dateString: string): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleString('fr-FR');
  }

  getStartIndex(): number {
    return (this.currentPage - 1) * this.pageSize + 1;
  }

  getEndIndex(): number {
    return Math.min(this.currentPage * this.pageSize, this.filteredInvoices.length);
  }

  viewInvoice(id: number) {
    this.router.navigate(['/invoices', id]);
  }

  downloadPdf(invoice: Invoice) {
    this.invoiceService.downloadInvoicePdf(invoice.id).subscribe({
      next: (blob) => {
        const filename = `facture_${invoice.invoiceNumber}.pdf`;
        this.invoiceService.downloadFile(blob, filename);
        this.notificationService.success('Facture téléchargée avec succès');
      },
      error: (err) => {
        console.error('Erreur lors du téléchargement du PDF:', err);
        this.notificationService.error('Erreur lors du téléchargement du PDF');
      }
    });
  }

  formatDate(dateString: string): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    return date.toLocaleDateString('fr-FR');
  }

  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR'
    }).format(amount);
  }
}

