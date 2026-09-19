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
package com.github.javydreamercsw.management.service.show.planning;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.base.ai.ollama.OllamaSegmentNarrationService;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatchRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentStatus;
import com.github.javydreamercsw.management.service.HolidayService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.planning.dto.ShowPlanningContextDTO;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

/**
 * Real-LLM end-to-end test of the tournament-fed PLE workflow (ATW-oahn), against a LOCAL Ollama
 * instance: the LLM plans a card for a PLE whose template pairs an event-only segment type with an
 * unseeded SCHEDULED tournament; approval must feed that segment from the bracket (auto-seed +
 * auto-start), not from the AI's participant picks.
 *
 * <p><b>Opt-in only</b> — skipped unless {@code OLLAMA_BASE_URL} is set (same gate as {@link
 * OllamaShowPlanningIT}):
 *
 * <pre>
 *   ollama serve &amp;&amp; ollama pull llama3.2:1b
 *   OLLAMA_BASE_URL=http://localhost:11434 mvn -Pintegration-test verify \
 *       -Dit.test=OllamaTournamentFedPleIT -Dsurefire.skip=true
 * </pre>
 *
 * <p><b>Assertion strategy:</b> LLM output is nondeterministic, so the AI-proposed card is only
 * required to parse; the deterministic assertions are all on the approval side — the fed segment's
 * participants must be the bracket's booked match entrants (never arbitrary AI choices), the
 * tournament ends IN_PROGRESS with the winner mirrored.
 */
@Slf4j
@Tag("ollama")
@ActiveProfiles("ollama")
@EnabledIfEnvironmentVariable(named = "OLLAMA_BASE_URL", matches = ".+")
class OllamaTournamentFedPleIT extends AbstractTournamentFedPleIT {

  @Autowired private OllamaSegmentNarrationService ollamaService;
  @Autowired private HolidayService holidayService;
  @Autowired private TournamentMatchRepository matchRepository;
  @Autowired private SegmentRepository segmentRepository;
  @Autowired private ShowService showService;

  private ShowPlanningAiService aiService;

  @BeforeEach
  void setUpFixtureAndAi() {
    buildTournamentFedPleFixture();
    aiService =
        new ShowPlanningAiService(
            new SegmentNarrationServiceFactory(List.of(ollamaService)),
            objectMapper,
            segmentTypeService,
            segmentRuleService,
            holidayService);
  }

  @Test
  void aiPlannedCard_feedsTournamentOnApproval() {
    // Plan the card with the real LLM — full roster, so the prompt may use any type; what
    // matters is that the paired event-only type reaches the card (the template's assignment
    // surfaces it as "Event Segment Types ... ONLY on this show").
    ShowPlanningContextDTO context = showPlanningService.getShowPlanningContext(show);
    ProposedShow proposed = aiService.planShow(context);

    log.info("Ollama proposed {} segment(s)", proposed.getSegments().size());

    // The AI card must include the paired event type for the tournament to be exercised —
    // the prompt lists it as allowed ONLY here. If the model ignored it, plan again with the
    // type forced onto the first segment (still LLM-generated content for everything else).
    boolean hasPairedType =
        proposed.getSegments().stream().anyMatch(s -> eventType.getName().equals(s.getType()));
    if (!hasPairedType) {
      ProposedSegment forced = new ProposedSegment();
      forced.setType(eventType.getName());
      forced.setNarration("Tournament-fed match");
      proposed.getSegments().add(forced);
    }

    approveCard(show, proposed.getSegments());

    // Deterministic assertions on the approval side.
    assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.IN_PROGRESS);

    Optional<Segment> fedSegment =
        segmentRepository.findByShowOrderBySegmentOrderAsc(show).stream()
            .filter(s -> s.getSegmentType().getId().equals(eventType.getId()))
            .findFirst();
    assertThat(fedSegment).as("paired event type must land on the card").isPresent();

    List<TournamentMatch> allBooked =
        tournamentService.findByIdWithDetails(tournament.getId()).orElseThrow().getRounds().stream()
            .flatMap(r -> r.getMatches().stream())
            .filter(m -> m.getSegment() != null)
            .toList();
    assertThat(allBooked).hasSize(1);
    TournamentMatch booked = allBooked.get(0);
    assertThat(fedSegment.orElseThrow().getWrestlers())
        .extracting(w -> w.getId())
        .containsExactlyInAnyOrder(
            booked.getEntrant1().getWrestler().getId(), booked.getEntrant2().getWrestler().getId());
    assertThat(booked.getWinner()).isNotNull();
  }

  @Test
  void aiCardWithoutPairedType_leavesTournamentUntouched() {
    // A weekly-style context (no template pairing consulted by the LLM beyond the prompt)
    // where the AI proposes only ordinary types: approval must not seed or start the cup.
    Show plainShow =
        showService.createShow(
            "Fed Weekly " + System.nanoTime(),
            "Non-paired show fixture",
            show.getType().getId(),
            LocalDate.now().plusDays(3),
            null,
            null,
            universe.getId(),
            null,
            null,
            null);

    ShowPlanningContextDTO context = showPlanningService.getShowPlanningContext(plainShow);
    ProposedShow proposed = aiService.planShow(context);
    approveCard(plainShow, proposed.getSegments());

    assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.SCHEDULED);
  }
}
