import { Component, inject } from '@angular/core';
import { CommonModule, AsyncPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { OrganizationAccountService } from '../../core/services/organization-account.service';
import { combineLatest, of } from 'rxjs';
import { map, switchMap, catchError, distinctUntilChanged } from 'rxjs/operators';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, AsyncPipe],
  template: `
    <div class="home-container">
      <h3>Bienvenue sur Enclume-Numérique</h3>
      <p>Votre solution complète pour la conformité tarifaire.</p>

      <!-- Présentation de l'API Recherche HS-code - Affichée uniquement pour les utilisateurs non connectés -->
      <section class="api-presentation" *ngIf="!(isAuthenticated$ | async)">
        <div class="api-header">
          <h2>🔍 Recherche de HS-Code</h2>
          <p class="api-subtitle">Recherche intelligente et multilingue dans le système harmonisé douanier</p>
        </div>
        
        <div class="api-content">
          <div class="api-features-grid">
            <div class="api-feature">
              <div class="api-icon">🌍</div>
              <h3>Recherche Multilingue</h3>
              <p>Recherchez des codes HS dans plusieurs langues : français, anglais, chinois, arabe et plus encore.</p>
            </div>
            
            <div class="api-feature">
              <div class="api-icon">🤖</div>
              <h3>Intelligence Artificielle</h3>
              <p>Moteur de recherche alimenté par l'IA pour des résultats précis et pertinents basés sur vos descriptions de produits.</p>
            </div>
            
            <div class="api-feature">
              <div class="api-icon">📊</div>
              <h3>Hiérarchie Complète</h3>
              <p>Accédez à tous les niveaux : Sections, Chapitres, Positions 4 chiffres et Positions 6 chiffres.</p>
            </div>
            
            <div class="api-feature">
              <div class="api-icon">⚡</div>
              <h3>Recherche en Cascade</h3>
              <p>Algorithme intelligent qui explore automatiquement les différents niveaux de classification pour trouver le code le plus précis.</p>
            </div>
            
            <div class="api-feature">
              <div class="api-icon">📋</div>
              <h3>Recherche par Lots</h3>
              <p>Traitez plusieurs produits simultanément avec l'outil de recherche par lots pour optimiser votre workflow.</p>
            </div>
            
            <div class="api-feature">
              <div class="api-icon">🔒</div>
              <h3>Sécurisé et Fiable</h3>
              <p>API sécurisée avec authentification OAuth2 et suivi détaillé de l'utilisation pour une conformité totale.</p>
            </div>
          </div>
          
          <div class="api-endpoints">
            <h3>Exemples d'utilisation</h3>
            <div class="endpoint-list">

              <div class="endpoint-item">
                <code class="endpoint-method">Produit recherché</code>
                <code class="endpoint-path">سيارة كهربائية</code>
                <code class="endpoint-method">Réponse HS-Code</code>
                <span class="endpoint-desc">
                <b>code</b>: "8703 80" <br>
                <b>description</b>: "Véhicules, équipés uniquement d’un moteur électrique pour la propulsion"
                </span>
              </div>
              <div class="endpoint-item">
                <code class="endpoint-method">Produit recherché</code>
                <code class="endpoint-path">dattes</code>
                <code class="endpoint-method">Réponse HS-Code</code>
                <span class="endpoint-desc"><b>code</b>: "0804 10" <br>
                <b>description</b>: "Dattes"</span>
              </div>
              <div class="endpoint-item">
                <code class="endpoint-method">Produit recherché</code>
                <code class="endpoint-path">Smart phone</code>
                <code class="endpoint-method">Réponse HS-Code</code>
                <span class="endpoint-desc"><b>code</b>: "8517 13" <br>
                  <b>description</b>: "Postes téléphoniques d’usagers, y compris les téléphones intelligents et autres téléphones pour réseaux cellulaires et pour autres réseaux sans fil: - Téléphones intelligents"</span>
              </div>
              <div class="endpoint-item">
                <code class="endpoint-method">Produit recherché</code>
                <code class="endpoint-path">笔记本电脑</code>
                <code class="endpoint-method">Réponse HS-Code</code>
                <span class="endpoint-desc"><b>code</b>: "8471.30" <br>
                <b>description</b>: "Ordinateurs portables ..."</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Section pour les utilisateurs connectés avec rôle ORGANIZATION ou COLLABORATOR -->
      <section class="user-actions" *ngIf="showRequestButtons$ | async">
        @if (!(canMakeRequests$ | async)) {
          <!-- Message d'avertissement quand l'essai est terminé -->
          <div class="trial-expired-message">
            <div class="message-content">
              <h4>⚠️ Essai gratuit terminé</h4>
              @if (isOrganizationAccount$ | async) {
                <p>
                  Le quota de votre essai gratuit a été atteint et est maintenant définitivement désactivé pour votre organisation. 
                  Aucune requête HS-code n'est autorisée pour tous les collaborateurs. 
                  Veuillez <a routerLink="/organization/stats">choisir un plan tarifaire</a> ou 
                  <a routerLink="/organization/quote-requests">faire une demande de devis</a> pour continuer à utiliser le service.
                </p>
              } @else {
                <p>
                  Le quota de l'essai gratuit de votre organisation a été atteint et est maintenant définitivement désactivé. 
                  Aucune requête HS-code n'est autorisée. 
                  Veuillez contacter votre administrateur d'organisation pour choisir un plan tarifaire ou faire une demande de devis.
                </p>
              }
            </div>
          </div>
        } @else {
          <!-- Boutons de recherche HS-code (affichés seulement si l'organisation peut faire des requêtes) -->
          <h4 class="section-title">Outils de recherche HS-Code</h4>
          <div class="features primary">
            <div class="feature-card request-card">
              <div class="request-icon">🔍</div>
              <h3>Recherche d'article unique</h3>
              <p>Recherchez le code HS d'un produit spécifique en quelques secondes.</p>
              <a [routerLink]="['/recherche/search']" class="cta-button secondary">
                Rechercher un article
              </a>
            </div>
            
            <div class="feature-card request-card">
              <div class="request-icon">📋</div>
              <h3>Recherche par lots (bientôt disponible)</h3>
              <p>Traitez une liste de produits simultanément avec l'outil de recherche par lots.</p>
              <a [routerLink]="['/recherche/searchListLots']" class="cta-button secondary">
                Rechercher par lots
              </a>
            </div>
          </div>
        }
        
        <!-- Section supplémentaire pour les organisations -->
        <div class="features primary" *ngIf="isOrganizationAccount$ | async">
          <div class="feature-card advise">
            <h3>👥 Gestion d'organisation</h3>
            <p>Utilisez l'espace <strong>Mon organisation</strong> pour inviter vos collaborateurs et suivre les statistiques globales.</p>
            <a [routerLink]="['/organization/account']" class="cta-button secondary">
              Ouvrir l'espace organisation
            </a>
          </div>
        </div>
      </section>

      <div class="features secondary" *ngIf="!(isAuthenticated$ | async)">
        <div class="feature-card">
          <h3>🔐 Sécurité</h3>
          <p>Authentification et gestion fine des rôles.</p>
        </div>
        <div class="feature-card">
          <h3>⚡ Performance</h3>
          <p>Moteur de recherche optimisé pour la réglementation douanière.</p>
        </div>
        <div class="feature-card">
          <h3>📱 Responsive</h3>
          <p>Une interface adaptée à tous les usages.</p>
        </div>
      </div>

      <!-- Pied de page -->
      <footer class="home-footer">
        <p>Micro-entreprise de développement d'applications pour les entreprises<br>
        Contact : med&#64;forge-numerique.com <br>
        <a href="https://www.forge-numerique.com">Enclume-Numérique</a></p>
      </footer>
    </div>
  `,
  styles: [`
    .home-container {
      text-align: center;
      padding: 2rem;
    }

    h1 {
      color: #2c3e50;
      font-size: 2.5rem;
      margin-bottom: 1rem;
    }

    p {
      font-size: 1.2rem;
      color: #7f8c8d;
      margin-bottom: 3rem;
    }

    .features {
      display: flex;
      justify-content: center;
      gap: 2rem;
      margin-bottom: 3rem;
      flex-wrap: wrap;
    }

    .features.primary {
      margin-top: 2rem;
    }

    .features.secondary {
      margin-top: 1rem;
    }

    .feature-card {
      background: rgb(220, 220, 220);
      padding: 2rem;
      border-radius: 8px;
      box-shadow: 0 2px 5px rgba(0,0,0,0.1);
      width: 220px;
      display: flex;
      flex-direction: column;
      gap: 1rem;
      align-items: center;
    }

    .feature-card h3 {
      color: #2c3e50;
      margin-bottom: 1rem;
    }

    .feature-card.advise {
      background: linear-gradient(135deg, #1f2937, #111827);
      color: #f9fafb;
      text-align: left;
    }

    .feature-card.advise h3 {
      color: #facc15;
      margin-bottom: 0.5rem;
    }

    .cta-button {
      background-color: #3498db;
      color: white;
      padding: 1rem 2rem;
      font-size: 1.1rem;
      border: none;
      border-radius: 6px;
      cursor: pointer;
      transition: background-color 0.3s;
      text-decoration: none;
      display: inline-block;
    }

    .cta-button:hover {
      background-color: #2980b9;
    }

    .cta-button.secondary {
      background-color: #facc15;
      color: #1f2937;
      font-weight: 600;
    }

    .cta-button.secondary:hover {
      background-color: #fbbf24;
    }

    .cta-button.ghost {
      background-color: transparent;
      color: #facc15;
      border: 2px solid #facc15;
    }

    .cta-button.ghost:hover {
      background-color: rgba(250, 204, 21, 0.1);
    }

    /* Styles pour les boutons d'utilisation de requêtes */
    .user-actions {
      margin: 3rem 0;
      padding: 2rem 0;
    }

    .section-title {
      color: #1e3c72;
      font-size: 2rem;
      margin-bottom: 2rem;
      font-weight: 700;
      text-align: center;
    }

    .request-card {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      text-align: center;
      min-width: 280px;
      max-width: 350px;
    }

    .request-card h3 {
      color: white;
      margin-bottom: 0.75rem;
    }

    .request-card p {
      color: rgba(255, 255, 255, 0.9);
      font-size: 1rem;
      margin-bottom: 1.5rem;
    }

    .request-icon {
      font-size: 3.5rem;
      margin-bottom: 1rem;
      display: block;
    }

    .request-card .cta-button.secondary {
      background-color: white;
      color: #667eea;
      font-weight: 600;
      margin-top: auto;
    }

    .request-card .cta-button.secondary:hover {
      background-color: #f9fafb;
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }

    /* Styles pour le message d'avertissement essai terminé */
    .trial-expired-message {
      margin: 2rem 0;
      padding: 2rem;
      background: linear-gradient(135deg, #f39c12 0%, #e67e22 100%);
      border-radius: 12px;
      box-shadow: 0 4px 12px rgba(243, 156, 18, 0.3);
      max-width: 800px;
      margin-left: auto;
      margin-right: auto;
    }

    .message-content {
      text-align: center;
      color: white;
    }

    .message-content h4 {
      margin: 0 0 1rem 0;
      font-size: 1.5rem;
      font-weight: 700;
      color: white;
    }

    .message-content p {
      margin: 0;
      font-size: 1rem;
      line-height: 1.6;
      color: white;
    }

    .message-content a {
      color: white;
      text-decoration: underline;
      font-weight: 700;
    }

    .message-content a:hover {
      text-decoration: none;
      opacity: 0.9;
    }

    /* Styles pour la présentation de l'API */
    .api-presentation {
      background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
      border-radius: 16px;
      padding: 3rem 2rem;
      margin: 3rem 0;
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
      max-width: 1200px;
      margin-left: auto;
      margin-right: auto;
    }

    .api-header {
      text-align: center;
      margin-bottom: 3rem;
    }

    .api-header h2 {
      color: #1e3c72;
      font-size: 2.2rem;
      margin-bottom: 0.5rem;
      font-weight: 700;
    }

    .api-subtitle {
      color: #6b7280;
      font-size: 1.1rem;
      margin: 0;
    }

    .api-content {
      display: flex;
      flex-direction: column;
      gap: 3rem;
    }

    .api-features-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 1.5rem;
    }

    .api-feature {
      background: white;
      padding: 2rem;
      border-radius: 12px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
      transition: transform 0.3s ease, box-shadow 0.3s ease;
      text-align: center;
    }

    .api-feature:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 20px rgba(0, 0, 0, 0.12);
    }

    .api-icon {
      font-size: 3rem;
      margin-bottom: 1rem;
    }

    .api-feature h3 {
      color: #1e3c72;
      font-size: 1.3rem;
      margin-bottom: 0.75rem;
      font-weight: 600;
    }

    .api-feature p {
      color: #4b5563;
      font-size: 0.95rem;
      line-height: 1.6;
      margin: 0;
    }

    .api-endpoints {
      background: white;
      padding: 2rem;
      border-radius: 12px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    }

    .api-endpoints h3 {
      color: #1e3c72;
      font-size: 1.5rem;
      margin-bottom: 1.5rem;
      font-weight: 600;
    }

    .endpoint-list {
      display: flex;
      flex-direction: column;
      gap: 1rem;
    }

    .endpoint-item {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 1rem;
      background: #f8f9fa;
      border-radius: 8px;
      border-left: 4px solid #3498db;
      transition: background-color 0.2s ease;
    }

    .endpoint-item:hover {
      background: #e9ecef;
    }

    .endpoint-method {
      background: #3498db;
      color: white;
      padding: 0.4rem 0.8rem;
      border-radius: 4px;
      font-weight: 600;
      font-size: 0.85rem;
      min-width: 60px;
      text-align: center;
    }

    .endpoint-path {
      color: #1e3c72;
      font-weight: 600;
      font-family: 'Courier New', monospace;
      font-size: 0.95rem;
      flex: 0 0 auto;
    }

    .endpoint-desc {
      text-align: left;
      color: #6b7280;
      font-size: 0.9rem;
      flex: 1;
    }

    .api-example {
      background: white;
      padding: 2rem;
      border-radius: 12px;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    }

    .api-example h3 {
      color: #1e3c72;
      font-size: 1.5rem;
      margin-bottom: 1.5rem;
      font-weight: 600;
    }

    .code-block {
      background: #1e293b;
      border-radius: 8px;
      padding: 1.5rem;
      overflow-x: auto;
    }

    .code-block pre {
      margin: 0;
      color: #e2e8f0;
      font-family: 'Courier New', monospace;
      font-size: 0.9rem;
      line-height: 1.6;
    }

    .code-block code {
      color: #e2e8f0;
    }

    /* Styles pour le pied de page */
    .home-footer {
      margin-top: 4rem;
      padding-top: 2rem;
      border-top: 1px solid #e1e8ed;
      text-align: center;
      color: #7f8c8d;
      font-size: 0.9rem;
    }

    .home-footer p {
      margin: 0;
      font-size: 0.9rem;
      background-color: #1e293b;
      color: white;
      padding: 1rem;
      border-radius: 8px;
    }

    @media (max-width: 768px) {
      .feature-card {
        width: 100%;
      }

      .api-presentation {
        padding: 2rem 1rem;
        margin: 2rem 0;
      }

      .api-header h2 {
        font-size: 1.8rem;
      }

      .api-features-grid {
        grid-template-columns: 1fr;
      }

      .endpoint-item {
        flex-direction: column;
        align-items: flex-start;
        gap: 0.5rem;
      }

      .endpoint-path {
        width: 100%;
        word-break: break-all;
      }

      .home-footer {
        margin-top: 2rem;
        padding-top: 1.5rem;
      }
    }
  `]
})
export class HomeComponent {
  private authService = inject(AuthService);
  private organizationAccountService = inject(OrganizationAccountService);

  isAuthenticated$ = this.authService.isAuthenticated();
  isOrganizationAccount$ = this.authService.isOrganizationAccount();
  isCollaboratorAccount$ = this.authService.isCollaboratorAccount();

  // Observable combiné pour afficher les boutons d'utilisation de requêtes
  showRequestButtons$ = combineLatest([
    this.isAuthenticated$,
    this.isOrganizationAccount$,
    this.isCollaboratorAccount$
  ]).pipe(
    map(([isAuthenticated, isOrganization, isCollaborator]) => 
      isAuthenticated && (isOrganization || isCollaborator)
    )
  );

  // Observable pour vérifier si l'organisation peut faire des requêtes
  canMakeRequests$ = combineLatest([
    this.isAuthenticated$,
    this.isOrganizationAccount$,
    this.isCollaboratorAccount$
  ]).pipe(
    switchMap(([isAuthenticated, isOrganization, isCollaborator]) => {
      const hasOrgOrCollabAccount = isOrganization || isCollaborator;
      if (!isAuthenticated || !hasOrgOrCollabAccount) {
        // Pas d'organisation, donc pas de restriction
        return of(true);
      }
      // Vérifier l'état de l'organisation
      return this.organizationAccountService.getOrganizationStatus().pipe(
        map(status => status.canMakeRequests),
        catchError(err => {
          console.error('Erreur lors de la vérification de l\'état de l\'organisation:', err);
          // En cas d'erreur, autoriser par défaut pour ne pas bloquer l'interface
          return of(true);
        })
      );
    }),
    distinctUntilChanged()
  );
}
