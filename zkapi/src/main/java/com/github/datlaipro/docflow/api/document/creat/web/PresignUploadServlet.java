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
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Collections;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

public class PresignUploadServlet extends HttpServlet {
  private static final long MAX_SIZE = 20L * 1024 * 1024; // 20MB

  // MIME cho phép
  private static final Set<String> ALLOWED_MIME = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
      "application/pdf",
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      "image/png",
      "image/jpeg")));

  // Extension chặn
  private static final Set<String> BLOCKED_EXT = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
      "jar", "war", "ear", "class", "exe", "msi", "bat", "cmd", "sh", "ps1",
      "js", "mjs", "php", "pl", "py", "rb", "cgi", "html", "htm", "svg")));

  private S3Presigner presigner;
  private R2Config cfg;

  @Override
  public void init() {
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

  @Override
  protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    setCors(resp, req.getHeader("Origin"));
    resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    setCors(resp, req.getHeader("Origin"));
    handlePresign(req, resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
    setCors(resp, req.getHeader("Origin"));
    handlePresign(req, resp);
  }

  private void handlePresign(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    resp.setCharacterEncoding("UTF-8");
    resp.setContentType("application/json; charset=UTF-8");

    final String name = param(req, "name");
    final String type = paramOr(req, "type", "application/octet-stream");
    final String sizeStr = paramOr(req, "size", "0");
    final String prefix = normalizePrefix(param(req, "prefix")); // ví dụ: documents/inbound

    if (isBlank(name)) {
      bad(resp, "Missing 'name'");
      return;
    }

    // ---- VALIDATION: tên/đuôi/size/MIME ----
    final long size;
    try {
      size = Long.parseLong(sizeStr);
    } catch (NumberFormatException e) {
      bad(resp, "Invalid 'size'");
      return;
    }

    if (size <= 0 || size > MAX_SIZE) {
      bad(resp, "File quá lớn hoặc kích thước không hợp lệ (<= 20MB)");
      return;
    }

    final String ext = getExtLower(name);
    if (BLOCKED_EXT.contains(ext)) {
      bad(resp, "Định dạng file không được phép");
      return;
    }
    if (!ALLOWED_MIME.contains(type)) {
      bad(resp, "Content-Type không được phép");
      return;
    }

    // (Tuỳ chọn) hạn chế prefix hợp lệ để tránh ghi lung tung
if (prefix != null && !( "documents".equals(prefix)
      || prefix.startsWith("documents/inbound")
      || prefix.startsWith("documents/outbound"))) {
  bad(resp, "Prefix không hợp lệ");
  return;
}

    final String key = buildObjectKey(prefix, name);

    // RÀNG BUỘC Content-Type vào presign (client PUT phải gửi đúng header này)
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

    // FE sẽ set đúng headers khi upload (đặc biệt là Content-Type)
    String json = "{"
        + "\"url\":\"" + esc(putUrl) + "\","
        + "\"key\":\"" + esc(key) + "\","
        + "\"publicUrl\":\"" + esc(publicUrl) + "\","
        + "\"headers\":{\"Content-Type\":\"" + esc(type) + "\"}"
        + "}";

    ok(resp, json);
  }

  @Override
  public void destroy() {
    if (presigner != null)
      presigner.close();
  }

  // -------- helpers (thêm vài cái nhỏ) --------
  private static String getExtLower(String name) {
    if (name == null)
      return "";
    int i = name.lastIndexOf('.');
    return (i >= 0 && i < name.length() - 1) ? name.substring(i + 1).toLowerCase() : "";
  }

  private static void setCors(HttpServletResponse resp, String origin) {
    if (origin != null && origin.startsWith("http://localhost:4200")) {
      resp.setHeader("Access-Control-Allow-Origin", origin);
      resp.setHeader("Vary", "Origin");
      resp.setHeader("Access-Control-Allow-Credentials", "true");
      resp.setHeader("Access-Control-Allow-Methods", "GET,POST,OPTIONS");
      resp.setHeader("Access-Control-Allow-Headers",
          "Origin,Accept,X-Requested-With,Content-Type,Authorization,Access-Control-Request-Method,Access-Control-Request-Headers");
      resp.setHeader("Access-Control-Max-Age", "3600");
    }
  }
  // ---------------- helpers ----------------

  private static String param(HttpServletRequest r, String k) {
    String v = r.getParameter(k);
    return v != null ? v.trim() : null;
  }

  private static String paramOr(HttpServletRequest r, String k, String d) {
    String v = param(r, k);
    return (v == null || v.isEmpty()) ? d : v;
  }

  private static boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
  }

  private static String buildObjectKey(String prefix, String originalName) {
    String safeName = originalName.replaceAll("[^A-Za-z0-9._-]", "_");
    String date = LocalDate.now().toString(); // yyyy-MM-dd
    String uuid = UUID.randomUUID().toString();
    String base = date + "/" + uuid + "__" + safeName;
    return (prefix != null && !prefix.isEmpty()) ? (prefix + "/" + base) : base;
  }

  private static String normalizePrefix(String p) {
    if (p == null)
      return null;
    p = p.trim();
    while (p.startsWith("/"))
      p = p.substring(1);
    while (p.endsWith("/"))
      p = p.substring(0, p.length() - 1);
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
    return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
  }

  /** Encode từng segment, giữ '/' */
  private static String encodePathPreservingSlashes(String path) {
    String[] parts = path.split("/");
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < parts.length; i++) {
      if (i > 0)
        sb.append('/');
      try {
        sb.append(java.net.URLEncoder.encode(parts[i], "UTF-8").replace("+", "%20"));
      } catch (java.io.UnsupportedEncodingException e) {
        throw new RuntimeException("UTF-8 not supported?", e);
      }
    }
    return sb.toString();
  }

  private static String esc(String s) {
    return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
  }

  private static void bad(HttpServletResponse resp, String msg) throws IOException {
    resp.setStatus(400);
    resp.getWriter().write("{\"error\":\"" + esc(msg) + "\"}");
  }

  private static void ok(HttpServletResponse resp, String json) throws IOException {
    resp.setStatus(200);
    resp.getWriter().write(json);
  }
}
