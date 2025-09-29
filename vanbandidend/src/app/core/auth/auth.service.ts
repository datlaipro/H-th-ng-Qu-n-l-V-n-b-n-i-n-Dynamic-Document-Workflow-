// core/auth/auth.service.ts
import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export interface SessionUser { id: string; username: string; role: 'EMPLOYEE'|'LEADER'; }

@Injectable({ providedIn: 'root' })
export class AuthService {
  user = signal<SessionUser | null>(null);
  private base = '/api/auth';

  constructor(private http: HttpClient) {}

  me()     { return this.http.get<SessionUser>(`${this.base}/me`); }
  login(body: {username:string; password:string}) {
    return this.http.post<SessionUser>(`${this.base}/login`, body);
  }
  logout() { return this.http.post<void>(`${this.base}/logout`, {}); }

  hasRole(r: 'EMPLOYEE'|'LEADER') { return this.user()?.role === r; }
}
