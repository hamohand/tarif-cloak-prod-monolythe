import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { User } from '../../shared/models/user.model'; // Assurez-vous que ce modèle correspond

export interface RegisterUser {
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  password: string;
  organizationName: string;
  organizationEmail: string; // Obligatoire
  organizationAddress: string;
  organizationActivityDomain?: string; // Domaine d'activité (optionnel)
  organizationCountry: string;
  organizationPhone: string;
  organizationPassword: string;
  pricingPlanId?: number | null; // ID du plan tarifaire sélectionné (optionnel)
  marketVersion?: string | null; // Version du marché
}

export interface KeycloakUser {
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  enabled: boolean;
  credentials: Array<{
    type: string;
    value: string;
    temporary: boolean;
  }>;
}

@Injectable({
  providedIn: 'root'
})
export class RegisterService {
  // L'URL pointe maintenant vers votre propre backend via environment
  private apiUrl = `${environment.apiUrl}/auth/register`;

  constructor(private http: HttpClient) {}

  // La méthode est renommée pour plus de clarté
  registerUser(userData: RegisterUser): Observable<any> {
    return this.http.post(this.apiUrl, userData);
  }
//  registerUser(user: Omit<User, 'id'>): Observable<any> {
//    return this.http.post(this.apiUrl, user);
//  }
}
