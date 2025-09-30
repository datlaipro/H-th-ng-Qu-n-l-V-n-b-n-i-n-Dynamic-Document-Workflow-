import { CanActivateFn, ActivatedRouteSnapshot, Router } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';
// Đây là Route Guard theo vai trò. Nó chặn/cho phép truy cập một route dựa trên role của user hiện tại.
export const RoleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const roles = route.data?.['roles'] as string[] | undefined;
  if (!roles) return true;// không có roles nào được yêu cầu, cho qua
  const u = auth.user();// lấy user hiện tại từ AuthService
  if (u && roles.includes(u.role)) return true;// user có role phù hợp, cho qua
  router.navigateByUrl('/'); return false;
};