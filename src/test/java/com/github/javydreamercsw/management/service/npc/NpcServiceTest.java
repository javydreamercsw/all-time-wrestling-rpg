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
    when(npcRepository.findByName("Test NPC")).thenReturn(npc);

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
