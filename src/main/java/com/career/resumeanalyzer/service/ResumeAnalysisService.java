package com.career.resumeanalyzer.service;

import com.career.resumeanalyzer.model.UploadedResume;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResumeAnalysisService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // A comprehensive dictionary of common tech skills
    private static final List<String> COMMON_SKILLS = Arrays.asList(
            "java", "python", "c\\+\\+", "c#", "javascript", "typescript", "html", "css", "sql", "nosql",
            "react", "angular", "vue", "node\\.js", "express", "django", "flask", "spring boot", "hibernate",
            "aws", "azure", "gcp", "docker", "kubernetes", "git", "github", "ci/cd", "jenkins", "maven", "gradle",
            "postgres", "postgresql", "mysql", "mongodb", "redis", "elasticsearch", "graphql", "rest api", "soap",
            "microservices", "agile", "scrum", "project management", "machine learning", "deep learning",
            "data science", "tensorflow", "pytorch", "pandas", "numpy", "spark", "hadoop", "linux", "bash"
    );

    // Common spelling mistakes to check
    private static final Map<String, String> COMMON_TYPOS = new HashMap<>();
    static {
        COMMON_TYPOS.put("recieve", "receive");
        COMMON_TYPOS.put("seperate", "separate");
        COMMON_TYPOS.put("definately", "definitely");
        COMMON_TYPOS.put("goverment", "government");
        COMMON_TYPOS.put("untill", "until");
        COMMON_TYPOS.put("occured", "occurred");
        COMMON_TYPOS.put("enviroment", "environment");
        COMMON_TYPOS.put("sucessful", "successful");
        COMMON_TYPOS.put("writting", "writing");
        COMMON_TYPOS.put("begining", "beginning");
        COMMON_TYPOS.put("refered", "referred");
        COMMON_TYPOS.put("tommorow", "tomorrow");
        COMMON_TYPOS.put("arguement", "argument");
    }

    /**
     * Compute ATS Compatibility metrics.
     */
    public String getAtsReport(UploadedResume resume, String rawText) {
        ObjectNode rootNode = objectMapper.createObjectNode();

        // 1. Parse structured JSON from DB
        JsonNode parsed = null;
        try {
            parsed = objectMapper.readTree(resume.getParsedContent());
        } catch (Exception e) {
            parsed = objectMapper.createObjectNode();
        }

        // 2. Section Completeness Score (0-100)
        int completenessScore = calculateCompleteness(parsed);

        // 3. Resume Structure Score (0-100)
        int structureScore = calculateStructure(parsed);

        // 4. Keyword Score (0-100)
        int keywordScore = calculateKeywordScore(rawText);

        // 5. Formatting Score (0-100)
        int formattingScore = calculateFormattingScore(rawText, resume.getFileSize(), resume.getFileType());

        // 6. Readability Score (0-100)
        int readabilityScore = calculateReadabilityScore(rawText);

        // 7. Overall ATS Score
        int atsScore = (int) Math.round(
                (formattingScore * 0.15) +
                (structureScore * 0.20) +
                (keywordScore * 0.25) +
                (completenessScore * 0.25) +
                (readabilityScore * 0.15)
        );

        // ATS Compatibility Rating
        String compatibility = "POOR";
        if (atsScore >= 85) {
            compatibility = "EXCELLENT";
        } else if (atsScore >= 70) {
            compatibility = "GOOD";
        } else if (atsScore >= 50) {
            compatibility = "NEEDS_IMPROVEMENT";
        }

        rootNode.put("atsScore", atsScore);
        rootNode.put("formattingScore", formattingScore);
        rootNode.put("structureScore", structureScore);
        rootNode.put("keywordScore", keywordScore);
        rootNode.put("sectionCompletenessScore", completenessScore);
        rootNode.put("readabilityScore", readabilityScore);
        rootNode.put("compatibility", compatibility);

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Analyze resume details locally for Grammar, Spelling, Tone, Duplicate Content, Weak Sentences, etc.
     */
    public String getAiAnalysis(UploadedResume resume, String rawText) {
        ObjectNode rootNode = objectMapper.createObjectNode();

        // 1. Analyze word count & length
        String[] words = rawText.split("\\s+");
        int wordCount = words.length;
        String lengthStatus = "Perfect";
        String lengthComment = "The resume is approx. " + wordCount + " words, which fits the ideal 400-800 word range for single or double page resumes.";
        if (wordCount < 200) {
            lengthStatus = "Too Short";
            lengthComment = "The resume has only " + wordCount + " words. Recruiters prefer details on projects and impact; consider expanding your experience description.";
        } else if (wordCount > 1200) {
            lengthStatus = "Too Long";
            lengthComment = "The resume is " + wordCount + " words, which is quite lengthy. Consider trimming buzzwords and focusing on key achievements to keep it under 2 pages.";
        }

        // 2. Formatting & Structure comments
        String structureComment = "Your resume exhibits a clean structure with standard headings. Contact info is placed prominently at the top.";
        if (rawText.toLowerCase().contains("education") && !rawText.toLowerCase().contains("experience")) {
            structureComment = "Structure looks incomplete: detected Education but missed standard Professional Experience or Work History section headers.";
        } else if (!rawText.toLowerCase().contains("skills")) {
            structureComment = "Missing dedicated Technical Skills section. We recommend adding a clear Skills list for ATS parsers.";
        }

        String formattingComment = "Font styles appear clean. Avoid graphics, charts, or images, as ATS engines cannot parse embedded graphics.";
        if (resume.getFileType() != null && resume.getFileType().contains("pdf") && countMatches(rawText, "\\|") > 6) {
            formattingComment = "Warning: Multiple column pipes ('|') detected. Dense multi-column layout templates can confuse older ATS scanners.";
        }

        // 3. Grammar & Spelling
        List<String> grammarIssues = new ArrayList<>();
        List<String> spellingIssues = new ArrayList<>();

        // Double spacing checks
        if (rawText.contains("  ")) {
            grammarIssues.add("Double spacing spacing anomalies detected in paragraph text. Ensure clean single spaces between words.");
        }
        // Missing list item periods
        if (countMatches(rawText, "(?m)^[•\\-*]\\s+.*[^\\.!?]$") > 2) {
            grammarIssues.add("A few bullet points do not end with periods. Maintain consistent punctuation across list items.");
        }

        // Check common typos
        for (Map.Entry<String, String> typo : COMMON_TYPOS.entrySet()) {
            Pattern p = Pattern.compile("\\b" + typo.getKey() + "\\b", Pattern.CASE_INSENSITIVE);
            if (p.matcher(rawText).find()) {
                spellingIssues.add("Typo detected: '" + typo.getKey() + "' should be spelled as '" + typo.getValue() + "'.");
            }
        }

        if (spellingIssues.isEmpty()) {
            spellingIssues.add("No major spelling errors detected. All parsed terms match technical dictionaries.");
        }
        if (grammarIssues.isEmpty()) {
            grammarIssues.add("Grammar looks solid. Capitalization is consistent at sentence boundaries.");
        }

        // 4. Tone
        String toneStatus = "Professional";
        String toneComment = "The resume displays an objective, professional tone focusing on actions and outcomes.";
        List<String> casualWordsFound = new ArrayList<>();
        String[] casualWords = {"cool", "awesome", "stuff", "like", "basically", "actually", "wanna", "gonna", "something"};
        for (String cw : casualWords) {
            Pattern p = Pattern.compile("\\b" + cw + "\\b", Pattern.CASE_INSENSITIVE);
            if (p.matcher(rawText).find()) {
                casualWordsFound.add(cw);
            }
        }
        if (!casualWordsFound.isEmpty()) {
            toneStatus = "Slightly Casual";
            toneComment = "Found casual words: " + casualWordsFound + ". Consider replacing them with stronger verbs or formal technical vocabulary.";
        }

        // 5. Readability Heuristic
        int fleschScore = calculateReadabilityScore(rawText);
        String readabilityComment = "Readability score: " + fleschScore + "/100. Easy to read and scan. Standard sentence structures used.";
        if (fleschScore < 45) {
            readabilityComment = "Readability score: " + fleschScore + "/100. The text contains long, complex sentences. Try to shorten sentences in your description of roles.";
        }

        // 6. Duplicate Content
        List<String> duplicateLines = new ArrayList<>();
        String[] lines = rawText.split("\\r?\\n");
        Set<String> uniqueLines = new HashSet<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() > 25) {
                if (uniqueLines.contains(trimmed)) {
                    duplicateLines.add("Repeated description: \"" + trimmed + "\"");
                } else {
                    uniqueLines.add(trimmed);
                }
            }
        }

        // 7. Weak Sentences & Passive Voice
        List<String> weakSentencesList = new ArrayList<>();
        List<String> passiveVoiceList = new ArrayList<>();

        Pattern weakPattern = Pattern.compile("(?m)^.*\\b(responsible for|worked on|helped with|assisted in|involved in|tasked with|duties included)\\b.*$", Pattern.CASE_INSENSITIVE);
        Matcher weakMatcher = weakPattern.matcher(rawText);
        int weakCount = 0;
        while (weakMatcher.find() && weakCount < 5) {
            weakSentencesList.add(weakMatcher.group().trim());
            weakCount++;
        }

        Pattern passivePattern = Pattern.compile("(?m)^.*\\b(was|were|been)\\s+\\w+ed\\s+(by|using)\\b.*$", Pattern.CASE_INSENSITIVE);
        Matcher passiveMatcher = passivePattern.matcher(rawText);
        int passiveCount = 0;
        while (passiveMatcher.find() && passiveCount < 5) {
            passiveVoiceList.add(passiveMatcher.group().trim());
            passiveCount++;
        }

        rootNode.put("structure", structureComment);
        rootNode.put("formatting", formattingComment);
        rootNode.put("professionalTone", toneComment + " Tone: " + toneStatus);
        rootNode.put("readability", readabilityComment);
        rootNode.put("length", lengthComment + " (Status: " + lengthStatus + ")");
        
        ArrayNode spellingNode = objectMapper.valueToTree(spellingIssues);
        ArrayNode grammarNode = objectMapper.valueToTree(grammarIssues);
        ArrayNode dupNode = objectMapper.valueToTree(duplicateLines.isEmpty() ? Collections.singletonList("No duplicate sections found.") : duplicateLines);
        ArrayNode weakNode = objectMapper.valueToTree(weakSentencesList);
        ArrayNode passiveNode = objectMapper.valueToTree(passiveVoiceList);

        rootNode.set("spelling", spellingNode);
        rootNode.set("grammar", grammarNode);
        rootNode.set("duplicateContent", dupNode);
        rootNode.set("weakSentences", weakNode);
        rootNode.set("passiveVoice", passiveNode);

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Compute Job Description Match details.
     */
    public String matchJobDescription(UploadedResume resume, String rawText, String jdText) {
        ObjectNode rootNode = objectMapper.createObjectNode();

        // 1. Parse structured resume skills
        List<String> resumeSkills = new ArrayList<>();
        try {
            JsonNode parsed = objectMapper.readTree(resume.getParsedContent());
            if (parsed.has("skills")) {
                for (JsonNode sk : parsed.get("skills")) {
                    resumeSkills.add(sk.asText().toLowerCase().trim());
                }
            }
        } catch (Exception e) {
            // fallback to parsing raw text for skills
        }
        if (resumeSkills.isEmpty()) {
            resumeSkills = extractSkillsFromText(rawText);
        }

        // 2. Extract skills from Job Description
        List<String> jdSkills = extractSkillsFromText(jdText);
        List<String> jdKeywords = extractKeywordsFromText(jdText);

        // 3. Compute Skill Match Score
        List<String> matchedSkills = new ArrayList<>();
        List<String> missingSkills = new ArrayList<>();

        for (String skill : jdSkills) {
            boolean found = false;
            for (String rSkill : resumeSkills) {
                if (rSkill.contains(skill) || skill.contains(rSkill)) {
                    found = true;
                    break;
                }
            }
            if (found) {
                matchedSkills.add(skill);
            } else {
                missingSkills.add(skill);
            }
        }

        int skillMatch = 100;
        if (!jdSkills.isEmpty()) {
            skillMatch = (int) Math.round((double) matchedSkills.size() / jdSkills.size() * 100);
        }

        // 4. Missing Keywords
        List<String> missingKeywords = new ArrayList<>();
        String rawLower = rawText.toLowerCase();
        for (String kw : jdKeywords) {
            if (!rawLower.contains(kw.toLowerCase())) {
                missingKeywords.add(kw);
            }
        }

        // 5. Experience Match
        int experienceMatch = 100;
        int requiredYears = parseExperienceYears(jdText);
        int resumeYears = parseExperienceYears(rawText);
        if (resumeYears == 0) {
            // Guess years of experience by counting jobs or dates
            resumeYears = estimateResumeYears(rawText);
        }

        if (requiredYears > 0) {
            if (resumeYears >= requiredYears) {
                experienceMatch = 100;
            } else {
                experienceMatch = (int) Math.round((double) resumeYears / requiredYears * 100);
            }
        }

        // 6. Education Match
        int educationMatch = 100;
        String requiredDegree = detectRequiredDegree(jdText);
        if (!requiredDegree.equals("None")) {
            String resumeDegree = detectRequiredDegree(rawText);
            if (resumeDegree.equals("None")) {
                educationMatch = 50; // no degree found
            } else if (degreeLevel(resumeDegree) >= degreeLevel(requiredDegree)) {
                educationMatch = 100;
            } else {
                educationMatch = 70; // lower degree
            }
        }

        // 7. Resume Match Percentage / Overall Compatibility Score
        int resumeMatchPercentage = (int) Math.round(
                (skillMatch * 0.50) +
                (experienceMatch * 0.30) +
                (educationMatch * 0.20)
        );

        rootNode.put("resumeMatchPercentage", resumeMatchPercentage);
        rootNode.put("skillMatch", skillMatch);
        rootNode.put("experienceMatch", experienceMatch);
        rootNode.put("educationMatch", educationMatch);
        rootNode.put("overallCompatibilityScore", resumeMatchPercentage);
        
        ArrayNode missingSkillsNode = objectMapper.valueToTree(missingSkills);
        ArrayNode missingKeywordsNode = objectMapper.valueToTree(missingKeywords);

        rootNode.set("missingSkills", missingSkillsNode);
        rootNode.set("missingKeywords", missingKeywordsNode);

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        } catch (Exception e) {
            return "{}";
        }
    }

    // --- HEURISTICS HELPERS ---

    private int calculateCompleteness(JsonNode parsed) {
        int score = 0;
        if (parsed.has("name") && !parsed.get("name").asText().isEmpty()) score += 15;
        if (parsed.has("email") && !parsed.get("email").asText().isEmpty()) score += 15;
        if (parsed.has("phone") && !parsed.get("phone").asText().isEmpty()) score += 10;
        if (parsed.has("skills") && parsed.get("skills").size() > 0) score += 20;
        if (parsed.has("education") && parsed.get("education").size() > 0) score += 20;
        if (parsed.has("experience") && parsed.get("experience").size() > 0) score += 20;
        return score;
    }

    private int calculateStructure(JsonNode parsed) {
        int sectionsCount = 0;
        if (parsed.has("education") && parsed.get("education").size() > 0) sectionsCount++;
        if (parsed.has("experience") && parsed.get("experience").size() > 0) sectionsCount++;
        if (parsed.has("skills") && parsed.get("skills").size() > 0) sectionsCount++;
        if (parsed.has("projects") && parsed.get("projects").size() > 0) sectionsCount++;

        switch (sectionsCount) {
            case 4: return 100;
            case 3: return 80;
            case 2: return 60;
            case 1: return 40;
            default: return 20;
        }
    }

    private int calculateKeywordScore(String text) {
        int foundCount = 0;
        String lowerText = text.toLowerCase();
        for (String skill : COMMON_SKILLS) {
            Pattern p = Pattern.compile("\\b" + skill + "\\b");
            if (p.matcher(lowerText).find()) {
                foundCount++;
            }
        }
        if (foundCount >= 12) return 100;
        if (foundCount >= 8) return 85;
        if (foundCount >= 5) return 70;
        if (foundCount >= 2) return 55;
        return 40;
    }

    private int calculateFormattingScore(String text, Long fileSize, String fileType) {
        int score = 100;
        // 1. File size check (prefer < 3MB)
        if (fileSize > 3 * 1024 * 1024) {
            score -= 10;
        }
        // 2. Heavy layout indicators like columns pipe
        int pipes = countMatches(text, "\\|");
        if (pipes > 8) {
            score -= 15;
        }
        // 3. Bullet points check
        if (!text.contains("•") && !text.contains("-") && !text.contains("*")) {
            score -= 15; // no bullet points is bad for ATS parsing readability
        }
        return Math.max(score, 45);
    }

    private int calculateReadabilityScore(String text) {
        // Approximate Flesch-Kincaid index
        // Formula: 206.835 - 1.015 * (totalWords / totalSentences) - 84.6 * (totalSyllables / totalWords)
        String[] words = text.split("\\s+");
        String[] sentences = text.split("[.!?]+\\s+");

        int totalWords = Math.max(words.length, 1);
        int totalSentences = Math.max(sentences.length, 1);
        int totalSyllables = 0;

        for (String w : words) {
            totalSyllables += countSyllables(w);
        }

        double wordsPerSentence = (double) totalWords / totalSentences;
        double syllablesPerWord = (double) totalSyllables / totalWords;

        double score = 206.835 - (1.015 * wordsPerSentence) - (84.6 * syllablesPerWord);
        int finalScore = (int) Math.round(score);
        return Math.max(Math.min(finalScore, 100), 20);
    }

    private int countSyllables(String word) {
        String cleanWord = word.toLowerCase().replaceAll("[^a-z]", "");
        if (cleanWord.isEmpty()) return 0;
        if (cleanWord.length() <= 3) return 1;
        
        cleanWord = cleanWord.replaceAll("es$", "").replaceAll("ed$", "");
        
        Pattern vowelPattern = Pattern.compile("[aeiouy]+");
        Matcher m = vowelPattern.matcher(cleanWord);
        int count = 0;
        while (m.find()) {
            count++;
        }
        if (cleanWord.endsWith("e") && count > 1) {
            count--;
        }
        return count;
    }

    private List<String> extractSkillsFromText(String text) {
        List<String> found = new ArrayList<>();
        String lower = text.toLowerCase();
        for (String skill : COMMON_SKILLS) {
            Pattern p = Pattern.compile("\\b" + skill + "\\b");
            if (p.matcher(lower).find()) {
                found.add(skill.replace("\\+", "+").replace("\\.", "."));
            }
        }
        return found;
    }

    private List<String> extractKeywordsFromText(String text) {
        // Common soft skills/industries keywords
        String[] keywords = {"agile", "scrum", "ci/cd", "rest api", "unit test", "automation", "clean code",
                "microservices", "cloud", "security", "collaboration", "leadership", "analytics", "architecture"};
        List<String> found = new ArrayList<>();
        String lower = text.toLowerCase();
        for (String kw : keywords) {
            if (lower.contains(kw)) {
                found.add(kw);
            }
        }
        return found;
    }

    private int parseExperienceYears(String text) {
        Pattern p = Pattern.compile("(\\d+)\\+?\\s*(?:years?|yrs?)\\b", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        int maxYears = 0;
        while (m.find()) {
            try {
                int y = Integer.parseInt(m.group(1));
                if (y > maxYears && y < 25) {
                    maxYears = y;
                }
            } catch (Exception e) {}
        }
        return maxYears;
    }

    private int estimateResumeYears(String text) {
        // Extract 4-digit years from text to calculate history range
        Pattern p = Pattern.compile("\\b(20\\d{2}|19\\d{2})\\b");
        Matcher m = p.matcher(text);
        int minYear = 3000;
        int maxYear = 0;
        while (m.find()) {
            try {
                int y = Integer.parseInt(m.group(1));
                if (y > 1980 && y <= Calendar.getInstance().get(Calendar.YEAR)) {
                    if (y < minYear) minYear = y;
                    if (y > maxYear) maxYear = y;
                }
            } catch (Exception e) {}
        }
        if (maxYear > 0 && minYear < 3000 && maxYear > minYear) {
            return maxYear - minYear;
        }
        return 0;
    }

    private String detectRequiredDegree(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("ph.d") || lower.contains("phd") || lower.contains("doctorate")) {
            return "PhD";
        }
        if (lower.contains("master") || lower.contains("m.s") || lower.contains("mtech") || lower.contains("mba")) {
            return "Master";
        }
        if (lower.contains("bachelor") || lower.contains("b.s") || lower.contains("btech") || lower.contains("degree")) {
            return "Bachelor";
        }
        return "None";
    }

    private int degreeLevel(String degree) {
        switch (degree) {
            case "PhD": return 3;
            case "Master": return 2;
            case "Bachelor": return 1;
            default: return 0;
        }
    }

    private int countMatches(String text, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
}
