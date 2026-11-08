package com.kec.placement.service;

import com.kec.placement.model.Assessment;
import com.kec.placement.model.EligibilityResult;
import com.kec.placement.model.PlacementBand;
import com.kec.placement.model.RecruitmentTier;
import com.kec.placement.model.Student;

import java.util.ArrayList;
import java.util.List;

/**
 * EligibilityEngine encapsulates all candidate screening, cutoff evaluation,
 * weighted aggregate calculations, and band/rank assignments.
 * 
 * Functional Specifications:
 * -------------------------------------------------------------------------------------------------
 * Tier                 Weights                                 Subject Threshold    Aggregate Min
 * -------------------------------------------------------------------------------------------------
 * TIER-1 PRODUCT       Coding 50%, Aptitude 30%, Soft 20%      Coding >= 60.0       Aggregate >= 70.0
 * CORE ENGINEERING     Technical 40%, Aptitude 40%, Soft 20%   Technical >= 50.0    Aggregate >= 60.0
 * MASS RECRUITER       Aptitude 50%, Soft 30%, Technical 20%   Aptitude >= 40.0     Aggregate >= 50.0
 * -------------------------------------------------------------------------------------------------
 * 
 * Attendance, Rating & Grading Rules:
 * - Backlog Filter: Active Backlogs > 2 -> NE (Not Eligible / Barred from drives, Rank F).
 * - Bands:
 *     - Super Dream: 85.0 - 100.0 Aggregate -> Rank A
 *     - Dream:       70.0 -  84.99 Aggregate -> Rank B
 *     - Regular:     50.0 -  69.99 Aggregate -> Rank C
 *     - Ineligible:  Below aggregate cutoff or barred -> Rank F
 * - Incomplete Records: Missing/null assessment scores are rejected with NE status.
 * - PRI: Kept separate pending formal clarification from faculty / evaluation guidelines.
 */
public class EligibilityEngine {

    /** Maximum permitted active backlogs before a candidate is barred */
    public static final int MAX_PERMISSIBLE_BACKLOGS = 2;

    /**
     * Evaluates a candidate student against a specific recruitment category tier.
     * Rejects incomplete assessment records.
     *
     * @param student Candidate to screen (must have associated Assessment)
     * @param tier Recruitment category criteria to apply
     * @return Fully populated EligibilityResult record
     */
    public EligibilityResult evaluate(Student student, RecruitmentTier tier) {
        if (student == null) {
            throw new IllegalArgumentException("Student record cannot be null for evaluation.");
        }
        if (tier == null) {
            throw new IllegalArgumentException("RecruitmentTier cannot be null for evaluation.");
        }

        Assessment assessment = student.getAssessment();
        EligibilityResult result = new EligibilityResult(student, tier);

        // Check if assessment is available
        if (assessment == null) {
            result.setIsBacklogBarred(false);
            result.setPassedSubjectCutoff(false);
            result.setPassedAggregateCutoff(false);
            result.setEligible(false);
            result.setEligibilityStatus("NE");
            result.setPlacementBand(PlacementBand.INELIGIBLE);
            result.setPlacementRank("F");
            result.setAggregateScore(0.0);
            result.setCriticalSubjectScore(0.0);
            result.setPriScore(null);
            result.setRemarks("NE: Assessment record missing or not started.");
            return result;
        }

        // Validate presence of required phased scores (Reject incomplete assessment records)
        Integer backlogs = assessment.getActiveBacklogs();
        Integer apt = assessment.getAptitudeScore();
        Integer coding = assessment.getCodingScore();
        Integer tech = assessment.getTechnicalScore();
        Integer soft = assessment.getSoftSkillsScore();

        boolean incomplete = (backlogs == null || apt == null || soft == null
                || (tier == RecruitmentTier.TIER_1_PRODUCT && coding == null)
                || ((tier == RecruitmentTier.CORE_ENGINEERING || tier == RecruitmentTier.MASS_RECRUITER) && tech == null));

        if (incomplete) {
            result.setIsBacklogBarred(backlogs != null && backlogs > MAX_PERMISSIBLE_BACKLOGS);
            result.setPassedSubjectCutoff(false);
            result.setPassedAggregateCutoff(false);
            result.setEligible(false);
            result.setEligibilityStatus("NE");
            result.setPlacementBand(PlacementBand.INELIGIBLE);
            result.setPlacementRank("F");
            result.setAggregateScore(0.0);
            result.setCriticalSubjectScore(0.0);
            result.setPriScore(null);
            result.setRemarks("NE: Incomplete assessment phases for tier " + tier.getDisplayName());
            return result;
        }

        // 1. Backlog Evaluation Rule (> 2 active backlogs = NE / Barred)
        boolean isBacklogBarred = (backlogs > MAX_PERMISSIBLE_BACKLOGS);
        result.setIsBacklogBarred(isBacklogBarred);

        // 2. Critical Subject Score Evaluation
        double criticalSubjectScore;
        switch (tier) {
            case TIER_1_PRODUCT:
                criticalSubjectScore = coding.doubleValue();
                break;
            case CORE_ENGINEERING:
                criticalSubjectScore = tech.doubleValue();
                break;
            case MASS_RECRUITER:
                criticalSubjectScore = apt.doubleValue();
                break;
            default:
                criticalSubjectScore = 0.0;
        }
        result.setCriticalSubjectScore(criticalSubjectScore);
        boolean passedSubjectCutoff = (criticalSubjectScore >= tier.getMinSubjectScore());
        result.setPassedSubjectCutoff(passedSubjectCutoff);

        // 3. Weighted Aggregate Score Calculation
        double rawAggregate = calculateAggregate(assessment, tier);
        double aggregateScore = roundToTwoDecimals(rawAggregate);
        result.setAggregateScore(aggregateScore);
        boolean passedAggregateCutoff = (aggregateScore >= tier.getMinAggregateScore());
        result.setPassedAggregateCutoff(passedAggregateCutoff);

        // 4. Overall Eligibility Determination (Must pass backlogs <= 2, subject min, and aggregate min)
        boolean isEligible = (!isBacklogBarred) && passedSubjectCutoff && passedAggregateCutoff;
        result.setEligible(isEligible);
        result.setEligibilityStatus(isEligible ? "ELIGIBLE" : "NE");

        // 5. Placement Band & Rank Assignment
        PlacementBand band = determineBand(aggregateScore, isEligible);
        result.setPlacementBand(band);
        result.setPlacementRank(band.getRank());

        // 6. Placement Readiness Index (PRI)
        // Kept separate and not forced onto screening decisions pending clarification
        result.setPriScore(null);

        // 7. Clear Explanatory Remarks
        String remarks = buildRemarks(isBacklogBarred, backlogs, passedSubjectCutoff,
                criticalSubjectScore, tier, passedAggregateCutoff, aggregateScore, isEligible, band);
        result.setRemarks(remarks);

        return result;
    }

    /**
     * Batch evaluates a list of candidates against a single recruitment tier.
     */
    public List<EligibilityResult> evaluateAll(List<Student> students, RecruitmentTier tier) {
        List<EligibilityResult> results = new ArrayList<>();
        if (students == null || students.isEmpty()) {
            return results;
        }
        for (Student student : students) {
            results.add(evaluate(student, tier));
        }
        return results;
    }

    /**
     * Calculates the category-specific weighted aggregate score:
     * - Tier-1 Product:   Coding * 0.50 + Aptitude * 0.30 + SoftSkills * 0.20
     * - Core Engineering: Technical * 0.40 + Aptitude * 0.40 + SoftSkills * 0.20
     * - Mass Recruiter:   Aptitude * 0.50 + SoftSkills * 0.30 + Technical * 0.20
     */
    public double calculateAggregate(Assessment assessment, RecruitmentTier tier) {
        if (assessment == null || tier == null) {
            return 0.0;
        }

        double apt = (assessment.getAptitudeScore() != null) ? assessment.getAptitudeScore() : 0.0;
        double coding = (assessment.getCodingScore() != null) ? assessment.getCodingScore() : 0.0;
        double tech = (assessment.getTechnicalScore() != null) ? assessment.getTechnicalScore() : 0.0;
        double soft = (assessment.getSoftSkillsScore() != null) ? assessment.getSoftSkillsScore() : 0.0;

        return (coding * tier.getCodingWeight())
             + (tech * tier.getTechnicalWeight())
             + (apt * tier.getAptitudeWeight())
             + (soft * tier.getSoftSkillsWeight());
    }

    /**
     * Determines placement band from aggregate score and eligibility status.
     * If not eligible (barred or failed cutoffs), Rank F / Ineligible is assigned.
     */
    public PlacementBand determineBand(double aggregateScore, boolean isEligible) {
        if (!isEligible) {
            return PlacementBand.INELIGIBLE;
        }
        if (aggregateScore >= 85.0) {
            return PlacementBand.SUPER_DREAM;
        } else if (aggregateScore >= 70.0) {
            return PlacementBand.DREAM;
        } else if (aggregateScore >= 50.0) {
            return PlacementBand.REGULAR;
        } else {
            return PlacementBand.INELIGIBLE;
        }
    }

    /**
     * Optional Standalone PRI Calculator (kept separate pending formula clarification).
     */
    public Double computePriOptional(double aggregateScore, RecruitmentTier tier, int totalEnrolledCandidates) {
        if (totalEnrolledCandidates <= 0 || tier == null) {
            return null;
        }
        double pri = (aggregateScore * tier.getTierWeight()) / totalEnrolledCandidates;
        return roundToFourDecimals(pri);
    }

    private String buildRemarks(boolean isBacklogBarred, int backlogs,
                                boolean passedSubjectCutoff, double criticalSubjectScore,
                                RecruitmentTier tier,
                                boolean passedAggregateCutoff, double aggregateScore,
                                boolean isEligible, PlacementBand band) {
        if (isBacklogBarred) {
            return String.format("NE: Barred due to active backlogs (%d > %d)",
                    backlogs, MAX_PERMISSIBLE_BACKLOGS);
        }
        if (!passedSubjectCutoff) {
            return String.format("NE: %s score (%.1f) below subject cutoff (%.1f)",
                    tier.getCriticalSubjectName(), criticalSubjectScore, tier.getMinSubjectScore());
        }
        if (!passedAggregateCutoff) {
            return String.format("NE: Aggregate score (%.2f) below tier cutoff (%.1f)",
                    aggregateScore, tier.getMinAggregateScore());
        }
        if (isEligible) {
            return String.format("ELIGIBLE: Qualified for %s (%s)", tier.getDisplayName(), band.getBandName());
        }
        return "NE: Disqualified";
    }

    public static double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public static double roundToFourDecimals(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}
