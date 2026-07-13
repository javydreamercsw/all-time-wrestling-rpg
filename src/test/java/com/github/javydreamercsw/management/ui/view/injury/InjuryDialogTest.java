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

import static com.github.mvysny.kaributesting.v10.GridKt._getCellComponent;
import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.injury.Injury;
import com.github.javydreamercsw.management.domain.injury.InjurySeverity;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.injury.InjuryTypeService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class InjuryDialogTest extends AbstractViewTest {

  @Mock private InjuryService injuryService;
  @Mock private InjuryTypeService injuryTypeService;
  @Mock private SecurityUtils securityUtils;

  private InjuryDialog dialog;
  private Wrestler wrestler;

  @BeforeEach
  void setup() {
    when(securityUtils.canCreate()).thenReturn(true);
    when(securityUtils.canEdit(null)).thenReturn(true);
    when(securityUtils.isAdmin()).thenReturn(false);
    when(injuryService.getAllInjuriesForWrestler(anyLong(), anyLong()))
        .thenReturn(Collections.emptyList());

    wrestler = new Wrestler();
    wrestler.setId(1L);
    wrestler.setName("Test Wrestler");

    dialog =
        new InjuryDialog(wrestler, 1L, injuryService, injuryTypeService, () -> {}, securityUtils);
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

  @Test
  @DisplayName("Force Heal click should call InjuryService.forceHeal, not attemptHealing")
  void forceHealClickCallsForceHeal() {
    Injury injury = new Injury();
    injury.setId(42L);
    injury.setWrestler(wrestler);
    injury.setName("Torn ACL");
    injury.setSeverity(InjurySeverity.MODERATE);
    injury.setHealthPenalty(3);
    injury.setInjuryDate(Instant.now());

    when(securityUtils.isAdmin()).thenReturn(true);
    when(securityUtils.canEdit(injury)).thenReturn(true);
    when(injuryService.getAllInjuriesForWrestler(anyLong(), anyLong()))
        .thenReturn(Collections.singletonList(injury));
    when(injuryService.forceHeal(42L))
        .thenReturn(
            new InjuryService.HealingResult(
                true, "Injury force healed successfully", injury, 6, false));

    // Recreate the dialog so the grid picks up the admin-visible Force Heal button and the
    // injury fixture configured above.
    dialog =
        new InjuryDialog(wrestler, 1L, injuryService, injuryTypeService, () -> {}, securityUtils);

    Grid<Injury> grid = _get(dialog, Grid.class);
    Component actionsCell = _getCellComponent(grid, 0, "actions");
    Button forceHealButton =
        _get(actionsCell, Button.class, spec -> spec.withId("force-heal-injury-42"));
    _click(forceHealButton);

    verify(injuryService).forceHeal(42L);
    verify(injuryService, never()).attemptHealing(anyLong());
  }
}
