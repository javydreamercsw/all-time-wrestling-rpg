package com.github.javydreamercsw.management.domain.deck;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.transaction.annotation.Transactional;

public interface DeckRepository extends JpaRepository<Deck, Long>, JpaSpecificationExecutor<Deck> {

  // If you don't need a total row count, Slice is better than Page.
  Page<Deck> findAllBy(Pageable pageable);

  List<Deck> findByWrestler(Wrestler wrestler);

  @Transactional
  void deleteByWrestler(Wrestler wrestler);
}
