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

import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Getter
public class NotionSyncServicesManager {
  // Entity-specific sync services
  @Autowired private ShowSyncService showSyncService;
  @Autowired private WrestlerSyncService wrestlerSyncService;
  @Autowired private FactionSyncService factionSyncService;
  @Autowired private TeamSyncService teamSyncService;
  @Autowired private SegmentSyncService segmentSyncService;
  @Autowired private SeasonSyncService seasonSyncService;
  @Autowired private ShowTypeSyncService showTypeSyncService;
  @Autowired private ShowTemplateSyncService showTemplateSyncService;
  @Autowired private InjuryTypeSyncService injuryTypeSyncService;
  @Autowired private NpcSyncService npcSyncService;
  @Autowired private TitleSyncService titleSyncService;
  @Autowired private TitleReignSyncService titleReignSyncService;
  @Autowired private RivalrySyncService rivalrySyncService;
  @Autowired private FactionRivalrySyncService factionRivalrySyncService;

  // Outbound to Notion sync services
  @Autowired @Lazy private WrestlerNotionSyncService wrestlerNotionSyncService;
  @Autowired private TitleNotionSyncService titleNotionSyncService;
  @Autowired private TitleReignNotionSyncService titleReignNotionSyncService;
  @Autowired private NpcNotionSyncService npcNotionSyncService;
  @Autowired private RivalryNotionSyncService rivalryNotionSyncService;
  @Autowired private SeasonNotionSyncService seasonNotionSyncService;
  @Autowired private ShowNotionSyncService showNotionSyncService;
  @Autowired private FactionNotionSyncService factionNotionSyncService;
  @Autowired private TeamNotionSyncService teamNotionSyncService;
  @Autowired private FactionRivalryNotionSyncService factionRivalryNotionSyncService;
  @Autowired private SegmentNotionSyncService segmentNotionSyncService;
  @Autowired private ShowTemplateNotionSyncService showTemplateNotionSyncService;
  @Autowired private ShowTypeNotionSyncService showTypeNotionSyncService;
  @Autowired private InjuryNotionSyncService injuryNotionSyncService;
  @Autowired private InjuryTypeNotionSyncService injuryTypeNotionSyncService;
  @Autowired private WrestlerRepository wrestlerRepository;
}
