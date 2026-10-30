package com.monday.app.intelligence.service;

import com.monday.app.intelligence.dto.*;
import com.monday.app.intelligence.entity.RssFeed;
import com.monday.app.intelligence.ingestion.FeedFetcher;
import com.monday.app.intelligence.ingestion.RssParser;
import com.monday.app.intelligence.repository.ArticleSourceRepository;
import com.monday.app.intelligence.repository.RssFeedRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class FeedManagementService {

    private static final Logger log = LoggerFactory.getLogger(FeedManagementService.class);

    private final RssFeedRepository feedRepository;
    private final ArticleSourceRepository articleSourceRepository;
    private final FeedFetcher feedFetcher;
    private final RssParser rssParser;
    private final com.monday.app.intelligence.repository.InterestCategoryRepository categoryRepository;

    public FeedManagementService(RssFeedRepository feedRepository,
                                 ArticleSourceRepository articleSourceRepository,
                                 FeedFetcher feedFetcher,
                                 RssParser rssParser,
                                 com.monday.app.intelligence.repository.InterestCategoryRepository categoryRepository) {
        this.feedRepository = feedRepository;
        this.articleSourceRepository = articleSourceRepository;
        this.feedFetcher = feedFetcher;
        this.rssParser = rssParser;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<RssNodeDto> getAllNodes() {
        return feedRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RssFeed> getAllFeeds() {
        return feedRepository.findAll();
    }

    @Transactional(readOnly = true)
    public RssNodeDto getNode(UUID id) {
        return toDto(getFeed(id));
    }

    @Transactional(readOnly = true)
    public RssFeed getFeed(UUID id) {
        return feedRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feed node not found with id: " + id));
    }

    public RssNodeDto createNode(CreateRssNodeRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new IllegalArgumentException("Feed node name is required");
        }
        if (request.feedUrl() == null || request.feedUrl().isBlank()) {
            throw new IllegalArgumentException("Feed URL is required");
        }

        String trimmedUrl = request.feedUrl().trim();
        if (feedRepository.existsByFeedUrl(trimmedUrl)) {
            throw new IllegalArgumentException("A feed node with URL '" + trimmedUrl + "' already exists");
        }

        RssFeed feed = new RssFeed();
        feed.setName(request.name().trim());
        feed.setFeedUrl(trimmedUrl);
        feed.setSiteUrl(request.siteUrl() != null && !request.siteUrl().isBlank() ? request.siteUrl().trim() : null);
        feed.setAreaOfInterest(request.areaOfInterest() != null && !request.areaOfInterest().isBlank()
                ? request.areaOfInterest().trim() : "TECHNOLOGY");
        ensureCategoryExists(feed.getAreaOfInterest());
        feed.setDescription(request.description() != null ? request.description().trim() : null);
        feed.setArticleLimit(request.articleLimit() != null && request.articleLimit() > 0 ? request.articleLimit() : 5);
        feed.setIsEnabled(request.isEnabled() != null ? request.isEnabled() : true);
        feed.setStatus(Boolean.TRUE.equals(feed.getIsEnabled()) ? "ACTIVE" : "DISABLED");
        feed.setLastFetchStatus("IDLE");
        feed.setCreatedAt(OffsetDateTime.now());
        feed.setUpdatedAt(OffsetDateTime.now());

        RssFeed saved = feedRepository.save(feed);
        log.info("Created new RSS feed node: {} ({})", saved.getName(), saved.getFeedUrl());
        return toDto(saved);
    }

    public RssFeed createFeed(String name, String feedUrl, String siteUrl) {
        CreateRssNodeRequest req = new CreateRssNodeRequest(name, feedUrl, siteUrl, "TECHNOLOGY", null, 5, true);
        RssNodeDto dto = createNode(req);
        return getFeed(dto.id());
    }

    public RssNodeDto updateNode(UUID id, UpdateRssNodeRequest request) {
        RssFeed feed = getFeed(id);

        if (request.name() != null && !request.name().isBlank()) {
            feed.setName(request.name().trim());
        }
        if (request.feedUrl() != null && !request.feedUrl().isBlank()) {
            String trimmedUrl = request.feedUrl().trim();
            if (feedRepository.existsByFeedUrlAndIdNot(trimmedUrl, id)) {
                throw new IllegalArgumentException("A feed node with URL '" + trimmedUrl + "' already exists");
            }
            feed.setFeedUrl(trimmedUrl);
        }
        if (request.siteUrl() != null) {
            feed.setSiteUrl(request.siteUrl().isBlank() ? null : request.siteUrl().trim());
        }
        if (request.areaOfInterest() != null && !request.areaOfInterest().isBlank()) {
            feed.setAreaOfInterest(request.areaOfInterest().trim());
            ensureCategoryExists(feed.getAreaOfInterest());
        }
        if (request.description() != null) {
            feed.setDescription(request.description().trim());
        }
        if (request.articleLimit() != null && request.articleLimit() > 0) {
            feed.setArticleLimit(Math.min(request.articleLimit(), 20));
        }
        if (request.isEnabled() != null) {
            feed.setIsEnabled(request.isEnabled());
            feed.setStatus(request.isEnabled() ? "ACTIVE" : "DISABLED");
        }
        feed.setUpdatedAt(OffsetDateTime.now());

        RssFeed saved = feedRepository.save(feed);
        log.info("Updated RSS feed node: {} ({})", saved.getName(), saved.getId());
        return toDto(saved);
    }

    public RssFeed updateFeed(UUID id, String name, String siteUrl, Double sourceQuality) {
        RssFeed feed = getFeed(id);
        if (name != null) feed.setName(name);
        if (siteUrl != null) feed.setSiteUrl(siteUrl);
        if (sourceQuality != null) feed.setSourceQuality(sourceQuality);
        feed.setUpdatedAt(OffsetDateTime.now());
        return feedRepository.save(feed);
    }

    public RssNodeDto toggleFeedEnabled(UUID id, boolean enabled) {
        RssFeed feed = getFeed(id);
        feed.setIsEnabled(enabled);
        feed.setStatus(enabled ? "ACTIVE" : "DISABLED");
        feed.setUpdatedAt(OffsetDateTime.now());
        RssFeed saved = feedRepository.save(feed);
        return toDto(saved);
    }

    public void enableFeed(UUID id) {
        toggleFeedEnabled(id, true);
    }

    public void disableFeed(UUID id) {
        toggleFeedEnabled(id, false);
    }

    public void deleteFeed(UUID id) {
        RssFeed feed = getFeed(id);
        feedRepository.delete(feed);
        log.info("Deleted RSS feed node: {} ({})", feed.getName(), id);
    }

    public void updateFetchMetadata(UUID id, boolean success, String status, String errorCode, String errorMessage,
                                    String etag, String lastModified) {
        RssFeed feed = getFeed(id);
        OffsetDateTime now = OffsetDateTime.now();
        feed.setLastAttemptedAt(now);
        feed.setLastFetchedAt(now);
        feed.setLastFetchStatus(status != null ? status : (success ? "SUCCESS" : "ERROR"));

        if (etag != null) feed.setEtag(etag);
        if (lastModified != null) feed.setLastModifiedHeader(lastModified);

        if (success) {
            feed.setLastSuccessAt(now);
            feed.setLastFailureCode(null);
            feed.setLastFailureMessage(null);
        } else {
            feed.setLastFailureAt(now);
            feed.setLastFailureCode(errorCode);
            feed.setLastFailureMessage(sanitizeErrorMessage(errorMessage));
        }
        feed.setUpdatedAt(now);
        feedRepository.save(feed);
    }

    public void updateFetchMetadata(UUID id, boolean success, String errorCode, String errorMessage) {
        updateFetchMetadata(id, success, success ? "SUCCESS" : "ERROR", errorCode, errorMessage, null, null);
    }

    /**
     * Preview a feed URL without saving or invoking AI.
     * Fetches, validates SSRF safety, and extracts entries for review.
     */
    @Transactional(readOnly = true)
    public FeedPreviewResponse previewFeed(String feedUrl) {
        if (feedUrl == null || feedUrl.isBlank()) {
            return new FeedPreviewResponse(false, null, null, null, 0, List.of(), "Feed URL is required");
        }

        try {
            String xml = feedFetcher.fetch(feedUrl.trim());
            RawRssFeed parsed = rssParser.parse(xml);

            List<FeedPreviewEntryDto> entries = new ArrayList<>();
            if (parsed.items() != null) {
                for (RawRssItem item : parsed.items()) {
                    if (entries.size() >= 5) break; // preview top 5
                    String desc = item.description() != null ? item.description().replaceAll("<[^>]*>", " ").trim() : "";
                    if (desc.length() > 200) desc = desc.substring(0, 200) + "...";
                    entries.add(new FeedPreviewEntryDto(
                            item.title(),
                            item.link(),
                            item.pubDate(),
                            desc,
                            item.author()
                    ));
                }
            }

            return new FeedPreviewResponse(
                    true,
                    parsed.title(),
                    parsed.description(),
                    parsed.link(),
                    parsed.items() != null ? parsed.items().size() : 0,
                    entries,
                    null
            );
        } catch (Exception e) {
            log.warn("Feed preview failed for URL '{}': {}", feedUrl, e.getMessage());
            return new FeedPreviewResponse(
                    false,
                    null,
                    null,
                    null,
                    0,
                    List.of(),
                    sanitizeErrorMessage(e.getMessage())
            );
        }
    }

    @Transactional(readOnly = true)
    public boolean testFeed(String feedUrl) {
        FeedPreviewResponse preview = previewFeed(feedUrl);
        return preview.reachable();
    }

    private RssNodeDto toDto(RssFeed feed) {
        long count = articleSourceRepository.findByIdFeedId(feed.getId()).size();
        return new RssNodeDto(
                feed.getId(),
                feed.getName(),
                feed.getFeedUrl(),
                feed.getSiteUrl(),
                feed.getAreaOfInterest(),
                feed.getDescription(),
                feed.getIsEnabled(),
                feed.getArticleLimit(),
                feed.getSourceQuality(),
                feed.getLastFetchedAt(),
                feed.getLastSuccessAt(),
                feed.getLastFailureAt(),
                feed.getLastFetchStatus(),
                feed.getLastFailureMessage(),
                feed.getCreatedAt(),
                feed.getUpdatedAt(),
                count
        );
    }

    private String sanitizeErrorMessage(String error) {
        if (error == null) return null;
        return error.replaceAll("sk-[a-zA-Z0-9_-]+", "sk-***")
                .substring(0, Math.min(error.length(), 250));
    }

    private void ensureCategoryExists(String area) {
        if (area == null || area.isBlank()) return;
        String cleanCode = area.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_");
        if (!categoryRepository.existsByCodeIgnoreCase(cleanCode)) {
            com.monday.app.intelligence.entity.InterestCategory cat = new com.monday.app.intelligence.entity.InterestCategory();
            cat.setCode(cleanCode);
            cat.setName(formatAreaName(area.trim()));
            cat.setStatus("ACTIVE");
            categoryRepository.save(cat);
        }
    }

    private String formatAreaName(String area) {
        String clean = area.replace('_', ' ').trim();
        StringBuilder sb = new StringBuilder();
        for (String word : clean.split("\\s+")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
