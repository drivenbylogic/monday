package com.monday.app.intelligence.repository;

import com.monday.app.intelligence.entity.IntelligenceRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IntelligenceRunRepository extends JpaRepository<IntelligenceRun, UUID> {
    Optional<IntelligenceRun> findTopByRunDateOrderByStartedAtDesc(LocalDate runDate);
}
