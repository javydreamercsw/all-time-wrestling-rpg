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

import com.github.javydreamercsw.base.ai.notion.NotionPropertyBuilder;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import java.util.HashMap;
import java.util.Map;
import notion.api.v1.model.pages.PageProperty;
import org.springframework.stereotype.Service;

@Service
public class WrestlerNotionSyncService extends BaseNotionSyncService<Wrestler> {

  public WrestlerNotionSyncService(
      WrestlerRepository repository,
      SyncServiceDependencies syncServiceDependencies,
      NotionApiExecutor notionApiExecutor,
      NotionSyncServicesManager notionSyncServicesManager) {
    super(repository, syncServiceDependencies, notionApiExecutor, notionSyncServicesManager);
  }

  @Override
  protected Map<String, PageProperty> getProperties(Wrestler entity) {
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
    return properties;
  }

  @Override
  protected String getDatabaseId() {
    return syncServiceDependencies.getNotionHandler().getDatabaseId("Wrestlers");
  }

  @Override
  protected String getEntityName() {
    return "Wrestler";
  }
}
