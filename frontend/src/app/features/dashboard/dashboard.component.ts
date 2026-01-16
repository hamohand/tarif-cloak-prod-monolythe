import { Component, OnInit, inject } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { JsonPipe, AsyncPipe, CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, JsonPipe, AsyncPipe, RouterLink],
  template: `
    <div class="dashboard-container" *ngIf="!(isOrganizationAccount | async); else organizationNotice">
      <h2>Tableau de bord</h2>

      <div class="user-info-card">
        <h3>Informations utilisateur</h3>
        Prénom Nom : {{ userInfo.name }}<br>
        Username : {{ userInfo.username }}<br>
        Email : {{ userInfo.email}}
        <pre>{{ userInfo | json }}</pre>
      </div>

      <div class="stats-container">
        <div class="stat-card">
          <h4>📈 Projets</h4>
          <p>5 projets actifs</p>
        </div>

        <div class="stat-card">
          <h4>👥 Utilisateurs</h4>
          <p>12 membres</p>
        </div>

        <div class="stat-card">
          <h4>✅ Tâches</h4>
          <p>24 tâches terminées</p>
        </div>
      </div>
    </div>

    <ng-template #organizationNotice>
      <div class="organization-notice">
        <h2>Compte organisation</h2>
        <p>Vous êtes connecté avec le compte de l'organisation. Utilisez la page <a routerLink="/organization/account">Mon organisation</a> pour gérer vos collaborateurs, suivre les statistiques globales et gérer votre plan tarifaire.</p>
      </div>
    </ng-template>
  `,
  styles: [`
    .dashboard-container {
      padding: 2rem;
    }

    h2 {
      color: #2c3e50;
      margin-bottom: 2rem;
    }

    .user-info-card {
      background: #f8f9fa;
      padding: 1.5rem;
      border-radius: 8px;
      margin-bottom: 2rem;
    }

    .user-info-card h3 {
      margin-top: 0;
      color: #2c3e50;
    }

    pre {
      background: white;
      padding: 1rem;
      border-radius: 4px;
      overflow-x: auto;
    }

    .stats-container {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 1.5rem;
      margin-top: 2rem;
    }

    .stat-card {
      background: white;
      padding: 1.5rem;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      text-align: center;
    }

    .stat-card h4 {
      color: #2c3e50;
      margin-bottom: 0.5rem;
    }

    .organization-notice {
      padding: 3rem;
      margin: 2rem;
      background: linear-gradient(135deg, #2563eb 0%, #1e3a8a 100%);
      color: white;
      border-radius: 12px;
      box-shadow: 0 8px 30px rgba(37, 99, 235, 0.35);
      text-align: center;
    }

    .organization-notice a {
      color: #facc15;
      font-weight: 600;
      text-decoration: underline;
    }

    .organization-notice a:hover {
      color: #fde68a;
    }
  `]
})
export class DashboardComponent implements OnInit {
  private authService = inject(AuthService);
  userInfo: any;
  isOrganizationAccount = this.authService.isOrganizationAccount();

  ngOnInit() {
    this.userInfo = this.authService.getUserInfo();
  }
}
