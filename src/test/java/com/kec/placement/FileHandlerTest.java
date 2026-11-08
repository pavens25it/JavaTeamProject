package com.kec.placement;

import com.kec.placement.model.EligibilityResult;
import com.kec.placement.model.RecruitmentTier;
import com.kec.placement.model.Student;
import com.kec.placement.service.EligibilityEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying FileHandler CSV generation, ingestion, validation, and export.
 */
class FileHandlerTest {

    @Test
    @DisplayName("Should generate 500+ synthetic candidates, import them, and validate data")
    void testSyntheticGenerationAndImport(@TempDir Path tempDir) throws IOException {
        FileHandler handler = new FileHandler();
        Path csvFile = tempDir.resolve("synthetic_candidates.csv");

        // 1. Generate 520 candidate records (exceeding 500+ requirement)
        handler.generateSyntheticCandidateCsv(csvFile, 520);
        assertTrue(Files.exists(csvFile));

        // 2. Import back from CSV
        List<Student> students = handler.importStudentsFromCsv(csvFile);
        assertEquals(520, students.size());

        // 3. Inspect first candidate to verify fields
        Student first = students.get(0);
        assertNotNull(first.getRegisterNo());
        assertTrue(first.getRegisterNo().startsWith("24"));
        assertNotNull(first.getFullName());
        assertNotNull(first.getDepartment());
        assertNotNull(first.getCgpa());
        assertNotNull(first.getAssessment());
        assertNotNull(first.getAssessment().getAptitudeScore());
        assertNotNull(first.getAssessment().getCodingScore());
        assertNotNull(first.getAssessment().getTechnicalScore());
        assertNotNull(first.getAssessment().getSoftSkillsScore());
        assertNotNull(first.getActiveBacklogs());
        assertFalse(first.getSkills().isEmpty());
    }

    @Test
    @DisplayName("Should export shortlist to CSV properly")
    void testExportShortlistCsv(@TempDir Path tempDir) throws IOException {
        FileHandler handler = new FileHandler();
        Path inputCsv = tempDir.resolve("input.csv");
        Path outputCsv = tempDir.resolve("shortlist_tier1.csv");

        // Generate small test batch
        handler.generateSyntheticCandidateCsv(inputCsv, 15);
        List<Student> students = handler.importStudentsFromCsv(inputCsv);

        EligibilityEngine engine = new EligibilityEngine();
        List<EligibilityResult> results = engine.evaluateAll(students, RecruitmentTier.TIER_1_PRODUCT);

        int exported = handler.exportShortlistCsv(outputCsv, results, true);
        assertTrue(Files.exists(outputCsv));

        List<String> lines = Files.readAllLines(outputCsv);
        assertTrue(lines.size() >= 1); // Has header
        assertEquals("register_no,full_name,department,cgpa,tier,aggregate_score,critical_score,placement_band,placement_rank,status,remarks", lines.get(0));
        assertEquals(exported, lines.size() - 1);
    }
}
