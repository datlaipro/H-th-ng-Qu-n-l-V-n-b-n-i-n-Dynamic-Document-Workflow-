import { ApplicationConfig, importProvidersFrom } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { withCredentialsInterceptor } from './core/auth/with-credentials.interceptor';
import { provideClientHydration } from '@angular/platform-browser';
import { HttpClientXsrfModule } from '@angular/common/http';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([
        withCredentialsInterceptor, // interceptor ở trên
      ])
    ),
    importProvidersFrom(HttpClientXsrfModule.withOptions({
      cookieName: 'XSRF-TOKEN',
      headerName: 'X-XSRF-TOKEN',
    })),
    provideClientHydration(),
  ]
};
