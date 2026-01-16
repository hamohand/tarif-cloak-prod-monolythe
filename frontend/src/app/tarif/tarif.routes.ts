import { Routes } from '@angular/router';
import { SearchComponent } from './search/search.component';
import {SearchListLotsComponent} from "./search-list-lots/search-list-lots.component";

export const TARIF_ROUTES: Routes = [
    // Article unique
//     { path: '', redirectTo: 'search', pathMatch: 'full' },
//     { path: 'search', component: SearchComponent },
//     { path: '**', redirectTo: 'search' }

    //Liste d'articles: examine par lots de 5 articles il attend 61s après chaque lot pour 'soulager le LLM'.
    { path: '', redirectTo: 'search', pathMatch: 'full' },
    { path: 'search', component: SearchComponent },
    { path: 'searchListLots', component: SearchListLotsComponent },
    { path: '**', redirectTo: 'search'}

    ///////////////////
    // Liste d'articles
    // { path: '', redirectTo: 'searchList', pathMatch: 'full' },
    // { path: 'searchList', component: SearchListComponent },
    // { path: '**', redirectTo: 'searchList' }
];
