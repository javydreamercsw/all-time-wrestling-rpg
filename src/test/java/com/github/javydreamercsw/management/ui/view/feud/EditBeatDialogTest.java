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
package com.github.javydreamercsw.management.ui.view.feud;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.feud.FeudBeatParticipantRole;
import com.github.javydreamercsw.management.domain.feud.FeudScript;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeat;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeatStatus;
import com.github.javydreamercsw.management.domain.feud.FeudScriptStatus;
import com.github.javydreamercsw.management.domain.feud.FeudScriptWinnerControl;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.feud.FeudScriptService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.github.mvysny.kaributesting.v10.HasValueUtilsKt;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

class EditBeatDialogTest extends AbstractViewTest {

  @Mock private FeudScriptService feudScriptService;

  private Wrestler wrestler1;
  private Wrestler wrestler2;
  private Wrestler externalWrestler;
  private FeudScript script;
  private FeudScriptBeat beat;

  @BeforeEach
  void setup() {
    wrestler1 = new Wrestler();
    wrestler1.setId(1L);
    wrestler1.setName("Adam Axe");
    wrestler2 = new Wrestler();
    wrestler2.setId(2L);
    wrestler2.setName("Bob Boulder");
    externalWrestler = new Wrestler();
    externalWrestler.setId(30L);
    externalWrestler.setName("Randy Orton");

    script = new FeudScript();
    script.setName("Title Arc");
    script.setStatus(FeudScriptStatus.ACTIVE);
    script.setRivalry(rivalry());

    beat = new FeudScriptBeat();
    beat.setId(10L);
    beat.setBeatOrder(2);
    beat.setBeatStatus(FeudScriptBeatStatus.PENDING);
    beat.setSegmentType("Singles Match");
    beat.setWinnerControl(FeudScriptWinnerControl.AI_PICKS);
    beat.setScript(script);

    when(feudScriptService.updateBeat(any(), any(), any())).thenAnswer(inv -> inv.getArgument(1));
  }

  private Rivalry rivalry() {
    Rivalry rivalry = new Rivalry();
    rivalry.setWrestler1(wrestler1);
    rivalry.setWrestler2(wrestler2);
    return rivalry;
  }

  private EditBeatDialog openDialog() {
    EditBeatDialog dialog =
        new EditBeatDialog(
            script,
            beat,
            List.of(wrestler1, wrestler2),
            List.of("Singles Match", "Ladder Match", "Tag Team Match"),
            List.of("No Disqualification"),
            feudScriptService,
            List.of(externalWrestler),
            null,
            null);
    dialog.open();
    UI.getCurrent().add(dialog);
    return dialog;
  }

  @Test
  @DisplayName("Opening the dialog pre-fills the editor from the pending beat")
  void openDialog_preFillsFromBeat() {
    EditBeatDialog dialog = openDialog();

    @SuppressWarnings("unchecked")
    ComboBox<String> matchType =
        _get(dialog, ComboBox.class, spec -> spec.withCaption("Match Type"));
    assertEquals("Singles Match", matchType.getValue());

    @SuppressWarnings("unchecked")
    RadioButtonGroup<String> winnerControl =
        _get(dialog, RadioButtonGroup.class, spec -> spec.withCaption("Winner"));
    assertEquals("AI Picks", winnerControl.getValue());
  }

  @Test
  @DisplayName("Saving persists the edited beat through FeudScriptService.updateBeat")
  void save_callsUpdateBeatWithEditedFields() {
    EditBeatDialog dialog = openDialog();

    @SuppressWarnings("unchecked")
    ComboBox<String> matchType =
        _get(dialog, ComboBox.class, spec -> spec.withCaption("Match Type"));
    HasValueUtilsKt._setValue(matchType, "Ladder Match", true);

    @SuppressWarnings("unchecked")
    RadioButtonGroup<String> winnerControl =
        _get(dialog, RadioButtonGroup.class, spec -> spec.withCaption("Winner"));
    HasValueUtilsKt._setValue(winnerControl, "Booker Picks", true);

    @SuppressWarnings("unchecked")
    ComboBox<Wrestler> plannedWinner =
        _get(dialog, ComboBox.class, spec -> spec.withCaption("Planned Winner"));
    HasValueUtilsKt._setValue(plannedWinner, wrestler2, true);

    Button saveBtn = _get(dialog, Button.class, spec -> spec.withText("Save Changes"));
    _click(saveBtn);

    ArgumentCaptor<FeudScriptBeat> editedCaptor = ArgumentCaptor.forClass(FeudScriptBeat.class);
    verify(feudScriptService).updateBeat(same(script), same(beat), editedCaptor.capture());
    FeudScriptBeat edited = editedCaptor.getValue();
    assertEquals("Ladder Match", edited.getSegmentType());
    assertEquals(FeudScriptWinnerControl.BOOKER_PICKS, edited.getWinnerControl());
    assertEquals(wrestler2, edited.getPlannedWinner());
    // The dialog preserves the beat's order for the service.
    assertEquals(2, edited.getBeatOrder());
  }

  @Test
  @DisplayName("Save without match type does not call the service")
  void save_withoutMatchType_doesNotCallService() {
    EditBeatDialog dialog = openDialog();

    @SuppressWarnings("unchecked")
    ComboBox<String> matchType =
        _get(dialog, ComboBox.class, spec -> spec.withCaption("Match Type"));
    HasValueUtilsKt._setValue(matchType, null, true);

    Button saveBtn = _get(dialog, Button.class, spec -> spec.withText("Save Changes"));
    _click(saveBtn);

    verify(feudScriptService, never()).updateBeat(any(), any(), any());
  }

  @Test
  @DisplayName("Persisted external opponent is pre-selected and carried through the edit")
  void save_keepsPersistedExternalParticipants() {
    beat.addExternalParticipant(externalWrestler, FeudBeatParticipantRole.OPPONENT);
    EditBeatDialog dialog = openDialog();

    @SuppressWarnings("unchecked")
    ComboBox<Wrestler> opponentCombo =
        _get(dialog, ComboBox.class, spec -> spec.withCaption("External Opponent"));
    assertEquals(externalWrestler, opponentCombo.getValue());

    @SuppressWarnings("unchecked")
    ComboBox<String> matchType =
        _get(dialog, ComboBox.class, spec -> spec.withCaption("Match Type"));
    HasValueUtilsKt._setValue(matchType, "Ladder Match", true);

    Button saveBtn = _get(dialog, Button.class, spec -> spec.withText("Save Changes"));
    _click(saveBtn);

    ArgumentCaptor<FeudScriptBeat> captor = ArgumentCaptor.forClass(FeudScriptBeat.class);
    verify(feudScriptService).updateBeat(same(script), same(beat), captor.capture());
    FeudScriptBeat edited = captor.getValue();
    assertEquals(1, edited.getExternalParticipants().size());
    assertEquals(externalWrestler.getName(), edited.getExternalOpponents().get(0).getName());
  }

  @Test
  @DisplayName("Editing keeps externals selectable even if they dropped off the candidate roster")
  void openDialog_persistedExternalMissingFromCandidates_stillSelectable() {
    beat.addExternalParticipant(externalWrestler, FeudBeatParticipantRole.EXTRA);
    EditBeatDialog dialog = openDialog();

    @SuppressWarnings("unchecked")
    MultiSelectComboBox<Wrestler> extrasMulti =
        _get(dialog, MultiSelectComboBox.class, spec -> spec.withCaption("External Extras"));
    assertTrue(extrasMulti.getValue().contains(externalWrestler));
    assertFalse(
        extrasMulti.getListDataView().getItems().noneMatch(w -> w.equals(externalWrestler)));
  }
}
