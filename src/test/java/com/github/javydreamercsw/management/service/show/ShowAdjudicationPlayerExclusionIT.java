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
package com.github.javydreamercsw.management.service.show;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.WellKnownSegmentType;
import com.github.javydreamercsw.management.domain.show.type.ShowCategory;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
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
 * Integration tests for player-controlled segment exclusion (ATW-v0vh): segments involving {@code
 * isPlayer == true} wrestlers are excluded from adjudication by default (stay PENDING — their
 * outcomes are the player's story), and only an explicit include flag adjudicates them. Pre-flight
 * lists them so the dialog can ask.
 */
class ShowAdjudicationPlayerExclusionIT extends ManagementIntegrationTest {

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
  private Wrestler playerWrestler;
  private Show show;
  private SegmentType singlesType;

  @BeforeEach
  void setUpFixture() {
    universe =
        universeRepository
            .findByName("Default Universe")
            .orElseThrow(() -> new IllegalStateException("Default Universe missing from test DB"));

    wrestlerA = seedWrestler("Exclusion IT A", false);
    wrestlerB = seedWrestler("Exclusion IT B", false);
    playerWrestler = seedWrestler("Exclusion IT Player", true);

    ShowType weeklyType =
        showTypeRepository.findAll().stream()
            .filter(t -> t.getCategory() == ShowCategory.WEEKLY)
            .findFirst()
            .orElseThrow();
    show =
        showService.createShow(
            "Exclusion IT Show " + System.nanoTime(),
            "ATW-v0vh player exclusion fixture",
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

  private Wrestler seedWrestler(String name, boolean isPlayer) {
    Wrestler w = new Wrestler();
    w.setName(name + " " + System.nanoTime());
    w.setActive(true);
    w.setIsPlayer(isPlayer);
    w.setGender(Gender.MALE);
    return wrestlerRepository.saveAndFlush(w);
  }

  private ProposedSegment singles(String w1, String w2) {
    ProposedSegment segment = new ProposedSegment();
    segment.setType(singlesType.getName());
    segment.setTeams(List.of(List.of(w1), List.of(w2)));
    return segment;
  }

  private List<Segment> segments() {
    return segmentRepository.findByShowOrderBySegmentOrderAsc(show);
  }

  @Test
  @DisplayName("Default run excludes every player-involving segment (all-player card)")
  void adjudicateShow_excludesPlayerSegmentByDefault() {
    showPlanningService.approveSegments(
        show,
        List.of(
            singles(wrestlerA.getName(), playerWrestler.getName()),
            singles(wrestlerB.getName(), playerWrestler.getName())));

    ShowService.AdjudicationResult result = showService.adjudicateShow(show.getId(), false);

    List<Segment> after = segments();
    // Both booked segments involve the player — BOTH must be excluded.
    assertThat(after).hasSize(2);
    assertThat(result.skippedPlayerSegmentIds()).hasSize(2);
    for (Segment s : after) {
      Segment reloaded = segmentRepository.findById(s.getId()).orElseThrow();
      assertThat(reloaded.getAdjudicationStatus()).isEqualTo(AdjudicationStatus.PENDING);
      assertThat(reloaded.isNarrationGenerated()).isFalse();
    }
    assertThat(result.adjudicatedCount()).isEqualTo(0);
    assertThat(result.failedSegmentIds()).isEmpty();
  }

  @Test
  @DisplayName("Mixed card: default run processes only the NPC segment")
  void adjudicateShow_mixedCard_processesOnlyNpcSegments() {
    showPlanningService.approveSegments(
        show,
        List.of(
            singles(wrestlerA.getName(), wrestlerB.getName()),
            singles(wrestlerA.getName(), playerWrestler.getName())));

    ShowService.AdjudicationResult result = showService.adjudicateShow(show.getId(), false);

    List<Segment> after = segments();
    assertThat(after).hasSize(2);
    // Exactly one segment adjudicated: the NPC-vs-NPC opener.
    assertThat(result.adjudicatedCount()).isEqualTo(1);
    assertThat(result.skippedPlayerSegmentIds()).hasSize(1);
    for (Segment s : after) {
      Segment reloaded = segmentRepository.findById(s.getId()).orElseThrow();
      boolean involvesPlayer =
          reloaded.getWrestlers().stream().anyMatch(w -> w.getId().equals(playerWrestler.getId()));
      if (involvesPlayer) {
        assertThat(reloaded.getAdjudicationStatus()).isEqualTo(AdjudicationStatus.PENDING);
      } else {
        assertThat(reloaded.getAdjudicationStatus()).isEqualTo(AdjudicationStatus.ADJUDICATED);
        assertThat(reloaded.isNarrationGenerated()).isTrue();
      }
    }
  }

  @Test
  @DisplayName("Explicit include adjudicates the player segment too")
  void adjudicateShow_includePlayerSegments_adjudicatesAll() {
    showPlanningService.approveSegments(
        show, List.of(singles(wrestlerA.getName(), playerWrestler.getName())));

    ShowService.AdjudicationResult result = showService.adjudicateShow(show.getId(), true);

    Segment after = segments().get(0);
    assertThat(after.getAdjudicationStatus()).isEqualTo(AdjudicationStatus.ADJUDICATED);
    assertThat(after.isNarrationGenerated()).isTrue();
    assertThat(result.skippedPlayerSegmentIds()).isEmpty();
    assertThat(result.adjudicatedCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("Pre-flight lists player segments and the pending count")
  void preFlight_listsPlayerSegments() {
    showPlanningService.approveSegments(
        show,
        List.of(
            singles(wrestlerA.getName(), playerWrestler.getName()),
            singles(wrestlerA.getName(), playerWrestler.getName())));

    ShowService.AdjudicationPreFlight preFlight =
        showService.getAdjudicationPreFlight(show.getId());

    assertThat(preFlight.playerSegments()).hasSize(2);
    assertThat(preFlight.pendingSegmentCount()).isEqualTo(2);
    // Both pending segments involve the player — the dialog must warn there is nothing left.
    assertThat(preFlight.pendingSegmentCount()).isEqualTo(preFlight.playerSegments().size());
  }

  @Test
  @DisplayName("Legacy no-flag overload excludes player segments (safe default)")
  void adjudicateShow_noFlagOverload_excludesPlayerSegments() {
    showPlanningService.approveSegments(
        show, List.of(singles(wrestlerA.getName(), playerWrestler.getName())));

    showService.adjudicateShow(show.getId());

    Segment after = segments().get(0);
    assertThat(after.getAdjudicationStatus()).isEqualTo(AdjudicationStatus.PENDING);
  }
}
