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
package com.github.javydreamercsw.management.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.config.CacheConfig;
import com.github.javydreamercsw.management.domain.rivalry.Rivalry;
import com.github.javydreamercsw.management.domain.rivalry.RivalryRepository;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

/** Unit tests for the canonical pair order backfill (ATW-8nc8). */
@ExtendWith(MockitoExtension.class)
class CanonicalizeRivalryPairsMigrationTest {

  @Mock private RivalryRepository rivalryRepository;
  @Mock private CacheManager cacheManager;
  @Mock private Cache rivalriesCache;

  @InjectMocks private CanonicalizeRivalryPairsMigration migration;

  private Universe universe;

  @BeforeEach
  void setUp() {
    universe = new Universe();
    universe.setId(1L);
    lenient().when(cacheManager.getCache(CacheConfig.RIVALRIES_CACHE)).thenReturn(rivalriesCache);
  }

  private Wrestler wrestler(long id) {
    Wrestler w = new Wrestler();
    w.setId(id);
    w.setName("Wrestler " + id);
    return w;
  }

  private Rivalry rivalry(long id, Wrestler w1, Wrestler w2, boolean active, int heat) {
    Rivalry r = new Rivalry();
    r.setId(id);
    r.setWrestler1(w1);
    r.setWrestler2(w2);
    r.setUniverse(universe);
    r.setIsActive(active);
    r.setHeat(heat);
    r.setStartedDate(Instant.parse("2026-01-01T00:00:00Z"));
    return r;
  }

  @Test
  @DisplayName("id is stable kebab-case")
  void idIsStableKebabCase() {
    assertThat(migration.id()).isEqualTo("canonicalize-rivalry-pairs");
  }

  @Test
  @DisplayName("Reversed rows are canonicalized; already-canonical rows untouched")
  void reversedRowsAreCanonicalized() {
    Wrestler w2 = wrestler(2L);
    Wrestler w5 = wrestler(5L);
    Rivalry reversed = rivalry(1L, w5, w2, true, 10);
    Rivalry canonical = rivalry(2L, w2, w5, true, 5);
    when(rivalryRepository.findAll()).thenReturn(List.of(reversed, canonical));

    migration.migrate();

    ArgumentCaptor<Rivalry> captor = ArgumentCaptor.forClass(Rivalry.class);
    verify(rivalryRepository, Mockito.times(2)).save(captor.capture());
    Rivalry swapped = captor.getAllValues().get(0);
    assertThat(swapped.getWrestler1()).isEqualTo(w2);
    assertThat(swapped.getWrestler2()).isEqualTo(w5);
  }

  @Test
  @DisplayName("Group with one active + ended cycles: only canonicalize, ended kept")
  void groupsWithSingleActiveRowAreOnlyCanonicalized() {
    Wrestler w3 = wrestler(3L);
    Wrestler w7 = wrestler(7L);
    Rivalry activeReversed = rivalry(1L, w7, w3, true, 20);
    Rivalry endedCycle = rivalry(2L, w3, w7, false, 44);
    when(rivalryRepository.findAll()).thenReturn(List.of(activeReversed, endedCycle));

    migration.migrate();

    ArgumentCaptor<Rivalry> captor = ArgumentCaptor.forClass(Rivalry.class);
    verify(rivalryRepository, Mockito.times(2)).save(captor.capture());
    // Active row swapped, still active.
    assertThat(captor.getAllValues().get(0).getIsActive()).isTrue();
    // Ended cycle never ended again (no merge reason).
    verify(rivalryRepository, never()).saveAndFlush(any());
    assertThat(captor.getAllValues().get(1).getEndedDate()).isNull();
  }

  @Test
  @DisplayName("Duplicate actives: hottest kept, others ended with merge reason")
  void duplicateActiveRowsKeepHottestAndEndOthersWithMergedReason() {
    Wrestler w2 = wrestler(2L);
    Wrestler w5 = wrestler(5L);
    Rivalry hot = rivalry(1L, w2, w5, true, 80);
    Rivalry cold = rivalry(2L, w5, w2, true, 10);
    when(rivalryRepository.findAll()).thenReturn(List.of(hot, cold));

    migration.migrate();

    ArgumentCaptor<Rivalry> captor = ArgumentCaptor.forClass(Rivalry.class);
    verify(rivalryRepository, Mockito.times(2)).save(captor.capture());
    assertThat(hot.getIsActive()).isTrue();
    assertThat(cold.getIsActive()).isFalse();
    assertThat(cold.getEndedDate()).isNotNull();
    // The terminal heat event records the merge like any rivalry end
    // (endRivalry prefixes the reason with "Rivalry ended: ").
    assertThat(
            cold.getHeatEvents().stream()
                .anyMatch(
                    e -> e.getReason().contains(CanonicalizeRivalryPairsMigration.MERGED_REASON)))
        .isTrue();
  }

  @Test
  @DisplayName("Three ended cycles, no active: all kept as history")
  void endedCyclesAreNeverMerged() {
    Wrestler w2 = wrestler(2L);
    Wrestler w5 = wrestler(5L);
    Rivalry e1 = rivalry(1L, w2, w5, false, 30);
    Rivalry e2 = rivalry(2L, w5, w2, false, 20);
    Rivalry e3 = rivalry(3L, w2, w5, false, 10);
    when(rivalryRepository.findAll()).thenReturn(List.of(e1, e2, e3));

    migration.migrate();

    assertThat(e1.getIsActive()).isFalse();
    assertThat(e2.getIsActive()).isFalse();
    assertThat(e3.getIsActive()).isFalse();
    // Swaps happened for e2 only; no row was ended (all already ended).
    verify(rivalryRepository, Mockito.times(3)).save(any(Rivalry.class));
  }

  @Test
  @DisplayName("Same pair in different universes is independent")
  void samePairInDifferentUniversesIsIndependent() {
    Wrestler w2 = wrestler(2L);
    Wrestler w5 = wrestler(5L);
    Universe otherUniverse = new Universe();
    otherUniverse.setId(2L);
    Rivalry universeOne = rivalry(1L, w2, w5, true, 15);
    Rivalry universeTwo = rivalry(2L, w2, w5, true, 15);
    universeTwo.setUniverse(otherUniverse);
    when(rivalryRepository.findAll()).thenReturn(List.of(universeOne, universeTwo));

    migration.migrate();

    assertThat(universeOne.getIsActive()).isTrue();
    assertThat(universeTwo.getIsActive()).isTrue();
  }

  @Test
  @DisplayName("Cache evicted when changes were made")
  void cacheEvictedAfterChanges() {
    Wrestler w2 = wrestler(2L);
    Wrestler w5 = wrestler(5L);
    when(rivalryRepository.findAll()).thenReturn(List.of(rivalry(1L, w5, w2, true, 5)));

    migration.migrate();

    verify(rivalriesCache).clear();
  }
}
