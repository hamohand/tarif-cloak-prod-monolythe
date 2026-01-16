import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface UsageStats {
  totalRequests: number;
  totalCostUsd: number;
  totalTokens: number;
  statsByOrganization?: OrganizationStats[];
  statsByUser?: UserStats[];
  recentUsage?: UsageLog[];
}

export interface OrganizationStats {
  organizationId: number;
  organizationName: string;
  requestCount: number;
  totalCostUsd: number;
  totalTokens: number;
}

export interface UserStats {
  keycloakUserId: string;
  username?: string; // Nom d'utilisateur depuis Keycloak
  requestCount: number;
  totalCostUsd: number;
  totalTokens: number;
}

export interface UsageLog {
  id: number;
  keycloakUserId: string;
  username?: string; // Nom d'utilisateur depuis Keycloak
  organizationId: number | null;
  endpoint: string;
  searchTerm: string;
  tokensUsed: number;
  costUsd: number | null;
  timestamp: string;
}

export interface UsageLogsResponse {
  total: number;
  totalCostUsd: number;
  totalTokens: number;
  logs: UsageLog[];
}

export interface Organization {
  id: number;
  name: string;
  email?: string | null;
  monthlyQuota?: number | null;
  enabled?: boolean; // false = organisation désactivée
  createdAt: string;
  userCount?: number;
  currentMonthUsage?: number; // Nombre de requêtes utilisées ce mois
}

export interface CreateOrganizationRequest {
  name: string;
  email?: string | null;
}

export interface UpdateOrganizationRequest {
  name?: string;
  email?: string | null;
}

export interface UpdateQuotaRequest {
  monthlyQuota?: number | null;
}

export interface OrganizationUser {
  id: number;
  keycloakUserId: string;
  username?: string; // Nom d'utilisateur depuis Keycloak
  organizationId: number;
  joinedAt: string;
}

export interface AddUserToOrganizationRequest {
  keycloakUserId: string;
}

export interface PendingRegistration {
  id: number;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  organizationName: string;
  organizationEmail: string;
  organizationAddress: string;
  organizationActivityDomain?: string;
  organizationCountry: string;
  organizationPhone: string;
  pricingPlanId?: number;
  marketVersion?: string;
  confirmationToken: string;
  expiresAt: string;
  confirmed: boolean;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private apiUrl = `${environment.apiUrl}/admin`;

  constructor(private http: HttpClient) {}

  /**
   * Récupère les statistiques d'utilisation.
   * @param organizationId ID de l'organisation (optionnel)
   * @param startDate Date de début (optionnel, format: YYYY-MM-DD)
   * @param endDate Date de fin (optionnel, format: YYYY-MM-DD)
   */
  getUsageStats(organizationId?: number, startDate?: string, endDate?: string): Observable<UsageStats> {
    let params = new HttpParams();
    
    // Ne pas inclure le paramètre si la valeur est null, undefined, ou NaN
    // Vérifier explicitement que c'est un nombre valide (y compris 0)
    if (organizationId !== null && organizationId !== undefined && 
        !isNaN(organizationId) && typeof organizationId === 'number') {
      params = params.set('organizationId', organizationId.toString());
    }
    if (startDate && startDate.trim() !== '' && startDate !== 'null') {
      params = params.set('startDate', startDate);
    }
    if (endDate && endDate.trim() !== '' && endDate !== 'null') {
      params = params.set('endDate', endDate);
    }

    return this.http.get<UsageStats>(`${this.apiUrl}/usage/stats`, { params });
  }

  /**
   * Récupère les logs d'utilisation.
   * @param userId ID de l'utilisateur Keycloak (optionnel)
   * @param organizationId ID de l'organisation (optionnel)
   * @param startDate Date de début (optionnel, format: YYYY-MM-DD)
   * @param endDate Date de fin (optionnel, format: YYYY-MM-DD)
   */
  getUsageLogs(userId?: string, organizationId?: number, startDate?: string, endDate?: string): Observable<UsageLogsResponse> {
    let params = new HttpParams();
    
    if (userId) {
      params = params.set('userId', userId);
    }
    if (organizationId) {
      params = params.set('organizationId', organizationId.toString());
    }
    if (startDate) {
      params = params.set('startDate', startDate);
    }
    if (endDate) {
      params = params.set('endDate', endDate);
    }

    return this.http.get<UsageLogsResponse>(`${this.apiUrl}/usage-logs`, { params });
  }

  /**
   * Récupère toutes les organisations.
   */
  getOrganizations(): Observable<Organization[]> {
    return this.http.get<Organization[]>(`${this.apiUrl}/organizations`);
  }

  /**
   * Récupère une organisation par son ID.
   */
  getOrganization(id: number): Observable<Organization> {
    return this.http.get<Organization>(`${this.apiUrl}/organizations/${id}`);
  }

  /**
   * Crée une nouvelle organisation.
   */
  createOrganization(request: CreateOrganizationRequest): Observable<Organization> {
    return this.http.post<Organization>(`${this.apiUrl}/organizations`, request);
  }

  /**
   * Met à jour une organisation.
   */
  updateOrganization(id: number, request: UpdateOrganizationRequest): Observable<Organization> {
    return this.http.put<Organization>(`${this.apiUrl}/organizations/${id}`, request);
  }

  /**
   * Met à jour le quota mensuel d'une organisation.
   */
  updateQuota(id: number, request: UpdateQuotaRequest): Observable<Organization> {
    return this.http.put<Organization>(`${this.apiUrl}/organizations/${id}/quota`, request);
  }

  /**
   * Récupère les utilisateurs d'une organisation.
   */
  getOrganizationUsers(organizationId: number): Observable<OrganizationUser[]> {
    return this.http.get<OrganizationUser[]>(`${this.apiUrl}/organizations/${organizationId}/users`);
  }

  /**
   * Ajoute un utilisateur à une organisation.
   */
  addUserToOrganization(organizationId: number, request: AddUserToOrganizationRequest): Observable<OrganizationUser> {
    return this.http.post<OrganizationUser>(`${this.apiUrl}/organizations/${organizationId}/users`, request);
  }

  /**
   * Retire un utilisateur d'une organisation.
   */
  removeUserFromOrganization(organizationId: number, keycloakUserId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/organizations/${organizationId}/users/${keycloakUserId}`);
  }

  /**
   * Récupère tous les utilisateurs en attente d'inscription
   * @return Liste des inscriptions en attente
   */
  getPendingRegistrations(): Observable<PendingRegistration[]> {
    return this.http.get<PendingRegistration[]>(`${this.apiUrl}/pending-registrations`);
  }

  /**
   * Désactive une organisation.
   * Les collaborateurs ne pourront plus utiliser l'application.
   */
  disableOrganization(organizationId: number): Observable<Organization> {
    return this.http.put<Organization>(`${this.apiUrl}/organizations/${organizationId}/disable`, {});
  }

  /**
   * Réactive une organisation.
   * Les collaborateurs pourront à nouveau utiliser l'application.
   */
  enableOrganization(organizationId: number): Observable<Organization> {
    return this.http.put<Organization>(`${this.apiUrl}/organizations/${organizationId}/enable`, {});
  }
}

