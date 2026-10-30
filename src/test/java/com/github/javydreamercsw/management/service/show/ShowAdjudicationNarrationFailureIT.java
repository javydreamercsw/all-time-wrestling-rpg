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

import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Integration test for the 2.13.0 adjudication failure posture on a real database: when no AI
 * narration provider is available, adjudication completes, the brief description survives, and the
 * narration flag stays false (so a later adjudication can still narrate once a provider exists).
 */
class ShowAdjudicationNarrationFailureIT extends ManagementIntegrationTest {

  @Autowired private ShowService showService;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private UniverseRepository universeRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private ShowPlanningService showPlanningService;

  // Replaces the real factory bean for this context — no provider available at all.
  @MockitoBean private SegmentNarrationServiceFactory narrationServiceFactory;

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

    wrestlerA = seedWrestler("Narration Failure IT A");
    wrestlerB = seedWrestler("Narration Failure IT B");

    com.github.javydreamercsw.management.domain.show.type.ShowType weeklyType =
        showTypeRepository.findAll().stream()
            .filter(t -> t.getCategory() == ShowCategory.WEEKLY)
            .findFirst()
            .orElseThrow();
    show =
        showService.createShow(
            "Narration Failure IT Show " + System.nanoTime(),
            "2.13.0 narration failure fixture",
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

    // The mocked factory provides no provider — getBestAvailableService() returns null.
    org.mockito.Mockito.when(narrationServiceFactory.getBestAvailableService()).thenReturn(null);
    org.mockito.Mockito.when(narrationServiceFactory.summarizeNarration(org.mockito.Mockito.any()))
        .thenThrow(new IllegalStateException("No provider"));
  }

  private Wrestler seedWrestler(String name) {
    Wrestler w = new Wrestler();
    w.setName(name + " " + System.nanoTime());
    w.setActive(true);
    w.setIsPlayer(false);
    w.setGender(com.github.javydreamercsw.base.domain.wrestler.Gender.MALE);
    return wrestlerRepository.saveAndFlush(w);
  }

  @Test
  @DisplayName("Provider unavailable: adjudication completes, brief description survives")
  void adjudicateShow_providerUnavailable_adjudicatesWithBriefDescription() {
    showPlanningService.approveSegments(
        show, List.of(singles(wrestlerA.getName(), wrestlerB.getName())));

    Segment segment = segmentRepository.findByShowOrderBySegmentOrderAsc(show).get(0);
    String briefNarration = segment.getNarration();

    showService.adjudicateShow(show.getId());

    Segment after = segmentRepository.findById(segment.getId()).orElseThrow();
    assertThat(after.getAdjudicationStatus()).isEqualTo(AdjudicationStatus.ADJUDICATED);
    // No provider — nothing was generated, the flag stays false (a later pass can narrate),
    // and the brief description the AI planning wrote was not clobbered.
    assertThat(after.isNarrationGenerated()).isFalse();
    assertThat(after.getNarration()).isEqualTo(briefNarration);
  }

  private ProposedSegment singles(String w1, String w2) {
    ProposedSegment segment = new ProposedSegment();
    segment.setType(singlesType.getName());
    segment.setTeams(List.of(List.of(w1), List.of(w2)));
    return segment;
  }
}
