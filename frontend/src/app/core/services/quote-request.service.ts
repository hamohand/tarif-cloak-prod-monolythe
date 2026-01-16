import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CreateQuoteRequestDto {
  organizationId?: number; // Optionnel, récupéré automatiquement depuis le token côté backend
  contactName: string;
  contactEmail: string;
  message?: string;
}

export interface QuoteRequestDto {
  id: number;
  organizationId: number;
  contactName: string;
  contactEmail: string;
  message?: string;
  status: 'PENDING' | 'IN_REVIEW' | 'RESPONDED' | 'CLOSED';
  adminNotes?: string;
  createdAt: string;
  updatedAt: string;
  respondedAt?: string;
}

export interface UpdateQuoteRequestDto {
  status?: 'PENDING' | 'IN_REVIEW' | 'RESPONDED' | 'CLOSED';
  adminNotes?: string;
}

@Injectable({
  providedIn: 'root'
})
export class QuoteRequestService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/quote-requests`;

  /**
   * Crée une nouvelle demande de devis.
   */
  createQuoteRequest(dto: CreateQuoteRequestDto): Observable<QuoteRequestDto> {
    return this.http.post<QuoteRequestDto>(this.baseUrl, dto);
  }

  /**
   * Récupère toutes les demandes de devis d'une organisation.
   * @deprecated Utiliser getMyOrganizationQuoteRequests() à la place
   */
  getQuoteRequestsByOrganization(organizationId: number): Observable<QuoteRequestDto[]> {
    return this.http.get<QuoteRequestDto[]>(`${this.baseUrl}/organization/${organizationId}`);
  }

  /**
   * Récupère toutes les demandes de devis de l'organisation de l'utilisateur connecté.
   */
  getMyOrganizationQuoteRequests(): Observable<QuoteRequestDto[]> {
    return this.http.get<QuoteRequestDto[]>(`${this.baseUrl}/my-organization`);
  }

  /**
   * Récupère toutes les demandes de devis (admin uniquement).
   */
  getAllQuoteRequests(): Observable<QuoteRequestDto[]> {
    return this.http.get<QuoteRequestDto[]>(this.baseUrl);
  }

  /**
   * Récupère les demandes de devis par statut (admin uniquement).
   */
  getQuoteRequestsByStatus(status: string): Observable<QuoteRequestDto[]> {
    return this.http.get<QuoteRequestDto[]>(`${this.baseUrl}/status/${status}`);
  }

  /**
   * Récupère une demande de devis par ID (admin uniquement).
   */
  getQuoteRequestById(id: number): Observable<QuoteRequestDto> {
    return this.http.get<QuoteRequestDto>(`${this.baseUrl}/${id}`);
  }

  /**
   * Met à jour une demande de devis (admin uniquement).
   */
  updateQuoteRequest(id: number, dto: UpdateQuoteRequestDto): Observable<QuoteRequestDto> {
    return this.http.put<QuoteRequestDto>(`${this.baseUrl}/${id}`, dto);
  }
}

