package com.monday.app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monday.app.shared.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Spring Security configuration.
 *
 * <p>Stateful session-based authentication using secure HttpOnly cookies.
 * Public endpoints are explicitly permitted; everything else requires an authenticated session.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    @Value("${cors.allowed-origins:https://monday-ui-omega.vercel.app,https://monday-ui-omega.vercel.app/}")
    private String configuredAllowedOrigins;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .securityContext(sc -> sc.securityContextRepository(securityContextRepository()))
            .sessionManagement(sm -> sm
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/health/**", "/actuator/health", "/actuator/info").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated()
            )
            .logout(logout -> logout
                .logoutUrl("/api/v1/auth/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    objectMapper.writeValue(response.getOutputStream(), Map.of("message", "Logged out successfully"));
                })
                .invalidateHttpSession(true)
                .deleteCookies("MONDAY_SESSION", "JSESSIONID")
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) ->
                        writeErrorResponse(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                                "UNAUTHORIZED", "Authentication required"))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                        writeErrorResponse(request, response, HttpServletResponse.SC_FORBIDDEN,
                                "FORBIDDEN", "Access denied"))
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = new ArrayList<>();
        // Explicitly allowed frontend URLs (including Vercel hosted frontend)
        origins.add("https://monday-ui-omega.vercel.app");
        origins.add("https://monday-ui-omega.vercel.app/");
        origins.add("https://*.vercel.app");

        // Merge any comma-separated allowed origins from configuration / environment
        if (configuredAllowedOrigins != null && !configuredAllowedOrigins.isBlank()) {
            for (String origin : configuredAllowedOrigins.split(",")) {
                String trimmed = origin.trim();
                if (!trimmed.isEmpty() && !origins.contains(trimmed)) {
                    origins.add(trimmed);
                }
            }
        }

        // Local development and private network patterns
        origins.addAll(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://192.168.*:*",
                "http://10.*:*",
                "http://172.16.*:*",
                "http://172.17.*:*",
                "http://172.18.*:*",
                "http://172.19.*:*",
                "http://172.20.*:*",
                "http://172.21.*:*",
                "http://172.22.*:*",
                "http://172.23.*:*",
                "http://172.24.*:*",
                "http://172.25.*:*",
                "http://172.26.*:*",
                "http://172.27.*:*",
                "http://172.28.*:*",
                "http://172.29.*:*",
                "http://172.30.*:*",
                "http://172.31.*:*",
                "*"
        ));

        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of(
                "Set-Cookie",
                "Authorization",
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"
        ));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private void writeErrorResponse(HttpServletRequest request, HttpServletResponse response,
                                     int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorResponse error = new ApiErrorResponse(code, message, request.getRequestURI());
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
