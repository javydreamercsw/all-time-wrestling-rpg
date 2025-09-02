package com.github.javydreamercsw.management.domain.title;

import com.github.javydreamercsw.management.domain.wrestler.TitleTier;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TitleRepository
    extends JpaRepository<Title, Long>, JpaSpecificationExecutor<Title> {

  // If you don't need a total row count, Slice is better than Page.
  Page<Title> findAllBy(Pageable pageable);

  Optional<Title> findByName(String name);

  List<Title> findByTier(TitleTier tier);

  List<Title> findByIsActiveTrue();

  List<Title> findByIsVacantTrue();

  List<Title> findByIsVacantFalse();

  /** Find titles currently held by a specific wrestler. */
  @Query("SELECT t FROM Title t WHERE t.currentChampion = :wrestler AND t.isVacant = false")
  List<Title> findByCurrentChampion(@Param("wrestler") Wrestler wrestler);

  /** Find active titles of a specific tier. */
  @Query("SELECT t FROM Title t WHERE t.tier = :tier AND t.isActive = true")
  List<Title> findActiveTitlesByTier(@Param("tier") TitleTier tier);

  /** Find vacant active titles. */
  @Query("SELECT t FROM Title t WHERE t.isVacant = true AND t.isActive = true")
  List<Title> findVacantActiveTitles();

  /** Check if a title with the given name already exists. */
  boolean existsByName(String name);

  /** Find titles that a wrestler is eligible to challenge for. */
  @Query(
      """
      SELECT t FROM Title t
      WHERE t.isActive = true
      AND (
          (t.tier = 'ROOKIE' AND :fanCount >= 0) OR
          (t.tier = 'TAG_TEAM' AND :fanCount >= 40000) OR
          (t.tier = 'EXTREME' AND :fanCount >= 25000) OR
          (t.tier = 'WORLD' AND :fanCount >= 100000)
      )
      """)
  List<Title> findEligibleTitlesForFanCount(@Param("fanCount") Long fanCount);
}
