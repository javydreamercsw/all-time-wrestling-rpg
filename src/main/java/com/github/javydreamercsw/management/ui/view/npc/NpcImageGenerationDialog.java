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

import com.github.javydreamercsw.base.ai.image.ImageGenerationServiceFactory;
import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.ai.image.ui.GenericImageGenerationDialog;
import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.service.npc.NpcService;

public class NpcImageGenerationDialog extends GenericImageGenerationDialog {

  public NpcImageGenerationDialog(
      Npc npc,
      NpcService npcService,
      ImageGenerationServiceFactory imageFactory,
      ImageStorageService storageService,
      AiSettingsService aiSettingsService,
      Runnable onSave) {
    super(
        () -> {
          StringBuilder sb = new StringBuilder();
          sb.append("A portrait of an NPC named ").append(npc.getName());
          sb.append(". ");
          if (npc.getDescription() != null) {
            sb.append(npc.getDescription());
          }
          sb.append(" High quality, photorealistic, 8k resolution, dramatic lighting.");
          return sb.toString();
        },
        (imageUrl) -> {
          npc.setImageUrl(imageUrl);
          npcService.save(npc);
        },
        imageFactory,
        storageService,
        aiSettingsService,
        onSave);
  }
}
