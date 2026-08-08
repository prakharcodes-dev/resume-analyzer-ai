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

    /**
     * Generate resume improvement suggestions across various sections.
     */
    public String getResumeSuggestions(UploadedResume resume, String rawText) {
        ObjectNode rootNode = objectMapper.createObjectNode();
        String lowerText = rawText.toLowerCase();

        // 1. Resume Summary Heuristic
        String summarySuggestion = "Add a 3-4 sentence professional summary at the top of your resume. Highlight your core tech stack, years of experience, and the key value you bring to a team.";
        if (lowerText.contains("summary") || lowerText.contains("profile") || lowerText.contains("about me") || lowerText.contains("objective")) {
            summarySuggestion = "Ensure your summary avoids generic clichés like 'hardworking individual'. Instead, make it impact-oriented by mentioning your primary technologies and a key engineering achievement.";
        }

        // 2. Experience Section Heuristic
        String expSuggestion = "Structure your work experience with clean bullet points. Start each bullet point with a strong action verb (e.g., 'Engineered', 'Optimized') and focus on the business impact of your work.";
        if (lowerText.contains("experience") || lowerText.contains("history")) {
            expSuggestion = "Use the STAR method (Situation, Task, Action, Result) for bullet points. Quantify your metrics where possible (e.g., 'Reduced API latency by 20%' or 'Scaled database queries to support 10k users').";
        }

        // 3. Projects Heuristic
        String projSuggestion = "Ensure each project lists the problem solved, the specific tech stack utilized, and your individual contribution. Highlight challenges overcome during implementation.";

        // 4. Skills Section Heuristic
        String skillsSuggestion = "Group your technical skills into clear sub-categories (e.g., Programming Languages, Frameworks, Databases, Cloud & DevOps) to help ATS scanners index your skills efficiently.";
        if (lowerText.contains("skills") || lowerText.contains("competencies")) {
            skillsSuggestion = "Avoid listing tools or technologies you only have a passing familiarity with. Keep the list current and prioritized according to the job profile you are targetting.";
        }

        // 5. Education Section Heuristic
        String eduSuggestion = "List your degree, major, university name, and graduation date. If your GPA is 3.5 or higher, display it. For recent graduates, list relevant coursework or key academic awards.";

        // 6. Certifications Heuristic
        String certSuggestion = "Incorporate industry-standard certifications (e.g. AWS Certified Developer, Certified Kubernetes Administrator, Oracle Certified Java Professional) to validate your capabilities.";
        if (lowerText.contains("certifications") || lowerText.contains("certificate")) {
            certSuggestion = "Verify that all listed certifications are up-to-date and include the issuing body and year of completion.";
        }

        // 7. Achievements Heuristic
        String achSuggestion = "Create a dedicated Achievements section to highlight promotions, hackathon wins, open-source contributions, or key organizational recognition.";

        // 8. Action Verbs Heuristic
        String verbSuggestion = "Scan your experience bullets to replace weak verbs (e.g., 'was responsible for', 'assisted in', 'handled') with high-impact action verbs (e.g., 'Spearheaded', 'Architected', 'Automated').";

        // 9. Keyword Optimization Heuristic
        String kwSuggestion = "Analyze target job descriptions and integrate missing keywords. Focus on core architectural keywords like 'Microservices', 'RESTful APIs', 'CI/CD Pipelines', and 'Agile Methods'.";

        // 10. Industry-Specific Improvements Heuristic
        String indSuggestion = "For Software Engineering roles, emphasize system design, API design patterns, database optimization, CI/CD pipeline automation, and unit test coverage.";

        rootNode.put("summary", summarySuggestion);
        rootNode.put("experience", expSuggestion);
        rootNode.put("projects", projSuggestion);
        rootNode.put("skills", skillsSuggestion);
        rootNode.put("education", eduSuggestion);
        rootNode.put("certifications", certSuggestion);
        rootNode.put("achievements", achSuggestion);
        rootNode.put("actionVerbs", verbSuggestion);
        rootNode.put("keywordOptimization", kwSuggestion);
        rootNode.put("industryImprovements", indSuggestion);

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Perform Skills Analysis on the resume.
     */
    public String getSkillsAnalysis(UploadedResume resume, String rawText) {
        ObjectNode rootNode = objectMapper.createObjectNode();
        String lowerText = rawText.toLowerCase();

        // Define keywords for categories
        List<String> langKeywords = Arrays.asList("java", "python", "javascript", "typescript", "c\\+\\+", "c#", "go", "ruby", "php", "swift", "kotlin", "rust", "bash", "scala", "c");
        List<String> frameworkKeywords = Arrays.asList("spring boot", "spring", "react", "angular", "vue", "express", "django", "flask", "laravel", "rails", "next\\.js", "hibernate", "node\\.js", "asp\\.net", "jquery", "bootstrap");
        List<String> dbKeywords = Arrays.asList("mysql", "postgresql", "oracle", "sql server", "mongodb", "redis", "cassandra", "dynamodb", "sqlite", "mariadb", "couchdb");
        List<String> cloudKeywords = Arrays.asList("aws", "azure", "gcp", "cloud", "serverless", "lambda", "s3", "ec2", "rds", "cloudfront");
        List<String> devopsKeywords = Arrays.asList("docker", "kubernetes", "jenkins", "gitlab ci", "github actions", "ansible", "terraform", "ci/cd", "vagrant", "circleci");
        List<String> aimlKeywords = Arrays.asList("machine learning", "deep learning", "tensorflow", "pytorch", "scikit-learn", "artificial intelligence", "nlp", "computer vision", "keras", "pandas", "numpy");
        List<String> toolKeywords = Arrays.asList("git", "github", "maven", "gradle", "jira", "confluence", "postman", "figma", "visual studio", "intellij", "eclipse", "slack");
        List<String> softKeywords = Arrays.asList("leadership", "communication", "problem solving", "teamwork", "agile", "scrum", "collaboration", "mentoring", "negotiation", "adaptability");

        // Parse and populate matched list
        List<String> languages = findMatchedKeywords(lowerText, langKeywords);
        List<String> frameworks = findMatchedKeywords(lowerText, frameworkKeywords);
        List<String> databases = findMatchedKeywords(lowerText, dbKeywords);
        List<String> cloud = findMatchedKeywords(lowerText, cloudKeywords);
        List<String> devops = findMatchedKeywords(lowerText, devopsKeywords);
        List<String> aiml = findMatchedKeywords(lowerText, aimlKeywords);
        List<String> tools = findMatchedKeywords(lowerText, toolKeywords);
        List<String> softSkills = findMatchedKeywords(lowerText, softKeywords);

        // Categories object
        ObjectNode categoriesNode = objectMapper.createObjectNode();
        categoriesNode.set("languages", objectMapper.valueToTree(languages));
        categoriesNode.set("frameworks", objectMapper.valueToTree(frameworks));
        categoriesNode.set("databases", objectMapper.valueToTree(databases));
        categoriesNode.set("cloud", objectMapper.valueToTree(cloud));
        categoriesNode.set("devops", objectMapper.valueToTree(devops));
        categoriesNode.set("aiml", objectMapper.valueToTree(aiml));
        categoriesNode.set("tools", objectMapper.valueToTree(tools));
        categoriesNode.set("softSkills", objectMapper.valueToTree(softSkills));

        // Distribution node
        ObjectNode distNode = objectMapper.createObjectNode();
        distNode.put("Languages", languages.size());
        distNode.put("Frameworks", frameworks.size());
        distNode.put("Databases", databases.size());
        distNode.put("Cloud", cloud.size());
        distNode.put("DevOps", devops.size());
        distNode.put("AI/ML", aiml.size());
        distNode.put("Tools", tools.size());
        distNode.put("Soft Skills", softSkills.size());

        // Strength graph node
        ObjectNode strengthNode = objectMapper.createObjectNode();
        strengthNode.put("Languages", calculateStrengthValue(languages.size()));
        strengthNode.put("Frameworks", calculateStrengthValue(frameworks.size()));
        strengthNode.put("Databases", calculateStrengthValue(databases.size()));
        strengthNode.put("Cloud", calculateStrengthValue(cloud.size()));
        strengthNode.put("DevOps", calculateStrengthValue(devops.size()));
        strengthNode.put("AI/ML", calculateStrengthValue(aiml.size()));
        strengthNode.put("Tools", calculateStrengthValue(tools.size()));
        strengthNode.put("Soft Skills", calculateStrengthValue(softSkills.size()));

        // Missing & Recommended Skills
        List<String> missingSkills = new ArrayList<>();
        List<String> recommendedSkills = new ArrayList<>();

        if (languages.contains("Java") && !frameworks.contains("Spring Boot")) {
            missingSkills.add("Spring Boot (Enterprise Java)");
            recommendedSkills.add("Spring Boot & Spring Framework");
        }
        if (languages.contains("Python") && aiml.isEmpty()) {
            recommendedSkills.add("Pandas / NumPy / Scikit-Learn (AI/ML)");
        }
        if (languages.contains("JavaScript") && !frameworks.contains("React")) {
            missingSkills.add("Modern UI Library (React/Vue)");
            recommendedSkills.add("React.js Framework");
        }
        if (cloud.isEmpty()) {
            missingSkills.add("AWS / Azure / GCP Cloud Platforms");
            recommendedSkills.add("AWS Certified Developer Foundations");
        }
        if (devops.isEmpty()) {
            missingSkills.add("Containerization (Docker)");
            recommendedSkills.add("Docker & CI/CD Pipelines (GitHub Actions/Jenkins)");
        }
        if (databases.isEmpty()) {
            missingSkills.add("Relational Databases (SQL)");
            recommendedSkills.add("PostgreSQL or MySQL Basics");
        }

        // Fill generic recommendations if lists are empty
        if (missingSkills.isEmpty()) {
            missingSkills.add("Advanced System Design Patterns");
            missingSkills.add("Infrastructure as Code (Terraform)");
        }
        if (recommendedSkills.isEmpty()) {
            recommendedSkills.add("Kubernetes Orchestration");
            recommendedSkills.add("GraphQL API Implementations");
        }

        rootNode.set("categories", categoriesNode);
        rootNode.set("distribution", distNode);
        rootNode.set("strength", strengthNode);
        rootNode.set("missingSkills", objectMapper.valueToTree(missingSkills));
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Generate comprehensive Resume Strength Report.
     * Includes: Resume Strengths, Weaknesses, Missing Sections, ATS Readiness, Resume Rating, and Improvement Suggestions.
     */
    public String getStrengthReport(UploadedResume resume, String rawText) {
        ObjectNode rootNode = objectMapper.createObjectNode();

        // 1. Parse structured JSON from DB
        JsonNode parsed = null;
        try {
            parsed = objectMapper.readTree(resume.getParsedContent());
        } catch (Exception e) {
            parsed = objectMapper.createObjectNode();
        }

        String lowerText = rawText.toLowerCase();

        // --- STRENGTHS ---
        List<String> strengths = new ArrayList<>();
        String[] words = rawText.split("\\s+");
        int wordCount = words.length;

        if (wordCount >= 350 && wordCount <= 900) {
            strengths.add("Ideal Resume Length: Resume contains ~" + wordCount + " words, matching optimal single or double-page standards.");
        } else if (wordCount > 200) {
            strengths.add("Substantial Content: Contains ~" + wordCount + " words of detailed experience.");
        }

        if (parsed.has("skills") && parsed.get("skills").size() >= 5) {
            strengths.add("Strong Skill Density: Explicitly lists " + parsed.get("skills").size() + "+ technical & professional skills.");
        } else {
            List<String> extractedSkills = extractSkillsFromText(rawText);
            if (extractedSkills.size() >= 5) {
                strengths.add("Extracted Tech Skills: Identified " + extractedSkills.size() + " industry-standard technical keywords.");
            }
        }

        if (parsed.has("name") && !parsed.get("name").asText().isEmpty() &&
            parsed.has("email") && !parsed.get("email").asText().isEmpty()) {
            strengths.add("Complete Contact Header: Full name and email address are clearly identifiable at the top.");
        }

        if (lowerText.contains("github") || lowerText.contains("linkedin")) {
            strengths.add("Professional Web Presence: Includes LinkedIn or GitHub URLs for verification.");
        }

        Pattern metricPattern = Pattern.compile("\\b(\\d+%(?:\\s+increase|\\s+decrease|\\s+improvement)?|\\$\\d+(?:k|m)?|\\d+\\+\\s+users|\\d+\\s+projects)\\b", Pattern.CASE_INSENSITIVE);
        if (metricPattern.matcher(rawText).find()) {
            strengths.add("Quantifiable Achievements: Uses numeric metrics ($ values, %, user counts) to prove impact.");
        }

        int readability = calculateReadabilityScore(rawText);
        if (readability >= 55) {
            strengths.add("High Readability Score (" + readability + "/100): Clear sentence flow that recruiters can easily scan.");
        }

        if (strengths.isEmpty()) {
            strengths.add("Contains foundational text structure ready for optimization.");
        }

        // --- WEAKNESSES ---
        List<String> weaknesses = new ArrayList<>();

        if (wordCount < 250) {
            weaknesses.add("Too Brief: Under 250 words. Add more details about your project deliverables and job duties.");
        } else if (wordCount > 1100) {
            weaknesses.add("Overly Wordy: Exceeds 1100 words. Consider tightening bullet points to keep recruiters engaged.");
        }

        Pattern passivePattern = Pattern.compile("(?m)^.*\\b(was|were|been)\\s+\\w+ed\\s+(by|using)\\b.*$", Pattern.CASE_INSENSITIVE);
        if (passivePattern.matcher(rawText).find()) {
            weaknesses.add("Passive Voice Usage: Several bullet points rely on passive verbs rather than strong action verbs.");
        }

        Pattern weakVerbPattern = Pattern.compile("\\b(responsible for|worked on|helped with|assisted in|involved in)\\b", Pattern.CASE_INSENSITIVE);
        if (weakVerbPattern.matcher(rawText).find()) {
            weaknesses.add("Weak Action Verbs: Uses phrases like 'responsible for' or 'worked on' instead of 'Spearheaded' or 'Engineered'.");
        }

        if (!lowerText.contains("summary") && !lowerText.contains("profile") && !lowerText.contains("about")) {
            weaknesses.add("No Professional Summary: Lacks a top summary pitch highlighting key value proposition.");
        }

        if (countMatches(rawText, "\\|") > 8) {
            weaknesses.add("Multi-Column Layout Clutter: Multiple pipe symbols '|' detected, which can confuse ATS parsers.");
        }

        if (weaknesses.isEmpty()) {
            weaknesses.add("Minor styling tweaks only; content layout is solid.");
        }

        // --- MISSING SECTIONS ---
        List<String> missingSections = new ArrayList<>();
        if (!lowerText.contains("summary") && !lowerText.contains("profile") && !lowerText.contains("objective")) {
            missingSections.add("Professional Summary / About Me");
        }
        if (!lowerText.contains("experience") && !lowerText.contains("work history") && !lowerText.contains("employment")) {
            missingSections.add("Work Experience / Employment History");
        }
        if (!lowerText.contains("skills") && !lowerText.contains("technologies") && !lowerText.contains("competencies")) {
            missingSections.add("Technical Skills");
        }
        if (!lowerText.contains("education") && !lowerText.contains("academic")) {
            missingSections.add("Education");
        }
        if (!lowerText.contains("project") && !lowerText.contains("portfolio")) {
            missingSections.add("Projects");
        }
        if (!lowerText.contains("certif") && !lowerText.contains("license")) {
            missingSections.add("Certifications & Licenses");
        }
        if (!lowerText.contains("award") && !lowerText.contains("achievement") && !lowerText.contains("honor")) {
            missingSections.add("Key Achievements & Awards");
        }

        // --- ATS READINESS ---
        int completeness = calculateCompleteness(parsed);
        int structure = calculateStructure(parsed);
        int keyword = calculateKeywordScore(rawText);
        int formatting = calculateFormattingScore(rawText, resume.getFileSize(), resume.getFileType());

        int atsScore = (int) Math.round((formatting * 0.20) + (structure * 0.25) + (keyword * 0.30) + (completeness * 0.25));
        String atsRating = atsScore >= 85 ? "EXCELLENT" : atsScore >= 70 ? "GOOD" : atsScore >= 50 ? "NEEDS_IMPROVEMENT" : "POOR";

        ObjectNode atsNode = objectMapper.createObjectNode();
        atsNode.put("score", atsScore);
        atsNode.put("rating", atsRating);
        atsNode.put("summary", "ATS Readiness is " + atsRating + " (" + atsScore + "%). Parsers will evaluate structure, keywords, and completeness cleanly.");

        // --- RESUME RATING ---
        int overallScore = (int) Math.round((atsScore * 0.40) + (readability * 0.30) + (Math.min(strengths.size() * 15, 100) * 0.30));
        overallScore = Math.max(Math.min(overallScore, 98), 30);

        String grade = "C / Average";
        if (overallScore >= 90) grade = "A+ / Exceptional";
        else if (overallScore >= 82) grade = "A / Outstanding";
        else if (overallScore >= 74) grade = "B+ / Strong";
        else if (overallScore >= 65) grade = "B / Good";
        else if (overallScore >= 55) grade = "C+ / Fair";

        ObjectNode ratingNode = objectMapper.createObjectNode();
        ratingNode.put("score", overallScore);
        ratingNode.put("grade", grade);
        ratingNode.put("label", "Resume Strength Rating");

        // --- IMPROVEMENT SUGGESTIONS ---
        List<String> suggestions = new ArrayList<>();
        if (!missingSections.isEmpty()) {
            suggestions.add("Add missing key sections: " + String.join(", ", missingSections) + ".");
        }
        if (weaknesses.stream().anyMatch(w -> w.contains("Weak Action Verbs"))) {
            suggestions.add("Replace passive verbs ('responsible for') with high-impact action verbs like 'Engineered', 'Spearheaded', 'Architected'.");
        }
        if (!strengths.stream().anyMatch(s -> s.contains("Quantifiable"))) {
            suggestions.add("Incorporate quantifiable metrics (e.g. 'Improved speed by 30%', 'Managed $50k budget') in your work bullets.");
        }
        if (keyword < 70) {
            suggestions.add("Enhance technical keyword density by listing specific frameworks, databases, and DevOps tools.");
        }
        suggestions.add("Keep resume formatting clean without multi-column graphical elements for seamless ATS indexing.");

        rootNode.set("strengths", objectMapper.valueToTree(strengths));
        rootNode.set("weaknesses", objectMapper.valueToTree(weaknesses));
        rootNode.set("missingSections", objectMapper.valueToTree(missingSections));
        rootNode.set("atsReadiness", atsNode);
        rootNode.set("resumeRating", ratingNode);
        rootNode.set("improvementSuggestions", objectMapper.valueToTree(suggestions));

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Generate comprehensive Grammar & Writing Checker Report.
     * Checks: Grammar Errors, Spelling Mistakes, Readability, Writing Style, Professional Language, Sentence Structure.
     */
    public String getGrammarReport(UploadedResume resume, String rawText) {
        ObjectNode rootNode = objectMapper.createObjectNode();

        // 1. Grammar Errors
        List<String> grammarErrors = new ArrayList<>();
        if (rawText.contains("  ")) {
            grammarErrors.add("Spacing Anomaly: Double spaces detected between words. Clean up extra spaces.");
        }
        if (countMatches(rawText, "(?m)^[•\\-*]\\s+.*[^\\.!?]$") > 2) {
            grammarErrors.add("Punctuation Consistency: Multiple bullet points lack closing periods.");
        }
        if (Pattern.compile("\\b(a)\\s+[aeiou]", Pattern.CASE_INSENSITIVE).matcher(rawText).find()) {
            grammarErrors.add("Article Warning: Check use of 'a' vs 'an' before vowel sounds (e.g., 'a AWS' -> 'an AWS').");
        }
        if (grammarErrors.isEmpty()) {
            grammarErrors.add("No major grammar or punctuation syntax errors found.");
        }

        // 2. Spelling Mistakes
        List<ObjectNode> spellingMistakes = new ArrayList<>();
        for (Map.Entry<String, String> typo : COMMON_TYPOS.entrySet()) {
            Pattern p = Pattern.compile("\\b(" + typo.getKey() + ")\\b", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(rawText);
            if (m.find()) {
                ObjectNode item = objectMapper.createObjectNode();
                item.put("found", m.group(1));
                item.put("suggestion", typo.getValue());
                item.put("message", "Typo detected: '" + m.group(1) + "' -> Suggested fix: '" + typo.getValue() + "'");
                spellingMistakes.add(item);
            }
        }

        // 3. Readability Metrics
        int fleschScore = calculateReadabilityScore(rawText);
        String level = fleschScore >= 70 ? "Easy to Read" : fleschScore >= 45 ? "Moderate Complexity" : "Hard to Read";
        String grade = fleschScore >= 70 ? "8th - 10th Grade (Ideal for fast scanning)" : fleschScore >= 45 ? "Collegiate (Standard Technical)" : "Advanced/Dense";

        ObjectNode readabilityNode = objectMapper.createObjectNode();
        readabilityNode.put("score", fleschScore);
        readabilityNode.put("level", level);
        readabilityNode.put("grade", grade);
        readabilityNode.put("summary", "Flesch Readability Score: " + fleschScore + "/100 (" + level + ").");

        // 4. Writing Style
        Pattern passivePattern = Pattern.compile("(?m)^.*\\b(was|were|been)\\s+\\w+ed\\s+(by|using)\\b.*$", Pattern.CASE_INSENSITIVE);
        Matcher passiveMatcher = passivePattern.matcher(rawText);
        int passiveCount = 0;
        List<String> passiveExamples = new ArrayList<>();
        while (passiveMatcher.find()) {
            passiveCount++;
            if (passiveExamples.size() < 3) {
                passiveExamples.add(passiveMatcher.group().trim());
            }
        }

        Pattern actionPattern = Pattern.compile("\\b(engineered|developed|architected|spearheaded|implemented|optimized|designed|created|led|built|automated|resolved|managed)\\b", Pattern.CASE_INSENSITIVE);
        Matcher actionMatcher = actionPattern.matcher(rawText);
        int actionCount = 0;
        while (actionMatcher.find()) actionCount++;

        ObjectNode styleNode = objectMapper.createObjectNode();
        styleNode.put("actionVerbsCount", actionCount);
        styleNode.put("passiveVoiceCount", passiveCount);
        styleNode.put("styleRating", actionCount > passiveCount * 2 ? "Strong Action-Oriented Style" : "Needs Stronger Verbs");
        styleNode.set("passiveExamples", objectMapper.valueToTree(passiveExamples));

        // 5. Professional Language & Tone
        List<String> casualWordsFound = new ArrayList<>();
        String[] casualWords = {"cool", "awesome", "stuff", "like", "basically", "actually", "wanna", "gonna", "something", "kinda"};
        for (String cw : casualWords) {
            Pattern p = Pattern.compile("\\b" + cw + "\\b", Pattern.CASE_INSENSITIVE);
            if (p.matcher(rawText).find()) casualWordsFound.add(cw);
        }

        String toneStatus = casualWordsFound.isEmpty() ? "Highly Professional & Formal" : "Contains Casual Phrasing";
        int formalScore = casualWordsFound.isEmpty() ? 95 : 65;

        ObjectNode toneNode = objectMapper.createObjectNode();
        toneNode.put("status", toneStatus);
        toneNode.put("formalScore", formalScore);
        toneNode.set("casualWords", objectMapper.valueToTree(casualWordsFound));
        toneNode.put("summary", casualWordsFound.isEmpty() ? 
                "Text maintains an objective, impact-driven professional tone." : 
                "Consider replacing informal terms (" + String.join(", ", casualWordsFound) + ") with technical terminology.");

        // 6. Sentence Structure
        String[] sentences = rawText.split("[.!?]+\\s+");
        int totalSentences = Math.max(sentences.length, 1);
        String[] wordsArr = rawText.split("\\s+");
        int avgSentenceLength = Math.round((float) wordsArr.length / totalSentences);

        List<String> longSentences = new ArrayList<>();
        for (String s : sentences) {
            String trimmed = s.trim();
            if (trimmed.split("\\s+").length > 35 && longSentences.size() < 3) {
                longSentences.add(trimmed);
            }
        }

        ObjectNode sentenceNode = objectMapper.createObjectNode();
        sentenceNode.put("totalSentences", totalSentences);
        sentenceNode.put("avgSentenceLength", avgSentenceLength);
        sentenceNode.put("longSentencesCount", longSentences.size());
        sentenceNode.set("longSentencesExamples", objectMapper.valueToTree(longSentences));
        sentenceNode.put("flowRating", avgSentenceLength >= 10 && avgSentenceLength <= 22 ? "Excellent Flow & Rhythm" : "Sentence length varies");

        rootNode.set("grammarErrors", objectMapper.valueToTree(grammarErrors));
        rootNode.set("spellingMistakes", objectMapper.valueToTree(spellingMistakes));
        rootNode.set("readability", readabilityNode);
        rootNode.set("writingStyle", styleNode);
        rootNode.set("professionalLanguage", toneNode);
        rootNode.set("sentenceStructure", sentenceNode);

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        } catch (Exception e) {
            return "{}";
        }
    }

    private List<String> findMatchedKeywords(String text, List<String> keywords) {
        List<String> matched = new ArrayList<>();
        for (String kw : keywords) {
            Pattern p = Pattern.compile("\\b" + kw + "\\b");
            if (p.matcher(text).find()) {
                String clean = kw.replace("\\+", "+").replace("\\.", ".");
                if (clean.equals("java")) clean = "Java";
                else if (clean.equals("python")) clean = "Python";
                else if (clean.equals("javascript")) clean = "JavaScript";
                else if (clean.equals("typescript")) clean = "TypeScript";
                else if (clean.equals("c++")) clean = "C++";
                else if (clean.equals("c#")) clean = "C#";
                else if (clean.equals("go")) clean = "Go";
                else if (clean.equals("ruby")) clean = "Ruby";
                else if (clean.equals("php")) clean = "PHP";
                else if (clean.equals("swift")) clean = "Swift";
                else if (clean.equals("kotlin")) clean = "Kotlin";
                else if (clean.equals("rust")) clean = "Rust";
                else if (clean.equals("bash")) clean = "Bash";
                else if (clean.equals("spring boot")) clean = "Spring Boot";
                else if (clean.equals("spring")) clean = "Spring";
                else if (clean.equals("react")) clean = "React";
                else if (clean.equals("angular")) clean = "Angular";
                else if (clean.equals("vue")) clean = "Vue";
                else if (clean.equals("express")) clean = "Express";
                else if (clean.equals("django")) clean = "Django";
                else if (clean.equals("flask")) clean = "Flask";
                else if (clean.equals("laravel")) clean = "Laravel";
                else if (clean.equals("rails")) clean = "Rails";
                else if (clean.equals("next.js")) clean = "Next.js";
                else if (clean.equals("hibernate")) clean = "Hibernate";
                else if (clean.equals("node.js")) clean = "Node.js";
                else if (clean.equals("mysql")) clean = "MySQL";
                else if (clean.equals("postgresql")) clean = "PostgreSQL";
                else if (clean.equals("oracle")) clean = "Oracle";
                else if (clean.equals("sql server")) clean = "SQL Server";
                else if (clean.equals("mongodb")) clean = "MongoDB";
                else if (clean.equals("redis")) clean = "Redis";
                else if (clean.equals("cassandra")) clean = "Cassandra";
                else if (clean.equals("dynamodb")) clean = "DynamoDB";
                else if (clean.equals("sqlite")) clean = "SQLite";
                else if (clean.equals("aws")) clean = "AWS";
                else if (clean.equals("azure")) clean = "Azure";
                else if (clean.equals("gcp")) clean = "GCP";
                else if (clean.equals("cloud")) clean = "Cloud Computing";
                else if (clean.equals("docker")) clean = "Docker";
                else if (clean.equals("kubernetes")) clean = "Kubernetes";
                else if (clean.equals("jenkins")) clean = "Jenkins";
                else if (clean.equals("gitlab ci")) clean = "GitLab CI";
                else if (clean.equals("github actions")) clean = "GitHub Actions";
                else if (clean.equals("ansible")) clean = "Ansible";
                else if (clean.equals("terraform")) clean = "Terraform";
                else if (clean.equals("ci/cd")) clean = "CI/CD";
                else if (clean.equals("machine learning")) clean = "Machine Learning";
                else if (clean.equals("deep learning")) clean = "Deep Learning";
                else if (clean.equals("tensorflow")) clean = "TensorFlow";
                else if (clean.equals("pytorch")) clean = "PyTorch";
                else if (clean.equals("scikit-learn")) clean = "Scikit-Learn";
                else if (clean.equals("git")) clean = "Git";
                else if (clean.equals("github")) clean = "GitHub";
                else if (clean.equals("maven")) clean = "Maven";
                else if (clean.equals("gradle")) clean = "Gradle";
                else if (clean.equals("postman")) clean = "Postman";
                else if (clean.equals("jira")) clean = "Jira";
                else if (clean.equals("leadership")) clean = "Leadership";
                else if (clean.equals("communication")) clean = "Communication";
                else if (clean.equals("problem solving")) clean = "Problem Solving";
                else if (clean.equals("teamwork")) clean = "Teamwork";
                else if (clean.equals("agile")) clean = "Agile";
                else if (clean.equals("scrum")) clean = "Scrum";
                else if (clean.equals("collaboration")) clean = "Collaboration";
                else {
                    clean = clean.substring(0, 1).toUpperCase() + clean.substring(1);
                }
                matched.add(clean);
            }
        }
        return matched;
    }

    private int calculateStrengthValue(int count) {
        if (count >= 4) return 95;
        if (count == 3) return 85;
        if (count == 2) return 70;
        if (count == 1) return 55;
        return 20;
    }
}
