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
package com.github.javydreamercsw.management.service.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.world.Arena;
import com.github.javydreamercsw.management.domain.world.ArenaRepository;
import com.github.javydreamercsw.management.domain.world.Location;
import com.github.javydreamercsw.management.domain.world.LocationRepository;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@WithMockUser(roles = {"ADMIN"})
class WorldDataIntegrationTest extends AbstractIntegrationTest {

  @Autowired private LocationService locationService;
  @Autowired private ArenaService arenaService;
  @Autowired private ShowService showService;
  @Autowired private LocationRepository locationRepository;
  @Autowired private ArenaRepository arenaRepository;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private com.github.javydreamercsw.management.DataInitializer dataInitializer;

  @BeforeEach
  void setupData() {
    // Explicitly initialize data since it's disabled by profile in Application.java
    dataInitializer.init();
  }

  @Test
  void testLocationCRUD() {
    // Create
    Location location =
        locationService.createLocation(
            "Test City",
            "A test location",
            "http://test.com/img.png",
            new HashSet<>(Arrays.asList("Test", "Tag")));
    assertNotNull(location.getId());

    // Read
    Optional<Location> found = locationRepository.findByName("Test City");
    assertTrue(found.isPresent());
    assertEquals("A test location", found.get().getDescription());
    assertTrue(found.get().getCulturalTags().contains("Test"));

    // Update
    locationService.updateLocation(
        location.getId(),
        "Test City Updated",
        "New desc",
        "http://test.com/new.png",
        new HashSet<>(Arrays.asList("New")));
    Location updated = locationRepository.findById(location.getId()).get();
    assertEquals("Test City Updated", updated.getName());
    assertEquals("New desc", updated.getDescription());
    assertFalse(updated.getCulturalTags().contains("Test"));
    assertTrue(updated.getCulturalTags().contains("New"));

    // Delete
    locationService.deleteLocation(location.getId());
    assertFalse(locationRepository.existsById(location.getId()));
  }

  @Test
  void testArenaCRUDAndLinking() {
    Location location = locationService.createLocation("Arena City", "Desc", null, new HashSet<>());

    // Create Arena
    Arena arena =
        arenaService.createArena(
            "Test Arena",
            "Desc",
            location.getId(),
            10000,
            Arena.AlignmentBias.FACE_FAVORABLE,
            new HashSet<>(Arrays.asList("Vibe")));
    assertNotNull(arena.getId());
    assertEquals(location.getId(), arena.getLocation().getId());

    // Update Arena
    arenaService.updateArena(
        arena.getId(),
        "Updated Arena",
        "New desc",
        location.getId(),
        20000,
        Arena.AlignmentBias.HEEL_FAVORABLE,
        "http://img.png",
        new HashSet<>(Arrays.asList("NewTrait")));
    Arena updated = arenaRepository.findById(arena.getId()).get();
    assertEquals("Updated Arena", updated.getName());
    assertEquals(20000, updated.getCapacity());
    assertEquals(Arena.AlignmentBias.HEEL_FAVORABLE, updated.getAlignmentBias());

    // Create Show linked to Arena
    ShowType type = new ShowType();
    type.setName("Test Type");
    type.setDescription("Test Description");
    showTypeRepository.save(type);

    Show show =
        showService.createShow(
            "Test Show", "Desc", type.getId(), null, null, null, null, null, arena.getId());
    assertNotNull(show.getArena());
    assertEquals(arena.getId(), show.getArena().getId());
  }

  @Test
  void testDataInitializationIntegrity() {
    // Verify that DataInitializer has loaded the expected lore data.
    assertTrue(locationRepository.count() >= 26, "Should have at least 26 lore locations");
    assertTrue(arenaRepository.count() >= 33, "Should have at least 33 lore arenas");

    Optional<Location> tokyo = locationRepository.findByName("Neo-Tokyo");
    assertTrue(tokyo.isPresent());
    assertTrue(tokyo.get().getCulturalTags().contains("cyberpunk"));

    Optional<Arena> octagon = arenaRepository.findByName("Zero-G Octagon");
    assertTrue(octagon.isPresent());
    assertEquals("Lunar Colony Alpha", octagon.get().getLocation().getName());
  }
}
