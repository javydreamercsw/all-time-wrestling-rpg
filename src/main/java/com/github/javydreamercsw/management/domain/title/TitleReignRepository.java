package com.github.javydreamercsw.management.domain.title;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TitleReignRepository extends JpaRepository<TitleReign, Long> {

  List<TitleReign> findByTitle(Title title);

  Optional<TitleReign> findByTitleAndReignNumber(Title title, Integer reignNumber);
}
