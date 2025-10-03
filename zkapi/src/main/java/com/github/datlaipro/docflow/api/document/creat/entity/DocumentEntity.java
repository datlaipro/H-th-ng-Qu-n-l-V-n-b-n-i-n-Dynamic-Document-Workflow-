package com.github.datlaipro.docflow.api.document.creat.entity;

import java.sql.Date;

public class DocumentEntity {
    public Long id;
    public String type;
    public String number;// số hiệu văn bản
    public String title;
    public String content;
    public String status;
    public Date createdAt;
    public Date updatedAt;
    public Long createdBy;
   public String current_handler_id;// người đăng xử lý hiện tại 
   public Date issued_at;//ngày ban hành
   public Date received_at;//ngày nhận
   public String sender_unit;//đơn vị gửi
    public String receiver_unit;//đơn vị nhận

}
