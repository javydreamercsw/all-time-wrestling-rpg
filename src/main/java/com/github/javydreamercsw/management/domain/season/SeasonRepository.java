package com.github.javydreamercsw.management.domain.season;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface SeasonRepository
    extends JpaRepository<Season, Long>, JpaSpecificationExecutor<Season> {

  // If you don't need a total row count, Slice is better than Page.
  Page<Season> findAllBy(Pageable pageable);

  Optional<Season> findByName(String name);

  /** Find the currently active season. There should only be one active season at a time. */
  @Query("SELECT s FROM Season s WHERE s.isActive = true AND s.endDate IS NULL")
  Optional<Season> findActiveSeason();

  /** Find the most recent season (by creation date). */
  @Query("SELECT s FROM Season s ORDER BY s.creationDate DESC LIMIT 1")
  Optional<Season> findLatestSeason();
}
