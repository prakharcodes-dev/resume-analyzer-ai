package com.career.resumeanalyzer.controller;

import com.career.resumeanalyzer.model.UploadedResume;
import com.career.resumeanalyzer.model.User;
import com.career.resumeanalyzer.repository.UploadedResumeRepository;
import com.career.resumeanalyzer.repository.UserRepository;
import com.career.resumeanalyzer.service.RecruiterService;
import com.career.resumeanalyzer.service.ResumeParserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@RestController
@RequestMapping("/api/recruiter")
public class RecruiterController {

    private static final String UPLOAD_DIR = "uploads";
    private static final Long DEFAULT_USER_ID = 1L;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UploadedResumeRepository uploadedResumeRepository;

    @Autowired
    private ResumeParserService resumeParserService;

    @Autowired
    private RecruiterService recruiterService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Upload & process multiple candidate resumes at once.
     * Fault-tolerant: if one resume fails, it continues processing the rest!
     */
    @PostMapping("/upload-batch")
    public ResponseEntity<?> uploadBatch(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\": \"No files selected for upload.\"}");
        }

        User user = userRepository.findById(DEFAULT_USER_ID)
                .orElseGet(() -> userRepository.save(new User("guest@career.com", "ROLE_USER")));

        File uploadFolder = new File(UPLOAD_DIR);
        if (!uploadFolder.exists()) {
            uploadFolder.mkdirs();
        }

        ArrayNode resultsArray = objectMapper.createArrayNode();

        for (MultipartFile file : files) {
            ObjectNode fileResult = objectMapper.createObjectNode();
            String originalFileName = file.getOriginalFilename();
            fileResult.put("fileName", originalFileName);

            if (file.isEmpty()) {
                fileResult.put("status", "FAILED");
                fileResult.put("error", "File is empty.");
                resultsArray.add(fileResult);
                continue;
            }

            String contentType = file.getContentType();
            if (contentType == null || (!contentType.equals("application/pdf") &&
                !contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") &&
                !contentType.equals("application/msword"))) {
                fileResult.put("status", "FAILED");
                fileResult.put("error", "Unsupported file type. Please upload PDF or DOCX.");
                resultsArray.add(fileResult);
                continue;
            }

            try {
                String uniqueFileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 6) + "_" + originalFileName;
                Path filePath = Paths.get(UPLOAD_DIR, uniqueFileName);
                Files.write(filePath, file.getBytes());

                UploadedResume resume = new UploadedResume(
                        user,
                        originalFileName,
                        filePath.toString(),
                        file.getContentType(),
                        file.getSize(),
                        "PENDING"
                );
                resume = uploadedResumeRepository.save(resume);

                try {
                    String rawText = resumeParserService.extractRawText(file);
                    String parsedJson = resumeParserService.parseResume(file);
                    resume.setRawText(rawText);
                    resume.setParsedContent(parsedJson);
                    resume.setParseStatus("SUCCESS");
                    resume = uploadedResumeRepository.save(resume);

                    ObjectNode candidateSummary = recruiterService.getCandidateSummary(resume);
                    candidateSummary.put("status", "SUCCESS");
                    resultsArray.add(candidateSummary);
                } catch (Exception ex) {
                    resume.setParseStatus("FAILED");
                    uploadedResumeRepository.save(resume);
                    fileResult.put("status", "FAILED");
                    fileResult.put("error", "Parsing failed: " + ex.getMessage());
                    resultsArray.add(fileResult);
                }
            } catch (Exception e) {
                fileResult.put("status", "FAILED");
                fileResult.put("error", "Upload error: " + e.getMessage());
                resultsArray.add(fileResult);
            }
        }

        ObjectNode response = objectMapper.createObjectNode();
        response.put("totalUploaded", files.length);
        response.set("candidates", resultsArray);

        return ResponseEntity.ok(response);
    }

    /**
     * Get all processed candidate resumes enriched with ATS score, degree level, experience, and rank.
     */
    @GetMapping("/candidates")
    public ResponseEntity<?> getAllCandidates() {
        List<UploadedResume> resumes = uploadedResumeRepository.findAll();
        List<ObjectNode> candidateList = new ArrayList<>();

        for (UploadedResume r : resumes) {
            if ("SUCCESS".equalsIgnoreCase(r.getParseStatus())) {
                candidateList.add(recruiterService.getCandidateSummary(r));
            }
        }

        // Sort by ATS score descending by default
        candidateList.sort((a, b) -> Integer.compare(b.get("atsScore").asInt(), a.get("atsScore").asInt()));

        // Assign Rank (#1, #2, #3...)
        ArrayNode rankedCandidates = objectMapper.createArrayNode();
        for (int i = 0; i < candidateList.size(); i++) {
            ObjectNode c = candidateList.get(i);
            c.put("rank", i + 1);
            rankedCandidates.add(c);
        }

        ObjectNode response = objectMapper.createObjectNode();
        response.put("totalCandidates", rankedCandidates.size());
        response.set("candidates", rankedCandidates);

        return ResponseEntity.ok(response);
    }

    /**
     * Toggle or update shortlisted status for a candidate in H2 database.
     */
    @PostMapping("/candidates/{id}/shortlist")
    public ResponseEntity<?> toggleShortlist(@PathVariable("id") Long id, @RequestBody(required = false) JsonNode body) {
        Optional<UploadedResume> opt = uploadedResumeRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("{\"error\": \"Candidate not found.\"}");
        }

        UploadedResume resume = opt.get();
        boolean newStatus = !resume.getShortlisted();

        if (body != null && body.has("shortlisted")) {
            newStatus = body.get("shortlisted").asBoolean();
        }

        resume.setShortlisted(newStatus);
        uploadedResumeRepository.save(resume);

        ObjectNode summary = recruiterService.getCandidateSummary(resume);
        return ResponseEntity.ok(summary);
    }

    /**
     * Download comprehensive candidate evaluation report.
     */
    @GetMapping("/candidates/{id}/report")
    public ResponseEntity<byte[]> downloadCandidateReport(@PathVariable("id") Long id) {
        Optional<UploadedResume> opt = uploadedResumeRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        UploadedResume resume = opt.get();
        ObjectNode c = recruiterService.getCandidateSummary(resume);

        StringBuilder sb = new StringBuilder();
        sb.append("=================================================================\n");
        sb.append("                 RECRUITER CANDIDATE EVALUATION REPORT           \n");
        sb.append("=================================================================\n\n");
        sb.append("Candidate Name    : ").append(c.get("candidateName").asText()).append("\n");
        sb.append("Filename          : ").append(c.get("fileName").asText()).append("\n");
        sb.append("Email             : ").append(c.get("email").asText()).append("\n");
        sb.append("Phone             : ").append(c.get("phone").asText()).append("\n");
        sb.append("Upload Date       : ").append(c.get("uploadDate").asText()).append("\n");
        sb.append("Shortlisted       : ").append(c.get("shortlisted").asBoolean() ? "YES [SHORTLISTED]" : "NO").append("\n\n");

        sb.append("--- EVALUATION SCORES & METRICS ---\n");
        sb.append("ATS Score         : ").append(c.get("atsScore").asInt()).append(" / 100\n");
        sb.append("ATS Compatibility : ").append(c.get("atsCompatibility").asText()).append("\n");
        sb.append("Years Experience  : ~").append(c.get("yearsOfExperience").asInt()).append(" years\n");
        sb.append("Degree Level      : ").append(c.get("degreeLevel").asText()).append("\n\n");

        sb.append("--- DETECTED TECHNICAL SKILLS ---\n");
        if (c.has("skills") && c.get("skills").size() > 0) {
            for (JsonNode sk : c.get("skills")) {
                sb.append(" - ").append(sk.asText()).append("\n");
            }
        } else {
            sb.append(" None explicitly detected.\n");
        }
        sb.append("\n");

        sb.append("--- CANDIDATE STRENGTHS ---\n");
        if (c.has("strengths") && c.get("strengths").size() > 0) {
            for (JsonNode st : c.get("strengths")) {
                sb.append(" [+] ").append(st.asText()).append("\n");
            }
        }
        sb.append("\n");

        sb.append("--- WEAKNESSES / GAPS TO VERIFY ---\n");
        if (c.has("weaknesses") && c.get("weaknesses").size() > 0) {
            for (JsonNode wk : c.get("weaknesses")) {
                sb.append(" [-] ").append(wk.asText()).append("\n");
            }
        }
        sb.append("\n");

        sb.append("=================================================================\n");
        sb.append("Generated by AI Resume Analyzer - Recruiter Dashboard System\n");

        byte[] reportBytes = sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        String safeName = c.get("candidateName").asText().replaceAll("[^a-zA-Z0-9_-]", "_");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setContentDispositionFormData("attachment", "Candidate_Report_" + safeName + ".txt");

        return new ResponseEntity<>(reportBytes, headers, HttpStatus.OK);
    }
}
