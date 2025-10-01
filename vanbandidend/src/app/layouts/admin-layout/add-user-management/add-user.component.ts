import { Component, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../core/auth/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-add-user',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
  ],
  templateUrl: './add-user.component.html',
  styleUrls: ['./add-user.component.css'],
})
export class AddUserComponent {
  constructor(private auth: AuthService, private router: Router) {}

  // inject FB trước rồi mới dùng
  private fb = inject(FormBuilder);

  hide = signal(true);

  form = this.fb.group({
    username: [
      '',
      [
        Validators.required,
        Validators.minLength(3),
        Validators.maxLength(24),
        Validators.pattern(/^[A-Za-z0-9._-]+$/),
      ],
    ],
    email: ['', [Validators.required, Validators.email]],
    password: [
      '',
      [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern(/^(?=.*[A-Za-z])(?=.*\d).+$/),
      ],
    ],
  });

  get f() {
    return this.form.controls;
  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    // Thêm username vào payload
    const { username, email, password } = this.form.getRawValue();
    const payload = { fullName: username, email, password };

    // Gọi API tạo user
   this.auth.createUser(payload as any).subscribe({
  next: () => {
    console.log('tạo user thành công');
    this.router.navigate(['/admin/list-user'], { replaceUrl: true });
  },
  error: (err) => console.error('tạo user thất bại', err),
});

  }

  reset() {
    this.form.reset();
    this.hide.set(true);
  }
}
