import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDividerModule } from '@angular/material/divider';
import { MatExpansionModule } from '@angular/material/expansion';
import { DomSanitizer } from '@angular/platform-browser';
import { HttpClient } from '@angular/common/http';

export interface Attachment {
  id: number;
  fileName: string;
  mimeType: string;
  sizeBytes: number;
  url?: string; // link trực tiếp (nếu có)
  downloadApi?: string; // endpoint tải (nếu cần stream blob)
}

export interface DocumentDetail {
  soVanBan: string;
  soTrang: number;
  soLuongVanBan: number;
  noiNhan: string;
  mucDoBaoMat: 'Thấp' | 'Bình thường' | 'Cao';
  nguoiKy: string;
  mucDoKhanCap: 'Bình thường' | 'Khẩn' | 'Khẩn cấp';
  ngayKy: string; // ISO hoặc dd/MM/yyyy
  ngayHieuLuc: string;
  ngayHetHieuLuc: string;
  boPhanPhatHanh: string;
  ngayPhatHanh: string;

  loaiVanBan: string;
  nguoiXuLy: string;
  tinhTrangXuLy: string;
  nguoiTao: string;
  ngayTao: string;

  trichYeu?: string;
  lyDo?: string;
  attachments?: Attachment[];
}

@Component({
  selector: 'app-document-detail',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatDividerModule, MatExpansionModule],
  templateUrl: './document-detail.component.html',
  styleUrls: ['./document-detail.component.css'],
})
export class DocumentDetailComponent {
  private http = inject(HttpClient);
  private sanitizer = inject(DomSanitizer);

  // Nhận dữ liệu từ cha; để demo có thể gán mặc định
  @Input() data: DocumentDetail = {
    soVanBan: '01',
    soTrang: 15,
    soLuongVanBan: 2,
    noiNhan: 'ngõ 15 duy tân, Cầu Giấy, Hà Nội',
    mucDoBaoMat: 'Cao',
    nguoiKy: 'Quản trị hệ thống',
    mucDoKhanCap: 'Bình thường',
    ngayKy: '18/12/2020',
    ngayHieuLuc: '18/12/2020',
    ngayHetHieuLuc: '22/12/2020',
    boPhanPhatHanh: 'Kinh doanh',
    ngayPhatHanh: '20/12/2020',

    loaiVanBan: 'Đề Nghị',
    nguoiXuLy: 'FW.SPMB',
    tinhTrangXuLy: 'Dự thảo',
    nguoiTao: 'FW.SPMB',
    ngayTao: '20/12/2020',

    trichYeu: 'Đề nghị phê duyệt phương án… (nội dung mô tả ngắn gọn trích yếu tài liệu).',
    lyDo: 'Cần triển khai trước ngày 25/12 để đảm bảo tiến độ chiến dịch cuối năm.',
    attachments: [
      {
        id: 1,
        fileName: 'PhuLuc_01.xlsx',
        mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        sizeBytes: 153600,
        url: 'https://example.com/files/PhuLuc_01.xlsx',
      },
    ],
  };

  // ====== Hành động ======
  approve() {
    console.log('Phê duyệt', this.data.soVanBan);
  }
  reject() {
    console.log('Từ chối', this.data.soVanBan);
  }
  forward() {
    console.log('Chuyển tiếp', this.data.soVanBan);
  }
  edit() {
    console.log('Sửa', this.data.soVanBan);
  }
  remove() {
    console.log('Xóa', this.data.soVanBan);
  }

  // ====== Tải tệp đính kèm ======
  // Ưu tiên dùng link trực tiếp nếu có; nếu chỉ có API -> tải blob
  download(att: Attachment) {
    if (att.url) {
      // Mở link trực tiếp (để tải), an toàn hơn là dùng thẻ <a> trong template
      window.open(att.url, '_blank');
      return;
    }
    if (att.downloadApi) {
      this.http.get(att.downloadApi, { responseType: 'blob' }).subscribe((blob) => {
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = att.fileName;
        a.click();
        URL.revokeObjectURL(a.href);
      });
    }
  }

  // Formatter kích thước file
  prettySize(bytes: number) {
    if (!bytes && bytes !== 0) return '';
    const units = ['B', 'KB', 'MB', 'GB'];
    let i = 0,
      n = bytes;
    while (n >= 1024 && i < units.length - 1) {
      n /= 1024;
      i++;
    }
    return `${n.toFixed(1)} ${units[i]}`;
  }
}
