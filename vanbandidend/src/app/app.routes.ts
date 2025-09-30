// app.routes.ts
import { Routes } from '@angular/router';
import { PublicLayout } from './layouts/public-layout/public-layout.component';
import { AuthGuard } from './core/auth/auth-guard';
import { RoleGuard } from './core/auth/role.guard';

import { DocumentListComponent } from './features/documents/document-list.component';
import { IncomingList } from './features/documents/incoming/incoming-list/incoming-list.component';
import { DocumentDetailComponent } from './features/documents/documentsDetail/document-detail.component';
import { UsersManagementComponent } from './layouts/admin-layout/users-management/users-management.component';

export const routes: Routes = [
  // Login không cần guard
  {
    path: 'login',
    loadComponent: () =>
      import('./features/login/login.component').then(m => m.LoginComponent),
  },

  // Khu vực đã đăng nhập
  {
    path: '',
    component: PublicLayout,
    canActivate: [AuthGuard],
    children: [
      // Đổi redirect về route có thật
      { path: '', redirectTo: 'document-list', pathMatch: 'full' },

      { path: 'document-list', component: DocumentListComponent },
      { path: 'incoming-list', component: IncomingList },
      // nếu detail có id, nên để '/documents/:id'
      { path: 'documents/:id', component: DocumentDetailComponent },

      // Users đặt ĐÚNG bên trong children
      {
        path: 'admin/create-user',
        component: UsersManagementComponent,
        canActivate: [RoleGuard],
        data: { roles: ['LEADER', 'ADMIN'] },
      },
    ],
  },

  // Fallback
  { path: '**', redirectTo: 'login' },
];
