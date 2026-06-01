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
package com.github.javydreamercsw.management.ui.view.npc;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.base.ai.image.ImageGenerationServiceFactory;
import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;

class NpcListViewTest extends AbstractViewTest {

  @Mock private NpcService npcService;
  @Mock private SecurityUtils securityUtils;
  @Mock private ImageGenerationServiceFactory imageFactory;
  @Mock private ImageStorageService storageService;
  @Mock private AiSettingsService aiSettingsService;

  private NpcListView view;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setup() {
    Mockito.when(npcService.findAll(ArgumentMatchers.any())).thenReturn(Page.empty());
    view =
        new NpcListView(npcService, securityUtils, imageFactory, storageService, aiSettingsService);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the NPC grid")
  void shouldRenderGrid() {
    Grid<?> grid = _get(view, Grid.class, spec -> spec.withId("npc-grid"));
    assertTrue(grid.isVisible());
  }
}
