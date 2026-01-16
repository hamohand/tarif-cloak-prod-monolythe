import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface OrganizationInfo {
  id: number;
  name: string;
  email: string;
  address: string;
  activityDomain?: string | null;
  country: string;
  phone: string;
  monthlyQuota?: number | null;
  pricingPlanId?: number | null;
  trialExpiresAt?: string | null;
  trialPermanentlyExpired?: boolean | null;
  createdAt?: string | null;
  userCount?: number | null;
  currentMonthUsage?: number | null;
  marketVersion?: string | null;
}

export interface OrganizationCollaborator {
  id: number;
  organizationId: number;
  organizationName: string;
  keycloakUserId: string;
  username: string;
  email?: string | null;
  firstName?: string | null;
  lastName?: string | null;
  joinedAt?: string | null;
  enabled?: boolean | null;
}

export interface CollaboratorsResponse {
  organization: OrganizationInfo;
  collaborators: OrganizationCollaborator[];
}

export interface InviteCollaboratorRequest {
  username: string;
  email: string;
  firstName: string;
  lastName: string;
}

export interface InviteCollaboratorResponse {
  message: string;
  tokenExpiresAt: string;
}

export interface OrganizationUsageLog {
  id: number;
  keycloakUserId: string;
  collaboratorName: string;
  endpoint: string;
  searchTerm: string;
  tokensUsed: number | null;
  tokenCostUsd: number | null;
  totalCostUsd: number | null;
  baseCostUsd: number | null;
  timestamp: string;
}

export interface OrganizationUsageLogsResponse {
  organizationId: number;
  organizationName: string;
  startDate: string;
  endDate: string;
  totalRequests: number;
  usageLogs: OrganizationUsageLog[];
}

export interface OrganizationStatus {
  canMakeRequests: boolean;
  isTrialExpired: boolean;
  trialPermanentlyExpired?: boolean | null;
  trialExpiresAt?: string | null;
  hasPricingPlan: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class OrganizationAccountService {
  private http = inject(HttpClient);
  private baseUrl = `${environment.apiUrl}/organization/account`;

  getMyOrganization(): Observable<OrganizationInfo> {
    return this.http.get<OrganizationInfo>(`${this.baseUrl}/me`);
  }

  getMyCollaborators(): Observable<CollaboratorsResponse> {
    return this.http.get<CollaboratorsResponse>(`${this.baseUrl}/collaborators`);
  }

  inviteCollaborator(request: InviteCollaboratorRequest): Observable<InviteCollaboratorResponse> {
    return this.http.post<InviteCollaboratorResponse>(`${this.baseUrl}/collaborators`, request);
  }

  disableCollaborator(keycloakUserId: string): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.baseUrl}/collaborators/${keycloakUserId}/disable`, {});
  }

  enableCollaborator(keycloakUserId: string): Observable<{ message: string }> {
    return this.http.put<{ message: string }>(`${this.baseUrl}/collaborators/${keycloakUserId}/enable`, {});
  }

  deleteCollaborator(keycloakUserId: string): Observable<{ message: string }> {
    return this.http.delete<{ message: string }>(`${this.baseUrl}/collaborators/${keycloakUserId}`);
  }

  getOrganizationUsageLogs(startDate?: string, endDate?: string): Observable<OrganizationUsageLogsResponse> {
    let url = `${this.baseUrl}/usage-logs`;
    const params: string[] = [];
    if (startDate) {
      params.push(`startDate=${encodeURIComponent(startDate)}`);
    }
    if (endDate) {
      params.push(`endDate=${encodeURIComponent(endDate)}`);
    }
    if (params.length > 0) {
      url += '?' + params.join('&');
    }
    return this.http.get<OrganizationUsageLogsResponse>(url);
  }

  getOrganizationStatus(): Observable<OrganizationStatus> {
    return this.http.get<OrganizationStatus>(`${this.baseUrl}/status`);
  }
}

