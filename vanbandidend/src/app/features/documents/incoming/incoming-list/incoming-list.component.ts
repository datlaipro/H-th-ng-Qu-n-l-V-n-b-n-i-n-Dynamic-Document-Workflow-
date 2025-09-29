import { Component, ViewChild, computed, effect, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';

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

const MOCK_DATA: DocumentItem[] = [
  {
    id: 1,
    loai: 'Đến',
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
    loai: 'Đến',
    soHieu: 'VB-013/2025',
    ngayBanHanh: '2025-09-18',
    nguoiKhoiTao: 'Lê Chi',
    trangThai: 'Hoàn thành',
  },
];

@Component({
  selector: 'app-incoming-detail',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatIconModule,
    MatButtonModule,
    MatChipsModule,
    MatButtonToggleModule,
    MatDatepickerModule,
    MatNativeDateModule,
  ],
  templateUrl: './incoming-list.component.html',
  styleUrls: ['./incoming-list.component.css'],
})
export class IncomingList{
  viewMode = new FormControl<'list' | 'grid'>('list');

  search = new FormControl<string>('');
  loai = new FormControl<DocType | ''>('');
  trangThai = new FormControl<DocStatus | ''>('');
  nguoiKhoiTao = new FormControl<string>('');
  dateFrom = new FormControl<Date | null>(null);
  dateTo = new FormControl<Date | null>(null);

  displayedColumns = ['loai', 'soHieu', 'ngay', 'nguoiKhoiTao', 'trangThai', 'actions'];
  dataSource = new MatTableDataSource<DocumentItem>(MOCK_DATA);

  @ViewChild(MatPaginator, { static: true }) paginator!: MatPaginator;
  @ViewChild(MatSort, { static: true }) sort!: MatSort;

  private filterState = computed(() => ({
    search: (this.search.value || '').trim().toLowerCase(),
    loai: this.loai.value || '',
    trangThai: this.trangThai.value || '',
    nguoiKhoiTao: (this.nguoiKhoiTao.value || '').trim().toLowerCase(),
    from: this.dateFrom.value ? startOfDay(this.dateFrom.value) : null,
    to: this.dateTo.value ? endOfDay(this.dateTo.value) : null,
  }));

  constructor() {
    this.dataSource.filterPredicate = (row, filterStr) => {
      const f = JSON.parse(filterStr) as ReturnType<typeof this.filterState>;
      const haystack = [
        row.loai,
        row.soHieu,
        row.ngayBanHanh ?? '',
        row.ngayNhan ?? '',
        row.nguoiKhoiTao,
        row.trangThai,
      ]
        .join(' ')
        .toLowerCase();

      const passSearch = !f.search || haystack.includes(f.search);
      const passLoai = !f.loai || row.loai === f.loai;
      const passTrangThai = !f.trangThai || row.trangThai === f.trangThai;
      const passNguoi = !f.nguoiKhoiTao || row.nguoiKhoiTao.toLowerCase().includes(f.nguoiKhoiTao);

      const dateStr = row.loai === 'Đi' ? row.ngayBanHanh : row.ngayNhan;
      let passDate = true;
      if (f.from || f.to) {
        if (!dateStr) passDate = false;
        else {
          const d = new Date(dateStr + 'T00:00:00');
          if (f.from && d < f.from) passDate = false;
          if (f.to && d > f.to) passDate = false;
        }
      }
      return passSearch && passLoai && passTrangThai && passNguoi && passDate;
    };

    effect(() => {
      this.dataSource.filter = JSON.stringify(this.filterState());
      if (this.dataSource.paginator) this.dataSource.paginator.firstPage();
    });
  }

  ngAfterViewInit() {
    this.dataSource.paginator = this.paginator;
    this.dataSource.sort = this.sort;
  }

  clearFilters() {
    this.search.setValue('');
    this.loai.setValue('');
    this.trangThai.setValue('');
    this.nguoiKhoiTao.setValue('');
    this.dateFrom.setValue(null);
    this.dateTo.setValue(null);
  }

  getNgayDisplay(row: DocumentItem): string {
    const s = row.loai === 'Đi' ? row.ngayBanHanh : row.ngayNhan;
    return s ? new Date(s).toLocaleDateString() : '—';
  }

  statusClass(st: DocStatus) {
    return st === 'Chờ xử lý' ? 'st-wait' : st === 'Đang xử lý' ? 'st-doing' : 'st-done';
  }

  openDetail(row: DocumentItem) {
    console.log('open detail', row);
  }
}

function startOfDay(d: Date) {
  const x = new Date(d);
  x.setHours(0, 0, 0, 0);
  return x;
}
function endOfDay(d: Date) {
  const x = new Date(d);
  x.setHours(23, 59, 59, 999);
  return x;
}
