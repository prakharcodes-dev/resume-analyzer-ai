package com.career.resumeanalyzer.service;

import com.career.resumeanalyzer.model.User;
import com.career.resumeanalyzer.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApplicationUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public ApplicationUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .filter(existing -> existing.getPasswordHash() != null)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password."));
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(), user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority(user.getRole())));
    }
}
