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
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.universe.UniverseSettingsService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
  @Mock private UniverseContextService universeContextService;
  @Mock private UniverseSettingsService universeSettingsService;
  @Mock private com.github.javydreamercsw.base.image.DefaultImageService imageService;

  @InjectMocks private NpcService npcService;

  private Npc npc;

  @BeforeEach
  public void setUp() {
    npc = new Npc();
    npc.setId(1L);
    npc.setName("Test NPC");
    npc.setNpcType("Referee");
    npc.setExpansionCode("BASE_GAME");

    when(universeContextService.getCurrentUniverse()).thenReturn(Optional.empty());
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

  @Test
  void findAllByType_nullExpansionCode_notFilteredOut() {
    // Commentators are created by CommentaryService without an expansionCode.
    // They must always appear regardless of which expansions are enabled.
    Npc commentator = new Npc();
    commentator.setName("Jane Commentator");
    commentator.setNpcType("Commentator");
    commentator.setExpansionCode(null);

    when(npcRepository.findAllByNpcType("Commentator")).thenReturn(Arrays.asList(commentator));

    List<Npc> result = npcService.findAllByType("Commentator");

    assertEquals(1, result.size());
  }

  @Test
  void findAll_nullExpansionCode_notFilteredOut() {
    Npc systemNpc = new Npc();
    systemNpc.setName("System NPC");
    systemNpc.setNpcType("Commentator");
    systemNpc.setExpansionCode(null);

    Npc baseNpc = Npc.builder().name("Base Ref").expansionCode("BASE_GAME").build();

    when(npcRepository.findAll()).thenReturn(Arrays.asList(systemNpc, baseNpc));
    when(expansionService.getEnabledExpansionCodes()).thenReturn(Arrays.asList("BASE_GAME"));

    List<Npc> result = npcService.findAll();

    assertEquals(2, result.size());
  }

  @Test
  void findAll_excludesInactiveNpcs() {
    Npc activeNpc = Npc.builder().name("Active Ref").expansionCode("BASE_GAME").build();
    activeNpc.setActive(true);

    Npc inactiveNpc = Npc.builder().name("Inactive Ref").expansionCode("BASE_GAME").build();
    inactiveNpc.setActive(false);

    when(npcRepository.findAll()).thenReturn(Arrays.asList(activeNpc, inactiveNpc));
    when(expansionService.getEnabledExpansionCodes()).thenReturn(Arrays.asList("BASE_GAME"));

    List<Npc> result = npcService.findAll();

    assertEquals(1, result.size());
    assertEquals("Active Ref", result.get(0).getName());
  }

  @Test
  void findAllIncludingInactive_includesInactiveNpcs() {
    Npc activeNpc = Npc.builder().name("Active Ref").expansionCode("BASE_GAME").build();
    activeNpc.setActive(true);

    Npc inactiveNpc = Npc.builder().name("Inactive Ref").expansionCode("BASE_GAME").build();
    inactiveNpc.setActive(false);

    when(npcRepository.findAll()).thenReturn(Arrays.asList(activeNpc, inactiveNpc));
    when(expansionService.getEnabledExpansionCodes()).thenReturn(Arrays.asList("BASE_GAME"));

    List<Npc> result = npcService.findAllIncludingInactive();

    assertEquals(2, result.size());
  }

  // ==================== findAll(Set<String>) — async-safe overload ====================
  // Regression coverage for ATW-uwqv: findAll() is @Cacheable with no explicit key, so a caller
  // on a thread with no universe context (e.g. cache warm-up from a request thread) must not
  // populate that shared cache entry with the wrong universe's filtered list. Callers outside the
  // UI thread should use this uncached, explicit-codes overload instead (mirrors the
  // SegmentTypeService/SegmentRuleService fix for ATW-8djt).

  @Test
  void findAllWithCodes_doesNotConsultUniverseContext() {
    Npc baseNpc = Npc.builder().name("Base").expansionCode("BASE_GAME").build();
    when(npcRepository.findAll()).thenReturn(Arrays.asList(baseNpc));

    List<Npc> result = npcService.findAll(Set.of("BASE_GAME"));

    assertEquals(1, result.size());
    verify(universeContextService, never()).getCurrentUniverse();
  }

  @Test
  void findAllWithCodes_emptyCodesFiltersOutAllNpcsWithExpansionCode() {
    Npc baseNpc = Npc.builder().name("Base").expansionCode("BASE_GAME").build();
    when(npcRepository.findAll()).thenReturn(Arrays.asList(baseNpc));

    List<Npc> result = npcService.findAll(Set.of());

    assertTrue(
        result.isEmpty(), "NPCs with non-null expansionCode must be excluded when set is empty");
  }

  @Test
  void findAllWithCodes_nullExpansionCodeAlwaysIncluded() {
    Npc noCode = Npc.builder().name("Commentator").expansionCode(null).build();
    when(npcRepository.findAll()).thenReturn(Arrays.asList(noCode));

    List<Npc> result = npcService.findAll(Set.of());

    assertEquals(1, result.size(), "NPCs with null expansionCode must always be included");
  }

  @Test
  void findAllWithCodes_excludesInactiveNpcs() {
    Npc activeNpc = Npc.builder().name("Active Ref").expansionCode("BASE_GAME").build();
    activeNpc.setActive(true);
    Npc inactiveNpc = Npc.builder().name("Inactive Ref").expansionCode("BASE_GAME").build();
    inactiveNpc.setActive(false);

    when(npcRepository.findAll()).thenReturn(Arrays.asList(activeNpc, inactiveNpc));

    List<Npc> result = npcService.findAll(Set.of("BASE_GAME"));

    assertEquals(1, result.size());
    assertEquals("Active Ref", result.get(0).getName());
  }

  @Test
  void setActive_persistsChange() {
    Npc activeNpc = Npc.builder().name("Ref").expansionCode("BASE_GAME").build();
    activeNpc.setId(42L);
    activeNpc.setActive(true);

    when(npcRepository.findById(42L)).thenReturn(Optional.of(activeNpc));

    npcService.setActive(42L, false);

    assertFalse(activeNpc.isActive());
    verify(npcRepository).save(activeNpc);
  }
}
