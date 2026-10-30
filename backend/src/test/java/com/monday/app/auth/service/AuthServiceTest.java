package com.monday.app.auth.service;

import com.monday.app.auth.dto.LoginRequest;
import com.monday.app.auth.dto.RegisterRequest;
import com.monday.app.auth.dto.UserResponse;
import com.monday.app.auth.entity.User;
import com.monday.app.auth.repository.UserRepository;
import com.monday.app.security.MondayUserDetails;
import com.monday.app.shared.exception.BusinessRuleException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private SecurityContextRepository securityContextRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        securityContextRepository = mock(SecurityContextRepository.class);
        authService = new AuthService(userRepository, passwordEncoder, securityContextRepository);
        SecurityContextHolder.clearContext();
    }

    @Test
    void testRegister_Success() {
        RegisterRequest request = new RegisterRequest("newuser", "new@monday.io", "password123");
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@monday.io")).thenReturn(Optional.empty());

        UUID createdId = UUID.randomUUID();
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(createdId);
            return user;
        });

        UserResponse response = authService.register(request, httpRequest, httpResponse);

        assertNotNull(response);
        assertEquals(createdId, response.id());
        assertEquals("newuser", response.username());
        assertEquals("new@monday.io", response.email());
        assertEquals("ROLE_USER", response.role());

        verify(securityContextRepository).saveContext(any(), eq(httpRequest), eq(httpResponse));
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("newuser", SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void testRegister_DuplicateUsername() {
        RegisterRequest request = new RegisterRequest("existing", "new@monday.io", "password123");
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();

        when(userRepository.findByUsername("existing")).thenReturn(Optional.of(new User()));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> authService.register(request, httpRequest, httpResponse));

        assertEquals("USERNAME_TAKEN", ex.getCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testRegister_DuplicateEmail() {
        RegisterRequest request = new RegisterRequest("newuser", "existing@monday.io", "password123");
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();

        when(userRepository.findByUsername("newuser")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("existing@monday.io")).thenReturn(Optional.of(new User()));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> authService.register(request, httpRequest, httpResponse));

        assertEquals("EMAIL_TAKEN", ex.getCode());
        verify(userRepository, never()).save(any());
    }

    @Test
    void testLogin_SuccessWithBcrypt() {
        LoginRequest request = new LoginRequest("elena.rostova", "password123");
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("elena.rostova");
        user.setEmail("elena@sovereign.io");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole("ROLE_USER");

        when(userRepository.findByUsername("elena.rostova")).thenReturn(Optional.of(user));

        UserResponse response = authService.login(request, httpRequest, httpResponse);

        assertNotNull(response);
        assertEquals(user.getId(), response.id());
        assertEquals("elena.rostova", response.username());
        verify(securityContextRepository).saveContext(any(), eq(httpRequest), eq(httpResponse));
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void testLogin_SuccessWithEmail() {
        LoginRequest request = new LoginRequest("elena@sovereign.io", "password123");
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("elena.rostova");
        user.setEmail("elena@sovereign.io");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole("ROLE_USER");

        when(userRepository.findByUsername("elena@sovereign.io")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("elena@sovereign.io")).thenReturn(Optional.of(user));

        UserResponse response = authService.login(request, httpRequest, httpResponse);

        assertNotNull(response);
        assertEquals("elena.rostova", response.username());
    }

    @Test
    void testLogin_SuccessAndUpgradeLegacyPassword() {
        LoginRequest request = new LoginRequest("johndoe", "password123");
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("johndoe");
        user.setEmail("johndoe@example.com");
        user.setPasswordHash("hashed_pw"); // Legacy seed hash
        user.setRole("ROLE_USER");

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(user));

        UserResponse response = authService.login(request, httpRequest, httpResponse);

        assertNotNull(response);
        assertEquals("johndoe", response.username());
        // Verify that password was upgraded with BCrypt
        verify(userRepository).updatePasswordHash(eq(user.getId()), argThat(hash -> hash.startsWith("$2a$")));
    }

    @Test
    void testLogin_InvalidPassword() {
        LoginRequest request = new LoginRequest("elena.rostova", "wrongpass");
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("elena.rostova");
        user.setEmail("elena@sovereign.io");
        user.setPasswordHash(passwordEncoder.encode("password123"));

        when(userRepository.findByUsername("elena.rostova")).thenReturn(Optional.of(user));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class,
                () -> authService.login(request, httpRequest, httpResponse));

        assertEquals("INVALID_CREDENTIALS", ex.getCode());
        verify(securityContextRepository, never()).saveContext(any(), any(), any());
    }

    @Test
    void testLogout_ClearsSessionAndCookies() {
        MockHttpServletRequest httpRequest = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        httpRequest.setSession(session);
        MockHttpServletResponse httpResponse = new MockHttpServletResponse();

        authService.logout(httpRequest, httpResponse);

        assertTrue(session.isInvalid());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(2, httpResponse.getCookies().length);
        assertEquals("MONDAY_SESSION", httpResponse.getCookies()[0].getName());
        assertEquals(0, httpResponse.getCookies()[0].getMaxAge());
    }

    @Test
    void testGetCurrentUser_Authenticated() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("alexander.cross");
        user.setEmail("alex@sovereign.io");
        user.setRole("ROLE_ADMIN");

        MondayUserDetails userDetails = new MondayUserDetails(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        UserResponse response = authService.getCurrentUser(auth);

        assertNotNull(response);
        assertEquals("alexander.cross", response.username());
        assertEquals("ROLE_ADMIN", response.role());
    }

    @Test
    void testGetCurrentUser_Unauthenticated() {
        assertNull(authService.getCurrentUser(null));

        Authentication anonAuth = new AnonymousAuthenticationToken("key", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        assertNull(authService.getCurrentUser(anonAuth));
    }
}
