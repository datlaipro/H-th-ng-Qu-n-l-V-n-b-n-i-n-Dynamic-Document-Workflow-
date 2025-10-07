// core/auth/auth.service.ts
import { Injectable, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Input } from '@angular/core';
import { switchMap, tap } from 'rxjs/operators';
type DocType = 'Đi' | 'Đến';
type DocStatus = 'Chờ xử lý' | 'Đang xử lý' | 'Hoàn thành';
export type Role = 'EMPLOYEE' | 'LEADER' | 'ADMIN';
export interface DocumentRow {
  id: number;
  doc_type: DocType; // Đi/Đến
  doc_number: string; // Số hiệu
  title: string;
  content: string;
  status: DocStatus;
  originator_id: number;
  current_handler_id?: number | null;
  issued_at?: string | null; // OUTBOUND
  received_at?: string | null; // INBOUND
  sender_unit?: string | null; // INBOUND
  recipient_unit?: string | null; // OUTBOUND
  created_at: string;
  updated_at: string;
}

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
  uploadFile(body: { data: FormData }) {
    // api upload file
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
  public load(id: number) {// xem chi tiết văn bản
    this.http.get<DocumentRow>(`${this.api}/documents/${id}`, { withCredentials: true }).subscribe({
      next: (d) => {
        return d;
      },
      error: (error) => console.log('error', error),
    });
  }
}
