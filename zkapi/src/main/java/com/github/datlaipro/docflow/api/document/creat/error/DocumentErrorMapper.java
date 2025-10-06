package com.github.datlaipro.docflow.api.document.creat.error;

import java.sql.SQLException;

public final class DocumentErrorMapper {
    private DocumentErrorMapper(){}

    /**
     * Chuẩn hoá lỗi SQL (ví dụ Duplicate entry) thành exception nghiệp vụ dễ hiểu cho FE.
     * - MySQL duplicate: SQLState = "23000", message chứa "Duplicate entry"
     */
    public static RuntimeException map(Throwable t, String docType, String docNumber) {
        if (t instanceof SQLException) {
            SQLException se = (SQLException) t;
            String sqlState = se.getSQLState();
            String msg = se.getMessage() != null ? se.getMessage() : "";
            if ("23000".equals(sqlState) && msg.toLowerCase().contains("duplicate entry")) {
                String txt = String.format("Số hiệu văn bản đã tồn tại: (%s, %s)", docType, docNumber);
                return new DocumentDuplicateException(txt);
            }
            return new DocumentDatabaseException("Database error", se);
        }
        // Không phải SQLException -> trả lại như Validation (mặc định) hoặc giữ nguyên
        return (t instanceof RuntimeException) ? (RuntimeException) t
                : new DocumentDatabaseException("Unexpected error", t);
    }
}
