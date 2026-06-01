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

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.service.account.AccountService;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.campaign.AlignmentService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.textfield.IntegerField;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class WrestlerDialogFieldsTest extends AbstractViewTest {

  @Mock private WrestlerService wrestlerService;
  @Mock private AccountService accountService;
  @Mock private NpcService npcService;
  @Mock private ImageStorageService imageStorageService;
  @Mock private WrestlerStateRepository wrestlerStateRepository;
  @Mock private SecurityUtils securityUtils;
  @Mock private UniverseContextService universeContextService;
  @Mock private AlignmentService alignmentService;

  private static final Set<String> CAMPAIGN_FIELD_LABELS =
      Set.of("Drive (1-6)", "Resilience (1-6)", "Charisma (1-6)", "Brawl (1-6)");

  private WrestlerDialog buildDialog() {
    when(npcService.findAllByType("Manager")).thenReturn(Collections.emptyList());
    when(accountService.findAll()).thenReturn(Collections.emptyList());

    Wrestler wrestler = new Wrestler();
    wrestler.setId(1L);
    wrestler.setName("Test");

    WrestlerDialog d =
        new WrestlerDialog(
            wrestlerService,
            accountService,
            npcService,
            imageStorageService,
            wrestlerStateRepository,
            wrestler,
            () -> {},
            securityUtils,
            universeContextService,
            alignmentService);
    UI.getCurrent().add(d);
    return d;
  }

  @BeforeEach
  void setup() {
    when(securityUtils.canEdit(any())).thenReturn(true);
    when(securityUtils.isAdmin()).thenReturn(true);
    when(securityUtils.isBooker()).thenReturn(true);
    when(universeContextService.getCurrentUniverse()).thenReturn(Optional.empty());
  }

  @Test
  @DisplayName("Campaign attribute fields are visible for admin/booker")
  void campaignAttributeFieldsVisibleForAdmin() {
    WrestlerDialog dialog = buildDialog();
    List<IntegerField> intFields = _find(dialog, IntegerField.class);

    for (String label : CAMPAIGN_FIELD_LABELS) {
      boolean found = intFields.stream().anyMatch(f -> label.equals(f.getLabel()) && f.isVisible());
      assertTrue(found, "Expected visible IntegerField with label: " + label);
    }
  }

  @Test
  @DisplayName("Campaign attribute fields are hidden for non-admin/non-booker")
  void campaignAttributeFieldsHiddenForViewer() {
    when(securityUtils.isAdmin()).thenReturn(false);
    when(securityUtils.isBooker()).thenReturn(false);
    when(securityUtils.canEdit(any())).thenReturn(false);

    WrestlerDialog dialog = buildDialog();
    List<IntegerField> intFields = _find(dialog, IntegerField.class);

    for (String label : CAMPAIGN_FIELD_LABELS) {
      boolean anyVisible =
          intFields.stream().anyMatch(f -> label.equals(f.getLabel()) && f.isVisible());
      assertFalse(anyVisible, "Campaign field should NOT be visible for viewer: " + label);
    }
  }
}
