package com.kec.placement;

import com.kec.placement.model.EligibilityResult;
import com.kec.placement.model.PlacementBand;
import com.kec.placement.model.RecruitmentTier;
import com.kec.placement.model.Student;
import com.kec.placement.service.EligibilityEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * ShortlistWorker executes concurrent candidate screening using Java multithreading.
 * 
 * Functional Specifications:
 * - Implements Callable<WorkerResult> for asynchronous evaluation.
 * - Runs 3 recruitment categories (Tier-1 Product, Core Engineering, Mass Recruiter) in parallel.
 * - Processes 500+ candidate records concurrently.
 * - Displays worker thread names, processing counts, and execution times.
 */
public class ShortlistWorker implements Callable<ShortlistWorker.WorkerResult> {

    private final RecruitmentTier tier;
    private final List<Student> students;
    private final EligibilityEngine engine;

    /**
     * Constructs a ShortlistWorker for a specific recruitment category tier.
     *
     * @param tier Recruitment category to evaluate
     * @param students Candidate cohort list
     */
    public ShortlistWorker(RecruitmentTier tier, List<Student> students) {
        this(tier, students, new EligibilityEngine());
    }

    public ShortlistWorker(RecruitmentTier tier, List<Student> students, EligibilityEngine engine) {
        if (tier == null) {
            throw new IllegalArgumentException("RecruitmentTier cannot be null.");
        }
        this.tier = tier;
        this.students = (students != null) ? students : Collections.emptyList();
        this.engine = (engine != null) ? engine : new EligibilityEngine();
    }

    @Override
    public WorkerResult call() {
        String threadName = Thread.currentThread().getName();
        long startTime = System.currentTimeMillis();

        List<EligibilityResult> results = new ArrayList<>(students.size());
        int eligibleCount = 0;
        int barredCount = 0;
        int superDreamCount = 0;
        int dreamCount = 0;
        int regularCount = 0;
        int ineligibleCount = 0;

        // Execute screening for every candidate in this worker thread
        for (Student student : students) {
            EligibilityResult res = engine.evaluate(student, tier);
            results.add(res);

            if (res.isEligible()) {
                eligibleCount++;
                if (res.getPlacementBand() == PlacementBand.SUPER_DREAM) {
                    superDreamCount++;
                } else if (res.getPlacementBand() == PlacementBand.DREAM) {
                    dreamCount++;
                } else if (res.getPlacementBand() == PlacementBand.REGULAR) {
                    regularCount++;
                }
            } else {
                ineligibleCount++;
                if (Boolean.TRUE.equals(res.getIsBacklogBarred())) {
                    barredCount++;
                }
            }
        }

        long executionTimeMs = System.currentTimeMillis() - startTime;

        // Display worker execution telemetry
        System.out.printf("[ShortlistWorker | Thread: %-20s] Completed screening %d candidates for [%s] in %d ms | Eligible: %d (Super Dream: %d, Dream: %d, Regular: %d) | Disqualified: %d (Backlog Barred: %d)%n",
                threadName,
                students.size(),
                tier.getDisplayName(),
                executionTimeMs,
                eligibleCount,
                superDreamCount,
                dreamCount,
                regularCount,
                ineligibleCount,
                barredCount);

        return new WorkerResult(tier, threadName, results, students.size(),
                eligibleCount, ineligibleCount, barredCount, executionTimeMs);
    }

    /**
     * Executes the three recruitment tier screenings concurrently across an ExecutorService.
     *
     * @param candidates Full candidate cohort (500+ records)
     * @param executor Thread pool executor
     * @return List of WorkerResult instances for each recruitment tier
     * @throws InterruptedException if thread execution is interrupted
     * @throws ExecutionException if a worker throws an unhandled exception
     */
    public static List<WorkerResult> runParallelScreening(List<Student> candidates, ExecutorService executor)
            throws InterruptedException, ExecutionException {
        long overallStart = System.currentTimeMillis();
        System.out.printf("%n=== Starting Parallel Recruitment Screening on %d candidates across 3 tiers ===%n",
                candidates.size());

        List<ShortlistWorker> tasks = List.of(
                new ShortlistWorker(RecruitmentTier.TIER_1_PRODUCT, candidates),
                new ShortlistWorker(RecruitmentTier.CORE_ENGINEERING, candidates),
                new ShortlistWorker(RecruitmentTier.MASS_RECRUITER, candidates)
        );

        List<Future<WorkerResult>> futures = executor.invokeAll(tasks);
        List<WorkerResult> results = new ArrayList<>();

        for (Future<WorkerResult> f : futures) {
            results.add(f.get());
        }

        long overallTimeMs = System.currentTimeMillis() - overallStart;
        System.out.printf("=== Parallel Screening Completed in %d ms for all 3 categories ===%n%n", overallTimeMs);

        return results;
    }

    /**
     * WorkerResult holds the evaluation results and thread telemetry for one tier.
     */
    public static class WorkerResult {
        private final RecruitmentTier tier;
        private final String threadName;
        private final List<EligibilityResult> results;
        private final int totalCandidates;
        private final int eligibleCount;
        private final int disqualifiedCount;
        private final int backlogBarredCount;
        private final long executionTimeMs;

        public WorkerResult(RecruitmentTier tier, String threadName, List<EligibilityResult> results,
                            int totalCandidates, int eligibleCount, int disqualifiedCount,
                            int backlogBarredCount, long executionTimeMs) {
            this.tier = tier;
            this.threadName = threadName;
            this.results = results;
            this.totalCandidates = totalCandidates;
            this.eligibleCount = eligibleCount;
            this.disqualifiedCount = disqualifiedCount;
            this.backlogBarredCount = backlogBarredCount;
            this.executionTimeMs = executionTimeMs;
        }

        public RecruitmentTier getTier() {
            return tier;
        }

        public String getThreadName() {
            return threadName;
        }

        public List<EligibilityResult> getResults() {
            return results;
        }

        public int getTotalCandidates() {
            return totalCandidates;
        }

        public int getEligibleCount() {
            return eligibleCount;
        }

        public int getDisqualifiedCount() {
            return disqualifiedCount;
        }

        public int getBacklogBarredCount() {
            return backlogBarredCount;
        }

        public long getExecutionTimeMs() {
            return executionTimeMs;
        }

        public double getEligibilityRate() {
            return totalCandidates > 0 ? (eligibleCount * 100.0 / totalCandidates) : 0.0;
        }
    }
}
