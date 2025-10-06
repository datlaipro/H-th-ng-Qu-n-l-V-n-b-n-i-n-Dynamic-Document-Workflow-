import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams, HttpEvent, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PresignResponse {
  url: string;
  key: string;
  publicUrl: string;
  headers?: Record<string, string>;
}

@Injectable({ providedIn: 'root' })
export class UploadService {
  private api = 'http://localhost:18080/api';

  constructor(private http: HttpClient) {}

  getPresign(
    name: string,
    type: string,
    size: number,
    prefix = 'documents'
  ): Observable<PresignResponse> {
    const params = new HttpParams()
      .set('name', name)
      .set('type', type || 'application/octet-stream')
      .set('size', String(size))
      .set('prefix', prefix);

    return this.http.get<PresignResponse>(`${this.api}/uploads`, {
      params,
      withCredentials: true,
    });
  }

  // ✅ Sửa chỗ này
  uploadToR2(
    presignedUrl: string,
    file: File,
    contentType: string,
    extraHeaders?: Record<string, string>
  ): Observable<HttpEvent<unknown>> {
    let headers = new HttpHeaders().set('Content-Type', contentType || 'application/octet-stream');
    if (extraHeaders) {
      for (const [k, v] of Object.entries(extraHeaders)) {
        if (v != null && v !== '' && !/^host$|^content-length$|^authorization$/i.test(k)) {
          headers = headers.set(k, v);
        }
      }
    }

    const req = new HttpRequest('PUT', presignedUrl, file, {
      reportProgress: true,
      withCredentials: false, // bắt buộc là false
      headers: new HttpHeaders().set('Content-Type', file.type || 'application/octet-stream'),
    });
    return this.http.request(req);
  }
}
