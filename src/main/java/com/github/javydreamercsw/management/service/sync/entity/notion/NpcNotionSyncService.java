/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.service.sync.entity.notion;

import com.github.javydreamercsw.base.ai.notion.NotionApiExecutor;
import com.github.javydreamercsw.base.ai.notion.NotionPropertyBuilder;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import notion.api.v1.model.pages.PageProperty;
import org.springframework.stereotype.Service;

@Service
public class NpcNotionSyncService extends BaseNotionSyncService<Npc> {

  public NpcNotionSyncService(
      NpcRepository repository,
      SyncServiceDependencies syncServiceDependencies,
      NotionApiExecutor notionApiExecutor) {
    super(repository, syncServiceDependencies, notionApiExecutor);
  }

  @Override
  protected Map<String, PageProperty> getProperties(@NonNull Npc entity) {
    Map<String, PageProperty> properties = new HashMap<>();
    properties.put("Name", NotionPropertyBuilder.createTitleProperty(entity.getName()));

    if (entity.getDescription() != null && !entity.getDescription().isBlank()) {
      properties.put(
          "Description", NotionPropertyBuilder.createRichTextProperty(entity.getDescription()));
    }

    if (entity.getNpcType() != null) {
      properties.put("Role", NotionPropertyBuilder.createSelectProperty(entity.getNpcType()));
    }

    if (entity.getAlignment() != null) {
      properties.put(
          "Alignment", NotionPropertyBuilder.createSelectProperty(entity.getAlignment().name()));
    }

    if (entity.getGender() != null) {
      properties.put("Sex", NotionPropertyBuilder.createSelectProperty(entity.getGender().name()));
    }

    Map<String, Object> attrs = entity.getAttributes();
    if (attrs.containsKey("likeness")) {
      properties.put(
          "Likeness", NotionPropertyBuilder.createRichTextProperty((String) attrs.get("likeness")));
    }
    if (attrs.containsKey("origin")) {
      properties.put(
          "Origin", NotionPropertyBuilder.createRichTextProperty((String) attrs.get("origin")));
    }
    if (attrs.containsKey("catchphrase")) {
      properties.put(
          "Catchphrase",
          NotionPropertyBuilder.createRichTextProperty((String) attrs.get("catchphrase")));
    }
    if (attrs.containsKey("signatureStyle")) {
      properties.put(
          "Signature Style",
          NotionPropertyBuilder.createRichTextProperty((String) attrs.get("signatureStyle")));
    }
    if (attrs.containsKey("status")) {
      properties.put(
          "Status", NotionPropertyBuilder.createSelectProperty((String) attrs.get("status")));
    }

    return properties;
  }

  @Override
  protected String getDatabaseName() {
    return "NPCs";
  }

  @Override
  protected String getEntityName() {
    return "NPC";
  }
}
