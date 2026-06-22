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
package com.github.javydreamercsw.management.ui.view.show;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Karibu tests for EditSegmentDialog focusing on the team-dropdown population bug: when the dialog
 * is opened for an existing segment, the wrestler instances inside Segment.getWrestlersByTeam() are
 * different Java objects from those in PreloadedData.activeWrestlers(). Vaadin's KeyMapper uses an
 * IdentityHashMap, so setValue() with the wrong instances silently sets nothing.
 */
class EditSegmentDialogKaribuTest extends AbstractViewTest {

  // --- canonical instances (what PreloadedData.activeWrestlers() holds) ---
  private Wrestler canonical1;
  private Wrestler canonical2;
  private Wrestler canonical3;

  // --- segment-copy instances (same IDs, different Java objects, as from Segment entity) ---
  private Wrestler segmentCopy1;
  private Wrestler segmentCopy2;

  private EditSegmentDialog.PreloadedData preloaded;
  private WrestlerService wrestlerService;

  @BeforeEach
  void buildFixtures() {
    canonical1 = wrestler(1L, "Alpha");
    canonical2 = wrestler(2L, "Beta");
    canonical3 = wrestler(3L, "Gamma");

    // Deliberately separate instances — same IDs, same names, but not the same objects.
    segmentCopy1 = wrestler(1L, "Alpha");
    segmentCopy2 = wrestler(2L, "Beta");

    preloaded =
        new EditSegmentDialog.PreloadedData(
            List.of(segmentType("Match")),
            List.of(),
            List.of(),
            List.of(),
            List.of(canonical1, canonical2, canonical3),
            Map.of("Alpha", canonical1, "Beta", canonical2, "Gamma", canonical3));

    wrestlerService = mock(WrestlerService.class);
    // alignment-filter path (should never be reached in the no-filter case)
    when(wrestlerService.findAllFiltered(any(), any(), anyLong(), any(Set.class)))
        .thenReturn(List.of(canonical1, canonical2, canonical3));
  }

  /**
   * Core regression: opening the dialog with an existing segment whose wrestlers are DIFFERENT
   * instances than those in the preloaded data must still show the correct selections.
   */
  @Test
  void existingSegmentTeamDropdownsShowSelectedWrestlers() {
    Map<Integer, List<Wrestler>> teams = new LinkedHashMap<>();
    teams.put(1, List.of(segmentCopy1));
    teams.put(2, List.of(segmentCopy2));

    EditSegmentDialog dialog = openDialog(teams);

    assertThat(dialog.getTeamCombos()).hasSize(2);

    Set<Wrestler> team1 = dialog.getTeamCombos().get(0).getValue();
    assertThat(team1).as("Team 1 must show the wrestler from the existing segment").hasSize(1);
    assertThat(team1.iterator().next().getId()).isEqualTo(1L);

    Set<Wrestler> team2 = dialog.getTeamCombos().get(1).getValue();
    assertThat(team2).as("Team 2 must show the wrestler from the existing segment").hasSize(1);
    assertThat(team2.iterator().next().getId()).isEqualTo(2L);
  }

  /** Each team dropdown must expose the full wrestler pool, not just the selected one. */
  @Test
  void existingSegmentTeamDropdownsHaveFullItemPool() {
    Map<Integer, List<Wrestler>> teams = new LinkedHashMap<>();
    teams.put(1, List.of(segmentCopy1));
    teams.put(2, List.of(segmentCopy2));

    EditSegmentDialog dialog = openDialog(teams);

    for (int i = 0; i < dialog.getTeamCombos().size(); i++) {
      MultiSelectComboBox<Wrestler> combo = dialog.getTeamCombos().get(i);
      assertThat(combo.getListDataView().getItemCount())
          .as("Team %d combo must list all preloaded wrestlers", i + 1)
          .isEqualTo(3);
    }
  }

  /**
   * When no alignment filter is active, the dialog must NOT call wrestlerService.findAllFiltered
   * during initial population — the preloaded data is sufficient.
   */
  @Test
  void noRedundantDbQueryOnOpenWhenAlignmentFilterIsBlank() {
    Map<Integer, List<Wrestler>> teams = new LinkedHashMap<>();
    teams.put(1, List.of(segmentCopy1));
    teams.put(2, List.of(segmentCopy2));

    openDialog(teams);

    verify(wrestlerService, never()).findAllFiltered(any(), any(), anyLong(), any(Set.class));
  }

  /** Clicking Add Team must also produce a fully-populated dropdown. */
  @Test
  void addTeamButtonProducesPopulatedDropdown() {
    Map<Integer, List<Wrestler>> teams = new LinkedHashMap<>();
    teams.put(1, List.of(segmentCopy1));
    teams.put(2, List.of(segmentCopy2));

    EditSegmentDialog dialog = openDialog(teams);
    int before = dialog.getTeamCombos().size();

    dialog.getAddTeamButton().click();

    assertThat(dialog.getTeamCombos()).hasSize(before + 1);
    MultiSelectComboBox<Wrestler> newCombo =
        dialog.getTeamCombos().get(dialog.getTeamCombos().size() - 1);
    assertThat(newCombo.getListDataView().getItemCount())
        .as("Newly added team combo must list all wrestlers")
        .isEqualTo(3);
  }

  // ── helpers ──────────────────────────────────────────────────────────────

  private EditSegmentDialog openDialog(Map<Integer, List<Wrestler>> teams) {
    EditSegmentDialog.SegmentDialogData initial =
        new EditSegmentDialog.SegmentDialogData(
            "Match", teams, new ArrayList<>(), Set.of(), null, "", "", "", false, Set.of());

    EditSegmentDialog dialog =
        new EditSegmentDialog(preloaded, initial, wrestlerService, null, 1L, saveData -> {});
    dialog.open();
    return dialog;
  }

  private static Wrestler wrestler(long id, String name) {
    Wrestler w = new Wrestler();
    w.setId(id);
    w.setName(name);
    return w;
  }

  private static SegmentType segmentType(String name) {
    SegmentType st = new SegmentType();
    st.setName(name);
    return st;
  }
}
