package com.github.javydreamercsw.management.domain.show.match.rule;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface MatchRuleRepository
    extends JpaRepository<MatchRule, Long>, JpaSpecificationExecutor<MatchRule> {

  // If you don't need a total row count, Slice is better than Page.
  Page<MatchRule> findAllBy(Pageable pageable);

  /** Find match rule by name. */
  Optional<MatchRule> findByName(String name);

  /** Check if match rule name exists. */
  boolean existsByName(String name);

  /** Find all active match rules. */
  List<MatchRule> findByIsActiveTrue();

  /** Find match rules suitable for high-heat rivalries. */
  @Query(
      "SELECT mr FROM MatchRule mr WHERE mr.isActive = true AND mr.requiresHighHeat = true ORDER BY"
          + " mr.name")
  List<MatchRule> findSuitableForHighHeat();

  /** Find standard match rules (not requiring high heat). */
  @Query(
      "SELECT mr FROM MatchRule mr WHERE mr.isActive = true AND mr.requiresHighHeat = false ORDER"
          + " BY mr.name")
  List<MatchRule> findStandardRules();
}
