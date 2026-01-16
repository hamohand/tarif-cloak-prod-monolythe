import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Invoice {
  id: number;
  organizationId: number;
  organizationName: string;
  organizationEmail?: string | null;
  invoiceNumber: string;
  periodStart: string;
  periodEnd: string;
  totalAmount: number;
  status: 'DRAFT' | 'PENDING' | 'PAID' | 'OVERDUE' | 'CANCELLED';
  createdAt: string;
  dueDate: string;
  paidAt?: string | null;
  notes?: string | null;
  viewedAt?: string | null;
  items?: InvoiceItem[];
  totalRequests?: number;
  totalTokens?: number;
  totalCostUsd?: number;
}

export interface InvoiceItem {
  id: number;
  description: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  itemType?: string;
}

export interface GenerateInvoiceRequest {
  organizationId: number;
  periodStart: string;
  periodEnd: string;
}

export interface UpdateInvoiceStatusRequest {
  status: 'DRAFT' | 'PENDING' | 'PAID' | 'OVERDUE' | 'CANCELLED';
  notes?: string;
}

@Injectable({
  providedIn: 'root'
})
export class InvoiceService {
  private apiUrl = `${environment.apiUrl}/invoices`;
  private http = inject(HttpClient);

  /**
   * Récupère les factures de l'utilisateur connecté.
   */
  getMyInvoices(): Observable<Invoice[]> {
    return this.http.get<Invoice[]>(`${this.apiUrl}/my-invoices`);
  }

  /**
   * Récupère une facture par son ID (pour l'utilisateur connecté).
   */
  getMyInvoice(id: number): Observable<Invoice> {
    return this.http.get<Invoice>(`${this.apiUrl}/my-invoices/${id}`);
  }

  /**
   * Télécharge le PDF d'une facture (pour l'utilisateur connecté).
   */
  downloadInvoicePdf(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/my-invoices/${id}/pdf`, {
      responseType: 'blob'
    });
  }

  /**
   * Compte les nouvelles factures non consultées de l'utilisateur connecté.
   */
  getNewInvoicesCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.apiUrl}/my-invoices/new-count`);
  }

  /**
   * Marque une facture comme consultée (pour l'utilisateur connecté).
   * Note: Les factures sont automatiquement marquées comme consultées lors de la récupération.
   */
  markInvoiceAsViewed(id: number): Observable<Invoice> {
    return this.http.put<Invoice>(`${this.apiUrl}/my-invoices/${id}/mark-viewed`, {});
  }

  /**
   * Compte les factures en retard (OVERDUE) de l'utilisateur connecté.
   */
  getOverdueInvoicesCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.apiUrl}/my-invoices/overdue-count`);
  }

  /**
   * Récupère toutes les factures (admin uniquement).
   */
  getAllInvoices(): Observable<Invoice[]> {
    return this.http.get<Invoice[]>(`${this.apiUrl}/admin/all`);
  }

  /**
   * Récupère les factures d'une organisation (admin uniquement).
   */
  getInvoicesByOrganization(organizationId: number): Observable<Invoice[]> {
    return this.http.get<Invoice[]>(`${this.apiUrl}/admin/organization/${organizationId}`);
  }

  /**
   * Récupère une facture par son ID (admin uniquement).
   */
  getInvoice(id: number): Observable<Invoice> {
    return this.http.get<Invoice>(`${this.apiUrl}/admin/${id}`);
  }

  /**
   * Télécharge le PDF d'une facture (admin uniquement).
   */
  downloadInvoicePdfAdmin(id: number): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/admin/${id}/pdf`, {
      responseType: 'blob'
    });
  }

  /**
   * Génère une facture pour une période personnalisée (admin uniquement).
   */
  generateInvoice(request: GenerateInvoiceRequest): Observable<Invoice> {
    return this.http.post<Invoice>(`${this.apiUrl}/admin/generate`, request);
  }

  /**
   * Génère une facture mensuelle (admin uniquement).
   */
  generateMonthlyInvoice(organizationId: number, year: number, month: number): Observable<Invoice> {
    let params = new HttpParams();
    params = params.set('organizationId', organizationId.toString());
    params = params.set('year', year.toString());
    params = params.set('month', month.toString());
    return this.http.post<Invoice>(`${this.apiUrl}/admin/generate-monthly`, null, { params });
  }

  /**
   * Génère les factures mensuelles pour toutes les organisations (admin uniquement).
   */
  generateAllMonthlyInvoices(year: number, month: number): Observable<{ message: string; count: number; invoices: Invoice[] }> {
    let params = new HttpParams();
    params = params.set('year', year.toString());
    params = params.set('month', month.toString());
    return this.http.post<{ message: string; count: number; invoices: Invoice[] }>(
      `${this.apiUrl}/admin/generate-all-monthly`, null, { params });
  }

  /**
   * Met à jour le statut d'une facture (admin uniquement).
   */
  updateInvoiceStatus(id: number, request: UpdateInvoiceStatusRequest): Observable<Invoice> {
    return this.http.put<Invoice>(`${this.apiUrl}/admin/${id}/status`, request);
  }

  /**
   * Télécharge un fichier blob en tant que fichier.
   */
  downloadFile(blob: Blob, filename: string): void {
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  }

  /**
   * Formate le statut d'une facture en français.
   */
  getStatusText(status: Invoice['status']): string {
    switch (status) {
      case 'DRAFT':
        return 'Brouillon';
      case 'PENDING':
        return 'En attente';
      case 'PAID':
        return 'Payée';
      case 'OVERDUE':
        return 'En retard';
      case 'CANCELLED':
        return 'Annulée';
      default:
        return status;
    }
  }

  /**
   * Retourne la classe CSS pour le statut.
   */
  getStatusClass(status: Invoice['status']): string {
    switch (status) {
      case 'DRAFT':
        return 'status-draft';
      case 'PENDING':
        return 'status-pending';
      case 'PAID':
        return 'status-paid';
      case 'OVERDUE':
        return 'status-overdue';
      case 'CANCELLED':
        return 'status-cancelled';
      default:
        return '';
    }
  }
}

