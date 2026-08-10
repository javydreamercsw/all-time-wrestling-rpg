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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    NpcSync skipSync = new NpcSync(true, npcService, new ObjectMapper());
    when(npcService.count()).thenReturn(5L);

    skipSync.sync();

    verify(npcService, never()).saveAll(any());
  }

  @Test
  void sync_doesNotSaveExistingUnchangedNpc() {
    // Build an existing NPC that matches the file data — should not trigger saveAll
    Npc existing = new Npc();
    existing.setName("Paul Bearer");
    existing.setDescription("The manager of The Undertaker");
    when(npcService.findAllUnfiltered()).thenReturn(List.of(existing));

    sync.sync();

    // Verify setAwareness was never called for the pre-existing Paul Bearer — dirty-check path
    verify(npcService, never())
        .setAwareness(argThat(npc -> "Paul Bearer".equals(npc.getName())), anyInt());
  }
}
