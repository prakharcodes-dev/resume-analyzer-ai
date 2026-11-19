package com.career.resumeanalyzer.controller;

import com.career.resumeanalyzer.repository.UserRepository;
import com.career.resumeanalyzer.service.AccountRegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepository userRepository;
    private final AccountRegistrationService registrationService;
    private final AuthenticationManager authenticationManager;
    private final String recruiterRegistrationKey;
    private final CookieCsrfTokenRepository csrfTokenRepository;

    public AuthController(UserRepository userRepository,
                          AccountRegistrationService registrationService,
                          AuthenticationManager authenticationManager,
                          @org.springframework.beans.factory.annotation.Value("${app.recruiter-registration-key:}") String recruiterRegistrationKey,
                          CookieCsrfTokenRepository csrfTokenRepository) {
        this.userRepository = userRepository;
        this.registrationService = registrationService;
        this.authenticationManager = authenticationManager;
        this.recruiterRegistrationKey = recruiterRegistrationKey;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken csrfToken, HttpServletRequest request, HttpServletResponse response) {
        csrfTokenRepository.saveToken(csrfToken, request, response);
        return Map.of("token", csrfToken.getToken());
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest request) {
        if (request == null) return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required."));
        String email = normalizeEmail(request.email());
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Enter a valid email address."));
        }
        if (request.password() == null || request.password().length() < 12
                || request.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be between 12 and 72 characters."));
        }
        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "An account with this email already exists."));
        }

        boolean recruiter = matchesRecruiterKey(request.recruiterKey());
        try {
            registrationService.register(email, request.password(), recruiter);
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "An account with this email already exists."));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Account created. Sign in to continue."));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        if (request == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid email or password."));
        String email = normalizeEmail(request.email());
        if (email == null || request.password() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid email or password."));
        }
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            httpRequest.getSession(true);
            httpRequest.changeSessionId();
            httpRequest.getSession(true).setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            return ResponseEntity.ok(authenticatedUser(authentication));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid email or password."));
        }
    }

    @GetMapping("/me")
    public Map<String, String> me(Authentication authentication) {
        return authenticatedUser(authentication);
    }

    private Map<String, String> authenticatedUser(Authentication authentication) {
        return Map.of("email", authentication.getName(), "role", authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority()).findFirst().orElse("ROLE_USER"));
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private boolean matchesRecruiterKey(String submittedKey) {
        if (recruiterRegistrationKey.isBlank() || submittedKey == null) return false;
        return java.security.MessageDigest.isEqual(
                recruiterRegistrationKey.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                submittedKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public record AuthRequest(String email, String password, String recruiterKey) {}
}
