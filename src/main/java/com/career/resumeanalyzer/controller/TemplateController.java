package com.career.resumeanalyzer.controller;

import com.career.resumeanalyzer.model.UploadedResume;
import com.career.resumeanalyzer.model.UserProfile;
import com.career.resumeanalyzer.repository.UploadedResumeRepository;
import com.career.resumeanalyzer.repository.UserProfileRepository;
import com.career.resumeanalyzer.service.TemplateService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/templates")
public class TemplateController {

    private static final Long DEFAULT_USER_ID = 1L;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UploadedResumeRepository uploadedResumeRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping
    public ResponseEntity<?> getTemplates() {
        return ResponseEntity.ok(templateService.getAvailableTemplates());
    }

    @PostMapping("/render")
    public ResponseEntity<?> renderTemplate(@RequestBody JsonNode requestBody) {
        String templateId = requestBody.has("templateId") ? requestBody.get("templateId").asText() : "ats";
        JsonNode resumeData = null;

        // Check if explicit resumeId is requested
        if (requestBody.has("resumeId") && !requestBody.get("resumeId").isNull() && requestBody.get("resumeId").asLong() > 0) {
            Long resumeId = requestBody.get("resumeId").asLong();
            Optional<UploadedResume> opt = uploadedResumeRepository.findById(resumeId);
            if (opt.isPresent() && opt.get().getParsedContent() != null) {
                try {
                    resumeData = objectMapper.readTree(opt.get().getParsedContent());
                } catch (Exception ignored) {}
            }
        }

        // Fallback to active user profile if no resumeData found
        if (resumeData == null) {
            Optional<UserProfile> profileOpt = userProfileRepository.findByUserId(DEFAULT_USER_ID);
            if (profileOpt.isPresent()) {
                UserProfile profile = profileOpt.get();
                ObjectNode node = objectMapper.createObjectNode();
                node.put("name", profile.getFullName());
                node.put("email", profile.getEmail());
                node.put("phone", profile.getPhone());
                node.put("linkedin", profile.getLinkedinUrl());
                node.put("github", profile.getGithubUrl());
                node.put("portfolio", profile.getPortfolioUrl());
                node.put("summary", "Experienced " + (profile.getFullName() != null ? profile.getFullName() : "Professional") + " specializing in technology and engineering applications.");
                node.put("skills", profile.getSkills() != null ? profile.getSkills() : "[]");
                node.put("education", profile.getEducation() != null ? profile.getEducation() : "[]");
                node.put("experience", profile.getExperience() != null ? profile.getExperience() : "[]");
                node.put("projects", profile.getProjects() != null ? profile.getProjects() : "[]");
                resumeData = node;
            } else {
                resumeData = objectMapper.createObjectNode();
            }
        }

        ObjectNode result = templateService.renderTemplate(templateId, resumeData);
        return ResponseEntity.ok(result);
    }
}
