package com.github.javydreamercsw.management.domain.wrestler;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WrestlerRepository
    extends JpaRepository<Wrestler, Long>, JpaSpecificationExecutor<Wrestler> {

  // If you don't need a total row count, Slice is better than Page.
  Page<Wrestler> findAllBy(Pageable pageable);

  @Query("SELECT w FROM Wrestler w LEFT JOIN FETCH w.decks WHERE w.name = :name")
  Optional<Wrestler> findByName(@Param("name") String name);

  Optional<Wrestler> findByExternalId(String externalId);

  List<Wrestler> findByFansBetween(long minFans, long maxFans);
}
