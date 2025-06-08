package com.github.javydreamercsw.management.domain.wrestler;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface WrestlerRepository
    extends JpaRepository<Wrestler, Long>, JpaSpecificationExecutor<Wrestler> {

  // If you don't need a total row count, Slice is better than Page.
  Page<Wrestler> findAllBy(Pageable pageable);
}
