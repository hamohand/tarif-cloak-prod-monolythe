import {Component, inject} from '@angular/core';
import {Article} from "../shared/model/articles";
import {SearchService} from "../services/search.service";
import * as Papa from "papaparse";
import {
    bufferCount,
    catchError,
    concatMap,
    delay,
    finalize,
    forkJoin,
    from,
    map,
    Observable,
    of,
    reduce,
    tap
} from "rxjs";
import {OAuthService} from 'angular-oauth2-oidc';

@Component({
  selector: 'app-search-list-lots',
  standalone: true,
    imports: [],
  templateUrl: './search-list-lots.component.html',
  styleUrl: './search-list-lots.component.css'
})
export class SearchListLotsComponent {
    lesarticles: Article[] = [];
    fileName: string = '';
    reponse: string = ''
    isLoading: boolean = false;
    error: string | null = null;
    isSearchComplete: boolean = false;
    isSaved: boolean = false;

    completedCount: number = 0;
    totalCount: number = 0;

    // Réglages basés sur votre analyse : 5 requêtes par minute
    private readonly BATCH_SIZE = 5; // Liste par lots de 5 d'articles
    private readonly DELAY_BETWEEN_BATCHES = 61000; // 61 secondes entre les recherches Ai de 2 lots de 5 d'articles

    //constructor(private searchService: SearchService) {}
  private searchService = inject(SearchService);
  private oauthService = inject(OAuthService);

    onFileSelected(event: Event): void {
        const input = event.target as HTMLInputElement;
        if (!input.files?.length) return;

        const file = input.files[0];
        this.fileName = file.name;
        this.isLoading = true;
        this.lesarticles = [];
        this.error = null;
        this.isSearchComplete = false;
        this.isSaved = false;
        this.completedCount = 0;
        this.totalCount = 0;

        const fileName = file.name.toLowerCase();

        // Pour les fichiers .xls, .xlsx, .ods, on les envoie au backend pour conversion en TSV.
        if (fileName.endsWith('.xls') || fileName.endsWith('.xlsx') || fileName.endsWith('.ods')) {
            this.searchService.convertFile(file).subscribe({
                next: (tsvContent: string) => {
                    this.parseCsvData(tsvContent);
                },
                error: (err: { error: any; message: any; }) => {
                    this.error = `Erreur lors de la conversion du fichier : ${err.error || err.message}`;
                    this.isLoading = false;
                }
            });
            // Pour les fichiers .csv et .tsv, on les lit directement et on les traite comme du TSV.
        } else if (fileName.endsWith('.csv') || fileName.endsWith('.tsv')) {
            const reader = new FileReader();
            reader.onload = () => {
                const fileContent = reader.result as string;
                this.parseCsvData(fileContent);
            };
            reader.onerror = () => {
                this.error = "Impossible de lire le fichier sélectionné.";
                this.isLoading = false;
            };
            reader.readAsText(file, 'UTF-8');
        } else {
            this.error = "Format de fichier non supporté. Veuillez sélectionner un fichier .csv, .tsv, .xls, .xlsx, ou .ods.";
            this.isLoading = false;
        }
    }

    private parseCsvData(csvContent: string): void {
        Papa.parse<Article>(csvContent, {
            header: true,
            skipEmptyLines: true,
            delimiter: "\t", // On utilise TOUJOURS la tabulation comme délimiteur
            transformHeader: (header: string) => {
                const normalizedHeader = header.toLowerCase().trim();
                // Si l'en-tête est 'articles', on le normalise en 'article'
                if (normalizedHeader === 'articles') {
                    return 'article';
                }
                return normalizedHeader;
            },
            complete: (results: { errors: any[]; data: Article[]; }) => {
                if (results.errors.length > 0) {
                    this.error = `Erreur d'analyse TSV : ${results.errors.map(e => e.message).join(', ')}`;
                } else {
                    this.lesarticles = results.data;
                    console.log("results: ", results);
                    console.log("les articles : "+this.lesarticles);
                    this.totalCount = this.lesarticles.length;
                    console.log("Nombre d'articles : "+this.lesarticles.length);
                }
                this.isLoading = false;
            },
            error: (err: any) => {
                this.error = `Erreur de lecture des données CSV : ${err.message}`;
                this.isLoading = false;
            }
        });
    }

    /**
     * Lance la recherche en respectant une limite de 5 requêtes par minute.
     */
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
      //
        if (!this.lesarticles?.length) return;

        this.isLoading = true;
        this.error = null;
        this.isSearchComplete = false;
        this.completedCount = 0;

        from(this.lesarticles).pipe(
            // 1. Crée des paquets de 5 articles
            bufferCount(this.BATCH_SIZE),

            // 2. Traite chaque paquet l'un après l'autre
            //concatMap((batchOfArticles: any[], index: number) => {
            concatMap((batchOfArticles: Article[], index: number) => {
                    console.log(`Traitement du paquet n°${index + 1} (${batchOfArticles.length} articles)...`);

                // Crée les requêtes pour le paquet en cours
                const searchRequests$ = batchOfArticles.map(article =>
                    this.createArticleSearchObservable(article).pipe(
                        tap(() => this.completedCount++)
                    )
                );

                // Exécute les requêtes du paquet en parallèle et, une fois terminées,
                // attend 61 secondes avant de laisser concatMap passer au paquet suivant.
                return forkJoin(searchRequests$).pipe(
                    tap(() => {
                        // Ce log est important pour savoir que le système est en pause
                        if ((index + 1) * this.BATCH_SIZE < this.totalCount) {
                            console.log(`Paquet n°${index + 1} traité. Pause de 61 secondes avant le prochain...`);
                        }
                    }),
                    delay(this.DELAY_BETWEEN_BATCHES)
                );
            }),

            // 3. Rassemble les résultats de tous les paquets
            //reduce((acc: string | any[], batchResults: any) => acc.concat(batchResults), [] as string[]),
            reduce((acc: string[], batchResults: string[]) => acc.concat(batchResults), [] as string[]),

            // 4. Se déclenche quand TOUT est terminé
            finalize(() => {
                this.isLoading = false;
                this.isSearchComplete = true;
            })
        ).subscribe({
            next: (allResults: string[]) => {
                allResults.forEach((code, index) => {
                    if (this.lesarticles[index]) {
                        this.lesarticles[index].code = code;
                    }
                });
                console.log("Traitement de tous les paquets terminé.", this.lesarticles);
            },
            error: (err: any) => {
                this.error = 'Une erreur majeure est survenue pendant le traitement des paquets.';
                console.error(err);
            }
        });
    }

    // fichier résultat
    private createArticleSearchObservable(article: Article): Observable<string> {
        if (!article.article || !article.article.trim()) {
            return of(article.code || '').pipe(delay(0));
        }
        // seule la valeur de 'code' est extraite et utilisée pour chaque article `code`
        return this.searchService.searchCodes(article.article).pipe(

            map((response: any) => {
                let results;
                try {
                    // On s'assure que les résultats sont bien un objet/tableau et non une chaîne JSON
                    results = typeof response === 'string' ? JSON.parse(response) : response;
                } catch (e) {
                    console.error(e);
                    return article.code || ''; // Retourne le code original si le JSON est invalide
                }

                // Extrait le code du premier résultat, s'il est disponible
                if (Array.isArray(results) && results.length > 0 && results[0].code) {
                    //return results[0].code;
                    for (let i = 0; i < results.length; i++) {
                        if (results[i].code) {
                            if (i == 0) { // éviter la virgule au début
                                this.reponse = results[i].code;
                                continue;
                            }
                            this.reponse = this.reponse + ", " + results[i].code;
                            return this.reponse;
                        }
                    }
                }


                return article.code || ''; // Retourne le code original si aucun résultat n'est trouvé
            }),
            catchError((err: any) => {
                console.error(err);
                if (!this.error) {
                    this.error = 'Certaines requêtes ont échoué. Les codes originaux sont conservés.';
                }
                return of(article.code || '');
            })
        );
    }

    saveAndDownload(): void {
        if (!this.lesarticles?.length) {
            return;
        }

        const tsvContent = Papa.unparse(this.lesarticles, {
            delimiter: "\t",
            header: true,
        });

        const blob = new Blob([tsvContent], { type: 'text/tab-separated-values;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");

        // On s'assure que le nom du fichier de sortie a TOUJOURS l'extension .tsv
        const baseFileName = this.fileName.substring(0, this.fileName.lastIndexOf('.')) || this.fileName;
        link.setAttribute("href", url);
        link.setAttribute("download", `resultat-${baseFileName}.tsv`);

        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
        this.isSaved = true;
    }

  // En savoir plus ...
  showMore: boolean = false;
  showMoreDonnees: boolean = false;
  showMore2: boolean = false;
  showMoreResultat: boolean = false;
  toggleMoreFormatDonnees() {
    this.showMore = !this.showMore;
  }
  toggleAfficheDonnees() {
    this.showMoreDonnees = !this.showMoreDonnees;
  }
  toggleMoreFormatResultat() {
    this.showMore2 = !this.showMore2;
  }
  toggleAfficheResultat() {
    this.showMoreResultat = !this.showMoreResultat;
  }
}


