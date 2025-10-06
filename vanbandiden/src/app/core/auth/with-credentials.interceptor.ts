import { HttpInterceptorFn, HttpRequest, HttpHandlerFn } from '@angular/common/http';

const API_BASES = [
  'http://localhost:18080',                 // BE local
  'https://dd-demo.abc.com:8443',           // ví dụ BE trên server (tuỳ bạn)
];

const isApiUrl = (url: string) => API_BASES.some(b => url.startsWith(b));
const isR2Url  = (url: string) =>
  url.includes('.r2.cloudflarestorage.com') || url.includes('.r2.dev');

export const withCredentialsInterceptor: HttpInterceptorFn = (req: HttpRequest<any>, next: HttpHandlerFn) => {
  let r = req;

  // 1) Chỉ bật cookie/credentials cho API của bạn
  if (isApiUrl(req.url)) {
    r = r.clone({ withCredentials: true });
  }

  // 2) Với presigned R2: tắt credentials và gỡ các header có thể gây preflight/403
  if (isR2Url(req.url)) {
    // Không gửi cookie/credentials cho presigned URL
    let headers = r.headers;

    // Loại các header hay bị auto-thêm
    ['Authorization', 'X-Requested-With', 'Accept', 'X-XSRF-TOKEN'].forEach(h => {
      if (headers.has(h)) headers = headers.delete(h);
    });

    // Quan trọng: KHÔNG động vào Content-Type ở interceptor.
    // PUT presigned cần Content-Type đúng như lúc ký (bạn đã set ở chỗ upload).

    r = r.clone({
      withCredentials: false,
      headers
    });
  }

  // (Tuỳ chọn) debug
  // console.log('[HTTP]', r.method, r.url, 'withCred=', r.withCredentials, 'hdr=', r.headers.keys());

  return next(r);
};
