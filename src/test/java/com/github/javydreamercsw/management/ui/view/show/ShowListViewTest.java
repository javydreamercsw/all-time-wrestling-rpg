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
package com.github.javydreamercsw.management.ui.view.show;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.image.ImageGenerationServiceFactory;
import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.base.ui.service.NotificationService;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import com.github.javydreamercsw.management.domain.show.export.ShowExportService;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.world.ArenaService;
import com.vaadin.flow.component.UI;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.domain.Page;

class ShowListViewTest extends com.github.javydreamercsw.management.ui.view.AbstractViewTest {

  @Mock private ShowService showService;
  @Mock private ShowTypeService showTypeService;
  @Mock private SeasonService seasonService;
  @Mock private ShowTemplateService showTemplateService;
  @Mock private UniverseRepository universeRepository;
  @Mock private UniverseContextService universeContextService;
  @Mock private SecurityUtils securityUtils;
  @Mock private ImageGenerationServiceFactory imageGenerationServiceFactory;
  @Mock private ImageStorageService imageStorageService;
  @Mock private AiSettingsService aiSettingsService;
  @Mock private ArenaService arenaService;
  @Mock private ShowExportService exportService;
  @Mock private NotificationService notificationService;
  @Mock private LeagueRepository leagueRepository;

  private ShowListView view;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setup() {
    when(showTypeService.findAll()).thenReturn(Collections.emptyList());
    when(seasonService.getAllSeasons(org.mockito.ArgumentMatchers.any())).thenReturn(Page.empty());
    when(leagueRepository.findAll()).thenReturn(Collections.emptyList());
    when(universeRepository.findAll()).thenReturn(Collections.emptyList());
    when(arenaService.findAll()).thenReturn(Collections.emptyList());
    when(universeContextService.getCurrentUniverseId()).thenReturn(null);

    view =
        new ShowListView(
            showService,
            showTypeService,
            seasonService,
            showTemplateService,
            universeRepository,
            universeContextService,
            securityUtils,
            imageGenerationServiceFactory,
            imageStorageService,
            aiSettingsService,
            arenaService,
            exportService,
            notificationService,
            leagueRepository,
            null);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the Show List toolbar")
  void shouldRenderToolbar() {
    ViewToolbar toolbar = _get(view, ViewToolbar.class);
    assertTrue(toolbar.isVisible());
  }
}
