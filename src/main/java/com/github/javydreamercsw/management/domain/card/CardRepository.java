package com.github.javydreamercsw.management.domain.card;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRepository extends JpaRepository<Card, Long>, JpaSpecificationExecutor<Card> {

  // If you don't need a total row count, Slice is better than Page.
  Page<Card> findAllBy(Pageable pageable);

  Optional<Card> findByNumberAndSetSetCode(Integer number, String setCode);

  @Query("SELECT MAX(c.number) FROM Card c WHERE c.set.id = :setId")
  Integer findMaxCardNumberBySet(@Param("setId") Long setId);
}
