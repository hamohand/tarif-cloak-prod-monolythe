import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-confirm-registration',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="confirm-container">
      <div class="confirm-card">
        @if (isLoading) {
          <div class="loading-state">
            <h2>Confirmation en cours...</h2>
            <p>Veuillez patienter pendant que nous confirmons votre inscription.</p>
            <div class="spinner"></div>
          </div>
        }
        
        @if (!isLoading && !errorMessage && !successMessage) {
          <div class="initial-state">
            <h2>Confirmation d'inscription</h2>
            <p>Veuillez patienter...</p>
          </div>
        }
        
        @if (errorMessage) {
          <div class="error-state">
            <h2>Erreur de confirmation</h2>
            <div class="error-icon">⚠️</div>
            <p class="error-message">{{ errorMessage }}</p>
            <div class="actions">
              <button class="btn btn-secondary" routerLink="/auth/register">
                Réessayer l'inscription
              </button>
              <button class="btn btn-primary" routerLink="/auth/login">
                Se connecter
              </button>
            </div>
          </div>
        }
        
        @if (successMessage) {
          <div class="success-state">
            <h2>Inscription confirmée !</h2>
            <div class="success-icon">✓</div>
            <p class="success-message">{{ successMessage }}</p>
            <div class="actions">
              <button class="btn btn-primary" routerLink="/auth/login">
                Se connecter maintenant
              </button>
            </div>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .confirm-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 80vh;
      padding: 2rem;
    }

    .confirm-card {
      background: white;
      padding: 3rem;
      border-radius: 12px;
      box-shadow: 0 4px 20px rgba(0,0,0,0.1);
      width: 100%;
      max-width: 500px;
      text-align: center;
    }

    h2 {
      color: #2c3e50;
      margin-bottom: 1rem;
    }

    .loading-state,
    .initial-state,
    .error-state,
    .success-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 1rem;
    }

    .spinner {
      border: 4px solid #f3f3f3;
      border-top: 4px solid #3498db;
      border-radius: 50%;
      width: 50px;
      height: 50px;
      animation: spin 1s linear infinite;
      margin: 1rem 0;
    }

    @keyframes spin {
      0% { transform: rotate(0deg); }
      100% { transform: rotate(360deg); }
    }

    .error-icon,
    .success-icon {
      font-size: 4rem;
      margin: 1rem 0;
    }

    .success-icon {
      color: #27ae60;
      background: #d4edda;
      width: 80px;
      height: 80px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 3rem;
      font-weight: bold;
    }

    .error-icon {
      color: #e74c3c;
    }

    .error-message,
    .success-message {
      color: #7f8c8d;
      margin: 1rem 0;
      line-height: 1.6;
    }

    .error-message {
      color: #e74c3c;
    }

    .success-message {
      color: #27ae60;
    }

    .actions {
      display: flex;
      gap: 1rem;
      margin-top: 2rem;
      flex-wrap: wrap;
      justify-content: center;
    }

    .btn {
      padding: 0.75rem 1.5rem;
      border: none;
      border-radius: 6px;
      font-size: 1rem;
      cursor: pointer;
      transition: background-color 0.3s;
      text-decoration: none;
      display: inline-block;
    }

    .btn-primary {
      background-color: #3498db;
      color: white;
    }

    .btn-primary:hover {
      background-color: #2980b9;
    }

    .btn-secondary {
      background-color: #95a5a6;
      color: white;
    }

    .btn-secondary:hover {
      background-color: #7f8c8d;
    }
  `]
})
export class ConfirmRegistrationComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private http = inject(HttpClient);
  
  isLoading = false;
  errorMessage = '';
  successMessage = '';

  ngOnInit() {
    const token = this.route.snapshot.queryParams['token'];
    
    if (!token) {
      this.errorMessage = 'Token de confirmation manquant. Veuillez utiliser le lien fourni dans l\'email de confirmation.';
      return;
    }
    
    this.confirmRegistration(token);
  }

  confirmRegistration(token: string) {
    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';
    
    const apiUrl = `${environment.apiUrl}/auth/confirm-registration?token=${encodeURIComponent(token)}`;
    
    this.http.get(apiUrl).subscribe({
      next: (response: any) => {
        this.isLoading = false;
        this.successMessage = response.message || 'Inscription confirmée avec succès ! Vous pouvez maintenant vous connecter.';
      },
      error: (error) => {
        this.isLoading = false;
        if (error.error && error.error.error) {
          this.errorMessage = error.error.error;
        } else if (error.status === 400) {
          this.errorMessage = 'Token de confirmation invalide ou expiré. Veuillez réessayer l\'inscription.';
        } else if (error.status === 0) {
          this.errorMessage = 'Impossible de joindre le serveur. Vérifiez votre connexion.';
        } else {
          this.errorMessage = 'Une erreur est survenue lors de la confirmation de l\'inscription.';
        }
      }
    });
  }
}

