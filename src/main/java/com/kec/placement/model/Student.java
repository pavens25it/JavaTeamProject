package com.kec.placement.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Student represents a candidate participating in campus recruitment.
 * 
 * Design Principles:
 * - Backlog tracking is managed strictly within Assessment to prevent duplication.
 * - Skills are kept as raw text and can be processed into parsed keyword tokens.
 * - Assessment holds phased scores and verified backlogs.
 */
public class Student {

    private Integer studentId;
    private String registerNo;
    private String fullName;
    private String department;
    private String email;
    private Double cgpa;
    private String rawSkills;
    private List<String> skills = new ArrayList<>();
    private Assessment assessment;

    public Student() {
    }

    public Student(String registerNo, String fullName, String department, Double cgpa) {
        setRegisterNo(registerNo);
        this.fullName = fullName;
        this.department = department;
        setCgpa(cgpa);
    }

    public Student(String registerNo, String fullName, String department, Double cgpa, String rawSkills) {
        this(registerNo, fullName, department, cgpa);
        this.rawSkills = rawSkills;
    }

    public Student(Integer studentId, String registerNo, String fullName, String department,
                   String email, Double cgpa, String rawSkills) {
        this.studentId = studentId;
        setRegisterNo(registerNo);
        this.fullName = fullName;
        this.department = department;
        this.email = email;
        setCgpa(cgpa);
        this.rawSkills = rawSkills;
    }

    // =========================================================================
    // Validation Helpers
    // =========================================================================

    public static void validateCgpa(Double cgpa) {
        if (cgpa != null && (cgpa < 0.0 || cgpa > 10.0)) {
            throw new IllegalArgumentException("CGPA must be between 0.0 and 10.0. Received: " + cgpa);
        }
    }

    // =========================================================================
    // Convenience Accessors
    // =========================================================================

    /**
     * Retrieves active backlogs from the associated Assessment record.
     * Guaranteed single source of truth: no duplicate backlog field in Student.
     *
     * @return active backlogs count or null if not yet assessed
     */
    public Integer getActiveBacklogs() {
        return assessment != null ? assessment.getActiveBacklogs() : null;
    }

    public void addSkill(String skill) {
        if (skill != null && !skill.trim().isEmpty()) {
            this.skills.add(skill.trim());
        }
    }

    // =========================================================================
    // Getters and Setters
    // =========================================================================

    public Integer getStudentId() {
        return studentId;
    }

    public void setStudentId(Integer studentId) {
        this.studentId = studentId;
    }

    public String getRegisterNo() {
        return registerNo;
    }

    public void setRegisterNo(String registerNo) {
        if (registerNo == null || registerNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Register number cannot be null or empty.");
        }
        this.registerNo = registerNo.trim();
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Double getCgpa() {
        return cgpa;
    }

    public void setCgpa(Double cgpa) {
        validateCgpa(cgpa);
        this.cgpa = cgpa;
    }

    public String getRawSkills() {
        return rawSkills;
    }

    public void setRawSkills(String rawSkills) {
        this.rawSkills = rawSkills;
    }

    public List<String> getSkills() {
        return Collections.unmodifiableList(skills);
    }

    public void setSkills(List<String> skills) {
        this.skills = (skills != null) ? new ArrayList<>(skills) : new ArrayList<>();
    }

    public Assessment getAssessment() {
        return assessment;
    }

    public void setAssessment(Assessment assessment) {
        this.assessment = assessment;
    }

    @Override
    public String toString() {
        return String.format("Student[regNo=%s, name=%s, dept=%s, cgpa=%.2f, backlogs=%s]",
                registerNo,
                fullName,
                department,
                cgpa != null ? cgpa : 0.0,
                getActiveBacklogs() != null ? getActiveBacklogs() : "PENDING");
    }
}
