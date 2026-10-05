package com.monday.app.intelligence.repository;

import com.monday.app.intelligence.entity.InterestCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterestCategoryRepository extends JpaRepository<InterestCategory, UUID> {
    Optional<InterestCategory> findByCode(String code);
}
