package com.monday.app.auth.service;

import com.monday.app.auth.dto.AuthResponse;
import com.monday.app.auth.dto.LoginRequest;
import com.monday.app.auth.dto.RegisterRequest;
import com.monday.app.auth.entity.User;
import com.monday.app.auth.repository.UserRepository;
import com.monday.app.security.JwtTokenProvider;
import com.monday.app.shared.exception.BusinessRuleException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new BusinessRuleException("USERNAME_TAKEN", "Username is already taken");
        }
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new BusinessRuleException("EMAIL_TAKEN", "Email is already registered");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole("ROLE_USER");

        User saved = userRepository.save(user);
        String token = tokenProvider.generateToken(saved.getId(), saved.getUsername(), saved.getRole());
        return new AuthResponse(token, saved.getUsername(), saved.getRole());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new BusinessRuleException("INVALID_CREDENTIALS", "Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessRuleException("INVALID_CREDENTIALS", "Invalid username or password");
        }

        String token = tokenProvider.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new AuthResponse(token, user.getUsername(), user.getRole());
    }
}
