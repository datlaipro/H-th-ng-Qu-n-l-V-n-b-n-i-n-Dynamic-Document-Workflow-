package com.github.datlaipro.docflow.api.document.creat.dto;


/** Response tối giản cho API tạo văn bản. */
public class DocumentRes {
    public String message;

    public DocumentRes() {}
    public DocumentRes(String message) { this.message = message; }

    public static DocumentRes success() {
        return new DocumentRes("Tạo mới văn bản thành công");
    }
}

