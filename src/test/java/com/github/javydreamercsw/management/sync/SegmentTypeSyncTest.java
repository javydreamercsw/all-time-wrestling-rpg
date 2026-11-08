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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.dto.SegmentTypeDTO;
import com.github.javydreamercsw.management.service.segment.type.SegmentTypeService;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SegmentTypeSyncTest {

  @Mock private SegmentTypeService segmentTypeService;

  @Mock private SeedUpdateReviewService seedReviewService;

  private static final SeedSyncSupport VERIFY_SUPPORT = new SeedSyncSupport("verify");

  private static final SeedProvenanceManifest EMPTY_MANIFEST = SeedProvenanceManifest.empty();

  private SegmentTypeSync sync;

  @BeforeEach
  void setUp() {
    sync = sync("false");
  }

  private SegmentTypeSync sync(String mode) {
    return new SegmentTypeSync(
        mode, segmentTypeService, new ObjectMapper(), seedReviewService, EMPTY_MANIFEST);
  }

  @Test
  void sync_passesEventOnlyThroughToService() {
    SegmentType rumble = new SegmentType();
    rumble.setName("Abu Dhabi Rumble");
    when(segmentTypeService.createOrUpdateSegmentType(
            anyString(), anyString(), anyString(), any(), anyString(), eq(true)))
        .thenReturn(rumble);
    SegmentType regular = new SegmentType();
    regular.setName("One on One");
    when(segmentTypeService.createOrUpdateSegmentType(
            anyString(), anyString(), anyString(), any(), anyString(), isNull()))
        .thenReturn(regular);
    when(segmentTypeService.count()).thenReturn(0L);

    sync.sync();

    // Abu Dhabi Rumble is the only seeded entry with event_only=true (ATW-0331).
    verify(segmentTypeService)
        .createOrUpdateSegmentType(
            eq("Abu Dhabi Rumble"),
            anyString(),
            eq("RUMBLE"),
            any(),
            eq("abu_dhabi_rumble"),
            eq(true));
  }

  @Test
  void sync_parsesSeedJsonWithoutErrors() {
    // The seed JSON must deserialize into SegmentTypeDTO including the event_only key.
    ObjectMapper mapper = new ObjectMapper();
    try (var is = getClass().getResourceAsStream("/segment_types.json")) {
      List<SegmentTypeDTO> dtos =
          mapper.readValue(
              is,
              mapper.getTypeFactory().constructCollectionType(List.class, SegmentTypeDTO.class));
      assertEquals(7, dtos.size(), "Seed JSON must contain 7 segment types");
      assertTrue(
          dtos.stream().filter(d -> Boolean.TRUE.equals(d.getEventOnly())).count() == 1,
          "Exactly one seeded segment type must be event_only (Abu Dhabi Rumble)");
    } catch (Exception e) {
      throw new IllegalStateException("Seed segment_types.json failed to parse", e);
    }
  }

  @Test
  void sync_skipsWhenNotEmptyAndSkipFlagSet() {
    SegmentTypeSync skipSync = sync("true");
    when(segmentTypeService.count()).thenReturn(5L);
    when(segmentTypeService.findAll())
        .thenReturn(
            List.of(
                segmentType("One on One"),
                segmentType("Tag Team"),
                segmentType("Promo"),
                segmentType("Handicap Match"),
                segmentType("Free-for-All")));

    skipSync.sync();

    // Top-up inserts only what's missing: Trios + Abu Dhabi Rumble; the 5 existing rows are
    // never passed to the service.
    verify(segmentTypeService)
        .createOrUpdateSegmentType(eq("Trios"), any(), any(), any(), any(), any());
    verify(segmentTypeService)
        .createOrUpdateSegmentType(eq("Abu Dhabi Rumble"), any(), any(), any(), any(), any());
    verify(segmentTypeService, Mockito.never())
        .createOrUpdateSegmentType(eq("One on One"), any(), any(), any(), any(), any());
    verify(segmentTypeService, Mockito.never())
        .createOrUpdateSegmentType(eq("Tag Team"), any(), any(), any(), any(), any());
  }

  @Test
  void sync_topUpInsertsNothingWhenCatalogComplete() {
    SegmentTypeSync skipSync = sync("true");
    when(segmentTypeService.count()).thenReturn(7L);
    when(segmentTypeService.findAll())
        .thenReturn(
            List.of(
                segmentType("One on One"),
                segmentType("Tag Team"),
                segmentType("Trios"),
                segmentType("Free-for-All"),
                segmentType("Abu Dhabi Rumble"),
                segmentType("Promo"),
                segmentType("Handicap Match")));

    skipSync.sync();

    verify(segmentTypeService, Mockito.never())
        .createOrUpdateSegmentType(
            anyString(), anyString(), anyString(), any(), anyString(), anyBoolean());
  }

  @Test
  void verifyMode_reconciledRowIsRestampedAndPersisted() {
    // Regression: the clean-row refresh previously saved through the service without persisting
    // a re-stamp — the DB kept the old fingerprint and the next pass froze the row.
    SegmentTypeSync verifySync = sync("verify");
    when(segmentTypeService.count()).thenReturn(1L);

    SegmentType clean = segmentType("Promo");
    clean.setDescription("stale description from an older release");
    clean.setGuideHash(null);
    clean.setSeedFingerprint(segmentTypeFingerprint(clean));
    when(segmentTypeService.findAll()).thenReturn(List.of(clean));
    when(segmentTypeService.createOrUpdateSegmentType(
            anyString(), anyString(), anyString(), any(), anyString(), any()))
        .thenAnswer(
            inv -> {
              SegmentType st = segmentType(inv.getArgument(0));
              st.setDescription(inv.getArgument(1));
              return st;
            });
    List<SegmentType> persisted = new ArrayList<>();
    when(segmentTypeService.saveSegmentType(any()))
        .thenAnswer(
            inv -> {
              persisted.add(inv.getArgument(0));
              return inv.getArgument(0);
            });

    verifySync.sync();

    // Top-up inserts 6 missing types first; the reconcile's refresh of Promo is the last persist.
    Assertions.assertFalse(persisted.isEmpty(), "a refresh must persist");
    SegmentType last = persisted.getLast();
    assertNotNull(last.getSeedFingerprint(), "persisted row must carry a fingerprint");
    assertEquals(
        "Non-wrestling segment for storyline development and character work",
        last.getDescription(),
        "the persisted row is the refreshed Promo");
  }

  /** Trios's segment_types.json description, verbatim (the tier-2 fixture aligns to it). */
  static final String SEED_TRIOS_DESCRIPTION =
      "Team-based wrestling match between two three-wrestler teams with tag-in/tag-out mechanics";

  private static SegmentType segmentType(String name) {
    SegmentType st = new SegmentType();
    st.setName(name);
    return st;
  }

  private static String segmentTypeFingerprint(SegmentType st) {
    return VERIFY_SUPPORT.fingerprint(
        st.getDescription(),
        st.getExpansionCode(),
        st.getGuideHash(),
        st.getCode(),
        st.isEventOnly());
  }

  @Test
  void verifyMode_reconcilesCleanRowAndPreservesEditedRow() {
    SegmentTypeSync verifySync = sync("verify");
    when(segmentTypeService.count()).thenReturn(2L);

    // Clean: "Promo" as an older release's seed write left it (guide-less), description since
    // fixed in the seed file.
    SegmentType clean = segmentType("Promo");
    clean.setDescription("stale description from an older release");
    clean.setGuideHash(null);
    clean.setSeedFingerprint(segmentTypeFingerprint(clean));

    // Edited: stamped at seed write, then hand-tuned.
    SegmentType edited = segmentType("Trios");
    edited.setDescription("user-tuned description");
    edited.setGuideHash(null);
    edited.setSeedFingerprint(segmentTypeFingerprint(edited));
    edited.setDescription("user-tuned description (changed after stamping)");

    when(segmentTypeService.findAll()).thenReturn(List.of(clean, edited));
    when(segmentTypeService.createOrUpdateSegmentType(
            anyString(), anyString(), anyString(), any(), anyString(), any()))
        .thenAnswer(inv -> segmentType(inv.getArgument(0)));

    verifySync.sync();

    verify(segmentTypeService)
        .createOrUpdateSegmentType(
            eq("Promo"),
            eq("Non-wrestling segment for storyline development and character work"),
            any(),
            any(),
            any(),
            any());
    verify(segmentTypeService, Mockito.never())
        .createOrUpdateSegmentType(eq("Trios"), any(), any(), any(), any(), any());
    assertEquals(
        "user-tuned description (changed after stamping)",
        edited.getDescription(),
        "edited segment type must be preserved");
  }

  @Test
  void verifyMode_adoptsLegacyRowWhenFieldsMatchSeed() {
    SegmentTypeSync verifySync = sync("verify");
    when(segmentTypeService.count()).thenReturn(1L);

    // Legacy: no stamp; fields equal what the seed write would produce for a guide-less entry.
    SegmentType legacy = segmentType("Promo");
    legacy.setDescription("Non-wrestling segment for storyline development and character work");
    legacy.setExpansionCode("BASE_GAME");
    legacy.setGuideHash(null);
    legacy.setCode("promo");
    when(segmentTypeService.findAll()).thenReturn(List.of(legacy));

    verifySync.sync();

    assertEquals(
        segmentTypeFingerprint(legacy),
        legacy.getSeedFingerprint(),
        "legacy row matching the seed must be adopted (fingerprint stamped, no-op refresh)");
  }

  // ── Version-change detection (seed-update review, PR 4 Step 4 three-tier pin) ──

  @Test
  void versionChangeBootsParkCollisions() {
    // THREE-TIER pin, end-to-end through the wired pipeline. Segment types are stamped as the
    // previous release's seed write left them; the incoming seed (segment_types.json) supplies
    // the new values through the sync's normal resource path. "Promo" is the tier-1 row
    // (description stale vs the stamp, seed current), "Trios" the tier-2 row (admin-tuned code
    // vs the stamp's seed value), "One on One" the tier-3 row (admin eventOnly edit the seed
    // doesn't touch).
    SegmentType tier1row = segmentType("Promo");
    tier1row.setDescription("stale description from an older release");
    tier1row.setExpansionCode("BASE_GAME");
    tier1row.setGuideHash(null);
    tier1row.setCode("promo");
    tier1row.setSeedFingerprint(segmentTypeFingerprint(tier1row));

    SegmentType tier2row = segmentType("Trios");
    tier2row.setDescription(SEED_TRIOS_DESCRIPTION); // matches the seed so only `code` collides
    tier2row.setExpansionCode("BASE_GAME");
    tier2row.setGuideHash(null);
    tier2row.setCode("trios_v1"); // the PREVIOUS release's seed code
    tier2row.setSeedFingerprint(segmentTypeFingerprint(tier2row));
    tier2row.setCode("admin_renamed_code"); // admin edit AFTER the stamp → dirty row

    SegmentType tier3row = segmentType("One on One");
    tier3row.setDescription("Traditional singles wrestling match between two competitors");
    tier3row.setExpansionCode("BASE_GAME");
    tier3row.setGuideHash(null);
    tier3row.setCode("one_on_one");
    tier3row.setSeedFingerprint(segmentTypeFingerprint(tier3row));
    tier3row.setEventOnly(true); // admin edit AFTER the stamp; the seed leaves eventOnly unset

    when(segmentTypeService.count()).thenReturn(3L);
    when(segmentTypeService.findAllForAdmin()).thenReturn(List.of(tier1row, tier2row, tier3row));

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

    SegmentTypeSync detectionSync =
        new SegmentTypeSync(
            "true", segmentTypeService, new ObjectMapper(), parking, EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    // Tier 1: the untouched description updated silently to the NEW seed value.
    assertEquals(
        "Non-wrestling segment for storyline development and character work",
        tier1row.getDescription(),
        "tier 1: untouched field with a changed seed value must update silently");
    // Tier 2: the admin's code kept its value; the row parked the collision.
    assertEquals(
        "admin_renamed_code", tier2row.getCode(), "tier 2: admin edit must keep its value");
    // Tier 3: the admin eventOnly edit preserved (the seed doesn't declare it → PRESERVE_EDIT).
    assertTrue(tier3row.isEventOnly(), "tier 3: admin edit must be preserved");
    assertEquals(
        Set.of(SegmentTypeSync.entityKeyFor("Trios") + "|code"),
        parkedCoordinates,
        "exactly the one tier-2 collision parked; tier-1 and tier-3 fields never park");
  }

  @Test
  void normalBootBehaviorUnchanged() {
    // Normal boot pin: FULL-mode detection is inert, and the pre-existing verify behavior still
    // holds with the new constructor dependencies.
    SegmentTypeSync fullSync = sync("false");
    when(segmentTypeService.count()).thenReturn(7L);

    fullSync.detectVersionChangeCollisions(); // FULL mode → inert

    Mockito.verifyNoInteractions(seedReviewService);
    verify(segmentTypeService, Mockito.never()).saveSegmentType(any());
  }
}
