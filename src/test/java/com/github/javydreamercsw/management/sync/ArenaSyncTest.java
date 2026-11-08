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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.world.Arena;
import com.github.javydreamercsw.management.domain.world.ArenaRepository;
import com.github.javydreamercsw.management.domain.world.Location;
import com.github.javydreamercsw.management.domain.world.LocationRepository;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for the arenas.json seed sync, including the version-change detection pass. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ArenaSyncTest {

  @Mock private ArenaRepository arenaRepository;
  @Mock private LocationRepository locationRepository;
  @Mock private SeedUpdateReviewService seedReviewService;

  private static final SeedProvenanceManifest EMPTY_MANIFEST = SeedProvenanceManifest.empty();

  private static final SeedSyncSupport VERIFY_SUPPORT = new SeedSyncSupport("verify");

  private ArenaSync sync;

  @BeforeEach
  void setUp() {
    sync = sync("false");
    when(arenaRepository.count()).thenReturn(0L);
  }

  private ArenaSync sync(String mode) {
    return new ArenaSync(
        mode,
        arenaRepository,
        locationRepository,
        new ObjectMapper(),
        seedReviewService,
        EMPTY_MANIFEST);
  }

  private static Location location(String name) {
    Location l = Location.builder().name(name).build();
    l.setCulturalTags(Set.of());
    return l;
  }

  private static Arena arena(
      String name,
      String description,
      Location location,
      int capacity,
      Arena.AlignmentBias bias,
      Set<String> traits) {
    Arena a =
        Arena.builder()
            .name(name)
            .description(description)
            .location(location)
            .capacity(capacity)
            .alignmentBias(bias)
            .build();
    a.setEnvironmentalTraits(traits);
    return a;
  }

  @Test
  void versionChangeBootsParkCollisions() {
    // THREE-TIER pin, end-to-end through the wired pipeline. "Madison Square Garden" is the tier-1
    // row (description stale vs the stamp, seed current); "Tokyo Dome" the tier-2 row (admin-tuned
    // capacity while the seed also changed it); the admin's environmentalTraits edit on the tier-2
    // row (seed unchanged there) is tier 3.
    var nyc = location("New York City");
    var neoTokyo = location("Neo-Tokyo");
    var tier1row =
        arena(
            "Madison Square Garden",
            "stale description",
            nyc,
            18500,
            Arena.AlignmentBias.NEUTRAL,
            Set.of("historic-prestige", "intense-spotlight"));
    tier1row.setSeedFingerprint(
        VERIFY_SUPPORT.fingerprint(
            "stale description",
            "New York City",
            18500,
            "NEUTRAL",
            null,
            "historic-prestige\nintense-spotlight"));

    Arena tier2row =
        arena(
            "Tokyo Dome",
            "The massive 'Big Egg'. Host to the largest spectacles in Japanese wrestling history,"
                + " where the atmosphere is both respectful and electric.",
            neoTokyo,
            40001,
            Arena.AlignmentBias.FACE_FAVORABLE,
            Set.of("massive-scale", "respectful-silence"));
    // The stamp carries the PREVIOUS release's seed capacity (40000) while the row shows the
    // admin's tweak (40001) and the incoming seed ships a NEW value (45000): the seed changed the
    // very field the admin edited, so neither the clean nor the single-edit reconstruction matches
    // (substituting the seed's 45000 for that field does not reproduce the 40000 stamp) →
    // fail-safe, and every field whose current value differs from the incoming seed parks. Every
    // other field matches the seed exactly → exactly the capacity collision parks.
    tier2row.setSeedFingerprint(
        VERIFY_SUPPORT.fingerprint(
            "The massive 'Big Egg'. Host to the largest spectacles in Japanese wrestling history,"
                + " where the atmosphere is both respectful and electric.",
            "Neo-Tokyo",
            40000,
            "FACE_FAVORABLE",
            null,
            "massive-scale\nrespectful-silence"));

    when(arenaRepository.count()).thenReturn(2L);
    when(arenaRepository.findAllWithLocation()).thenReturn(List.of(tier1row, tier2row));
    when(locationRepository.findByName(anyString()))
        .thenAnswer(inv -> Optional.of(location(inv.getArgument(0))));

    Set<String> parkedCoordinates = new HashSet<>();
    SeedUpdateReviewService parking = Mockito.mock(SeedUpdateReviewService.class);
    Mockito.doAnswer(
            inv -> {
              parkedCoordinates.add(inv.getArgument(1) + "|" + inv.getArgument(3));
              return null;
            })
        .when(parking)
        .parkFieldConflict(
            anyString(), anyString(), anyString(), anyString(), any(), any(), anyString());

    ArenaSync detectionSync =
        new ArenaSync(
            "true",
            arenaRepository,
            locationRepository,
            new ObjectMapper(),
            parking,
            EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    // Tier 1: the untouched description updated silently to the NEW seed value.
    assertEquals(
        "The 'World's Most Famous Arena' and the spiritual home of sports entertainment. Every"
            + " legend has walked these hallowed halls.",
        tier1row.getDescription(),
        "tier 1: untouched field with a changed seed value must update silently");
    // Tier 2: the admin's capacity kept its value; the row parked the collision.
    assertEquals(40001, tier2row.getCapacity(), "tier 2: admin edit must keep its value");
    // Tier 3: the seed-matching trait set must never be touched (NO_OP).
    assertEquals(
        Set.of("massive-scale", "respectful-silence"),
        tier2row.getEnvironmentalTraits(),
        "tier 3: the seed-matching field must never be touched");
    Mockito.verify(parking)
        .parkFieldConflict(
            eq("arena"),
            eq("arena:tokyo-dome"),
            eq("Tokyo Dome"),
            eq("capacity"),
            eq("40001"),
            eq("45000"),
            anyString());
    assertEquals(
        Set.of("arena:tokyo-dome|capacity"),
        parkedCoordinates,
        "exactly the one tier-2 collision parked; tier-1 and tier-3 fields never park");
  }

  @Test
  void normalBootBehaviorUnchanged() {
    // Normal boot pin: FULL-mode detection is inert (the FULL sync's own load does the writing),
    // and the PROTECT-mode gate returns early when the table is empty.
    ArenaSync fullSync = sync("false");
    when(arenaRepository.count()).thenReturn(2L);

    fullSync.detectVersionChangeCollisions(); // FULL mode → inert

    verifyNoInteractions(seedReviewService);
    verify(arenaRepository, never()).findAllWithLocation();
  }

  @Test
  void versionChange_silentUpdatesCoverEveryWriteArm() {
    // Tier-1 write arms the three-tier pin never reached, driven against the real arenas.json:
    // "Madison Square Garden" drifted on location/capacity/alignmentBias/imageUrl/traits (each a
    // SILENT_UPDATE against its clean stamp); "Tokyo Dome" carries a null capacity (the
    // observation ternary's null side) and a location whose seed lookup resolves to nothing (the
    // not-found warn arm — the reference is kept).
    var ghostCity = location("Ghost City");
    var tier1row =
        arena(
            "Madison Square Garden",
            "The 'World's Most Famous Arena' and the spiritual home of sports entertainment. Every"
                + " legend has walked these hallowed halls.",
            ghostCity,
            999,
            Arena.AlignmentBias.HEEL_FAVORABLE,
            Set.of("old-trait"));
    tier1row.setImageUrl("http://example.com/old-msg.png");
    tier1row.setSeedFingerprint(
        VERIFY_SUPPORT.fingerprint(
            "The 'World's Most Famous Arena' and the spiritual home of sports entertainment. Every"
                + " legend has walked these hallowed halls.",
            "Ghost City",
            999,
            "HEEL_FAVORABLE",
            "http://example.com/old-msg.png",
            "old-trait"));

    var egg =
        Arena.builder()
            .name("Tokyo Dome")
            .description(
                "The massive 'Big Egg'. Host to the largest spectacles in Japanese wrestling"
                    + " history, where the atmosphere is both respectful and electric.")
            .location(location("Ghost City 2"))
            .capacity(null)
            .alignmentBias(Arena.AlignmentBias.FACE_FAVORABLE)
            .build();
    egg.setEnvironmentalTraits(Set.of("massive-scale", "respectful-silence"));
    egg.setSeedFingerprint(
        VERIFY_SUPPORT.fingerprint(
            "The massive 'Big Egg'. Host to the largest spectacles in Japanese wrestling history,"
                + " where the atmosphere is both respectful and electric.",
            "Ghost City 2",
            null,
            "FACE_FAVORABLE",
            null,
            "massive-scale\nrespectful-silence"));

    when(arenaRepository.count()).thenReturn(2L);
    when(arenaRepository.findAllWithLocation()).thenReturn(List.of(tier1row, egg));
    when(locationRepository.findByName(anyString()))
        .thenAnswer(
            inv ->
                "New York City".equals(inv.getArgument(0))
                    ? Optional.of(location("New York City"))
                    : Optional.empty());

    SeedUpdateReviewService parking = Mockito.mock(SeedUpdateReviewService.class);
    ArenaSync detectionSync =
        new ArenaSync(
            "true",
            arenaRepository,
            locationRepository,
            new ObjectMapper(),
            parking,
            EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    // MSG: every silent arm wrote the seed value.
    assertEquals("New York City", tier1row.getLocation().getName(), "location arm resolved");
    assertEquals(18500, tier1row.getCapacity(), "capacity arm wrote the seed value");
    assertEquals(Arena.AlignmentBias.NEUTRAL, tier1row.getAlignmentBias(), "bias arm wrote");
    assertNull(tier1row.getImageUrl(), "imageUrl arm writes unconditionally (no guard)");
    assertEquals(
        Set.of("historic-prestige", "intense-spotlight"),
        tier1row.getEnvironmentalTraits(),
        "traits arm wrote the seed set");
    // Tokyo Dome: null capacity written through the guard; unresolvable location kept (warn arm).
    assertEquals(45000, egg.getCapacity(), "null capacity written from the seed");
    assertEquals("Ghost City 2", egg.getLocation().getName(), "unresolvable location kept");
    Mockito.verify(parking, Mockito.never())
        .parkFieldConflict(
            anyString(), anyString(), anyString(), anyString(), any(), any(), anyString());
  }

  @Test
  void versionChange_stampOnlyRowReStampsFromManifest() {
    // The stamp-only-save branch: a clean row whose seed is unchanged adopts the manifest hash
    // through the silent-apply-free save path.
    var msg =
        arena(
            "Madison Square Garden",
            "The 'World's Most Famous Arena' and the spiritual home of sports entertainment. Every"
                + " legend has walked these hallowed halls.",
            location("New York City"),
            18500,
            Arena.AlignmentBias.NEUTRAL,
            Set.of("historic-prestige", "intense-spotlight"));
    msg.setSeedFingerprint(
        VERIFY_SUPPORT.fingerprint(
            "The 'World's Most Famous Arena' and the spiritual home of sports entertainment. Every"
                + " legend has walked these hallowed halls.",
            "New York City",
            18500,
            "NEUTRAL",
            null,
            "historic-prestige\nintense-spotlight"));

    when(arenaRepository.count()).thenReturn(1L);
    when(arenaRepository.findAllWithLocation()).thenReturn(List.of(msg));
    SeedProvenanceManifest manifest =
        SeedProvenanceManifest.of(
            Map.of("arena", Map.of("Madison Square Garden", "manifest-hash-msg")));

    ArenaSync detectionSync =
        new ArenaSync(
            "true",
            arenaRepository,
            locationRepository,
            new ObjectMapper(),
            Mockito.mock(SeedUpdateReviewService.class),
            manifest);
    detectionSync.detectVersionChangeCollisions();

    assertEquals(
        "manifest-hash-msg",
        msg.getSeedFingerprint(),
        "a clean unchanged row must adopt the manifest hash (stamp-only save)");
    ArgumentCaptor<List<Arena>> captor = ArgumentCaptor.forClass(List.class);
    verify(arenaRepository).saveAll(captor.capture());
    assertTrue(captor.getValue().contains(msg), "the stamp-only row must be persisted");
  }

  @Test
  void versionChangeDetection_emptyTableIsInert() {
    // Empty-DB guard arm: PROTECT mode with no rows returns before any lookup.
    when(arenaRepository.count()).thenReturn(0L);
    ArenaSync detectionSync =
        new ArenaSync(
            "true",
            arenaRepository,
            locationRepository,
            new ObjectMapper(),
            Mockito.mock(SeedUpdateReviewService.class),
            EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();
    verify(arenaRepository, never()).findAllWithLocation();
  }

  @Test
  void versionChangeDetection_toleratesBrokenSeedResource() {
    // A seed file that fails to deserialize is logged and skipped — the pass never propagates.
    when(arenaRepository.count()).thenReturn(1L);
    when(arenaRepository.findAllWithLocation()).thenReturn(List.of());
    ObjectMapper brokenMapper =
        new ObjectMapper() {
          @Override
          public <T> T readValue(InputStream src, TypeReference<T> typeRef) throws IOException {
            throw new IOException("boom");
          }
        };
    ArenaSync detectionSync =
        new ArenaSync(
            "true",
            arenaRepository,
            locationRepository,
            brokenMapper,
            Mockito.mock(SeedUpdateReviewService.class),
            EMPTY_MANIFEST);
    Assertions.assertDoesNotThrow(detectionSync::detectVersionChangeCollisions);
    verify(arenaRepository, never()).saveAll(any());
  }

  @Test
  void fullModeStampsFreshRows() {
    // FULL-mode load stamps every freshly seeded row so the manifest generator and the
    // version-change pass can match it against the committed manifest.
    when(arenaRepository.count()).thenReturn(0L);
    when(arenaRepository.findAllWithLocation()).thenReturn(List.of());
    when(locationRepository.findByName(anyString()))
        .thenAnswer(inv -> Optional.of(location(inv.getArgument(0))));

    sync.sync();

    ArgumentCaptor<List<Arena>> captor = ArgumentCaptor.forClass(List.class);
    verify(arenaRepository).saveAll(captor.capture());
    List<Arena> saved = captor.getValue();
    assertEquals(83, saved.size(), "every fresh seed row must be saved");
    assertTrue(
        saved.stream().allMatch(a -> a.getSeedFingerprint() != null),
        "every fresh row must carry a stamp");
    assertNotNull(saved.getFirst().getSeedFingerprint());
  }
}
