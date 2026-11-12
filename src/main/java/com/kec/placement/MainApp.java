package com.kec.placement;

import com.kec.placement.model.Assessment;
import com.kec.placement.model.EligibilityResult;
import com.kec.placement.model.RecruitmentTier;
import com.kec.placement.model.Student;
import com.kec.placement.service.StringSanitizer;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MainApp is the central application host coordinator for the
 * KEC Placement & Campus Recruitment Eligibility System.
 * 
 * Execution Host: Student 1 (OCI VM 1).
 * 
 * Provides an interactive 13-option console interface and an automated batch mode.
 */
public class MainApp {

    private final FileHandler fileHandler = new FileHandler();
    private final PlacementDAO dao = new PlacementDAO();
    private final List<Student> loadedCandidates = new ArrayList<>();
    private List<ShortlistWorker.WorkerResult> latestResults = new ArrayList<>();

    public static void main(String[] args) {
        MainApp app = new MainApp();

        // Non-interactive automated batch mode (for benchmark / CI)
        if (args.length > 0 && ("--batch".equalsIgnoreCase(args[0]) || "-b".equalsIgnoreCase(args[0]))) {
            app.runBatchMode();
            return;
        }

        app.runInteractiveConsole();
    }

    /**
     * Automated end-to-end batch execution pipeline for Step 9 & benchmarks.
     */
    public void runBatchMode() {
        System.out.println("==========================================================================");
        System.out.println(" KEC PLACEMENT ANALYTICS ENGINE - BATCH EXECUTION MODE (OCI VM 1)       ");
        System.out.println("==========================================================================");

        try {
            Path inputPath = Path.of("data", "input", "candidates_500.csv");
            if (!new File(inputPath.toString()).exists()) {
                System.out.println("[Batch] Generating synthetic 550 candidate dataset...");
                fileHandler.generateSyntheticCandidateCsv(inputPath, 550);
            }

            System.out.println("[Batch] Ingesting candidate records from CSV...");
            loadedCandidates.clear();
            loadedCandidates.addAll(fileHandler.importStudentsFromCsv(inputPath));
            System.out.printf("[Batch] Loaded %d candidates.%n", loadedCandidates.size());

            System.out.println("[Batch] Launching 3-tier parallel screening across worker threads...");
            ExecutorService executor = Executors.newFixedThreadPool(3);
            try {
                latestResults = ShortlistWorker.runParallelScreening(loadedCandidates, executor);
            } finally {
                executor.shutdown();
            }

            System.out.println("[Batch] Exporting separate company-wise shortlists to data/output/...");
            Path outputDir = Path.of("data", "output");
            for (ShortlistWorker.WorkerResult wr : latestResults) {
                String filename = "shortlist_" + wr.getTier().name().toLowerCase() + ".csv";
                Path outPath = outputDir.resolve(filename);
                fileHandler.exportShortlistCsv(outPath, wr.getResults(), true);
            }

            System.out.println("[Batch] Probing remote database connectivity (Student 2 - OCI VM 2)...");
            boolean dbOnline = DBConnection.testConnection();
            if (dbOnline) {
                System.out.println("[Batch] DB Online. Syncing students and results...");
                dao.initializeTablesIfNotExist();

                dao.saveOrUpdateStudentsBatch(loadedCandidates);

                List<Assessment> assessments = new ArrayList<>();
                for (Student student : loadedCandidates) {
                    if (student.getAssessment() != null) {
                        assessments.add(student.getAssessment());
                    }
                }

                int savedAssessments = dao.saveOrUpdateAssessmentsBatch(assessments);
                System.out.println("[Batch] Assessments synchronized: " + savedAssessments);

                for (ShortlistWorker.WorkerResult wr : latestResults) {
                    dao.saveEligibilityResultsBatch(wr.getResults());
                }
                System.out.println("[Batch] Remote DB synchronization complete.");
            } else {
                System.out.println("[Batch Note] Student 2's remote DB is currently offline/unreachable.");
                System.out.println("             All local file handling and calculation engine operations succeeded.");
            }

            System.out.println("==========================================================================");
            System.out.println(" BATCH PIPELINE COMPLETED SUCCESSFULLY                                    ");
            System.out.println("==========================================================================");

        } catch (Exception e) {
            System.err.println("[Batch Error] " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Interactive terminal UI providing all 13 required functional options.
     */
    public void runInteractiveConsole() {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        printBanner();

        while (running) {
            printMenu();
            System.out.print("Enter your choice (1-13): ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    handleTestDbConnection();
                    break;
                case "2":
                    handleAddStudent(scanner);
                    break;
                case "3":
                    handleEnterPhase1Scores(scanner);
                    break;
                case "4":
                    handleEnterPhase2Scores(scanner);
                    break;
                case "5":
                    handleEnterPhase3Scores(scanner);
                    break;
                case "6":
                    handleImportCsv(scanner);
                    break;
                case "7":
                    handleViewStudents();
                    break;
                case "8":
                    handleGeneratePlacementShortlists();
                    break;
                case "9":
                    handleViewEligibilityResults(scanner);
                    break;
                case "10":
                    handleExportCompanyShortlists();
                    break;
                case "11":
                    handleTestStringProcessing(scanner);
                    break;
                case "12":
                    handleRun500CandidateBenchmark();
                    break;
                case "13":
                    System.out.println("\nExiting KEC Placement Analytics Engine. Goodbye!");
                    running = false;
                    break;
                default:
                    System.out.println("Invalid selection! Please enter a number between 1 and 13.\n");
            }
        }
    }

    // =========================================================================
    // 13 Menu Handlers
    // =========================================================================

    // Option 1: Test Database Connection
    private void handleTestDbConnection() {
        System.out.println("\n=== [1] Testing Student 2 Remote Database Connection ===");
        System.out.printf("RDBMS: %s | Host: %s:%d | Database: %s | User: %s%n",
                DBConnection.getDbType().toUpperCase(),
                DBConnection.getHost(),
                DBConnection.getPort(),
                DBConnection.getDatabase(),
                DBConnection.getUsername());
        boolean online = DBConnection.testConnection();
        if (online) {
            System.out.println("STATUS: Remote database is ONLINE and responsive.");
        } else {
            System.out.println("STATUS: Remote database is OFFLINE / UNREACHABLE.");
            System.out.println("Check that Student 2's OCI VM 2 is running and Port " + DBConnection.getPort() + " is open.");
        }
        System.out.println();
    }

    // Option 2: Add Student
    private void handleAddStudent(Scanner scanner) {
        System.out.println("\n=== [2] Add New Candidate Profile ===");
        try {
            System.out.print("Enter Register Number (e.g. 24ITR001): ");
            String regNo = scanner.nextLine().trim();
            if (!StringSanitizer.isValidRegisterNo(regNo)) {
                System.out.println("ERROR: Invalid register number format. Expected format like 24ITR001.\n");
                return;
            }
            regNo = StringSanitizer.sanitizeRegisterNo(regNo);

            final String finalRegNo = regNo;
            if (loadedCandidates.stream().anyMatch(s -> s.getRegisterNo().equalsIgnoreCase(finalRegNo))) {
                System.out.println("WARNING: Student with register number " + regNo + " already exists.\n");
                return;
            }

            System.out.print("Enter Full Name: ");
            String name = StringSanitizer.sanitize(scanner.nextLine());

            System.out.print("Enter Department (e.g. IT, CSE, ECE, MECH): ");
            String dept = StringSanitizer.sanitize(scanner.nextLine());

            System.out.print("Enter CGPA (0.0 to 10.0): ");
            double cgpa = Double.parseDouble(scanner.nextLine().trim());
            Student.validateCgpa(cgpa);

            System.out.print("Enter Skills (comma-separated, e.g. Java, Spring Boot, SQL): ");
            String rawSkills = StringSanitizer.sanitize(scanner.nextLine());

            Student student = new Student(regNo, name, dept, cgpa, rawSkills);
            student.setSkills(StringSanitizer.parseSkills(rawSkills));
            student.setAssessment(new Assessment(regNo));

            loadedCandidates.add(student);
            System.out.printf("SUCCESS: Student %s (%s) added to in-memory cohort.%n", name, regNo);

            // Optional DB save
            dao.saveOrUpdateStudent(student);
            System.out.println();
        } catch (Exception e) {
            System.err.println("Error adding student: " + e.getMessage() + "\n");
        }
    }

    // Option 3: Enter Phase 1 Scores
    private void handleEnterPhase1Scores(Scanner scanner) {
        System.out.println("\n=== [3] Enter Phase 1 Scores (Aptitude) ===");
        Student student = promptForStudent(scanner);
        if (student == null) return;

        try {
            System.out.print("Enter Phase 1 Aptitude Score (0 - 100): ");
            int apt = Integer.parseInt(scanner.nextLine().trim());
            Assessment.validateScore(apt, "Aptitude");

            Assessment a = student.getAssessment();
            if (a == null) {
                a = new Assessment(student.getRegisterNo());
                student.setAssessment(a);
            }
            a.recordPhase1(apt);
            System.out.printf("SUCCESS: Phase 1 Aptitude score %d recorded for %s.%n", apt, student.getRegisterNo());

            dao.saveOrUpdateAssessment(a);
            System.out.println();
        } catch (Exception e) {
            System.err.println("Error recording Phase 1 score: " + e.getMessage() + "\n");
        }
    }

    // Option 4: Enter Phase 2 Scores
    private void handleEnterPhase2Scores(Scanner scanner) {
        System.out.println("\n=== [4] Enter Phase 2 Scores (Coding & Technical) ===");
        Student student = promptForStudent(scanner);
        if (student == null) return;

        try {
            System.out.print("Enter Phase 2 Coding Score (0 - 100): ");
            int coding = Integer.parseInt(scanner.nextLine().trim());
            Assessment.validateScore(coding, "Coding");

            System.out.print("Enter Phase 2 Technical / Domain Score (0 - 100): ");
            int tech = Integer.parseInt(scanner.nextLine().trim());
            Assessment.validateScore(tech, "Technical");

            Assessment a = student.getAssessment();
            if (a == null) {
                a = new Assessment(student.getRegisterNo());
                student.setAssessment(a);
            }
            a.recordPhase2(coding, tech);
            System.out.printf("SUCCESS: Phase 2 scores recorded for %s (Coding=%d, Technical=%d).%n",
                    student.getRegisterNo(), coding, tech);

            dao.saveOrUpdateAssessment(a);
            System.out.println();
        } catch (Exception e) {
            System.err.println("Error recording Phase 2 scores: " + e.getMessage() + "\n");
        }
    }

    // Option 5: Enter Phase 3 Scores and Backlogs
    private void handleEnterPhase3Scores(Scanner scanner) {
        System.out.println("\n=== [5] Enter Phase 3 Scores (Soft Skills & Backlogs) ===");
        Student student = promptForStudent(scanner);
        if (student == null) return;

        try {
            System.out.print("Enter Phase 3 Soft Skills Score (0 - 100): ");
            int soft = Integer.parseInt(scanner.nextLine().trim());
            Assessment.validateScore(soft, "Soft Skills");

            System.out.print("Enter Active Backlog Count (>= 0): ");
            int backlogs = Integer.parseInt(scanner.nextLine().trim());
            Assessment.validateBacklogs(backlogs);

            Assessment a = student.getAssessment();
            if (a == null) {
                a = new Assessment(student.getRegisterNo());
                student.setAssessment(a);
            }
            a.recordPhase3(soft, backlogs);
            System.out.printf("SUCCESS: Phase 3 recorded for %s (Soft Skills=%d, Backlogs=%d).%n",
                    student.getRegisterNo(), soft, backlogs);

            if (backlogs > 2) {
                System.out.println("NOTE: Active backlogs > 2. This candidate will be flagged as NE (Rank F).");
            }

            dao.saveOrUpdateAssessment(a);
            System.out.println();
        } catch (Exception e) {
            System.err.println("Error recording Phase 3 scores: " + e.getMessage() + "\n");
        }
    }

    // Option 6: Import CSV
    private void handleImportCsv(Scanner scanner) {
        System.out.println("\n=== [6] Import Candidate Assessments from CSV ===");
        System.out.print("Enter CSV path [Default: data/input/candidates_500.csv]: ");
        String pathInput = scanner.nextLine().trim();
        Path path = pathInput.isEmpty() ? Path.of("data", "input", "candidates_500.csv") : Path.of(pathInput);

        try {
            if (!new File(path.toString()).exists()) {
                System.out.println("File not found at: " + path.toAbsolutePath());
                System.out.println("Generating sample 550 candidate file first...");
                fileHandler.generateSyntheticCandidateCsv(path, 550);
            }

            List<Student> imported = fileHandler.importStudentsFromCsv(path);
            loadedCandidates.clear();
            loadedCandidates.addAll(imported);
            System.out.printf("SUCCESS: %d candidates loaded into memory from %s.%n%n",
                    loadedCandidates.size(), path.getFileName());
        } catch (Exception e) {
            System.err.println("Import failed: " + e.getMessage() + "\n");
        }
    }

    // Option 7: View Students
    private void handleViewStudents() {
        System.out.println("\n=== [7] Loaded Candidate Cohort ===");
        if (loadedCandidates.isEmpty()) {
            System.out.println("No candidates loaded yet. Import CSV (Option 6) or Add Student (Option 2).\n");
            return;
        }

        System.out.printf("Total Enrolled Candidates: %d%n", loadedCandidates.size());
        String line = "+------------+----------------------+------------+------+-----+-----+-----+-----+------+%n";
        String fmt  = "| %-10s | %-20s | %-10s | %-4s | %-3s | %-3s | %-3s | %-3s | %-4s |%n";

        System.out.printf(line);
        System.out.printf(fmt, "REG NO", "STUDENT NAME", "DEPT", "CGPA", "APT", "COD", "TEC", "SFT", "BKLOG");
        System.out.printf(line);

        int previewLimit = Math.min(25, loadedCandidates.size());
        for (int i = 0; i < previewLimit; i++) {
            Student s = loadedCandidates.get(i);
            Assessment a = s.getAssessment();
            System.out.printf(fmt,
                    s.getRegisterNo(),
                    truncate(s.getFullName(), 20),
                    s.getDepartment(),
                    s.getCgpa() != null ? String.format("%.2f", s.getCgpa()) : "N/A",
                    (a != null && a.getAptitudeScore() != null) ? a.getAptitudeScore() : "-",
                    (a != null && a.getCodingScore() != null) ? a.getCodingScore() : "-",
                    (a != null && a.getTechnicalScore() != null) ? a.getTechnicalScore() : "-",
                    (a != null && a.getSoftSkillsScore() != null) ? a.getSoftSkillsScore() : "-",
                    (a != null && a.getActiveBacklogs() != null) ? a.getActiveBacklogs() : "-");
        }
        System.out.printf(line);

        if (loadedCandidates.size() > previewLimit) {
            System.out.printf("... and %d more candidates. Run option 8 to screen all candidates.%n",
                    loadedCandidates.size() - previewLimit);
        }
        System.out.println();
    }

    // Option 8: Generate Placement Shortlists
    private void handleGeneratePlacementShortlists() {
        System.out.println("\n=== [8] Generate Placement Shortlists (Concurrent Multithreading) ===");
        if (loadedCandidates.isEmpty()) {
            System.out.println("No candidates loaded. Attempting default data/input/candidates_500.csv...");
            Path defaultPath = Path.of("data", "input", "candidates_500.csv");
            if (new File(defaultPath.toString()).exists()) {
                try {
                    loadedCandidates.addAll(fileHandler.importStudentsFromCsv(defaultPath));
                } catch (Exception ignored) {}
            }
            if (loadedCandidates.isEmpty()) {
                System.out.println("Please import CSV (Option 6) or run benchmark (Option 12) first.\n");
                return;
            }
        }

        ExecutorService executor = Executors.newFixedThreadPool(3);
        try {
            latestResults = ShortlistWorker.runParallelScreening(loadedCandidates, executor);
            System.out.println("Shortlist generation finished for all 3 categories.");
            System.out.println("Select Option 9 to view tables or Option 10 to export CSVs.\n");
        } catch (Exception e) {
            System.err.println("Screening execution error: " + e.getMessage() + "\n");
        } finally {
            executor.shutdown();
        }
    }

    // Option 9: View Eligibility Results
    private void handleViewEligibilityResults(Scanner scanner) {
        System.out.println("\n=== [9] View Shortlist Tables ===");
        if (latestResults.isEmpty()) {
            System.out.println("No shortlists generated yet. Run Option 8 first.\n");
            return;
        }

        System.out.println("Select Category to View:");
        System.out.println("  1. Tier-1 Product");
        System.out.println("  2. Core Engineering");
        System.out.println("  3. Mass Recruiter");
        System.out.println("  4. All Categories");
        System.out.print("Choice: ");
        String c = scanner.nextLine().trim();

        for (ShortlistWorker.WorkerResult wr : latestResults) {
            boolean match = "4".equals(c)
                    || ("1".equals(c) && wr.getTier() == RecruitmentTier.TIER_1_PRODUCT)
                    || ("2".equals(c) && wr.getTier() == RecruitmentTier.CORE_ENGINEERING)
                    || ("3".equals(c) && wr.getTier() == RecruitmentTier.MASS_RECRUITER);

            if (match) {
                List<EligibilityResult> preview = wr.getResults().subList(0, Math.min(20, wr.getResults().size()));
                System.out.println(StringSanitizer.formatShortlistTable(wr.getTier(), preview));
                if (wr.getResults().size() > 20) {
                    System.out.printf("... preview limited to 20 rows. Export to CSV (Option 10) to inspect all %d records.%n%n",
                            wr.getResults().size());
                }
            }
        }
    }

    // Option 10: Export Company Shortlists
    private void handleExportCompanyShortlists() {
        System.out.println("\n=== [10] Export Separate Company-wise Shortlist CSV Files ===");
        if (latestResults.isEmpty()) {
            System.out.println("No screening results available. Run Option 8 first.\n");
            return;
        }

        Path outDir = Path.of("data", "output");
        try {
            for (ShortlistWorker.WorkerResult wr : latestResults) {
                String fname = "shortlist_" + wr.getTier().name().toLowerCase() + ".csv";
                Path outPath = outDir.resolve(fname);
                int count = fileHandler.exportShortlistCsv(outPath, wr.getResults(), true);
                System.out.printf("-> %s: %d eligible candidates exported to %s%n",
                        wr.getTier().getDisplayName(), count, outPath.toAbsolutePath());
            }
            System.out.println("SUCCESS: All company shortlist CSVs written to data/output/.\n");
        } catch (Exception e) {
            System.err.println("Export failed: " + e.getMessage() + "\n");
        }
    }

    // Option 11: Test String Processing
    private void handleTestStringProcessing(Scanner scanner) {
        System.out.println("\n=== [11] String Processing Verification ===");
        System.out.println("1. Test Register Number Validation (Regex: 24ITR001)");
        System.out.println("2. Test Skill Keyword Extraction (using substring() and indexOf())");
        System.out.print("Choice: ");
        String sub = scanner.nextLine().trim();

        if ("1".equals(sub)) {
            System.out.print("Enter register number to test: ");
            String reg = scanner.nextLine().trim();
            boolean valid = StringSanitizer.isValidRegisterNo(reg);
            System.out.printf("Result for '%s': %s%n%n", reg, valid ? "VALID KEC FORMAT" : "INVALID FORMAT");
        } else {
            System.out.print("Enter raw skills string (e.g. Java, Python, Docker, SQL): ");
            String raw = scanner.nextLine();
            List<String> parsed = StringSanitizer.parseSkills(raw);
            System.out.printf("Extracted %d tokens via substring() & indexOf(): %s%n%n", parsed.size(), parsed);
        }
    }

    // Option 12: Run 500+ Candidate Benchmark
    private void handleRun500CandidateBenchmark() {
        System.out.println("\n=== [12] Run 500+ Candidate Benchmark ===");
        Path benchFile = Path.of("data", "input", "candidates_500.csv");
        try {
            long t0 = System.currentTimeMillis();
            System.out.println("1. Generating 550 synthetic candidates...");
            fileHandler.generateSyntheticCandidateCsv(benchFile, 550);

            System.out.println("2. Importing dataset from disk...");
            loadedCandidates.clear();
            loadedCandidates.addAll(fileHandler.importStudentsFromCsv(benchFile));

            System.out.println("3. Executing parallel screening with 3 concurrent worker threads...");
            ExecutorService executor = Executors.newFixedThreadPool(3);
            try {
                latestResults = ShortlistWorker.runParallelScreening(loadedCandidates, executor);
            } finally {
                executor.shutdown();
            }

            System.out.println("4. Exporting category shortlists...");
            Path outDir = Path.of("data", "output");
            for (ShortlistWorker.WorkerResult wr : latestResults) {
                fileHandler.exportShortlistCsv(outDir.resolve("shortlist_" + wr.getTier().name().toLowerCase() + ".csv"),
                        wr.getResults(), true);
            }

            long totalTime = System.currentTimeMillis() - t0;
            System.out.printf("SUCCESS: 550 candidate benchmark executed in %d ms total.%n%n", totalTime);

        } catch (Exception e) {
            System.err.println("Benchmark error: " + e.getMessage() + "\n");
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Student promptForStudent(Scanner scanner) {
        System.out.print("Enter Student Register Number: ");
        String reg = scanner.nextLine().trim().toUpperCase();

        Optional<Student> opt = loadedCandidates.stream()
                .filter(s -> s.getRegisterNo().equalsIgnoreCase(reg))
                .findFirst();

        if (opt.isPresent()) {
            return opt.get();
        }

        // Try DB lookup
        Student fromDb = dao.getStudentByRegisterNo(reg);
        if (fromDb != null) {
            loadedCandidates.add(fromDb);
            return fromDb;
        }

        System.out.println("Candidate " + reg + " not found. Add student first via Option 2 or Import via Option 6.\n");
        return null;
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen - 3) + "...";
    }

    private void printBanner() {
        System.out.println("==========================================================================");
        System.out.println("        KEC PLACEMENT & CAMPUS RECRUITMENT ELIGIBILITY SYSTEM            ");
        System.out.println("            Student 1 (Application Host) - OCI VM 1 Engine               ");
        System.out.println("==========================================================================");
    }

    private void printMenu() {
        System.out.println("MAIN MENU:");
        System.out.println("  1.  Test Database Connection (OCI VM 2)");
        System.out.println("  2.  Add Student");
        System.out.println("  3.  Enter Phase 1 Scores (Aptitude)");
        System.out.println("  4.  Enter Phase 2 Scores (Coding & Technical)");
        System.out.println("  5.  Enter Phase 3 Scores and Backlogs (Soft Skills & Backlogs)");
        System.out.println("  6.  Import CSV (Assessment Dataset)");
        System.out.println("  7.  View Students (Loaded Cohort)");
        System.out.println("  8.  Generate Placement Shortlists (Multithreaded Screening)");
        System.out.println("  9.  View Eligibility Results (Formatted Tables)");
        System.out.println("  10. Export Company Shortlists (Separate CSVs)");
        System.out.println("  11. Test String Processing (Regex, substring & indexOf)");
        System.out.println("  12. Run 500+ Candidate Benchmark");
        System.out.println("  13. Exit");
        System.out.println("--------------------------------------------------------------------------");
    }
}
