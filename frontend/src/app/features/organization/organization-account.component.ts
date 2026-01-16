import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {
  OrganizationAccountService,
  OrganizationCollaborator,
  OrganizationInfo
} from '../../core/services/organization-account.service';
import { AuthService } from '../../core/services/auth.service';
import { take } from 'rxjs/operators';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-organization-account',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <ng-container *ngIf="(isOrganizationAccount$ | async); else notOrganization">
      <div class="page-container">
        <h1>Mon organisation</h1>

        <section class="card" *ngIf="organization">
          <div class="card-header">
            <div>
              <h2>{{ organization.name }}</h2>
              <p class="subtitle">{{ organization.email }}</p>
            </div>
            <div class="stats">
              <div>
                <span class="stat-value">{{ organization.userCount ?? 0 }}</span>
                <span class="stat-label">Collaborateurs</span>
              </div>
              <div>
                <span class="stat-value">{{ organization.currentMonthUsage ?? 0 }}</span>
                <span class="stat-label">Requêtes ce mois</span>
              </div>
            </div>
          </div>

          <div class="card-body">
            <div class="info-grid">
              <div>
                <h3>Adresse</h3>
                <p>{{ organization.address }}</p>
              </div>
              <div>
                <h3>Pays</h3>
                <p>{{ organization.country }}</p>
              </div>
              <div>
                <h3>Téléphone</h3>
                <p>{{ organization.phone }}</p>
              </div>
              <div *ngIf="organization.pricingPlanId">
                <h3>Plan tarifaire</h3>
                <p>#{{ organization.pricingPlanId }}</p>
              </div>
            </div>
          </div>
        </section>

        <section class="card">
          <div class="card-header">
            <h2>Inviter un collaborateur</h2>
          </div>
          <div class="card-body">
            <form [formGroup]="inviteForm" (ngSubmit)="invite()" class="form-grid">
              <div class="form-group">
                <label for="username">Nom d'utilisateur *</label>
                <input id="username" type="text" formControlName="username" [class.error]="isInvalid('username')">
                <p class="error-message" *ngIf="isInvalid('username')">{{ getError('username') }}</p>
              </div>
              <div class="form-group">
                <label for="email">Email *</label>
                <input
                  id="email"
                  type="email"
                  formControlName="email"
                  [class.error]="isInvalid('email') || emailConflictsWithOrganization()">
                <p class="error-message" *ngIf="isInvalid('email')">{{ getError('email') }}</p>
                <p class="error-message" *ngIf="!isInvalid('email') && emailConflictsWithOrganization()">
                  L'email du collaborateur doit être différent de celui de l'organisation.
                </p>
              </div>
              <div class="form-group">
                <label for="firstName">Prénom *</label>
                <input id="firstName" type="text" formControlName="firstName" [class.error]="isInvalid('firstName')">
                <p class="error-message" *ngIf="isInvalid('firstName')">{{ getError('firstName') }}</p>
              </div>
              <div class="form-group">
                <label for="lastName">Nom *</label>
                <input id="lastName" type="text" formControlName="lastName" [class.error]="isInvalid('lastName')">
                <p class="error-message" *ngIf="isInvalid('lastName')">{{ getError('lastName') }}</p>
              </div>
              <div class="form-actions">
                <button type="submit" [disabled]="inviteForm.invalid || inviting || emailConflictsWithOrganization()">
                  <span *ngIf="inviting">Envoi...</span>
                  <span *ngIf="!inviting">Inviter</span>
                </button>
              </div>
            </form>
            <p class="success-message" *ngIf="inviteSuccess">{{ inviteSuccess }}</p>
            <p class="error-message" *ngIf="inviteError">{{ inviteError }}</p>
            <p class="warning-message" *ngIf="emailConflictsWithOrganization()">
              L'email du collaborateur doit être différent de celui de l'organisation.
            </p>
          </div>
        </section>

        <section class="card">
          <div class="card-header">
            <h2>Collaborateurs</h2>
          </div>
          <div class="card-body">
            <p *ngIf="collaborators.length === 0" class="empty-state">
              Aucun collaborateur pour le moment.
            </p>
            <table class="collaborators-table" *ngIf="collaborators.length > 0">
              <thead>
              <tr>
                <th>Nom d'utilisateur</th>
                <th>Prénom</th>
                <th>Nom</th>
                <th>Email</th>
                <th>Date d'ajout</th>
                <th>Statut</th>
                <th>Actions</th>
              </tr>
              </thead>
              <tbody>
              <tr *ngFor="let collaborator of collaborators">
                <td>{{ collaborator.username }}</td>
                <td>{{ collaborator.firstName || '-' }}</td>
                <td>{{ collaborator.lastName || '-' }}</td>
                <td>{{ collaborator.email || '-' }}</td>
                <td>{{ collaborator.joinedAt ? (collaborator.joinedAt | date:'short') : '-' }}</td>
                <td>
                  <span class="status-badge" [class.status-active]="collaborator.enabled === true" [class.status-inactive]="collaborator.enabled === false">
                    {{ collaborator.enabled === true ? 'Actif' : (collaborator.enabled === false ? 'Désactivé' : 'Inconnu') }}
                  </span>
                </td>
                <td class="actions-cell">
                  <label class="toggle-switch">
                    <input 
                      type="checkbox" 
                      [checked]="collaborator.enabled === true"
                      (change)="toggleCollaboratorStatus(collaborator)"
                      [disabled]="toggling === collaborator.keycloakUserId || deleting === collaborator.keycloakUserId">
                    <span class="toggle-slider"></span>
                  </label>
                  <button 
                    class="btn btn-danger" 
                    (click)="deleteCollaborator(collaborator.keycloakUserId)"
                    [disabled]="toggling === collaborator.keycloakUserId || deleting === collaborator.keycloakUserId">
                    Supprimer
                  </button>
                </td>
              </tr>
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </ng-container>

    <ng-template #notOrganization>
      <div class="not-organization">
        <h2>Accès réservé</h2>
        <p>Cette page est réservée au compte organisation. Connectez-vous avec l'email de l'organisation pour gérer vos collaborateurs.</p>
      </div>
    </ng-template>
  `,
  styles: [`
    .page-container {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
      padding: 2rem;
    }

    h1 {
      margin: 0;
      color: #1f2937;
    }

    .card {
      background: #ffffff;
      border-radius: 12px;
      box-shadow: 0 4px 24px rgba(15, 23, 42, 0.08);
      overflow: hidden;
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1.5rem 2rem;
      border-bottom: 1px solid #e5e7eb;
      gap: 1rem;
    }

    .card-body {
      padding: 1.5rem 2rem;
    }

    .subtitle {
      margin: 0;
      color: #6b7280;
    }

    .stats {
      display: flex;
      gap: 2rem;
    }

    .stat-value {
      display: block;
      font-size: 1.5rem;
      font-weight: 600;
      color: #2563eb;
    }

    .stat-label {
      color: #6b7280;
      font-size: 0.875rem;
    }

    .info-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 1.5rem;
    }

    .info-grid h3 {
      margin-bottom: 0.25rem;
      color: #374151;
    }

    .info-grid p {
      margin: 0;
      color: #4b5563;
    }

    .form-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 1rem;
      margin-bottom: 1rem;
    }

    .form-group {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    label {
      font-weight: 600;
      color: #1f2937;
    }

    input {
      padding: 0.75rem 1rem;
      border: 1px solid #d1d5db;
      border-radius: 8px;
      font-size: 1rem;
      transition: border-color 0.2s ease;
    }

    input:focus {
      outline: none;
      border-color: #2563eb;
      box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
    }

    input.error {
      border-color: #dc2626;
    }

    .form-actions {
      display: flex;
      align-items: flex-end;
    }

    button {
      padding: 0.75rem 1.5rem;
      background-color: #2563eb;
      color: #fff;
      border: none;
      border-radius: 8px;
      font-size: 1rem;
      cursor: pointer;
      transition: background-color 0.2s ease;
      min-width: 140px;
    }

    button:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    button:not(:disabled):hover {
      background-color: #1d4ed8;
    }

    .error-message {
      color: #dc2626;
      font-size: 0.875rem;
      margin: 0;
    }

    .success-message {
      color: #16a34a;
      font-weight: 500;
      margin: 0;
    }

    .warning-message {
      color: #fb923c;
      font-weight: 500;
      margin: 0;
    }

    .not-organization {
      padding: 3rem;
      margin: 2rem;
      text-align: center;
      background: linear-gradient(135deg, #f97316 0%, #ea580c 100%);
      color: white;
      border-radius: 12px;
      box-shadow: 0 12px 32px rgba(249, 115, 22, 0.35);
    }

    .collaborators-table {
      width: 100%;
      border-collapse: collapse;
    }

    .collaborators-table th,
    .collaborators-table td {
      padding: 0.75rem;
      text-align: left;
      border-bottom: 1px solid #e5e7eb;
    }

    .collaborators-table th {
      font-weight: 600;
      color: #374151;
    }

    .empty-state {
      color: #6b7280;
      margin: 0;
    }

    .actions-cell {
      display: flex;
      gap: 0.5rem;
      align-items: center;
    }

    .btn-warning {
      background-color: #f59e0b;
      color: white;
      padding: 0.5rem 1rem;
      border: none;
      border-radius: 6px;
      cursor: pointer;
      font-size: 0.875rem;
      transition: background-color 0.2s ease;
    }

    .btn-warning:hover:not(:disabled) {
      background-color: #d97706;
    }

    .btn-warning:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .btn-danger {
      background-color: #dc2626;
      color: white;
      padding: 0.5rem 1rem;
      border: none;
      border-radius: 6px;
      cursor: pointer;
      font-size: 0.875rem;
      transition: background-color 0.2s ease;
    }

    .btn-danger:hover:not(:disabled) {
      background-color: #b91c1c;
    }

    .btn-danger:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .status-badge {
      display: inline-block;
      padding: 0.25rem 0.75rem;
      border-radius: 12px;
      font-size: 0.875rem;
      font-weight: 600;
    }

    .status-active {
      background-color: #d1fae5;
      color: #065f46;
    }

    .status-inactive {
      background-color: #fee2e2;
      color: #991b1b;
    }

    .toggle-switch {
      position: relative;
      display: inline-block;
      width: 50px;
      height: 24px;
      margin-right: 0.5rem;
    }

    .toggle-switch input {
      opacity: 0;
      width: 0;
      height: 0;
    }

    .toggle-slider {
      position: absolute;
      cursor: pointer;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background-color: #ccc;
      transition: 0.3s;
      border-radius: 24px;
    }

    .toggle-slider:before {
      position: absolute;
      content: "";
      height: 18px;
      width: 18px;
      left: 3px;
      bottom: 3px;
      background-color: white;
      transition: 0.3s;
      border-radius: 50%;
    }

    .toggle-switch input:checked + .toggle-slider {
      background-color: #16a34a;
    }

    .toggle-switch input:checked + .toggle-slider:before {
      transform: translateX(26px);
    }

    .toggle-switch input:disabled + .toggle-slider {
      opacity: 0.6;
      cursor: not-allowed;
    }

    @media (max-width: 768px) {
      .card-header {
        flex-direction: column;
        align-items: flex-start;
      }

      .stats {
        width: 100%;
        justify-content: space-between;
      }
    }
  `]
})
export class OrganizationAccountComponent implements OnInit {
  private organizationAccountService = inject(OrganizationAccountService);
  private authService = inject(AuthService);
  private fb = inject(FormBuilder);

  organization: OrganizationInfo | null = null;
  collaborators: OrganizationCollaborator[] = [];

  inviteForm: FormGroup = this.fb.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    firstName: ['', [Validators.required, Validators.minLength(2)]],
    lastName: ['', [Validators.required, Validators.minLength(2)]],
  });

  inviting = false;
  inviteSuccess = '';
  inviteError = '';
  toggling: string | null = null;
  deleting: string | null = null;
  isOrganizationAccount$: Observable<boolean> = this.authService.isOrganizationAccount();

  ngOnInit(): void {
    this.isOrganizationAccount$.pipe(take(1)).subscribe(isOrg => {
      if (isOrg) {
        this.loadOrganization();
        this.loadCollaborators();
      }
    });
  }

  loadOrganization(): void {
    this.organizationAccountService.getMyOrganization().subscribe({
      next: (organization) => {
        this.organization = organization;
      },
      error: (error) => {
        console.error('Erreur lors du chargement de l’organisation', error);
      }
    });
  }

  loadCollaborators(): void {
    this.organizationAccountService.getMyCollaborators().subscribe({
      next: (response) => {
        this.organization = response.organization;
        this.collaborators = response.collaborators ?? [];
      },
      error: (error) => {
        console.error('Erreur lors du chargement des collaborateurs', error);
      }
    });
  }

  invite(): void {
    if (this.inviteForm.invalid) {
      this.inviteForm.markAllAsTouched();
      return;
    }

    this.inviteSuccess = '';
    this.inviteError = '';

    if (this.organization && this.inviteForm.value.email && this.organization.email &&
        this.inviteForm.value.email.toLowerCase() === this.organization.email.toLowerCase()) {
      this.inviteError = 'L\'email du collaborateur doit être différent de celui de l\'organisation.';
      return;
    }

    if (!this.organization) {
      this.inviteError = 'Les informations de l\'organisation ne sont pas encore disponibles. Réessayez dans un instant.';
      return;
    }

    this.inviting = true;

    this.organizationAccountService.inviteCollaborator(this.inviteForm.value).subscribe({
      next: (response) => {
        this.inviting = false;
        this.inviteSuccess = response.message || 'Invitation envoyée.';
        this.inviteForm.reset();
        this.loadCollaborators();
      },
      error: (error) => {
        this.inviting = false;
        const message = error?.error?.message || 'Impossible d’envoyer l’invitation.';
        this.inviteError = message;
      }
    });
  }

  isInvalid(controlName: string): boolean {
    const control = this.inviteForm.get(controlName);
    return !!(control && control.invalid && (control.dirty || control.touched));
  }

  getError(controlName: string): string {
    const control = this.inviteForm.get(controlName);
    if (!control || !control.errors) {
      return '';
    }
    if (control.errors['required']) {
      return 'Ce champ est requis';
    }
    if (control.errors['email']) {
      return 'Email invalide';
    }
    if (control.errors['minlength']) {
      return `Minimum ${control.errors['minlength'].requiredLength} caractères`;
    }
    return 'Valeur invalide';
  }

  emailConflictsWithOrganization(): boolean {
    if (!this.organization) {
      return false;
    }
    const emailControl = this.inviteForm.get('email');
    const emailValue = emailControl?.value?.toLowerCase();
    const organizationEmail = this.organization.email?.toLowerCase();
    return !!emailValue && !!organizationEmail && emailValue === organizationEmail;
  }

  toggleCollaboratorStatus(collaborator: OrganizationCollaborator): void {
    const isCurrentlyEnabled = collaborator.enabled === true;
    const action = isCurrentlyEnabled ? 'désactiver' : 'activer';
    
    if (!confirm(`Êtes-vous sûr de vouloir ${action} ce collaborateur ?`)) {
      // Recharger les collaborateurs pour réinitialiser l'état du toggle
      this.loadCollaborators();
      return;
    }
    
    this.toggling = collaborator.keycloakUserId;
    this.inviteError = '';
    
    const operation = isCurrentlyEnabled
      ? this.organizationAccountService.disableCollaborator(collaborator.keycloakUserId)
      : this.organizationAccountService.enableCollaborator(collaborator.keycloakUserId);
    
    operation.subscribe({
      next: () => {
        this.toggling = null;
        this.loadCollaborators();
        const successMessage = isCurrentlyEnabled 
          ? 'Collaborateur désactivé avec succès' 
          : 'Collaborateur activé avec succès';
        alert(successMessage);
      },
      error: (error) => {
        this.toggling = null;
        const message = error?.error?.message || `Impossible de ${action} le collaborateur.`;
        alert('Erreur: ' + message);
        // Recharger pour réinitialiser l'état du toggle en cas d'erreur
        this.loadCollaborators();
      }
    });
  }

  deleteCollaborator(keycloakUserId: string): void {
    if (!confirm('Êtes-vous sûr de vouloir supprimer ce collaborateur ? Cette action est irréversible.')) {
      return;
    }
    this.deleting = keycloakUserId;
    this.inviteError = '';
    this.organizationAccountService.deleteCollaborator(keycloakUserId).subscribe({
      next: () => {
        this.deleting = null;
        this.loadCollaborators();
        alert('Collaborateur supprimé avec succès');
      },
      error: (error) => {
        this.deleting = null;
        const message = error?.error?.message || 'Impossible de supprimer le collaborateur.';
        alert('Erreur: ' + message);
      }
    });
  }
}

