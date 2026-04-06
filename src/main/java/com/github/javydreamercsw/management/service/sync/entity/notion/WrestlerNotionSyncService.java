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
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.NonNull;
import notion.api.v1.model.pages.PageProperty;
import org.springframework.stereotype.Service;

@Service
public class WrestlerNotionSyncService extends BaseNotionSyncService<Wrestler> {

  public WrestlerNotionSyncService(
      WrestlerRepository repository,
      SyncServiceDependencies syncServiceDependencies,
      NotionApiExecutor notionApiExecutor) {
    super(repository, syncServiceDependencies, notionApiExecutor);
  }

  @Override
  protected Map<String, PageProperty> getProperties(@NonNull Wrestler entity) {
    Map<String, PageProperty> properties = new HashMap<>();
    properties.put("Name", NotionPropertyBuilder.createTitleProperty(entity.getName()));
    if (entity.getStartingStamina() != null) {
      properties.put(
          "Starting Stamina",
          NotionPropertyBuilder.createNumberProperty(entity.getStartingStamina().doubleValue()));
    }
    if (entity.getStartingHealth() != null) {
      properties.put(
          "Starting Health",
          NotionPropertyBuilder.createNumberProperty(entity.getStartingHealth().doubleValue()));
    }
    if (entity.getFans() != null) {
      properties.put(
          "Fans", NotionPropertyBuilder.createNumberProperty(entity.getFans().doubleValue()));
    }
    if (entity.getTier() != null) {
      properties.put(
          "Tier", NotionPropertyBuilder.createSelectProperty(entity.getTier().getDisplayName()));
    }
    if (entity.getGender() != null) {
      properties.put(
          "Gender", NotionPropertyBuilder.createSelectProperty(entity.getGender().name()));
    }
    if (entity.getBumps() != null) {
      properties.put(
          "Bumps", NotionPropertyBuilder.createNumberProperty(entity.getBumps().doubleValue()));
    }
    if (entity.getLowHealth() != null) {
      properties.put(
          "Low Health",
          NotionPropertyBuilder.createNumberProperty(entity.getLowHealth().doubleValue()));
    }
    if (entity.getLowStamina() != null) {
      properties.put(
          "Low Stamina",
          NotionPropertyBuilder.createNumberProperty(entity.getLowStamina().doubleValue()));
    }
    if (entity.getDeckSize() != null) {
      properties.put(
          "Deck Size",
          NotionPropertyBuilder.createNumberProperty(entity.getDeckSize().doubleValue()));
    }
    if (entity.getAlignment() != null && entity.getAlignment().getAlignmentType() != null) {
      properties.put(
          "Alignment",
          NotionPropertyBuilder.createSelectProperty(
              entity.getAlignment().getAlignmentType().name()));
    }
    if (entity.getDrive() != null) {
      properties.put(
          "Drive", NotionPropertyBuilder.createNumberProperty(entity.getDrive().doubleValue()));
    }
    if (entity.getResilience() != null) {
      properties.put(
          "Resilience",
          NotionPropertyBuilder.createNumberProperty(entity.getResilience().doubleValue()));
    }
    if (entity.getCharisma() != null) {
      properties.put(
          "Charisma",
          NotionPropertyBuilder.createNumberProperty(entity.getCharisma().doubleValue()));
    }
    if (entity.getBrawl() != null) {
      properties.put(
          "Brawl", NotionPropertyBuilder.createNumberProperty(entity.getBrawl().doubleValue()));
    }
    if (entity.getHeritageTag() != null) {
      properties.put(
          "Heritage Tag", NotionPropertyBuilder.createRichTextProperty(entity.getHeritageTag()));
    }
    if (entity.getFaction() != null && entity.getFaction().getExternalId() != null) {
      properties.put(
          "Faction",
          NotionPropertyBuilder.createRelationProperty(entity.getFaction().getExternalId()));
    }
    if (entity.getManager() != null && entity.getManager().getExternalId() != null) {
      properties.put(
          "Manager",
          NotionPropertyBuilder.createRelationProperty(entity.getManager().getExternalId()));
    }
    properties.put(
        "Injuries",
        NotionPropertyBuilder.createRelationProperty(
            entity.getInjuries() == null
                ? java.util.Collections.emptyList()
                : entity.getInjuries().stream()
                    .map(com.github.javydreamercsw.management.domain.injury.Injury::getExternalId)
                    .filter(Objects::nonNull)
                    .toList()));

    properties.put(
        "Titles",
        NotionPropertyBuilder.createRelationProperty(
            entity.getReigns() == null
                ? java.util.Collections.emptyList()
                : entity.getReigns().stream()
                    .map(
                        com.github.javydreamercsw.management.domain.title.TitleReign::getExternalId)
                    .filter(Objects::nonNull)
                    .toList()));
    return properties;
  }

  @Override
  protected String getDatabaseName() {
    return "Wrestlers";
  }

  @Override
  protected String getEntityName() {
    return "Wrestler";
  }
}
