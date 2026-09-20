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

import static com.github.mvysny.kaributesting.v10.GridKt._getCellComponent;
import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.management.domain.show.ShowRepository;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.show.ShowFacade;
import com.github.javydreamercsw.management.service.tournament.TournamentFormat;
import com.github.javydreamercsw.management.service.tournament.TournamentService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.ui.ViewContext;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Karibu tests for the tournament detail view (ATW-oahn): pre-start seed editing (reorder +
 * replace) and its lifecycle gating. Grid cell components are reached through {@code
 * _getCellComponent} (tree-wide search cannot traverse grid cells); the replace dialog is driven
 * through the view's test hook.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TournamentDetailViewTest extends AbstractViewTest {

  @Mock private TournamentService tournamentService;
  @Mock private ShowRepository showRepository;
  @Mock private ShowFacade showFacade;
  @Mock private ViewContext viewContext;
  @Mock private SegmentRuleService segmentRuleService;
  @Mock private UniverseContextService universeContextService;
  @Mock private TournamentFormat format;

  private TournamentDetailView view;
  private Tournament tournament;
  private Wrestler alpha;
  private Wrestler bravo;
  private Wrestler charlie;
  private Wrestler echo;
  private List<TournamentEntry> entries;

  @BeforeEach
  void setup() {
    lenient().when(format.getFormatId()).thenReturn("SINGLE_ELIMINATION");
    lenient().when(tournamentService.getAvailableFormats()).thenReturn(List.of(format));
    lenient().when(segmentRuleService.findAll()).thenReturn(List.of());
    lenient().when(showFacade.getSegmentRuleService()).thenReturn(segmentRuleService);
    lenient().when(viewContext.getUniverseContextService()).thenReturn(universeContextService);
    lenient().when(universeContextService.getCurrentUniverseId()).thenReturn(1L);
    lenient()
        .when(showRepository.findByShowDateGreaterThanEqualOrderByShowDate(any(), any()))
        .thenReturn(List.of());

    alpha = wrestler(1L, "Alpha");
    bravo = wrestler(2L, "Bravo");
    charlie = wrestler(3L, "Charlie");
    Wrestler delta = wrestler(4L, "Delta");
    // Not entered anywhere — the only valid replacement candidate the dialog can offer.
    echo = wrestler(5L, "Echo");

    tournament = new Tournament();
    tournament.setId(1L);
    tournament.setName("Crown Cup");
    tournament.setFormatId("SINGLE_ELIMINATION");
    tournament.setStatus(TournamentStatus.SCHEDULED);
    entries = new java.util.ArrayList<>();
    for (int i = 0; i < 4; i++) {
      entries.add(
          TournamentEntry.builder()
              .id((long) (i + 1))
              .wrestler(List.of(alpha, bravo, charlie, delta).get(i))
              .seed(i + 1)
              .build());
    }
    tournament.setEntries(entries);
    tournament.setRounds(new java.util.ArrayList<>());

    lenient()
        .when(tournamentService.findEligibleWrestlersSortedByFans(any(), anyLong()))
        .thenReturn(List.of(alpha, bravo, charlie, delta, echo));
    // findByIdWithDetails copies the refreshed graph onto the fixture's instance (detached-safe
    // refresh path used by the view after each seeding edit).
    lenient()
        .when(tournamentService.findByIdWithDetails(1L))
        .thenAnswer(
            inv -> {
              tournament.setEntries(new java.util.ArrayList<>(entries));
              return java.util.Optional.of(tournament);
            });

    buildView();
  }

  private void buildView() {
    view = new TournamentDetailView(tournamentService, showRepository, showFacade, viewContext);
    view.setTournamentForTest(tournament);
    view.buildContentForTest();
    UI.getCurrent().add(view);
  }

  private static Wrestler wrestler(Long id, String name) {
    Wrestler w = new Wrestler();
    w.setId(id);
    w.setName(name);
    w.setActive(true);
    w.setGender(Gender.MALE);
    return w;
  }

  private static List<Button> buttonsWithTooltip(Component root, String tooltipSubstring) {
    return _find(
        root,
        Button.class,
        spec ->
            spec.withPredicate(
                (Predicate<Button>)
                    btn ->
                        btn.getTooltip() != null
                            && btn.getTooltip().getText() != null
                            && btn.getTooltip().getText().contains(tooltipSubstring)));
  }

  @Test
  @DisplayName("Scheduled tournament renders seed reorder controls for every entrant")
  void scheduledTournament_showsSeedEditingControls() {
    Grid<TournamentEntry> grid = _get(view, Grid.class);
    for (int row = 0; row < 4; row++) {
      Component reorderCell = _getCellComponent(grid, row, "reorder");
      assertEquals(2, _find(reorderCell, Button.class).size(), "row " + row + " needs up+down");
      Component swapCell = _getCellComponent(grid, row, "swap");
      assertEquals(
          1, _find(swapCell, Button.class, spec -> spec.withText("Replace")).size(), "row " + row);
    }
  }

  @Test
  @DisplayName("Top seed cannot move up; bottom seed cannot move down")
  void seedControls_disableAtEdges() {
    Grid<TournamentEntry> grid = _get(view, Grid.class);
    Button topUp =
        buttonsWithTooltip(_getCellComponent(grid, 0, "reorder"), "Move up one seed").get(0);
    assertFalse(topUp.isEnabled(), "Seed 1 has nowhere to move up to");
    Button bottomDown =
        buttonsWithTooltip(_getCellComponent(grid, 3, "reorder"), "Move down one seed").get(0);
    assertFalse(bottomDown.isEnabled(), "The last seed has nowhere to move down to");
  }

  @Test
  @DisplayName("Moving a seed persists the swapped order through the service")
  void moveSeed_persistsSwappedOrder() {
    // Down-arrow on the top seed row swaps seeds 1 and 2.
    Component cell = _getCellComponent(_get(view, Grid.class), 0, "reorder");
    buttonsWithTooltip(cell, "Move down one seed").get(0).click();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass((Class) List.class);
    verify(tournamentService).reorderSeeds(anyLong(), captor.capture());
    // Entry 1 moves to position 2; entry 2 takes position 1; the rest stay put.
    assertEquals(List.of(2L, 1L, 3L, 4L), captor.getValue());
  }

  @Test
  @DisplayName("Start Tournament persists the bracket and switches to IN_PROGRESS")
  void startTournament_switchesStatus() {
    Mockito.when(tournamentService.startTournament(tournament)).thenReturn(tournament);

    _get(view, Button.class, spec -> spec.withText("Start Tournament")).click();

    Mockito.verify(tournamentService).startTournament(tournament);
  }

  @Test
  @DisplayName("Replace dialog opens with a wrestler picker for the chosen seed")
  void replaceDialog_opens() {
    view.openReplaceDialogForTest(entries.get(0));

    Dialog dialog = _get(UI.getCurrent(), Dialog.class);
    assertNotNull(dialog);
    ComboBox<Wrestler> picker =
        _get(dialog, ComboBox.class, spec -> spec.withLabel("New wrestler"));
    assertNotNull(picker);
  }

  @Test
  @DisplayName("Confirming replace routes through the service with the picked wrestler")
  void replaceDialog_confirmCallsService() {
    view.openReplaceDialogForTest(entries.get(1));

    Dialog dialog = _get(UI.getCurrent(), Dialog.class);
    ComboBox<Wrestler> picker =
        _get(dialog, ComboBox.class, spec -> spec.withLabel("New wrestler"));
    // Echo (id 5) is eligible and not entered — a valid replacement for seed 2 (Bravo).
    picker.setValue(echo);

    // Scoped to the dialog so the grid's own Replace buttons can't shadow it.
    _get(dialog, Button.class, spec -> spec.withText("Replace")).click();

    Mockito.verify(tournamentService).replaceEntrant(1L, 2L, 5L);
  }

  @Test
  @DisplayName("In-progress tournaments lock seed editing")
  void inProgressTournament_locksSeeds() {
    tournament.setStatus(TournamentStatus.IN_PROGRESS);
    buildView();

    assertEquals(0, buttonsWithTooltip(view, "Move up one seed").size());
    assertEquals(0, _find(view, Button.class, spec -> spec.withText("Replace")).size());
    verify(tournamentService, never()).reorderSeeds(anyLong(), any());
  }
}
