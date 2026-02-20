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
package com.github.javydreamercsw.management.service.season;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.SegmentStatus;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.title.TitleReign;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.dto.SeasonStatsDTO;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeasonStatsService Tests")
class SeasonStatsServiceTest {

  @Mock private SegmentRepository segmentRepository;
  @Mock private SeasonRepository seasonRepository;

  @InjectMocks private SeasonStatsService seasonStatsService;

  private Wrestler wrestler;
  private Season season;
  private SegmentType matchType;
  private SegmentType promoType;

  @BeforeEach
  void setUp() {
    wrestler = new Wrestler();
    wrestler.setId(1L);
    wrestler.setName("Test Wrestler");
    wrestler.setFans(1000L);

    season = new Season();
    season.setId(1L);
    season.setName("Season 1");
    season.setStartDate(Instant.parse("2024-01-01T00:00:00Z"));
    season.setEndDate(Instant.parse("2024-03-31T23:59:59Z"));

    matchType = new SegmentType();
    matchType.setName("One on One");

    promoType = new SegmentType();
    promoType.setName("Promo");
  }

  @Test
  @DisplayName("Should calculate season stats correctly for a wrestler")
  void testCalculateSeasonStatsForPlayer() {
    // Given
    Segment win1 = createSegment(101L, matchType, true);
    Segment win2 = createSegment(102L, matchType, true);
    Segment loss1 = createSegment(103L, matchType, false);
    Segment draw1 = createSegment(104L, matchType, null); // Null winner means draw if completed
    Segment promo = createSegment(105L, promoType, true); // Promos shouldn't count for wins/losses

    List<Segment> segments = Arrays.asList(win1, win2, loss1, draw1, promo);
    when(segmentRepository.findByWrestlerParticipationAndSeason(
            eq(wrestler), eq(season), any(Pageable.class)))
        .thenReturn(new PageImpl<>(segments));

    // Accolades
    Title title = new Title();
    title.setName("World Championship");
    TitleReign reign = new TitleReign();
    reign.setTitle(title);
    reign.setStartDate(Instant.parse("2024-02-01T00:00:00Z"));
    reign.setEndDate(Instant.parse("2024-02-15T00:00:00Z"));
    wrestler.setReigns(Arrays.asList(reign));

    // When
    SeasonStatsDTO stats = seasonStatsService.calculateStats(wrestler, season);

    // Then
    assertThat(stats).isNotNull();
    assertThat(stats.getSeasonName()).isEqualTo("Season 1");
    assertThat(stats.getWins()).isEqualTo(2);
    assertThat(stats.getLosses()).isEqualTo(1);
    assertThat(stats.getDraws()).isEqualTo(1);
    assertThat(stats.getAccolades()).contains("World Championship");
  }

  private Segment createSegment(Long id, SegmentType type, Boolean isWinner) {
    Segment segment = new Segment();
    segment.setId(id);
    segment.setSegmentType(type);
    segment.setStatus(SegmentStatus.COMPLETED);
    segment.setAdjudicationStatus(AdjudicationStatus.ADJUDICATED);

    segment.addParticipant(wrestler);
    if (isWinner != null) {
      if (isWinner) {
        segment.setWinners(Arrays.asList(wrestler));
      } else {
        Wrestler opponent = new Wrestler();
        opponent.setId(2L);
        opponent.setName("Opponent");
        segment.addParticipant(opponent);
        segment.setWinners(Arrays.asList(opponent));
      }
    } else {
      // Draw - completed but no winners
      segment.setWinners(new ArrayList<>());
    }

    return segment;
  }
}
