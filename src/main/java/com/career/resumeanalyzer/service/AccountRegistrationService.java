package com.career.resumeanalyzer.service;

import com.career.resumeanalyzer.model.User;
import com.career.resumeanalyzer.model.UserProfile;
import com.career.resumeanalyzer.repository.UserProfileRepository;
import com.career.resumeanalyzer.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountRegistrationService {
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountRegistrationService(UserRepository userRepository, UserProfileRepository profileRepository,
                                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(String email, String password, boolean recruiter) {
        User user = new User(email, recruiter ? "ROLE_RECRUITER" : "ROLE_USER");
        user.setPasswordHash(passwordEncoder.encode(password));
        userRepository.saveAndFlush(user);

        UserProfile profile = new UserProfile(user);
        profile.setEmail(email);
        profile.setFullName(email.substring(0, email.indexOf('@')));
        profileRepository.save(profile);
    }
}
