package com.github.javydreamercsw.management.domain.show.match.type;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MatchTypeRepository
    extends JpaRepository<MatchType, Long>, JpaSpecificationExecutor<MatchType> {

  // If you don't need a total row count, Slice is better than Page.
  Page<MatchType> findAllBy(Pageable pageable);

  /** Find match type by name. */
  Optional<MatchType> findByName(String name);

  /** Check if match type name exists. */
  boolean existsByName(String name);
}
