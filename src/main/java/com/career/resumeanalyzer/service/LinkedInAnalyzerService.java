package com.career.resumeanalyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LinkedInAnalyzerService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ObjectNode analyzeLinkedInProfile(JsonNode profilePayload) {
        ObjectNode result = objectMapper.createObjectNode();

        String targetRole = getJsonField(profilePayload, "targetRole", "Software Engineer");
        String headline = getJsonField(profilePayload, "headline", "");
        String about = getJsonField(profilePayload, "about", "");
        String skillsText = getJsonField(profilePayload, "skills", "");
        String experienceText = getJsonField(profilePayload, "experience", "");
        String certificationsText = getJsonField(profilePayload, "certifications", "");
        String profileUrl = getJsonField(profilePayload, "profileUrl", "");

        List<String> skillsList = parseSkillsList(skillsText);

        // 1. Category Scores
        int completenessScore = calculateCompletenessScore(headline, about, skillsList, experienceText, certificationsText, profileUrl);
        int headlineScore = calculateHeadlineScore(headline, targetRole);
        int aboutScore = calculateAboutScore(about, targetRole);
        int skillsScore = calculateSkillsScore(skillsList, targetRole);
        int experienceScore = calculateExperienceScore(experienceText);
        int certificationsScore = calculateCertificationsScore(certificationsText, targetRole);

        // Overall Weighted LinkedIn Score (0-100)
        int overallScore = (int) Math.round(
            (completenessScore * 0.20) +
            (headlineScore * 0.20) +
            (aboutScore * 0.20) +
            (skillsScore * 0.15) +
            (experienceScore * 0.15) +
            (certificationsScore * 0.10)
        );

        String grade = getScoreGrade(overallScore);

        result.put("overallScore", overallScore);
        result.put("grade", grade);
        result.put("targetRole", targetRole);

        // Breakdown scores
        ObjectNode categoryScores = objectMapper.createObjectNode();
        addCategoryDetail(categoryScores, "Profile Completeness", completenessScore, 20);
        addCategoryDetail(categoryScores, "Headline Positioning", headlineScore, 20);
        addCategoryDetail(categoryScores, "About / Summary Quality", aboutScore, 20);
        addCategoryDetail(categoryScores, "Skills & Keyword Density", skillsScore, 15);
        addCategoryDetail(categoryScores, "Experience & Impact", experienceScore, 15);
        addCategoryDetail(categoryScores, "Certifications & Validation", certificationsScore, 10);
        result.set("categoryScores", categoryScores);

        // Identified Problems
        ArrayNode problems = objectMapper.createArrayNode();

        if (headline.isEmpty()) {
            problems.add("Headline is completely missing.");
        } else if (headlineScore < 70) {
            problems.add("Headline is too generic or missing key technologies/role titles for " + targetRole + ".");
        }

        if (about.isEmpty()) {
            problems.add("About/Summary section is missing.");
        } else if (about.length() < 150) {
            problems.add("About section is too short (" + about.length() + " chars) to effectively communicate career narrative.");
        }

        if (skillsList.size() < 5) {
            problems.add("Fewer than 5 skills listed (LinkedIn recommends at least 10-15+ skills for recruiter indexing).");
        }

        if (experienceText.isEmpty()) {
            problems.add("Work experience descriptions are missing or empty.");
        } else if (!containsMetrics(experienceText)) {
            problems.add("Work experience lacks quantifiable impact metrics (percentages, team sizes, project scale, or revenue).");
        }

        if (certificationsText.isEmpty()) {
            problems.add("No professional certifications or continuous learning listed.");
        }

        result.set("identifiedProblems", problems);

        // Specific Actionable Suggestions
        ArrayNode suggestions = objectMapper.createArrayNode();

        // Specific Headline suggestion
        if (headline.isEmpty() || headlineScore < 70) {
            String suggestedHeadline = generateSuggestedHeadline(targetRole, skillsList);
            suggestions.add("Headline Optimization: Refine headline to clearly state your role and core tech stack. Example: '" + suggestedHeadline + "'");
        } else {
            suggestions.add("Headline Strength: Good positioning! Consider adding a short value proposition phrase like 'Building scalable cloud applications'.");
        }

        // Specific About suggestion
        if (about.isEmpty()) {
            suggestions.add("About Section Structure: Write a 3-paragraph summary covering (1) your passion and years of experience in " + targetRole + ", (2) core tech stack & key achievements, and (3) what you are currently building or looking for next.");
        } else if (about.length() < 200) {
            suggestions.add("Expand Summary Narrative: Your summary is concise. Elaborate on 2-3 specific projects or engineering problems you solved in " + targetRole + ".");
        } else {
            suggestions.add("Keywords in Summary: Ensure primary keywords for " + targetRole + " appear naturally in your About section to boost recruiter search rankings.");
        }

        // Specific Skills suggestion
        List<String> missingSkills = getMissingTargetSkills(skillsList, targetRole);
        if (!missingSkills.isEmpty()) {
            suggestions.add("Add High-Demand Skills: For target role '" + targetRole + "', consider adding missing industry skills: " + String.join(", ", missingSkills) + ".");
        } else {
            suggestions.add("Reorder Skills: Ensure your top 3 endorsed skills directly match key requirements for " + targetRole + ".");
        }

        // Specific Experience suggestion
        if (!containsMetrics(experienceText)) {
            suggestions.add("Quantify Achievements: Update bullet points using the Action-Verb + Task + Result formula (e.g., 'Engineered microservices API using Spring Boot, reducing response latency by 35%').");
        } else {
            suggestions.add("Experience Impact: Good job using quantifiable metrics! Ensure every role lists the exact technologies used.");
        }

        // Specific Certifications suggestion
        if (certificationsText.isEmpty()) {
            suggestions.add("Add Recognized Credentials: Boost credibility by listing relevant certifications (e.g. AWS Certified Developer, Oracle Java SE, Google Cloud Engineer, or Meta Frontend Developer).");
        }

        result.set("actionableSuggestions", suggestions);

        return result;
    }

    private String getJsonField(JsonNode node, String field, String def) {
        if (node != null && node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asText().trim();
        }
        return def;
    }

    private List<String> parseSkillsList(String text) {
        List<String> list = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return list;
        if (text.startsWith("[")) {
            try {
                JsonNode arr = objectMapper.readTree(text);
                if (arr.isArray()) {
                    for (JsonNode n : arr) list.add(n.asText());
                    return list;
                }
            } catch (Exception ignored) {}
        }
        String[] parts = text.split("[,;\n]");
        for (String p : parts) {
            if (!p.trim().isEmpty()) list.add(p.trim());
        }
        return list;
    }

    private int calculateCompletenessScore(String headline, String about, List<String> skills, String exp, String certs, String url) {
        int score = 0;
        if (!headline.isEmpty()) score += 20;
        if (!about.isEmpty()) score += 20;
        if (skills.size() >= 5) score += 25;
        else if (!skills.isEmpty()) score += 15;
        if (!exp.isEmpty()) score += 20;
        if (!certs.isEmpty()) score += 10;
        if (!url.isEmpty()) score += 5;
        return Math.min(score, 100);
    }

    private int calculateHeadlineScore(String headline, String targetRole) {
        if (headline.isEmpty()) return 20;
        int score = 50;
        String lower = headline.toLowerCase();
        String roleLower = targetRole.toLowerCase();

        // Check if role or core words appear in headline
        String[] roleWords = roleLower.split("\\s+");
        for (String rw : roleWords) {
            if (rw.length() > 2 && lower.contains(rw)) score += 15;
        }

        // Check presence of tech keywords
        if (lower.contains("developer") || lower.contains("engineer") || lower.contains("specialist") || lower.contains("lead")) score += 10;
        if (headline.contains("|") || headline.contains("•") || headline.contains("-") || headline.contains("at")) score += 10; // Structured headline

        return Math.min(score, 100);
    }

    private int calculateAboutScore(String about, String targetRole) {
        if (about.isEmpty()) return 20;
        int score = 40;
        int len = about.length();
        if (len >= 150 && len <= 1500) score += 30;
        else if (len > 50) score += 15;

        if (containsMetrics(about)) score += 15;
        if (about.toLowerCase().contains(targetRole.toLowerCase())) score += 15;

        return Math.min(score, 100);
    }

    private int calculateSkillsScore(List<String> skills, String targetRole) {
        if (skills.isEmpty()) return 20;
        int score = 40;
        int count = skills.size();
        if (count >= 10) score += 30;
        else if (count >= 5) score += 20;
        else score += 10;

        List<String> missing = getMissingTargetSkills(skills, targetRole);
        if (missing.isEmpty()) score += 30;
        else if (missing.size() <= 2) score += 20;
        else score += 10;

        return Math.min(score, 100);
    }

    private int calculateExperienceScore(String exp) {
        if (exp.isEmpty()) return 25;
        int score = 50;
        if (exp.length() > 100) score += 20;
        if (containsMetrics(exp)) score += 20;
        if (exp.toLowerCase().contains("built") || exp.toLowerCase().contains("developed") || exp.toLowerCase().contains("managed") || exp.toLowerCase().contains("led")) score += 10;
        return Math.min(score, 100);
    }

    private int calculateCertificationsScore(String certs, String targetRole) {
        if (certs.isEmpty()) return 40;
        int score = 75;
        if (certs.length() > 20) score += 25;
        return Math.min(score, 100);
    }

    private boolean containsMetrics(String text) {
        if (text == null) return false;
        return text.matches(".*(\\d+%|\\$\\d+|\\d+\\s*(k|M|users|clients|projects|team|ms|seconds|x)).*");
    }

    private String generateSuggestedHeadline(String targetRole, List<String> skills) {
        StringBuilder sb = new StringBuilder();
        sb.append(targetRole);
        if (!skills.isEmpty()) {
            sb.append(" | ");
            List<String> top3 = skills.subList(0, Math.min(skills.size(), 3));
            sb.append(String.join(", ", top3));
        }
        sb.append(" | Building Scalable High-Impact Solutions");
        return sb.toString();
    }

    private List<String> getMissingTargetSkills(List<String> existingSkills, String targetRole) {
        List<String> missing = new ArrayList<>();
        String roleLower = targetRole.toLowerCase();
        Set<String> existingSet = new HashSet<>();
        for (String s : existingSkills) existingSet.add(s.toLowerCase());

        List<String> recommended;
        if (roleLower.contains("ai") || roleLower.contains("machine learning") || roleLower.contains("data")) {
            recommended = Arrays.asList("python", "tensorflow", "pytorch", "scikit-learn", "sql", "pandas");
        } else if (roleLower.contains("full") || roleLower.contains("web") || roleLower.contains("frontend") || roleLower.contains("backend")) {
            recommended = Arrays.asList("java", "spring boot", "react", "javascript", "typescript", "docker", "sql");
        } else if (roleLower.contains("devops") || roleLower.contains("cloud")) {
            recommended = Arrays.asList("aws", "docker", "kubernetes", "ci/cd", "terraform", "linux");
        } else {
            recommended = Arrays.asList("java", "python", "sql", "git", "rest api");
        }

        for (String r : recommended) {
            if (!existingSet.contains(r)) missing.add(r);
        }
        return missing;
    }

    private void addCategoryDetail(ObjectNode parent, String name, int score, int weight) {
        ObjectNode item = objectMapper.createObjectNode();
        item.put("score", score);
        item.put("weight", weight + "%");
        item.put("status", score >= 80 ? "EXCELLENT" : (score >= 60 ? "GOOD" : "NEEDS_IMPROVEMENT"));
        parent.set(name, item);
    }

    private String getScoreGrade(int score) {
        if (score >= 90) return "A+ / All-Star Profile";
        if (score >= 80) return "A / Strong Professional Profile";
        if (score >= 70) return "B+ / Above Average";
        if (score >= 60) return "B / Good Baseline";
        if (score >= 50) return "C / Needs Optimization";
        return "D / Needs Major Updates";
    }
}
