package com.career.resumeanalyzer;

import com.career.resumeanalyzer.model.UploadedResume;
import com.career.resumeanalyzer.model.User;
import com.career.resumeanalyzer.model.UserProfile;
import com.career.resumeanalyzer.repository.UploadedResumeRepository;
import com.career.resumeanalyzer.repository.UserProfileRepository;
import com.career.resumeanalyzer.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:security_tests;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "app.recruiter-registration-key=test-recruiter-key",
        "app.upload-dir=./target/test-uploads"
})
@AutoConfigureMockMvc
@Transactional
class AuthenticationSecurityTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private UserProfileRepository profileRepository;
    @Autowired private UploadedResumeRepository resumeRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void registersWithBCryptAndRejectsDuplicateEmail() throws Exception {
        mockMvc.perform(withCsrf(post("/api/auth/register"))
                        .contentType("application/json")
                        .content("{\"email\":\"owner@example.com\",\"password\":\"correct-horse-123\"}"))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail("owner@example.com").orElseThrow();
        org.junit.jupiter.api.Assertions.assertNotEquals("correct-horse-123", user.getPasswordHash());
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("correct-horse-123", user.getPasswordHash()));

        mockMvc.perform(withCsrf(post("/api/auth/register"))
                        .contentType("application/json")
                        .content("{\"email\":\"owner@example.com\",\"password\":\"correct-horse-123\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void loginRejectsWrongPasswordAndAllowsAuthenticatedProfileRequest() throws Exception {
        User user = new User("login@example.com", "ROLE_USER");
        user.setPasswordHash(passwordEncoder.encode("correct-horse-123"));
        userRepository.save(user);

        mockMvc.perform(withCsrf(post("/api/auth/login"))
                        .contentType("application/json")
                        .content("{\"email\":\"login@example.com\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());

        var login = mockMvc.perform(withCsrf(post("/api/auth/login"))
                        .contentType("application/json")
                        .content("{\"email\":\"login@example.com\",\"password\":\"correct-horse-123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(get("/api/profile").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("login@example.com"));
    }

    @Test
    void privateApisRequireLoginAndRecruiterEndpointsRequireRecruiterRole() throws Exception {
        mockMvc.perform(get("/api/resumes/history")).andExpect(status().isUnauthorized());
        User user = new User("regular@example.com", "ROLE_USER");
        user.setPasswordHash(passwordEncoder.encode("correct-horse-123"));
        userRepository.save(user);
        MockHttpSession session = login("regular@example.com");
        mockMvc.perform(get("/api/recruiter/candidates").session(session)).andExpect(status().isForbidden());
    }

    @Test
    void resumeIdsCannotCrossAccountBoundariesForReadDownloadOrDelete() throws Exception {
        User owner = user("owner2@example.com");
        User other = user("other@example.com");
        UploadedResume resume = resumeRepository.save(new UploadedResume(other, "private.pdf", "private-file", "application/pdf", 5L, "SUCCESS"));
        MockHttpSession session = login(owner.getEmail());

        mockMvc.perform(get("/api/resumes/{id}", resume.getId()).session(session)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/resumes/{id}/download-report/ats", resume.getId()).session(session)).andExpect(status().isNotFound());
        mockMvc.perform(withCsrf(post("/api/templates/render").session(session))
                        .contentType("application/json").content("{\"templateId\":\"ats\",\"resumeId\":" + resume.getId() + "}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(withCsrf(delete("/api/resumes/{id}", resume.getId()).session(session))).andExpect(status().isNotFound());
        org.junit.jupiter.api.Assertions.assertTrue(resumeRepository.findById(resume.getId()).isPresent());
    }

    @Test
    void recruiterRoleIsAssignedOnlyToConfiguredEmail() throws Exception {
        mockMvc.perform(withCsrf(post("/api/auth/register"))
                        .contentType("application/json")
                        .content("{\"email\":\"recruiter@example.com\",\"password\":\"correct-horse-123\",\"recruiterKey\":\"test-recruiter-key\"}"))
                .andExpect(status().isCreated());
        org.junit.jupiter.api.Assertions.assertEquals("ROLE_RECRUITER",
                userRepository.findByEmail("recruiter@example.com").orElseThrow().getRole());
        MockHttpSession session = login("recruiter@example.com");
        mockMvc.perform(get("/api/recruiter/candidates").session(session))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalCandidates").value(0));
    }

    @Test
    void profileDataIsIsolatedByAuthenticatedUser() throws Exception {
        User owner = user("profile-owner@example.com");
        User other = user("profile-other@example.com");
        UserProfile otherProfile = new UserProfile(other);
        otherProfile.setEmail(other.getEmail());
        otherProfile.setFullName("Other Account");
        profileRepository.save(otherProfile);

        mockMvc.perform(get("/api/profile").session(login(owner.getEmail())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(owner.getEmail()))
                .andExpect(jsonPath("$.fullName").value("profile-owner"));
    }

    @Test
    void csrfCookieTokenWorksForBrowserStyleRequests() throws Exception {
        var csrfResponse = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        var csrfCookie = csrfResponse.getResponse().getCookie("XSRF-TOKEN");
        org.junit.jupiter.api.Assertions.assertNotNull(csrfCookie);
        String token = objectMapper.readTree(csrfResponse.getResponse().getContentAsString()).get("token").asText();

        mockMvc.perform(post("/api/auth/register").cookie(csrfCookie).header("X-XSRF-TOKEN", token)
                        .contentType("application/json")
                        .content("{\"email\":\"csrf@example.com\",\"password\":\"correct-horse-123\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void validUploadWithPathLikeNameUsesPrivateGeneratedStoragePath() throws Exception {
        User owner = user("upload@example.com");
        MockHttpSession session = login(owner.getEmail());
        byte[] pdf = createReadablePdf();
        MockMultipartFile file = new MockMultipartFile("file", "../../escape.pdf", "application/octet-stream", pdf);

        mockMvc.perform(withCsrf(multipart("/api/resumes/upload").file(file).session(session)))
                .andExpect(status().isOk());
        UploadedResume saved = resumeRepository.findByUserId(owner.getId()).get(0);
        Path stored = Path.of(saved.getFilePath()).toAbsolutePath().normalize();
        Path uploadRoot = Path.of("./target/test-uploads").toAbsolutePath().normalize();
        org.junit.jupiter.api.Assertions.assertTrue(stored.startsWith(uploadRoot));
        org.junit.jupiter.api.Assertions.assertFalse(saved.getFileName().contains("/"));
        org.junit.jupiter.api.Assertions.assertTrue(Files.exists(stored));

        mockMvc.perform(withCsrf(delete("/api/resumes/{id}", saved.getId()).session(session)))
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertFalse(Files.exists(stored));
    }

    private User user(String email) {
        User user = new User(email, "ROLE_USER");
        user.setPasswordHash(passwordEncoder.encode("correct-horse-123"));
        return userRepository.save(user);
    }

    private MockHttpSession login(String email) throws Exception {
        return (MockHttpSession) mockMvc.perform(withCsrf(post("/api/auth/login"))
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"correct-horse-123\"}"))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    private <T extends AbstractMockHttpServletRequestBuilder<T>> T withCsrf(T request) throws Exception {
        var response = mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        var cookie = response.getResponse().getCookie("XSRF-TOKEN");
        String token = objectMapper.readTree(response.getResponse().getContentAsString()).get("token").asText();
        return request.cookie(cookie).header("X-XSRF-TOKEN", token);
    }

    private byte[] createReadablePdf() throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);
                content.showText("Candidate Software Engineer resume skills Java and Spring");
                content.endText();
            }
            document.save(output);
            return output.toByteArray();
        }
    }
}
