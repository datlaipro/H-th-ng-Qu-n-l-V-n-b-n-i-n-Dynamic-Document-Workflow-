import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  standalone: true,
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css'],
  imports: [FormsModule, InputTextModule, ButtonModule],
})
export class LoginComponent {
  email = '';
  password = '';
  loading = false;
  error = '';

  constructor(private auth: AuthService, private router: Router) {}

  doLogin() {
    if (!this.email || !this.password) {
      this.error = 'Please enter email and password.';
      return;
    }
    this.error = '';
    this.loading = true;

    this.auth.login({ username: this.email, password: this.password }).subscribe({
      next: (u) => {
        this.auth.user.set(u);
        this.router.navigateByUrl('/');
      },
      error: () => {
        this.error = 'Invalid credentials.';
        this.loading = false;
      },
    });
  }
}
