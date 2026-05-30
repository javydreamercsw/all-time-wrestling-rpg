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
package com.github.javydreamercsw.management.ui.view;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.faction.FactionService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

class TeamFormDialogFieldsTest extends AbstractViewTest {

  @Mock private TeamService teamService;
  @Mock private WrestlerService wrestlerService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private FactionService factionService;
  @Mock private NpcService npcService;
  @Mock private SecurityUtils securityUtils;

  private TeamFormDialog dialog;

  @BeforeEach
  void setup() {
    when(wrestlerRepository.findAll()).thenReturn(Collections.emptyList());
    when(factionService.findAll()).thenReturn(Collections.emptyList());
    when(npcService.findAllByType("Manager")).thenReturn(Collections.emptyList());

    dialog =
        new TeamFormDialog(
            teamService,
            wrestlerService,
            wrestlerRepository,
            factionService,
            npcService,
            securityUtils);
  }

  @Test
  @DisplayName("nameField should exist in TeamFormDialog")
  void nameFieldExists() {
    TextField nameField = (TextField) ReflectionTestUtils.getField(dialog, "nameField");
    assertNotNull(nameField, "nameField should not be null");
  }

  @Test
  @DisplayName("descriptionField should exist in TeamFormDialog")
  void descriptionFieldExists() {
    TextArea descriptionField = (TextArea) ReflectionTestUtils.getField(dialog, "descriptionField");
    assertNotNull(descriptionField, "descriptionField should not be null");
  }

  @Test
  @DisplayName("wrestler1Field and wrestler2Field should exist in TeamFormDialog")
  void wrestlerFieldsExist() {
    ComboBox<?> wrestler1Field =
        (ComboBox<?>) ReflectionTestUtils.getField(dialog, "wrestler1Field");
    assertNotNull(wrestler1Field, "wrestler1Field should not be null");

    ComboBox<?> wrestler2Field =
        (ComboBox<?>) ReflectionTestUtils.getField(dialog, "wrestler2Field");
    assertNotNull(wrestler2Field, "wrestler2Field should not be null");
  }

  @Test
  @DisplayName("statusField should exist in TeamFormDialog")
  void statusFieldExists() {
    ComboBox<?> statusField = (ComboBox<?>) ReflectionTestUtils.getField(dialog, "statusField");
    assertNotNull(statusField, "statusField should not be null");
  }
}
