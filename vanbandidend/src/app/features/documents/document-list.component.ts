import { Component, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Router, ActivatedRoute } from '@angular/router';

type DocType = 'Đi' | 'Đến';
type DocStatus = 'Chờ xử lý' | 'Đang xử lý' | 'Hoàn thành';

export interface DocumentItem {
  id: number;
  loai: DocType;
  soHieu: string;
  ngayBanHanh?: string;
  ngayNhan?: string;
  nguoiKhoiTao: string;
  trangThai: DocStatus;
}

// Demo data — thay bằng dữ liệu API thật
const MOCK_DATA: DocumentItem[] = [
  {
    id: 1,
    loai: 'Đi',
    soHieu: 'VB-001/2025',
    ngayBanHanh: '2025-09-10',
    nguoiKhoiTao: 'Nguyễn An',
    trangThai: 'Chờ xử lý',
  },
  {
    id: 2,
    loai: 'Đến',
    soHieu: 'CV-778/UBND',
    ngayNhan: '2025-09-12',
    nguoiKhoiTao: 'Trần Bình',
    trangThai: 'Đang xử lý',
  },
  {
    id: 3,
    loai: 'Đi',
    soHieu: 'VB-013/2025',
    ngayBanHanh: '2025-09-18',
    nguoiKhoiTao: 'Lê Chi',
    trangThai: 'Hoàn thành',
  },
  {
    id: 4,
    loai: 'Đến',
    soHieu: 'CV-805/SYT',
    ngayNhan: '2025-09-20',
    nguoiKhoiTao: 'Phạm Dũng',
    trangThai: 'Chờ xử lý',
  },
  {
    id: 5,
    loai: 'Đi',
    soHieu: 'VB-020/2025',
    ngayBanHanh: '2025-09-22',
    nguoiKhoiTao: 'Nguyễn An',
    trangThai: 'Đang xử lý',
  },
  {
    id: 6,
    loai: 'Đến',
    soHieu: 'CV-112/SGD',
    ngayNhan: '2025-09-23',
    nguoiKhoiTao: 'Vũ Hạnh',
    trangThai: 'Hoàn thành',
  },
];

@Component({
  selector: 'app-document-list',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatIconModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatChipsModule,
    MatTooltipModule,
  ],
  templateUrl: './document-list.component.html',
  styleUrls: ['./document-list.component.css'],
})
export class DocumentListComponent {
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  viewMode = new FormControl<'list' | 'grid'>('list');

  displayedColumns = ['loai', 'soHieu', 'ngay', 'nguoiKhoiTao', 'trangThai'];
  dataSource = new MatTableDataSource<DocumentItem>(MOCK_DATA);

  @ViewChild(MatPaginator, { static: true }) paginator!: MatPaginator;
  @ViewChild(MatSort, { static: true }) sort!: MatSort;

  ngAfterViewInit(): void {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  getNgayDisplay(row: DocumentItem): string {
    const s = row.loai === 'Đi' ? row.ngayBanHanh : row.ngayNhan;
    return s ? new Date(s).toLocaleDateString() : '—';
  }

  statusClass(st: DocStatus): string {
    return st === 'Chờ xử lý' ? 'st-wait' : st === 'Đang xử lý' ? 'st-doing' : 'st-done';
  }
  open(id: number) {
    console.log(id);
    this.router.navigate(['/DocumentDetail']);
  }
  trackById = (_: number, r: any) => r.id ?? r.soHieu;
}
