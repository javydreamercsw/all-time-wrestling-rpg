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

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplate;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateRepository;
import com.github.javydreamercsw.management.domain.show.template.ShowTemplateSegmentAssignment;
import com.github.javydreamercsw.management.domain.show.type.ShowCategory;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentStatus;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.tournament.TournamentService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Shared fixture for tournament-fed PLE end-to-end tests (ATW-oahn): builds a PLE show of a
 * template carrying a type+tournament AUTO_ATTACH assignment, plus a seeded roster. Subclasses
 * supply the card — hand-built {@link ProposedSegment}s (deterministic IT) or an AI-planned card
 * (Ollama IT) — and call {@link #approveCard} to run the real {@code approveSegments}.
 */
abstract class AbstractTournamentFedPleIT extends ManagementIntegrationTest {

  @Autowired protected TournamentService tournamentService;
  @Autowired protected ShowService showService;
  @Autowired protected ShowTemplateService showTemplateService;
  @Autowired protected ShowTypeRepository showTypeRepository;
  @Autowired protected SegmentTypeRepository segmentTypeRepository;
  @Autowired protected ShowTemplateRepository showTemplateRepository;
  @Autowired protected TournamentRepository tournamentRepository;
  @Autowired protected UniverseRepository universeRepository;
  @Autowired protected WrestlerRepository wrestlerRepository;
  @Autowired protected ShowRepository showRepository;
  @Autowired protected ShowPlanningService showPlanningService;

  protected Universe universe;
  protected List<Wrestler> roster;
  protected Show show;
  protected Tournament tournament;
  protected SegmentType eventType;

  protected void buildTournamentFedPleFixture() {
    // The integration-test base seeds a "Default Universe" per test and guarantees exactly one —
    // reuse it (never create a second; findByName is not backed by a unique constraint under
    // ddl-auto=none, so duplicates would poison every subsequent test's baseSetUp).
    this.universe =
        universeRepository.findByName("Default Universe").orElseGet(this::seedDefaultUniverse);
    roster = seedRoster(8);
    eventType = seedEventType();
    ShowType pleType = seedPleType();
    tournament = seedTournament();
    ShowTemplate template = seedTemplateWithPairing(pleType, eventType, tournament);
    show = seedShow(template);
  }

  private Universe seedDefaultUniverse() {
    // Guarded last-resort for the (unexpected) case where baseSetUp's guarantee was skipped —
    // re-check inside a synchronized block so two threads cannot both insert.
    synchronized (AbstractTournamentFedPleIT.class) {
      return universeRepository
          .findByName("Default Universe")
          .orElseGet(
              () -> {
                Universe u = new Universe();
                u.setName("Default Universe");
                return universeRepository.saveAndFlush(u);
              });
    }
  }

  private List<Wrestler> seedRoster(int count) {
    List<Wrestler> roster = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Wrestler w = new Wrestler();
      w.setName("Fed Wrestler " + i + " " + System.nanoTime());
      w.setActive(true);
      w.setIsPlayer(false);
      w.setGender(Gender.MALE);
      w = wrestlerRepository.saveAndFlush(w);
      WrestlerState state = new WrestlerState();
      state.setWrestler(w);
      // Fans well above the sync roster's (which default to 0) so the top-N seeds are exactly
      // these fixture wrestlers, in fan order, no matter how many sync wrestlers exist.
      state.setFans(1_000_000L * (count - i));
      state.setUniverse(universe);
      w.getWrestlerStates().add(state);
      roster.add(wrestlerRepository.saveAndFlush(w));
    }
    return roster;
  }

  private SegmentType seedEventType() {
    SegmentType type = new SegmentType();
    type.setName("Test Rumble " + System.nanoTime());
    type.setDescription("IT fixture event-only type");
    type.setEventOnly(true);
    return segmentTypeRepository.saveAndFlush(type);
  }

  private ShowType seedPleType() {
    return showTypeRepository.findAll().stream()
        .filter(t -> t.getCategory() == ShowCategory.PLE)
        .findFirst()
        .orElseGet(
            () -> {
              ShowType type = new ShowType();
              type.setName("PLE IT " + System.nanoTime());
              type.setCategory(ShowCategory.PLE);
              return showTypeRepository.saveAndFlush(type);
            });
  }

  private Tournament seedTournament() {
    Tournament t = new Tournament();
    t.setName("Fed Cup IT " + System.nanoTime());
    t.setFormatId("SINGLE_ELIMINATION");
    t.setStatus(TournamentStatus.SCHEDULED);
    t.setUniverse(universe);
    return tournamentRepository.saveAndFlush(t);
  }

  private ShowTemplate seedTemplateWithPairing(
      ShowType pleType, SegmentType eventType, Tournament cup) {
    ShowTemplate template = new ShowTemplate();
    template.setName("Fed PLE Template " + System.nanoTime());
    template.setShowType(pleType);
    template = showTemplateService.save(template);

    ShowTemplateSegmentAssignment row = new ShowTemplateSegmentAssignment();
    row.setTemplate(template);
    row.setSegmentType(eventType);
    row.setTournament(cup);
    row.setMode(ShowTemplateSegmentAssignment.AssignmentMode.AUTO_ATTACH);
    showTemplateService.syncSegmentAssignments(template.getId(), List.of(row));
    return showTemplateRepository.findByIdWithAssignments(template.getId()).orElseThrow();
  }

  private Show seedShow(ShowTemplate template) {
    return showService.createShow(
        "Fed PLE " + System.nanoTime(),
        "Tournament-fed PLE fixture",
        template.getShowType().getId(),
        LocalDate.now().plusDays(7),
        null,
        template.getId(),
        universe.getId(),
        null,
        null,
        null);
  }

  /** Run the real approval path over the given card. */
  protected void approveCard(Show targetShow, List<ProposedSegment> card) {
    showPlanningService.approveSegments(targetShow, card);
  }

  /**
   * Seed {@code count} bracket entries onto {@code t} (top of the fixture roster by fans) and flush
   * — used by edition-cycle tests to hand a tournament a playable bracket.
   */
  protected void seedTournamentEntries(Tournament t, int count) {
    List<Wrestler> seeded = roster.subList(0, count);
    for (int i = 0; i < seeded.size(); i++) {
      tournamentService.addEntry(t, seeded.get(i), i + 1);
    }
  }
}
