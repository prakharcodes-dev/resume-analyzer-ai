package com.career.resumeanalyzer.controller;

import com.career.resumeanalyzer.model.User;
import com.career.resumeanalyzer.model.UserProfile;
import com.career.resumeanalyzer.repository.UserProfileRepository;
import com.career.resumeanalyzer.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private static final Long DEFAULT_USER_ID = 1L;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @GetMapping
    public ResponseEntity<?> getProfile() {
        Optional<UserProfile> profile = userProfileRepository.findByUserId(DEFAULT_USER_ID);
        if (profile.isEmpty()) {
            // Lazy create profile if not exists
            User user = userRepository.findById(DEFAULT_USER_ID)
                    .orElseGet(() -> userRepository.save(new User("guest@career.com", "ROLE_USER")));
            UserProfile newProfile = new UserProfile(user);
            newProfile.setFullName("Guest User");
            newProfile.setEmail("guest@career.com");
            return ResponseEntity.ok(userProfileRepository.save(newProfile));
        }
        return ResponseEntity.ok(profile.get());
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody UserProfile updatedProfile) {
        Optional<UserProfile> optionalProfile = userProfileRepository.findByUserId(DEFAULT_USER_ID);
        if (optionalProfile.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Profile not found.");
        }

        UserProfile existingProfile = optionalProfile.get();
        existingProfile.setFullName(updatedProfile.getFullName());
        existingProfile.setEmail(updatedProfile.getEmail());
        existingProfile.setPhone(updatedProfile.getPhone());
        existingProfile.setLinkedinUrl(updatedProfile.getLinkedinUrl());
        existingProfile.setGithubUrl(updatedProfile.getGithubUrl());
        existingProfile.setPortfolioUrl(updatedProfile.getPortfolioUrl());
        existingProfile.setSkills(updatedProfile.getSkills());
        existingProfile.setEducation(updatedProfile.getEducation());
        existingProfile.setExperience(updatedProfile.getExperience());
        existingProfile.setProjects(updatedProfile.getProjects());

        UserProfile saved = userProfileRepository.save(existingProfile);
        return ResponseEntity.ok(saved);
    }
}
