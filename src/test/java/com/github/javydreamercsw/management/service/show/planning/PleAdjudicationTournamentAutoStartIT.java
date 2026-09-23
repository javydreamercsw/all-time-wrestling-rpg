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

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.type.ShowCategory;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentStatus;
import com.github.javydreamercsw.management.event.AdjudicationCompletedEvent;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/**
 * PLE-adjudication auto-start end to end (ATW-o4ad follow-up): publishing the real {@code
 * AdjudicationCompletedEvent} for a PLE starts the SCHEDULED tournament whose target PLE comes next
 * — its bracket exists afterwards, so weekly planning previews real pairings instead of
 * placeholders. Unpaired tournaments and weekly adjudications start nothing.
 */
class PleAdjudicationTournamentAutoStartIT extends AbstractTournamentFedPleIT {

  @Autowired private ApplicationEventPublisher eventPublisher;
  @Autowired private ShowTemplateService showTemplateService;
  @Autowired private ShowTypeRepository showTypeRepository;

  private Show weeklyShow;
  private Show earlierPle;

  @BeforeEach
  void setUpFixture() {
    buildTournamentFedPleFixture();
    weeklyShow = seedWeeklyShow();
    // The PLE whose adjudication opens the cycle: a PLE BEFORE the paired one. The tournament's
    // target is the first PLE pairing it AFTER the adjudicated show — the fixture show.
    earlierPle = seedPleShow("Fed Earlier PLE " + System.nanoTime(), LocalDate.now().plusDays(1));
  }

  @Test
  @DisplayName("PLE adjudication event auto-starts the paired SCHEDULED tournament")
  void pleAdjudication_startsTargetTournament() {
    assertThat(tournament.getStatus()).isEqualTo(TournamentStatus.SCHEDULED);

    eventPublisher.publishEvent(new AdjudicationCompletedEvent(this, earlierPle));

    Tournament reloaded = tournamentService.findByIdWithDetails(tournament.getId()).orElseThrow();
    assertThat(reloaded.getStatus()).isEqualTo(TournamentStatus.IN_PROGRESS);
    assertThat(reloaded.getRounds()).isNotEmpty();
  }

  @Test
  @DisplayName("Weekly show adjudication event starts nothing")
  void weeklyAdjudication_startsNothing() {
    eventPublisher.publishEvent(new AdjudicationCompletedEvent(this, weeklyShow));

    assertThat(tournamentService.findById(tournament.getId()).orElseThrow().getStatus())
        .isEqualTo(TournamentStatus.SCHEDULED);
  }

  @Test
  @DisplayName("Only paired tournaments start — an unpaired one stays SCHEDULED")
  void unpairedTournament_staysScheduled() {
    Tournament unpaired = new Tournament();
    unpaired.setName("Unpaired Cup IT " + System.nanoTime());
    unpaired.setFormatId("SINGLE_ELIMINATION");
    unpaired.setStatus(TournamentStatus.SCHEDULED);
    unpaired.setUniverse(universe);
    unpaired = tournamentRepository.saveAndFlush(unpaired);

    eventPublisher.publishEvent(new AdjudicationCompletedEvent(this, earlierPle));

    assertThat(tournamentService.findById(tournament.getId()).orElseThrow().getStatus())
        .isEqualTo(TournamentStatus.IN_PROGRESS);
    assertThat(tournamentService.findById(unpaired.getId()).orElseThrow().getStatus())
        .isEqualTo(TournamentStatus.SCHEDULED);
  }

  // ── Fixture helpers ───────────────────────────────────────────────────────

  private Show seedPleShow(String name, LocalDate date) {
    return showService.createShow(
        name,
        "PLE adjudication fixture",
        show.getType().getId(),
        date,
        null,
        show.getTemplate().getId(),
        universe.getId(),
        null,
        null,
        null);
  }

  private Show seedWeeklyShow() {
    ShowType weeklyType =
        showTypeRepository.findAll().stream()
            .filter(t -> t.getCategory() == ShowCategory.WEEKLY)
            .findFirst()
            .orElseGet(
                () -> {
                  ShowType type = new ShowType();
                  type.setName("Weekly IT " + System.nanoTime());
                  type.setCategory(ShowCategory.WEEKLY);
                  return showTypeRepository.saveAndFlush(type);
                });
    ShowTemplate weeklyTemplate = new ShowTemplate();
    weeklyTemplate.setName("Fed Weekly Template " + System.nanoTime());
    weeklyTemplate.setShowType(weeklyType);
    showTemplateService.save(weeklyTemplate);
    return showService.createShow(
        "Fed Weekly " + System.nanoTime(),
        "Weekly adjudication fixture",
        weeklyType.getId(),
        LocalDate.now().plusDays(1),
        null,
        weeklyTemplate.getId(),
        universe.getId(),
        null,
        null,
        null);
  }
}
