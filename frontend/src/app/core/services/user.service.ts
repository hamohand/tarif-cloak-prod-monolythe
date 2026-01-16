import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface Organization {
  id: number;
  name: string;
  email?: string | null;
  monthlyQuota?: number | null;
  pricingPlanId?: number | null;
  trialExpiresAt?: string | null;
  trialPermanentlyExpired?: boolean | null;
  createdAt: string;
  userCount?: number;
  marketVersion?: string | null;
  monthlyPlanStartDate?: string | null;
  monthlyPlanEndDate?: string | null;
  pendingMonthlyPlanId?: number | null;
  pendingMonthlyPlanChangeDate?: string | null;
  lastPayPerRequestInvoiceDate?: string | null;
  pendingPayPerRequestPlanId?: number | null;
  pendingPayPerRequestChangeDate?: string | null;
}

export interface UserUsageStats {
  totalRequests: number;
  totalCostUsd: number;
  totalTokens: number;
  monthlyRequests: number;
  recentUsage: UsageLog[];
  quotaInfo?: QuotaInfo;
}

export interface UsageLog {
  id: number;
  endpoint: string;
  searchTerm: string;
  tokensUsed: number;
  costUsd: number | null;
  timestamp: string;
}

export interface QuotaInfo {
  monthlyQuota: number | null;
  currentUsage: number; // Usage total de l'organisation
  personalUsage?: number; // Usage personnel
  remaining: number; // -1 = illimité
  percentageUsed: number;
}

export interface UserQuota {
  hasOrganization: boolean;
  organizationId?: number;
  organizationName?: string;
  monthlyQuota?: number | null;
  currentUsage?: number; // Usage total de l'organisation
  personalUsage?: number; // Usage personnel de l'utilisateur
  remaining?: number; // -1 = illimité
  percentageUsed?: number;
  isUnlimited?: boolean;
  message?: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private apiUrl = `${environment.apiUrl}/user`;

  constructor(private http: HttpClient) {}

  /**
   * Récupère l'organisation de l'utilisateur connecté.
   */
  getMyOrganization(): Observable<Organization | null> {
    return this.http.get<Organization | null>(`${this.apiUrl}/organization`);
  }

  /**
   * Récupère les statistiques d'utilisation de l'utilisateur connecté.
   * @param startDate Date de début (optionnel, format: YYYY-MM-DD)
   * @param endDate Date de fin (optionnel, format: YYYY-MM-DD)
   */
  getMyUsageStats(startDate?: string, endDate?: string): Observable<UserUsageStats> {
    let params = new HttpParams();
    
    if (startDate && startDate.trim() !== '') {
      params = params.set('startDate', startDate);
    }
    if (endDate && endDate.trim() !== '') {
      params = params.set('endDate', endDate);
    }

    return this.http.get<UserUsageStats>(`${this.apiUrl}/usage/stats`, { params });
  }

  /**
   * Récupère l'état du quota de l'utilisateur connecté.
   */
  getMyQuota(): Observable<UserQuota> {
    return this.http.get<UserQuota>(`${this.apiUrl}/quota`);
  }
}

