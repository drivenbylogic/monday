package com.monday.app.auth;

import com.monday.app.auth.dto.LoginRequest;
import com.monday.app.auth.dto.UserResponse;
import com.monday.app.auth.entity.User;
import com.monday.app.auth.repository.UserRepository;
import com.monday.app.auth.service.AuthService;
import com.monday.app.security.MondayUserDetails;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionLifecycleTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private SecurityContextRepository securityContextRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        securityContextRepository = new HttpSessionSecurityContextRepository();
        authService = new AuthService(userRepository, passwordEncoder, securityContextRepository);
        SecurityContextHolder.clearContext();
    }

    @Test
    void testFullSessionLifecycle_Login_Persist_Retrieve_Logout() {
        // 1. Prepare user in repository
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setUsername("elena.rostova");
        user.setEmail("elena@sovereign.io");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole("ROLE_USER");

        when(userRepository.findByUsername("elena.rostova")).thenReturn(Optional.of(user));

        // 2. Perform Login Request
        MockHttpServletRequest loginRequest = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        MockHttpServletResponse loginResponse = new MockHttpServletResponse();

        LoginRequest credentials = new LoginRequest("elena.rostova", "password123");
        UserResponse userResponse = authService.login(credentials, loginRequest, loginResponse);

        assertNotNull(userResponse);
        assertEquals("elena.rostova", userResponse.username());

        // Verify session was created and contains Spring Security context
        HttpSession session = loginRequest.getSession(false);
        assertNotNull(session, "Session must be created upon login");
        Object springSecurityContextObj = session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertNotNull(springSecurityContextObj, "Session must store the SPRING_SECURITY_CONTEXT");
        assertTrue(springSecurityContextObj instanceof SecurityContext);
        SecurityContext savedContext = (SecurityContext) springSecurityContextObj;
        assertTrue(savedContext.getAuthentication().getPrincipal() instanceof MondayUserDetails);

        // 3. Simulate a Subsequent Request using the SAME Session
        SecurityContextHolder.clearContext(); // Reset thread-local to simulate new HTTP request thread

        MockHttpServletRequest subsequentRequest = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        subsequentRequest.setSession(session);
        MockHttpServletResponse subsequentResponse = new MockHttpServletResponse();

        // Load context using securityContextRepository
        HttpRequestResponseHolder holder = new HttpRequestResponseHolder(subsequentRequest, subsequentResponse);
        SecurityContext loadedContext = securityContextRepository.loadContext(holder);
        assertNotNull(loadedContext);
        Authentication auth = loadedContext.getAuthentication();
        assertNotNull(auth, "Authentication must be restored from session");
        assertTrue(auth.isAuthenticated());

        // Verify getCurrentUser with restored authentication
        UserResponse meResponse = authService.getCurrentUser(auth);
        assertNotNull(meResponse);
        assertEquals(userId, meResponse.id());
        assertEquals("elena.rostova", meResponse.username());

        // 4. Perform Logout Request with the SAME Session
        MockHttpServletRequest logoutRequest = new MockHttpServletRequest("POST", "/api/v1/auth/logout");
        logoutRequest.setSession(session);
        MockHttpServletResponse logoutResponse = new MockHttpServletResponse();

        authService.logout(logoutRequest, logoutResponse);

        // Verify session was invalidated
        assertTrue(((org.springframework.mock.web.MockHttpSession) session).isInvalid(), "Session must be invalidated on logout");

        // Verify cookie clearing headers were added
        boolean clearedMondaySession = false;
        for (jakarta.servlet.http.Cookie cookie : logoutResponse.getCookies()) {
            if ("MONDAY_SESSION".equals(cookie.getName()) && cookie.getMaxAge() == 0) {
                clearedMondaySession = true;
            }
        }
        assertTrue(clearedMondaySession, "MONDAY_SESSION cookie must be cleared with maxAge=0");
    }
}
