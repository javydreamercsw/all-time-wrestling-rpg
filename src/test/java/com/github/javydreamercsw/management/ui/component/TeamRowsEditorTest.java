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
}
