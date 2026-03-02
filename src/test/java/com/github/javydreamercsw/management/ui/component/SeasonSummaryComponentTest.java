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

import com.github.javydreamercsw.management.domain.season.Season;
import com.github.javydreamercsw.management.dto.SeasonStatsDTO;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class SeasonSummaryComponentTest extends AbstractViewTest {

  @Test
  void testSeasonSummaryComponentRendering() {
    SeasonStatsDTO stats =
        SeasonStatsDTO.builder()
            .seasonName("Season 1")
            .wins(5)
            .losses(2)
            .draws(1)
            .startingFans(1000L)
            .endingFans(1500L)
            .accolades(Arrays.asList("World Championship"))
            .build();

    SeasonSummaryComponent component = new SeasonSummaryComponent(Collections.emptyList(), s -> {});
    component.updateStats(stats);
    assertNotNull(component);

    // Verify presence of info
    assertNotNull(_get(component, H4.class, spec -> spec.withText("Season 1 Summary")));
    assertNotNull(_get(component, Span.class, spec -> spec.withText("Record: 5-2-1")));
    assertNotNull(
        _get(component, Span.class, spec -> spec.withText("Accolades: World Championship")));
  }

  @Test
  void testSeasonSelectionUpdatesData() {
    Season season1 = new Season();
    season1.setName("Season 1");
    Season season2 = new Season();
    season2.setName("Season 2");
    List<Season> seasons = Arrays.asList(season1, season2);

    AtomicReference<Season> selectedSeason = new AtomicReference<>();
    SeasonSummaryComponent component = new SeasonSummaryComponent(seasons, selectedSeason::set);

    ComboBox<Season> comboBox = _get(component, ComboBox.class);
    comboBox.setValue(season2);

    assertEquals(season2, selectedSeason.get());
  }
}
