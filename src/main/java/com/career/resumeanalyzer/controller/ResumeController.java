package com.career.resumeanalyzer.controller;

import com.career.resumeanalyzer.model.UploadedResume;
import com.career.resumeanalyzer.model.User;
import com.career.resumeanalyzer.model.UserProfile;
import com.career.resumeanalyzer.repository.UploadedResumeRepository;
import com.career.resumeanalyzer.repository.UserProfileRepository;
import com.career.resumeanalyzer.repository.UserRepository;
import com.career.resumeanalyzer.service.ResumeParserService;
import com.career.resumeanalyzer.service.ResumeAnalysisService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private static final String UPLOAD_DIR = "uploads";
    private static final Long DEFAULT_USER_ID = 1L;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UploadedResumeRepository uploadedResumeRepository;

    @Autowired
    private ResumeParserService resumeParserService;

    @Autowired
    private ResumeAnalysisService resumeAnalysisService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/upload")
    public ResponseEntity<?> uploadResume(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File is empty. Please select a file.");
        }

        // File size validation (max 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("File size exceeds the limit of 10MB.");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("application/pdf") && 
            !contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") &&
            !contentType.equals("application/msword"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Unsupported file type. Please upload a PDF or DOCX file.");
        }

        try {
            // Get default guest user
            User user = userRepository.findById(DEFAULT_USER_ID)
                    .orElseGet(() -> userRepository.save(new User("guest@career.com", "ROLE_USER")));

            // Ensure upload directory exists
            File uploadFolder = new File(UPLOAD_DIR);
            if (!uploadFolder.exists()) {
                uploadFolder.mkdirs();
            }

            // Save file locally
            String originalFileName = file.getOriginalFilename();
            String uniqueFileName = System.currentTimeMillis() + "_" + originalFileName;
            Path filePath = Paths.get(UPLOAD_DIR, uniqueFileName);
            Files.write(filePath, file.getBytes());

            // Create uploaded resume metadata with PENDING status
            UploadedResume resume = new UploadedResume(
                    user,
                    originalFileName,
                    filePath.toString(),
                    file.getContentType(),
                    file.getSize(),
                    "PENDING"
            );
            resume = uploadedResumeRepository.save(resume);

            // Parse resume text using local service
            try {
                String rawText = resumeParserService.extractRawText(file);
                String parsedJson = resumeParserService.parseResume(file);
                resume.setRawText(rawText);
                resume.setParsedContent(parsedJson);
                resume.setParseStatus("SUCCESS");

                // Update active user profile with parsed data
                UserProfile profile = userProfileRepository.findByUserId(DEFAULT_USER_ID)
                        .orElse(new UserProfile(user));

                JsonNode parsedNode = objectMapper.readTree(parsedJson);
                if (parsedNode.has("name") && !parsedNode.get("name").asText().isEmpty()) {
                    profile.setFullName(parsedNode.get("name").asText());
                }
                if (parsedNode.has("email") && !parsedNode.get("email").asText().isEmpty()) {
                    profile.setEmail(parsedNode.get("email").asText());
                }
                if (parsedNode.has("phone") && !parsedNode.get("phone").asText().isEmpty()) {
                    profile.setPhone(parsedNode.get("phone").asText());
                }
                if (parsedNode.has("linkedin") && !parsedNode.get("linkedin").asText().isEmpty()) {
                    profile.setLinkedinUrl(parsedNode.get("linkedin").asText());
                }
                if (parsedNode.has("github") && !parsedNode.get("github").asText().isEmpty()) {
                    profile.setGithubUrl(parsedNode.get("github").asText());
                }
                if (parsedNode.has("portfolio") && !parsedNode.get("portfolio").asText().isEmpty()) {
                    profile.setPortfolioUrl(parsedNode.get("portfolio").asText());
                }
                if (parsedNode.has("skills")) {
                    profile.setSkills(parsedNode.get("skills").toString());
                }
                if (parsedNode.has("education")) {
                    profile.setEducation(parsedNode.get("education").toString());
                }
                if (parsedNode.has("experience")) {
                    profile.setExperience(parsedNode.get("experience").toString());
                }
                if (parsedNode.has("projects")) {
                    profile.setProjects(parsedNode.get("projects").toString());
                }
                userProfileRepository.save(profile);

            } catch (Exception parseException) {
                resume.setParseStatus("FAILED");
                resume.setParsedContent("{\"error\": \"" + parseException.getMessage() + "\"}");
            }

            resume = uploadedResumeRepository.save(resume);
            return ResponseEntity.ok(resume);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file: " + e.getMessage());
        }
    }

    private String getOrExtractRawText(UploadedResume resume) throws IOException {
        if (resume.getRawText() != null && !resume.getRawText().isEmpty()) {
            return resume.getRawText();
        }
        String rawText = resumeParserService.extractRawText(resume.getFilePath(), resume.getFileType());
        resume.setRawText(rawText);
        uploadedResumeRepository.save(resume);
        return rawText;
    }

    @GetMapping("/history")
    public ResponseEntity<List<UploadedResume>> getHistory() {
        List<UploadedResume> resumes = uploadedResumeRepository.findByUserIdOrderByUploadDateDesc(DEFAULT_USER_ID);
        return ResponseEntity.ok(resumes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getResumeDetails(@PathVariable("id") Long id) {
        Optional<UploadedResume> optionalResume = uploadedResumeRepository.findById(id);
        if (optionalResume.isEmpty() || !optionalResume.get().getUser().getId().equals(DEFAULT_USER_ID)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Resume not found.");
        }
        return ResponseEntity.ok(optionalResume.get());
    }

    @GetMapping("/{id}/ats")
    public ResponseEntity<?> getResumeAtsReport(@PathVariable("id") Long id) {
        Optional<UploadedResume> optionalResume = uploadedResumeRepository.findById(id);
        if (optionalResume.isEmpty() || !optionalResume.get().getUser().getId().equals(DEFAULT_USER_ID)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Resume not found.");
        }
        UploadedResume resume = optionalResume.get();
        try {
            String rawText = getOrExtractRawText(resume);
            String report = resumeAnalysisService.getAtsReport(resume, rawText);
            return ResponseEntity.ok(report);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Failed to read resume file: " + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/{id}/ai-analysis")
    public ResponseEntity<?> getResumeAiAnalysis(@PathVariable("id") Long id) {
        Optional<UploadedResume> optionalResume = uploadedResumeRepository.findById(id);
        if (optionalResume.isEmpty() || !optionalResume.get().getUser().getId().equals(DEFAULT_USER_ID)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Resume not found.");
        }
        UploadedResume resume = optionalResume.get();
        try {
            String rawText = getOrExtractRawText(resume);
            String report = resumeAnalysisService.getAiAnalysis(resume, rawText);
            return ResponseEntity.ok(report);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Failed to read resume file: " + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/{id}/suggestions")
    public ResponseEntity<?> getResumeSuggestions(@PathVariable("id") Long id) {
        Optional<UploadedResume> optionalResume = uploadedResumeRepository.findById(id);
        if (optionalResume.isEmpty() || !optionalResume.get().getUser().getId().equals(DEFAULT_USER_ID)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Resume not found.");
        }
        UploadedResume resume = optionalResume.get();
        try {
            String rawText = getOrExtractRawText(resume);
            String report = resumeAnalysisService.getResumeSuggestions(resume, rawText);
            return ResponseEntity.ok(report);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Failed to read resume file: " + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/{id}/skills-analysis")
    public ResponseEntity<?> getSkillsAnalysis(@PathVariable("id") Long id) {
        Optional<UploadedResume> optionalResume = uploadedResumeRepository.findById(id);
        if (optionalResume.isEmpty() || !optionalResume.get().getUser().getId().equals(DEFAULT_USER_ID)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Resume not found.");
        }
        UploadedResume resume = optionalResume.get();
        try {
            String rawText = getOrExtractRawText(resume);
            String report = resumeAnalysisService.getSkillsAnalysis(resume, rawText);
            return ResponseEntity.ok(report);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Failed to read resume file: " + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/{id}/strength-report")
    public ResponseEntity<?> getStrengthReport(@PathVariable("id") Long id) {
        Optional<UploadedResume> optionalResume = uploadedResumeRepository.findById(id);
        if (optionalResume.isEmpty() || !optionalResume.get().getUser().getId().equals(DEFAULT_USER_ID)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Resume not found.");
        }
        UploadedResume resume = optionalResume.get();
        try {
            String rawText = getOrExtractRawText(resume);
            String report = resumeAnalysisService.getStrengthReport(resume, rawText);
            return ResponseEntity.ok(report);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Failed to read resume file: " + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/{id}/grammar-report")
    public ResponseEntity<?> getGrammarReport(@PathVariable("id") Long id) {
        Optional<UploadedResume> optionalResume = uploadedResumeRepository.findById(id);
        if (optionalResume.isEmpty() || !optionalResume.get().getUser().getId().equals(DEFAULT_USER_ID)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Resume not found.");
        }
        UploadedResume resume = optionalResume.get();
        try {
            String rawText = getOrExtractRawText(resume);
            String report = resumeAnalysisService.getGrammarReport(resume, rawText);
            return ResponseEntity.ok(report);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Failed to read resume file: " + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/{id}/download-report/{type}")
    public ResponseEntity<?> downloadReport(@PathVariable("id") Long id, @PathVariable("type") String type) {
        Optional<UploadedResume> optionalResume = uploadedResumeRepository.findById(id);
        if (optionalResume.isEmpty() || !optionalResume.get().getUser().getId().equals(DEFAULT_USER_ID)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Resume not found.");
        }
        UploadedResume resume = optionalResume.get();
        try {
            String rawText = getOrExtractRawText(resume);
            String content = "";
            String reportTitle = "Report";

            switch (type.toLowerCase()) {
                case "strength":
                    content = resumeAnalysisService.getStrengthReport(resume, rawText);
                    reportTitle = "Resume_Strength_Report";
                    break;
                case "grammar":
                    content = resumeAnalysisService.getGrammarReport(resume, rawText);
                    reportTitle = "Grammar_Writing_Report";
                    break;
                case "ats":
                    content = resumeAnalysisService.getAtsReport(resume, rawText);
                    reportTitle = "ATS_Readiness_Report";
                    break;
                case "ai":
                    content = resumeAnalysisService.getAiAnalysis(resume, rawText);
                    reportTitle = "AI_Analysis_Report";
                    break;
                default:
                    content = resumeAnalysisService.getStrengthReport(resume, rawText);
                    reportTitle = "Resume_Report";
                    break;
            }

            String filename = resume.getFileName().replaceAll("[^a-zA-Z0-9.-]", "_") + "_" + reportTitle + ".json";
            
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .header("Content-Type", "application/json")
                    .body(content);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to generate report download: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/match")
    public ResponseEntity<?> matchJobDescription(@PathVariable("id") Long id, @RequestBody JsonNode requestBody) {
        Optional<UploadedResume> optionalResume = uploadedResumeRepository.findById(id);
        if (optionalResume.isEmpty() || !optionalResume.get().getUser().getId().equals(DEFAULT_USER_ID)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Resume not found.");
        }
        if (requestBody == null || !requestBody.has("jobDescription")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\": \"Missing jobDescription parameter.\"}");
        }
        String jdText = requestBody.get("jobDescription").asText();
        UploadedResume resume = optionalResume.get();
        try {
            String rawText = getOrExtractRawText(resume);
            String report = resumeAnalysisService.matchJobDescription(resume, rawText, jdText);
            return ResponseEntity.ok(report);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Failed to read resume file: " + e.getMessage() + "\"}");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteResume(@PathVariable("id") Long id) {
        Optional<UploadedResume> optionalResume = uploadedResumeRepository.findById(id);
        if (optionalResume.isEmpty() || !optionalResume.get().getUser().getId().equals(DEFAULT_USER_ID)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Resume not found.");
        }

        UploadedResume resume = optionalResume.get();
        // Delete local file
        try {
            Path filePath = Paths.get(resume.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Log file deletion issue but proceed with DB delete
            System.err.println("Failed to delete file " + resume.getFilePath() + ": " + e.getMessage());
        }

        uploadedResumeRepository.delete(resume);
        return ResponseEntity.ok("Resume deleted successfully.");
    }

    @PostMapping("/compare")
    public ResponseEntity<?> compareResumes(@RequestBody JsonNode requestBody) {
        if (requestBody == null || !requestBody.has("id1") || !requestBody.has("id2")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\": \"Missing id1 or id2 parameter.\"}");
        }
        Long id1 = requestBody.get("id1").asLong();
        Long id2 = requestBody.get("id2").asLong();

        Optional<UploadedResume> opt1 = uploadedResumeRepository.findById(id1);
        Optional<UploadedResume> opt2 = uploadedResumeRepository.findById(id2);

        if (opt1.isEmpty() || opt2.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\": \"One or both resumes not found.\"}");
        }

        try {
            UploadedResume r1 = opt1.get();
            UploadedResume r2 = opt2.get();
            String rawText1 = getOrExtractRawText(r1);
            String rawText2 = getOrExtractRawText(r2);
            String comparison = resumeAnalysisService.compareResumes(r1, rawText1, r2, rawText2);
            return ResponseEntity.ok(comparison);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Failed to read resume files: " + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/cover-letter")
    public ResponseEntity<?> generateCoverLetter(@RequestBody JsonNode requestBody) {
        if (requestBody == null || !requestBody.has("resumeId")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\": \"Missing resumeId parameter.\"}");
        }
        Long id = requestBody.get("resumeId").asLong();
        Optional<UploadedResume> opt = uploadedResumeRepository.findById(id);

        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\": \"Resume not found.\"}");
        }

        String companyName = requestBody.has("companyName") ? requestBody.get("companyName").asText() : "";
        String jobRole = requestBody.has("jobRole") ? requestBody.get("jobRole").asText() : "";
        String jobDescription = requestBody.has("jobDescription") ? requestBody.get("jobDescription").asText() : "";

        try {
            UploadedResume resume = opt.get();
            String rawText = getOrExtractRawText(resume);
            String result = resumeAnalysisService.generateCoverLetter(resume, rawText, companyName, jobRole, jobDescription);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\": \"Failed to read resume file: " + e.getMessage() + "\"}");
        }
    }
}
