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
package com.github.javydreamercsw.management.service.npc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.npc.NpcRepository;
import com.github.javydreamercsw.management.service.expansion.ExpansionService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NpcServiceTest {

  @Mock private NpcRepository npcRepository;
  @Mock private ExpansionService expansionService;
  @Mock private com.github.javydreamercsw.base.image.DefaultImageService imageService;

  @InjectMocks private NpcService npcService;

  private Npc npc;

  @BeforeEach
  void setUp() {
    npc = new Npc();
    npc.setId(1L);
    npc.setName("Test NPC");
    npc.setNpcType("Referee");
    npc.setExpansionCode("BASE_GAME");

    when(expansionService.getEnabledExpansionCodes()).thenReturn(Arrays.asList("BASE_GAME"));
  }

  @Test
  void testFindAll() {
    List<Npc> npcs = new ArrayList<>();
    npcs.add(npc);
    when(npcRepository.findAll()).thenReturn(npcs);

    List<Npc> result = npcService.findAll();

    assertEquals(1, result.size());
    assertEquals(npc, result.get(0));
  }

  @Test
  void testFindAllByType() {
    List<Npc> npcs = new ArrayList<>();
    npcs.add(npc);
    when(npcRepository.findAllByNpcType("Referee")).thenReturn(npcs);

    List<Npc> result = npcService.findAllByType("Referee");

    assertEquals(1, result.size());
    assertEquals(npc, result.get(0));
  }

  @Test
  void testSave() {
    when(npcRepository.save(npc)).thenReturn(npc);

    Npc result = npcService.save(npc);

    assertEquals(npc, result);
  }

  @Test
  void testDelete() {
    npcService.delete(npc);
    verify(npcRepository, times(1)).delete(npc);
  }

  @Test
  void testFindByName() {
    when(npcRepository.findByName("Test NPC")).thenReturn(Optional.of(npc));

    Npc result = npcService.findByName("Test NPC");

    assertEquals(npc, result);
  }

  @Test
  void testFindByExternalId() {
    npc.setExternalId("test-id");
    when(npcRepository.findByExternalId("test-id")).thenReturn(Optional.of(npc));

    Npc result = npcService.findByExternalId("test-id").orElse(null);

    assertEquals(npc, result);
  }

  @Test
  void testFindAll_FiltersByExpansion() {
    Npc baseNpc = Npc.builder().name("Base").expansionCode("BASE_GAME").build();
    Npc rumbleNpc = Npc.builder().name("Rumble").expansionCode("RUMBLE").build();

    when(npcRepository.findAll()).thenReturn(Arrays.asList(baseNpc, rumbleNpc));

    // Case 1: Only BASE_GAME enabled
    when(expansionService.getEnabledExpansionCodes()).thenReturn(Arrays.asList("BASE_GAME"));
    List<Npc> results = npcService.findAll();
    assertEquals(1, results.size());
    assertEquals("Base", results.get(0).getName());

    // Case 2: Both enabled
    when(expansionService.getEnabledExpansionCodes())
        .thenReturn(Arrays.asList("BASE_GAME", "RUMBLE"));
    results = npcService.findAll();
    assertEquals(2, results.size());
  }
}
