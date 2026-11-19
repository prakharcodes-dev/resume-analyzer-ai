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
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @GetMapping
    public ResponseEntity<?> getProfile() {
        User user = currentUser();
        Optional<UserProfile> profile = userProfileRepository.findByUserId(user.getId());
        if (profile.isEmpty()) {
            UserProfile newProfile = new UserProfile(user);
            newProfile.setFullName(user.getEmail().substring(0, user.getEmail().indexOf('@')));
            newProfile.setEmail(user.getEmail());
            return ResponseEntity.ok(userProfileRepository.save(newProfile));
        }
        return ResponseEntity.ok(profile.get());
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody UserProfile updatedProfile) {
        Optional<UserProfile> optionalProfile = userProfileRepository.findByUserId(currentUser().getId());
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

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists."));
    }
}
