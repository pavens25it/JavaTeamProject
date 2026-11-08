package com.kec.placement.model;

/**
 * PlacementBand represents the official KEC placement achievement tiers and ranks.
 * 
 * Rules:
 * - Super Dream: 85 - 100 Aggregate -> Rank A
 * - Dream:       70 - 84  Aggregate -> Rank B
 * - Regular:     50 - 69  Aggregate -> Rank C
 * - Ineligible:  Below aggregate cutoff or backlog barred -> Rank F
 */
public enum PlacementBand {
    SUPER_DREAM("Super Dream", "A", 85.0, 100.0),
    DREAM("Dream", "B", 70.0, 84.99),
    REGULAR("Regular", "C", 50.0, 69.99),
    INELIGIBLE("Ineligible", "F", 0.0, 49.99);

    private final String bandName;
    private final String rank;
    private final double minScore;
    private final double maxScore;

    PlacementBand(String bandName, String rank, double minScore, double maxScore) {
        this.bandName = bandName;
        this.rank = rank;
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    public String getBandName() {
        return bandName;
    }

    public String getRank() {
        return rank;
    }

    public double getMinScore() {
        return minScore;
    }

    public double getMaxScore() {
        return maxScore;
    }

    @Override
    public String toString() {
        return String.format("%s (Rank %s)", bandName, rank);
    }
}
