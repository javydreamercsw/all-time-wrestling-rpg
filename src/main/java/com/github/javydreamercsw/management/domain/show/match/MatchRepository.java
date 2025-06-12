package com.github.javydreamercsw.management.domain.show.match;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MatchRepository
    extends JpaRepository<Match, Long>, JpaSpecificationExecutor<Match> {

  // If you don't need a total row count, Slice is better than Page.
  Page<Match> findAllBy(Pageable pageable);
}
