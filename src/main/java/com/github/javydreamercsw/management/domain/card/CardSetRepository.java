package com.github.javydreamercsw.management.domain.card;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CardSetRepository
    extends JpaRepository<CardSet, Long>, JpaSpecificationExecutor<CardSet> {

  // If you don't need a total row count, Slice is better than Page.
  Page<CardSet> findAllBy(Pageable pageable);
}
