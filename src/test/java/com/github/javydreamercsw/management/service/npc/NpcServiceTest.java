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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NpcServiceTest {

  @Mock private NpcRepository npcRepository;

  @InjectMocks private NpcService npcService;

  private Npc npc;

  @BeforeEach
  void setUp() {
    npc = new Npc();
    npc.setId(1L);
    npc.setName("Test NPC");
    npc.setNpcType("Referee");
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
    when(npcRepository.findByName("Test NPC")).thenReturn(java.util.Optional.of(npc));

    Npc result = npcService.findByName("Test NPC");

    assertEquals(npc, result);
  }

  @Test
  void testFindByExternalId() {
    npc.setExternalId("test-id");
    when(npcRepository.findByExternalId("test-id")).thenReturn(npc);

    Npc result = npcService.findByExternalId("test-id");

    assertEquals(npc, result);
  }
}
