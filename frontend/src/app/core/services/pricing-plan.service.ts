import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface PricingPlan {
  id: number;
  name: string;
  description?: string;
  pricePerMonth?: number | null; // null pour les plans facturés à la requête
  pricePerRequest?: number | null; // null pour les plans mensuels
  monthlyQuota?: number | null; // null = quota illimité ou plan facturé à la requête
  trialPeriodDays?: number | null; // null si pas un plan d'essai
  features?: string;
  isActive: boolean;
  displayOrder: number;
  marketVersion?: string; // DEFAULT, DZ, etc.
  currency?: string; // EUR, DZD, etc.
  isCustom?: boolean; // true pour les plans créés via devis
  organizationId?: number | null; // Pour les plans personnalisés
}

export interface ChangePricingPlanRequest {
  pricingPlanId: number | null;
}

@Injectable({
  providedIn: 'root'
})
export class PricingPlanService {
  private apiUrl = `${environment.apiUrl}/pricing-plans`;

  constructor(private http: HttpClient) {}

  /**
   * Récupère tous les plans tarifaires actifs.
   * @param marketVersion Version de marché (ex: 'DZ', 'DEFAULT'). Si non fourni, récupère depuis l'environnement.
   */
  getActivePricingPlans(marketVersion?: string): Observable<PricingPlan[]> {
    let params = new HttpParams();
    
    // Si marketVersion n'est pas fourni, essayer de le récupérer depuis l'environnement
    if (!marketVersion || marketVersion.trim() === '') {
      marketVersion = this.getMarketVersionFromEnvironment();
    }
    
    // Vérification plus stricte
    if (marketVersion != null && marketVersion !== undefined && marketVersion.trim() !== '') {
      const trimmedVersion = marketVersion.trim();
      params = params.set('marketVersion', trimmedVersion);
      console.log('📤 Envoi de la requête avec marketVersion:', trimmedVersion);
      console.log('🌐 URL complète:', `${this.apiUrl}?marketVersion=${trimmedVersion}`);
    } else {
      console.warn('⚠️ marketVersion est undefined/null/vide - Récupération de tous les plans');
      console.log('📤 Envoi de la requête sans marketVersion (récupération de tous les plans)');
      console.log('🌐 URL complète:', this.apiUrl);
    }
    return this.http.get<PricingPlan[]>(this.apiUrl, { params });
  }

  /**
   * Récupère la version de marché depuis l'environnement.
   */
  private getMarketVersionFromEnvironment(): string | undefined {
    // Essayer plusieurs façons d'accéder à marketVersion
    if ((environment as any).marketVersion) {
      return (environment as any).marketVersion;
    } else if ((environment as any)['marketVersion']) {
      return (environment as any)['marketVersion'];
    } else if (environment.marketVersion) {
      return environment.marketVersion;
    }
    
    // Valeur par défaut basée sur l'environnement
    const isProduction = (environment as any).production === true;
    if (isProduction) {
      console.warn('⚠️ marketVersion non trouvé dans environment, utilisation de la valeur par défaut: DZ (production)');
      return 'DZ';
    } else {
      console.warn('⚠️ marketVersion non trouvé dans environment, utilisation de la valeur par défaut: DEFAULT (développement)');
      return 'DEFAULT';
    }
  }

  /**
   * Récupère les plans tarifaires disponibles pour une organisation.
   * Exclut automatiquement le plan d'essai gratuit si l'organisation l'a déjà utilisé.
   * @param marketVersion Version de marché (ex: 'DZ', 'DEFAULT')
   * @param organizationId ID de l'organisation (optionnel)
   */
  getAvailablePricingPlans(marketVersion?: string, organizationId?: number): Observable<PricingPlan[]> {
    let params = new HttpParams();
    
    if (marketVersion && marketVersion.trim() !== '') {
      params = params.set('marketVersion', marketVersion.trim());
    }
    
    if (organizationId) {
      params = params.set('organizationId', organizationId.toString());
    }
    
    return this.http.get<PricingPlan[]>(`${this.apiUrl}/available`, { params });
  }

  /**
   * Récupère un plan tarifaire par ID.
   */
  getPricingPlanById(id: number): Observable<PricingPlan> {
    return this.http.get<PricingPlan>(`${this.apiUrl}/${id}`);
  }

  /**
   * Change le plan tarifaire de l'organisation de l'utilisateur connecté.
   */
  changeMyOrganizationPricingPlan(pricingPlanId: number | null): Observable<any> {
    console.log('📤 PricingPlanService.changeMyOrganizationPricingPlan appelé avec pricingPlanId:', pricingPlanId);
    console.log('🌐 URL:', `${environment.apiUrl}/user/organization/pricing-plan`);
    console.log('📦 Body:', { pricingPlanId });
    
    return this.http.put(`${environment.apiUrl}/user/organization/pricing-plan`, {
      pricingPlanId
    });
  }
}

