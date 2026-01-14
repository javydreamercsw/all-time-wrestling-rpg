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

import static com.github.mvysny.kaributesting.v10.LocatorJ._assert;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.github.javydreamercsw.management.dto.ranking.TitleReignDTO;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.html.Span;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class HistoryTimelineComponentTest extends AbstractViewTest {

  @Test
  void testTimelineRendering() {
    TitleReignDTO reign1 =
        TitleReignDTO.builder()
            .id(1L)
            .championNames(List.of("Undertaker"))
            .startDate(Instant.now().minusSeconds(200 * 24 * 60 * 60))
            .endDate(Instant.now().minusSeconds(100 * 24 * 60 * 60))
            .isCurrent(false)
            .build();

    TitleReignDTO reign2 =
        TitleReignDTO.builder()
            .id(2L)
            .championNames(List.of("Kane"))
            .startDate(Instant.now().minusSeconds(100 * 24 * 60 * 60))
            .isCurrent(true)
            .build();

    HistoryTimelineComponent component = new HistoryTimelineComponent(List.of(reign2, reign1));
    assertNotNull(component);

    // Verify champions are shown
    _assert(component, Span.class, 1, spec -> spec.withText("Undertaker"));
    _assert(component, Span.class, 1, spec -> spec.withText("Kane"));
    _assert(component, Span.class, 1, spec -> spec.withText("CURRENT"));
  }
}
