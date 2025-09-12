package com.github.javydreamercsw.management.domain.title;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TitleReignRepository extends JpaRepository<TitleReign, Long> {

  Optional<TitleReign> findByTitleAndChampion(Title title, Wrestler champion);

  List<TitleReign> findByTitle(Title title);

  Optional<TitleReign> findByTitleAndChampionAndReignNumber(
      Title title, Wrestler champion, Integer reignNumber);
}
