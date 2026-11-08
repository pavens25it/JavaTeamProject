package com.kec.placement;

import com.kec.placement.model.Assessment;
import com.kec.placement.model.EligibilityResult;
import com.kec.placement.model.RecruitmentTier;
import com.kec.placement.model.Student;
import com.kec.placement.service.StringSanitizer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * FileHandler manages CSV ingestion, data sanitization, synthetic dataset generation,
 * and company/tier-wise shortlist file export.
 * 
 * Functional Specifications:
 * - Import candidate assessment data from CSV.
 * - Validate and sanitize imported records.
 * - Export separate company-wise shortlist CSV files.
 * - Generate 500+ synthetic candidate records for testing and benchmarks.
 */
public class FileHandler {

    public static final String CSV_HEADER = "register_no,full_name,department,cgpa,phase1_aptitude,phase2_coding,phase2_technical,phase3_soft_skills,active_backlogs,skills";
    public static final String SHORTLIST_HEADER = "register_no,full_name,department,cgpa,tier,aggregate_score,critical_score,placement_band,placement_rank,status,remarks";

    /**
     * Imports candidate student and assessment records from a CSV file.
     * Invalid or malformed rows are skipped safely with warning messages.
     *
     * @param csvPath Path to input CSV file
     * @return List of validated Student objects with attached Assessments
     * @throws IOException on file read errors
     */
    public List<Student> importStudentsFromCsv(Path csvPath) throws IOException {
        if (!Files.exists(csvPath)) {
            throw new IllegalArgumentException("CSV file does not exist: " + csvPath);
        }

        List<Student> students = new ArrayList<>();
        int lineNo = 0;
        int skippedCount = 0;

        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // Skip comments and blank lines
                }

                // Skip header line if present
                if (lineNo == 1 && line.toLowerCase().contains("register_no")) {
                    continue;
                }

                try {
                    Student student = parseStudentCsvLine(line);
                    if (student != null) {
                        students.add(student);
                    } else {
                        skippedCount++;
                    }
                } catch (Exception e) {
                    System.err.printf("[FileHandler Warning] Skipping malformed line %d: %s (%s)%n",
                            lineNo, line, e.getMessage());
                    skippedCount++;
                }
            }
        }

        System.out.printf("[FileHandler] Ingestion completed: %d valid candidates loaded, %d rows skipped from %s%n",
                students.size(), skippedCount, csvPath.getFileName());
        return students;
    }

    /**
     * Parses a single comma-separated row into a Student and attached Assessment.
     */
    public Student parseStudentCsvLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        // Split CSV row while preserving empty tokens
        String[] tokens = line.split(",", -1);
        if (tokens.length < 9) {
            throw new IllegalArgumentException("Expected at least 9 fields, found " + tokens.length);
        }

        // 1. Sanitize & validate register number
        String rawRegNo = StringSanitizer.sanitize(tokens[0]);
        if (!StringSanitizer.isValidRegisterNo(rawRegNo)) {
            throw new IllegalArgumentException("Invalid register number format: " + rawRegNo);
        }
        String regNo = StringSanitizer.sanitizeRegisterNo(rawRegNo);

        // 2. Profile metadata
        String name = StringSanitizer.sanitize(tokens[1]);
        String dept = StringSanitizer.sanitize(tokens[2]);
        Double cgpa = parseNullableDouble(tokens[3]);
        if (cgpa != null) {
            Student.validateCgpa(cgpa);
        }

        // 3. Phased Assessment scores (nullable Integer)
        Integer apt = parseNullableInteger(tokens[4]);
        Integer coding = parseNullableInteger(tokens[5]);
        Integer tech = parseNullableInteger(tokens[6]);
        Integer soft = parseNullableInteger(tokens[7]);
        Integer backlogs = parseNullableInteger(tokens[8]);

        // Validate bounds when values are present
        Assessment.validateScore(apt, "Aptitude");
        Assessment.validateScore(coding, "Coding");
        Assessment.validateScore(tech, "Technical");
        Assessment.validateScore(soft, "Soft Skills");
        Assessment.validateBacklogs(backlogs);

        // 4. Skills Parsing via StringSanitizer (substring/indexOf)
        String rawSkills = (tokens.length > 9) ? StringSanitizer.sanitize(tokens[9]) : "";
        List<String> parsedSkills = StringSanitizer.parseSkills(rawSkills);

        // Construct models
        Student student = new Student(regNo, name, dept, cgpa, rawSkills);
        student.setSkills(parsedSkills);

        Assessment assessment = new Assessment(regNo, apt, coding, tech, soft, backlogs);
        student.setAssessment(assessment);

        return student;
    }

    /**
     * Exports a list of candidate eligibility results to a CSV file.
     *
     * @param outputPath Target file destination
     * @param results Candidate evaluation results
     * @param onlyEligible If true, exports only ELIGIBLE students; otherwise exports all
     * @return count of exported records
     * @throws IOException on write errors
     */
    public int exportShortlistCsv(Path outputPath, List<EligibilityResult> results, boolean onlyEligible)
            throws IOException {
        if (outputPath.getParent() != null) {
            Files.createDirectories(outputPath.getParent());
        }

        int exportedCount = 0;
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            writer.write(SHORTLIST_HEADER);
            writer.newLine();

            if (results != null) {
                for (EligibilityResult r : results) {
                    if (onlyEligible && !r.isEligible()) {
                        continue;
                    }

                    Student s = r.getStudent();
                    String line = String.format("%s,%s,%s,%.2f,%s,%.2f,%.2f,%s,%s,%s,\"%s\"",
                            s != null ? s.getRegisterNo() : "",
                            s != null ? s.getFullName() : "",
                            s != null ? s.getDepartment() : "",
                            s != null && s.getCgpa() != null ? s.getCgpa() : 0.0,
                            r.getTier() != null ? r.getTier().name() : "",
                            r.getAggregateScore() != null ? r.getAggregateScore() : 0.0,
                            r.getCriticalSubjectScore() != null ? r.getCriticalSubjectScore() : 0.0,
                            r.getPlacementBand() != null ? r.getPlacementBand().getBandName() : "",
                            r.getPlacementRank() != null ? r.getPlacementRank() : "",
                            r.getEligibilityStatus() != null ? r.getEligibilityStatus() : "",
                            r.getRemarks() != null ? r.getRemarks().replace("\"", "'") : "");

                    writer.write(line);
                    writer.newLine();
                    exportedCount++;
                }
            }
        }

        System.out.printf("[FileHandler] Successfully exported %d records to %s%n",
                exportedCount, outputPath.toAbsolutePath());
        return exportedCount;
    }

    /**
     * Generates a synthetic dataset of realistic candidate records for testing & benchmark.
     *
     * @param targetPath Destination CSV path
     * @param count Number of candidate records to generate (e.g. 500+)
     * @throws IOException on file write errors
     */
    public void generateSyntheticCandidateCsv(Path targetPath, int count) throws IOException {
        if (targetPath.getParent() != null) {
            Files.createDirectories(targetPath.getParent());
        }

        Random rand = new Random(42); // Deterministic seed for reproducible benchmarks
        String[] depts = {"ITR", "CSR", "ECR", "MER", "EER", "ADR"};
        String[] firstNames = {"Aarav", "Bala", "Charan", "Deepa", "Ezhil", "Farooq", "Gokul", "Harini",
                               "Ishaan", "Janani", "Karthik", "Lavanya", "Manoj", "Naveen", "Oviya", "Praveen",
                               "Rahul", "Sneha", "Tarun", "Vignesh"};
        String[] lastNames = {"Kumar", "Rajan", "Senguttuvan", "Murugan", "Subramanian", "Natarajan",
                              "Chandran", "Sundaram", "Krishnan", "Velu"};
        String[] skillPool = {"Java", "Python", "C++", "Spring Boot", "SQL", "Docker", "AWS",
                              "React", "NodeJS", "Machine Learning", "Data Structures", "Linux"};

        try (BufferedWriter writer = Files.newBufferedWriter(targetPath, StandardCharsets.UTF_8)) {
            writer.write(CSV_HEADER);
            writer.newLine();

            for (int i = 1; i <= count; i++) {
                String dept = depts[rand.nextInt(depts.length)];
                String regNo = String.format("24%s%03d", dept, i % 1000 == 0 ? 999 : i % 1000);
                String fullName = firstNames[rand.nextInt(firstNames.length)] + " " + lastNames[rand.nextInt(lastNames.length)];
                String deptName = dept.replace("R", "");

                // CGPA 6.0 to 9.8
                double cgpa = 6.0 + (rand.nextDouble() * 3.8);

                // Varied score distributions to hit different bands
                int aptitude = 40 + rand.nextInt(61);    // 40 - 100
                int coding = 30 + rand.nextInt(71);      // 30 - 100
                int technical = 35 + rand.nextInt(66);   // 35 - 100
                int softSkills = 50 + rand.nextInt(51);  // 50 - 100

                // 80% have 0-1 backlogs, 12% have 2 backlogs, 8% have 3-5 backlogs (>2 barred)
                int backlogsRoll = rand.nextInt(100);
                int backlogs = (backlogsRoll < 70) ? 0 : (backlogsRoll < 85 ? 1 : (backlogsRoll < 94 ? 2 : (3 + rand.nextInt(3))));

                // 2 to 4 skills
                int numSkills = 2 + rand.nextInt(3);
                StringBuilder skillsSb = new StringBuilder();
                for (int s = 0; s < numSkills; s++) {
                    if (s > 0) skillsSb.append("; "); // using semicolon or comma
                    skillsSb.append(skillPool[rand.nextInt(skillPool.length)]);
                }
                // Convert to comma separated
                String skills = skillsSb.toString().replace(";", ",");

                String line = String.format("%s,%s,%s,%.2f,%d,%d,%d,%d,%d,\"%s\"",
                        regNo, fullName, deptName, cgpa, aptitude, coding, technical, softSkills, backlogs, skills);
                writer.write(line);
                writer.newLine();
            }
        }

        System.out.printf("[FileHandler] Generated %d synthetic candidates at %s%n",
                count, targetPath.toAbsolutePath());
    }

    public static void main(String[] args) {
        try {
            FileHandler handler = new FileHandler();
            Path defaultInput = Path.of("data", "input", "candidates_500.csv");
            handler.generateSyntheticCandidateCsv(defaultInput, 550);
            System.out.println("[FileHandler] Ready! Sample dataset generated at: " + defaultInput.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("Error generating sample data: " + e.getMessage());
        }
    }

    private Double parseNullableDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return Double.parseDouble(value.trim());
    }

    private Integer parseNullableInteger(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return Integer.parseInt(value.trim());
    }
}
