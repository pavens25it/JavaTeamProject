package com.kec.placement.model;

/**
 * EligibilityResult encapsulates the screening outcome for a candidate
 * against a specific RecruitmentTier.
 * 
 * Rules:
 * - Backlog Filter: Active Backlogs > 2 flags candidate as NE (Not Eligible / Barred).
 * - Cutoff Filters: Must meet specific critical subject cutoff and aggregate minimum.
 * - Placement Bands & Ranks:
 *     - Super Dream: 85 - 100 Aggregate -> Rank A
 *     - Dream:       70 - 84  Aggregate -> Rank B
 *     - Regular:     50 - 69  Aggregate -> Rank C
 *     - Ineligible:  Below aggregate minimum or Backlog Barred -> Rank F
 */
public class EligibilityResult {

    private Student student;
    private RecruitmentTier tier;

    // Numerical scores
    private Double aggregateScore;          // Calculated weighted percentage
    private Double criticalSubjectScore;    // The subject tested against subject cutoff
    private Double priScore;                // Placement Readiness Index

    // Screening decision flags
    private Boolean isBacklogBarred;        // True if active backlogs > 2
    private Boolean passedSubjectCutoff;    // True if critical subject >= tier min
    private Boolean passedAggregateCutoff;  // True if aggregate >= tier min
    private boolean isEligible;             // True only if all criteria met

    // Placement classification
    private String eligibilityStatus;       // "ELIGIBLE" or "NE"
    private PlacementBand placementBand;    // SUPER_DREAM, DREAM, REGULAR, INELIGIBLE
    private String placementRank;           // "A", "B", "C", "F"
    private String remarks;                 // Explanatory comment or reason for disqualification

    public EligibilityResult() {
    }

    public EligibilityResult(Student student, RecruitmentTier tier) {
        this.student = student;
        this.tier = tier;
    }

    public EligibilityResult(Student student,
                             RecruitmentTier tier,
                             Double aggregateScore,
                             Double criticalSubjectScore,
                             Boolean isBacklogBarred,
                             Boolean passedSubjectCutoff,
                             Boolean passedAggregateCutoff,
                             boolean isEligible,
                             String eligibilityStatus,
                             PlacementBand placementBand,
                             String placementRank,
                             Double priScore,
                             String remarks) {
        this.student = student;
        this.tier = tier;
        this.aggregateScore = aggregateScore;
        this.criticalSubjectScore = criticalSubjectScore;
        this.isBacklogBarred = isBacklogBarred;
        this.passedSubjectCutoff = passedSubjectCutoff;
        this.passedAggregateCutoff = passedAggregateCutoff;
        this.isEligible = isEligible;
        this.eligibilityStatus = eligibilityStatus;
        this.placementBand = placementBand;
        this.placementRank = placementRank;
        this.priScore = priScore;
        this.remarks = remarks;
    }

    // =========================================================================
    // Getters and Setters
    // =========================================================================

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public RecruitmentTier getTier() {
        return tier;
    }

    public void setTier(RecruitmentTier tier) {
        this.tier = tier;
    }

    public Double getAggregateScore() {
        return aggregateScore;
    }

    public void setAggregateScore(Double aggregateScore) {
        this.aggregateScore = aggregateScore;
    }

    public Double getCriticalSubjectScore() {
        return criticalSubjectScore;
    }

    public void setCriticalSubjectScore(Double criticalSubjectScore) {
        this.criticalSubjectScore = criticalSubjectScore;
    }

    public Double getPriScore() {
        return priScore;
    }

    public void setPriScore(Double priScore) {
        this.priScore = priScore;
    }

    public Boolean getIsBacklogBarred() {
        return isBacklogBarred;
    }

    public void setIsBacklogBarred(Boolean isBacklogBarred) {
        this.isBacklogBarred = isBacklogBarred;
    }

    public Boolean getPassedSubjectCutoff() {
        return passedSubjectCutoff;
    }

    public void setPassedSubjectCutoff(Boolean passedSubjectCutoff) {
        this.passedSubjectCutoff = passedSubjectCutoff;
    }

    public Boolean getPassedAggregateCutoff() {
        return passedAggregateCutoff;
    }

    public void setPassedAggregateCutoff(Boolean passedAggregateCutoff) {
        this.passedAggregateCutoff = passedAggregateCutoff;
    }

    public boolean isEligible() {
        return isEligible;
    }

    public void setEligible(boolean eligible) {
        isEligible = eligible;
    }

    public String getEligibilityStatus() {
        return eligibilityStatus;
    }

    public void setEligibilityStatus(String eligibilityStatus) {
        this.eligibilityStatus = eligibilityStatus;
    }

    public PlacementBand getPlacementBand() {
        return placementBand;
    }

    public void setPlacementBand(PlacementBand placementBand) {
        this.placementBand = placementBand;
    }

    public String getPlacementRank() {
        return placementRank;
    }

    public void setPlacementRank(String placementRank) {
        this.placementRank = placementRank;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    @Override
    public String toString() {
        return String.format(
                "EligibilityResult[regNo=%s, tier=%s, agg=%.2f, status=%s, band=%s, rank=%s, remarks=%s]",
                student != null ? student.getRegisterNo() : "UNKNOWN",
                tier != null ? tier.getDisplayName() : "UNKNOWN",
                aggregateScore != null ? aggregateScore : 0.0,
                eligibilityStatus,
                placementBand != null ? placementBand.getBandName() : "N/A",
                placementRank != null ? placementRank : "N/A",
                remarks != null ? remarks : "OK");
    }
}
