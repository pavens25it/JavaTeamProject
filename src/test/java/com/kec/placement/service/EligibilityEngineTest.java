package com.kec.placement.service;

import com.kec.placement.model.Assessment;
import com.kec.placement.model.EligibilityResult;
import com.kec.placement.model.PlacementBand;
import com.kec.placement.model.RecruitmentTier;
import com.kec.placement.model.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite verifying EligibilityEngine rules:
 * - All three recruitment categories (Tier 1 Product, Core Engineering, Mass Recruiter)
 * - Critical subject minimums
 * - Aggregate minimums
 * - Active backlogs gating (> 2 = NE)
 * - Placement bands & ranks (Super Dream/A, Dream/B, Regular/C, Ineligible/F)
 * - Rejection of incomplete assessment records
 * - Standalone PRI calculation
 */
class EligibilityEngineTest {

    private EligibilityEngine engine;

    @BeforeEach
    void setUp() {
        engine = new EligibilityEngine();
    }

    @Test
    @DisplayName("Tier-1 Product: Eligible Super Dream (Aggregate >= 85, Coding >= 60)")
    void testTier1SuperDream() {
        Student student = new Student("24ITR001", "Arun Kumar", "IT", 9.1);
        Assessment assessment = new Assessment("24ITR001", 90, 92, 85, 90, 0);
        student.setAssessment(assessment);

        EligibilityResult result = engine.evaluate(student, RecruitmentTier.TIER_1_PRODUCT);

        // Coding 50% = 46.0, Aptitude 30% = 27.0, Soft 20% = 18.0 -> Aggregate = 91.0
        assertEquals(91.0, result.getAggregateScore());
        assertEquals(92.0, result.getCriticalSubjectScore());
        assertTrue(result.getPassedSubjectCutoff());
        assertTrue(result.getPassedAggregateCutoff());
        assertFalse(result.getIsBacklogBarred());
        assertTrue(result.isEligible());
        assertEquals("ELIGIBLE", result.getEligibilityStatus());
        assertEquals(PlacementBand.SUPER_DREAM, result.getPlacementBand());
        assertEquals("A", result.getPlacementRank());

        // PRI tested separately without coupling to core screening
        Double pri = engine.computePriOptional(result.getAggregateScore(), RecruitmentTier.TIER_1_PRODUCT, 100);
        assertNotNull(pri);
        assertEquals(0.91, pri);
    }

    @Test
    @DisplayName("Tier-1 Product: Eligible Dream (Aggregate 70-84, Coding >= 60)")
    void testTier1Dream() {
        Student student = new Student("24ITR002", "Bala M", "CSE", 8.2);
        // Coding=70 (35.0), Aptitude=80 (24.0), Soft=75 (15.0) -> Aggregate = 74.0
        Assessment assessment = new Assessment("24ITR002", 80, 70, 70, 75, 1);
        student.setAssessment(assessment);

        EligibilityResult result = engine.evaluate(student, RecruitmentTier.TIER_1_PRODUCT);

        assertEquals(74.0, result.getAggregateScore());
        assertTrue(result.isEligible());
        assertEquals(PlacementBand.DREAM, result.getPlacementBand());
        assertEquals("B", result.getPlacementRank());
    }

    @Test
    @DisplayName("Tier-1 Product: Disqualified if Coding < 60 despite high aggregate")
    void testTier1FailsCodingThreshold() {
        Student student = new Student("24ITR003", "Charan R", "IT", 8.5);
        // Coding=55 (< 60 cutoff), Aptitude=95, Soft=90
        // Aggregate = (55 * 0.5) + (95 * 0.3) + (90 * 0.2) = 27.5 + 28.5 + 18.0 = 74.0
        Assessment assessment = new Assessment("24ITR003", 95, 55, 80, 90, 0);
        student.setAssessment(assessment);

        EligibilityResult result = engine.evaluate(student, RecruitmentTier.TIER_1_PRODUCT);

        assertEquals(74.0, result.getAggregateScore());
        assertEquals(55.0, result.getCriticalSubjectScore());
        assertFalse(result.getPassedSubjectCutoff());
        assertTrue(result.getPassedAggregateCutoff());
        assertFalse(result.isEligible());
        assertEquals("NE", result.getEligibilityStatus());
        assertEquals(PlacementBand.INELIGIBLE, result.getPlacementBand());
        assertEquals("F", result.getPlacementRank());
        assertTrue(result.getRemarks().contains("below subject cutoff"));
    }

    @Test
    @DisplayName("Tier-1 Product: Disqualified if Aggregate < 70 despite passing Coding threshold")
    void testTier1FailsAggregateThreshold() {
        Student student = new Student("24ITR008", "Hari P", "IT", 7.5);
        // Coding=65 (32.5), Aptitude=60 (18.0), Soft=60 (12.0) -> Aggregate = 62.5 (< 70)
        Assessment assessment = new Assessment("24ITR008", 60, 65, 70, 60, 0);
        student.setAssessment(assessment);

        EligibilityResult result = engine.evaluate(student, RecruitmentTier.TIER_1_PRODUCT);

        assertEquals(62.5, result.getAggregateScore());
        assertTrue(result.getPassedSubjectCutoff()); // 65 >= 60
        assertFalse(result.getPassedAggregateCutoff()); // 62.5 < 70
        assertFalse(result.isEligible());
        assertEquals("NE", result.getEligibilityStatus());
        assertEquals(PlacementBand.INELIGIBLE, result.getPlacementBand());
        assertEquals("F", result.getPlacementRank());
    }

    @Test
    @DisplayName("Core Engineering: Technical >= 50, Aggregate >= 60, Regular Band")
    void testCoreEngineeringRegular() {
        Student student = new Student("24ITR004", "Deepak K", "MECH", 7.8);
        // Tech=60 (24.0), Apt=65 (26.0), Soft=70 (14.0) -> Aggregate = 64.0
        Assessment assessment = new Assessment("24ITR004", 65, 50, 60, 70, 2);
        student.setAssessment(assessment);

        EligibilityResult result = engine.evaluate(student, RecruitmentTier.CORE_ENGINEERING);

        assertEquals(64.0, result.getAggregateScore());
        assertEquals(60.0, result.getCriticalSubjectScore());
        assertTrue(result.isEligible());
        assertEquals(PlacementBand.REGULAR, result.getPlacementBand());
        assertEquals("C", result.getPlacementRank());
    }

    @Test
    @DisplayName("Core Engineering: Disqualified if Technical < 50")
    void testCoreEngineeringFailsTechnicalThreshold() {
        Student student = new Student("24ITR009", "Indran M", "MECH", 8.0);
        // Tech=45 (< 50), Apt=90, Soft=80 -> Aggregate = 18 + 36 + 16 = 70.0
        Assessment assessment = new Assessment("24ITR009", 90, 70, 45, 80, 0);
        student.setAssessment(assessment);

        EligibilityResult result = engine.evaluate(student, RecruitmentTier.CORE_ENGINEERING);

        assertEquals(70.0, result.getAggregateScore());
        assertFalse(result.getPassedSubjectCutoff());
        assertFalse(result.isEligible());
        assertEquals("NE", result.getEligibilityStatus());
        assertEquals(PlacementBand.INELIGIBLE, result.getPlacementBand());
    }

    @Test
    @DisplayName("Mass Recruiter: Aptitude >= 40, Aggregate >= 50")
    void testMassRecruiterEligibility() {
        Student student = new Student("24ITR005", "Elango S", "ECE", 7.2);
        // Apt=50 (25.0), Soft=60 (18.0), Tech=45 (9.0) -> Aggregate = 52.0
        Assessment assessment = new Assessment("24ITR005", 50, 40, 45, 60, 1);
        student.setAssessment(assessment);

        EligibilityResult result = engine.evaluate(student, RecruitmentTier.MASS_RECRUITER);

        assertEquals(52.0, result.getAggregateScore());
        assertEquals(50.0, result.getCriticalSubjectScore());
        assertTrue(result.isEligible());
        assertEquals(PlacementBand.REGULAR, result.getPlacementBand());
        assertEquals("C", result.getPlacementRank());
    }

    @Test
    @DisplayName("Mass Recruiter: Disqualified if Aptitude < 40")
    void testMassRecruiterFailsAptitudeThreshold() {
        Student student = new Student("24ITR010", "Janani K", "ECE", 7.9);
        // Apt=35 (< 40), Soft=80, Tech=70 -> Aggregate = 17.5 + 24 + 14 = 55.5
        Assessment assessment = new Assessment("24ITR010", 35, 60, 70, 80, 0);
        student.setAssessment(assessment);

        EligibilityResult result = engine.evaluate(student, RecruitmentTier.MASS_RECRUITER);

        assertFalse(result.getPassedSubjectCutoff());
        assertFalse(result.isEligible());
        assertEquals("NE", result.getEligibilityStatus());
    }

    @Test
    @DisplayName("Backlog Filter: Active Backlogs > 2 must be barred (NE, Rank F)")
    void testActiveBacklogsGating() {
        Student student = new Student("24ITR006", "Farooq A", "IT", 8.9);
        // Perfect scores across all phases, but 3 active backlogs
        Assessment assessment = new Assessment("24ITR006", 100, 100, 100, 100, 3);
        student.setAssessment(assessment);

        EligibilityResult result = engine.evaluate(student, RecruitmentTier.TIER_1_PRODUCT);

        assertTrue(result.getIsBacklogBarred());
        assertFalse(result.isEligible());
        assertEquals("NE", result.getEligibilityStatus());
        assertEquals(PlacementBand.INELIGIBLE, result.getPlacementBand());
        assertEquals("F", result.getPlacementRank());
        assertTrue(result.getRemarks().contains("Barred due to active backlogs (3 > 2)"));
    }

    @Test
    @DisplayName("Incomplete Assessment must be rejected with NE status")
    void testIncompleteAssessmentRejection() {
        Student student = new Student("24ITR007", "Gokul V", "IT", 7.5);
        // Only Phase 1 recorded, Phase 2 and 3 missing (null)
        Assessment assessment = new Assessment("24ITR007");
        assessment.recordPhase1(80);
        student.setAssessment(assessment);

        EligibilityResult result = engine.evaluate(student, RecruitmentTier.TIER_1_PRODUCT);

        assertFalse(result.isEligible());
        assertEquals("NE", result.getEligibilityStatus());
        assertEquals(PlacementBand.INELIGIBLE, result.getPlacementBand());
        assertEquals("F", result.getPlacementRank());
        assertTrue(result.getRemarks().contains("Incomplete assessment"));
    }
}
