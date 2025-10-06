package com.github.datlaipro.docflow.api.config;



import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;

public final class R2ClientFactory {

    private static volatile S3Client INSTANCE;
    private static volatile R2Config CONFIG;

    private R2ClientFactory() {}

    public static S3Client getClient() {
        if (INSTANCE == null) {
            synchronized (R2ClientFactory.class) {
                if (INSTANCE == null) {
                    R2Config cfg = getConfig();
                    INSTANCE = S3Client.builder()
                            .region(Region.of(cfg.region)) // us-east-1 (placeholder hợp lệ)
                            .credentialsProvider(StaticCredentialsProvider.create(
                                    AwsBasicCredentials.create(cfg.accessKeyId, cfg.secretAccessKey)))
                            .endpointOverride(URI.create(cfg.endpoint)) // ví dụ: https://<yourid>.r2.cloudflarestorage.com
                            .serviceConfiguration(S3Configuration.builder()
                                    .pathStyleAccessEnabled(true) // path-style để tương thích R2
                                    .build())
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public static R2Config getConfig() {
        if (CONFIG == null) {
            synchronized (R2ClientFactory.class) {
                if (CONFIG == null) {
                    CONFIG = loadConfig();
                }
            }
        }
        return CONFIG;
    }

    private static R2Config loadConfig() {
        // ƯU TIÊN ENV, sau đó r2.properties
        String accessKey = getenvOrNull("R2_ACCESS_KEY_ID");
        String secretKey = getenvOrNull("R2_SECRET_ACCESS_KEY");
        String bucket = getenvOrNull("R2_BUCKET");
        String endpoint = getenvOrNull("R2_ENDPOINT");
        String region = getenvOrNull("R2_REGION");
        String publicBaseUrl = getenvOrNull("R2_PUBLIC_BASE_URL");

        if (accessKey == null || secretKey == null || bucket == null || endpoint == null) {
            Properties p = new Properties();
            try (InputStream in = R2ClientFactory.class.getClassLoader().getResourceAsStream("r2.properties")) {
                if (in != null) p.load(in);
            } catch (IOException e) {
                throw new RuntimeException("Cannot load r2.properties", e);
            }

            if (accessKey == null) accessKey = p.getProperty("R2_ACCESS_KEY_ID");
            if (secretKey == null) secretKey = p.getProperty("R2_SECRET_ACCESS_KEY");
            if (bucket == null) bucket = p.getProperty("R2_BUCKET");
            if (endpoint == null) endpoint = p.getProperty("R2_ENDPOINT"); // DÙNG THẲNG endpoint đầy đủ
            if (region == null) region = p.getProperty("R2_REGION", "us-east-1");
            if (publicBaseUrl == null) publicBaseUrl = p.getProperty("R2_PUBLIC_BASE_URL");
        }

        if (region == null || region.isEmpty()) region = "us-east-1";

        // Kiểm tra đầu vào tối thiểu
        if (isBlank(accessKey) || isBlank(secretKey) || isBlank(bucket) || isBlank(endpoint)) {
            throw new IllegalStateException(
                "Missing R2 config. Required: R2_ACCESS_KEY_ID, R2_SECRET_ACCESS_KEY, R2_BUCKET, R2_ENDPOINT. " +
                "Optional: R2_REGION (default us-east-1), R2_PUBLIC_BASE_URL."
            );
        }

        return new R2Config(accessKey, secretKey, bucket, endpoint, region, publicBaseUrl);
    }

    private static String getenvOrNull(String k) {
        String v = System.getenv(k);
        return (v == null || v.isEmpty()) ? null : v;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // Cấu hình đã load (KHÔNG còn accountId)
    public static final class R2Config {
        public final String accessKeyId;
        public final String secretAccessKey;
        public final String bucket;
        public final String endpoint;
        public final String region;
        public final String publicBaseUrl; // có thể null

        public R2Config(String accessKeyId, String secretAccessKey,
                        String bucket, String endpoint, String region, String publicBaseUrl) {
            this.accessKeyId = accessKeyId;
            this.secretAccessKey = secretAccessKey;
            this.bucket = bucket;
            this.endpoint = endpoint;
            this.region = region;
            this.publicBaseUrl = publicBaseUrl;
        }
    }
}
