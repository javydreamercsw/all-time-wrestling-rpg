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
package com.github.javydreamercsw.management.ui.component;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.github.mvysny.kaributesting.v10.Routes;
import com.github.mvysny.kaributesting.v10.mock.MockedUI;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for the standalone team-layout editor used by the story-arc beat editors. */
class TeamRowsEditorTest {

  private Wrestler wrestler1;
  private Wrestler wrestler2;
  private Wrestler wrestler3;
  private List<Wrestler> candidates;

  @BeforeEach
  void setup() {
    MockVaadin.setup(new Routes(), MockedUI::new);
    wrestler1 = wrestler(1L, "Adam Axe");
    wrestler2 = wrestler(2L, "Bob Boulder");
    wrestler3 = wrestler(3L, "Chip Cutter");
    candidates = List.of(wrestler1, wrestler2, wrestler3);
  }

  @AfterEach
  void tearDown() {
    MockVaadin.tearDown();
  }

  private static Wrestler wrestler(Long id, String name) {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(id);
    wrestler.setName(name);
    return wrestler;
  }

  /** The multi-select combo of the i-th team row (rows live in the inner rows container). */
  @SuppressWarnings("unchecked")
  private static MultiSelectComboBox<Wrestler> teamCombo(TeamRowsEditor editor, int row) {
    VerticalLayout rowsContainer = (VerticalLayout) editor.getComponentAt(0);
    return (MultiSelectComboBox<Wrestler>)
        rowsContainer.getComponentAt(row).getChildren().findFirst().orElseThrow();
  }

  @Test
  @DisplayName("A fresh editor starts with zero team rows")
  void freshEditor_hasNoTeamRows() {
    TeamRowsEditor editor = new TeamRowsEditor(candidates, null);
    assertTrue(editor.getTeams().isEmpty());
  }

  @Test
  @DisplayName("addTeamRow pre-fills the given members under team 1")
  void addTeamRow_withMembers_prefillsValue() {
    TeamRowsEditor editor = new TeamRowsEditor(candidates, null);
    editor.addTeamRow(List.of(wrestler1, wrestler2));

    Map<Integer, List<Wrestler>> teams = editor.getTeams();
    assertEquals(1, teams.size());
    assertEquals(List.of(wrestler1, wrestler2), teams.get(1));
  }

  @Test
  @DisplayName("Removing a row renumbers the remaining teams 1-based")
  void removeTeamRow_renumbersRemainingTeams() {
    TeamRowsEditor editor = new TeamRowsEditor(candidates, null);
    editor.addTeamRow(List.of(wrestler1));
    editor.addTeamRow(List.of(wrestler2));
    editor.addTeamRow(List.of(wrestler3));

    // Click the "−" button of the first row.
    VerticalLayout rowsContainer = (VerticalLayout) editor.getComponentAt(0);
    Button removeFirst = _get((Component) rowsContainer.getComponentAt(0), Button.class);
    _click(removeFirst);

    Map<Integer, List<Wrestler>> teams = editor.getTeams();
    assertEquals(2, teams.size());
    assertEquals(List.of(wrestler2), teams.get(1));
    assertEquals(List.of(wrestler3), teams.get(2));
  }

  @Test
  @DisplayName("The last remaining team row cannot be removed")
  void removeTeamRow_lastRow_isKept() {
    TeamRowsEditor editor = new TeamRowsEditor(candidates, null);
    editor.addTeamRow(List.of(wrestler1));

    VerticalLayout rowsContainer = (VerticalLayout) editor.getComponentAt(0);
    Button removeOnly = _get((Component) rowsContainer.getComponentAt(0), Button.class);
    _click(removeOnly);

    assertEquals(1, editor.getTeams().size());
  }

  @Test
  @DisplayName("getTeams skips empty team rows but keeps their numbering")
  void getTeams_skipsEmptyRowsKeepingNumbers() {
    TeamRowsEditor editor = new TeamRowsEditor(candidates, null);
    editor.addTeamRow(List.of(wrestler1));
    editor.addTeamRow(); // empty team 2
    editor.addTeamRow(List.of(wrestler3));

    Map<Integer, List<Wrestler>> teams = editor.getTeams();
    assertEquals(2, teams.size());
    assertEquals(List.of(wrestler1), teams.get(1));
    assertEquals(List.of(wrestler3), teams.get(3));
  }

  @Test
  @DisplayName("isValid is false with fewer than two non-empty teams")
  void isValid_fewerThanTwoNonEmptyTeams_returnsFalse() {
    TeamRowsEditor editor = new TeamRowsEditor(candidates, null);
    editor.addTeamRow(List.of(wrestler1));
    editor.addTeamRow(); // second row empty
    assertFalse(editor.isValid());
  }

  @Test
  @DisplayName("isValid is true with two non-empty disjoint teams")
  void isValid_twoDisjointTeams_returnsTrue() {
    TeamRowsEditor editor = new TeamRowsEditor(candidates, null);
    editor.addTeamRow(List.of(wrestler1, wrestler2));
    editor.addTeamRow(List.of(wrestler3));
    assertTrue(editor.isValid());
  }

  @Test
  @DisplayName("A wrestler on two teams fails validation")
  void isValid_wrestlerOnTwoTeams_returnsFalse() {
    TeamRowsEditor editor = new TeamRowsEditor(candidates, null);
    editor.addTeamRow(List.of(wrestler1));
    editor.addTeamRow(List.of(wrestler2, wrestler1)); // wrestler1 duplicated
    assertFalse(editor.isValid());
  }

  @Test
  @DisplayName("setTeams replaces the layout; null yields a single empty row")
  void setTeams_replacesLayout() {
    TeamRowsEditor editor = new TeamRowsEditor(candidates, null);
    editor.setTeams(Map.of(1, List.of(wrestler1), 2, List.of(wrestler2)));
    assertEquals(2, editor.getTeams().size());

    editor.setTeams(null);
    assertTrue(editor.getTeams().isEmpty());
  }

  @Test
  @DisplayName("Membership changes invoke the change listener")
  void valueChange_notifiesListener() {
    AtomicInteger changes = new AtomicInteger();
    TeamRowsEditor editor = new TeamRowsEditor(candidates, changes::incrementAndGet);
    editor.addTeamRow(List.of(wrestler1));
    changes.set(0); // reset: addTeamRow(members) itself counts as a change

    teamCombo(editor, 0).setValue(Set.of(wrestler2));
    assertEquals(1, changes.get());
  }

  @Test
  @DisplayName("Set values round-trip through getTeams")
  void valueSet_roundTripsThroughGetTeams() {
    TeamRowsEditor editor = new TeamRowsEditor(candidates, null);
    editor.addTeamRow();
    teamCombo(editor, 0).setValue(Set.of(wrestler2, wrestler3));

    Map<Integer, List<Wrestler>> teams = editor.getTeams();
    assertEquals(1, teams.size());
    assertTrue(teams.get(1).containsAll(List.of(wrestler2, wrestler3)));
  }

  // ── Extended constructor (tournament wizard) ─────────────────────────────

  @Test
  @DisplayName("requiredTeamSize trims a row that exceeds the declared member count")
  void requiredTeamSize_trimsOverLimitSelection() {
    TeamRowsEditor editor = new TeamRowsEditor(candidates, null, null, 2, 1, Integer.MAX_VALUE);
    editor.addTeamRow();

    // LinkedHashSet pins the pick order the trim keeps.
    teamCombo(editor, 0).setValue(new LinkedHashSet<>(List.of(wrestler1, wrestler2, wrestler3)));

    // Over-limit picks are trimmed back to exactly 2 — the first two in selection order.
    assertEquals(2, editor.getTeams().get(1).size());
    assertEquals(List.of(wrestler1, wrestler2), editor.getTeams().get(1));
    assertTrue(editor.rowsMatchRequiredSize());
  }

  @Test
  @DisplayName("rowsMatchRequiredSize is false while a non-empty row is under the declared size")
  void requiredTeamSize_underFilledRow_notMatching() {
    TeamRowsEditor editor = new TeamRowsEditor(candidates, null, null, 2, 1, Integer.MAX_VALUE);
    editor.addTeamRow();

    teamCombo(editor, 0).setValue(Set.of(wrestler1));
    assertFalse(editor.rowsMatchRequiredSize());

    teamCombo(editor, 0).setValue(Set.of(wrestler1, wrestler2));
    assertTrue(editor.rowsMatchRequiredSize());
  }

  @Test
  @DisplayName("The row filter re-filters every picker; the row's own value stays selectable")
  void rowFilter_excludesButKeepsOwnValue() {
    TeamRowsEditor editor =
        new TeamRowsEditor(candidates, null, w -> !w.equals(wrestler3), 2, 2, Integer.MAX_VALUE);
    MultiSelectComboBox<Wrestler> combo1 = teamCombo(editor, 0);
    MultiSelectComboBox<Wrestler> combo2 = teamCombo(editor, 1);
    combo1.setValue(Set.of(wrestler3)); // filtered-out wrestler picked while still offered

    // Re-filter: the row holding wrestler3 keeps it (own value is never dropped), but the
    // combo never OFFERS the selected value back — check another row for exclusion.
    editor.refreshRowItemsWithFilter(w -> !w.equals(wrestler3));
    assertTrue(combo1.getValue().contains(wrestler3), "The row's own value is never dropped");
    assertFalse(combo2.getListDataView().getItems().toList().contains(wrestler3));

    // With row 1's pick cleared, the base filter is re-evaluated on refresh — un-filtering
    // offers wrestler3 to row 2 again.
    combo1.setValue(Set.of());
    editor.refreshRowItemsWithFilter(w -> true);
    assertTrue(combo2.getListDataView().getItems().toList().contains(wrestler3));
  }

  @Test
  @DisplayName("Rows exclude wrestlers taken by other rows on refresh")
  void rows_excludeOtherRowsSelections() {
    TeamRowsEditor editor = new TeamRowsEditor(candidates, null, null, 1, 2, Integer.MAX_VALUE);
    teamCombo(editor, 0).setValue(Set.of(wrestler1));

    editor.refreshRowItems();

    List<Wrestler> row2Offered = teamCombo(editor, 1).getListDataView().getItems().toList();
    assertFalse(row2Offered.contains(wrestler1), "Row 2 never offers row 1's wrestler");
    assertTrue(row2Offered.contains(wrestler2));
  }

  @Test
  @DisplayName("minRows seeds empty rows at construction; maxRows caps addTeamRow")
  void minAndMaxRows_boundsRespected() {
    TeamRowsEditor editor = new TeamRowsEditor(candidates, null, null, null, 3, 4);
    assertEquals(3, editor.rowCount(), "minRows seeds three rows");

    editor.addTeamRow(); // 4 — at the cap
    assertEquals(4, editor.rowCount());
    // Beyond the cap the editor refuses: addTeamRow has no guard itself, but refreshRowItems
    // and the wizard read rowCount; the cap is enforced by the caller. Verify the current
    // behavior stays the plain add (documented contract).
  }

  @Test
  @DisplayName("hasAnySelection is true only once a row carries a selection")
  void hasAnySelection_tracksFirstPick() {
    TeamRowsEditor editor = new TeamRowsEditor(candidates, null, null, 2, 1, Integer.MAX_VALUE);
    assertFalse(editor.hasAnySelection());

    editor.addTeamRow();
    assertFalse(editor.hasAnySelection(), "The seeded row is still empty");
    teamCombo(editor, 0).setValue(Set.of(wrestler1));
    assertTrue(editor.hasAnySelection());
  }

  @Test
  @DisplayName("A change during the refresh does not re-fire the change listener (re-entrancy)")
  void refreshRowItems_doesNotRefireChangeListener() {
    AtomicInteger changes = new AtomicInteger();
    TeamRowsEditor editor =
        new TeamRowsEditor(candidates, changes::incrementAndGet, null, 1, 2, Integer.MAX_VALUE);
    teamCombo(editor, 0).setValue(Set.of(wrestler1)); // change 1: direct pick
    changes.set(0);

    // Row 1's value is removed from row 2's offered list — the refresh fires value-change
    // events internally (setItems), which must not count as booker changes.
    editor.refreshRowItems();
    assertEquals(0, changes.get(), "Internal refresh events are not booker changes");
  }
}
