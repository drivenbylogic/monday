package com.monday.app.intelligence.ingestion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SsrfValidatorTest {

    private SsrfValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SsrfValidator();
    }

    @Test
    void validateUrl_permitsSafePublicUrls() {
        assertDoesNotThrow(() -> validator.validateUrl("https://example.com/rss"));
        assertDoesNotThrow(() -> validator.validateUrl("http://news.ycombinator.com/rss"));
    }

    @Test
    void validateUrl_rejectsInvalidSchemes() {
        assertThrows(IllegalArgumentException.class, () -> validator.validateUrl("ftp://example.com/feed"));
        assertThrows(IllegalArgumentException.class, () -> validator.validateUrl("file:///etc/passwd"));
        assertThrows(IllegalArgumentException.class, () -> validator.validateUrl("gopher://example.com"));
        assertThrows(IllegalArgumentException.class, () -> validator.validateUrl("javascript:alert(1)"));
    }

    @Test
    void validateUrl_rejectsLocalhostAndLoopback() {
        assertThrows(SecurityException.class, () -> validator.validateUrl("http://localhost:8080/feed"));
        assertThrows(SecurityException.class, () -> validator.validateUrl("http://127.0.0.1:8080/feed"));
        assertThrows(SecurityException.class, () -> validator.validateUrl("http://0.0.0.0:8080/feed"));
        assertThrows(SecurityException.class, () -> validator.validateUrl("http://test.localhost/feed"));
    }

    @Test
    void validateUrl_rejectsCloudMetadata() {
        assertThrows(SecurityException.class, () -> validator.validateUrl("http://169.254.169.254/latest/meta-data"));
        assertThrows(SecurityException.class, () -> validator.validateUrl("http://metadata.google.internal/computeMetadata/v1"));
    }

    @Test
    void validateUrl_rejectsNullOrEmpty() {
        assertThrows(IllegalArgumentException.class, () -> validator.validateUrl(null));
        assertThrows(IllegalArgumentException.class, () -> validator.validateUrl("   "));
    }
}
