package infra;

import java.sql.Connection;
import java.sql.DriverManager;

public class Db {
    private static final String DB_HOST = "host.docker.internal"; // hoặc "mysql" (docker-compose), hoặc "172.17.0.1"
    private static final String DB_URL = "jdbc:mysql://" + DB_HOST + ":3306/docflow" +
            "?useUnicode=true&characterEncoding=utf8" +
            "&useSSL=false" +
            "&serverTimezone=UTC" +
            "&allowPublicKeyRetrieval=true"; // tránh lỗi public key với MySQL 8

    private static final String DB_USER = "root";
    private static final String DB_PASS = "rootpass";
    static {
        try {
            Class.forName("com.mysql.jdbc.Driver"); // <-- dùng driver 5.1.x
            System.out.println("[DB] Loaded com.mysql.jdbc.Driver (5.1.x)");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Connection get() throws Exception {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }
}
