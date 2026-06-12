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
import com.github.javydreamercsw.management.service.sync.base.BaseSyncService;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

/**
 * Registry for entity-specific Notion sync services. Uses @PostConstruct to avoid the circular
 * dependency that arises when constructor-injecting List<BaseSyncService> (NotionSyncService also
 * extends BaseSyncService and depends on this manager).
 */
@Component
public class NotionSyncServicesManager {

  private static final String ENTITY_SYNC_PACKAGE = ".sync.entity.notion";

  private final ApplicationContext applicationContext;
  private final FactionRivalryNotionSyncService factionRivalryNotionSyncService;
  private final ShowTemplateNotionSyncService showTemplateNotionSyncService;
  private final WrestlerRepository wrestlerRepository;

  private Map<Class<?>, BaseSyncService> registry;

  public NotionSyncServicesManager(
      final ApplicationContext applicationContext,
      final FactionRivalryNotionSyncService factionRivalryNotionSyncService,
      final ShowTemplateNotionSyncService showTemplateNotionSyncService,
      final WrestlerRepository wrestlerRepository) {
    this.applicationContext = applicationContext;
    this.factionRivalryNotionSyncService = factionRivalryNotionSyncService;
    this.showTemplateNotionSyncService = showTemplateNotionSyncService;
    this.wrestlerRepository = wrestlerRepository;
  }

  @PostConstruct
  void buildRegistry() {
    registry =
        applicationContext.getBeansOfType(BaseSyncService.class).values().stream()
            .filter(
                s ->
                    ClassUtils.getUserClass(s.getClass())
                        .getPackageName()
                        .contains(ENTITY_SYNC_PACKAGE))
            .collect(
                Collectors.toUnmodifiableMap(s -> ClassUtils.getUserClass(s.getClass()), s -> s));
  }

  @SuppressWarnings("unchecked")
  private <T extends BaseSyncService> T get(final Class<T> type) {
    T service = (T) registry.get(type);
    if (service == null) {
      throw new IllegalStateException("No sync service registered for " + type.getSimpleName());
    }
    return service;
  }

  public ShowSyncService getShowSyncService() {
    return get(ShowSyncService.class);
  }

  public WrestlerSyncService getWrestlerSyncService() {
    return get(WrestlerSyncService.class);
  }

  public FactionSyncService getFactionSyncService() {
    return get(FactionSyncService.class);
  }

  public TeamSyncService getTeamSyncService() {
    return get(TeamSyncService.class);
  }

  public SegmentSyncService getSegmentSyncService() {
    return get(SegmentSyncService.class);
  }

  public SeasonSyncService getSeasonSyncService() {
    return get(SeasonSyncService.class);
  }

  public ShowTypeSyncService getShowTypeSyncService() {
    return get(ShowTypeSyncService.class);
  }

  public ShowTemplateSyncService getShowTemplateSyncService() {
    return get(ShowTemplateSyncService.class);
  }

  public InjuryTypeSyncService getInjuryTypeSyncService() {
    return get(InjuryTypeSyncService.class);
  }

  public InjurySyncService getInjurySyncService() {
    return get(InjurySyncService.class);
  }

  public NpcSyncService getNpcSyncService() {
    return get(NpcSyncService.class);
  }

  public TitleSyncService getTitleSyncService() {
    return get(TitleSyncService.class);
  }

  public TitleReignSyncService getTitleReignSyncService() {
    return get(TitleReignSyncService.class);
  }

  public RivalrySyncService getRivalrySyncService() {
    return get(RivalrySyncService.class);
  }

  public FactionRivalrySyncService getFactionRivalrySyncService() {
    return get(FactionRivalrySyncService.class);
  }

  public FactionRivalryNotionSyncService getFactionRivalryNotionSyncService() {
    return factionRivalryNotionSyncService;
  }

  public ShowTemplateNotionSyncService getShowTemplateNotionSyncService() {
    return showTemplateNotionSyncService;
  }

  public WrestlerRepository getWrestlerRepository() {
    return wrestlerRepository;
  }
}
