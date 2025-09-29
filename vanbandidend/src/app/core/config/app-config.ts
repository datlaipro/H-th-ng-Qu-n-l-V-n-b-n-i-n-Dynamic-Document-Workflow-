import { provideHttpClient, withInterceptors } from '@angular/common/http';

export const withCredentialsInterceptor = (req: any, next: any) => {
  return next(req.clone({ withCredentials: true }));
};

export const appConfig = {
  providers: [
    provideHttpClient(
      withInterceptors([
        withCredentialsInterceptor,
        // …(giữ error interceptor nếu có)
      ])
    ),
  ]
};
