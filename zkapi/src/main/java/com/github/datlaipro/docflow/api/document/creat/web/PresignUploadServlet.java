package com.github.datlaipro.docflow.api.document.creat.web;


import com.github.datlaipro.docflow.api.config.R2ClientFactory;
import com.github.datlaipro.docflow.api.config.R2ClientFactory.R2Config;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import javax.servlet.*;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

public class PresignUploadServlet extends HttpServlet {

  private S3Presigner presigner;
  private R2Config cfg;

  @Override public void init() {
    // Lấy config tập trung từ factory (ENV hoặc r2.properties)
    cfg = R2ClientFactory.getConfig();

    S3Presigner.Builder b = S3Presigner.builder()
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(cfg.accessKeyId, cfg.secretAccessKey)))
        .region(Region.of(cfg.region != null && !cfg.region.isEmpty() ? cfg.region : "auto"))
        // Cloudflare R2 cần endpoint override
        .endpointOverride(URI.create(cfg.endpoint));

    presigner = b.build();
  }

  @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    handlePresign(req, resp);
  }

  // Cho phép FE gửi FormData (action=presign, name, type, size, prefix)
  @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
    // Nếu body là multipart với field 'action=presign' thì vẫn xử lý tương tự
    String action = param(req, "action");
    if (action == null && req.getContentType() != null && req.getContentType().toLowerCase().startsWith("multipart/")) {
      // đọc từ form-data (không bắt buộc, vì nhiều HTTP client vẫn gửi được query)
      // nhưng để đơn giản ta vẫn dùng req.getParameter(...)
    }
    handlePresign(req, resp);
  }

  private void handlePresign(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setCharacterEncoding("UTF-8");
    resp.setContentType("application/json; charset=UTF-8");// 

    final String name = param(req, "name");
    final String type = paramOr(req, "type", "application/octet-stream");
    final String sizeStr = paramOr(req, "size", "0");
    final String prefix = normalizePrefix(param(req, "prefix")); // ví dụ: "documents/outbound"

    if (isBlank(name)) {
      bad(resp, "Missing 'name'");
      return;
    }

    final String key = buildObjectKey(prefix, name);

    PutObjectRequest putReq = PutObjectRequest.builder()
        .bucket(cfg.bucket)
        .key(key)
        .contentType(type)
        .build();

    PutObjectPresignRequest presignReq = PutObjectPresignRequest.builder()
        .signatureDuration(Duration.ofMinutes(10))
        .putObjectRequest(putReq)
        .build();

    final String putUrl = presigner.presignPutObject(presignReq).url().toString();
    final String publicUrl = buildPublicUrl(cfg, key);

    ok(resp, "{\"url\":\"" + esc(putUrl) + "\",\"key\":\"" + esc(key) + "\",\"publicUrl\":\"" + esc(publicUrl) + "\"}");
  }

  @Override public void destroy() {
    if (presigner != null) presigner.close();
  }

  // ---------------- helpers ----------------

  private static String param(HttpServletRequest r, String k) { 
    String v = r.getParameter(k); return v != null ? v.trim() : null; 
  }
  private static String paramOr(HttpServletRequest r, String k, String d) {
    String v = param(r, k); return (v == null || v.isEmpty()) ? d : v;
  }
  private static boolean isBlank(String s){ return s == null || s.trim().isEmpty(); }

  private static String buildObjectKey(String prefix, String originalName) {
    String safeName = originalName.replaceAll("[^A-Za-z0-9._-]", "_");
    String date = LocalDate.now().toString(); // yyyy-MM-dd
    String uuid = UUID.randomUUID().toString();
    String base = date + "/" + uuid + "__" + safeName;
    return (prefix != null && !prefix.isEmpty()) ? (prefix + "/" + base) : base;
  }

  private static String normalizePrefix(String p) {
    if (p == null) return null;
    p = p.trim();
    while (p.startsWith("/")) p = p.substring(1);
    while (p.endsWith("/")) p = p.substring(0, p.length()-1);
    return p;
  }

  private static String buildPublicUrl(R2Config cfg, String key) {
    // Ưu tiên publicBaseUrl nếu có (custom domain/R2.dev/CDN)
    if (cfg.publicBaseUrl != null && !cfg.publicBaseUrl.isEmpty()) {
      return stripTrailingSlash(cfg.publicBaseUrl) + "/" + encodePathPreservingSlashes(key);
    }
    // Fallback: build từ endpoint theo path-style
    // endpoint ví dụ: https://<accountid>.r2.cloudflarestorage.com
    String base = stripTrailingSlash(cfg.endpoint);
    return base + "/" + cfg.bucket + "/" + encodePathPreservingSlashes(key);
  }

  private static String stripTrailingSlash(String s) {
    return s.endsWith("/") ? s.substring(0, s.length()-1) : s;
  }

  /** Encode từng segment, giữ '/' */
  private static String encodePathPreservingSlashes(String path) {
    String[] parts = path.split("/");
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < parts.length; i++) {
      if (i > 0) sb.append('/');
      try {
        sb.append(java.net.URLEncoder.encode(parts[i], "UTF-8").replace("+", "%20"));
      } catch (java.io.UnsupportedEncodingException e) {
        throw new RuntimeException("UTF-8 not supported?", e);
      }
    }
    return sb.toString();
  }

  private static String esc(String s){ return s==null?"":s.replace("\\","\\\\").replace("\"","\\\""); }

  private static void bad(HttpServletResponse resp, String msg) throws IOException {
    resp.setStatus(400); resp.getWriter().write("{\"error\":\"" + esc(msg) + "\"}");
  }
  private static void ok(HttpServletResponse resp, String json) throws IOException {
    resp.setStatus(200); resp.getWriter().write(json);
  }
}
