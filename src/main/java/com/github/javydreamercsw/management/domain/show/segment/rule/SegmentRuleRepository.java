package com.github.javydreamercsw.management.domain.show.segment.rule;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SegmentRuleRepository extends JpaRepository<SegmentRule, Long> {

  Optional<SegmentRule> findByName(String name);

  @Query("SELECT r FROM SegmentRule r WHERE r.requiresHighHeat = true")
  List<SegmentRule> findSuitableForHighHeat();

  @Query("SELECT r FROM SegmentRule r WHERE r.requiresHighHeat = false")
  List<SegmentRule> findStandardRules();

  boolean existsByName(String name);
}
