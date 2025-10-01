// core/auth/role.guard.ts
import { inject } from '@angular/core';
import { CanMatchFn, Route, UrlSegment, Router, UrlTree } from '@angular/router';
import { AuthService } from './auth.service';

export const RoleCanMatch: CanMatchFn = (
  route: Route,
  segments: UrlSegment[]
): boolean | UrlTree => {
  const router = inject(Router);
  const auth   = inject(AuthService);

  const roles = route.data?.['roles'] as string[] | undefined;
  const u = auth.user(); // SessionUser | null (signal getter)

  // 1) Chưa đăng nhập -> về /login + redirect
  if (!u) {
    const target = '/' + segments.map(s => s.path).join('/');
    return router.createUrlTree(['/login'], { queryParams: { redirect: target } });
  }

  // 2) Không yêu cầu role -> cho qua
  if (!roles || roles.length === 0) return true;

  // 3) Có yêu cầu role -> kiểm tra
  if (roles.includes(u.role)) return true;

  // 4) Sai role -> về /forbidden (hoặc trang bạn muốn)
  return router.createUrlTree(['/forbidden']);
};
