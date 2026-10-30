package com.monday.app.intelligence.repository;

import com.monday.app.intelligence.entity.IntelligenceRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IntelligenceRunRepository extends JpaRepository<IntelligenceRun, UUID> {
    Optional<IntelligenceRun> findTopByRunDateOrderByStartedAtDesc(LocalDate runDate);
    Optional<IntelligenceRun> findFirstByOrderByStartedAtDesc();
    Optional<IntelligenceRun> findFirstByStatus(String status);
    List<IntelligenceRun> findByStartedAtBetweenOrderByStartedAtDesc(OffsetDateTime start, OffsetDateTime end);
    List<IntelligenceRun> findTop30ByOrderByStartedAtDesc();
}
