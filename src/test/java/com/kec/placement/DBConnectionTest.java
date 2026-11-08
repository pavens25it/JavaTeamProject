package com.kec.placement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying DBConnection URL generation, environment configuration,
 * and graceful fallback handling.
 */
class DBConnectionTest {

    @Test
    @DisplayName("Should generate proper MySQL JDBC URL by default")
    void testMysqlJdbcUrl() {
        DBConnection.setConfiguration("10.0.0.5", 3306, "kec_placement_db", "user", "pass", "mysql");
        String url = DBConnection.getJdbcUrl();
        assertTrue(url.startsWith("jdbc:mysql://10.0.0.5:3306/kec_placement_db"));
        assertTrue(url.contains("useSSL=false"));
    }

    @Test
    @DisplayName("Should generate proper PostgreSQL JDBC URL when configured")
    void testPostgresJdbcUrl() {
        DBConnection.setConfiguration("10.0.0.6", 5432, "kec_placement_db", "user", "pass", "postgresql");
        String url = DBConnection.getJdbcUrl();
        assertEquals("jdbc:postgresql://10.0.0.6:5432/kec_placement_db?sslmode=disable", url);
    }

    @Test
    @DisplayName("testConnection should fail gracefully when remote DB is offline without throwing uncaught exceptions")
    void testOfflineDbGracefulFailure() {
        // Point to an unreachable mock IP on port 3306
        DBConnection.setConfiguration("192.0.2.1", 3306, "mock_db", "mock_user", "mock_pass", "mysql");
        boolean connected = DBConnection.testConnection();
        assertFalse(connected, "Connection to mock IP should return false gracefully");
    }

    @Test
    @DisplayName("PlacementDAO should handle offline DB gracefully without throwing unhandled exceptions")
    void testPlacementDaoOfflineHandling() {
        DBConnection.setConfiguration("192.0.2.1", 3306, "mock_db", "mock_user", "mock_pass", "mysql");
        PlacementDAO dao = new PlacementDAO();

        assertFalse(dao.initializeTablesIfNotExist());
        assertEquals(0, dao.saveOrUpdateStudentsBatch(null));
        assertTrue(dao.getAllStudentsWithAssessments().isEmpty());
        assertNull(dao.getStudentByRegisterNo("24ITR001"));
    }
}
