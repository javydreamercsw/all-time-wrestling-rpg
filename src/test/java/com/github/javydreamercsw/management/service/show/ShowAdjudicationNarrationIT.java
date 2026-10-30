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
package com.github.javydreamercsw.management.service.show;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.SegmentResult;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.WellKnownSegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowCategory;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.show.planning.ProposedSegment;
import com.github.javydreamercsw.management.service.show.planning.ShowPlanningService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for the 2.13.0 adjudication narration phase: pending segments still holding the
 * AI-planning brief description get full narrations (Mock provider, test profile) before
 * adjudication runs; a booked DRAW carries no winners and is not warned about; AI failure does not
 * block the show.
 */
class ShowAdjudicationNarrationIT extends ManagementIntegrationTest {

  @Autowired private ShowService showService;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private UniverseRepository universeRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private ShowPlanningService showPlanningService;

  private Universe universe;
  private Wrestler wrestlerA;
  private Wrestler wrestlerB;
  private Show show;
  private SegmentType singlesType;

  @BeforeEach
  void setUpFixture() {
    universe =
        universeRepository
            .findByName("Default Universe")
            .orElseThrow(() -> new IllegalStateException("Default Universe missing from test DB"));

    wrestlerA = seedWrestler("Narration IT A");
    wrestlerB = seedWrestler("Narration IT B");

    com.github.javydreamercsw.management.domain.show.type.ShowType weeklyType =
        showTypeRepository.findAll().stream()
            .filter(t -> t.getCategory() == ShowCategory.WEEKLY)
            .findFirst()
            .orElseThrow();
    show =
        showService.createShow(
            "Narration IT Show " + System.nanoTime(),
            "2.13.0 narration fixture",
            weeklyType.getId(),
            LocalDate.now().plusDays(7),
            null,
            null,
            universe.getId(),
            null,
            null,
            null);
    singlesType =
        segmentTypeRepository.findByCode(WellKnownSegmentType.ONE_ON_ONE.getCode()).orElseThrow();
  }

  private Wrestler seedWrestler(String name) {
    Wrestler w = new Wrestler();
    w.setName(name + " " + System.nanoTime());
    w.setActive(true);
    w.setIsPlayer(false);
    w.setGender(com.github.javydreamercsw.base.domain.wrestler.Gender.MALE);
    w = wrestlerRepository.saveAndFlush(w);
    return w;
  }

  private ProposedSegment singles(String w1, String w2) {
    ProposedSegment segment = new ProposedSegment();
    segment.setType(singlesType.getName());
    segment.setTeams(List.of(List.of(w1), List.of(w2)));
    return segment;
  }

  @Test
  @DisplayName("Adjudication narrates pending segments, then adjudicates them")
  void adjudicateShow_narratesPendingSegments_thenAdjudicates() {
    showPlanningService.approveSegments(
        show, List.of(singles(wrestlerA.getName(), wrestlerB.getName())));

    Segment segment = segmentRepository.findByShowOrderBySegmentOrderAsc(show).get(0);
    assertThat(segment.getAdjudicationStatus()).isEqualTo(AdjudicationStatus.PENDING);
    assertThat(segment.isNarrationGenerated()).isFalse();

    showService.adjudicateShow(show.getId());

    Segment after = segmentRepository.findById(segment.getId()).orElseThrow();
    // The Mock provider (test profile) produced a real narration and filled the summary.
    assertThat(after.getNarration()).isNotBlank();
    assertThat(after.isNarrationGenerated()).isTrue();
    assertThat(after.getSummary()).isNotBlank();
    assertThat(after.getAdjudicationStatus()).isEqualTo(AdjudicationStatus.ADJUDICATED);
  }

  @Test
  @DisplayName("Re-adjudication does not regenerate narrations")
  void adjudicateShow_secondRun_doesNotRegenerate() {
    showPlanningService.approveSegments(
        show, List.of(singles(wrestlerA.getName(), wrestlerB.getName())));

    showService.adjudicateShow(show.getId());
    Segment first =
        segmentRepository
            .findById(segmentRepository.findByShowOrderBySegmentOrderAsc(show).get(0).getId())
            .orElseThrow();
    String narration = first.getNarration();

    // Re-adjudicating (no pending segments) must leave the narration untouched.
    showService.adjudicateShow(show.getId());

    Segment after = segmentRepository.findById(first.getId()).orElseThrow();
    assertThat(after.getNarration()).isEqualTo(narration);
  }

  @Test
  @DisplayName("A booked draw adjudicates with no winners and no warning path")
  void adjudicateShow_bookedDraw_hasNoWinners() {
    ProposedSegment drawMatch = singles(wrestlerA.getName(), wrestlerB.getName());
    drawMatch.setResult(SegmentResult.DRAW);
    showPlanningService.approveSegments(show, List.of(drawMatch));

    Segment segment = segmentRepository.findByShowOrderBySegmentOrderAsc(show).get(0);
    assertThat(segment.getResult()).isEqualTo(SegmentResult.DRAW);
    assertThat(segment.getWinners()).isEmpty();

    // Pre-flight: the booked draw is intentional — no missing-winner warning.
    ShowService.AdjudicationPreFlight preFlight =
        showService.getAdjudicationPreFlight(show.getId());
    assertThat(preFlight.missingWinnerWarnings()).isEmpty();

    showService.adjudicateShow(show.getId());

    Segment after = segmentRepository.findById(segment.getId()).orElseThrow();
    assertThat(after.getResult()).isEqualTo(SegmentResult.DRAW);
    assertThat(after.getWinners()).isEmpty();
    assertThat(after.getNarration()).isNotBlank();
  }

  @Test
  @DisplayName("Pre-flight lists a winnerless match as a warning")
  void preFlight_winnerlessMatch_isAWarning() {
    showPlanningService.approveSegments(
        show, List.of(singles(wrestlerA.getName(), wrestlerB.getName())));

    ShowService.AdjudicationPreFlight preFlight =
        showService.getAdjudicationPreFlight(show.getId());

    assertThat(preFlight.missingWinnerWarnings()).hasSize(1);
    assertThat(preFlight.toNarrate()).hasSize(1);
  }

  @Test
  @DisplayName("adjudicateShow throws for a missing show")
  void adjudicateShow_missingShow_throws() {
    assertThatThrownBy(() -> showService.adjudicateShow(999_999L))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
