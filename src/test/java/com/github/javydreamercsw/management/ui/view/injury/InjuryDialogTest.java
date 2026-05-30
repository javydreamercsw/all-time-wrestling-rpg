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
package com.github.javydreamercsw.management.ui.view.injury;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.grid.Grid;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class InjuryDialogTest extends AbstractViewTest {

  @Mock private InjuryService injuryService;
  @Mock private SecurityUtils securityUtils;

  private InjuryDialog dialog;

  @BeforeEach
  void setup() {
    when(securityUtils.canCreate()).thenReturn(true);
    when(securityUtils.canEdit(null)).thenReturn(true);
    when(securityUtils.isAdmin()).thenReturn(false);
    when(injuryService.getAllInjuriesForWrestler(anyLong(), anyLong()))
        .thenReturn(Collections.emptyList());

    Wrestler wrestler = new Wrestler();
    wrestler.setName("Test Wrestler");

    dialog = new InjuryDialog(wrestler, 1L, injuryService, () -> {}, securityUtils);
  }

  @Test
  @DisplayName("InjuryDialog should construct without throwing")
  void dialogConstructs() {
    assertNotNull(dialog, "InjuryDialog should not be null");
  }

  @Test
  @DisplayName("Dialog should contain an injury Grid")
  void gridExists() {
    List<Grid> grids = _find(dialog, Grid.class);
    assertFalse(grids.isEmpty(), "Expected at least one Grid for injuries");
  }
}
