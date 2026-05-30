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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.image.ImageGenerationServiceFactory;
import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class NpcImageGenerationDialogTest extends AbstractViewTest {

  @Mock private NpcService npcService;
  @Mock private ImageGenerationServiceFactory imageFactory;
  @Mock private ImageStorageService storageService;
  @Mock private AiSettingsService aiSettingsService;

  private NpcImageGenerationDialog dialog;

  @BeforeEach
  void setup() {
    // getBestAvailableService() is called in GenericImageGenerationDialog constructor
    when(imageFactory.getBestAvailableService()).thenReturn(null);

    Npc npc = new Npc();
    npc.setName("Test NPC");
    npc.setDescription("A tough referee with years of experience.");

    dialog =
        new NpcImageGenerationDialog(
            npc, npcService, imageFactory, storageService, aiSettingsService, () -> {});
  }

  @Test
  @DisplayName("NpcImageGenerationDialog should construct without throwing")
  void dialogConstructs() {
    assertNotNull(dialog, "NpcImageGenerationDialog should not be null");
  }
}
