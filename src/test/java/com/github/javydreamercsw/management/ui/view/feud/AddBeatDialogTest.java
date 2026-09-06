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

import com.github.javydreamercsw.management.domain.feud.FeudScript;
import com.github.javydreamercsw.management.domain.feud.FeudScriptBeat;
import com.github.javydreamercsw.management.domain.feud.FeudScriptStatus;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.feud.FeudScriptService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.github.mvysny.kaributesting.v10.HasValueUtilsKt;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

class AddBeatDialogTest extends AbstractViewTest {

  @Mock private FeudScriptService feudScriptService;

  private Wrestler wrestler1;
  private Wrestler wrestler2;
  private FeudScript script;

  @BeforeEach
  void setup() {
    wrestler1 = new Wrestler();
    wrestler1.setName("Adam Axe");
    wrestler2 = new Wrestler();
    wrestler2.setName("Bob Boulder");

    script = new FeudScript();
    script.setName("Title Arc");
    script.setStatus(FeudScriptStatus.ACTIVE);

    when(feudScriptService.addBeat(any(), any())).thenAnswer(inv -> inv.getArgument(1));
  }

  private AddBeatDialog openDialog() {
    AddBeatDialog dialog =
        new AddBeatDialog(
            script,
            List.of(wrestler1, wrestler2),
            List.of("Singles Match", "Tag Team Match"),
            List.of("No Disqualification"),
            feudScriptService,
            null);
    dialog.open();
    UI.getCurrent().add(dialog);
    return dialog;
  }

  @Test
  @DisplayName("Save without match type does not call the service")
  void save_withoutMatchType_doesNotCallService() {
    AddBeatDialog dialog = openDialog();

    Button saveBtn = _get(dialog, Button.class, spec -> spec.withText("Add Beat"));
    _click(saveBtn);

    verify(feudScriptService, never()).addBeat(any(), any());
  }

  @Test
  @DisplayName("Save with match type persists the beat via FeudScriptService")
  void save_withMatchType_callsService() {
    AddBeatDialog dialog = openDialog();

    @SuppressWarnings("unchecked")
    ComboBox<String> matchType =
        _get(dialog, ComboBox.class, spec -> spec.withCaption("Match Type"));
    HasValueUtilsKt._setValue(matchType, "Singles Match", true);

    Button saveBtn = _get(dialog, Button.class, spec -> spec.withText("Add Beat"));
    _click(saveBtn);

    ArgumentCaptor<FeudScriptBeat> captor = ArgumentCaptor.forClass(FeudScriptBeat.class);
    verify(feudScriptService).addBeat(same(script), captor.capture());
    assertEquals("Singles Match", captor.getValue().getSegmentType());
    assertFalse(captor.getValue().isCulmination());
  }

  @Test
  @DisplayName("participantsOf returns both rivalry wrestlers")
  void participantsOf_returnsBothWrestlers() {
    Rivalry rivalry = new Rivalry();
    rivalry.setWrestler1(wrestler1);
    rivalry.setWrestler2(wrestler2);

    List<Wrestler> participants = AddBeatDialog.participantsOf(rivalry);
    assertEquals(2, participants.size());
    assertTrue(participants.containsAll(List.of(wrestler1, wrestler2)));
  }
}
