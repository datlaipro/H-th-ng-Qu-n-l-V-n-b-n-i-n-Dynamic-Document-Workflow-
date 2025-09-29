// app.routes.ts (Angular standalone)
import { Routes } from '@angular/router';
import { PublicLayout } from './layouts/public-layout/public-layout.component';
import { AuthGuard } from './core/auth/auth-guard';
import { RoleGuard } from './core/auth/role.guard';
import { DocumentListComponent } from './features/documents/document-list.component';
import { IncomingList } from './features/documents/incoming/incoming-list/incoming-list.component'; 
import { DocumentDetailComponent } from './features/documents/documentsDetail/document-detail.component';

export const routes: Routes = [
  // Trang login KHÔNG cần guard
  //   {
  //     path: 'login',
  //     loadComponent: () => import('./features/login/login.component').then((m) => m.LoginComponent),
  //   },

  // Khu vực đã đăng nhập
  {
    path: '',
    component: PublicLayout,
    // canActivate: [AuthGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'documentList', component:DocumentListComponent },
      { path: 'IncomingList', component:IncomingList },
      { path: 'DocumentDetail', component:DocumentDetailComponent },

      //   {
      //     path: 'dashboard',
      //     loadComponent: () =>
      //       import('./features/dashboard/dashboard.component')
      //         .then(m => m.DashboardComponent),
      //   },
      //   {
      //     path: 'documents',
      //     loadComponent: () =>
      //       import('./features/documents/list/document-list.component')
      //         .then(m => m.DocumentListComponent),
      //   },
      //   {
      //     path: 'documents/:id',
      //     loadComponent: () =>
      //       import('./features/documents/detail/document-detail.component')
      //         .then(m => m.DocumentDetailComponent),
      //   },
      //   {
      //     path: 'documents/new',
      //     loadComponent: () =>
      //       import('./features/documents/form/document-form.component')
      //         .then(m => m.DocumentFormComponent),
      //     canActivate: [RoleGuard],
      //     data: { roles: ['EMPLOYEE', 'LEADER'] },
      //   },
    ],
  },

  // Bắt mọi thứ còn lại → login (hoặc 'dashboard' tuỳ flow của bạn)
  { path: '**', redirectTo: 'login' },
];
