// core/auth/auth.service.ts
import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { switchMap, tap } from 'rxjs/operators';

export type Role = 'EMPLOYEE' | 'LEADER' | 'ADMIN';

export interface SessionUser {
  id: string;
  username: string;
  role: Role; // mở rộng để có ADMIN
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  user = signal<SessionUser | null>(null);
  isLoggedIn = computed(() => this.user() !== null);

  private base = 'http://localhost:18080/api/auth';
  private api = 'http://localhost:18080/api';

  constructor(private http: HttpClient) {}

  me() {
    return this.http.get<SessionUser>(`${this.base}/me`, { withCredentials: true });
  }

  login(body: { email: string; password: string }) {
    return this.http
      .post<{ message: string }>(`${this.base}/login`, body, { withCredentials: true })
      .pipe(
        switchMap(() => this.me()),
        tap((u) => this.user.set(u))
      );
  }

  logout() {
    return this.http
      .post<void>(`${this.base}/logout`, {}, { withCredentials: true })
      .pipe(tap(() => this.user.set(null)));
  }
  createUser(body: { email: string; password: string }) {
    // chú ý: 1 dấu /, truyền body là tham số thứ 2, options là tham số thứ 3
    return this.http.post<SessionUser>(`${this.base}/register`, body, {
      withCredentials: true,
    });
  }
createDocument(payload: any | FormData) {
  return this.http.post(`${this.api}/documents`, payload, {
    withCredentials: true,
  });
}
uploadFile(body: { data:FormData }) {// api upload file
    // chú ý: 1 dấu /, truyền body là tham số thứ 2, options là tham số thứ 3
    return this.http.post<SessionUser>(`${this.api}/uploads`, body, {
      withCredentials: true,
    });
  }
  // check 1 role
  hasRole(r: Role): boolean {
    const u = this.user();
    return !!u && u.role === r;
  }

  // nếu cần check nhiều role
  hasAnyRole(...roles: Role[]): boolean {
    const u = this.user();
    return !!u && roles.includes(u.role);
  }
}
