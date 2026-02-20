/*
* Copyright (C) 2026 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.domain.season;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.management.AbstractJpaTest;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class SeasonRepositoryTest extends AbstractJpaTest {

  @Autowired private SeasonRepository seasonRepository;
  @Autowired private ShowRepository showRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private ShowTypeRepository showTypeRepository;

  @Test
  void testFindByWrestler() {
    // Given
    Wrestler wrestler =
        Wrestler.builder().name("Test Wrestler").creationDate(Instant.now()).build();
    wrestlerRepository.save(wrestler);

    Season season1 = new Season();
    season1.setName("Season 1");
    season1.setStartDate(Instant.now());
    seasonRepository.save(season1);

    Season season2 = new Season();
    season2.setName("Season 2");
    season2.setStartDate(Instant.now());
    seasonRepository.save(season2);

    SegmentType type = new SegmentType();
    type.setName("Match");
    segmentTypeRepository.save(type);

    ShowType showType = new ShowType();
    showType.setName("Weekly");
    showType.setDescription("Weekly Show Type");
    showType.setExpectedMatches(3);
    showType.setExpectedPromos(2);
    showTypeRepository.save(showType);

    Show show1 = new Show();
    show1.setName("Show 1");
    show1.setSeason(season1);
    show1.setShowDate(LocalDate.now());
    show1.setType(showType);
    show1.setDescription("Test Description");
    showRepository.save(show1);

    Segment segment1 = new Segment();
    segment1.setShow(show1);
    segment1.setSegmentType(type);
    segment1.setSegmentDate(Instant.now());
    segment1.addParticipant(wrestler);
    segmentRepository.save(segment1);

    // When
    List<Season> results = seasonRepository.findByWrestler(wrestler);

    // Then
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getName()).isEqualTo("Season 1");
  }
}
