import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { map, catchError, shareReplay, tap } from 'rxjs/operators';
import { MarketProfileService } from './market-profile.service';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class CurrencyService {
  private marketProfileService = inject(MarketProfileService);
  private cachedCurrency: string | null = null;
  private cachedSymbol$: Observable<string> | null = null;

  /**
   * Récupère le symbole de devise du marché actuel.
   */
  getCurrencySymbol(): Observable<string> {
    // Si on a déjà un Observable en cache, le retourner
    if (this.cachedSymbol$) {
      console.log('✅ CurrencyService: Utilisation du symbole en cache:', this.cachedCurrency);
      return this.cachedSymbol$;
    }

    // Si on a déjà la devise en cache, créer un Observable partagé
    if (this.cachedCurrency) {
      const symbol = this.getSymbolForCurrency(this.cachedCurrency);
      console.log('✅ CurrencyService: Création Observable depuis cache:', symbol, 'pour', this.cachedCurrency);
      this.cachedSymbol$ = of(symbol).pipe(shareReplay(1));
      return this.cachedSymbol$;
    }

    // Récupérer la version de marché depuis l'environnement
    let marketVersion: string | undefined = undefined;
    
    // Essayer plusieurs façons d'accéder à marketVersion
    if (environment.marketVersion) {
      marketVersion = environment.marketVersion;
    } else if ((environment as any).marketVersion) {
      marketVersion = (environment as any).marketVersion;
    } else if ((environment as any)['marketVersion']) {
      marketVersion = (environment as any)['marketVersion'];
    }

    if (!marketVersion || marketVersion.trim() === '') {
      // Valeur par défaut si marketVersion n'est pas défini
      const isProduction = (environment as any).production === true;
      marketVersion = isProduction ? 'DZ' : 'DEFAULT';
      console.warn('⚠️ CurrencyService: marketVersion non trouvé dans environment, utilisation de:', marketVersion);
    }

    console.log('🔍 CurrencyService: Récupération de la devise pour marketVersion:', marketVersion);
    console.log('🔍 CurrencyService: environment.marketVersion:', environment.marketVersion);
    console.log('🔍 CurrencyService: (environment as any).marketVersion:', (environment as any).marketVersion);

    // Récupérer le profil de marché et extraire la devise
    this.cachedSymbol$ = this.marketProfileService.getMarketProfileByVersion(marketVersion).pipe(
      tap(profile => {
        console.log('✅ CurrencyService: Profil de marché récupéré:', profile);
        console.log('✅ CurrencyService: Code devise:', profile.currencyCode);
        console.log('✅ CurrencyService: marketVersion du profil:', profile.marketVersion);
      }),
      map(profile => {
        const currency = profile.currencyCode || 'EUR';
        this.cachedCurrency = currency;
        const symbol = this.getSymbolForCurrency(currency);
        console.log('✅ CurrencyService: Symbole de devise calculé:', symbol, 'pour', currency);
        console.log('✅ CurrencyService: Vérification mapping DZD -> DA:', currency === 'DZD' ? 'DA' : 'autre');
        return symbol;
      }),
      catchError((error) => {
        console.error('❌ CurrencyService: Erreur lors de la récupération du profil de marché:', error);
        console.error('❌ CurrencyService: Détails de l\'erreur:', error.message, error.stack);
        // En cas d'erreur, utiliser EUR par défaut
        this.cachedCurrency = 'EUR';
        const defaultSymbol = '€';
        this.cachedSymbol$ = of(defaultSymbol).pipe(shareReplay(1));
        return of(defaultSymbol);
      }),
      shareReplay(1) // Partager l'Observable pour éviter plusieurs appels
    );
    
    return this.cachedSymbol$;
  }

  /**
   * Récupère le code de devise du marché actuel.
   */
  getCurrencyCode(): Observable<string> {
    if (this.cachedCurrency) {
      return of(this.cachedCurrency);
    }

    let marketVersion: string | undefined = undefined;
    
    if ((environment as any).marketVersion) {
      marketVersion = (environment as any).marketVersion;
    } else if ((environment as any)['marketVersion']) {
      marketVersion = (environment as any)['marketVersion'];
    }

    if (!marketVersion) {
      const isProduction = (environment as any).production === true;
      marketVersion = isProduction ? 'DZ' : 'DEFAULT';
    }

    return this.marketProfileService.getMarketProfileByVersion(marketVersion).pipe(
      map(profile => {
        const currency = profile.currencyCode || 'EUR';
        this.cachedCurrency = currency;
        return currency;
      }),
      catchError(() => {
        this.cachedCurrency = 'EUR';
        return of('EUR');
      })
    );
  }

  /**
   * Convertit un code de devise en symbole.
   * Méthode publique pour être utilisée dans les composants.
   */
  public getSymbolForCurrency(currency: string): string {
    const currencyMap: { [key: string]: string } = {
      'EUR': '€',
      'DZD': 'DA',
      'USD': '$',
      'GBP': '£',
      'JPY': '¥',
      'CNY': '¥',
      'CHF': 'CHF',
      'CAD': 'C$',
      'AUD': 'A$',
      'MAD': 'DH'
    };
    return currencyMap[currency.toUpperCase()] || currency;
  }

  /**
   * Formate un montant avec la devise du marché.
   */
  formatAmount(amount: number, currencyCode?: string): Observable<string> {
    if (currencyCode) {
      return of(this.formatWithCurrency(amount, currencyCode));
    }

    return this.getCurrencyCode().pipe(
      map(code => this.formatWithCurrency(amount, code))
    );
  }

  /**
   * Formate un montant avec un code de devise spécifique.
   */
  private formatWithCurrency(amount: number, currencyCode: string): string {
    const symbol = this.getSymbolForCurrency(currencyCode);
    
    // Pour certaines devises, le symbole est placé après le montant
    if (currencyCode === 'DZD' || currencyCode === 'MAD') {
      return `${amount.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} ${symbol}`;
    }
    
    // Pour les autres devises, le symbole est placé avant
    return `${symbol}${amount.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  }
}

