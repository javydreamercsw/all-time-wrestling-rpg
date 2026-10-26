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
package com.github.javydreamercsw.management.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.service.npc.NpcService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NpcSyncTest {

  @Mock private NpcService npcService;

  private NpcSync sync;

  @BeforeEach
  void setUp() {
    sync = new NpcSync(false, npcService, new ObjectMapper());
  }

  @Test
  void sync_savesNewNpcs() {
    when(npcService.findAllUnfiltered()).thenReturn(List.of());

    sync.sync();

    verify(npcService, atLeastOnce()).saveAll(any());
  }

  @Test
  void sync_skipsWhenNotEmptyFlagSet() {
    // Skip gate with a complete catalog: every seeded NPC exists → top-up inserts nothing.
    NpcSync skipSync = new NpcSync(true, npcService, new ObjectMapper());
    when(npcService.count()).thenReturn(15L);
    when(npcService.findAllUnfiltered())
        .thenReturn(
            List.of(
                    "Colonel Mustafa",
                    "Charles",
                    "Samuel Winters",
                    "Rezan the Resolved",
                    "Rigo Del Sarto",
                    "Anika Stroud",
                    "Tomás “Kid Whistle” Iglesias",
                    "Beatrix “Trixie” Halvorsen",
                    "Brother Cassian",
                    "Dr. Yumi Sato",
                    "Reginald Fairfax III",
                    "Amara Diallo",
                    "MVP",
                    "Commissioner Blake",
                    "Lena Voss")
                .stream()
                .map(
                    name -> {
                      Npc npc = new Npc();
                      npc.setName(name);
                      return npc;
                    })
                .toList());

    skipSync.sync();

    verify(npcService, never()).saveAll(any());
  }

  @Test
  void sync_doesNotSaveExistingUnchangedNpc() {
    // Charles is in npcs.json with no awareness field — unchanged NPC must not be re-saved
    Npc existing = new Npc();
    existing.setName("Charles");
    existing.setDescription("The mysterious owner of ATW.");
    when(npcService.findAllUnfiltered()).thenReturn(List.of(existing));

    sync.sync();

    verify(npcService, never())
        .setAwareness(argThat(npc -> "Charles".equals(npc.getName())), anyInt());
  }

  @Test
  void sync_savesExistingNpcWhenDescriptionChanged() {
    Npc existing = new Npc();
    existing.setName("Charles"); // Charles is in npcs.json
    existing.setDescription("stale description"); // differs from file → triggers dirty-check save
    when(npcService.findAllUnfiltered()).thenReturn(List.of(existing));

    sync.sync();

    // saveAll must have been called (dirty-check path was taken)
    verify(npcService, atLeastOnce()).saveAll(any());
    // the existing object's description was overwritten by the sync with the file value
    assertEquals("The mysterious owner of ATW.", existing.getDescription());
  }

  @Test
  void sync_topUpInsertsOnlyMissingNpcsWhenSkipFlagSet() {
    // Simulates a prod install with the skip flag set: 4 pre-2.12.0 referees exist, the 6 new
    // ones (and the rest of the seed catalog) are missing. Top-up must insert exactly the 11
    // missing seed NPCs and never re-save an existing row.
    NpcSync skipSync = new NpcSync(true, npcService, new ObjectMapper());
    when(npcService.count()).thenReturn(4L);
    List<String> existingNames =
        List.of("Samuel Winters", "Rezan the Resolved", "Rigo Del Sarto", "Anika Stroud");
    when(npcService.findAllUnfiltered())
        .thenReturn(
            existingNames.stream()
                .map(
                    name -> {
                      Npc npc = new Npc();
                      npc.setName(name);
                      return npc;
                    })
                .toList());

    skipSync.sync();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Npc>> captor = ArgumentCaptor.forClass(List.class);
    verify(npcService).saveAll(captor.capture());
    List<Npc> saved = captor.getValue();
    assertEquals(11, saved.size(), "npcs.json seeds 15 NPCs; 4 exist → 11 must be inserted");
    Set<String> savedNames = saved.stream().map(Npc::getName).collect(Collectors.toSet());
    // 2.12.0's new referees must be among the inserted rows
    assertTrue(savedNames.contains("Tomás “Kid Whistle” Iglesias"));
    assertTrue(savedNames.contains("Amara Diallo"));
    for (String existing : existingNames) {
      assertFalse(savedNames.contains(existing), "existing NPC must not be re-saved: " + existing);
    }
  }

  @Test
  void sync_topUpLeavesExistingNpcUntouchedWhenSkipFlagSet() {
    NpcSync skipSync = new NpcSync(true, npcService, new ObjectMapper());
    when(npcService.count()).thenReturn(1L);
    Npc existing = new Npc();
    existing.setName("Charles");
    existing.setDescription("stale description");
    when(npcService.findAllUnfiltered()).thenReturn(List.of(existing));

    skipSync.sync();

    // Charles is in npcs.json but already exists — under the skip gate his row must not be
    // re-saved (data-protection guarantee) and his stale description must survive.
    verify(npcService, never()).saveAll(argThat(list -> list.contains(existing)));
    assertEquals("stale description", existing.getDescription());
  }
}
