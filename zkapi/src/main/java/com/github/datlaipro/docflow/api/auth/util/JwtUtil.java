package com.github.datlaipro.docflow.api.auth.util;


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {
    // ĐỔI SECRET trong môi trường thực (đặt ENV/Properties)
    private static final String SECRET = "CHANGE_ME_SUPER_SECRET_256_BITS";
    private static final Gson gson = new Gson();

    private static String b64Url(byte[] b) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static byte[] hmacSha256(byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(data);
    }

    public static String sign(long userId, String role, String email, long expiresEpochSec) throws Exception {
        Map<String,Object> header = new HashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        Map<String,Object> payload = new HashMap<>();
        payload.put("sub", userId);
        payload.put("role", role);
        payload.put("email", email);
        payload.put("exp", expiresEpochSec);

        String h = b64Url(gson.toJson(header).getBytes(StandardCharsets.UTF_8));
        String p = b64Url(gson.toJson(payload).getBytes(StandardCharsets.UTF_8));
        String msg = h + "." + p;
        String s  = b64Url(hmacSha256(msg.getBytes(StandardCharsets.UTF_8)));
        return msg + "." + s;
    }

    public static Map<String,Object> verify(String token) throws Exception {
        String[] parts = token.split("\\.");
        if (parts.length != 3) return null;
        String msg = parts[0] + "." + parts[1];
        String sig = parts[2];

        String expected = b64Url(hmacSha256(msg.getBytes(StandardCharsets.UTF_8)));
        if (!expected.equals(sig)) return null;

        String json = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        Map<String,Object> payload = gson.fromJson(json, Map.class);
        double exp = (double) payload.get("exp");
        long now = System.currentTimeMillis()/1000L;
        if (now > (long)exp) return null;
        return payload;
    }
}

