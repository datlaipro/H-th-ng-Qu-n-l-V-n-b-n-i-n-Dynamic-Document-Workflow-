import { Component, inject, signal, computed, effect } from '@angular/core';
import { CommonModule ,Location} from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router'; 
import { toSignal } from '@angular/core/rxjs-interop';
import { startWith } from 'rxjs/operators';
// Angular Material
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressBarModule } from '@angular/material/progress-bar';

type DocType = 'OUTBOUND' | 'INBOUND';

@Component({
  selector: 'app-document-create',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatTooltipModule,
    MatProgressBarModule,
  ],
  templateUrl: './document-create.component.html',
  styleUrls: ['./document-create.component.css'],
})
export class DocumentCreateComponent {
  
   private fb = inject(FormBuilder);
  private http = inject(HttpClient);
  private router = inject(Router);      // <—
  private location = inject(Location); 

  // đổi URL API phù hợp backend của bạn
  private readonly baseUrl = 'http://localhost:18080/api/documents';

  // lưu file tạm thời trước khi submit
  files = signal<File[]>([]);
  uploading = signal(false);
  uploadProgress = signal<number | null>(null);

  form = this.fb.group({
    doc_type: ['OUTBOUND' as DocType, Validators.required], // Đi (OUTBOUND) / Đến (INBOUND)
    doc_number: ['', [Validators.required, Validators.maxLength(100)]],
    title: ['', [Validators.required, Validators.maxLength(500)]],
    content: ['', [Validators.required]],
    // ngày — chỉ required theo loại văn bản
    issued_at: [null as Date | null], // dùng cho Đi (OUTBOUND)
    received_at: [null as Date | null], // dùng cho Đến (INBOUND)
    // đơn vị — chỉ required theo loại văn bản
    sender_unit: [''], // Nơi gửi   (INBOUND)
    recipient_unit: [''], // Nơi nhận  (OUTBOUND)
  });

docType = toSignal(
  this.form.get('doc_type')!.valueChanges.pipe(
    startWith(this.form.get('doc_type')!.value as DocType)
  ),
  { initialValue: this.form.get('doc_type')!.value as DocType }
);
  constructor() {
    effect(() => {
      const t = this.docType();
      const issued = this.form.get('issued_at')!;
      const received = this.form.get('received_at')!;
      const sender = this.form.get('sender_unit')!;
      const recipient = this.form.get('recipient_unit')!;

      // reset validators trước
      issued.clearValidators();
      received.clearValidators();
      sender.clearValidators();
      recipient.clearValidators();

      if (t === 'OUTBOUND') {
        // Văn bản Đi: cần issued_at + recipient_unit
        issued.setValidators([Validators.required]);
        recipient.setValidators([Validators.required, Validators.maxLength(255)]);

        // DỌN các trường không dùng của INBOUND
        if (received.value) received.setValue(null, { emitEvent: false });
        if (sender.value) sender.setValue('', { emitEvent: false });
      } else {
        // Văn bản Đến: cần received_at + sender_unit
        received.setValidators([Validators.required]);
        sender.setValidators([Validators.required, Validators.maxLength(255)]);

        // DỌN các trường không dùng của OUTBOUND
        if (issued.value) issued.setValue(null, { emitEvent: false });
        if (recipient.value) recipient.setValue('', { emitEvent: false });
      }

      issued.updateValueAndValidity({ emitEvent: false });
      received.updateValueAndValidity({ emitEvent: false });
      sender.updateValueAndValidity({ emitEvent: false });
      recipient.updateValueAndValidity({ emitEvent: false });
    });
  }

  // thay thế bằng id user đăng nhập nếu có
  private getOriginatorId(): number {
    return 2;
  }

  onPickFiles(ev: Event) {
    const input = ev.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const list = Array.from(input.files);
    // gộp với file cũ (không trùng tên + size để tránh lặp)
    const cur = this.files();
    const merged: File[] = [...cur];

    list.forEach((f) => {
      const exists = merged.some(
        (x) => x.name === f.name && x.size === f.size && x.lastModified === f.lastModified
      );
      if (!exists) merged.push(f);
    });

    this.files.set(merged);
    input.value = ''; // cho phép chọn lại cùng tên
  }

  removeFile(index: number) {
    const arr = [...this.files()];
    arr.splice(index, 1);
    this.files.set(arr);
  }

  submit() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    // Chuẩn hóa payload theo cột DB
    const value = this.form.value;
    const payload: any = {
      doc_type: value.doc_type,
      doc_number: value.doc_number?.trim(),
      title: value.title?.trim(),
      content: value.content?.trim(),
      originator_id: this.getOriginatorId(),
      // only set ngày phù hợp
      issued_at: value.doc_type === 'OUTBOUND' ? this.toDateStr(value.issued_at) : null,
      received_at: value.doc_type === 'INBOUND' ? this.toDateStr(value.received_at) : null,
      // only set đơn vị phù hợp
      sender_unit: value.doc_type === 'INBOUND' ? value.sender_unit?.trim() : null,
      recipient_unit: value.doc_type === 'OUTBOUND' ? value.recipient_unit?.trim() : null,
    };

    const fd = new FormData();
    fd.append('document', new Blob([JSON.stringify(payload)], { type: 'application/json' }));
    this.files().forEach((f, i) => fd.append('attachments', f, f.name));

    this.uploading.set(true);
    this.uploadProgress.set(null);

    // Ví dụ POST multipart: backend nhận "document"(json) + "attachments"[*]
    this.http
      .post(`${this.baseUrl}`, fd, {
        reportProgress: true,
        observe: 'events',
      })
      .subscribe({
        next: (evt: any) => {
          // bắt sự kiện tiến độ upload (HttpEventType.UploadProgress = 1)
          if (evt?.type === 1 && evt.total) {
            const pct = Math.round((evt.loaded / evt.total) * 100);
            this.uploadProgress.set(pct);
          }
          // HttpEventType.Response = 4
          if (evt?.type === 4) {
            this.uploading.set(false);
            alert('Tạo văn bản thành công!');
            this.form.reset({ doc_type: 'OUTBOUND' });
            this.files.set([]);
            this.uploadProgress.set(null);
          }
        },
        error: (err) => {
          this.uploading.set(false);
          this.uploadProgress.set(null);
          console.error(err);
          alert('Có lỗi khi tạo văn bản.');
        },
      });
  }
  

  private toDateStr(d: any): string | null {
    if (!d) return null;
    const dd = d instanceof Date ? d : new Date(d);
    if (isNaN(dd.getTime())) return null;
    // DB đang dùng DATE (YYYY-MM-DD)
    const y = dd.getFullYear();
    const m = String(dd.getMonth() + 1).padStart(2, '0');
    const day = String(dd.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }
  goBack() {
    // C1: về trang chủ/tùy bạn
    // this.router.navigate(['/document-list']);
    // C2 (thay thế): quay lại đúng trang trước đó trong lịch sử
    this.location.back();
  }
}
