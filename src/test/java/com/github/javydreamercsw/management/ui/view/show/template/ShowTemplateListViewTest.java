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
package com.github.javydreamercsw.management.ui.view.show.template;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.image.ImageGenerationServiceFactory;
import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.ai.service.AiSettingsService;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeamRepository;
import com.github.javydreamercsw.management.service.show.template.ShowTemplateService;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class ShowTemplateListViewTest extends AbstractViewTest {

  @Mock private ShowTemplateService showTemplateService;
  @Mock private ShowTypeService showTypeService;
  @Mock private CommentaryTeamRepository commentaryTeamRepository;
  @Mock private SecurityUtils securityUtils;
  @Mock private ImageGenerationServiceFactory imageGenerationServiceFactory;
  @Mock private ImageStorageService imageStorageService;
  @Mock private AiSettingsService aiSettingsService;

  private ShowTemplateListView view;

  @BeforeEach
  void setup() {
    when(showTypeService.findAll()).thenReturn(Collections.emptyList());
    when(showTemplateService.findAll()).thenReturn(Collections.emptyList());
    when(commentaryTeamRepository.findAll()).thenReturn(Collections.emptyList());

    view =
        new ShowTemplateListView(
            showTemplateService,
            showTypeService,
            commentaryTeamRepository,
            securityUtils,
            imageGenerationServiceFactory,
            imageStorageService,
            aiSettingsService);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the Show Templates toolbar")
  void shouldRenderToolbar() {
    ViewToolbar toolbar = _get(view, ViewToolbar.class);
    assertTrue(toolbar.isVisible());
  }

  @Test
  @DisplayName(
      "Grid should have Commentary Team, Recurrence, Duration, Matches, and Promos columns")
  void shouldHaveNewGridColumns() {
    Grid<?> grid = _get(view, Grid.class);
    List<String> headers =
        grid.getColumns().stream()
            .map(Grid.Column::getHeaderText)
            .filter(h -> h != null && !h.isEmpty())
            .collect(Collectors.toList());
    assertTrue(headers.contains("Commentary Team"));
    assertTrue(headers.contains("Recurrence"));
    assertTrue(headers.contains("Duration"));
    assertTrue(headers.contains("Matches"));
    assertTrue(headers.contains("Promos"));
  }
}
