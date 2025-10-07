import { Component, inject, signal, effect } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { startWith, firstValueFrom } from 'rxjs';
import { UploadService } from '../../../domain/document/services/upload.service';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
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

import { AuthService } from '../../../core/auth/auth.service';

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
    MatSnackBarModule,
  ],
  templateUrl: './document-create.component.html',
  styleUrls: ['./document-create.component.css'],
})
export class DocumentCreateComponent {
  private fb = inject(FormBuilder);
  private location = inject(Location);
  private uploader = inject(UploadService);
  private auth = inject(AuthService);
  private snackbar = inject(MatSnackBar); //

  errorMsg = signal<string | null>(null);
  // file chọn ở FE (chưa upload)
  files = signal<File[]>([]);
  // metadata sau khi upload xong (để gửi vào attachments)
  uploaded = signal<{ fileName: string; fileUrl: string; mimeType: string; sizeBytes: number }[]>(
    []
  );
  private parseError(err: any): string {
    // BE của bạn thường trả { error: "..."} hoặc { message: "..." }
    const status = err?.status ?? 0;
    const bodyErr = err?.error?.error || err?.error?.message || err?.message;

    // map nhanh theo HTTP status
    if (status === 0) {
      return 'Không thể kết nối máy chủ (CORS/Mạng).';
    }
    if (status === 400) {
      // validation hoặc request sai
      return bodyErr || 'Dữ liệu không hợp lệ. Vui lòng kiểm tra lại.';
    }
    if (status === 401) {
      return 'Phiên đăng nhập đã hết hạn hoặc chưa đăng nhập.';
    }
    if (status === 413) {
      return 'File quá lớn, vượt quá giới hạn cho phép.';
    }
    if (status === 415) {
      return 'Định dạng file/Content-Type không được hỗ trợ.';
    }
    if (status >= 500) {
      return 'Lỗi máy chủ. Vui lòng thử lại sau.';
    }
    return bodyErr || `Lỗi không xác định (HTTP ${status}).`;
  }

  uploading = signal(false);
  uploadProgress = signal<number | null>(null); // % tổng cho bước upload lên R2

  form = this.fb.group({
    doc_number: [
      '',
      [
        Validators.required,
        Validators.maxLength(100),
        Validators.pattern(/^[A-Z0-9]{2}-\d{4}-\d{2}$/), // hoặc /^[A-Z]{2}-\d{4}-\d{2}$/
      ],
    ],
    doc_type: ['OUTBOUND' as DocType, Validators.required], // Đi (OUTBOUND) / Đến (INBOUND)

    title: ['', [Validators.required, Validators.maxLength(500)]],
    content: ['', [Validators.required]],
    issued_at: [null as Date | null], // OUTBOUND
    received_at: [null as Date | null], // INBOUND
    sender_unit: [''], // INBOUND
    recipient_unit: [''], // OUTBOUND
  });

  docType = toSignal(
    this.form
      .get('doc_type')!
      .valueChanges.pipe(startWith(this.form.get('doc_type')!.value as DocType)),
    { initialValue: this.form.get('doc_type')!.value as DocType }
  );

  constructor() {
    effect(() => {
      const t = this.docType();
      const issued = this.form.get('issued_at')!;
      const received = this.form.get('received_at')!;
      const sender = this.form.get('sender_unit')!;
      const recipient = this.form.get('recipient_unit')!;

      issued.clearValidators();
      received.clearValidators();
      sender.clearValidators();
      recipient.clearValidators();

      if (t === 'OUTBOUND') {
        issued.setValidators([Validators.required]);
        recipient.setValidators([Validators.required, Validators.maxLength(255)]);

        if (received.value) received.setValue(null, { emitEvent: false });
        if (sender.value) sender.setValue('', { emitEvent: false });
      } else {
        received.setValidators([Validators.required]);
        sender.setValidators([Validators.required, Validators.maxLength(255)]);

        if (issued.value) issued.setValue(null, { emitEvent: false });
        if (recipient.value) recipient.setValue('', { emitEvent: false });
      }

      issued.updateValueAndValidity({ emitEvent: false });
      received.updateValueAndValidity({ emitEvent: false });
      sender.updateValueAndValidity({ emitEvent: false });
      recipient.updateValueAndValidity({ emitEvent: false });
    });
  }

  // thay thế bằng id user đăng nhập nếu có (nếu BE cần)
  private getOriginatorId(): number {
    return 2;
  }

  onPickFiles(ev: Event) {
    const input = ev.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;

    const list = Array.from(input.files);
    const cur = this.files();
    const merged: File[] = [...cur];

    list.forEach((f) => {
      const exists = merged.some(
        (x) => x.name === f.name && x.size === f.size && x.lastModified === f.lastModified
      );
      if (!exists) merged.push(f);
    });

    this.files.set(merged);
    input.value = '';
  }

  removeFile(index: number) {
    const arr = [...this.files()];
    arr.splice(index, 1);
    this.files.set(arr);
  }

  // ===== Upload lên R2/S3 trước, lấy publicUrl để điền attachments =====
  private async uploadAllSelected(): Promise<void> {
    const files = this.files();
    if (!files.length) {
      this.uploaded.set([]);
      return;
    }

    this.uploadProgress.set(0);
    const results: { fileName: string; fileUrl: string; mimeType: string; sizeBytes: number }[] =
      [];

    let uploadedBytes = 0;
    const totalBytes = files.reduce((sum, f) => sum + f.size, 0) || 1;

    for (const f of files) {
      // 1) xin presign
      const presign = await firstValueFrom(
        this.uploader.getPresign(f.name, f.type || 'application/octet-stream', f.size)
      );
      if (!presign?.url || !presign?.publicUrl) {
        throw new Error('Không nhận được presigned URL hợp lệ.');
      }
      console.log('presign-', presign);
      // 2) PUT trực tiếp file lên R2/S3
      await new Promise<void>((resolve, reject) => {
        this.uploader
          .uploadToR2(presign.url, f, f.type || 'application/octet-stream', presign.headers)
          .subscribe({
            next: (evt) => {
              // evt.type === UploadProgress (1) → không phụ thuộc enum, chỉ tính toán tổng
              // cập nhật theo byte tương đối (phần của file hiện tại)
              // Lưu ý: nhiều server không trả total ở PUT; trong trường hợp đó, ta chỉ cập nhật khi complete.
              if ((evt as any)?.loaded) {
                const fileLoaded = (evt as any).loaded as number;
                const doneSoFar = uploadedBytes + fileLoaded;
                const pct = Math.min(100, Math.round((doneSoFar / totalBytes) * 100));
                this.uploadProgress.set(pct);
              }
            },
            error: (err) => {
              reject(err);
            },
            complete: () => {
              uploadedBytes += f.size;
              const pct = Math.min(100, Math.round((uploadedBytes / totalBytes) * 100));
              this.uploadProgress.set(pct);

              results.push({
                fileName: f.name,
                fileUrl: presign.publicUrl,
                mimeType: f.type || 'application/octet-stream',
                sizeBytes: f.size,
              });
              resolve();
            },
          });
      });
    }

    this.uploaded.set(results);
    this.uploadProgress.set(null);
  }

  async submit() {
    this.errorMsg.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      const msg = 'Vui lòng điền đủ các trường bắt buộc.';
      this.errorMsg.set(msg);
      this.snackbar.open(msg, 'Đóng', { duration: 3000 });
      return;
    }

    try {
      this.uploading.set(true);

      await this.uploadAllSelected();

      const v = this.form.value;
      const type = v.doc_type as DocType;

      const body: any = {
        type,
        number: v.doc_number?.trim(),
        title: v.title?.trim(),
        content: v.content?.trim(),
        attachments: this.uploaded().map((a) => ({
          fileName: a.fileName,
          fileUrl: a.fileUrl,
          mimeType: a.mimeType,
          sizeBytes: a.sizeBytes,
        })),
      };

      if (type === 'OUTBOUND') {
        body.issued_at = this.toDateStr(v.issued_at);
        body.recipient_unit = v.recipient_unit?.trim();
      } else {
        body.received_at = this.toDateStr(v.received_at);
        body.sender_unit = v.sender_unit?.trim();
      }

      await firstValueFrom(this.auth.createDocument(body));

      this.uploading.set(false);
      this.snackbar.open('Tạo văn bản thành công!', 'Đóng', { duration: 2500 });
      this.form.reset({ doc_type: 'OUTBOUND' });
      this.files.set([]);
      this.uploaded.set([]);
      this.uploadProgress.set(null);
    } catch (err: any) {
      this.uploading.set(false);
      this.uploadProgress.set(null);

      const raw = err?.error?.error || err?.error?.message || '';
      // ví dụ BE nối bằng dấu '; ' như "number không hợp lệ; receivedAt phải yyyy-MM-dd"
      if (err?.status === 400 && typeof raw === 'string') {
        const lower = raw.toLowerCase();
        if (lower.includes('number')) {
          this.form.get('doc_number')?.setErrors({ server: true });
        }
        if (lower.includes('issuedat') || lower.includes('issued_at')) {
          this.form.get('issued_at')?.setErrors({ server: true });
        }
        if (lower.includes('receivedat') || lower.includes('received_at')) {
          this.form.get('received_at')?.setErrors({ server: true });
        }
        if (lower.includes('recipient')) {
          this.form.get('recipient_unit')?.setErrors({ server: true });
        }
        if (lower.includes('sender')) {
          this.form.get('sender_unit')?.setErrors({ server: true });
        }
      }

      const msg = this.parseError(err);
      this.errorMsg.set(msg);
      this.snackbar.open(msg, 'Đóng', { duration: 4000 });
    }
  }

  private toDateStr(d: any): string | null {
    if (!d) return null;
    const dd = d instanceof Date ? d : new Date(d);
    if (isNaN(dd.getTime())) return null;
    const y = dd.getFullYear();
    const m = String(dd.getMonth() + 1).padStart(2, '0');
    const day = String(dd.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  goBack() {
    this.location.back();
  }
}
