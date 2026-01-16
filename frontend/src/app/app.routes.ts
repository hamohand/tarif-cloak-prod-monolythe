import { Routes } from '@angular/router';
import { RouterModule } from '@angular/router';

import { HomeComponent } from './shared/home/home.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { authGuard } from './core/guards/auth.guard';
import { AUTH_ROUTES } from './features/auth/auth.routes';
import {RegisterComponent} from './features/auth/register/register.component';
import {LoginComponent} from './features/auth/login/login.component';
import {SearchComponent} from './tarif/search/search.component';
import {TarifComponent} from './tarif/home/tarif.component';
import {TARIF_ROUTES} from './tarif/tarif.routes';
import { StatsComponent } from './features/admin/stats/stats.component';
import { OrganizationsComponent } from './features/admin/organizations/organizations.component';
import { UserDashboardComponent } from './features/user/dashboard/dashboard.component';
import { AlertsComponent } from './features/user/alerts/alerts.component';
import { InvoicesComponent } from './features/user/invoices/invoices.component';
import { InvoiceDetailComponent } from './features/user/invoices/invoice-detail.component';
import { InvoicesAdminComponent } from './features/admin/invoices/invoices-admin.component';
import { InvoiceDetailAdminComponent } from './features/admin/invoices/invoice-detail-admin.component';
import { QuoteRequestsAdminComponent } from './features/admin/quote-requests/quote-requests-admin.component';
import { PendingRegistrationsComponent } from './features/admin/pending-registrations/pending-registrations.component';
import { PricingPlansComponent } from './features/pricing/pricing-plans.component';
import { OrganizationAccountComponent } from './features/organization/organization-account.component';
import { OrganizationStatsComponent } from './features/organization/organization-stats.component';
import { QuoteRequestsComponent } from './features/organization/quote-requests.component';
import { collaboratorGuard } from './core/guards/collaborator.guard';
import { organizationGuard } from './core/guards/organization.guard';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'pricing', component: PricingPlansComponent },
  {
    path: 'auth',
    children: AUTH_ROUTES
  },
  {
    path: 'recherche',
    children: TARIF_ROUTES,
    component: TarifComponent,
    //component: DashboardComponent,
    canActivate: [authGuard, collaboratorGuard]
  },
  {
    path: 'dashboard',
    component: UserDashboardComponent,
    canActivate: [authGuard, collaboratorGuard]
  },
  {
    path: 'alerts',
    component: AlertsComponent,
    canActivate: [authGuard, collaboratorGuard]
  },
  {
    path: 'organization/account',
    component: OrganizationAccountComponent,
    canActivate: [authGuard, organizationGuard]
  },
  {
    path: 'organization/stats',
    component: OrganizationStatsComponent,
    canActivate: [authGuard, organizationGuard]
  },
  {
    path: 'organization/invoices',
    component: InvoicesComponent,
    canActivate: [authGuard, organizationGuard]
  },
  {
    path: 'organization/invoices/:id',
    component: InvoiceDetailComponent,
    canActivate: [authGuard, organizationGuard]
  },
  {
    path: 'organization/quote-requests',
    component: QuoteRequestsComponent,
    canActivate: [authGuard, organizationGuard]
  },
  {
    path: 'admin/stats',
    component: StatsComponent,
    canActivate: [authGuard]
  },
  {
    path: 'admin/organizations',
    component: OrganizationsComponent,
    canActivate: [authGuard]
  },
  {
    path: 'admin/invoices',
    component: InvoicesAdminComponent,
    canActivate: [authGuard]
  },
  {
    path: 'admin/invoices/:id',
    component: InvoiceDetailAdminComponent,
    canActivate: [authGuard]
  },
  {
    path: 'admin/quote-requests',
    component: QuoteRequestsAdminComponent,
    canActivate: [authGuard]
  },
  {
    path: 'admin/pending-registrations',
    component: PendingRegistrationsComponent,
    canActivate: [authGuard]
  },
  { path: '**', redirectTo: '' }
];

export const AppRoutingModule = RouterModule.forRoot(routes, {
  onSameUrlNavigation: 'reload',
  scrollPositionRestoration: 'enabled'
});


