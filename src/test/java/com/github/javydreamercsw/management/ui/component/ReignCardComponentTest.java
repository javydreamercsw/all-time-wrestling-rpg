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
package com.github.javydreamercsw.management.ui.component;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.dto.ranking.TitleReignDTO;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.RouterLink;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReignCardComponentTest extends AbstractViewTest {

  @Test
  void testComponentRendering() {
    TitleReignDTO dto =
        TitleReignDTO.builder()
            .id(1L)
            .championshipName("World Championship")
            .championshipTier("Main Event")
            .championNames(List.of("John Cena"))
            .durationDays(100)
            .startDate(Instant.now().minusSeconds(100 * 24 * 60 * 60))
            .isCurrent(true)
            .wonAtShowId(10L)
            .wonAtShowName("WrestleMania")
            .build();

    ReignCardComponent component = new ReignCardComponent(dto);
    assertNotNull(component);

    // Verify presence of info
    assertTrue(_get(component, Span.class, spec -> spec.withText("World Championship")) != null);
    assertTrue(_get(component, Span.class, spec -> spec.withText("CURRENT CHAMPION")) != null);

    RouterLink link = _get(component, RouterLink.class);
    assertEquals("Won at: WrestleMania", link.getText());
  }
}
