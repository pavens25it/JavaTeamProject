package com.kec.placement.model;

/**
 * Assessment model representing candidate performance across phased recruitment evaluations.
 * 
 * Phased Incremental Data Entry:
 * - Phase 1: Record Aptitude Assessment Scores (0–100).
 * - Phase 2: Record Technical Coding and Technical / Domain Test Scores (0–100).
 * - Phase 3: Record HR Interview & Soft Skills Evaluation (0–100) and Active Backlog Count.
 * 
 * Design Principles:
 * - Coding and Technical/Domain scores are maintained as distinct fields.
 * - Incomplete phase scores are stored as nullable Integer fields (null = pending assessment, 0 = scored zero).
 * - Active backlogs are recorded at Phase 3 evaluation and stored solely in this model to avoid duplication.
 */
public class Assessment {

    private Integer assessmentId;
    private String registerNo;

    // Phase 1 Assessment
    private Integer aptitudeScore;      // Phase 1: 0 - 100 (null if pending)

    // Phase 2 Assessments (Coding and Technical/Domain kept separate)
    private Integer codingScore;        // Phase 2: Coding score 0 - 100 (null if pending)
    private Integer technicalScore;     // Phase 2: Technical/Domain score 0 - 100 (null if pending)

    // Phase 3 Assessments
    private Integer softSkillsScore;    // Phase 3: Soft Skills/HR 0 - 100 (null if pending)
    private Integer activeBacklogs;     // Phase 3: Active backlogs count at verification (null if pending)

    /**
     * Default no-arg constructor. All score fields default to null (incomplete/pending).
     */
    public Assessment() {
    }

    /**
     * Minimal constructor with registration number.
     *
     * @param registerNo Candidate registration number (e.g. 24ITR001)
     */
    public Assessment(String registerNo) {
        this.registerNo = registerNo;
    }

    /**
     * Full constructor for fully evaluated candidate.
     */
    public Assessment(String registerNo,
                      Integer aptitudeScore,
                      Integer codingScore,
                      Integer technicalScore,
                      Integer softSkillsScore,
                      Integer activeBacklogs) {
        this.registerNo = registerNo;
        setAptitudeScore(aptitudeScore);
        setCodingScore(codingScore);
        setTechnicalScore(technicalScore);
        setSoftSkillsScore(softSkillsScore);
        setActiveBacklogs(activeBacklogs);
    }

    // =========================================================================
    // Phased Data Entry Methods
    // =========================================================================

    /**
     * Phase 1: Record Aptitude Assessment Score.
     *
     * @param score Aptitude score (0 - 100)
     */
    public void recordPhase1(Integer score) {
        validateScore(score, "Phase 1 Aptitude");
        this.aptitudeScore = score;
    }

    /**
     * Phase 2: Record Coding and Technical/Domain Assessment Scores.
     *
     * @param coding Coding assessment score (0 - 100)
     * @param technical Technical/Domain assessment score (0 - 100)
     */
    public void recordPhase2(Integer coding, Integer technical) {
        validateScore(coding, "Phase 2 Coding");
        validateScore(technical, "Phase 2 Technical");
        this.codingScore = coding;
        this.technicalScore = technical;
    }

    /**
     * Phase 3: Record Soft Skills / HR Score and verified Active Backlogs count.
     *
     * @param softSkills Soft skills score (0 - 100)
     * @param backlogs Verified active backlogs count (>= 0)
     */
    public void recordPhase3(Integer softSkills, Integer backlogs) {
        validateScore(softSkills, "Phase 3 Soft Skills");
        validateBacklogs(backlogs);
        this.softSkillsScore = softSkills;
        this.activeBacklogs = backlogs;
    }

    // =========================================================================
    // Validation Helpers
    // =========================================================================

    /**
     * Validates that score is within legal range [0, 100] when present.
     * Null represents an incomplete or pending score and is permitted.
     */
    public static void validateScore(Integer score, String fieldName) {
        if (score != null && (score < 0 || score > 100)) {
            throw new IllegalArgumentException(fieldName + " score must be between 0 and 100. Received: " + score);
        }
    }

    /**
     * Validates that active backlogs count is non-negative when present.
     */
    public static void validateBacklogs(Integer backlogs) {
        if (backlogs != null && backlogs < 0) {
            throw new IllegalArgumentException("Active backlogs count cannot be negative. Received: " + backlogs);
        }
    }

    // =========================================================================
    // Phase Completion Checks
    // =========================================================================

    public boolean isPhase1Complete() {
        return aptitudeScore != null;
    }

    public boolean isPhase2Complete() {
        return codingScore != null && technicalScore != null;
    }

    public boolean isPhase3Complete() {
        return softSkillsScore != null && activeBacklogs != null;
    }

    public boolean isFullyAssessed() {
        return isPhase1Complete() && isPhase2Complete() && isPhase3Complete();
    }

    // =========================================================================
    // Getters and Setters
    // =========================================================================

    public Integer getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(Integer assessmentId) {
        this.assessmentId = assessmentId;
    }

    public String getRegisterNo() {
        return registerNo;
    }

    public void setRegisterNo(String registerNo) {
        this.registerNo = registerNo;
    }

    public Integer getAptitudeScore() {
        return aptitudeScore;
    }

    public void setAptitudeScore(Integer aptitudeScore) {
        validateScore(aptitudeScore, "Aptitude");
        this.aptitudeScore = aptitudeScore;
    }

    public Integer getCodingScore() {
        return codingScore;
    }

    public void setCodingScore(Integer codingScore) {
        validateScore(codingScore, "Coding");
        this.codingScore = codingScore;
    }

    public Integer getTechnicalScore() {
        return technicalScore;
    }

    public void setTechnicalScore(Integer technicalScore) {
        validateScore(technicalScore, "Technical");
        this.technicalScore = technicalScore;
    }

    public Integer getSoftSkillsScore() {
        return softSkillsScore;
    }

    public void setSoftSkillsScore(Integer softSkillsScore) {
        validateScore(softSkillsScore, "Soft Skills");
        this.softSkillsScore = softSkillsScore;
    }

    public Integer getActiveBacklogs() {
        return activeBacklogs;
    }

    public void setActiveBacklogs(Integer activeBacklogs) {
        validateBacklogs(activeBacklogs);
        this.activeBacklogs = activeBacklogs;
    }

    @Override
    public String toString() {
        return String.format("Assessment[regNo=%s, Apt=%s, Coding=%s, Tech=%s, Soft=%s, Backlogs=%s]",
                registerNo,
                aptitudeScore != null ? aptitudeScore : "PENDING",
                codingScore != null ? codingScore : "PENDING",
                technicalScore != null ? technicalScore : "PENDING",
                softSkillsScore != null ? softSkillsScore : "PENDING",
                activeBacklogs != null ? activeBacklogs : "PENDING");
    }
}
