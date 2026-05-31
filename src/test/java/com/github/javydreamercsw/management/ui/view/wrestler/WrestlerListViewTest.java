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
package com.github.javydreamercsw.management.ui.view.wrestler;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.service.account.AccountService;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.campaign.AlignmentService;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.injury.InjuryTypeService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.universe.UniverseSettingsService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class WrestlerListViewTest extends AbstractViewTest {

  @Mock private WrestlerService wrestlerService;
  @Mock private InjuryService injuryService;
  @Mock private InjuryTypeService injuryTypeService;
  @Mock private NpcService npcService;
  @Mock private ExpansionService expansionService;
  @Mock private UniverseSettingsService universeSettingsService;
  @Mock private AccountService accountService;
  @Mock private SecurityUtils securityUtils;
  @Mock private CampaignService campaignService;
  @Mock private ImageStorageService imageStorageService;
  @Mock private UniverseContextService universeContextService;
  @Mock private WrestlerStateRepository wrestlerStateRepository;
  @Mock private AlignmentService alignmentService;

  private WrestlerListView view;

  @BeforeEach
  void setup() {
    when(universeContextService.getCurrentUniverseId()).thenReturn(1L);
    when(universeContextService.getCurrentUniverse()).thenReturn(Optional.empty());
    when(injuryService.getWrestlersWithActiveInjuries(any())).thenReturn(Collections.emptyList());
    when(expansionService.getEnabledExpansionCodes()).thenReturn(Collections.emptyList());
    when(securityUtils.isAdmin()).thenReturn(true);
    when(securityUtils.isBooker()).thenReturn(false);
    when(securityUtils.canCreate()).thenReturn(true);
    when(wrestlerService.findAllIncludingInactive()).thenReturn(Collections.emptyList());

    view =
        new WrestlerListView(
            wrestlerService,
            injuryService,
            injuryTypeService,
            npcService,
            expansionService,
            universeSettingsService,
            accountService,
            securityUtils,
            campaignService,
            imageStorageService,
            universeContextService,
            wrestlerStateRepository,
            alignmentService);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the wrestler list grid")
  void shouldRenderGrid() {
    Grid<?> grid = _get(view, Grid.class, spec -> spec.withId("wrestler-list-grid"));
    assertTrue(grid.isVisible());
  }
}
