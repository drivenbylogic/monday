package com.monday.app.intelligence.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Set;

@Component
public class SsrfValidator {

    private static final Logger log = LoggerFactory.getLogger(SsrfValidator.class);

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final Set<String> BLOCKED_HOSTS = Set.of(
            "localhost",
            "127.0.0.1",
            "::1",
            "0.0.0.0",
            "metadata.google.internal",
            "instance-data"
    );

    /**
     * Validates that the provided URI is a safe HTTP/HTTPS URL and does not target
     * loopback, RFC 1918 private subnets, link-local, or cloud metadata services.
     */
    public void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL must not be empty");
        }

        URI uri;
        try {
            uri = URI.create(url.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid URL syntax: " + e.getMessage());
        }

        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new IllegalArgumentException("Only HTTP and HTTPS protocols are allowed. Rejected scheme: " + scheme);
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("URL host is missing or invalid");
        }

        String lowerHost = host.toLowerCase();
        if (BLOCKED_HOSTS.contains(lowerHost) || lowerHost.endsWith(".localhost") || lowerHost.endsWith(".internal") || lowerHost.endsWith(".local")) {
            throw new SecurityException("Host is blocked by SSRF policy: " + host);
        }

        // Validate resolved IP addresses
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses == null || addresses.length == 0) {
                throw new UnknownHostException("Unable to resolve host: " + host);
            }

            for (InetAddress addr : addresses) {
                if (isBlockedIpAddress(addr)) {
                    log.warn("SSRF blocked attempt to connect to restricted destination: {} ({})", host, addr.getHostAddress());
                    throw new SecurityException("Resolved IP is blocked by SSRF security policy: " + addr.getHostAddress());
                }
            }
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Unknown or unresolvable host: " + host, e);
        }
    }

    private boolean isBlockedIpAddress(InetAddress addr) {
        if (addr.isLoopbackAddress() || addr.isAnyLocalAddress() || addr.isLinkLocalAddress() || addr.isSiteLocalAddress() || addr.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = addr.getAddress();
        if (bytes.length == 4) {
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;

            // 127.0.0.0/8 (Loopback)
            if (b0 == 127) return true;
            // 10.0.0.0/8 (Private)
            if (b0 == 10) return true;
            // 172.16.0.0/12 (Private)
            if (b0 == 172 && (b1 >= 16 && b1 <= 31)) return true;
            // 192.168.0.0/16 (Private)
            if (b0 == 192 && b1 == 168) return true;
            // 169.254.0.0/16 (Link local / AWS & Cloud Metadata: 169.254.169.254)
            if (b0 == 169 && b1 == 254) return true;
            // 100.64.0.0/10 (Carrier-grade NAT / metadata)
            if (b0 == 100 && (b1 >= 64 && b1 <= 127)) return true;
            // 0.0.0.0/8
            if (b0 == 0) return true;
        }

        return false;
    }
}
