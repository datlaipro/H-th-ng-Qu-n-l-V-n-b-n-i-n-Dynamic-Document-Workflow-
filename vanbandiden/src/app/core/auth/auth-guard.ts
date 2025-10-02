// core/auth/auth-guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { AuthService } from './auth.service';
import { catchError, map, of, tap } from 'rxjs';

export const AuthGuard: CanActivateFn = (route, state): any => {
  const auth = inject(AuthService);
  const router = inject(Router);

  // 1) Nếu đã có user trong signal -> cho qua
  const u = auth.user();
  if (u) return true;

  // 2) Chưa có -> gọi me() để hydrate từ cookie
  return auth.me().pipe(
    tap((u) => auth.user.set(u)),
    map(() => true),
    catchError(() => {
      // Ghi nhớ URL đích để sau login quay lại
      return of(router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } }));
    })
  );
};
