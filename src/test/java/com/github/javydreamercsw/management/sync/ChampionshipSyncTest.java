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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.domain.wrestler.Gender;
import com.github.javydreamercsw.base.domain.wrestler.WrestlerTier;
import com.github.javydreamercsw.management.domain.title.ChampionshipType;
import com.github.javydreamercsw.management.domain.title.Title;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.universe.UniverseRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.title.TitleReignRepairService;
import com.github.javydreamercsw.management.service.title.TitleService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChampionshipSyncTest {

  @Mock private TitleService titleService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private UniverseRepository universeRepository;
  @Mock private TitleReignRepairService titleReignRepairService;

  private static final SeedSyncSupport VERIFY_SUPPORT = new SeedSyncSupport("verify");

  private void stubUniverse() {
    Universe universe = new Universe();
    universe.setId(1L);
    when(universeRepository.findAll()).thenReturn(List.of(universe));
  }

  private String titleFingerprint(Title title) {
    return VERIFY_SUPPORT.fingerprint(
        title.getDescription(),
        title.getTier() == null ? null : title.getTier().name(),
        title.getChampionshipType() == null ? null : title.getChampionshipType().name(),
        title.getEffectScript(),
        title.getGender() == null ? null : title.getGender().name(),
        title.getImageUrl(),
        title.getExpansionCode(),
        title.getIncludeInRankings());
  }

  @Test
  void sync_topUpCreatesOnlyMissingTitlesWhenSkipFlagSet() {
    ChampionshipSync sync =
        new ChampionshipSync(
            "true",
            titleService,
            wrestlerRepository,
            universeRepository,
            titleReignRepairService,
            new ObjectMapper(),
            SeedFingerprintManifest.of(Map.of()));
    when(titleService.count()).thenReturn(1L);
    stubUniverse();

    Title existing = new Title();
    existing.setName("ATW World");
    when(titleService.findByName("ATW World")).thenReturn(Optional.of(existing));
    when(titleService.createTitle(anyString(), anyString(), any(), any(), any(), any()))
        .thenAnswer(
            inv -> {
              Title t = new Title();
              t.setName(inv.getArgument(0));
              return t;
            });

    sync.sync();

    verify(titleService, never()).saveAll(List.of(existing));
    verify(titleService, never()).awardTitleTo(any(), any());
  }

  @Test
  void verifyMode_reconcilesCleanRowAndPreservesEditedRow() {
    ChampionshipSync verifySync =
        new ChampionshipSync(
            "verify",
            titleService,
            wrestlerRepository,
            universeRepository,
            titleReignRepairService,
            new ObjectMapper(),
            SeedFingerprintManifest.of(Map.of()));
    when(titleService.count()).thenReturn(2L);
    stubUniverse();

    // Clean: as an older release's seed write left it — effect script since fixed in the seed.
    Title clean = new Title();
    clean.setName("ATW World");
    clean.setEffectScript("stale effect script");
    clean.setExpansionCode("BASE_GAME");
    clean.setIncludeInRankings(true);
    clean.setSeedFingerprint(titleFingerprint(clean));

    // Edited: stamped at seed write, then hand-tuned.
    Title edited = new Title();
    edited.setName("ATW Women");
    edited.setEffectScript("user effect script");
    edited.setExpansionCode("BASE_GAME");
    edited.setIncludeInRankings(true);
    edited.setSeedFingerprint(titleFingerprint(edited));
    edited.setEffectScript("user effect script (changed after stamping)");

    when(titleService.findByName("ATW World")).thenReturn(Optional.of(clean));
    when(titleService.findByName("ATW Women")).thenReturn(Optional.of(edited));
    when(titleService.createTitle(anyString(), anyString(), any(), any(), any(), any()))
        .thenAnswer(
            inv -> {
              Title t = new Title();
              t.setName(inv.getArgument(0));
              return t;
            });

    verifySync.sync();

    verify(titleService, Mockito.atLeastOnce()).saveAll(any());
    assertEquals(
        "user effect script (changed after stamping)",
        edited.getEffectScript(),
        "edited title must be preserved");
    // Finding 6: description/tier are seed-owned (spec) — the clean row adopts the seed's
    // description even though the FULL update path never writes it.
    assertEquals(
        "The top title in All Time Wrestling.",
        clean.getDescription(),
        "clean row must adopt the seed description");
    assertEquals(
        "gainInitiative()",
        clean.getEffectScript(),
        "clean-fingerprint title must adopt the seed effect script");
  }

  @Test
  void verifyMode_adoptsLegacyRowWhenFieldsMatchSeed() {
    ChampionshipSync verifySync =
        new ChampionshipSync(
            "verify",
            titleService,
            wrestlerRepository,
            universeRepository,
            titleReignRepairService,
            new ObjectMapper(),
            SeedFingerprintManifest.of(Map.of()));
    when(titleService.count()).thenReturn(1L);
    stubUniverse();

    // Legacy: no stamp; fields equal what the seed write produces for ATW World (SINGLE/MALE,
    // gainInitiative() effect, BASE_GAME, null imageUrl, includeInRankings defaulting true).
    Title legacy = new Title();
    legacy.setName("ATW World");
    legacy.setDescription("The top title in All Time Wrestling.");
    legacy.setTier(WrestlerTier.MAIN_EVENTER);
    legacy.setChampionshipType(ChampionshipType.SINGLE);
    legacy.setEffectScript("gainInitiative()");
    legacy.setGender(Gender.MALE);
    legacy.setExpansionCode("BASE_GAME");
    legacy.setIncludeInRankings(true);
    when(titleService.findByName("ATW World")).thenReturn(Optional.of(legacy));
    when(titleService.createTitle(anyString(), anyString(), any(), any(), any(), any()))
        .thenAnswer(
            inv -> {
              Title t = new Title();
              t.setName(inv.getArgument(0));
              return t;
            });

    verifySync.sync();

    verify(titleService, Mockito.atLeastOnce()).saveAll(any());
    assertEquals(
        titleFingerprint(legacy),
        legacy.getSeedFingerprint(),
        "legacy title must be adopted (fingerprint stamped)");
  }

  @Test
  void fastPath_skipsReconcileWhenManifestMatchesStoredRows() {
    // Clean: stamped over its own (then-current) fields; the seed's effect script has since
    // changed. The manifest matches the stored stamp, so reconcile must be skipped.
    Title clean = new Title();
    clean.setName("ATW World");
    clean.setEffectScript("stale effect script");
    clean.setExpansionCode("BASE_GAME");
    clean.setIncludeInRankings(true);
    clean.setSeedFingerprint(titleFingerprint(clean));

    ChampionshipSync fastSync =
        new ChampionshipSync(
            "verify",
            titleService,
            wrestlerRepository,
            universeRepository,
            titleReignRepairService,
            new ObjectMapper(),
            SeedFingerprintManifest.of(
                Map.of("championship", Map.of("ATW World", clean.getSeedFingerprint()))));
    when(titleService.count()).thenReturn(1L);
    stubUniverse();
    when(titleService.findAllUnfiltered()).thenReturn(List.of(clean));
    when(titleService.findByName("ATW World")).thenReturn(Optional.of(clean));
    when(titleService.createTitle(anyString(), anyString(), any(), any(), any(), any()))
        .thenAnswer(
            inv -> {
              Title t = new Title();
              t.setName(inv.getArgument(0));
              return t;
            });

    fastSync.sync();

    // The stale effect script proves reconcile never ran (it would have adopted the seed's
    // "gainInitiative()").
    assertEquals(
        "stale effect script",
        clean.getEffectScript(),
        "fast path must skip reconcile; the stale seed-owned field survives");
  }
}
