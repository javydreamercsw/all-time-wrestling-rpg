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
package com.github.javydreamercsw.management.ui.view.world;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.world.Arena;
import com.github.javydreamercsw.management.domain.world.ArenaRepository;
import com.github.javydreamercsw.management.domain.world.Location;
import com.github.javydreamercsw.management.domain.world.LocationRepository;
import com.github.javydreamercsw.management.service.world.ArenaService;
import com.github.javydreamercsw.management.service.world.LocationService;
import com.vaadin.flow.component.grid.Grid;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class WorldUIIT extends ManagementIntegrationTest {

  @Autowired private LocationService locationService;
  @Autowired private ArenaService arenaService;
  @Autowired private ImageStorageService imageStorageService;
  @Autowired private ArenaRepository arenaRepository;
  @Autowired private LocationRepository locationRepository;

  @BeforeEach
  void setUp() {
    arenaRepository.deleteAllInBatch();
    locationRepository.deleteAllInBatch();
  }

  @Test
  @DisplayName("LocationListView should display locations correctly")
  void locationListViewTest() {
    locationService.createLocation("Test City", "Description", null, Set.of("Tag1", "Tag2"));

    SecurityUtils securityUtils = mock(SecurityUtils.class);
    when(securityUtils.hasAnyRole(RoleName.ADMIN, RoleName.BOOKER)).thenReturn(true);

    LocationListView view =
        new LocationListView(locationService, securityUtils, imageStorageService);

    // Check grid columns
    Grid<Location> grid =
        (Grid<Location>)
            view.getChildren().filter(c -> c instanceof Grid).findFirst().orElseThrow();

    assertNotNull(grid);
    assertTrue(
        grid.getColumns().stream().anyMatch(c -> "Name".equals(c.getHeaderText())),
        "Name column not found");
    assertTrue(
        grid.getColumns().stream().anyMatch(c -> "Actions".equals(c.getHeaderText())),
        "Actions column not found");
  }

  @Test
  @DisplayName("ArenaListView should display arenas correctly")
  void arenaListViewTest() {
    Location loc = locationService.createLocation("Arena City", "Desc", null, Set.of());
    arenaService.createArena(
        "Test Arena", "Desc", loc.getId(), 5000, Arena.AlignmentBias.NEUTRAL, Set.of("Trait1"));

    SecurityUtils securityUtils = mock(SecurityUtils.class);
    when(securityUtils.hasAnyRole(RoleName.ADMIN, RoleName.BOOKER)).thenReturn(true);

    ArenaListView view =
        new ArenaListView(arenaService, locationService, securityUtils, imageStorageService);

    Grid<Arena> grid =
        (Grid<Arena>) view.getChildren().filter(c -> c instanceof Grid).findFirst().orElseThrow();

    assertNotNull(grid);
    assertTrue(
        grid.getColumns().stream().anyMatch(c -> "Location".equals(c.getHeaderText())),
        "Location column not found");
    assertTrue(
        grid.getColumns().stream().anyMatch(c -> "Bias".equals(c.getHeaderText())),
        "Bias column not found");
    assertTrue(
        grid.getColumns().stream().anyMatch(c -> "Actions".equals(c.getHeaderText())),
        "Actions column not found");
  }
}
