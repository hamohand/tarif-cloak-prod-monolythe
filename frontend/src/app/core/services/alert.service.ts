import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface QuotaAlert {
  id: number;
  organizationId: number;
  organizationName: string;
  alertType: 'WARNING' | 'CRITICAL' | 'EXCEEDED';
  currentUsage: number;
  monthlyQuota: number | null;
  percentageUsed: number;
  message: string;
  isRead: boolean;
  createdAt: string;
}

export interface AlertCount {
  count: number;
}

@Injectable({
  providedIn: 'root'
})
export class AlertService {
  private apiUrl = `${environment.apiUrl}/alerts`;

  constructor(private http: HttpClient) {}

  /**
   * Récupère les alertes non lues de l'utilisateur connecté.
   */
  getMyAlerts(): Observable<QuotaAlert[]> {
    return this.http.get<QuotaAlert[]>(`${this.apiUrl}/my-alerts`);
  }

  /**
   * Compte les alertes non lues de l'utilisateur connecté.
   */
  getMyAlertsCount(): Observable<AlertCount> {
    return this.http.get<AlertCount>(`${this.apiUrl}/my-alerts/count`);
  }

  /**
   * Récupère toutes les alertes non lues (admin uniquement).
   */
  getAllAlerts(): Observable<QuotaAlert[]> {
    return this.http.get<QuotaAlert[]>(`${this.apiUrl}/admin/all`);
  }

  /**
   * Compte toutes les alertes non lues (admin uniquement).
   */
  getAllAlertsCount(): Observable<AlertCount> {
    return this.http.get<AlertCount>(`${this.apiUrl}/admin/count`);
  }

  /**
   * Récupère les alertes d'une organisation spécifique (admin uniquement).
   */
  getAlertsForOrganization(organizationId: number): Observable<QuotaAlert[]> {
    return this.http.get<QuotaAlert[]>(`${this.apiUrl}/admin/organization/${organizationId}`);
  }

  /**
   * Marque une alerte comme lue.
   */
  markAlertAsRead(alertId: number): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.apiUrl}/${alertId}/read`, {});
  }

  /**
   * Marque toutes les alertes de l'utilisateur comme lues.
   */
  markAllMyAlertsAsRead(): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.apiUrl}/my-alerts/read-all`, {});
  }

  /**
   * Déclenche manuellement une vérification des quotas (admin uniquement).
   */
  checkQuotasManually(): Observable<{ message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/admin/check-quotas`, {});
  }
}

