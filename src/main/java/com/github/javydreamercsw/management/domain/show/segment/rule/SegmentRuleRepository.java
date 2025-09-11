package com.github.javydreamercsw.management.domain.show.segment.rule;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SegmentRuleRepository extends JpaRepository<SegmentRule, Long> {

  List<SegmentRule> findByIsActiveTrue();

  Optional<SegmentRule> findByName(String name);

  @Query("SELECT r FROM SegmentRule r WHERE r.requiresHighHeat = true AND r.isActive = true")
  List<SegmentRule> findSuitableForHighHeat();

  @Query("SELECT r FROM SegmentRule r WHERE r.requiresHighHeat = false AND r.isActive = true")
  List<SegmentRule> findStandardRules();

  boolean existsByName(String name);
}
