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
package com.github.javydreamercsw.management.service.sync.entity.notion;

import com.github.javydreamercsw.base.ai.notion.NotionApiExecutor;
import com.github.javydreamercsw.base.ai.notion.NotionPropertyBuilder;
import com.github.javydreamercsw.management.domain.injury.InjuryType;
import com.github.javydreamercsw.management.domain.injury.InjuryTypeRepository;
import com.github.javydreamercsw.management.service.sync.SyncServiceDependencies;
import java.util.HashMap;
import java.util.Map;
import lombok.NonNull;
import notion.api.v1.model.pages.PageProperty;
import org.springframework.stereotype.Service;

@Service
public class InjuryTypeNotionSyncService extends BaseNotionSyncService<InjuryType> {

  public InjuryTypeNotionSyncService(
      InjuryTypeRepository repository,
      SyncServiceDependencies syncServiceDependencies,
      NotionApiExecutor notionApiExecutor) {
    super(repository, syncServiceDependencies, notionApiExecutor);
  }

  @Override
  protected Map<String, PageProperty> getProperties(@NonNull InjuryType entity) {
    Map<String, PageProperty> properties = new HashMap<>();
    properties.put("Name", NotionPropertyBuilder.createTitleProperty(entity.getInjuryName()));
    properties.put(
        "Health Effect",
        NotionPropertyBuilder.createNumberProperty((double) entity.getEffectiveHealthEffect()));
    properties.put(
        "Stamina Effect",
        NotionPropertyBuilder.createNumberProperty((double) entity.getEffectiveStaminaEffect()));
    properties.put(
        "Card Effect",
        NotionPropertyBuilder.createNumberProperty((double) entity.getEffectiveCardEffect()));
    return properties;
  }

  @Override
  protected String getDatabaseName() {
    return "Injury Types";
  }

  @Override
  protected String getEntityName() {
    return "Injury Type";
  }
}
