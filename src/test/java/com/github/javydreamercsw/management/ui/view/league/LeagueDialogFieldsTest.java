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
package com.github.javydreamercsw.management.ui.view.league;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.league.LeagueMembershipRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.league.LeagueService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class LeagueDialogFieldsTest extends AbstractViewTest {

  @Mock private LeagueService leagueService;
  @Mock private AccountService accountService;
  @Mock private SecurityUtils securityUtils;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private LeagueMembershipRepository leagueMembershipRepository;

  private LeagueDialog dialog;

  @BeforeEach
  void setup() {
    when(wrestlerRepository.findAll()).thenReturn(Collections.emptyList());
    when(accountService.findAll()).thenReturn(Collections.emptyList());
    when(securityUtils.canCreate()).thenReturn(true);
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.empty());

    dialog =
        new LeagueDialog(
            leagueService,
            accountService,
            securityUtils,
            wrestlerRepository,
            leagueMembershipRepository,
            null,
            null,
            () -> {});
    UI.getCurrent().add(dialog);
  }

  @Test
  @DisplayName("Dialog should contain IntegerField components for duration, morale, etc.")
  void dialogShouldContainIntegerFields() {
    List<IntegerField> intFields = _find(dialog, IntegerField.class);
    assertTrue(intFields.size() >= 2, "Expected at least 2 IntegerFields (duration, morale)");
  }

  @Test
  @DisplayName("Dialog should contain exactly one BigDecimalField for budget")
  void dialogShouldContainBudgetField() {
    List<BigDecimalField> bigDecimalFields = _find(dialog, BigDecimalField.class);
    assertEquals(1, bigDecimalFields.size(), "Expected exactly 1 BigDecimalField (budget)");
  }

  @Test
  @DisplayName("Dialog should contain ComboBox components for status and universe")
  void dialogShouldContainComboBoxes() {
    List<ComboBox> combos = _find(dialog, ComboBox.class);
    assertTrue(combos.size() >= 2, "Expected at least 2 ComboBoxes (status, universe)");
  }
}
