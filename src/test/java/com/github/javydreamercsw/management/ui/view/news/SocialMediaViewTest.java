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
package com.github.javydreamercsw.management.ui.view.news;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.service.news.NewsService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H2;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class SocialMediaViewTest extends AbstractViewTest {

  @Mock private NewsService newsService;
  @Mock private SecurityUtils securityUtils;

  private SocialMediaView view;

  @BeforeEach
  void setup() {
    when(newsService.getAllNews()).thenReturn(Collections.emptyList());
    view = new SocialMediaView(newsService, securityUtils, new ObjectMapper());
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the Wrestling World Feed heading")
  void shouldRenderHeading() {
    H2 heading = _get(view, H2.class, spec -> spec.withText("Wrestling World Feed"));
    assertTrue(heading.isVisible());
  }
}
