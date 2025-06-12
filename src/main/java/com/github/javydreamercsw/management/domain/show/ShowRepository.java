package com.github.javydreamercsw.management.domain.show;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ShowRepository extends JpaRepository<Show, Long>, JpaSpecificationExecutor<Show> {

  // If you don't need a total row count, Slice is better than Page.
  Page<Show> findAllBy(Pageable pageable);

  Optional<Show> findByName(String name);
}
