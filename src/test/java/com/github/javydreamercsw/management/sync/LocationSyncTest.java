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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.world.Location;
import com.github.javydreamercsw.management.domain.world.LocationRepository;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for the locations.json seed sync, including the version-change detection pass. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LocationSyncTest {

  @Mock private LocationRepository locationRepository;
  @Mock private SeedUpdateReviewService seedReviewService;

  private static final SeedProvenanceManifest EMPTY_MANIFEST = SeedProvenanceManifest.empty();

  private static final SeedSyncSupport VERIFY_SUPPORT = new SeedSyncSupport("verify");

  private LocationSync sync;

  @BeforeEach
  void setUp() {
    sync = sync("false");
    when(locationRepository.count()).thenReturn(0L);
  }

  private LocationSync sync(String mode) {
    return new LocationSync(
        mode, locationRepository, new ObjectMapper(), seedReviewService, EMPTY_MANIFEST);
  }

  private static Location location(String name, String description, String imageUrl) {
    Location l = Location.builder().name(name).description(description).imageUrl(imageUrl).build();
    l.setCulturalTags(Set.of());
    return l;
  }

  @Test
  void versionChangeBootsParkCollisions() {
    // THREE-TIER pin, end-to-end through the wired pipeline. "New York City" is the tier-1 row
    // (description stale vs the stamp, seed current); "London" the tier-2 row (admin-tuned
    // description while the seed also changed it). Every other field of both rows matches the
    // incoming seed exactly — a row-level stamp reconstructs per-field provenance only when the
    // dirty row is a single edit away from the seed vector.
    Location tier1row = location("New York City", "stale description", null);
    tier1row.setCulturalTags(Set.of("metropolis", "usa", "historic", "big-apple", "east-coast"));
    // Clean row: the stamp hashes the row's CURRENT field vector exactly (the previous seed's
    // write), so the provenance reconstruction attests every field and the seed-changed
    // description classifies tier 1.
    tier1row.setSeedFingerprint(
        VERIFY_SUPPORT.fingerprint(
            "stale description", null, "big-apple\neast-coast\nhistoric\nmetropolis\nusa"));

    Location tier2row = location("London", "admin's description", null);
    tier2row.setCulturalTags(
        Set.of("uk", "british-strong-style", "europe", "historic", "vocal-crowd"));
    // Fail-safe row: the stamp matches NEITHER the current vector (admin-edited description) NOR
    // any single-field substitution (the tags set also differs from its hash slot) → per-field
    // provenance is unresolvable and every field whose current value differs from the incoming
    // seed parks. Only the description differs (tags match the seed) → exactly one collision.
    tier2row.setSeedFingerprint(
        VERIFY_SUPPORT.fingerprint("previous seed description", null, "unrelated\nhash"));

    when(locationRepository.count()).thenReturn(2L);
    when(locationRepository.findAll()).thenReturn(List.of(tier1row, tier2row));

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

    LocationSync detectionSync =
        new LocationSync("true", locationRepository, new ObjectMapper(), parking, EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    // Tier 1: the untouched description updated silently to the NEW seed value.
    assertEquals(
        "The 'Big Apple' and a historic epicenter for professional wrestling. From the bright"
            + " lights of Times Square to the grit of the boroughs, it's the ultimate stage.",
        tier1row.getDescription(),
        "tier 1: untouched field with a changed seed value must update silently");
    // Tier 2: the admin's description kept its value; the row parked the collision.
    assertEquals(
        "admin's description", tier2row.getDescription(), "tier 2: admin edit must keep its value");
    // Tier 3: the seed-unchanged tags set must never be touched (matches the seed → NO_OP).
    assertEquals(
        Set.of("uk", "british-strong-style", "europe", "historic", "vocal-crowd"),
        tier2row.getCulturalTags(),
        "tier 3: the seed-unchanged field must never be touched");
    Mockito.verify(parking)
        .parkFieldConflict(
            eq("location"),
            eq("location:london"),
            eq("London"),
            eq("description"),
            eq("admin's description"),
            eq(
                "A hub for the European wrestling scene, known for its rowdy, singing crowds and a"
                    + " mix of technical and hard-hitting styles."),
            anyString());
    assertEquals(
        Set.of("location:london|description"),
        parkedCoordinates,
        "exactly the one tier-2 collision parked; tier-1 and tier-3 fields never park");
  }

  @Test
  void normalBootBehaviorUnchanged() {
    // Normal boot pin: FULL-mode detection is inert (the FULL sync's own load does the writing),
    // and the PROTECT-mode gate returns early when the table is empty.
    LocationSync fullSync = sync("false");
    when(locationRepository.count()).thenReturn(2L);

    fullSync.detectVersionChangeCollisions(); // FULL mode → inert

    verifyNoInteractions(seedReviewService);
    verify(locationRepository, never()).findAll();
  }

  @Test
  void fullModeStampsFreshRows() {
    // FULL-mode load stamps every freshly seeded row so the manifest generator and the
    // version-change pass can match it against the committed manifest.
    when(locationRepository.count()).thenReturn(0L);
    when(locationRepository.findByName(anyString())).thenReturn(Optional.empty());

    sync.sync();

    ArgumentCaptor<List<Location>> captor = ArgumentCaptor.forClass(List.class);
    verify(locationRepository).saveAll(captor.capture());
    verify(locationRepository).flush();
    List<Location> saved = captor.getValue();
    assertEquals(62, saved.size(), "every fresh seed row must be saved");
    assertTrue(
        saved.stream().allMatch(l -> l.getSeedFingerprint() != null),
        "every fresh row must carry a stamp");
    assertNotNull(saved.getFirst().getSeedFingerprint());
  }
}
