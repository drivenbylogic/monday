package com.monday.app.intelligence.ingestion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

@Component
public class FeedFetcher {

    private final HttpClient httpClient;
    private final Set<String> allowedSchemes = Set.of("http", "https");

    @Value("${intelligence.feed.fetch.timeout:10000}")
    private long timeoutMillis;

    public FeedFetcher() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String fetch(String url) throws Exception {
        URI uri = new URI(url);
        if (!allowedSchemes.contains(uri.getScheme().toLowerCase())) {
            throw new IllegalArgumentException("Only HTTP/HTTPS are allowed. Blocked scheme: " + uri.getScheme());
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofMillis(timeoutMillis))
                .header("User-Agent", "Monday Intelligence Bot/1.0")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("HTTP request failed with status: " + response.statusCode());
        }

        return response.body();
    }
}
