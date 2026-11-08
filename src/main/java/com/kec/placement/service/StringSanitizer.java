package com.kec.placement.service;

import com.kec.placement.model.EligibilityResult;
import com.kec.placement.model.RecruitmentTier;
import com.kec.placement.model.Student;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * StringSanitizer provides utilities for:
 * 1. Register number validation (e.g. 24ITR001).
 * 2. Input text sanitization and cleaning.
 * 3. Skill keyword parsing using substring() and indexOf().
 * 4. Shortlist table generation using String.format().
 */
public class StringSanitizer {

    /**
     * KEC Register Number Pattern:
     * - 2 digits admission year (e.g. 24)
     * - 2 to 4 uppercase letters department code (e.g. ITR, CSE, ECE, MECH, AIDS)
     * - 3 digits sequential roll number (e.g. 001)
     */
    private static final Pattern REG_NO_PATTERN = Pattern.compile("^[0-9]{2}[A-Z]{2,4}[0-9]{3}$");

    /**
     * Validates whether a register number strictly adheres to the KEC format (e.g., 24ITR001).
     *
     * @param regNo Candidate register number string
     * @return true if valid, false otherwise
     */
    public static boolean isValidRegisterNo(String regNo) {
        if (regNo == null) {
            return false;
        }
        String cleaned = regNo.trim().toUpperCase();
        return REG_NO_PATTERN.matcher(cleaned).matches();
    }

    /**
     * Sanitizes and normalizes a candidate register number to uppercase trimmed format.
     *
     * @param regNo input register number
     * @return normalized register number or empty string if null
     */
    public static String sanitizeRegisterNo(String regNo) {
        if (regNo == null) {
            return "";
        }
        return regNo.trim().toUpperCase();
    }

    /**
     * Sanitizes general text input by stripping control characters and trimming whitespace.
     *
     * @param input Raw text input
     * @return Sanitized string
     */
    public static String sanitize(String input) {
        if (input == null) {
            return "";
        }
        // Remove control characters (except common printable) and trim
        return input.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "").trim();
    }

    /**
     * Parses comma-delimited skill keywords using substring() and indexOf().
     * (Demonstrates required algorithmic String manipulation).
     *
     * @param rawSkills Raw comma-separated skills string (e.g. "Java, Spring Boot, SQL, Docker")
     * @return List of trimmed, non-empty skill tokens
     */
    public static List<String> parseSkills(String rawSkills) {
        if (rawSkills == null || rawSkills.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> skills = new ArrayList<>();
        String text = rawSkills.trim();
        int start = 0;
        int delimiterIndex;

        // Iteratively locate comma positions using indexOf() and slice using substring()
        while ((delimiterIndex = text.indexOf(',', start)) != -1) {
            String token = text.substring(start, delimiterIndex).trim();
            if (!token.isEmpty()) {
                skills.add(token);
            }
            start = delimiterIndex + 1;
        }

        // Handle trailing token after last comma
        if (start < text.length()) {
            String lastToken = text.substring(start).trim();
            if (!lastToken.isEmpty()) {
                skills.add(lastToken);
            }
        }

        return skills;
    }

    /**
     * Searches for a skill keyword within a raw skill string using indexOf().
     *
     * @param rawSkills Raw skills string
     * @param targetSkill Keyword to search for (case-insensitive)
     * @return true if found, false otherwise
     */
    public static boolean containsSkill(String rawSkills, String targetSkill) {
        if (rawSkills == null || targetSkill == null || targetSkill.trim().isEmpty()) {
            return false;
        }
        String lowerRaw = rawSkills.toLowerCase();
        String lowerTarget = targetSkill.trim().toLowerCase();
        return lowerRaw.indexOf(lowerTarget) != -1;
    }

    /**
     * Generates a cleanly aligned ASCII shortlist table using String.format().
     *
     * @param tier Recruitment category evaluated
     * @param results List of eligibility results to render
     * @return Formatted multi-line tabular string
     */
    public static String formatShortlistTable(RecruitmentTier tier, List<EligibilityResult> results) {
        StringBuilder sb = new StringBuilder();
        String line = "+------------+----------------------+------------+--------+--------+---------------+------+----------+%n";
        String headerFmt = "| %-10s | %-20s | %-10s | %-6s | %-6s | %-13s | %-4s | %-8s |%n";
        String rowFmt    = "| %-10s | %-20s | %-10s | %6.2f | %6.2f | %-13s | %-4s | %-8s |%n";

        sb.append(String.format("=========================================================================================%n"));
        sb.append(String.format("                   KEC PLACEMENT SHORTLIST: %s%n", tier.getDisplayName()));
        sb.append(String.format("=========================================================================================%n"));
        sb.append(String.format(line));
        sb.append(String.format(headerFmt, "REG NO", "STUDENT NAME", "DEPT", "AGG %", "CRIT %", "BAND", "RANK", "STATUS"));
        sb.append(String.format(line));

        long eligibleCount = 0;
        if (results != null) {
            for (EligibilityResult r : results) {
                Student s = r.getStudent();
                String regNo = s != null ? s.getRegisterNo() : "N/A";
                String name = s != null ? truncate(s.getFullName(), 20) : "N/A";
                String dept = s != null ? s.getDepartment() : "N/A";
                double agg = r.getAggregateScore() != null ? r.getAggregateScore() : 0.0;
                double crit = r.getCriticalSubjectScore() != null ? r.getCriticalSubjectScore() : 0.0;
                String band = r.getPlacementBand() != null ? r.getPlacementBand().getBandName() : "N/A";
                String rank = r.getPlacementRank() != null ? r.getPlacementRank() : "N/A";
                String status = r.getEligibilityStatus() != null ? r.getEligibilityStatus() : "NE";

                if (r.isEligible()) {
                    eligibleCount++;
                }

                sb.append(String.format(rowFmt, regNo, name, dept, agg, crit, band, rank, status));
            }
        }

        sb.append(String.format(line));
        int total = (results != null) ? results.size() : 0;
        sb.append(String.format(" Summary: Total Screened: %d | Eligible: %d | Disqualified: %d%n",
                total, eligibleCount, total - eligibleCount));
        sb.append(String.format("=========================================================================================%n"));

        return sb.toString();
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen - 3) + "...";
    }
}
