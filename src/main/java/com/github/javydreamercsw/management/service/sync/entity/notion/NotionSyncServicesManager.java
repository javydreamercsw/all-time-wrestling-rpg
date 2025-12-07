package com.github.javydreamercsw.management.service.sync.entity.notion;

import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Getter
public class NotionSyncServicesManager {
  // Entity-specific sync services
  private final ShowSyncService showSyncService;
  private final WrestlerSyncService wrestlerSyncService;
  private final FactionSyncService factionSyncService;
  private final TeamSyncService teamSyncService;
  private final SegmentSyncService segmentSyncService;
  private final SeasonSyncService seasonSyncService;
  private final ShowTypeSyncService showTypeSyncService;
  private final ShowTemplateSyncService showTemplateSyncService;
  private final InjurySyncService injurySyncService;
  private final NpcSyncService npcSyncService;
  private final TitleSyncService titleSyncService;
  private final TitleReignSyncService titleReignSyncService;
  private final RivalrySyncService rivalrySyncService;
  private final FactionRivalrySyncService factionRivalrySyncService;

  // Outbound to Notion sync services
  private final WrestlerNotionSyncService wrestlerNotionSyncService;
  private final TitleNotionSyncService titleNotionSyncService;
  private final NpcNotionSyncService npcNotionSyncService;
  private final RivalryNotionSyncService rivalryNotionSyncService;
  private final SeasonNotionSyncService seasonNotionSyncService;
  private final ShowNotionSyncService showNotionSyncService;
  private final FactionNotionSyncService factionNotionSyncService;
  private final TeamNotionSyncService teamNotionSyncService;
  private final FactionRivalryNotionSyncService factionRivalryNotionSyncService;
  private final SegmentNotionSyncService segmentNotionSyncService;
  private final ShowTemplateNotionSyncService showTemplateNotionSyncService;
  private final ShowTypeNotionSyncService showTypeNotionSyncService;
  private final InjuryNotionSyncService injuryNotionSyncService;
  private final WrestlerRepository wrestlerRepository;

  @Autowired
  public NotionSyncServicesManager(
      ShowSyncService showSyncService,
      WrestlerSyncService wrestlerSyncService,
      FactionSyncService factionSyncService,
      TeamSyncService teamSyncService,
      SegmentSyncService segmentSyncService,
      SeasonSyncService seasonSyncService,
      ShowTypeSyncService showTypeSyncService,
      ShowTemplateSyncService showTemplateSyncService,
      InjurySyncService injurySyncService,
      NpcSyncService npcSyncService,
      TitleSyncService titleSyncService,
      TitleReignSyncService titleReignSyncService,
      RivalrySyncService rivalrySyncService,
      FactionRivalrySyncService factionRivalrySyncService,
      WrestlerNotionSyncService wrestlerNotionSyncService,
      TitleNotionSyncService titleNotionSyncService,
      NpcNotionSyncService npcNotionSyncService,
      RivalryNotionSyncService rivalryNotionSyncService,
      SeasonNotionSyncService seasonNotionSyncService,
      ShowNotionSyncService showNotionSyncService,
      FactionNotionSyncService factionNotionSyncService,
      TeamNotionSyncService teamNotionSyncService,
      FactionRivalryNotionSyncService factionRivalryNotionSyncService,
      SegmentNotionSyncService segmentNotionSyncService,
      ShowTemplateNotionSyncService showTemplateNotionSyncService,
      ShowTypeNotionSyncService showTypeNotionSyncService,
      WrestlerRepository wrestlerRepository,
      InjuryNotionSyncService injuryNotionSyncService) {
    this.showSyncService = showSyncService;
    this.wrestlerSyncService = wrestlerSyncService;
    this.factionSyncService = factionSyncService;
    this.teamSyncService = teamSyncService;
    this.segmentSyncService = segmentSyncService;
    this.seasonSyncService = seasonSyncService;
    this.showTypeSyncService = showTypeSyncService;
    this.showTemplateSyncService = showTemplateSyncService;
    this.injurySyncService = injurySyncService;
    this.npcSyncService = npcSyncService;
    this.titleSyncService = titleSyncService;
    this.titleReignSyncService = titleReignSyncService;
    this.rivalrySyncService = rivalrySyncService;
    this.factionRivalrySyncService = factionRivalrySyncService;
    this.wrestlerNotionSyncService = wrestlerNotionSyncService;
    this.titleNotionSyncService = titleNotionSyncService;
    this.npcNotionSyncService = npcNotionSyncService;
    this.rivalryNotionSyncService = rivalryNotionSyncService;
    this.seasonNotionSyncService = seasonNotionSyncService;
    this.showNotionSyncService = showNotionSyncService;
    this.factionNotionSyncService = factionNotionSyncService;
    this.teamNotionSyncService = teamNotionSyncService;
    this.factionRivalryNotionSyncService = factionRivalryNotionSyncService;
    this.segmentNotionSyncService = segmentNotionSyncService;
    this.showTemplateNotionSyncService = showTemplateNotionSyncService;
    this.showTypeNotionSyncService = showTypeNotionSyncService;
    this.wrestlerRepository = wrestlerRepository;
    this.injuryNotionSyncService = injuryNotionSyncService;
  }
}
