package com.kec.placement;

import com.kec.placement.model.Assessment;
import com.kec.placement.model.EligibilityResult;
import com.kec.placement.model.PlacementBand;
import com.kec.placement.model.RecruitmentTier;
import com.kec.placement.model.Student;
import com.kec.placement.service.StringSanitizer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PlacementDAO handles all JDBC database queries and persistence for the Application Host.
 * 
 * Target Environment:
 * - Hosted on OCI VM 2 by Student 2 (Database Host).
 * - Queries adapt to Student 2's schema contract.
 * - Handles offline database states gracefully without crashing the application host.
 */
public class PlacementDAO {

    /**
     * Initializes the agreed schema tables if they do not exist yet.
     */
    public boolean initializeTablesIfNotExist() {
        String idColumn = "postgresql".equalsIgnoreCase(DBConnection.getDbType())
                ? "SERIAL PRIMARY KEY"
                : "INT AUTO_INCREMENT PRIMARY KEY";
        String createStudents = "CREATE TABLE IF NOT EXISTS students ("
                + "student_id " + idColumn + ", "
                + "register_no VARCHAR(20) NOT NULL UNIQUE, "
                + "full_name VARCHAR(100) NOT NULL, "
                + "department VARCHAR(50) NOT NULL, "
                + "email VARCHAR(100), "
                + "cgpa DECIMAL(4, 2) NOT NULL, "
                + "skills_raw TEXT, "
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ")";

        String createAssessments = "CREATE TABLE IF NOT EXISTS assessments ("
                + "assessment_id " + idColumn + ", "
                + "register_no VARCHAR(20) NOT NULL UNIQUE, "
                + "phase1_aptitude INT, "
                + "phase2_coding INT, "
                + "phase2_technical INT, "
                + "phase3_soft_skills INT, "
                + "active_backlogs INT, "
                + "assessment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ")";

        String createResults = "CREATE TABLE IF NOT EXISTS eligibility_results ("
                + "result_id " + idColumn + ", "
                + "register_no VARCHAR(20) NOT NULL, "
                + "tier_category VARCHAR(50) NOT NULL, "
                + "aggregate_score DECIMAL(5, 2) NOT NULL, "
                + "critical_score DECIMAL(5, 2) NOT NULL, "
                + "placement_band VARCHAR(30) NOT NULL, "
                + "placement_rank VARCHAR(5) NOT NULL, "
                + "is_eligible BOOLEAN NOT NULL, "
                + "eligibility_status VARCHAR(20) NOT NULL, "
                + "pri_score DECIMAL(7, 4), "
                + "remarks TEXT, "
                + "screened_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ")";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(createStudents);
            stmt.execute(createAssessments);
            stmt.execute(createResults);
            System.out.println("[PlacementDAO] Verified schema tables on Student 2's database.");
            return true;
        } catch (SQLException e) {
            System.err.println("[PlacementDAO Warning] Table initialization skipped: " + e.getMessage());
            return false;
        }
    }

    /**
     * Inserts or updates a single student profile.
     */
    public boolean saveOrUpdateStudent(Student s) {
        if (s == null) return false;
        return saveOrUpdateStudentsBatch(List.of(s)) > 0;
    }

    /**
     * Batch inserts or updates candidate student profiles.
     */
    public int saveOrUpdateStudentsBatch(List<Student> students) {
        if (students == null || students.isEmpty()) {
            return 0;
        }

        String sql;
        if ("postgresql".equalsIgnoreCase(DBConnection.getDbType())) {
            sql = "INSERT INTO students (register_no, full_name, department, email, cgpa, skills_raw) "
                + "VALUES (?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT (register_no) DO UPDATE SET full_name=EXCLUDED.full_name, "
                + "department=EXCLUDED.department, email=EXCLUDED.email, cgpa=EXCLUDED.cgpa, skills_raw=EXCLUDED.skills_raw";
        } else {
            sql = "INSERT INTO students (register_no, full_name, department, email, cgpa, skills_raw) "
                + "VALUES (?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE full_name=VALUES(full_name), department=VALUES(department), "
                + "email=VALUES(email), cgpa=VALUES(cgpa), skills_raw=VALUES(skills_raw)";
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            for (Student s : students) {
                ps.setString(1, s.getRegisterNo());
                ps.setString(2, s.getFullName());
                ps.setString(3, s.getDepartment());
                ps.setString(4, s.getEmail() != null ? s.getEmail() : (s.getRegisterNo().toLowerCase() + "@kongu.edu"));
                ps.setDouble(5, s.getCgpa() != null ? s.getCgpa() : 0.0);
                ps.setString(6, s.getRawSkills());
                ps.addBatch();
            }

            int[] counts = ps.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
            return counts.length;
        } catch (SQLException e) {
            System.err.println("[PlacementDAO Error] Failed to save students batch: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Inserts or updates a single phased assessment.
     */
    public boolean saveOrUpdateAssessment(Assessment a) {
        if (a == null) return false;
        return saveOrUpdateAssessmentsBatch(List.of(a)) > 0;
    }

    /**
     * Batch inserts or updates phased assessments.
     */
    public int saveOrUpdateAssessmentsBatch(List<Assessment> assessments) {
        if (assessments == null || assessments.isEmpty()) {
            return 0;
        }

        String sql;
        if ("postgresql".equalsIgnoreCase(DBConnection.getDbType())) {
            sql = "INSERT INTO assessments (register_no, phase1_aptitude, phase2_coding, phase2_technical, phase3_soft_skills, active_backlogs) "
                + "VALUES (?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT (register_no) DO UPDATE SET phase1_aptitude=EXCLUDED.phase1_aptitude, "
                + "phase2_coding=EXCLUDED.phase2_coding, phase2_technical=EXCLUDED.phase2_technical, "
                + "phase3_soft_skills=EXCLUDED.phase3_soft_skills, active_backlogs=EXCLUDED.active_backlogs";
        } else {
            sql = "INSERT INTO assessments (register_no, phase1_aptitude, phase2_coding, phase2_technical, phase3_soft_skills, active_backlogs) "
                + "VALUES (?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE phase1_aptitude=VALUES(phase1_aptitude), phase2_coding=VALUES(phase2_coding), "
                + "phase2_technical=VALUES(phase2_technical), phase3_soft_skills=VALUES(phase3_soft_skills), active_backlogs=VALUES(active_backlogs)";
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            for (Assessment a : assessments) {
                ps.setString(1, a.getRegisterNo());
                setNullableInt(ps, 2, a.getAptitudeScore());
                setNullableInt(ps, 3, a.getCodingScore());
                setNullableInt(ps, 4, a.getTechnicalScore());
                setNullableInt(ps, 5, a.getSoftSkillsScore());
                setNullableInt(ps, 6, a.getActiveBacklogs());
                ps.addBatch();
            }

            int[] counts = ps.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
            return counts.length;
        } catch (SQLException e) {
            System.err.println("[PlacementDAO Error] Failed to save assessments batch: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Saves calculated candidate screening results.
     */
    public int saveEligibilityResultsBatch(List<EligibilityResult> results) {
        if (results == null || results.isEmpty()) {
            return 0;
        }

        String sql;
        if ("postgresql".equalsIgnoreCase(DBConnection.getDbType())) {
            sql = "INSERT INTO eligibility_results "
                + "(register_no, tier_category, aggregate_score, critical_score, placement_band, "
                + "placement_rank, is_eligible, eligibility_status, pri_score, remarks) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON CONFLICT (register_no, tier_category) DO UPDATE SET "
                + "aggregate_score=EXCLUDED.aggregate_score, "
                + "critical_score=EXCLUDED.critical_score, "
                + "placement_band=EXCLUDED.placement_band, "
                + "placement_rank=EXCLUDED.placement_rank, "
                + "is_eligible=EXCLUDED.is_eligible, "
                + "eligibility_status=EXCLUDED.eligibility_status, "
                + "pri_score=EXCLUDED.pri_score, "
                + "remarks=EXCLUDED.remarks, "
                + "screened_at=CURRENT_TIMESTAMP";
        } else {
            sql = "INSERT INTO eligibility_results "
                + "(register_no, tier_category, aggregate_score, critical_score, placement_band, "
                + "placement_rank, is_eligible, eligibility_status, pri_score, remarks) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE "
                + "aggregate_score=VALUES(aggregate_score), "
                + "critical_score=VALUES(critical_score), "
                + "placement_band=VALUES(placement_band), "
                + "placement_rank=VALUES(placement_rank), "
                + "is_eligible=VALUES(is_eligible), "
                + "eligibility_status=VALUES(eligibility_status), "
                + "pri_score=VALUES(pri_score), "
                + "remarks=VALUES(remarks), "
                + "screened_at=CURRENT_TIMESTAMP";
        }
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            conn.setAutoCommit(false);
            for (EligibilityResult r : results) {
                Student s = r.getStudent();
                ps.setString(1, s != null ? s.getRegisterNo() : "UNKNOWN");
                ps.setString(2, r.getTier() != null ? r.getTier().name() : "UNKNOWN");
                ps.setDouble(3, r.getAggregateScore() != null ? r.getAggregateScore() : 0.0);
                ps.setDouble(4, r.getCriticalSubjectScore() != null ? r.getCriticalSubjectScore() : 0.0);
                ps.setString(5, r.getPlacementBand() != null ? r.getPlacementBand().getBandName() : "N/A");
                ps.setString(6, r.getPlacementRank() != null ? r.getPlacementRank() : "N/A");
                ps.setBoolean(7, r.isEligible());
                ps.setString(8, r.getEligibilityStatus() != null ? r.getEligibilityStatus() : "NE");

                if (r.getPriScore() != null) {
                    ps.setDouble(9, r.getPriScore());
                } else {
                    ps.setNull(9, Types.DECIMAL);
                }

                ps.setString(10, r.getRemarks());
                ps.addBatch();
            }

            int[] counts = ps.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
            return counts.length;
        } catch (SQLException e) {
            System.err.println("[PlacementDAO Error] Failed to persist eligibility results: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Retrieves all candidates along with their phased assessment data from remote DB.
     */
    public List<Student> getAllStudentsWithAssessments() {
        List<Student> students = new ArrayList<>();
        Map<String, Student> map = new HashMap<>();

        String studentSql = "SELECT register_no, full_name, department, email, cgpa, skills_raw FROM students";
        String assessmentSql = "SELECT register_no, phase1_aptitude, phase2_coding, phase2_technical, phase3_soft_skills, active_backlogs FROM assessments";

        try (Connection conn = DBConnection.getConnection()) {
            // 1. Fetch Students
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(studentSql)) {
                while (rs.next()) {
                    String reg = rs.getString("register_no");
                    String name = rs.getString("full_name");
                    String dept = rs.getString("department");
                    String email = rs.getString("email");
                    double cgpa = rs.getDouble("cgpa");
                    String rawSkills = rs.getString("skills_raw");

                    Student s = new Student(null, reg, name, dept, email, cgpa, rawSkills);
                    s.setSkills(StringSanitizer.parseSkills(rawSkills));
                    map.put(reg, s);
                    students.add(s);
                }
            }

            // 2. Fetch and attach Assessments
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(assessmentSql)) {
                while (rs.next()) {
                    String reg = rs.getString("register_no");
                    Student s = map.get(reg);
                    if (s != null) {
                        Integer apt = getNullableInt(rs, "phase1_aptitude");
                        Integer coding = getNullableInt(rs, "phase2_coding");
                        Integer tech = getNullableInt(rs, "phase2_technical");
                        Integer soft = getNullableInt(rs, "phase3_soft_skills");
                        Integer backlogs = getNullableInt(rs, "active_backlogs");

                        Assessment a = new Assessment(reg, apt, coding, tech, soft, backlogs);
                        s.setAssessment(a);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("[PlacementDAO Error] Failed to retrieve students: " + e.getMessage());
        }

        return students;
    }

    /**
     * Retrieves a single student by register number.
     */
    public Student getStudentByRegisterNo(String registerNo) {
        if (registerNo == null || registerNo.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT register_no, full_name, department, email, cgpa, skills_raw FROM students WHERE register_no = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, registerNo.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Student s = new Student(
                            null,
                            rs.getString("register_no"),
                            rs.getString("full_name"),
                            rs.getString("department"),
                            rs.getString("email"),
                            rs.getDouble("cgpa"),
                            rs.getString("skills_raw")
                    );
                    s.setSkills(StringSanitizer.parseSkills(s.getRawSkills()));
                    return s;
                }
            }
        } catch (SQLException e) {
            System.err.println("[PlacementDAO Error] Failed to find student: " + e.getMessage());
        }
        return null;
    }

    private void setNullableInt(PreparedStatement ps, int paramIndex, Integer value) throws SQLException {
        if (value != null) {
            ps.setInt(paramIndex, value);
        } else {
            ps.setNull(paramIndex, Types.INTEGER);
        }
    }

    private Integer getNullableInt(ResultSet rs, String colName) throws SQLException {
        int val = rs.getInt(colName);
        return rs.wasNull() ? null : val;
    }
}
