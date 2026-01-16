import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class SearchService {
  private apiUrl = '/api/recherche';
  private conversionApiUrl = '/api/conversion';

  constructor(private http: HttpClient) { }

  searchCodes(searchTerm: string): Observable<any[]> {

    // Le backend produit 'application/json', on s'attend donc à recevoir du JSON.
    // 'responseType: 'json'' est le comportement par défaut de HttpClient.
    return this.http.get<any[]>(`${this.apiUrl}/positions6`, {
      params: { termeRecherche: searchTerm }
      // responseType: 'text' a été supprimé ici.
    }).pipe(
      // HttpClient parse maintenant automatiquement le JSON.
      // La réponse est directement un tableau JavaScript.
      map((response: any) => {
        // On s'assure juste que la réponse est bien un tableau.
        if (!Array.isArray(response)) {
          console.warn('La réponse reçue du serveur n\'est pas un tableau.', response);
          return [];
        }
        return response;
      }),
      catchError(this.handleError)
    );
  }

  convertFile(file: File): Observable<string> {
    const formData = new FormData();
    formData.append('file', file, file.name);

    return this.http.post(`${this.conversionApiUrl}/convert`, formData, {
      responseType: 'text'
    }).pipe(
      catchError(this.handleError)
    );
  }

  private handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'Une erreur est survenue';

    if (error.status === 0) {
      // Erreur réseau ou CORS
      errorMessage = 'Impossible de contacter le serveur. Vérifiez votre connexion.';
    } else if (error.status === 401) {
      errorMessage = 'Session expirée. Veuillez vous reconnecter.';
    } else if (error.status === 403) {
      errorMessage = 'Accès refusé. Vous n\'avez pas les permissions nécessaires.';
    } else if (error.status >= 500) {
      errorMessage = 'Erreur serveur. Veuillez réessayer plus tard.';
    } else if (error.error) {
      errorMessage = typeof error.error === 'string'
        ? error.error
        : JSON.stringify(error.error);
    }

    console.error('Erreur HTTP:', {
      status: error.status,
      message: errorMessage,
      error: error
    });

    return throwError(() => new Error(errorMessage));
  }
}
