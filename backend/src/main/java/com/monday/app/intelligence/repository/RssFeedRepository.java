package com.monday.app.intelligence.repository;

import com.monday.app.intelligence.entity.RssFeed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RssFeedRepository extends JpaRepository<RssFeed, UUID> {
}
