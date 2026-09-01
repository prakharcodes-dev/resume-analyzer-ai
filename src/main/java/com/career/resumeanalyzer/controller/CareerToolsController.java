package com.career.resumeanalyzer.controller;

import com.career.resumeanalyzer.service.GitHubAnalyzerService;
import com.career.resumeanalyzer.service.LinkedInAnalyzerService;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analyzer")
public class CareerToolsController {

    @Autowired
    private LinkedInAnalyzerService linkedInAnalyzerService;

    @Autowired
    private GitHubAnalyzerService gitHubAnalyzerService;

    @PostMapping("/linkedin")
    public ResponseEntity<?> analyzeLinkedIn(@RequestBody JsonNode requestBody) {
        if (requestBody == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\": \"Missing request body.\"}");
        }
        return ResponseEntity.ok(linkedInAnalyzerService.analyzeLinkedInProfile(requestBody));
    }

    @PostMapping("/github")
    public ResponseEntity<?> analyzeGitHub(@RequestBody JsonNode requestBody) {
        if (requestBody == null || (!requestBody.has("username") && !requestBody.has("githubUrl"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\": \"Missing username or githubUrl parameter.\"}");
        }

        String username = requestBody.has("username") ? requestBody.get("username").asText() : requestBody.get("githubUrl").asText();
        String targetRole = requestBody.has("targetRole") ? requestBody.get("targetRole").asText() : "Software Engineer";

        return ResponseEntity.ok(gitHubAnalyzerService.analyzeGitHubProfile(username, targetRole));
    }
}
