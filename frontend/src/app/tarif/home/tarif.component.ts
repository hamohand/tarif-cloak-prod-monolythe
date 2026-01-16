import {Component, OnInit} from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <div class="container">
    <header>
      <h1>Universal Customs Tariff for International Trade</h1>
        <h2>Harmonised System : HS Code</h2>
      <h3>Recherche multilingue - Multilingual search - 多语言搜索 - بحث متعدد اللغات</h3>

    </header>
    <main>
      <router-outlet></router-outlet>

    </main>
    <footer>
      <p>&copy; 2025 Enclume-Numérique</p>
    </footer>

    </div>
  `,
  styles: [`
      :host { /* Utiliser :host pour cibler le composant lui-même */
          display: block;
          background-color: hsl(220, 15%, 92%); /* Fond de page plus assombri */
          min-height: 100vh; /* S'assurer que le fond couvre toute la hauteur */
          font-family: 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; /* Police moderne et propre */
          color: hsl(210, 10%, 25%); /* Couleur de texte par défaut plus douce */
      }

      .container {
          max-width: 1200px;
          margin: 0 auto;
          padding: 30px;
          background-color: #e8e8e8; /* Fond plus assombri pour le contenu principal */
          border-radius: 8px; /* Bords légèrement arrondis pour un aspect moderne */
          box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05); /* Ombre douce pour un effet de "flottaison" */
          min-height: calc(100vh - 60px); /* Ajuster la hauteur minimale du conteneur */
          display: flex;
          flex-direction: column;
      }

      header h1 {
        text-align: center;
        color: hsl(210, 100%, 35%); /* Un bleu plus vif mais toujours professionnel */
        font-weight: 700; /* Plus de contraste pour le titre principal */
        text-shadow: none; /* Suppression de l'ombre de texte pour une lecture plus nette */
        margin-bottom: 5px;
      }
      h2 {
          text-align: center;
          margin-bottom: 15px; /* Réduire l'espace pour une meilleure cohésion */
          color: hsl(210, 100%, 35%); /* Un bleu plus vif mais toujours professionnel */
          font-weight: 500;
      }
      h3 { /* Style pour le titre de recherche multilingue */
        text-align: center;
        color: hsl(210, 100%, 35%); /* Un bleu plus vif mais toujours professionnel */
        font-size: 1rem;
        font-weight: 400;
        margin-top: 10px;
      }
      main {
          flex-grow: 1; /* Permet au contenu principal de prendre l'espace disponible */
          padding-bottom: 20px; /* Ajout d'un peu de padding en bas du main */
      }
      footer {
          margin-top: 30px;
          border-top: 1px solid hsl(210, 15%, 90%); /* Bordure plus fine et plus claire */
          padding-top: 15px;
          text-align: center;
          color: hsl(210, 10%, 60%); /* Gris doux pour le pied de page */
          font-size: 0.85rem; /* Légèrement plus petit pour la discrétion */
      }

  `]
})
export class TarifComponent {
  //title = 'Chapitres';
  //title = 'Positions-4';
  title = 'Positions-6';
  expired = false;

  constructor() {}

}
