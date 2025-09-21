package com.github.javydreamercsw.management.domain.show.type;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ShowTypeRepository
    extends JpaRepository<ShowType, Long>, JpaSpecificationExecutor<ShowType> {

  // If you don't need a total row count, Slice is better than Page.
  Page<ShowType> findAllBy(Pageable pageable);

  /** Find show type by name. */
  Optional<ShowType> findByName(String name);

  /** Check if show type name exists. */
  boolean existsByName(String name);
}
