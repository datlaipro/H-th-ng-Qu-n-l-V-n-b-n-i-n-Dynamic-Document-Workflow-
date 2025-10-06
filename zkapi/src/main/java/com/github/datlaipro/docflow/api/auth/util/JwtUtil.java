package com.github.datlaipro.docflow.api.auth.util;

import com.google.gson.Gson;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public final class JwtUtil {
  private static final Logger LOG = Logger.getLogger(JwtUtil.class.getName());
  private static final String ALG = "HmacSHA256";
  private static final Gson GSON = new Gson();

  // Lấy secret từ ENV → System property → fallback hằng (dev)
  private static final String SECRET = resolveSecret();

  private JwtUtil() {}

  private static String resolveSecret() {
    String s = System.getenv("JWT_SECRET");
    if (s == null || s.isEmpty()) s = System.getProperty("jwt.secret");
    if (s == null || s.isEmpty()) s = "CHANGE_ME_SUPER_SECRET_256_BITS";
    try {
      LOG.info("[JWT] SECRET starts with: " + (s.length() >= 5 ? s.substring(0, 5) : s.length()) + "***");
    } catch (Exception ignore) {}
    return s;
  }

  private static String b64Url(byte[] b) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  }

  private static byte[] b64UrlDecode(String s) {
    // Chuẩn Base64URL (không padding); thêm padding nếu thiếu
    String p = s.replace('-', '+').replace('_', '/');
    switch (p.length() % 4) { case 2: p += "=="; break; case 3: p += "="; break; }
    return Base64.getDecoder().decode(p);
  }

  private static byte[] hmacSha256(byte[] data) throws Exception {
    Mac mac = Mac.getInstance(ALG);
    mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), ALG));
    return mac.doFinal(data);
  }

  public static String sign(long userId, String role, String email, long expEpochSec) throws Exception {
    Map<String, Object> header = new HashMap<>();
    header.put("alg", "HS256");
    header.put("typ", "JWT");

    Map<String, Object> payload = new HashMap<>();
    payload.put("sub", userId);
    payload.put("role", role);
    payload.put("email", email);
    payload.put("exp", expEpochSec);

    String h = b64Url(GSON.toJson(header).getBytes(StandardCharsets.UTF_8));
    String p = b64Url(GSON.toJson(payload).getBytes(StandardCharsets.UTF_8));
    String msg = h + "." + p;
    String s = b64Url(hmacSha256(msg.getBytes(StandardCharsets.UTF_8)));
    return msg + "." + s;
  }

  public static Map<String, Object> verify(String token) {
    try {
      if (token == null || token.isEmpty()) {
        LOG.warning("[JWT] token empty");
        return null;
      }
      String[] parts = token.split("\\.");
      if (parts.length != 3) {
        LOG.warning("[JWT] format invalid");
        return null;
      }

      String msg = parts[0] + "." + parts[1];
      byte[] expected = hmacSha256(msg.getBytes(StandardCharsets.UTF_8));
      byte[] actual = b64UrlDecode(parts[2]);
      if (!MessageDigest.isEqual(actual, expected)) {
        LOG.warning("[JWT] signature mismatch");
        return null;
      }

      String payloadJson = new String(b64UrlDecode(parts[1]), StandardCharsets.UTF_8);
      Map<String, Object> claims = GSON.fromJson(payloadJson, Map.class);

      // exp check + skew 60s
      long now = System.currentTimeMillis() / 1000L;
      long exp = toLong(claims.get("exp"), -1);
      if (exp > 0 && now > exp + 60) {
        LOG.warning("[JWT] expired: now=" + now + " exp=" + exp);
        return null;
      }

      return claims;
    } catch (Exception e) {
      LOG.warning("[JWT] verify error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
      return null;
    }
  }

  private static long toLong(Object v, long def) {
    try {
      if (v instanceof Number) return ((Number) v).longValue();
      if (v instanceof String) return Long.parseLong(((String) v).trim());
    } catch (Exception ignore) {}
    return def;
  }
}
