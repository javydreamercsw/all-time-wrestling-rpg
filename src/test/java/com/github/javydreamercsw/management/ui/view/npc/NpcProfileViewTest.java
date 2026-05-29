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
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class NpcProfileViewTest extends AbstractViewTest {

  @Mock private NpcService npcService;
  @Mock private NpcRepository npcRepository;
  @Mock private ImageGenerationServiceFactory imageGenerationServiceFactory;
  @Mock private ImageStorageService imageStorageService;
  @Mock private AiSettingsService aiSettingsService;
  @Mock private SecurityUtils securityUtils;

  private NpcProfileView view;

  @BeforeEach
  void setup() {
    view =
        new NpcProfileView(
            npcService,
            npcRepository,
            imageGenerationServiceFactory,
            imageStorageService,
            aiSettingsService,
            securityUtils);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the NPC name heading")
  void shouldRenderNpcNameHeading() {
    H2 heading = _get(view, H2.class, spec -> spec.withId("npc-name"));
    assertTrue(heading.isVisible());
  }
}
