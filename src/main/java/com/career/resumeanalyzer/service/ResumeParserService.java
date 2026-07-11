package com.career.resumeanalyzer.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResumeParserService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Extracts text from the uploaded file based on its content type and parses it into structured JSON.
     */
    public String parseResume(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        String rawText = "";

        if (contentType != null && contentType.equals("application/pdf")) {
            rawText = extractTextFromPdf(file.getBytes());
        } else if (contentType != null && (contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") 
                || contentType.equals("application/msword"))) {
            rawText = extractTextFromDocx(file.getInputStream());
        } else {
            throw new IllegalArgumentException("Unsupported file type. Please upload a PDF or DOCX file.");
        }

        return structureRawText(rawText, file.getOriginalFilename());
    }

    /**
     * Extracts raw text from a saved file on disk.
     */
    public String extractRawText(String filePath, String contentType) throws IOException {
        java.io.File file = new java.io.File(filePath);
        if (!file.exists()) {
            throw new java.io.FileNotFoundException("File not found at: " + filePath);
        }
        if (contentType != null && contentType.equals("application/pdf")) {
            return extractTextFromPdf(java.nio.file.Files.readAllBytes(file.toPath()));
        } else if (contentType != null && (contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") 
                || contentType.equals("application/msword"))) {
            try (java.io.InputStream is = java.nio.file.Files.newInputStream(file.toPath())) {
                return extractTextFromDocx(is);
            }
        } else {
            throw new IllegalArgumentException("Unsupported file type: " + contentType);
        }
    }

    private String extractTextFromPdf(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractTextFromDocx(InputStream inputStream) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(inputStream)) {
            XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
            return extractor.getText();
        }
    }

    /**
     * Parses raw text into structured JSON.
     */
    private String structureRawText(String rawText, String originalFilename) {
        ObjectNode rootNode = objectMapper.createObjectNode();

        // 1. Extract contact details via Regex
        String email = findPattern(rawText, "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}");
        String phone = findPattern(rawText, "(?:\\+?\\d{1,3}[- ]?)?\\(?\\d{3}\\)?[- ]?\\d{3}[- ]?\\d{4}");
        String linkedin = findPattern(rawText, "linkedin\\.com/in/[a-zA-Z0-9-_/]+");
        String github = findPattern(rawText, "github\\.com/[a-zA-Z0-9-_/]+");
        
        // Find portfolio link: any link that is not github or linkedin
        String portfolio = "";
        Pattern urlPattern = Pattern.compile("https?://(www\\.)?([^\\s/$.?#].[^\\s]*)");
        Matcher urlMatcher = urlPattern.matcher(rawText);
        while (urlMatcher.find()) {
            String url = urlMatcher.group();
            if (!url.contains("linkedin") && !url.contains("github")) {
                portfolio = url;
                break;
            }
        }

        // 2. Guess Name from first few non-empty lines
        String name = guessName(rawText, originalFilename);

        rootNode.put("name", name);
        rootNode.put("email", email.isEmpty() ? "" : email);
        rootNode.put("phone", phone.isEmpty() ? "" : phone);
        rootNode.put("linkedin", linkedin.isEmpty() ? "" : linkedin);
        rootNode.put("github", github.isEmpty() ? "" : github);
        rootNode.put("portfolio", portfolio.isEmpty() ? "" : portfolio);

        // 3. Section Extraction
        String[] lines = rawText.split("\\r?\\n");
        List<String> skillsList = new ArrayList<>();
        List<Map<String, Object>> educationList = new ArrayList<>();
        List<Map<String, Object>> experienceList = new ArrayList<>();
        List<Map<String, Object>> projectsList = new ArrayList<>();

        String currentSection = "";
        List<String> sectionLines = new ArrayList<>();

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) continue;

            String sectionHeader = detectSectionHeader(trimmedLine);
            if (sectionHeader != null) {
                // Process previous section
                processSection(currentSection, sectionLines, skillsList, educationList, experienceList, projectsList);
                // Switch to new section
                currentSection = sectionHeader;
                sectionLines.clear();
            } else if (!currentSection.isEmpty()) {
                sectionLines.add(trimmedLine);
            }
        }
        // Process final section
        processSection(currentSection, sectionLines, skillsList, educationList, experienceList, projectsList);

        // Fallback for skills if empty: scan general text for common keywords
        if (skillsList.isEmpty()) {
            skillsList = extractKeywords(rawText);
        }

        // Convert Lists to Jackson JSON nodes
        ArrayNode skillsNode = objectMapper.valueToTree(skillsList);
        ArrayNode educationNode = objectMapper.valueToTree(educationList);
        ArrayNode experienceNode = objectMapper.valueToTree(experienceList);
        ArrayNode projectsNode = objectMapper.valueToTree(projectsList);

        rootNode.set("skills", skillsNode);
        rootNode.set("education", educationNode);
        rootNode.set("experience", experienceNode);
        rootNode.set("projects", projectsNode);

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String findPattern(String text, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group().trim();
        }
        return "";
    }

    private String guessName(String rawText, String filename) {
        String[] lines = rawText.split("\\r?\\n");
        for (int i = 0; i < Math.min(lines.length, 5); i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            // Ignore contact info lines
            if (line.contains("@") || line.contains("http") || line.contains(".com") || line.matches(".*\\d{5,}.*")) {
                continue;
            }
            // Check if it looks like a name (2 to 4 alphabetic words)
            if (line.matches("^[a-zA-Z\\s]{3,35}$")) {
                return line;
            }
        }
        // Fallback: clean up filename
        if (filename != null && filename.contains(".")) {
            String namePart = filename.substring(0, filename.lastIndexOf('.'));
            // Remove typical words like "Resume", "CV", "_", "-"
            namePart = namePart.replaceAll("(?i)(resume|cv|latest|updated|developer|engineer|\\d+)", "")
                    .replace("-", " ")
                    .replace("_", " ")
                    .trim();
            if (!namePart.isEmpty()) {
                return namePart;
            }
        }
        return "Candidate Name";
    }

    private String detectSectionHeader(String line) {
        String lower = line.toLowerCase().trim();
        // Remove trailing and leading markdown or bullets
        lower = lower.replaceAll("^[\\*\\-\\s#]+", "").replaceAll("[\\*\\-\\s#]+$", "");
        
        if (lower.matches("^(education|academic background|studies)$")) {
            return "EDUCATION";
        } else if (lower.matches("^(experience|work experience|employment history|work history|professional experience)$")) {
            return "EXPERIENCE";
        } else if (lower.matches("^(skills|technical skills|key skills|core competencies)$")) {
            return "SKILLS";
        } else if (lower.matches("^(projects|key projects|academic projects)$")) {
            return "PROJECTS";
        }
        return null;
    }

    private void processSection(String sectionName, List<String> lines,
                                List<String> skillsList,
                                List<Map<String, Object>> educationList,
                                List<Map<String, Object>> experienceList,
                                List<Map<String, Object>> projectsList) {
        if (lines.isEmpty()) return;

        switch (sectionName) {
            case "SKILLS":
                for (String line : lines) {
                    // Split on commas, vertical bars, or bullets
                    String[] tokens = line.split("[,|•\\t]|\\s{3,}");
                    for (String token : tokens) {
                        String clean = token.trim();
                        if (!clean.isEmpty() && clean.length() < 30) {
                            skillsList.add(clean);
                        }
                    }
                }
                break;

            case "EDUCATION":
                Map<String, Object> eduItem = new HashMap<>();
                eduItem.put("degree", "");
                eduItem.put("fieldOfStudy", "");
                eduItem.put("institution", "");
                eduItem.put("endDate", "");

                // Guessing details from lines
                for (String line : lines) {
                    String lower = line.toLowerCase();
                    if (eduItem.get("institution").toString().isEmpty() && (lower.contains("university") || lower.contains("college") || lower.contains("institute") || lower.contains("school"))) {
                        eduItem.put("institution", line);
                    } else if (eduItem.get("degree").toString().isEmpty() && (lower.contains("bachelor") || lower.contains("master") || lower.contains("phd") || lower.contains("b.s") || lower.contains("m.s") || lower.contains("b.e") || lower.contains("btech") || lower.contains("mtech"))) {
                        eduItem.put("degree", line);
                    } else if (eduItem.get("endDate").toString().isEmpty() && line.matches(".*(20\\d{2}|19\\d{2}|Present).*")) {
                        eduItem.put("endDate", line);
                    } else if (eduItem.get("fieldOfStudy").toString().isEmpty() && (lower.contains("computer science") || lower.contains("engineering") || lower.contains("business") || lower.contains("finance") || lower.contains("science"))) {
                        eduItem.put("fieldOfStudy", line);
                    }
                }

                // If nothing was mapped, put raw first line
                if (eduItem.get("institution").toString().isEmpty() && !lines.isEmpty()) {
                    eduItem.put("institution", lines.get(0));
                    if (lines.size() > 1) eduItem.put("degree", lines.get(1));
                }

                educationList.add(eduItem);
                break;

            case "EXPERIENCE":
                // Create experience items by grouping lines
                Map<String, Object> expItem = new HashMap<>();
                expItem.put("jobTitle", "");
                expItem.put("company", "");
                expItem.put("startDate", "");
                expItem.put("endDate", "");
                List<String> responsibilities = new ArrayList<>();

                for (String line : lines) {
                    String lower = line.toLowerCase();
                    // Bullet points represent responsibilities
                    if (line.startsWith("•") || line.startsWith("-") || line.startsWith("*") || responsibilities.size() > 0) {
                        responsibilities.add(line.replaceAll("^[•\\-*\\s]+", ""));
                    } else if (expItem.get("jobTitle").toString().isEmpty() && (lower.contains("developer") || lower.contains("engineer") || lower.contains("intern") || lower.contains("manager") || lower.contains("analyst") || lower.contains("lead"))) {
                        expItem.put("jobTitle", line);
                    } else if (expItem.get("company").toString().isEmpty() && !line.matches(".*(20\\d{2}|19\\d{2}|Present).*")) {
                        expItem.put("company", line);
                    } else if (expItem.get("endDate").toString().isEmpty() && line.matches(".*(20\\d{2}|19\\d{2}|Present).*")) {
                        expItem.put("endDate", line);
                    }
                }

                if (expItem.get("jobTitle").toString().isEmpty() && !lines.isEmpty()) {
                    expItem.put("jobTitle", lines.get(0));
                    if (lines.size() > 1) expItem.put("company", lines.get(1));
                }
                expItem.put("responsibilities", responsibilities);
                experienceList.add(expItem);
                break;

            case "PROJECTS":
                Map<String, Object> projItem = new HashMap<>();
                projItem.put("title", "");
                projItem.put("description", "");
                List<String> techList = new ArrayList<>();

                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (i == 0) {
                        projItem.put("title", line);
                    } else if (line.toLowerCase().contains("technologies") || line.toLowerCase().contains("stack") || line.toLowerCase().contains("built with")) {
                        // Extract words
                        String[] tokens = line.split("[,|•\\t:]");
                        for (String token : tokens) {
                            String clean = token.replaceAll("(?i)(technologies|stack|built with)", "").trim();
                            if (!clean.isEmpty()) techList.add(clean);
                        }
                    } else {
                        String currentDesc = projItem.get("description").toString();
                        projItem.put("description", currentDesc.isEmpty() ? line : currentDesc + " " + line);
                    }
                }
                projItem.put("technologies", techList);
                projectsList.add(projItem);
                break;
        }
    }

    private List<String> extractKeywords(String rawText) {
        // Scans the document for common technical keywords
        String[] keywords = {"Java", "Python", "C++", "C#", "JavaScript", "HTML", "CSS", "TypeScript", "React", "Angular", 
                "Vue", "Spring Boot", "Hibernate", "Django", "Flask", "Node.js", "Express", "SQL", "MySQL", "PostgreSQL", 
                "MongoDB", "AWS", "Azure", "Docker", "Kubernetes", "Git", "GitHub", "Maven", "Gradle", "CI/CD", "Machine Learning", 
                "Deep Learning", "TensorFlow", "PyTorch", "Excel", "Project Management", "Agile", "Scrum", "REST API", "JSON"};
        
        List<String> found = new ArrayList<>();
        String lowerText = rawText.toLowerCase();
        for (String kw : keywords) {
            // Find keyword as word boundary
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(kw.toLowerCase()) + "\\b");
            if (pattern.matcher(lowerText).find()) {
                found.add(kw);
            }
        }
        return found;
    }
}
