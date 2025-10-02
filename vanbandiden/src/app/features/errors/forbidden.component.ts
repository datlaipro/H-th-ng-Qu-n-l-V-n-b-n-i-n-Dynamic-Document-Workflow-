import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  standalone: true,
  selector: 'app-forbidden',
  templateUrl: './forbidden.component.html',
  styleUrl: './forbidden.component.css',
  imports: [CommonModule, RouterLink, MatCardModule, MatButtonModule, MatIconModule],
})
export class ForbiddenComponent {
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  // Nếu guard gửi query redirect thì hiển thị nút “Thử lại”
  redirectUrl = this.route.snapshot.queryParamMap.get('redirect') ?? '';

  goHome() {
    this.router.navigate(['/document-list'], { replaceUrl: true });
  }

  goBack() {
    // Nếu quay lại mà đụng guard lần nữa thì vẫn bị chặn — nhưng thường có ích
    this.router.navigateByUrl(this.router.url, { replaceUrl: false });
    history.back();
  }

  tryAgain() {
    if (this.redirectUrl) {
      this.router.navigateByUrl(this.redirectUrl, { replaceUrl: true });
    } else {
      this.goHome();
    }
  }
}
