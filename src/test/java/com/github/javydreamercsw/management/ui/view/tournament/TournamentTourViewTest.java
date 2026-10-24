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
package com.github.javydreamercsw.management.ui.view.tournament;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentRound;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.service.show.ShowFacade;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.tournament.TournamentFormat;
import com.github.javydreamercsw.management.service.tournament.TournamentRunnerService;
import com.github.javydreamercsw.management.service.tournament.TournamentRunnerService.ManualResultReport;
import com.github.javydreamercsw.management.service.tournament.TournamentService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerFacade;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

/** Karibu tests for the interactive tournament tour (ATW-6h2y). */
class TournamentTourViewTest extends AbstractViewTest {

  @Mock private TournamentService tournamentService;
  @Mock private TournamentRunnerService tournamentRunnerService;
  @Mock private WrestlerFacade wrestlerFacade;
  @Mock private ShowFacade showFacade;
  @Mock private UniverseContextService universeContextService;
  @Mock private SegmentRuleService segmentRuleService;
  @Mock private SegmentTypeService segmentTypeService;
  @Mock private ShowService showService;
  @Mock private TitleService titleService;
  @Mock private WrestlerService wrestlerService;
  @Mock private TeamService teamService;
  @Mock private TournamentFormat format;

  private TournamentTourView view;

  @BeforeEach
  void setUp() {
    lenient().when(universeContextService.getCurrentUniverseId()).thenReturn(1L);
    lenient().when(universeContextService.getCurrentUniverse()).thenReturn(Optional.empty());
    // Wizard-open-time reads (all PreAuthorize("isAuthenticated()")) — the dialog constructor
    // hits these the moment step 1's button opens it.
    lenient().when(tournamentService.getAvailableFormats()).thenReturn(List.of(format));
    lenient().when(format.getFormatId()).thenReturn("SINGLE_ELIMINATION");
    lenient().when(format.getDisplayName()).thenReturn("Single Elimination");
    lenient().when(titleService.findAll()).thenReturn(List.of());
    lenient().when(showService.getUpcomingShows(50)).thenReturn(List.of());
    lenient().when(segmentTypeService.findAll()).thenReturn(List.of());
    lenient().when(segmentRuleService.findAll()).thenReturn(List.of());
    lenient().when(wrestlerService.getAllWrestlers()).thenReturn(List.of());
    lenient().when(wrestlerFacade.getTitleService()).thenReturn(titleService);
    lenient().when(wrestlerFacade.getWrestlerService()).thenReturn(wrestlerService);
    lenient().when(wrestlerFacade.getTeamService()).thenReturn(teamService);
    lenient().when(showFacade.getShowService()).thenReturn(showService);
    lenient().when(showFacade.getSegmentRuleService()).thenReturn(segmentRuleService);
    lenient().when(showFacade.getSegmentTypeService()).thenReturn(segmentTypeService);
    lenient().when(tournamentService.countEligibleEntrants(any(), any())).thenReturn(20);

    view =
        new TournamentTourView(
            tournamentService,
            tournamentRunnerService,
            wrestlerFacade,
            showFacade,
            universeContextService);
    UI.getCurrent().add(view);
    view.beforeEnter(null);
  }

  // ── Step 1: Create a Tournament (wizard-backed) ───────────────────────────

  @Test
  @DisplayName("Step 1 opens the full creation wizard instead of asking only for a name")
  void step1_opensCreationWizard() {
    _get(view, Button.class, spec -> spec.withId("tour-create-button")).click();

    Dialog wizard =
        _get(
            UI.getCurrent(),
            TournamentCreationWizardDialog.class,
            spec -> spec.withPredicate(Dialog::isOpened));
    assertThat(wizard.isOpened()).isTrue();
    // The quick-name path is gone: no tour-name-field text field remains.
    assertThat(_find(view, TextField.class, spec -> spec.withId("tour-name-field"))).isEmpty();
  }

  @Test
  @DisplayName("Wizard completion advances the tour to step 2")
  void step1_wizardCompletionAdvancesTour() {
    _get(view, Button.class, spec -> spec.withId("tour-create-button")).click();
    Dialog wizard =
        _get(
            UI.getCurrent(),
            TournamentCreationWizardDialog.class,
            spec -> spec.withPredicate(Dialog::isOpened));

    // Complete the wizard the way a user does: fill the Details step, advance, Create with
    // Auto seeding (the default). The stubbed createTournament returns the tour's tournament.
    Tournament created = tournamentWithId(42L);
    created.setName("Wizard Cup");
    when(tournamentService.createTournament(
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            anyBoolean(),
            anyBoolean(),
            any(),
            any()))
        .thenReturn(created);
    when(tournamentService.save(any())).thenReturn(created);
    when(tournamentService.seedAuto(any(), anyInt(), anyLong())).thenReturn(List.of());

    _get(UI.getCurrent(), TextField.class, spec -> spec.withId("wizard-tournament-name"))
        .setValue("Wizard Cup");
    @SuppressWarnings({"rawtypes", "unchecked"})
    ComboBox<TournamentFormat> formatCombo =
        (ComboBox) _get(UI.getCurrent(), ComboBox.class, spec -> spec.withId("wizard-format"));
    formatCombo.setValue(format);
    _get(UI.getCurrent(), Button.class, spec -> spec.withId("wizard-next")).click();
    _get(UI.getCurrent(), Button.class, spec -> spec.withText("Create Tournament")).click();

    verify(tournamentService)
        .createTournament(
            eq("Wizard Cup"),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            anyBoolean(),
            anyBoolean(),
            any(),
            any());
    // The tour advanced: step 2's content is rendered and the wizard closed.
    H2 heading = _get(view, H2.class, spec -> spec.withId("tour-step-title"));
    assertThat(heading.getText()).contains("Step 2 of 5");
    assertThat(wizard.isOpened()).isFalse();
  }

  // ── Step 3: Seed and Start ────────────────────────────────────────────────

  @Test
  @DisplayName("Step 3 seeds with the chosen count and starts the tournament")
  void step3_seedAndStart() {
    Tournament t = tournamentWithId(42L);
    // The tour must resolve tournaments through the detached-safe accessor — a plain
    // findById leaves lazy collections uninitialized and startTournament throws
    // LazyInitializationException outside the session (found in live testing, ATW-6h2y).
    when(tournamentService.findByIdWithDetails(42L)).thenReturn(Optional.of(t));
    when(tournamentService.seedAuto(any(), anyInt(), anyLong())).thenReturn(List.of());
    when(tournamentService.startTournament(any())).thenReturn(t);
    startAtStep(3);

    IntegerField count = _get(view, IntegerField.class, spec -> spec.withId("tour-entrant-count"));
    count.setValue(8);

    _get(view, Button.class, spec -> spec.withId("tour-start-button")).click();

    verify(tournamentService).seedAuto(any(), eq(8), anyLong());
    verify(tournamentService).startTournament(t);
    verify(tournamentService, Mockito.never()).findById(anyLong());
  }

  @Test
  @DisplayName("Step 3 skips seeding when the wizard already seeded the bracket")
  void step3_skipsSeedWhenAlreadySeeded() {
    Tournament t = tournamentWithId(42L);
    TournamentEntry entry = new TournamentEntry();
    entry.setId(101L);
    t.setEntries(new ArrayList<>(List.of(entry)));
    when(tournamentService.findByIdWithDetails(42L)).thenReturn(Optional.of(t));
    when(tournamentService.startTournament(any())).thenReturn(t);
    startAtStep(3);

    // No count field — seeding already happened in the wizard.
    assertThat(_find(view, IntegerField.class, spec -> spec.withId("tour-entrant-count")))
        .isEmpty();

    _get(view, Button.class, spec -> spec.withId("tour-start-button")).click();

    verify(tournamentService, Mockito.never()).seedAuto(any(), anyInt(), anyLong());
    verify(tournamentService).startTournament(t);
  }

  // ── Step 4: Enter Results ────────────────────────────────────────────────

  @Test
  @DisplayName("Step 4 lists open matches and records a manual winner")
  void step4_recordManualResult() {
    Tournament t = startedTournamentWithOpenMatch();
    when(tournamentService.findByIdWithDetails(42L)).thenReturn(Optional.of(t));
    startAtStep(4);

    TournamentEntry winner = t.getRounds().getFirst().getMatches().getFirst().getEntrant1();
    when(tournamentRunnerService.recordManualResult(any(), anyLong(), anyLong(), any()))
        .thenReturn(new ManualResultReport(null, t.getRounds().getFirst(), winner, true, false));

    Button record = _get(view, Button.class, spec -> spec.withId("tour-record-button-1"));
    record.click();

    verify(tournamentRunnerService).recordManualResult(eq(t), eq(1L), eq(winner.getId()), any());
  }

  @Test
  @DisplayName("Step 4 offers Decide by Engine per match")
  void step4_engineOption() {
    when(tournamentService.findByIdWithDetails(42L))
        .thenReturn(Optional.of(startedTournamentWithOpenMatch()));
    startAtStep(4);

    _get(view, Button.class, spec -> spec.withId("tour-engine-button-1"));
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private void startAtStep(int step) {
    // Walk the state machine directly (session-only progress lives on the view)
    ReflectionTestUtils.setField(view, "tournamentId", 42L);
    ReflectionTestUtils.setField(view, "currentStep", step);
    view.renderStep();
  }

  private Tournament tournamentWithId(long id) {
    Tournament t = new Tournament();
    t.setId(id);
    t.setName("My First Tournament");
    return t;
  }

  private Tournament startedTournamentWithOpenMatch() {
    Tournament t = tournamentWithId(42L);

    Wrestler w1 = new Wrestler();
    w1.setId(1L);
    w1.setName("Apollo");
    Wrestler w2 = new Wrestler();
    w2.setId(2L);
    w2.setName("Tormenta");

    TournamentEntry e1 = new TournamentEntry();
    e1.setId(101L);
    e1.setWrestler(w1);
    TournamentEntry e2 = new TournamentEntry();
    e2.setId(102L);
    e2.setWrestler(w2);

    TournamentMatch match = new TournamentMatch();
    match.setId(1L);
    match.setEntrant1(e1);
    match.setEntrant2(e2);

    TournamentRound round = new TournamentRound();
    round.setId(201L);
    round.setRoundNumber(1);
    round.setRoundName("Quarter-Final");
    round.setMatches(new ArrayList<>(List.of(match)));
    match.setRound(round);

    t.setRounds(new ArrayList<>(List.of(round)));
    return t;
  }
}
