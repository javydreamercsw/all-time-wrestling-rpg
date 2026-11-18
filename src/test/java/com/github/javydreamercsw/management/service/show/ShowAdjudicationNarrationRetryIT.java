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

import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
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
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Integration tests for the narration retry path (ATW-v0vh): a provider failure during adjudication
 * records a persistent {@code narrationLastError} mark and does NOT block outcomes, and {@code
 * retryNarration} regenerates ONLY narration — winners, status, and rating from the original run
 * are untouched (no double-applied fan/title effects).
 */
class ShowAdjudicationNarrationRetryIT extends ManagementIntegrationTest {

  @Autowired private ShowService showService;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private UniverseRepository universeRepository;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private ShowPlanningService showPlanningService;

  // Replaces the real factory bean: fails on the first narrateSegment call, succeeds after.
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

    wrestlerA = seedWrestler("Narration Retry IT A");
    wrestlerB = seedWrestler("Narration Retry IT B");

    ShowType weeklyType =
        showTypeRepository.findAll().stream()
            .filter(t -> t.getCategory() == ShowCategory.WEEKLY)
            .findFirst()
            .orElseThrow();
    show =
        showService.createShow(
            "Narration Retry IT Show " + System.nanoTime(),
            "ATW-v0vh narration retry fixture",
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
    w.setGender(Gender.MALE);
    return wrestlerRepository.saveAndFlush(w);
  }

  private ProposedSegment singles(String w1, String w2) {
    ProposedSegment segment = new ProposedSegment();
    segment.setType(singlesType.getName());
    segment.setTeams(List.of(List.of(w1), List.of(w2)));
    return segment;
  }

  /** Fails the first narration call, then delegates to the real Mock provider behavior. */
  private void failFirstNarrationThenSucceed() {
    Mockito.when(narrationServiceFactory.getBestAvailableService())
        .thenReturn(Mockito.mock(SegmentNarrationService.class));
    Mockito.when(narrationServiceFactory.getBestAvailableService().isAvailable()).thenReturn(true);
    Mockito.when(narrationServiceFactory.getBestAvailableService().narrateSegment(Mockito.any()))
        .thenThrow(new IllegalStateException("Provider blew up"))
        .thenReturn("Retried narration text for the segment.");
    Mockito.when(narrationServiceFactory.summarizeNarration(Mockito.any()))
        .thenReturn("Retried summary.");
  }

  @Test
  @DisplayName("Failed narration is marked and retried narration-only without re-adjudication")
  void narrationFailure_marksSegment_retryNarratesWithoutReadjudication() {
    failFirstNarrationThenSucceed();
    showPlanningService.approveSegments(
        show, List.of(singles(wrestlerA.getName(), wrestlerB.getName())));

    ShowService.AdjudicationResult first = showService.adjudicateShow(show.getId(), false);
    assertThat(first.failedSegmentIds()).hasSize(1);
    Long failedId = first.failedSegmentIds().get(0);

    // The failure mark is persisted; the segment STILL got adjudicated this run.
    Segment afterFirstRun = segmentRepository.findById(failedId).orElseThrow();
    assertThat(afterFirstRun.getNarrationLastError()).contains("Provider blew up");
    assertThat(afterFirstRun.getAdjudicationStatus()).isEqualTo(AdjudicationStatus.ADJUDICATED);
    assertThat(afterFirstRun.isNarrationGenerated()).isFalse();
    List<String> winnersBefore =
        afterFirstRun.getWinners().stream().map(Wrestler::getName).toList();

    // Retry: narration-only — no re-adjudication, no outcome changes.
    ShowService.AdjudicationResult retryResult =
        showService.retryNarration(show.getId(), List.of(failedId));

    assertThat(retryResult.narratedCount()).isEqualTo(1);
    assertThat(retryResult.adjudicatedCount()).isEqualTo(0);
    assertThat(retryResult.failedSegmentIds()).isEmpty();

    Segment afterRetry = segmentRepository.findById(failedId).orElseThrow();
    assertThat(afterRetry.isNarrationGenerated()).isTrue();
    assertThat(afterRetry.getNarration()).isEqualTo("Retried narration text for the segment.");
    // The mark is cleared by the successful generation.
    assertThat(afterRetry.getNarrationLastError()).isNull();
    // CRITICAL: outcomes from the original run are untouched.
    assertThat(afterRetry.getAdjudicationStatus()).isEqualTo(AdjudicationStatus.ADJUDICATED);
    assertThat(afterRetry.getWinners().stream().map(Wrestler::getName).toList())
        .isEqualTo(winnersBefore);
  }

  @Test
  @DisplayName("A second failed retry keeps the mark and reports the id")
  void narrationRetry_failureAgain_keepsMark() {
    failFirstNarrationThenSucceed();
    showPlanningService.approveSegments(
        show, List.of(singles(wrestlerA.getName(), wrestlerB.getName())));

    ShowService.AdjudicationResult first = showService.adjudicateShow(show.getId(), false);
    Long failedId = first.failedSegmentIds().get(0);

    // Now make every call fail, including the retry.
    Mockito.when(narrationServiceFactory.getBestAvailableService().narrateSegment(Mockito.any()))
        .thenThrow(new IllegalStateException("Still down"));

    ShowService.AdjudicationResult retryResult =
        showService.retryNarration(show.getId(), List.of(failedId));

    assertThat(retryResult.narratedCount()).isEqualTo(0);
    assertThat(retryResult.failedSegmentIds()).containsExactly(failedId);
    Segment after = segmentRepository.findById(failedId).orElseThrow();
    assertThat(after.getNarrationLastError()).contains("Still down");
    assertThat(after.isNarrationGenerated()).isFalse();
  }

  @Test
  @DisplayName("retryNarration ignores ids that are not narratable on this show")
  void narrationRetry_ignoresForeignOrAlreadyNarratedIds() {
    failFirstNarrationThenSucceed();
    showPlanningService.approveSegments(
        show, List.of(singles(wrestlerA.getName(), wrestlerB.getName())));

    ShowService.AdjudicationResult first = showService.adjudicateShow(show.getId(), false);
    Long failedId = first.failedSegmentIds().get(0);

    // Retry with the failed id, a foreign id, and a nonexistent id.
    ShowService.AdjudicationResult result =
        showService.retryNarration(show.getId(), List.of(failedId, 999_999L));

    assertThat(result.narratedCount()).isEqualTo(1);
    assertThat(result.failedSegmentIds()).isEmpty();
    // The foreign/nonexistent id was not attempted (only 1 narration attempted).
    assertThat(result.attemptedNarrationCount()).isEqualTo(1);
  }
}
