package com.kec.placement;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * DBConnection provides JDBC connection management to Student 2's remote database.
 * 
 * Target Environment:
 * - Hosted on OCI VM 2 by Student 2 (Database Host).
 * - Supported RDBMS: MySQL (Port 3306) and PostgreSQL (Port 5432).
 * - Configuration priority:
 *     1. System Environment Variables (Production on OCI VM 1: DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD, DB_TYPE)
 *     2. application.properties (Local fallback)
 *     3. Safe development defaults
 */
public class DBConnection {

    private static String dbType = "mysql";
    private static String host = "localhost";
    private static int port = 3306;
    private static String database = "kec_placement_db";
    private static String username = "kec_placement_user";
    private static String password = "KecPlacement@2026";
    private static String customJdbcUrl = null;

    static {
        loadConfiguration();
    }

    /**
     * Loads database credentials from environment variables or application.properties.
     */
    public static void loadConfiguration() {
        // 1. Load from properties file as base defaults
        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                dbType = props.getProperty("db.type", dbType);
                host = props.getProperty("db.host", host);
                port = Integer.parseInt(props.getProperty("db.port", String.valueOf(port)));
                database = props.getProperty("db.name", database);
                username = props.getProperty("db.user", username);
                password = props.getProperty("db.password", password);
            }
        } catch (Exception ignored) {
            // Defaults remain active
        }

        // 2. Override with Environment Variables (Essential for OCI VM 1 Deployment)
        String envUrl = System.getenv("DB_URL");
        if (envUrl != null && !envUrl.trim().isEmpty()) {
            customJdbcUrl = envUrl.trim();
        }

        String envType = System.getenv("DB_TYPE");
        if (envType != null && !envType.trim().isEmpty()) {
            dbType = envType.trim().toLowerCase();
        }

        String envHost = System.getenv("DB_HOST");
        if (envHost != null && !envHost.trim().isEmpty()) {
            host = envHost.trim();
        }

        String envPort = System.getenv("DB_PORT");
        if (envPort != null && !envPort.trim().isEmpty()) {
            try {
                port = Integer.parseInt(envPort.trim());
            } catch (NumberFormatException ignored) {}
        }

        String envDb = System.getenv("DB_NAME");
        if (envDb != null && !envDb.trim().isEmpty()) {
            database = envDb.trim();
        }

        String envUser = System.getenv("DB_USER");
        if (envUser != null && !envUser.trim().isEmpty()) {
            username = envUser.trim();
        }

        String envPass = System.getenv("DB_PASSWORD");
        if (envPass != null) {
            password = envPass;
        }
    }

    /**
     * Builds JDBC connection URL based on configured database type.
     */
    public static String getJdbcUrl() {
        if (customJdbcUrl != null && !customJdbcUrl.trim().isEmpty()) {
            return customJdbcUrl;
        }

        if ("postgresql".equalsIgnoreCase(dbType)) {
            return String.format("jdbc:postgresql://%s:%d/%s?sslmode=disable", host, port, database);
        } else {
            // MySQL (Default)
            return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=5000",
                    host, port, database);
        }
    }

    /**
     * Obtains an active JDBC connection to Student 2's remote database.
     *
     * @return Connection object
     * @throws SQLException on connectivity failure
     */
    public static Connection getConnection() throws SQLException {
        ensureDriverLoaded();
        String url = getJdbcUrl();
        return DriverManager.getConnection(url, username, password);
    }

    private static void ensureDriverLoaded() {
        try {
            if ("postgresql".equalsIgnoreCase(dbType)) {
                Class.forName("org.postgresql.Driver");
            } else {
                Class.forName("com.mysql.cj.jdbc.Driver");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("[DBConnection Warning] JDBC Driver class not found: " + e.getMessage());
        }
    }

    /**
     * Diagnostic probe to verify remote connectivity to Student 2's database on OCI VM 2.
     *
     * @return true if connection succeeds within timeout, false otherwise
     */
    public static boolean testConnection() {
        long start = System.currentTimeMillis();
        String url = getJdbcUrl();
        System.out.printf("[DBConnection] Probing remote database at %s (User: %s)...%n", url, username);

        try (Connection conn = getConnection()) {
            long latency = System.currentTimeMillis() - start;
            boolean valid = conn.isValid(5);
            if (valid) {
                System.out.printf("[DBConnection SUCCESS] Connected to Student 2's database in %d ms (Product: %s %s)%n",
                        latency, conn.getMetaData().getDatabaseProductName(), conn.getMetaData().getDatabaseProductVersion());
                return true;
            } else {
                System.err.println("[DBConnection FAILED] Connection established but validation query timed out.");
                return false;
            }
        } catch (SQLException e) {
            long latency = System.currentTimeMillis() - start;
            System.err.printf("[DBConnection ERROR] Failed to connect after %d ms: %s (SQLState: %s, ErrorCode: %d)%n",
                    latency, e.getMessage(), e.getSQLState(), e.getErrorCode());
            return false;
        } catch (Throwable t) {
            long latency = System.currentTimeMillis() - start;
            Throwable cause = t;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            String err = (cause.getMessage() != null) ? cause.getMessage() : cause.getClass().getSimpleName();
            System.err.printf("[DBConnection ERROR] Remote DB probe failed after %d ms: %s (%s)%n",
                    latency, err, cause.getClass().getName());
            return false;
        }
    }

    // Dynamic configuration setters (useful for CLI/testing overrides)
    public static void setConfiguration(String newHost, int newPort, String newDb, String newUser, String newPass, String newType) {
        host = newHost;
        port = newPort;
        database = newDb;
        username = newUser;
        password = newPass;
        dbType = newType;
        customJdbcUrl = null;
    }

    public static String getHost() { return host; }
    public static int getPort() { return port; }
    public static String getDatabase() { return database; }
    public static String getUsername() { return username; }
    public static String getDbType() { return dbType; }
}
