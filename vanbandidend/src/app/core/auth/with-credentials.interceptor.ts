// core/auth/with-credentials.interceptor.ts
// file này có chức năng tự gửi cookie cùng api 
import { HttpInterceptorFn } from '@angular/common/http';
export const withCredentialsInterceptor: HttpInterceptorFn = (req, next) => {
  const cloned = req.clone({ withCredentials: true });
  return next(cloned);
};
