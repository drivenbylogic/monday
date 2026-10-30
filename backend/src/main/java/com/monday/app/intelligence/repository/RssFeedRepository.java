package com.monday.app.intelligence.repository;

import com.monday.app.intelligence.entity.RssFeed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RssFeedRepository extends JpaRepository<RssFeed, UUID> {
    List<RssFeed> findByIsEnabledTrue();
    List<RssFeed> findByStatus(String status);
    List<RssFeed> findByAreaOfInterest(String areaOfInterest);
    boolean existsByFeedUrl(String feedUrl);
    boolean existsByFeedUrlAndIdNot(String feedUrl, UUID id);
    Optional<RssFeed> findByFeedUrl(String feedUrl);
}
