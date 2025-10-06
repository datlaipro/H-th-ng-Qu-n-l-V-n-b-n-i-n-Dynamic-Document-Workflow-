package com.github.datlaipro.docflow.api.config;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.util.Objects;

public class R2StorageService {

    private final S3Client s3;
    private final R2ClientFactory.R2Config cfg;

    public R2StorageService() {
        this.s3 = R2ClientFactory.getClient();
        this.cfg = R2ClientFactory.getConfig();
    }

    /**
     * Upload file lên R2 theo key (vd: documents/2025/10/abc.pdf)
     * 
     * @return URL public dùng domain custom/R2.dev nếu có cấu hình
     */
    public String upload(InputStream in, long size, String key, String contentType) {
        Objects.requireNonNull(in, "inputStream");
        Objects.requireNonNull(key, "key");

        final String safeKey = normalizeKey(key);

        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(cfg.bucket)
                .key(safeKey)
                .contentType(contentType != null ? contentType : "application/octet-stream")
                .build();

        s3.putObject(req, RequestBody.fromInputStream(in, size));
        return buildPublicUrl(safeKey);
    }

    // --- Helpers -------------------------------------------------------------

    private String buildPublicUrl(String key) {
        // Ưu tiên domain public (custom domain hoặc *.r2.dev)
        if (cfg.publicBaseUrl != null && !cfg.publicBaseUrl.isEmpty()) {
            String base = stripTrailingSlash(cfg.publicBaseUrl);
            return base + "/" + encodePathPreservingSlashes(key);
        }
        // Fallback: endpoint path-style
        return stripTrailingSlash(cfg.endpoint) + "/" + cfg.bucket + "/" + encodePathPreservingSlashes(key);
    }

    private static String normalizeKey(String key) {
        String k = key.trim();
        // bỏ slash đầu nếu có
        if (k.startsWith("/"))
            k = k.substring(1);
        // tránh double // ở giữa
        while (k.contains("//"))
            k = k.replace("//", "/");
        return k;
    }

    /** Encode từng segment, giữ nguyên dấu '/' để tránh double-encode path */
private static String encodePathPreservingSlashes(String path) {
    String[] parts = path.split("/");
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < parts.length; i++) {
        if (i > 0) sb.append('/');
        try {
            sb.append(URLEncoder.encode(parts[i], "UTF-8").replace("+", "%20"));
        } catch (java.io.UnsupportedEncodingException e) {
            // UTF-8 luôn tồn tại trong JVM; nếu xảy ra thì bọc RuntimeException
            throw new RuntimeException("UTF-8 not supported?", e);
        }
    }
    return sb.toString();
}

    private static String stripTrailingSlash(String s) {
        return (s.endsWith("/")) ? s.substring(0, s.length() - 1) : s;
    }
}
