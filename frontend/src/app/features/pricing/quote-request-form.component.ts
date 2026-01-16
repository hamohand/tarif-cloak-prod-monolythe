import { Component, EventEmitter, Input, Output, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { QuoteRequestService, CreateQuoteRequestDto } from '../../core/services/quote-request.service';

@Component({
  selector: 'app-quote-request-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <div class="quote-form-container" *ngIf="showForm">
      <div class="quote-form-card">
        <div class="quote-form-header">
          <h2>Demander un devis personnalisé</h2>
          <button class="close-btn" (click)="close()" aria-label="Fermer">×</button>
        </div>

        <form [formGroup]="quoteForm" (ngSubmit)="onSubmit()" class="quote-form">
          <div class="form-group">
            <label for="contactName">Nom du contact *</label>
            <input
              id="contactName"
              type="text"
              formControlName="contactName"
              class="form-control"
              [class.error]="isFieldInvalid('contactName')"
              placeholder="Votre nom complet">
            <div class="error-message" *ngIf="isFieldInvalid('contactName')">
              {{ getErrorMessage('contactName') }}
            </div>
          </div>

          <div class="form-group">
            <label for="contactEmail">Email du contact *</label>
            <input
              id="contactEmail"
              type="email"
              formControlName="contactEmail"
              class="form-control"
              [class.error]="isFieldInvalid('contactEmail')"
              placeholder="votre.email@exemple.com">
            <div class="error-message" *ngIf="isFieldInvalid('contactEmail')">
              {{ getErrorMessage('contactEmail') }}
            </div>
          </div>

          <div class="form-group">
            <label for="message">Message (optionnel)</label>
            <textarea
              id="message"
              rows="5"
              formControlName="message"
              class="form-control"
              placeholder="Décrivez vos besoins spécifiques, le volume de requêtes attendu, ou toute autre information pertinente pour votre devis personnalisé..."></textarea>
          </div>

          <div class="form-actions">
            <button type="button" class="btn btn-secondary" (click)="close()">
              Annuler
            </button>
            <button type="submit" class="btn btn-primary" [disabled]="quoteForm.invalid || isSubmitting">
              <span *ngIf="isSubmitting">Envoi en cours...</span>
              <span *ngIf="!isSubmitting">Envoyer la demande</span>
            </button>
          </div>

          <div class="error-message" *ngIf="errorMessage">
            {{ errorMessage }}
          </div>

          <div class="success-message" *ngIf="successMessage">
            {{ successMessage }}
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .quote-form-container {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      justify-content: center;
      align-items: center;
      z-index: 1000;
      padding: 1rem;
    }

    .quote-form-card {
      background: white;
      border-radius: 12px;
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
      max-width: 600px;
      width: 100%;
      max-height: 90vh;
      overflow-y: auto;
    }

    .quote-form-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1.5rem;
      border-bottom: 1px solid #e1e8ed;
    }

    .quote-form-header h2 {
      margin: 0;
      color: #2c3e50;
      font-size: 1.5rem;
    }

    .close-btn {
      background: none;
      border: none;
      font-size: 2rem;
      color: #7f8c8d;
      cursor: pointer;
      padding: 0;
      width: 32px;
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      transition: all 0.2s;
    }

    .close-btn:hover {
      background: #ecf0f1;
      color: #2c3e50;
    }

    .quote-form {
      padding: 1.5rem;
    }

    .form-group {
      margin-bottom: 1.5rem;
    }

    label {
      display: block;
      margin-bottom: 0.5rem;
      color: #2c3e50;
      font-weight: 500;
    }

    .form-control {
      width: 100%;
      padding: 0.75rem;
      border: 2px solid #e1e8ed;
      border-radius: 6px;
      font-size: 1rem;
      transition: border-color 0.3s;
      box-sizing: border-box;
    }

    .form-control:focus {
      outline: none;
      border-color: #3498db;
    }

    .form-control.error {
      border-color: #e74c3c;
    }

    textarea.form-control {
      resize: vertical;
      font-family: inherit;
    }

    .error-message {
      color: #e74c3c;
      font-size: 0.875rem;
      margin-top: 0.25rem;
    }

    .success-message {
      color: #27ae60;
      font-size: 0.95rem;
      padding: 1rem;
      background: #d5f4e6;
      border-radius: 6px;
      margin-top: 1rem;
    }

    .form-actions {
      display: flex;
      gap: 1rem;
      justify-content: flex-end;
      margin-top: 2rem;
    }

    .btn {
      padding: 0.75rem 1.5rem;
      border: none;
      border-radius: 6px;
      font-size: 1rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.3s;
    }

    .btn-primary {
      background: linear-gradient(135deg, #3498db 0%, #2980b9 100%);
      color: white;
    }

    .btn-primary:hover:not(:disabled) {
      background: linear-gradient(135deg, #2980b9 0%, #1f6391 100%);
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(52, 152, 219, 0.3);
    }

    .btn-primary:disabled {
      background: #bdc3c7;
      cursor: not-allowed;
      transform: none;
    }

    .btn-secondary {
      background: #ecf0f1;
      color: #2c3e50;
    }

    .btn-secondary:hover {
      background: #d5dbdb;
    }

    @media (max-width: 768px) {
      .quote-form-card {
        max-width: 100%;
        margin: 0;
        border-radius: 0;
        max-height: 100vh;
      }

      .form-actions {
        flex-direction: column;
      }

      .btn {
        width: 100%;
      }
    }
  `]
})
export class QuoteRequestFormComponent implements OnInit {
  @Input() showForm = false;
  @Output() formClosed = new EventEmitter<void>();
  @Output() quoteSubmitted = new EventEmitter<void>();

  private quoteRequestService = inject(QuoteRequestService);
  private fb = inject(FormBuilder);

  quoteForm!: FormGroup;
  isSubmitting = false;
  errorMessage = '';
  successMessage = '';

  ngOnInit() {
    this.quoteForm = this.fb.group({
      contactName: ['', [Validators.required, Validators.minLength(2)]],
      contactEmail: ['', [Validators.required, Validators.email]],
      message: ['']
    });

    // L'organizationId sera récupéré automatiquement côté backend depuis le token
    // Plus besoin de le charger ici
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.quoteForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  getErrorMessage(fieldName: string): string {
    const field = this.quoteForm.get(fieldName);
    if (!field || !field.errors) return '';

    if (field.errors['required']) {
      return 'Ce champ est requis';
    }
    if (field.errors['email']) {
      return 'Email invalide';
    }
    if (field.errors['minlength']) {
      return `Minimum ${field.errors['minlength'].requiredLength} caractères`;
    }
    return 'Champ invalide';
  }

  onSubmit() {
    if (this.quoteForm.invalid) {
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    this.successMessage = '';

    // organizationId sera récupéré automatiquement côté backend depuis le token
    const dto: CreateQuoteRequestDto = {
      contactName: this.quoteForm.value.contactName,
      contactEmail: this.quoteForm.value.contactEmail,
      message: this.quoteForm.value.message || undefined
    };

    this.quoteRequestService.createQuoteRequest(dto).subscribe({
      next: () => {
        this.successMessage = 'Votre demande de devis a été envoyée avec succès. Nous vous contacterons bientôt.';
        this.quoteForm.reset();
        this.quoteSubmitted.emit();
        // Fermer automatiquement après 3 secondes
        setTimeout(() => {
          this.close();
        }, 3000);
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Erreur lors de l\'envoi de la demande. Veuillez réessayer.';
        this.isSubmitting = false;
        console.error('Erreur lors de la création de la demande de devis:', err);
      }
    });
  }

  close() {
    this.showForm = false;
    this.formClosed.emit();
    this.quoteForm.reset();
    this.errorMessage = '';
    this.successMessage = '';
  }
}

