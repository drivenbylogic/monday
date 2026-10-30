package com.monday.app.intelligence.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class FeedFetcher {

    private static final Logger log = LoggerFactory.getLogger(FeedFetcher.class);

    private final HttpClient httpClient;
    private final SsrfValidator ssrfValidator;

    @Value("${intelligence.feed.fetch.timeout-ms:10000}")
    private long timeoutMillis = 10000;

    @Value("${intelligence.feed.max-response-bytes:2097152}")
    private int maxResponseBytes = 2097152; // 2MB

    public FeedFetcher(SsrfValidator ssrfValidator) {
        this.ssrfValidator = ssrfValidator;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(8))
                .build();
    }

    public record FetchResult(
            int statusCode,
            String body,
            String etag,
            String lastModified,
            boolean notModified
    ) {}

    public FetchResult fetchWithCache(String url, String etag, String lastModified) throws Exception {
        ssrfValidator.validateUrl(url);

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(timeoutMillis))
                .header("User-Agent", "Monday-Intelligence-Bot/2.0 (Executive-Radar; +https://monday.internal)")
                .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml;q=0.9, */*;q=0.8")
                .GET();

        if (etag != null && !etag.isBlank()) {
            builder.header("If-None-Match", etag.trim());
        }
        if (lastModified != null && !lastModified.isBlank()) {
            builder.header("If-Modified-Since", lastModified.trim());
        }

        HttpResponse<InputStream> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
        int status = response.statusCode();

        String newEtag = response.headers().firstValue("ETag").orElse(etag);
        String newLastModified = response.headers().firstValue("Last-Modified").orElse(lastModified);

        if (status == 304) {
            log.info("Feed {} returned 304 Not Modified", url);
            return new FetchResult(status, null, newEtag, newLastModified, true);
        }

        if (status < 200 || status >= 300) {
            throw new RuntimeException("HTTP fetch failed with status: " + status);
        }

        // Read up to maxResponseBytes
        try (InputStream in = response.body(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            int total = 0;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > maxResponseBytes) {
                    log.warn("Feed {} exceeded max size limit of {} bytes, truncating", url, maxResponseBytes);
                    break;
                }
                out.write(buffer, 0, read);
            }
            String body = out.toString(StandardCharsets.UTF_8);
            return new FetchResult(status, body, newEtag, newLastModified, false);
        }
    }

    public String fetch(String url) throws Exception {
        FetchResult result = fetchWithCache(url, null, null);
        return result.body();
    }
}
