/*
* Copyright (C) 2025 Software Consulting Dreams LLC
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <www.gnu.org>.
*/
package com.github.javydreamercsw.management.domain.title;

import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TitleReignRepository extends JpaRepository<TitleReign, Long> {

  /**
   * Fetches every reign with its {@code title} eagerly loaded — used outside a Hibernate session
   * (e.g. Vaadin views with {@code open-in-view=false}) to avoid {@code
   * LazyInitializationException} when rendering {@code reign.getTitle().getName()}.
   */
  @Query("SELECT tr FROM TitleReign tr JOIN FETCH tr.title")
  List<TitleReign> findAllWithTitle();

  List<TitleReign> findByTitle(Title title);

  List<TitleReign> findByTitleAndEndDateIsNull(Title title);

  List<TitleReign> findByTitleIdAndEndDateIsNull(Long titleId);

  Optional<TitleReign> findByTitleAndReignNumber(Title title, Integer reignNumber);

  List<TitleReign> findByChampionsContaining(Wrestler wrestler);

  List<TitleReign> findByStartDateBetween(Instant startDate, Instant endDate);

  /**
   * Returns reigns that were won at the given segment — used to detect title changes during news
   * generation.
   */
  List<TitleReign> findByWonAtSegment(Segment segment);

  /**
   * Returns the most recently ended reign for a title — i.e. the pre-match champion when a title
   * just changed hands.
   */
  Optional<TitleReign> findFirstByTitleAndEndDateIsNotNullOrderByEndDateDesc(Title title);
}
