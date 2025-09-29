// core/auth/app-initializer.ts

// kiểm tra phiên đăng nhập trước khi render giao diện 
import { APP_INITIALIZER, Provider } from '@angular/core';
import { AuthService } from './auth.service';
export const authInitializer: Provider = {
  provide: APP_INITIALIZER,
  multi: true,
  useFactory: (auth: AuthService) => () => new Promise<void>(resolve => {
    auth.me().subscribe({
      next: u => { auth.user.set(u); resolve(); },
      error: () => resolve()
    });
  }),
  deps: [AuthService]
};
