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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.image.ImageGenerationServiceFactory;
import com.github.javydreamercsw.management.domain.world.Arena;
import com.github.javydreamercsw.management.domain.world.ArenaRepository;
import com.github.javydreamercsw.management.domain.world.Location;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArenaService Tests")
class ArenaServiceTest {

  @Mock private ArenaRepository arenaRepository;
  @Mock private LocationService locationService;
  @Mock private ImageGenerationServiceFactory imageGenerationServiceFactory;

  @InjectMocks private ArenaService arenaService;

  private List<Arena> testArenas;

  @BeforeEach
  void setUp() {
    Location loc1 = Location.builder().name("Location1").build();
    Location loc2 = Location.builder().name("Location2").build();
    Location loc3 = Location.builder().name("Location3").build();

    testArenas =
        Arrays.asList(
            Arena.builder().id(1L).name("Small Arena").capacity(5000).location(loc1).build(),
            Arena.builder().id(2L).name("Medium Arena").capacity(15000).location(loc2).build(),
            Arena.builder().id(3L).name("Large Arena").capacity(30000).location(loc3).build(),
            Arena.builder().id(4L).name("Biggest Arena").capacity(80000).location(loc1).build(),
            Arena.builder().id(5L).name("Another Small").capacity(7000).location(loc2).build());
  }

  @Test
  @DisplayName("Should assign a random arena for a regular show")
  void testAssignArenaToShow_regularShow() {
    when(arenaRepository.findAll()).thenReturn(testArenas);

    Long assignedArenaId = arenaService.assignArenaToShow(false);

    assertNotNull(assignedArenaId);
    Optional<Arena> assignedArena =
        testArenas.stream().filter(a -> a.getId().equals(assignedArenaId)).findFirst();
    assertTrue(assignedArena.isPresent());
  }

  @Test
  @DisplayName("Should assign a larger arena for a PLE")
  void testAssignArenaToShow_pleShow() {
    when(arenaRepository.findAll()).thenReturn(testArenas);

    Long assignedArenaId = arenaService.assignArenaToShow(true);

    assertNotNull(assignedArenaId);
    Optional<Arena> assignedArena =
        testArenas.stream().filter(a -> a.getId().equals(assignedArenaId)).findFirst();
    assertTrue(assignedArena.isPresent());

    // Verify it's one of the largest arenas (e.g., in the top 5)
    List<Arena> sortedArenas =
        testArenas.stream()
            .sorted(java.util.Comparator.comparingInt(Arena::getCapacity).reversed())
            .toList();
    List<Long> largestArenaIds = sortedArenas.subList(0, 5).stream().map(Arena::getId).toList();
    assertTrue(largestArenaIds.contains(assignedArenaId));
  }

  @Test
  @DisplayName("Should return null if no arenas are available")
  void testAssignArenaToShow_noArenas() {
    when(arenaRepository.findAll()).thenReturn(List.of());

    Long assignedArenaId = arenaService.assignArenaToShow(false);

    org.junit.jupiter.api.Assertions.assertNull(assignedArenaId);
  }
}
