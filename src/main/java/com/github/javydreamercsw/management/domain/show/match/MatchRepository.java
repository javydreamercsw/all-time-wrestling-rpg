package com.github.javydreamercsw.management.domain.show.match;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchRepository
    extends JpaRepository<Match, Long>, JpaSpecificationExecutor<Match> {

  // If you don't need a total row count, Slice is better than Page.
  Page<Match> findAllBy(Pageable pageable);

  /** Find all matches for a specific show. */
  @Query(
      """
      SELECT m FROM Match m
      LEFT JOIN FETCH m.participants p
      LEFT JOIN FETCH p.wrestler
      WHERE m.show = :show
      ORDER BY m.matchDate DESC
      """)
  List<Match> findByShow(@Param("show") Show show);

  /** Find all matches where a wrestler participated. */
  @Query(
      """
      SELECT m FROM Match m
      JOIN m.participants p
      WHERE p.wrestler = :wrestler
      ORDER BY m.matchDate DESC
      """)
  List<Match> findByWrestlerParticipation(@Param("wrestler") Wrestler wrestler);

  /** Find all matches won by a specific wrestler. */
  @Query(
      """
      SELECT m FROM Match m
      WHERE m.winner = :wrestler
      ORDER BY m.matchDate DESC
      """)
  List<Match> findByWinner(@Param("wrestler") Wrestler wrestler);

  /** Find recent matches between two wrestlers. */
  @Query(
      """
      SELECT m FROM Match m
      JOIN m.participants p1
      JOIN m.participants p2
      WHERE p1.wrestler = :wrestler1 AND p2.wrestler = :wrestler2
      AND p1.id != p2.id
      ORDER BY m.matchDate DESC
      """)
  List<Match> findMatchesBetween(
      @Param("wrestler1") Wrestler wrestler1, @Param("wrestler2") Wrestler wrestler2);

  /** Find NPC-generated matches. */
  List<Match> findByIsNpcGeneratedTrue();

  /** Find title matches. */
  List<Match> findByIsTitleMatchTrue();

  /** Find matches after a specific date. */
  List<Match> findByMatchDateAfter(Instant date);

  /** Find matches between two dates. */
  List<Match> findByMatchDateBetween(Instant startDate, Instant endDate);

  /** Count wins for a wrestler. */
  @Query(
      """
      SELECT COUNT(m) FROM Match m
      WHERE m.winner = :wrestler
      """)
  long countWinsByWrestler(@Param("wrestler") Wrestler wrestler);

  /** Count total matches for a wrestler. */
  @Query(
      """
      SELECT COUNT(m) FROM Match m
      JOIN m.participants p
      WHERE p.wrestler = :wrestler
      """)
  long countMatchesByWrestler(@Param("wrestler") Wrestler wrestler);

  /** Check if a match result exists by external ID. */
  boolean existsByExternalId(String externalId);

  /** Find a match result by external ID. */
  Optional<Match> findByExternalId(String externalId);
}
