package com.career.resumeanalyzer.controller;

import com.career.resumeanalyzer.model.UploadedResume;
import com.career.resumeanalyzer.model.User;
import com.career.resumeanalyzer.model.UserProfile;
import com.career.resumeanalyzer.repository.UploadedResumeRepository;
import com.career.resumeanalyzer.repository.UserProfileRepository;
import com.career.resumeanalyzer.repository.UserRepository;
import com.career.resumeanalyzer.service.ResumeParserService;
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
                String parsedJson = resumeParserService.parseResume(file);
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
}
