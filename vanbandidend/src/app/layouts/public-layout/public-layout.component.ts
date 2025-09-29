import { Component, computed, signal,ViewEncapsulation  } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterOutlet } from '@angular/router';

import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { MatInputModule } from '@angular/material/input';
import { MatBadgeModule } from '@angular/material/badge';
import { ReactiveFormsModule, FormControl } from '@angular/forms';

@Component({
  selector: 'app-public-layout',
  standalone: true,
  imports: [
    CommonModule, RouterOutlet, RouterLink,
    MatSidenavModule, MatToolbarModule, MatListModule,
    MatIconModule, MatButtonModule, MatMenuModule,
    MatDividerModule, MatInputModule, MatBadgeModule,
    ReactiveFormsModule
  ],
  templateUrl: './public-layout.component.html',
  styleUrls: ['./public-layout.component.css'],
    encapsulation: ViewEncapsulation.None,
})
export class PublicLayout {
  // Tìm kiếm nhanh toàn hệ thống (placeholder – tuỳ bạn bind service)
  quickSearch = new FormControl<string>('');

  // Menu trái – hiển thị đúng các chức năng bạn yêu cầu
  readonly mainMenus = [
    { label: 'Văn bản Đến', icon: 'download', link: '/documents?type=den' },
    { label: 'Văn bản Đi', icon: 'upload', link: '/documents?type=di' },
  ];
  readonly flowMenus = [
    { label: 'Trình duyệt', icon: 'rule_folder', link: '/workflows/submit' },
    { label: 'Phê duyệt', icon: 'task_alt', link: '/workflows/approve' },
    { label: 'Chuyển tiếp', icon: 'forward_to_inbox', link: '/workflows/forward' },
  ];
  readonly utilityMenus = [
    { label: 'Tra cứu/Lọc', icon: 'search', link: '/search' },
    { label: 'Thống kê – Báo cáo', icon: 'insights', link: '/reports' },
    { label: 'Danh mục – Cấu hình', icon: 'settings', link: '/settings' },
  ];

  // Giả lập trạng thái mở/đóng sidenav cho mobile
  sidenavOpened = signal(true);
  toggleSidenav() { this.sidenavOpened.set(!this.sidenavOpened()); }

  // Hiển thị badge số việc cần xử lý (demo)
  pendingTasks = signal(3);
  pendingLabel = computed(() => this.pendingTasks() > 0 ? `${this.pendingTasks()} việc` : 'Không có việc');
}
