package com.monday.app.auth.service;

import com.monday.app.auth.dto.LoginRequest;
import com.monday.app.auth.dto.RegisterRequest;
import com.monday.app.auth.dto.UserResponse;
import com.monday.app.auth.entity.User;
import com.monday.app.auth.repository.UserRepository;
import com.monday.app.security.MondayUserDetails;
import com.monday.app.shared.exception.BusinessRuleException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing user registration, stateful session authentication,
 * session lifecycle, and current user retrieval.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextRepository securityContextRepository;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       SecurityContextRepository securityContextRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.securityContextRepository = securityContextRepository;
    }

    @Transactional
    public UserResponse register(RegisterRequest request,
                                 HttpServletRequest httpRequest,
                                 HttpServletResponse httpResponse) {
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

        establishSession(saved, httpRequest, httpResponse);

        return new UserResponse(saved.getId(), saved.getUsername(), saved.getEmail(), saved.getRole());
    }

    @Transactional
    public UserResponse login(LoginRequest request,
                              HttpServletRequest httpRequest,
                              HttpServletResponse httpResponse) {
        if (request == null || request.username() == null || request.username().isBlank()
                || request.password() == null || request.password().isBlank()) {
            throw new BusinessRuleException("INVALID_CREDENTIALS", "Username and password are required");
        }

        User user = userRepository.findByUsername(request.username())
                .or(() -> userRepository.findByEmail(request.username()))
                .orElseThrow(() -> new BusinessRuleException("INVALID_CREDENTIALS", "Invalid username or password"));

        boolean matches = passwordEncoder.matches(request.password(), user.getPasswordHash());
        if (!matches) {
            // Support backward-compatible migration for initial seed entries
            if ("hashed_pw".equals(user.getPasswordHash()) || request.password().equals(user.getPasswordHash())) {
                matches = true;
                String upgradedHash = passwordEncoder.encode(request.password());
                userRepository.updatePasswordHash(user.getId(), upgradedHash);
                user.setPasswordHash(upgradedHash);
            }
        }

        if (!matches) {
            throw new BusinessRuleException("INVALID_CREDENTIALS", "Invalid username or password");
        }

        establishSession(user, httpRequest, httpResponse);

        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();

        Cookie sessionCookie = new Cookie("MONDAY_SESSION", "");
        sessionCookie.setPath("/");
        sessionCookie.setHttpOnly(true);
        sessionCookie.setMaxAge(0);
        sessionCookie.setSecure(true);
        sessionCookie.setAttribute("SameSite", "None");
        httpResponse.addCookie(sessionCookie);

        Cookie standardCookie = new Cookie("JSESSIONID", "");
        standardCookie.setPath("/");
        standardCookie.setHttpOnly(true);
        standardCookie.setMaxAge(0);
        standardCookie.setSecure(true);
        standardCookie.setAttribute("SameSite", "None");
        httpResponse.addCookie(standardCookie);
    }

    public UserResponse getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        if (authentication.getPrincipal() instanceof MondayUserDetails details) {
            return new UserResponse(details.getId(), details.getUsername(), details.getEmail(), details.getRole());
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .map(u -> new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getRole()))
                .orElse(null);
    }

    private void establishSession(User user,
                                  HttpServletRequest httpRequest,
                                  HttpServletResponse httpResponse) {
        MondayUserDetails userDetails = new MondayUserDetails(user);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // Session fixation protection: invalidate old session if existing, then create new
        HttpSession oldSession = httpRequest.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        HttpSession session = httpRequest.getSession(true);

        securityContextRepository.saveContext(context, httpRequest, httpResponse);
    }
}
