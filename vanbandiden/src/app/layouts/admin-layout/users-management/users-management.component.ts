import { Component, computed, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { RouterLink } from '@angular/router';
import { ViewChild } from '@angular/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { SelectionModel } from '@angular/cdk/collections';

export type UserStatus = 'Active' | 'In Active' | 'On Hold' | 'To Be Verified';

export interface UserRow {
  id: string;
  name: string;
  email: string;
  role: string;
  status: UserStatus;
  lastModifiedAgo: string; // e.g. '5 mins ago'
  lastLoginAgo: string; // e.g. '5 mins ago'
  avatar?: string; // optional, can be initial-generated in UI
}

const MOCK_DATA: UserRow[] = [
  {
    id: '1',
    name: 'Floyd Miles',
    email: 'floydmiles@example.com',
    role: 'UI/UX Designer',
    status: 'Active',
    lastModifiedAgo: '5 mins ago',
    lastLoginAgo: '5 mins ago',
  },
  {
    id: '2',
    name: 'Jane Cooper',
    email: 'janecooper@example.com',
    role: 'Front Developer',
    status: 'In Active',
    lastModifiedAgo: '15 mins ago',
    lastLoginAgo: '15 mins ago',
  },
  {
    id: '3',
    name: 'Dianne Russell',
    email: 'diannerussell@example.com',
    role: 'Backend Developer',
    status: 'To Be Verified',
    lastModifiedAgo: '1 day ago',
    lastLoginAgo: '1 day ago',
  },
  {
    id: '4',
    name: 'Annette Black',
    email: 'annetteblack@example.com',
    role: 'Business Analyst',
    status: 'To Be Verified',
    lastModifiedAgo: '1 month ago',
    lastLoginAgo: '1 month ago',
  },
  {
    id: '5',
    name: 'Eleanor Pena',
    email: 'eleanorpena@example.com',
    role: 'Product Manager',
    status: 'On Hold',
    lastModifiedAgo: '1 week ago',
    lastLoginAgo: '1 week ago',
  },
  {
    id: '6',
    name: 'Jerome Bell',
    email: 'jeromebell@example.com',
    role: 'Team Lead',
    status: 'To Be Verified',
    lastModifiedAgo: '22 Dec, 2024',
    lastLoginAgo: '22 Dec, 2024',
  },
  {
    id: '7',
    name: 'Brooklyn Simmons',
    email: 'brooklyns@example.com',
    role: 'Team Lead',
    status: 'To Be Verified',
    lastModifiedAgo: '22 Dec, 2024',
    lastLoginAgo: '22 Dec, 2024',
  },
  {
    id: '8',
    name: 'Esther Howard',
    email: 'estherh@example.com',
    role: 'Team Lead',
    status: 'To Be Verified',
    lastModifiedAgo: '22 Dec, 2024',
    lastLoginAgo: '22 Dec, 2024',
  },
  {
    id: '9',
    name: 'Arlene McCoy',
    email: 'arlenemccoy@example.com',
    role: 'Team Lead',
    status: 'To Be Verified',
    lastModifiedAgo: '22 Dec, 2024',
    lastLoginAgo: '22 Dec, 2024',
  },
];

@Component({
  selector: 'app-users-management',
  standalone: true,
  imports: [
    RouterLink,
    CommonModule,
    MatTableModule,
    MatSortModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatButtonModule,
    MatCheckboxModule,
    MatChipsModule,
    MatTooltipModule,
    MatMenuModule,
  ],
  templateUrl: './users-management.component.html',
  styleUrls: ['./users-management.component.css'],
})
export class UsersManagementComponent {
  displayedColumns: string[] = [
    'select',
    'user',
    'role',
    'status',
    'lastModified',
    'lastLogin',
    'actions',
  ];
  dataSource = new MatTableDataSource<UserRow>(MOCK_DATA);
  selection = new SelectionModel<UserRow>(true /* multiple */);

  search = signal('');
  activeTab = signal<
    'All Users' | 'Teams' | 'Roles & Permissions' | 'Invitations' | 'Activity Logs'
  >('All Users');

  @ViewChild(MatSort) sort!: MatSort;
  @ViewChild(MatPaginator) paginator!: MatPaginator;

  constructor() {
    this.dataSource.filterPredicate = (data, filter) => {
      const q = filter.trim().toLowerCase();
      return (
        data.name.toLowerCase().includes(q) ||
        data.email.toLowerCase().includes(q) ||
        data.role.toLowerCase().includes(q) ||
        data.status.toLowerCase().includes(q)
      );
    };
    effect(() => {
      this.dataSource.filter = this.search().trim().toLowerCase();
    });
  }

  ngAfterViewInit() {
    this.dataSource.sort = this.sort;
    this.dataSource.paginator = this.paginator;
  }

  /** UI helpers */
  isAllSelected() {
    const numSelected = this.selection.selected.length;
    const numRows = this.dataSource.filteredData.length;
    return numSelected === numRows && numRows > 0;
  }
  masterToggle() {
    if (this.isAllSelected()) {
      this.selection.clear();
    } else {
      this.selection.select(...this.dataSource.filteredData);
    }
  }

  statusChipColor(s: UserStatus): 'success' | 'warning' | 'hold' | 'danger' {
    switch (s) {
      case 'Active':
        return 'success';
      case 'On Hold':
        return 'hold';
      case 'In Active':
        return 'danger';
      default:
        return 'warning'; // To Be Verified
    }
  }

  /** Actions (mock) */

  exportCsv() {
    alert('Export clicked');
  }
  edit(row: UserRow) {
    console.log('EDIT', row);
  }
  remove(row: UserRow) {
    if (confirm(`Delete ${row.name}?`)) alert('Deleted (mock)');
  }

  /** Filter menu examples */
  setTab(tab: typeof this.activeTab extends any ? never : never) {}
  showOnly(status: UserStatus | 'all') {
    if (status === 'all') {
      this.dataSource.data = MOCK_DATA;
      return;
    }
    this.dataSource.data = MOCK_DATA.filter((r) => r.status === status);
  }
}
