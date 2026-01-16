import {Component, inject} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SearchService } from '../services/search.service';
import {OAuthService} from 'angular-oauth2-oidc';

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="search-container">
      <!-- -->
      <div class="search-form">
        <input
          type="text"
          [(ngModel)]="searchTerm"
          placeholder="Entrez un terme ou une expression de recherche (accepte plusieurs langues)"
          class="search-input"
          (keydown.enter)="search()"
        />
        <button
          (click)="search()"
          class="search-button"
          [disabled]="isLoading || !searchTerm"
        >
          {{ isLoading ? 'Recherche en cours...' : 'Rechercher' }}
        </button>
      </div>
      @if (isLoading) {
        <div class="loading">
          Recherche en cours...
        </div>
      }

      @if (error != null) {
        <div class="error">
          {{ error }}
        </div>
      }

      @if (searchResults && !isLoading) {
        <div class="results">
          <h3>Résultat de la recherche</h3>
          @if (searchResults.length > 0) {
            <table>
              <thead>
              <tr>
                @if (searchResults[0].code != null) {
                  <th>Code</th>
                }
                @if (searchResults[0].description != null) {
                  <th>Description</th>
                }
                @if (searchResults[0].justification != null) {
                  <th>Justification</th>
                }
              </tr>
              </thead>
              <tbody>
              <tr *ngFor="let result of searchResults">
                @if (result.code != null) {
                  <td>{{ result.code }}</td>
                }
                @if (result.description != null) {
                  <td>{{ result.description }}</td>
                }
                @if (result.justification != null) {
                  <td>{{ result.justification }}</td>
                }
              </tr>
              </tbody>
            </table>
          }
          @if (searchResults.length === 0) {
            <p>Aucun résultat n'a été trouvé.</p>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .search-container {
      padding: 40px 20px;
      max-width: 900px;
      margin: 0 auto;
      animation: fadeIn 0.5s ease-in;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(20px); }
      to { opacity: 1; transform: translateY(0); }
    }

    h2 {
      text-align: center;
      margin-bottom: 30px;
      color: #2c3e80;
      font-size: 2rem;
      font-weight: 700;
      text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.1);
    }

    .search-form {
      display: flex;
      margin-bottom: 30px;
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
      border-radius: 12px;
      overflow: hidden;
      background: white;
      border: 2px solid transparent;
      transition: all 0.3s ease;
    }

    .search-form:focus-within {
      border-color: #3498db;
      box-shadow: 0 12px 32px rgba(52, 152, 219, 0.2);
      transform: translateY(-2px);
    }

    .search-input {
      flex: 1;
      padding: 16px 20px;
      font-size: 16px;
      border: none;
      outline: none;
      transition: all 0.3s;
      background: transparent;
    }

    .search-input::placeholder {
      color: #95a5a6;
      transition: color 0.3s;
    }

    .search-input:focus::placeholder {
      color: #bdc3c7;
    }

    .search-button {
      padding: 16px 32px;
      font-size: 16px;
      background: linear-gradient(135deg, #3498db 0%, #2980b9 100%);
      color: white;
      border: none;
      font-weight: 600;
      letter-spacing: 0.5px;
      transition: all 0.3s ease;
      cursor: pointer;
      position: relative;
      overflow: hidden;
    }

    .search-button::before {
      content: '';
      position: absolute;
      top: 0;
      left: -100%;
      width: 100%;
      height: 100%;
      background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
      transition: left 0.6s ease;
    }

    .search-button:hover:not(:disabled)::before {
      left: 100%;
    }

    .search-button:hover:not(:disabled) {
      background: linear-gradient(135deg, #2980b9 0%, #21618c 100%);
      box-shadow: 0 4px 16px rgba(52, 152, 219, 0.4);
      transform: translateY(-2px);
    }

    .search-button:active:not(:disabled) {
      transform: translateY(0);
    }

    .search-button:disabled {
      background: linear-gradient(135deg, #bdc3c7 0%, #95a5a6 100%);
      cursor: not-allowed;
      transform: none;
      opacity: 0.7;
    }

    .loading {
      margin: 30px 0;
      text-align: center;
      color: #3498db;
      font-size: 1.1rem;
      font-weight: 500;
      animation: pulse 1.5s ease-in-out infinite;
    }

    @keyframes pulse {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.5; }
    }

    .error {
      margin: 30px 0;
      color: #c0392b;
      background: linear-gradient(135deg, #fadbd8 0%, #f8d7da 100%);
      padding: 16px 20px;
      border-radius: 10px;
      text-align: center;
      border-left: 4px solid #e74c3c;
      box-shadow: 0 4px 12px rgba(231, 76, 60, 0.15);
      animation: slideDown 0.3s ease;
    }

    @keyframes slideDown {
      from { opacity: 0; transform: translateY(-10px); }
      to { opacity: 1; transform: translateY(0); }
    }

    .results {
      margin-top: 30px;
      padding: 30px;
      background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
      border-radius: 12px;
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
      border-left: 5px solid #3498db;
      animation: slideUp 0.4s ease;
    }

    @keyframes slideUp {
      from { opacity: 0; transform: translateY(20px); }
      to { opacity: 1; transform: translateY(0); }
    }

    .results h3 {
      margin-bottom: 20px;
      color: #2c3e50;
      font-size: 1.5rem;
      font-weight: 700;
      position: relative;
      padding-bottom: 10px;
    }

    .results h3::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 0;
      width: 60px;
      height: 3px;
      background: linear-gradient(90deg, #3498db, #2ecc71);
      border-radius: 2px;
    }

    pre {
      white-space: pre-wrap;
      font-family: 'Courier New', Courier, monospace;
      line-height: 1.6;
      background-color: #fff;
      padding: 20px;
      border-radius: 8px;
      border: 1px solid #e9ecef;
      box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.05);
    }

    table {
      width: 100%;
      margin-top: 20px;
      border-collapse: separate;
      border-spacing: 0;
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
      border-radius: 10px;
      overflow: hidden;
      background: white;
    }

    th, td {
      padding: 14px 18px;
      text-align: left;
      border-bottom: 1px solid #ecf0f1;
    }

    th {
      background: linear-gradient(135deg, #34495e 0%, #2c3e50 100%);
      font-weight: 600;
      color: white;
      text-transform: uppercase;
      font-size: 0.85rem;
      letter-spacing: 0.5px;
      text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
    }

    tbody tr {
      transition: all 0.3s ease;
    }

    tbody tr:hover {
      background: linear-gradient(90deg, #ecf7fd 0%, #f8f9fa 100%);
      transform: scale(1.01);
      box-shadow: 0 2px 8px rgba(52, 152, 219, 0.1);
    }

    tbody tr:last-child td {
      border-bottom: none;
    }

    td {
      color: #2c3e50;
      font-size: 0.95rem;
    }

    .results p {
      text-align: center;
      color: #7f8c8d;
      font-style: italic;
      margin-top: 20px;
      font-size: 1.05rem;
    }

  `]
})
export class SearchComponent {
  searchTerm: string = '';
  //searchResults: string | null = null;
  searchResults: any[] | null | undefined;
  isLoading: boolean = false;
  error: string | null = null;

  private searchService = inject(SearchService);
  private oauthService = inject(OAuthService);

  //constructor(private searchService: SearchService) {}

  search(): void {
    // Vérifier l'authentification
    console.log('Vérification de l\'authentification...');
    console.log('Token valide:', this.oauthService.hasValidAccessToken());
    console.log('Token:', this.oauthService.getAccessToken());

    if (!this.oauthService.hasValidAccessToken()) {
      this.error = 'Vous devez être connecté pour effectuer une recherche.';
      console.log('Utilisateur non authentifié');
      return;
    }

    if (!this.searchTerm) {
      this.error = 'Veuillez entrer un terme de recherche';
      return;
    }

    this.isLoading = true;
    this.error = null;
    this.searchResults = null;

    console.log('Envoi de la requête de recherche...');
    this.searchService.searchCodes(this.searchTerm)
      .subscribe({
          next: (results: any) => {
            try {
                // On s'assure que les résultats sont bien un objet/tableau et non une chaîne JSON
                this.searchResults = typeof results === 'string' ? JSON.parse(results) : results;
            } catch (e) {
                console.error(e);
                this.error = 'Une erreur est survenue lors du traitement des résultats.';
                this.searchResults = [];
            }
            console.log('search resultats:', this.searchResults);
            if (this.searchResults && this.searchResults.length > 0 && this.searchResults[0]) {
                  console.log('search resultats:', this.searchResults[0].code);
                  console.log('search resultats:', this.searchResults[0].description);
                  console.log('search resultats:', this.searchResults[0].justification);
              }
              this.isLoading = false;
          },
        error: (err: any) => {
          console.error('Erreur complète:', err);

          // Afficher un message d'erreur plus spécifique
          if (err.status === 401) {
            this.error = 'Votre session a expiré. Veuillez vous reconnecter.';
            // Optionnel : rediriger vers la page de connexion
            this.oauthService.logOut();
          } else if (err.status === 403) {
            this.error = 'Accès refusé. Vous n\'avez pas les permissions nécessaires.';
          } else if (err.status === 0) {
            this.error = 'Impossible de contacter le serveur. Vérifiez votre connexion.';
          } else if (err.message && err.message.includes('HTML')) {
            this.error = 'Erreur d\'authentification. Veuillez vous reconnecter.';
            this.oauthService.logOut();
          } else {
            this.error = 'Une erreur est survenue lors de la recherche. Veuillez réessayer.';
          }

          this.isLoading = false;
        }
      });
  }
}
