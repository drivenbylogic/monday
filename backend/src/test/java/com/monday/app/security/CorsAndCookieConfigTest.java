package com.monday.app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

class CorsAndCookieConfigTest {

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(new ObjectMapper());
    }

    @Test
    void testCorsConfiguration_MatchesVercelFrontendOrigin() {
        ReflectionTestUtils.setField(securityConfig, "configuredAllowedOrigins", "https://monday-ui-omega.vercel.app,https://monday-ui-omega.vercel.app/");

        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.addHeader("Origin", "https://monday-ui-omega.vercel.app");

        CorsConfiguration config = source.getCorsConfiguration(request);
        assertNotNull(config, "CORS configuration must not be null");

        // Verify origin matching
        String matchedOrigin = config.checkOrigin("https://monday-ui-omega.vercel.app");
        assertEquals("https://monday-ui-omega.vercel.app", matchedOrigin, "Must match Vercel frontend origin");

        // Verify credentials
        assertTrue(config.getAllowCredentials(), "AllowCredentials must be true for session cookie auth");

        // Verify allowed methods
        assertTrue(config.getAllowedMethods().contains("GET"));
        assertTrue(config.getAllowedMethods().contains("POST"));
        assertTrue(config.getAllowedMethods().contains("OPTIONS"));

        // Verify exposed headers
        assertTrue(config.getExposedHeaders().contains("Set-Cookie"));
        assertTrue(config.getExposedHeaders().contains("Access-Control-Allow-Origin"));
        assertTrue(config.getExposedHeaders().contains("Access-Control-Allow-Credentials"));
    }

    @Test
    void testCorsConfiguration_MatchesVercelPreviewDeployments() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/me");
        request.addHeader("Origin", "https://monday-ui-preview.vercel.app");

        CorsConfiguration config = source.getCorsConfiguration(request);
        assertNotNull(config);

        String matchedOrigin = config.checkOrigin("https://monday-ui-preview.vercel.app");
        assertEquals("https://monday-ui-preview.vercel.app", matchedOrigin, "Must match any *.vercel.app deployment");
    }

    @Test
    void testCorsConfiguration_MatchesLocalhost() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/agenda");
        request.addHeader("Origin", "http://localhost:5173");

        CorsConfiguration config = source.getCorsConfiguration(request);
        assertNotNull(config);

        String matchedOrigin = config.checkOrigin("http://localhost:5173");
        assertEquals("http://localhost:5173", matchedOrigin, "Must match localhost origins");
    }
}
