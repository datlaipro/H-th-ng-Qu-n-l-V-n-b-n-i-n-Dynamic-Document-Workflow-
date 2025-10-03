import { Component, computed, signal, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  RouterLink,
  RouterOutlet,
  Router,
  RouterLinkActive,
  IsActiveMatchOptions,
} from '@angular/router';
import { AuthService, Role } from '../../core/auth/auth.service';

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
type MenuItem = { label: string; icon: string; link: string; roles?: Role[] };
@Component({
  selector: 'app-public-layout',
  standalone: true,
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatSidenavModule,
    MatToolbarModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatMenuModule,
    MatDividerModule,
    MatInputModule,
    MatBadgeModule,
    ReactiveFormsModule,
  ],
  templateUrl: './public-layout.component.html',
  styleUrls: ['./public-layout.component.css'],
  encapsulation: ViewEncapsulation.None,
})
export class PublicLayout {
  // Tìm kiếm nhanh toàn hệ thống (placeholder – tuỳ bạn bind service)
  quickSearch = new FormControl<string>('');
  constructor(private auth: AuthService, private router: Router) {}

  // Menu trái – hiển thị đúng các chức năng bạn yêu cầu
  readonly mainMenus = [
    { label: 'Văn bản Đến', icon: 'download', link: '/documents?type=den' },
    { label: 'Văn bản Đi', icon: 'upload', link: '/documents?type=di' },
  ];
  // Khai báo menu + quyền được phép thấy
  readonly flowMenus: MenuItem[] = [
    {
      label: 'Trình duyệt',
      icon: 'rule_folder',
      link: '/workflows/submit',
      roles: ['EMPLOYEE', 'LEADER', 'ADMIN'],
    },
    {
      label: 'Phê duyệt',
      icon: 'task_alt',
      link: '/workflows/approve',
      roles: ['LEADER', 'ADMIN'],
    },
    {
      label: 'Chuyển tiếp',
      icon: 'forward_to_inbox',
      link: '/workflows/forward',
      roles: ['LEADER', 'ADMIN','EMPLOYEE'],
    },
    {
      label: 'Thêm nhân viên',
      icon: 'admin_panel_settings',
      link: '/admin/create-user',
      roles: ['ADMIN'],
    },
    { label: 'Danh sách nhân viên', icon: 'groups', link: '/admin/list-user', roles: ['ADMIN'] },
    {
      label: 'Danh sách văn bản',
      icon: 'article',
      link: '/document-list',
      roles: ['EMPLOYEE', 'LEADER', 'ADMIN'],
    },
  ];

  // Lấy user hiện tại từ AuthService (signal)
  readonly currentUser = computed(() => this.auth.user());

  // Lọc menu theo role động; nếu chưa đăng nhập -> ẩn hết (hoặc tùy bạn)
  readonly filteredMenus = computed(() => {// dùng computed Theo dõi phụ thuộc tự động và Ghi nhớ (memoize):
    const role = this.currentUser()?.role as Role | undefined;
    if (!role) return [] as MenuItem[];//hêm as MenuItem[] để nói rõ: đây là mảng các MenuItem.
    return this.flowMenus.filter((m) => !m.roles || m.roles.includes(role));
  });
  go(link: string) {
    this.router.navigateByUrl(link);
  }

  isActive(link: string) {
    return this.router.isActive(link, this.exact);
  }
  trackByLink = (_: number, m: MenuItem) => m.link;
  readonly utilityMenus = [
    { label: 'Tra cứu/Lọc', icon: 'search', link: '/search' },
    { label: 'Thống kê – Báo cáo', icon: 'insights', link: '/reports' },
    { label: 'Danh mục – Cấu hình', icon: 'settings', link: '/settings' },
  ];

  // Giả lập trạng thái mở/đóng sidenav cho mobile
  sidenavOpened = signal(true);
  toggleSidenav() {
    this.sidenavOpened.set(!this.sidenavOpened());
  }

  // Hiển thị badge số việc cần xử lý (demo)
  pendingTasks = signal(3);
  pendingLabel = computed(() =>
    this.pendingTasks() > 0 ? `${this.pendingTasks()} việc` : 'Không có việc'
  );

  logout() {
    this.auth.logout().subscribe({
      next: () => {
        this.auth.user.set(null);
        this.router.navigateByUrl('/login');
      },
      error: () => {},
    });
  }
  private readonly exact: IsActiveMatchOptions = {
    paths: 'exact',
    queryParams: 'ignored',
    fragment: 'ignored',
    matrixParams: 'ignored',
  };
}
