package com.kec.placement.model;

/**
 * RecruitmentTier encapsulates category rules, weight distributions,
 * subject threshold cutoffs, and aggregate eligibility minimums.
 * 
 * Functional Specifications:
 * - TIER-1 PRODUCT:
 *     Coding = 50% | Aptitude = 30% | Soft Skills = 20%
 *     Coding Minimum Threshold = 60% | Aggregate Minimum = 70%
 * - CORE ENGINEERING:
 *     Technical = 40% | Aptitude = 40% | Soft Skills = 20%
 *     Technical Minimum Threshold = 50% | Aggregate Minimum = 60%
 * - MASS RECRUITER:
 *     Aptitude = 50% | Soft Skills = 30% | Technical = 20%
 *     Aptitude Minimum Threshold = 40% | Aggregate Minimum = 50%
 */
public enum RecruitmentTier {
    TIER_1_PRODUCT(
            "TIER-1 PRODUCT",
            "Coding",
            0.50, // Coding weight
            0.00, // Technical domain weight
            0.30, // Aptitude weight
            0.20, // Soft Skills weight
            60.0, // Coding minimum
            70.0, // Aggregate minimum
            1.00  // Tier Weight factor for PRI formula
    ),
    CORE_ENGINEERING(
            "CORE ENGINEERING",
            "Technical",
            0.00, // Coding weight
            0.40, // Technical domain weight
            0.40, // Aptitude weight
            0.20, // Soft Skills weight
            50.0, // Technical minimum
            60.0, // Aggregate minimum
            0.80  // Tier Weight factor for PRI formula
    ),
    MASS_RECRUITER(
            "MASS RECRUITER",
            "Aptitude",
            0.00, // Coding weight
            0.20, // Technical domain weight
            0.50, // Aptitude weight
            0.30, // Soft Skills weight
            40.0, // Aptitude minimum
            50.0, // Aggregate minimum
            0.60  // Tier Weight factor for PRI formula
    );

    private final String displayName;
    private final String criticalSubjectName;
    private final double codingWeight;
    private final double technicalWeight;
    private final double aptitudeWeight;
    private final double softSkillsWeight;
    private final double minSubjectScore;
    private final double minAggregateScore;
    private final double tierWeight;

    RecruitmentTier(String displayName,
                    String criticalSubjectName,
                    double codingWeight,
                    double technicalWeight,
                    double aptitudeWeight,
                    double softSkillsWeight,
                    double minSubjectScore,
                    double minAggregateScore,
                    double tierWeight) {
        this.displayName = displayName;
        this.criticalSubjectName = criticalSubjectName;
        this.codingWeight = codingWeight;
        this.technicalWeight = technicalWeight;
        this.aptitudeWeight = aptitudeWeight;
        this.softSkillsWeight = softSkillsWeight;
        this.minSubjectScore = minSubjectScore;
        this.minAggregateScore = minAggregateScore;
        this.tierWeight = tierWeight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCriticalSubjectName() {
        return criticalSubjectName;
    }

    public double getCodingWeight() {
        return codingWeight;
    }

    public double getTechnicalWeight() {
        return technicalWeight;
    }

    public double getAptitudeWeight() {
        return aptitudeWeight;
    }

    public double getSoftSkillsWeight() {
        return softSkillsWeight;
    }

    public double getMinSubjectScore() {
        return minSubjectScore;
    }

    public double getMinAggregateScore() {
        return minAggregateScore;
    }

    public double getTierWeight() {
        return tierWeight;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
