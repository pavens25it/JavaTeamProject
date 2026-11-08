package com.kec.placement;

import com.kec.placement.model.RecruitmentTier;
import com.kec.placement.model.Student;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying ShortlistWorker multithreaded screening on 500+ candidates.
 */
class ShortlistWorkerTest {

    @Test
    @DisplayName("Should screen 500+ candidates concurrently across 3 recruitment tiers")
    void testConcurrentScreening(@TempDir Path tempDir) throws IOException, ExecutionException, InterruptedException {
        FileHandler handler = new FileHandler();
        Path candidateCsv = tempDir.resolve("500_candidates.csv");

        // Generate 550 synthetic candidates (exceeding 500 requirement)
        handler.generateSyntheticCandidateCsv(candidateCsv, 550);
        List<Student> candidates = handler.importStudentsFromCsv(candidateCsv);
        assertEquals(550, candidates.size());

        // Create thread pool with 3 dedicated threads
        ExecutorService executor = Executors.newFixedThreadPool(3);

        try {
            List<ShortlistWorker.WorkerResult> results = ShortlistWorker.runParallelScreening(candidates, executor);

            assertEquals(3, results.size());

            for (ShortlistWorker.WorkerResult wr : results) {
                assertNotNull(wr.getTier());
                assertNotNull(wr.getThreadName());
                assertTrue(wr.getExecutionTimeMs() >= 0);
                assertEquals(550, wr.getTotalCandidates());
                assertEquals(550, wr.getResults().size());
                assertEquals(550, wr.getEligibleCount() + wr.getDisqualifiedCount());
                assertTrue(wr.getBacklogBarredCount() >= 0);
                assertTrue(wr.getEligibilityRate() >= 0.0 && wr.getEligibilityRate() <= 100.0);
            }

            // Verify all 3 tiers are evaluated
            assertTrue(results.stream().anyMatch(r -> r.getTier() == RecruitmentTier.TIER_1_PRODUCT));
            assertTrue(results.stream().anyMatch(r -> r.getTier() == RecruitmentTier.CORE_ENGINEERING));
            assertTrue(results.stream().anyMatch(r -> r.getTier() == RecruitmentTier.MASS_RECRUITER));

        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }
}
