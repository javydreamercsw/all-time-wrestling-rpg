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

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentRound;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.tournament.TournamentRunnerService;
import com.github.javydreamercsw.management.service.tournament.TournamentRunnerService.ManualResultReport;
import com.github.javydreamercsw.management.service.tournament.TournamentService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

/** Karibu tests for the interactive tournament tour (ATW-6h2y). */
class TournamentTourViewTest extends AbstractViewTest {

  @Mock private TournamentService tournamentService;
  @Mock private TournamentRunnerService tournamentRunnerService;
  @Mock private WrestlerService wrestlerService;
  @Mock private AccountService accountService;
  @Mock private SecurityUtils securityUtils;
  @Mock private UniverseContextService universeContextService;

  private TournamentTourView view;

  @BeforeEach
  void setUp() {
    lenient().when(universeContextService.getCurrentUniverseId()).thenReturn(1L);
    lenient().when(securityUtils.getCurrentAccountId()).thenReturn(Optional.of(1L));
    Account account = new Account("player", "pw", "p@t.com");
    lenient().when(accountService.get(1L)).thenReturn(Optional.of(account));
    lenient().when(wrestlerService.findAllActiveWrestlers()).thenReturn(List.of());

    view =
        new TournamentTourView(
            tournamentService,
            tournamentRunnerService,
            wrestlerService,
            accountService,
            securityUtils,
            universeContextService);
    UI.getCurrent().add(view);
    view.beforeEnter(null);
  }

  // ── Step 1: Create a Tournament ───────────────────────────────────────────

  @Test
  @DisplayName("Step 1 asks for a tournament name and creates it on Create")
  void step1_createTournament() {
    H2 heading = _get(view, H2.class, spec -> spec.withId("tour-step-title"));
    assertThat(heading.getText()).contains("Step 1 of 5");

    Tournament created = new Tournament();
    created.setId(42L);
    created.setName("My First Tournament");
    when(tournamentService.createTournament(
            eq("My First Tournament"),
            anyString(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            eq(false),
            eq(false),
            any(),
            any()))
        .thenReturn(created);

    TextField nameField = _get(view, TextField.class, spec -> spec.withId("tour-name-field"));
    nameField.setValue("My First Tournament");

    _get(view, Button.class, spec -> spec.withId("tour-create-button")).click();

    verify(tournamentService)
        .createTournament(
            eq("My First Tournament"),
            anyString(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            any(),
            eq(false),
            eq(false),
            any(),
            any());
    Span status = _get(view, Span.class, spec -> spec.withId("tour-status"));
    assertThat(status.getText()).contains("My First Tournament");
  }

  @Test
  @DisplayName("Step 1 without a name shows an error and does not create")
  void step1_requiresName() {
    _get(view, Button.class, spec -> spec.withId("tour-create-button")).click();

    Span status = _get(view, Span.class, spec -> spec.withId("tour-status"));
    assertThat(status.getText()).contains("name");
    verify(tournamentService, Mockito.never())
        .createTournament(
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
            ArgumentMatchers.anyBoolean(),
            ArgumentMatchers.anyBoolean(),
            any(),
            any());
  }

  // ── Step 3: Seed and Start ────────────────────────────────────────────────

  @Test
  @DisplayName("Step 3 seeds automatically and starts the tournament")
  void step3_seedAndStart() {
    Tournament t = tournamentWithId(42L);
    // The tour must resolve tournaments through the detached-safe accessor — a plain
    // findById leaves lazy collections uninitialized and startTournament throws
    // LazyInitializationException outside the session (found in live testing, ATW-6h2y).
    when(tournamentService.findByIdWithDetails(42L)).thenReturn(Optional.of(t));
    when(tournamentService.seedAuto(any(), anyInt(), anyLong())).thenReturn(List.of());
    when(tournamentService.startTournament(any())).thenReturn(t);
    startAtStep(3);

    _get(view, Button.class, spec -> spec.withId("tour-start-button")).click();

    verify(tournamentService).seedAuto(any(), anyInt(), anyLong());
    verify(tournamentService).startTournament(t);
    verify(tournamentService, Mockito.never()).findById(anyLong());
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
