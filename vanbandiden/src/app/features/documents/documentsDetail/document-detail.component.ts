import { Component, Input, OnInit, inject, computed, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressBarModule } from '@angular/material/progress-bar';

export type DocType = 'OUTBOUND' | 'INBOUND';
export type DocStatus = 'PENDING' | 'IN_PROGRESS' | 'APPROVED' | 'REJECTED' | 'COMPLETED';

export interface DocAttachment {
  id: number;
  document_id: number;
  file_name: string;
  file_url: string;         // lưu trực tiếp R2/S3 URL
  mime_type?: string;
  size_bytes?: number;
  uploaded_by?: number;
  uploaded_at?: string;
}

export interface DocumentRow {
  id: number;
  doc_type: DocType;                // Đi/Đến
  doc_number: string;               // Số hiệu
  title: string;
  content: string;
  status: DocStatus;
  originator_id: number;
  current_handler_id?: number | null;
  issued_at?: string | null;        // OUTBOUND
  received_at?: string | null;      // INBOUND
  sender_unit?: string | null;      // INBOUND
  recipient_unit?: string | null;   // OUTBOUND
  created_at: string;
  updated_at: string;
}

export interface TimelineRow {
  history_id: number;
  created_at: string;
  actor: string;
  action: 'CREATE'|'SUBMIT'|'APPROVE'|'REJECT'|'FORWARD'|'COMMENT'|'UPDATE_STATUS';
  forwarded_to?: string | null;
  note?: string | null;
}

@Component({
  selector: 'app-document-detail',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    MatButtonModule, MatIconModule, MatDividerModule,
    MatChipsModule, MatExpansionModule, MatTooltipModule,
    MatProgressBarModule,
  ],
  templateUrl: './document-detail.component.html',
  styleUrls: ['./document-detail.component.css'],
})
export class DocumentDetailComponent implements OnInit {
  private http = inject(HttpClient);

  /** API base tuỳ dự án */
  @Input() apiBase = '/api';

  /** Nhận id từ cha (router/parent) hoặc truyền sẵn object */
  @Input() id?: number;

  /** Cho phép truyền sẵn dữ liệu (nếu đã có ở parent để tránh gọi API lần nữa) */
  @Input() initialData?: DocumentRow;

  loading = signal(true);
  doc = signal<DocumentRow | null>(null);
  atts = signal<DocAttachment[]>([]);
  timeline = signal<TimelineRow[]>([]);

  // ====== Derived UI ======
  isOutbound = computed(() => this.doc()?.doc_type === 'OUTBOUND');
  unitLabel = computed(() => this.isOutbound() ? 'Nơi nhận' : 'Nơi gửi');
  unitValue = computed(() => this.isOutbound() ? this.doc()?.recipient_unit : this.doc()?.sender_unit);

  dateLabel = computed(() => this.isOutbound() ? 'Ngày ban hành' : 'Ngày nhận');
  dateValue = computed(() => this.isOutbound() ? this.doc()?.issued_at : this.doc()?.received_at);

  // badge màu theo trạng thái
  statusClass(status?: DocStatus) {
    switch (status) {
      case 'APPROVED': return 'badge badge--ok';
      case 'REJECTED': return 'badge badge--warn';
      case 'IN_PROGRESS': return 'badge badge--info';
      case 'COMPLETED': return 'badge badge--done';
      default: return 'badge';
    }
  }

  ngOnInit(): void {
    if (this.initialData) {
      this.doc.set(this.initialData);
      this.fetchChildren(this.initialData.id);
      this.loading.set(false);
      return;
    }
    if (!this.id) {
      this.loading.set(false);
      return;
    }
    this.load(this.id);
  }

  private load(id: number) {
    this.loading.set(true);
    // BE gợi ý:
    // GET /api/documents/:id
    // GET /api/documents/:id/attachments
    // GET /api/documents/:id/timeline
    this.http.get<DocumentRow>(`${this.apiBase}/documents/${id}`, { withCredentials: true }).subscribe({
      next: (d) => {
        this.doc.set(d);
        this.fetchChildren(d.id);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  private fetchChildren(documentId: number) {
    this.http.get<DocAttachment[]>(`${this.apiBase}/documents/${documentId}/attachments`, { withCredentials: true })
      .subscribe({ next: (rows) => this.atts.set(rows || []) });

    this.http.get<TimelineRow[]>(`${this.apiBase}/documents/${documentId}/timeline`, { withCredentials: true })
      .subscribe({ next: (rows) => this.timeline.set(rows || []) });
  }

  // ====== Actions (chỉ minh hoạ; gọi API thật theo BE của bạn) ======
  approve() {
    const id = this.doc()?.id; if (!id) return;
    this.http.post(`${this.apiBase}/documents/${id}/approve`, {}, { withCredentials: true })
      .subscribe(() => this.load(id));
  }
  reject() {
    const id = this.doc()?.id; if (!id) return;
    this.http.post(`${this.apiBase}/documents/${id}/reject`, {}, { withCredentials: true })
      .subscribe(() => this.load(id));
  }
  forward() {
    const id = this.doc()?.id; if (!id) return;
    // ví dụ chuyển cho userId=1; thực tế mở dialog chọn người
    this.http.post(`${this.apiBase}/documents/${id}/forward`, { toUserId: 1 }, { withCredentials: true })
      .subscribe(() => this.load(id));
  }
  edit() {
    // điều hướng router tới trang chỉnh sửa
    console.log('Edit doc', this.doc()?.id);
  }
  remove() {
    const id = this.doc()?.id; if (!id) return;
    this.http.delete(`${this.apiBase}/documents/${id}`, { withCredentials: true })
      .subscribe(() => console.log('Removed', id));
  }

  // ====== Helpers ======
  openFile(att: DocAttachment) {
    if (!att.file_url) return;
    window.open(att.file_url, '_blank');
  }

  prettySize(bytes?: number) {
    if (bytes === undefined || bytes === null) return '';
    const units = ['B','KB','MB','GB','TB'];
    let i = 0, n = bytes;
    while (n >= 1024 && i < units.length - 1) { n /= 1024; i++; }
    return `${n.toFixed(1)} ${units[i]}`;
  }

  prettyStatus(s?: DocStatus) {
    switch (s) {
      case 'PENDING': return 'Chờ xử lý';
      case 'IN_PROGRESS': return 'Đang xử lý';
      case 'APPROVED': return 'Đã phê duyệt';
      case 'REJECTED': return 'Từ chối';
      case 'COMPLETED': return 'Hoàn tất';
      default: return s || '';
    }
  }

  prettyAction(a: TimelineRow['action']) {
    switch (a) {
      case 'CREATE': return 'Tạo';
      case 'SUBMIT': return 'Trình';
      case 'APPROVE': return 'Phê duyệt';
      case 'REJECT': return 'Từ chối';
      case 'FORWARD': return 'Chuyển tiếp';
      case 'COMMENT': return 'Ghi chú';
      case 'UPDATE_STATUS': return 'Cập nhật trạng thái';
    }
  }
}
