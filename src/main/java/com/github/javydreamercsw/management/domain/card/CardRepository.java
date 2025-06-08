package com.github.javydreamercsw.management.domain.card;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {

  // If you don't need a total row count, Slice is better than Page.
  Slice<Card> findAllBy(Pageable pageable);
}
