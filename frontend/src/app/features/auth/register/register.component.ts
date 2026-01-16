import { Component, inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { RegisterService } from '../../../core/services/register.service';
import { PricingPlanService, PricingPlan } from '../../../core/services/pricing-plan.service';
import { MarketProfileService, MarketProfile } from '../../../core/services/market-profile.service';
import { CurrencyService } from '../../../core/services/currency.service';
import { CommonModule, AsyncPipe } from '@angular/common';
import { environment } from '../../../../environments/environment';
import { take } from 'rxjs/operators';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, CommonModule, AsyncPipe],
  template: `
    <div class="register-container">
      <div class="register-card">
        <h2>Créer un compte organisation</h2>
        <p>Ce compte administrateur gérera l'ensemble de vos collaborateurs.</p>

        <form [formGroup]="registerForm" (ngSubmit)="onSubmit()" class="register-form">
          <div class="form-section">
            <h3>Profil de marché</h3>
            <div class="form-group">
              <label for="marketVersion">Marché *</label>
              <ng-container *ngIf="selectedMarketProfile; else loadingProfile">
                <input 
                  id="marketVersion" 
                  type="text" 
                  [value]="selectedMarketProfile.countryName + ' (' + selectedMarketProfile.currencyCode + ')'"
                  readonly
                  class="form-control"
                  formControlName="marketVersion">
                <small class="form-hint">Profil de marché configuré pour cette instance de l'application.</small>
              </ng-container>
              <ng-template #loadingProfile>
                <div class="loading-plans">Chargement du profil de marché...</div>
              </ng-template>
            </div>
          </div>

          <div class="form-section">
            <h3>Informations de l'organisation</h3>

            <div class="form-group">
              <label for="organizationName">Nom de l'organisation *</label>
              <input
                id="organizationName"
                type="text"
                formControlName="organizationName"
                class="form-control"
                [class.error]="isFieldInvalid('organizationName')"
                placeholder="Ex: Enclume Numérique">
              <div class="error-message" *ngIf="isFieldInvalid('organizationName')">
                {{ getErrorMessage('organizationName') }}
              </div>
            </div>

            <div class="form-group">
              <label for="organizationEmail">Email de l'organisation *</label>
              <input
                id="organizationEmail"
                type="email"
                formControlName="organizationEmail"
                class="form-control"
                [class.error]="isFieldInvalid('organizationEmail')"
                placeholder="contact@mon-entreprise.com">
              <div class="error-message" *ngIf="isFieldInvalid('organizationEmail')">
                {{ getErrorMessage('organizationEmail') }}
              </div>
              <small class="form-hint">Cet email servira d'identifiant pour accéder à l'espace organisation.</small>
            </div>

            <div class="form-group">
              <label for="organizationActivityDomain">Domaine d'activité</label>
              <select
                id="organizationActivityDomain"
                formControlName="organizationActivityDomain"
                class="form-control"
                [class.error]="isFieldInvalid('organizationActivityDomain')">
                <option [value]="null">Sélectionner un domaine</option>
                <option value="Sociétés de négoce international (Trading Companies)">Sociétés de négoce international (Trading Companies)</option>
                <option value="Sociétés de transport et logistique">Sociétés de transport et logistique</option>
                <option value="Sociétés de transit et de commissionnaire en douane">Sociétés de transit et de commissionnaire en douane</option>
                <option value="Sociétés industrielles exportatrices">Sociétés industrielles exportatrices</option>
                <option value="Sociétés d'importation et de distribution">Sociétés d'importation et de distribution</option>
                <option value="Sociétés de conseil en commerce international">Sociétés de conseil en commerce international</option>
                <option value="Entreprises d'emballage et de conditionnement pour l'export">Entreprises d'emballage et de conditionnement pour l'export</option>
                <option value="Autre">Autre</option>
              </select>
              <div class="error-message" *ngIf="isFieldInvalid('organizationActivityDomain')">
                {{ getErrorMessage('organizationActivityDomain') }}
              </div>
            </div>

            <div class="form-group">
              <label for="organizationAddress">Adresse complète *</label>
              <textarea
                id="organizationAddress"
                rows="3"
                formControlName="organizationAddress"
                class="form-control"
                [class.error]="isFieldInvalid('organizationAddress')"
                placeholder="Numéro, rue \ncode postal ville \n{{ selectedMarketProfile?.countryName }}"></textarea>
              <div class="error-message" *ngIf="isFieldInvalid('organizationAddress')">
                {{ getErrorMessage('organizationAddress') }}
              </div>
            </div>

            <div class="form-row">
              <div class="form-group half-width">
                <label for="organizationCountry">Pays (code ISO) *</label>
                <input
                  id="organizationCountry"
                  type="text"
                  readonly
                  maxlength="2"
                  formControlName="organizationCountry"
                  class="form-control"
                  [class.error]="isFieldInvalid('organizationCountry')"
                  placeholder="{{ selectedMarketProfile?.countryCodeIsoAlpha2 }}">
                <div class="error-message" *ngIf="isFieldInvalid('organizationCountry')">
                  {{ getErrorMessage('organizationCountry') }}
                </div>
                <small class="form-hint">Code pays ISO à 2 lettres.</small>
              </div>

              <div class="form-group half-width">
                <label for="organizationPhone">Téléphone (indicatif international) *</label>
                <input
                  id="organizationPhone"
                  type="tel"
                  formControlName="organizationPhone"
                  class="form-control"
                  [class.error]="isFieldInvalid('organizationPhone')"
                  placeholder="{{selectedMarketProfile?.phonePrefix}} 123456789'">
                <div class="error-message" *ngIf="isFieldInvalid('organizationPhone')">
                  {{ getErrorMessage('organizationPhone') }}
                </div>
                <small class="form-hint">Format international recommandé (ex : +33123456789).</small>
              </div>
            </div>
          </div>

          <div class="form-section">
            <h3>Authentification du compte organisation</h3>

            <div class="form-group">
              <label for="organizationPassword">Mot de passe *</label>
              <input
                id="organizationPassword"
                type="password"
                formControlName="organizationPassword"
                class="form-control"
                [class.error]="isFieldInvalid('organizationPassword')"
                (focus)="onPasswordFieldFirstFocus()">
              <div class="error-message" *ngIf="isFieldInvalid('organizationPassword')">
                {{ getErrorMessage('organizationPassword') }}
              </div>
              <small class="form-hint">Ce mot de passe sera utilisé pour vous connecter à l'espace organisation.</small>
            </div>

            <div class="form-group">
              <label for="organizationConfirmPassword">Confirmer le mot de passe *</label>
              <input
                id="organizationConfirmPassword"
                type="password"
                formControlName="organizationConfirmPassword"
                class="form-control"
                [class.error]="isFieldInvalid('organizationConfirmPassword')">
              <div class="error-message" *ngIf="isFieldInvalid('organizationConfirmPassword')">
                {{ getErrorMessage('organizationConfirmPassword') }}
              </div>
            </div>
          </div>

          <div class="form-section">
            <h3>Plan tarifaire</h3>
            <div class="form-group">
              <label for="pricingPlanId">Sélectionner un plan</label>
              <div class="pricing-info-bar">
                Possibilité de changer de plan tarifaire et demande de devis personnalisé à tout moment.
              </div>
              <ng-container *ngIf="loadingPlans; else plansLoaded">
                <div class="loading-plans">Chargement des plans...</div>
              </ng-container>
              <ng-template #plansLoaded>
                <select id="pricingPlanId" formControlName="pricingPlanId" class="pricing-plan-select">
                  <option *ngFor="let plan of pricingPlans" [value]="plan.id">
                    {{ plan.name }} -
                    <ng-container *ngIf="plan.pricePerMonth !== null; else pricePerRequest">
                      <ng-container *ngIf="plan.pricePerMonth === 0; else paidPlan">
                        Gratuit
                      </ng-container>
                      <ng-template #paidPlan>
                        {{ plan.pricePerMonth }} 
                        @if (currencySymbol$ | async; as symbol) {
                          {{ symbol }}
                        } @else {
                          €
                        }/mois
                      </ng-template>
                    </ng-container>
                    <ng-template #pricePerRequest>
                      <ng-container *ngIf="plan.pricePerRequest !== null; else unlimited">
                        {{ plan.pricePerRequest }} 
                        @if (currencySymbol$ | async; as symbol) {
                          {{ symbol }}
                        } @else {
                          €
                        }/requête
                      </ng-container>
                      <ng-template #unlimited>Quota illimité</ng-template>
                    </ng-template>
                    <ng-container *ngIf="plan.monthlyQuota">
                      ({{ plan.monthlyQuota | number }} requêtes/mois)
                    </ng-container>
                    <ng-container *ngIf="!plan.monthlyQuota && plan.trialPeriodDays">
                      ({{ plan.trialPeriodDays }} jours d'essai)
                    </ng-container>
                  </option>
                </select>
                <small class="form-hint">
                  <a routerLink="/pricing" target="_blank">Voir le détail des plans</a>
                </small>
              </ng-template>
            </div>
          </div>

          <div class="form-actions">
            <button type="submit" class="btn btn-primary" [disabled]="registerForm.invalid || isLoading">
              <ng-container *ngIf="isLoading; else createAccount">
                Création en cours...
              </ng-container>
              <ng-template #createAccount>
                Créer mon compte organisation
              </ng-template>
            </button>
          </div>

          <div class="form-footer">
            <p>Déjà administrateur ? <a routerLink="/auth/login">Se connecter</a></p>
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
    .register-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 80vh;
      padding: 2rem;
    }

    .register-card {
      background: white;
      padding: 2rem;
      border-radius: 12px;
      box-shadow: 0 4px 20px rgba(0,0,0,0.1);
      width: 100%;
      max-width: 680px;
    }

    h2 {
      color: #2c3e50;
      margin-bottom: 0.5rem;
      text-align: center;
    }

    p {
      color: #7f8c8d;
      margin-bottom: 2rem;
      text-align: center;
    }

    .register-form {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
    }

    .form-section {
      border: 2px solid #d5dbdb;
      border-radius: 10px;
      padding: 1.5rem;
      background: #fafbfc;
    }

    .form-section h3 {
      margin: 0 0 1rem 0;
      color: #2c3e50;
      font-size: 1.2rem;
    }

    .form-group {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .form-row {
      display: flex;
      flex-wrap: wrap;
      gap: 1rem;
    }

    .half-width {
      flex: 1 1 240px;
    }

    label {
      font-weight: 500;
      color: #2c3e50;
    }

    .form-control {
      padding: 0.75rem;
      border: 3px solid #bdc3c7;
      border-radius: 6px;
      font-size: 1rem;
      transition: border-color 0.3s;
      background-color: white;
    }

    .form-control:focus {
      outline: none;
      border-color: #3498db;
      border-width: 3px;
    }

    .form-control.error {
      border-color: #e74c3c;
      border-width: 3px;
    }

    .form-control[readonly] {
      background-color: #f5f5f5;
      border-color: #95a5a6;
      border-width: 3px;
      cursor: not-allowed;
    }

    .form-control[readonly]:focus {
      border-color: #95a5a6;
      background-color: #f5f5f5;
    }

    .error-message {
      color: #e74c3c;
      font-size: 0.875rem;
      margin-top: -0.25rem;
    }

    .success-message {
      color: #27ae60;
      font-size: 0.95rem;
      text-align: center;
      font-weight: 600;
    }

    .form-hint {
      color: #95a5a6;
      font-size: 0.85rem;
    }

    .pricing-info-bar {
      text-align: center;
      padding: 0.75rem 1rem;
      margin-bottom: 1rem;
      background-color: #f8f9fa;
      border: 2px solid #d5dbdb;
      border-radius: 6px;
      color: #495057;
      font-size: 0.9rem;
      font-style: italic;
      display: block;
      width: 100%;
    }

    .pricing-plan-select {
      background-color: #2c3e50 !important;
      color: #ecf0f1 !important;
      height: 2.5rem !important;
      min-height: 2.5rem !important;
      max-height: 2.5rem !important;
      padding: 0.5rem 0.75rem !important;
      line-height: 1.5 !important;
      border: 3px solid #34495e !important;
      border-radius: 6px !important;
      font-size: 1rem !important;
      width: 100% !important;
      box-sizing: border-box !important;
    }

    .pricing-plan-select option {
      background-color: #2c3e50 !important;
      color: #ecf0f1 !important;
      padding: 0.5rem !important;
    }

    .pricing-plan-select:focus {
      border-color: #3498db !important;
      border-width: 3px !important;
      background-color: #34495e !important;
      outline: none !important;
    }

    .form-actions {
      text-align: center;
      margin-top: 0.5rem;
    }

    .btn {
      padding: 0.75rem 1.5rem;
      border: none;
      border-radius: 6px;
      font-size: 1rem;
      cursor: pointer;
      width: 100%;
      transition: background-color 0.3s;
    }

    .btn-primary {
      background-color: #3498db;
      color: white;
    }

    .btn-primary:hover:not(:disabled) {
      background-color: #2980b9;
    }

    .btn-primary:disabled {
      background-color: #bdc3c7;
      cursor: not-allowed;
    }

    .form-footer {
      text-align: center;
      margin-top: 0.5rem;
    }

    .form-footer a {
      color: #3498db;
      text-decoration: none;
    }

    .form-footer a:hover {
      text-decoration: underline;
    }
  `]
})
export class RegisterComponent implements OnInit {
  private fb = inject(FormBuilder);
  private registerService = inject(RegisterService);
  private route = inject(ActivatedRoute);
  private pricingPlanService = inject(PricingPlanService);
  private marketProfileService = inject(MarketProfileService);
  private currencyService = inject(CurrencyService);

  isLoading = false;
  errorMessage = '';
  successMessage = '';
  pricingPlans: PricingPlan[] = [];
  loadingPlans = false;
  marketProfiles: MarketProfile[] = [];
  loadingProfiles = false;
  selectedMarketProfile: MarketProfile | null = null;
  private passwordFieldFirstFocus = false;
  currencySymbol$ = this.currencyService.getCurrencySymbol();

  registerForm: FormGroup = this.fb.group({
    marketVersion: [null, [Validators.required]],
    organizationName: ['', [Validators.required, Validators.minLength(1), Validators.maxLength(255)]],
    organizationEmail: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    organizationAddress: ['', [Validators.required, Validators.minLength(5), Validators.maxLength(512)]],
    organizationActivityDomain: [null],
    organizationCountry: ['', [Validators.required, Validators.pattern(/^[A-Za-z]{2}$/)]],
    organizationPhone: ['', [Validators.required, Validators.pattern(/^[+0-9\s().-]{5,32}$/)]],
    organizationPassword: ['', [Validators.required, Validators.minLength(8)]],
    organizationConfirmPassword: ['', [Validators.required]],
    pricingPlanId: [null]
  }, {
    validators: [this.organizationPasswordMatchValidator]
  });

  ngOnInit() {
    // Précharger la devise pour qu'elle soit disponible immédiatement
    this.currencySymbol$.subscribe({
      next: (symbol) => {
        console.log('✅ RegisterComponent: Symbole de devise chargé:', symbol);
      },
      error: (err) => {
        console.error('❌ RegisterComponent: Erreur lors du chargement de la devise:', err);
      }
    });
    
    // Charger automatiquement le profil de marché depuis l'environnement
    const marketVersion = environment.marketVersion;
    if (marketVersion) {
      // Définir la valeur dans le formulaire
      this.registerForm.patchValue({ marketVersion: marketVersion });
      
      // Charger le profil de marché
      this.marketProfileService.getMarketProfileByVersion(marketVersion).subscribe({
        next: (profile) => {
          this.selectedMarketProfile = profile;
          this.marketProfiles = [profile]; // Pour l'affichage si nécessaire
          
          // Pré-remplir les champs avec les valeurs du profil
          this.registerForm.patchValue({
            organizationCountry: profile.countryCodeIsoAlpha2,
            organizationPhone: profile.phonePrefix + ' '
          }, { emitEvent: false });
          
          // Charger les plans tarifaires pour ce marché
          this.loadPricingPlans(marketVersion);
        },
        error: (err) => {
          console.error('Erreur lors du chargement du profil de marché:', err);
        }
      });
    }
  }

  loadPricingPlans(marketVersion?: string) {
    this.loadingPlans = true;
    const marketVersionToUse = marketVersion || (this.registerForm.get('marketVersion')?.value);
    
    // Utiliser marketVersion si disponible, sinon utiliser environment.marketVersion
    const planMarketVersion = marketVersionToUse || (environment as any).marketVersion || undefined;
    
    this.pricingPlanService.getActivePricingPlans(planMarketVersion).subscribe({
      next: (plans) => {
        this.pricingPlans = plans;
        this.loadingPlans = false;
        
        // Vérifier d'abord les query params pour un planId spécifique
        this.route.queryParams.pipe(take(1)).subscribe((params: any) => {
          const planId = params['planId'];
          if (planId) {
            const planExists = plans.some(plan => plan.id === +planId);
            if (planExists) {
              this.registerForm.patchValue({ pricingPlanId: +planId });
              return; // Ne pas sélectionner le plan gratuit si un planId est fourni
            }
          }
          
          // Si aucun plan n'est sélectionné, sélectionner le plan gratuit par défaut
          const currentPlanId = this.registerForm.get('pricingPlanId')?.value;
          if (!currentPlanId) {
            // Trouver le plan gratuit (pricePerMonth === 0)
            const freePlan = plans.find(plan => plan.pricePerMonth === 0);
            if (freePlan) {
              this.registerForm.patchValue({ pricingPlanId: freePlan.id });
            }
          }
        });
      },
      error: (err) => {
        console.error('Erreur lors du chargement des plans tarifaires:', err);
        this.loadingPlans = false;
      }
    });
  }

  onPasswordFieldFirstFocus() {
    if (!this.passwordFieldFirstFocus) {
      this.passwordFieldFirstFocus = true;
      const passwordControl = this.registerForm.get('organizationPassword');
      if (passwordControl && passwordControl.value) {
        passwordControl.setValue('');
      }
    }
  }

  organizationPasswordMatchValidator(form: FormGroup) {
    const password = form.get('organizationPassword');
    const confirmPassword = form.get('organizationConfirmPassword');

    if (password && confirmPassword && password.value !== confirmPassword.value) {
      confirmPassword.setErrors({ passwordMismatch: true });
      return { organizationPasswordMismatch: true };
    }

    if (confirmPassword?.hasError('passwordMismatch')) {
      const errors = { ...confirmPassword.errors };
      delete errors['passwordMismatch'];
      confirmPassword.setErrors(Object.keys(errors).length > 0 ? errors : null);
    }
    return null;
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.registerForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  getErrorMessage(fieldName: string): string {
    const field = this.registerForm.get(fieldName);

    if (field?.errors) {
      if (field.errors['required']) {
        return 'Ce champ est requis';
      }
      if (field.errors['email']) {
        return 'Email invalide';
      }
      if (field.errors['minlength']) {
        const requiredLength = field.errors['minlength'].requiredLength;
        return `Minimum ${requiredLength} caractères requis`;
      }
      if (field.errors['maxlength']) {
        const maxLength = field.errors['maxlength'].requiredLength;
        return `Maximum ${maxLength} caractères autorisés`;
      }
      if (field.errors['pattern']) {
        if (fieldName === 'organizationCountry') {
          return 'Le pays doit être un code ISO à 2 lettres (ex : FR)';
        }
        if (fieldName === 'organizationPhone') {
          return 'Le numéro doit être au format international (ex : +33123456789)';
        }
      }
      if (field.errors['passwordMismatch']) {
        return 'Les mots de passe ne correspondent pas';
      }
    }

    return '';
  }

  onSubmit() {
    if (this.registerForm.invalid) {
      Object.values(this.registerForm.controls).forEach(control => control.markAsTouched());
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.successMessage = '';

    const formValue = this.registerForm.value;
    const organizationEmail = (formValue.organizationEmail || '').toLowerCase();
    const organizationCountry = (formValue.organizationCountry || '').toUpperCase();
    const organizationPassword = formValue.organizationPassword || '';

    const payload = {
      username: organizationEmail,
      email: organizationEmail,
      password: organizationPassword,
      firstName: formValue.organizationName || '',
      lastName: '',
      organizationName: formValue.organizationName,
      organizationEmail,
      organizationAddress: formValue.organizationAddress,
      organizationActivityDomain: formValue.organizationActivityDomain || null,
      organizationCountry,
      organizationPhone: formValue.organizationPhone,
      organizationPassword,
      pricingPlanId: formValue.pricingPlanId || null,
      marketVersion: formValue.marketVersion || null
    };

    this.registerService.registerUser(payload).subscribe({
      next: (response) => {
        this.isLoading = false;
        const orgEmail = response.organizationEmail || organizationEmail;
        this.successMessage = response.message || `Un email de confirmation a été envoyé à ${orgEmail}.`;
        this.registerForm.reset({ pricingPlanId: null });
      },
      error: (error) => {
        this.isLoading = false;
        this.errorMessage = this.handleError(error);
      }
    });
  }

  private handleError(error: any): string {
    if (error.status === 409) {
      return 'Un compte existe déjà avec cet email.';
    } else if (error.status === 400) {
      return error.error?.error || 'Données invalides. Vérifiez les informations saisies.';
    } else if (error.status === 0) {
      return 'Impossible de joindre le serveur. Vérifiez votre connexion.';
    } else {
      return 'Une erreur est survenue lors de la création du compte organisation.';
    }
  }
}
