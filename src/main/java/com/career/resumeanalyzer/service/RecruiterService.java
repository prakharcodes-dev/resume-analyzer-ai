package com.career.resumeanalyzer.service;

import com.career.resumeanalyzer.model.UploadedResume;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class RecruiterService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private ResumeAnalysisService resumeAnalysisService;

    public ObjectNode getCandidateSummary(UploadedResume resume) {
        ObjectNode node = objectMapper.createObjectNode();

        node.put("id", resume.getId());
        node.put("fileName", resume.getFileName());
        node.put("fileSize", resume.getFileSize());
        node.put("fileType", resume.getFileType());
        node.put("uploadDate", resume.getUploadDate() != null ? resume.getUploadDate().toString() : "");
        node.put("shortlisted", resume.getShortlisted());
        node.put("parseStatus", resume.getParseStatus());

        String rawText = resume.getRawText() != null ? resume.getRawText() : "";
        String parsedJson = resume.getParsedContent() != null ? resume.getParsedContent() : "{}";

        JsonNode parsedNode;
        try {
            parsedNode = objectMapper.readTree(parsedJson);
        } catch (Exception e) {
            parsedNode = objectMapper.createObjectNode();
        }

        // Candidate Name
        String candidateName = "";
        if (parsedNode.has("name") && !parsedNode.get("name").isNull() && !parsedNode.get("name").asText().trim().isEmpty()) {
            candidateName = parsedNode.get("name").asText().trim();
        } else {
            // Clean filename as fallback
            String fn = resume.getFileName();
            if (fn.contains(".")) fn = fn.substring(0, fn.lastIndexOf('.'));
            candidateName = fn.replaceAll("[_-]", " ").trim();
        }
        node.put("candidateName", candidateName);

        // Contact details
        node.put("email", parsedNode.has("email") ? parsedNode.get("email").asText() : "");
        node.put("phone", parsedNode.has("phone") ? parsedNode.get("phone").asText() : "");
        node.put("linkedin", parsedNode.has("linkedin") ? parsedNode.get("linkedin").asText() : "");
        node.put("github", parsedNode.has("github") ? parsedNode.get("github").asText() : "");

        // Skills List
        List<String> skills = new ArrayList<>();
        if (parsedNode.has("skills") && parsedNode.get("skills").isArray()) {
            for (JsonNode s : parsedNode.get("skills")) {
                skills.add(s.asText());
            }
        }
        if (skills.isEmpty()) {
            skills = extractSkillsFromRawText(rawText);
        }
        ArrayNode skillsArr = objectMapper.createArrayNode();
        for (String sk : skills) skillsArr.add(sk);
        node.set("skills", skillsArr);

        // Years of Experience
        int experienceYears = calculateExperienceYears(parsedNode, rawText);
        node.put("yearsOfExperience", experienceYears);

        // Education / Degree Level
        String degreeLevel = detectDegreeLevel(parsedNode, rawText);
        node.put("degreeLevel", degreeLevel);

        // Compute ATS Score & Compatibility
        int atsScore = 65;
        String compatibility = "GOOD";
        try {
            String atsReportJson = resumeAnalysisService.getAtsReport(resume, rawText);
            JsonNode atsNode = objectMapper.readTree(atsReportJson);
            if (atsNode.has("atsScore")) atsScore = atsNode.get("atsScore").asInt();
            if (atsNode.has("compatibility")) compatibility = atsNode.get("compatibility").asText();
        } catch (Exception ignored) {}

        node.put("atsScore", atsScore);
        node.put("atsCompatibility", compatibility);

        // Strengths & Weaknesses Summary
        List<String> strengths = extractStrengths(rawText, atsScore, skills.size(), experienceYears);
        List<String> weaknesses = extractWeaknesses(rawText, atsScore, skills.size(), experienceYears);

        ArrayNode strArr = objectMapper.createArrayNode();
        for (String st : strengths) strArr.add(st);
        node.set("strengths", strArr);

        ArrayNode wkArr = objectMapper.createArrayNode();
        for (String wk : weaknesses) wkArr.add(wk);
        node.set("weaknesses", wkArr);

        node.set("parsedDetails", parsedNode);

        return node;
    }

    private List<String> extractSkillsFromRawText(String rawText) {
        List<String> skills = new ArrayList<>();
        if (rawText == null || rawText.isEmpty()) return skills;

        List<String> commonSkills = Arrays.asList(
                "Java", "Python", "C++", "JavaScript", "TypeScript", "HTML", "CSS", "SQL",
                "React", "Angular", "Vue", "Node.js", "Spring Boot", "Docker", "Kubernetes",
                "AWS", "Azure", "GCP", "Git", "PostgreSQL", "MySQL", "MongoDB", "REST API",
                "Microservices", "Machine Learning", "Agile", "Linux", "DevOps", "CI/CD"
        );

        String lower = rawText.toLowerCase();
        for (String cs : commonSkills) {
            String regex = "\\b" + Pattern.quote(cs.toLowerCase()) + "\\b";
            if (Pattern.compile(regex).matcher(lower).find()) {
                skills.add(cs);
            }
        }
        return skills;
    }

    private int calculateExperienceYears(JsonNode parsedNode, String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) return 0;

        // Try pattern like "X years of experience" or "X+ yrs"
        Pattern p = Pattern.compile("(\\d+)\\+?\\s*(?:years?|yrs?)\\b", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(rawText);
        int maxYears = 0;
        while (m.find()) {
            try {
                int y = Integer.parseInt(m.group(1));
                if (y > maxYears && y < 35) {
                    maxYears = y;
                }
            } catch (Exception ignored) {}
        }

        if (maxYears > 0) return maxYears;

        // Otherwise count date ranges from 1990 to present year
        Pattern yearPattern = Pattern.compile("\\b(19\\d{2}|20\\d{2})\\b");
        Matcher ym = yearPattern.matcher(rawText);
        int minYear = 3000;
        int maxYear = 0;
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);

        while (ym.find()) {
            try {
                int y = Integer.parseInt(ym.group(1));
                if (y >= 1990 && y <= currentYear) {
                    if (y < minYear) minYear = y;
                    if (y > maxYear) maxYear = y;
                }
            } catch (Exception ignored) {}
        }

        if (maxYear > 0 && minYear < 3000 && maxYear > minYear) {
            int diff = maxYear - minYear;
            if (diff > 0 && diff < 35) return diff;
        }

        // Count parsed experience entries
        if (parsedNode.has("experience") && parsedNode.get("experience").isArray()) {
            int count = parsedNode.get("experience").size();
            if (count > 0) return count * 2; // rough estimate ~2 yrs per job
        }

        return 0;
    }

    private String detectDegreeLevel(JsonNode parsedNode, String rawText) {
        String text = (rawText + " " + (parsedNode != null ? parsedNode.toString() : "")).toLowerCase();

        if (text.contains("ph.d") || text.contains("phd") || text.contains("doctorate") || text.contains("doctor of philosophy")) {
            return "PhD";
        }
        if (text.contains("master") || text.contains("m.s") || text.contains("ms degree") || text.contains("mtech") || text.contains("m.tech") || text.contains("mba")) {
            return "Master's";
        }
        if (text.contains("bachelor") || text.contains("b.s") || text.contains("bs degree") || text.contains("btech") || text.contains("b.tech") || text.contains("b.e") || text.contains("degree")) {
            return "Bachelor's";
        }
        if (text.contains("associate") || text.contains("diploma")) {
            return "Associate / Diploma";
        }

        return "High School / Other";
    }

    private List<String> extractStrengths(String rawText, int atsScore, int skillCount, int expYears) {
        List<String> str = new ArrayList<>();
        if (atsScore >= 80) str.add("High ATS Score (" + atsScore + "/100) with excellent section formatting.");
        if (skillCount >= 6) str.add("Strong Technical Skills Density (" + skillCount + "+ core technologies detected).");
        if (expYears >= 3) str.add("Substantial Industry Experience (~" + expYears + " years documented).");
        if (rawText.toLowerCase().contains("github.com") || rawText.toLowerCase().contains("linkedin.com")) str.add("Complete Web Presence & Online Profiles.");
        if (str.isEmpty()) str.add("Structured Resume Layout with clean section headings.");
        return str;
    }

    private List<String> extractWeaknesses(String rawText, int atsScore, int skillCount, int expYears) {
        List<String> wk = new ArrayList<>();
        if (atsScore < 70) wk.add("ATS Score below recommended target threshold (" + atsScore + "/100).");
        if (skillCount < 4) wk.add("Low skill count (" + skillCount + " skills detected). Consider listing more tools.");
        if (expYears == 0) wk.add("Unstated or unquantified years of work experience.");
        if (!rawText.contains("•") && !rawText.contains("-")) wk.add("Lacks bullet point formatting for experience items.");
        if (wk.isEmpty()) wk.add("No critical structural gaps detected.");
        return wk;
    }
}
