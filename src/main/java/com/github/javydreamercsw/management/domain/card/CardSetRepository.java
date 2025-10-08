package com.github.javydreamercsw.management.domain.card;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardSetRepository extends JpaRepository<CardSet, Long> {
  Optional<CardSet> findBySetCode(String setCode);
}
