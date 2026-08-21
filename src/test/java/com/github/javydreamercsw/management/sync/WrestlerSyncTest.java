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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.ranking.TierRecalculationService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WrestlerSyncTest {

  @Mock private WrestlerRepository wrestlerRepository;

  @Mock
  private com.github.javydreamercsw.management.domain.wrestler.WrestlerAbilityRepository
      wrestlerAbilityRepository;

  @Mock private WrestlerService wrestlerService;
  @Mock private UniverseRepository universeRepository;
  @Mock private WrestlerStateRepository wrestlerStateRepository;
  @Mock private NpcService npcService;
  @Mock private TierRecalculationService tierRecalculationService;
  @Mock private ResourcePatternResolver resourcePatternResolver;

  private WrestlerSync sync;

  @BeforeEach
  void setUp() throws Exception {
    Universe mockUniverse = org.mockito.Mockito.mock(Universe.class);
    when(mockUniverse.getId()).thenReturn(1L);
    when(universeRepository.findAll()).thenReturn(List.of(mockUniverse));

    // Return the real wrestlers*.json from the classpath for integration-style characterization
    PathMatchingResourcePatternResolver real = new PathMatchingResourcePatternResolver();
    Resource[] resources = real.getResources("classpath*:wrestlers/*.json");
    when(resourcePatternResolver.getResources(anyString())).thenReturn(resources);

    sync =
        new WrestlerSync(
            false,
            wrestlerRepository,
            wrestlerAbilityRepository,
            wrestlerService,
            universeRepository,
            wrestlerStateRepository,
            npcService,
            tierRecalculationService,
            resourcePatternResolver,
            new ObjectMapper());
  }

  @Test
  void sync_savesNewWrestlers() {
    when(wrestlerRepository.findAllWithAlignments()).thenReturn(List.of());
    WrestlerState state = new WrestlerState();
    lenient().when(wrestlerService.getOrCreateState(any(), any())).thenReturn(state);

    sync.sync();

    verify(wrestlerRepository, atLeastOnce()).saveAll(any());
  }

  @Test
  void sync_skipsWhenNotEmptyFlagSet() {
    WrestlerSync skipSync =
        new WrestlerSync(
            true,
            wrestlerRepository,
            wrestlerAbilityRepository,
            wrestlerService,
            universeRepository,
            wrestlerStateRepository,
            npcService,
            tierRecalculationService,
            resourcePatternResolver,
            new ObjectMapper());
    when(wrestlerRepository.count()).thenReturn(10L);

    skipSync.sync();

    verify(wrestlerRepository, never()).saveAll(any());
    verify(wrestlerRepository, never()).flush();
  }

  @Test
  void sync_backfillsAbilitiesWhenSkippingExistingWrestlers() {
    Wrestler existing = new Wrestler();
    existing.setId(1L);
    existing.setName("Rob Van Dam");
    when(wrestlerRepository.count()).thenReturn(1L);
    when(wrestlerRepository.findAll()).thenReturn(List.of(existing));
    when(wrestlerAbilityRepository.findByWrestler(existing)).thenReturn(List.of());

    WrestlerSync skipSync =
        new WrestlerSync(
            true,
            wrestlerRepository,
            wrestlerAbilityRepository,
            wrestlerService,
            universeRepository,
            wrestlerStateRepository,
            npcService,
            tierRecalculationService,
            resourcePatternResolver,
            new ObjectMapper());

    skipSync.sync();

    verify(wrestlerAbilityRepository, atLeastOnce()).save(any());
  }

  @Test
  void sync_preservesFansWhenExistingIsHigher() {
    Wrestler existing = new Wrestler();
    existing.setId(1L);
    existing.setName("Rob Van Dam");
    when(wrestlerRepository.findAllWithAlignments()).thenReturn(List.of(existing));

    WrestlerState state = new WrestlerState();
    state.setId(1L);
    state.setFans(999_999L); // higher than any seed value in the file
    state.setBumps(Integer.MAX_VALUE); // higher than any seed bump value
    when(wrestlerService.getOrCreateState(any(), any())).thenReturn(state);

    sync.sync();

    // fans should NOT be decreased — dirty-check only sets if file value is higher
    verify(wrestlerStateRepository, never()).save(any());
  }

  @Test
  void sync_evictsWrestlerCacheAfterSync() {
    when(wrestlerRepository.findAllWithAlignments()).thenReturn(List.of());
    lenient().when(wrestlerService.getOrCreateState(any(), any())).thenReturn(new WrestlerState());

    sync.sync();

    verify(wrestlerService).evictWrestlerCache();
  }
}
