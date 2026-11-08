package com.kec.placement.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying Step 2 Java Model classes.
 */
class ModelTest {

    @Test
    @DisplayName("Assessment should hold nullable Integer scores and track completion")
    void testAssessmentPhasedScores() {
        Assessment assessment = new Assessment("24ITR001");

        // Initially all scores are null (incomplete)
        assertNull(assessment.getAptitudeScore());
        assertNull(assessment.getCodingScore());
        assertNull(assessment.getTechnicalScore());
        assertNull(assessment.getSoftSkillsScore());
        assertNull(assessment.getActiveBacklogs());
        assertFalse(assessment.isFullyAssessed());

        // Phase 1
        assessment.recordPhase1(85);
        assertEquals(85, assessment.getAptitudeScore());
        assertTrue(assessment.isPhase1Complete());
        assertFalse(assessment.isPhase2Complete());

        // Phase 2: Coding and Technical scores are separate
        assessment.recordPhase2(90, 75);
        assertEquals(90, assessment.getCodingScore());
        assertEquals(75, assessment.getTechnicalScore());
        assertTrue(assessment.isPhase2Complete());

        // Phase 3: Soft skills and active backlogs
        assessment.recordPhase3(80, 1);
        assertEquals(80, assessment.getSoftSkillsScore());
        assertEquals(1, assessment.getActiveBacklogs());
        assertTrue(assessment.isPhase3Complete());
        assertTrue(assessment.isFullyAssessed());
    }

    @Test
    @DisplayName("Assessment should reject invalid scores and negative backlogs")
    void testAssessmentValidation() {
        Assessment assessment = new Assessment("24ITR002");

        assertThrows(IllegalArgumentException.class, () -> assessment.recordPhase1(-5));
        assertThrows(IllegalArgumentException.class, () -> assessment.recordPhase1(105));
        assertThrows(IllegalArgumentException.class, () -> assessment.recordPhase2(150, 80));
        assertThrows(IllegalArgumentException.class, () -> assessment.recordPhase3(70, -1));
    }

    @Test
    @DisplayName("Student should avoid duplicate backlogs field and delegate to Assessment")
    void testStudentModelAndBacklogsDelegation() {
        Student student = new Student("24ITR003", "Kavya S", "IT", 8.75, "Java, Python, Spring Boot");

        assertEquals("24ITR003", student.getRegisterNo());
        assertEquals("Kavya S", student.getFullName());
        assertEquals("IT", student.getDepartment());
        assertEquals(8.75, student.getCgpa());

        // Without assessment, active backlogs returns null
        assertNull(student.getActiveBacklogs());

        // Attach assessment
        Assessment assessment = new Assessment("24ITR003", 85, 92, 88, 90, 0);
        student.setAssessment(assessment);

        // Delegated to assessment
        assertEquals(0, student.getActiveBacklogs());
        assertEquals(92, student.getAssessment().getCodingScore());
        assertEquals(88, student.getAssessment().getTechnicalScore());
    }

    @Test
    @DisplayName("RecruitmentTier should accurately reflect tier specifications")
    void testRecruitmentTierSpecifications() {
        // TIER-1 PRODUCT
        RecruitmentTier tier1 = RecruitmentTier.TIER_1_PRODUCT;
        assertEquals("Coding", tier1.getCriticalSubjectName());
        assertEquals(0.50, tier1.getCodingWeight());
        assertEquals(0.00, tier1.getTechnicalWeight());
        assertEquals(0.30, tier1.getAptitudeWeight());
        assertEquals(0.20, tier1.getSoftSkillsWeight());
        assertEquals(60.0, tier1.getMinSubjectScore());
        assertEquals(70.0, tier1.getMinAggregateScore());

        // CORE ENGINEERING
        RecruitmentTier core = RecruitmentTier.CORE_ENGINEERING;
        assertEquals("Technical", core.getCriticalSubjectName());
        assertEquals(0.00, core.getCodingWeight());
        assertEquals(0.40, core.getTechnicalWeight());
        assertEquals(0.40, core.getAptitudeWeight());
        assertEquals(0.20, core.getSoftSkillsWeight());
        assertEquals(50.0, core.getMinSubjectScore());
        assertEquals(60.0, core.getMinAggregateScore());

        // MASS RECRUITER
        RecruitmentTier mass = RecruitmentTier.MASS_RECRUITER;
        assertEquals("Aptitude", mass.getCriticalSubjectName());
        assertEquals(0.00, mass.getCodingWeight());
        assertEquals(0.20, mass.getTechnicalWeight());
        assertEquals(0.50, mass.getAptitudeWeight());
        assertEquals(0.30, mass.getSoftSkillsWeight());
        assertEquals(40.0, mass.getMinSubjectScore());
        assertEquals(50.0, mass.getMinAggregateScore());
    }

    @Test
    @DisplayName("PlacementBand should correctly specify score ranges and ranks")
    void testPlacementBandDefinitions() {
        assertEquals("A", PlacementBand.SUPER_DREAM.getRank());
        assertEquals(85.0, PlacementBand.SUPER_DREAM.getMinScore());

        assertEquals("B", PlacementBand.DREAM.getRank());
        assertEquals(70.0, PlacementBand.DREAM.getMinScore());

        assertEquals("C", PlacementBand.REGULAR.getRank());
        assertEquals(50.0, PlacementBand.REGULAR.getMinScore());

        assertEquals("F", PlacementBand.INELIGIBLE.getRank());
    }

    @Test
    @DisplayName("EligibilityResult should encapsulate evaluated candidate properties")
    void testEligibilityResultEncapsulation() {
        Student student = new Student("24ITR004", "Praveen M", "IT", 8.4);
        EligibilityResult result = new EligibilityResult(student, RecruitmentTier.TIER_1_PRODUCT);

        result.setAggregateScore(82.5);
        result.setCriticalSubjectScore(80.0);
        result.setIsBacklogBarred(false);
        result.setPassedSubjectCutoff(true);
        result.setPassedAggregateCutoff(true);
        result.setEligible(true);
        result.setEligibilityStatus("ELIGIBLE");
        result.setPlacementBand(PlacementBand.DREAM);
        result.setPlacementRank("B");
        result.setPriScore(0.825);
        result.setRemarks("Eligible for Dream Category");

        assertTrue(result.isEligible());
        assertEquals("ELIGIBLE", result.getEligibilityStatus());
        assertEquals(PlacementBand.DREAM, result.getPlacementBand());
        assertEquals("B", result.getPlacementRank());
        assertEquals(82.5, result.getAggregateScore());
    }
}
