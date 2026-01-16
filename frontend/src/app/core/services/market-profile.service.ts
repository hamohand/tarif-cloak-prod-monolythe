import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface MarketProfile {
  id: number;
  marketVersion: string;
  countryCodeIsoAlpha2: string;
  countryCodeIsoAlpha3?: string;
  countryName: string;
  countryNameNative?: string;
  phonePrefix: string;
  currencyCode: string;
  currencySymbol?: string;
  timezone?: string;
  locale?: string;
  languageCode?: string;
  isActive: boolean;
  displayOrder: number;
  description?: string;
  createdAt: string;
  updatedAt?: string;
}

@Injectable({
  providedIn: 'root'
})
export class MarketProfileService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/market-profiles`;

  /**
   * Récupère tous les profils de marché actifs.
   */
  getActiveMarketProfiles(): Observable<MarketProfile[]> {
    return this.http.get<MarketProfile[]>(this.apiUrl);
  }

  /**
   * Récupère un profil de marché par sa version.
   */
  getMarketProfileByVersion(marketVersion: string): Observable<MarketProfile> {
    return this.http.get<MarketProfile>(`${this.apiUrl}/version/${marketVersion}`);
  }

  /**
   * Récupère un profil de marché par son code pays ISO alpha-2.
   */
  getMarketProfileByCountryCode(countryCode: string): Observable<MarketProfile> {
    return this.http.get<MarketProfile>(`${this.apiUrl}/country/${countryCode}`);
  }
}

