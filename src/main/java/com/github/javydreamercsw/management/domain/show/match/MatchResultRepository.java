package com.github.javydreamercsw.management.domain.show.match;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchResultRepository
    extends JpaRepository<MatchResult, Long>, JpaSpecificationExecutor<MatchResult> {

  // If you don't need a total row count, Slice is better than Page.
  Page<MatchResult> findAllBy(Pageable pageable);

  /** Find all matches for a specific show. */
  List<MatchResult> findByShow(Show show);

  /** Find all matches where a wrestler participated. */
  @Query(
      """
      SELECT mr FROM MatchResult mr
      JOIN mr.participants p
      WHERE p.wrestler = :wrestler
      ORDER BY mr.matchDate DESC
      """)
  List<MatchResult> findByWrestlerParticipation(@Param("wrestler") Wrestler wrestler);

  /** Find all matches won by a specific wrestler. */
  @Query(
      """
      SELECT mr FROM MatchResult mr
      WHERE mr.winner = :wrestler
      ORDER BY mr.matchDate DESC
      """)
  List<MatchResult> findByWinner(@Param("wrestler") Wrestler wrestler);

  /** Find recent matches between two wrestlers. */
  @Query(
      """
      SELECT mr FROM MatchResult mr
      JOIN mr.participants p1
      JOIN mr.participants p2
      WHERE p1.wrestler = :wrestler1 AND p2.wrestler = :wrestler2
      AND p1.id != p2.id
      ORDER BY mr.matchDate DESC
      """)
  List<MatchResult> findMatchesBetween(
      @Param("wrestler1") Wrestler wrestler1, @Param("wrestler2") Wrestler wrestler2);

  /** Find NPC-generated matches. */
  List<MatchResult> findByIsNpcGeneratedTrue();

  /** Find title matches. */
  List<MatchResult> findByIsTitleMatchTrue();

  /** Find matches after a specific date. */
  List<MatchResult> findByMatchDateAfter(Instant date);

  /** Count wins for a wrestler. */
  @Query(
      """
      SELECT COUNT(mr) FROM MatchResult mr
      WHERE mr.winner = :wrestler
      """)
  long countWinsByWrestler(@Param("wrestler") Wrestler wrestler);

  /** Count total matches for a wrestler. */
  @Query(
      """
      SELECT COUNT(mr) FROM MatchResult mr
      JOIN mr.participants p
      WHERE p.wrestler = :wrestler
      """)
  long countMatchesByWrestler(@Param("wrestler") Wrestler wrestler);
}
