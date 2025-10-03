// app.routes.ts
import { Routes, UrlSegment, UrlMatchResult } from '@angular/router';
import { PublicLayout } from './layouts/public-layout/public-layout.component';
import { AuthGuard } from './core/auth/auth-guard';
import { RoleCanMatch } from './core/auth/role.guard';

import { AddUserComponent } from './layouts/admin-layout/add-user-management/add-user.component';
import { DocumentListComponent } from './features/documents/document-list.component';
import { IncomingList } from './features/documents/incoming/incoming-list/incoming-list.component';
import { DocumentDetailComponent } from './features/documents/documentsDetail/document-detail.component';
import { UsersManagementComponent } from './layouts/admin-layout/users-management/users-management.component';
import { DocumentCreateComponent } from './features/documents/document-create/document-create.component';
export function docIdMatcher(url: UrlSegment[]): UrlMatchResult | null {
  if (url.length === 2 && url[0].path === 'documents' && /^\d+$/.test(url[1].path)) {
    return { consumed: url };
  }
  return null;
}
export const routes: Routes = [
  {
    path: 'forbidden',
    loadComponent: () =>
      import('./features/errors/forbidden.component').then((m) => m.ForbiddenComponent),
  },
  // Login (không cần guard)
  {
    path: 'login',
    loadComponent: () => import('./features/login/login.component').then((m) => m.LoginComponent),
  },

  // Khung tổng sau khi đăng nhập
  {
    path: '',
    component: PublicLayout,
    canActivate: [AuthGuard], // ai đăng nhập cũng vào được layout này
    children: [
      // mặc định về khu user
      { path: '', pathMatch: 'full', redirectTo: 'document-list' },

      // ==== USER ZONE (mọi role đã đăng nhập) ====
      { path: 'document-list', component: DocumentListComponent },
      { path: 'incoming-list', component: IncomingList },
      { path: 'documents/new', component: DocumentCreateComponent },
      { matcher: docIdMatcher, component: DocumentDetailComponent },

      // ==== ADMIN ZONE (LEADER/ADMIN) ====
      {
        path: 'admin',
        canMatch: [RoleCanMatch], // <-- kiểm tra role 1 lần tại parent
        data: { roles: ['LEADER', 'ADMIN'] }, // <-- cấu hình role ở parent
        children: [
          { path: 'document-list', component: DocumentListComponent },

          { path: 'list-user', component: UsersManagementComponent },
          { path: 'create-user', component: AddUserComponent },
          // Thêm route admin khác ở đây, KHÔNG cần lặp guard:
          // { path: 'users', component: AdminUsersComponent },
          // { path: 'settings', component: AdminSettingsComponent },
        ],
      },

      // (Tuỳ chọn) trang 403 nếu role không đủ
      // {
      //   path: 'forbidden',
      //   loadComponent: () =>
      //     import('./features/errors/forbidden.component').then(m => m.ForbiddenComponent),
      // },
    ],
  },

  // Fallback
  { path: '**', redirectTo: '' },
];
