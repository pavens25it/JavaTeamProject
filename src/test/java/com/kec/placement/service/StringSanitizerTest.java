package com.kec.placement.service;

import com.kec.placement.model.Assessment;
import com.kec.placement.model.EligibilityResult;
import com.kec.placement.model.RecruitmentTier;
import com.kec.placement.model.Student;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test suite verifying StringSanitizer:
 * - Register number format validation (24ITR001)
 * - String sanitization
 * - Skill parsing via substring() and indexOf()
 * - Shortlist table formatting with String.format()
 */
class StringSanitizerTest {

    @Test
    @DisplayName("Should validate correct KEC register numbers")
    void testValidRegisterNumbers() {
        assertTrue(StringSanitizer.isValidRegisterNo("24ITR001"));
        assertTrue(StringSanitizer.isValidRegisterNo("24CSR050"));
        assertTrue(StringSanitizer.isValidRegisterNo("24ECR100"));
        assertTrue(StringSanitizer.isValidRegisterNo("24MER200"));
        assertTrue(StringSanitizer.isValidRegisterNo("24ADR015"));
        assertTrue(StringSanitizer.isValidRegisterNo("24IT001")); // 2 letter dept code
    }

    @Test
    @DisplayName("Should reject invalid register numbers")
    void testInvalidRegisterNumbers() {
        assertFalse(StringSanitizer.isValidRegisterNo(null));
        assertFalse(StringSanitizer.isValidRegisterNo(""));
        assertFalse(StringSanitizer.isValidRegisterNo("   "));
        assertFalse(StringSanitizer.isValidRegisterNo("ITR001"));      // missing year
        assertFalse(StringSanitizer.isValidRegisterNo("24ITR1"));      // only 1 digit roll
        assertFalse(StringSanitizer.isValidRegisterNo("24ITR12"));     // only 2 digits roll
        assertFalse(StringSanitizer.isValidRegisterNo("241TR001"));    // digit in dept code
        assertFalse(StringSanitizer.isValidRegisterNo("24TOOLONG001"));// dept code > 4 chars
    }

    @Test
    @DisplayName("Should sanitize inputs properly")
    void testInputSanitization() {
        assertEquals("24ITR001", StringSanitizer.sanitizeRegisterNo("  24itr001  "));
        assertEquals("John Doe", StringSanitizer.sanitize("  John Doe  \t\r\n"));
        assertEquals("", StringSanitizer.sanitize(null));
    }

    @Test
    @DisplayName("Should parse skills using substring() and indexOf()")
    void testSkillKeywordParsing() {
        String raw = "Java, Spring Boot, Microservices, Docker, Kubernetes";
        List<String> skills = StringSanitizer.parseSkills(raw);

        assertEquals(5, skills.size());
        assertEquals("Java", skills.get(0));
        assertEquals("Spring Boot", skills.get(1));
        assertEquals("Microservices", skills.get(2));
        assertEquals("Docker", skills.get(3));
        assertEquals("Kubernetes", skills.get(4));

        // Edge case: Extra commas, leading/trailing whitespace
        List<String> messySkills = StringSanitizer.parseSkills("  Python , , Django,   FastAPI, ");
        assertEquals(3, messySkills.size());
        assertEquals("Python", messySkills.get(0));
        assertEquals("Django", messySkills.get(1));
        assertEquals("FastAPI", messySkills.get(2));

        // Empty / null
        assertTrue(StringSanitizer.parseSkills("").isEmpty());
        assertTrue(StringSanitizer.parseSkills(null).isEmpty());
    }

    @Test
    @DisplayName("Should check for skill keyword presence via indexOf")
    void testContainsSkill() {
        String raw = "Java, AWS, React, Docker";
        assertTrue(StringSanitizer.containsSkill(raw, "Java"));
        assertTrue(StringSanitizer.containsSkill(raw, "docker"));
        assertTrue(StringSanitizer.containsSkill(raw, "react"));
        assertFalse(StringSanitizer.containsSkill(raw, "Kubernetes"));
    }

    @Test
    @DisplayName("Should format shortlist tables cleanly using String.format()")
    void testShortlistFormatting() {
        EligibilityEngine engine = new EligibilityEngine();

        Student s1 = new Student("24ITR001", "Aadhavan K", "IT", 9.0);
        s1.setAssessment(new Assessment("24ITR001", 90, 95, 90, 85, 0));

        Student s2 = new Student("24ITR002", "Bhavani S", "CSE", 8.0);
        s2.setAssessment(new Assessment("24ITR002", 50, 45, 60, 60, 1)); // Fails coding min 60

        List<EligibilityResult> results = new ArrayList<>();
        results.add(engine.evaluate(s1, RecruitmentTier.TIER_1_PRODUCT));
        results.add(engine.evaluate(s2, RecruitmentTier.TIER_1_PRODUCT));

        String table = StringSanitizer.formatShortlistTable(RecruitmentTier.TIER_1_PRODUCT, results);

        assertNotNull(table);
        assertTrue(table.contains("KEC PLACEMENT SHORTLIST: TIER-1 PRODUCT"));
        assertTrue(table.contains("24ITR001"));
        assertTrue(table.contains("Aadhavan K"));
        assertTrue(table.contains("ELIGIBLE"));
        assertTrue(table.contains("24ITR002"));
        assertTrue(table.contains("NE"));
        assertTrue(table.contains("Total Screened: 2 | Eligible: 1 | Disqualified: 1"));
    }
}
