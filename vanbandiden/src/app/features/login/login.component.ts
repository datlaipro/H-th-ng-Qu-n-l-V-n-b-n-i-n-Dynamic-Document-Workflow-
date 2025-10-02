import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

// Angular Material
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { HttpClient, HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Router } from '@angular/router';
// (tuỳ dự án) import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-login',
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
})
export class LoginComponent {
  email = '';
  password = '';
  showPassword = false;
  loading = false;

  error: string | null = null;
  retryAfterText: string | null = null; // ví dụ: "in 1 minute", "in 42 seconds"

  constructor(private http: HttpClient, private router: Router) // private snack: MatSnackBar
  {}

  togglePassword() {
    this.showPassword = !this.showPassword;
  }

  doLogin() {
    this.error = null;
    this.retryAfterText = null;
    this.loading = true;

    this.http
      .post(
        '/api/auth/login',
        { email: this.email?.trim(), password: this.password },
        { withCredentials: true, observe: 'response' }
      )
      .subscribe({
        next: (res: HttpResponse<any>) => {
          this.loading = false;
          // đăng nhập thành công
          this.router.navigateByUrl('/');
        },
        error: (err: HttpErrorResponse) => {
          this.loading = false;
          const mapped = this.mapBackendError(err);
          this.error = mapped.message;
          this.retryAfterText = mapped.retryAfterText ?? null; // 👈 fix

          // Option: snackbar thêm
          // if (mapped.snack) this.snack.open(mapped.snack, 'OK', { duration: 4000 });
        },
      });
  }

  /**
   * Map lỗi từ BE (GlobalErrorFilter) → thông điệp UI.
   * BE trả dạng: { code: string, message: string }
   * và có thể kèm header Retry-After khi 429.
   */
  private mapBackendError(err: HttpErrorResponse): {
    message: string;
    retryAfterText?: string;
    snack?: string;
  } {
    // Mất kết nối / CORS
    if (err.status === 0) {
      return { message: 'Không thể kết nối máy chủ. Vui lòng kiểm tra mạng hoặc thử lại sau.' };
    }

    // Thử đọc code & message từ body
    const body = (err.error || {}) as { code?: string; message?: string };
    const code = (body.code || '').toString().toLowerCase();
    const serverMsg = (body.message || '').toString();

    // 429: lấy Retry-After (giây)
    if (err.status === 429 || code === 'too_many_attempts') {
      const raHeader = err.headers?.get('Retry-After');
      let retryAfterText: string | undefined;
      if (raHeader) {
        const secs = parseInt(raHeader, 10);
        if (!Number.isNaN(secs)) {
          retryAfterText = this.humanizeSeconds(secs);
        }
      }
      return {
        message: 'Bạn đã thử quá nhiều lần. Vui lòng thử lại sau.',
        retryAfterText,
      };
    }

    // Map theo code
    switch (code) {
      case 'invalid_credentials':
        return { message: 'Sai email hoặc mật khẩu.' };
      case 'user_inactive':
        return { message: 'Tài khoản chưa được kích hoạt hoặc đã bị vô hiệu.' };
      case 'user_locked':
        return { message: 'Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.' };
      case 'password_expired':
        return { message: 'Mật khẩu đã hết hạn. Vui lòng đặt lại mật khẩu.' };
      case 'validation_error':
        // ưu tiên message cụ thể từ server nếu có
        return { message: serverMsg || 'Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.' };
      case 'forbidden':
        return { message: serverMsg || 'Bạn không có quyền thực hiện thao tác này.' };
      case 'not_found':
        return { message: 'Tài nguyên không tồn tại.' };
      case 'conflict':
        return { message: 'Xung đột dữ liệu. Vui lòng thử lại.' };
      case 'database_error':
        return { message: 'Hệ thống đang bận. Vui lòng thử lại sau.' };
      case 'server_error':
        return { message: 'Có lỗi không mong muốn. Vui lòng thử lại sau.' };
    }

    // Fallback: dựa theo HTTP status nếu BE chưa gắn code
    switch (err.status) {
      case 400:
        return { message: serverMsg || 'Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.' };
      case 401:
        return { message: 'Bạn cần đăng nhập để tiếp tục.' };
      case 403:
        return { message: 'Bạn không có quyền thực hiện thao tác này.' };
      case 404:
        return { message: 'Không tìm thấy đường dẫn hoặc tài nguyên.' };
      case 423:
        return { message: 'Tài khoản đã bị khóa. Vui lòng liên hệ quản trị viên.' };
      case 500:
        return { message: 'Hệ thống đang gặp sự cố. Vui lòng thử lại sau.' };
      default:
        return {
          message: serverMsg || `Đã xảy ra lỗi (HTTP ${err.status}). Vui lòng thử lại sau.`,
        };
    }
  }

  private humanizeSeconds(secs: number): string {
    if (secs < 60) return `in ${secs} second${secs === 1 ? '' : 's'}`;
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    if (s === 0) return `in ${m} minute${m === 1 ? '' : 's'}`;
    return `in ${m} minute${m === 1 ? '' : 's'} ${s} second${s === 1 ? '' : 's'}`;
  }
}
