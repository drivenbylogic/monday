package com.monday.app.intelligence.service;

import com.monday.app.intelligence.entity.RssFeed;
import com.monday.app.intelligence.repository.RssFeedRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class FeedManagementService {

    private final RssFeedRepository feedRepository;

    public FeedManagementService(RssFeedRepository feedRepository) {
        this.feedRepository = feedRepository;
    }

    public RssFeed createFeed(String name, String feedUrl, String siteUrl) {
        RssFeed feed = new RssFeed();
        feed.setName(name);
        feed.setFeedUrl(feedUrl);
        feed.setSiteUrl(siteUrl);
        feed.setStatus("ACTIVE");
        return feedRepository.save(feed);
    }

    @Transactional(readOnly = true)
    public List<RssFeed> getAllFeeds() {
        return feedRepository.findAll();
    }

    @Transactional(readOnly = true)
    public RssFeed getFeed(UUID id) {
        return feedRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Feed not found"));
    }

    public RssFeed updateFeed(UUID id, String name, String siteUrl, Double sourceQuality) {
        RssFeed feed = getFeed(id);
        if (name != null) feed.setName(name);
        if (siteUrl != null) feed.setSiteUrl(siteUrl);
        if (sourceQuality != null) feed.setSourceQuality(sourceQuality);
        feed.setUpdatedAt(OffsetDateTime.now());
        return feedRepository.save(feed);
    }

    public void enableFeed(UUID id) {
        RssFeed feed = getFeed(id);
        feed.setStatus("ACTIVE");
        feed.setUpdatedAt(OffsetDateTime.now());
        feedRepository.save(feed);
    }

    public void disableFeed(UUID id) {
        RssFeed feed = getFeed(id);
        feed.setStatus("DISABLED");
        feed.setUpdatedAt(OffsetDateTime.now());
        feedRepository.save(feed);
    }

    public void deleteFeed(UUID id) {
        feedRepository.deleteById(id);
    }

    public void updateFetchMetadata(UUID id, boolean success, String errorCode, String errorMessage) {
        RssFeed feed = getFeed(id);
        feed.setLastFetchedAt(OffsetDateTime.now());
        if (success) {
            feed.setLastSuccessAt(OffsetDateTime.now());
            feed.setLastFailureCode(null);
            feed.setLastFailureMessage(null);
        } else {
            feed.setLastFailureAt(OffsetDateTime.now());
            feed.setLastFailureCode(errorCode);
            feed.setLastFailureMessage(errorMessage);
        }
        feedRepository.save(feed);
    }

    public boolean testFeed(String feedUrl) {
        // Placeholder for feed validation logic (to be implemented in FeedFetcher)
        return true;
    }
}
