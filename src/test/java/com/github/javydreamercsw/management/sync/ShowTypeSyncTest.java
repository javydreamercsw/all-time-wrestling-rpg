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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.show.type.ShowCategory;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.service.show.type.ShowTypeService;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for the show_types.json seed sync, including the version-change detection pass. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShowTypeSyncTest {

  @Mock private ShowTypeService showTypeService;
  @Mock private ShowTypeRepository showTypeRepository;
  @Mock private SeedUpdateReviewService seedReviewService;

  private static final SeedProvenanceManifest EMPTY_MANIFEST = SeedProvenanceManifest.empty();

  private static final SeedSyncSupport VERIFY_SUPPORT = new SeedSyncSupport("verify");

  private ShowTypeSync sync;

  @BeforeEach
  void setUp() {
    sync = sync("false");
    when(showTypeService.count()).thenReturn(0L);
  }

  private ShowTypeSync sync(String mode) {
    return new ShowTypeSync(
        mode,
        showTypeService,
        showTypeRepository,
        new ObjectMapper(),
        seedReviewService,
        EMPTY_MANIFEST);
  }

  @Test
  void sync_seedsCatalogAndStampsFreshRows() {
    ShowType ple =
        showType("Premium Live Event (PLE)", "Premium Live Event", 7, 3, ShowCategory.PLE);
    ShowType weekly = showType("Weekly", "Weekly Event", 5, 2, ShowCategory.WEEKLY);
    when(showTypeService.createOrUpdateShowType(
            anyString(),
            anyString(),
            org.mockito.ArgumentMatchers.anyInt(),
            org.mockito.ArgumentMatchers.anyInt(),
            any()))
        .thenAnswer(
            inv ->
                showType(
                    inv.getArgument(0),
                    inv.getArgument(1),
                    inv.getArgument(2),
                    inv.getArgument(3),
                    inv.getArgument(4)));
    when(showTypeService.save(any())).thenAnswer(inv -> inv.getArgument(0));

    sync.sync();

    // Both catalog rows stamped (the service's save leaves the stamp unset).
    verify(showTypeService, org.mockito.Mockito.atLeast(2))
        .save(argThat(st -> st.getSeedFingerprint() != null));
  }

  @Test
  void versionChangeBootsParkCollisions() {
    // THREE-TIER pin, end-to-end through the wired pipeline. Rows are stamped as the previous
    // release's seed write left them; the incoming seed (show_types.json) supplies the new values
    // through the sync's normal resource path. "Weekly" is the tier-1 row (description stale vs
    // the stamp, seed current); "Premium Live Event (PLE)" the tier-2 row (admin-tuned
    // expectedMatches); the admin's isActive toggle on the tier-2 row is tier 3 (not seed-owned).
    ShowType tier1row =
        showType("Weekly", "stale description from an older release", 5, 2, ShowCategory.WEEKLY);
    tier1row.setSeedFingerprint(
        VERIFY_SUPPORT.fingerprint("stale description from an older release", 5, 2, "WEEKLY"));

    ShowType tier2row =
        showType("Premium Live Event (PLE)", "Premium Live Event", 9, 3, ShowCategory.PLE);
    // The stamp carries the PREVIOUS release's seed value (4 matches) while the row shows the
    // admin's edit (9) and the incoming seed ships 7 → genuine tier-2 collision.
    tier2row.setSeedFingerprint(VERIFY_SUPPORT.fingerprint("Premium Live Event", 4, 3, "PLE"));
    tier2row.setExpectedMatches(9); // admin edit AFTER the stamp → dirty row
    tier2row.setActive(false); // admin toggle; not seed-owned — excluded from the observation set

    when(showTypeService.count()).thenReturn(2L);
    when(showTypeRepository.findAll()).thenReturn(List.of(tier1row, tier2row));

    java.util.Set<String> parkedCoordinates = new java.util.HashSet<>();
    SeedUpdateReviewService parking = org.mockito.Mockito.mock(SeedUpdateReviewService.class);
    org.mockito.Mockito.doAnswer(
            inv -> {
              parkedCoordinates.add(inv.getArgument(1) + "|" + inv.getArgument(3));
              return null;
            })
        .when(parking)
        .parkFieldConflict(
            anyString(), anyString(), anyString(), anyString(), any(), any(), anyString());

    ShowTypeSync detectionSync =
        new ShowTypeSync(
            "true",
            showTypeService,
            showTypeRepository,
            new ObjectMapper(),
            parking,
            EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    // Tier 1: the untouched description updated silently to the NEW seed value.
    assertEquals(
        "Weekly Event",
        tier1row.getDescription(),
        "tier 1: untouched field with a changed seed value must update silently");
    assertEquals(
        5, tier1row.getExpectedMatches(), "tier 1: untouched fields stay at their row values");
    // Tier 2: the admin's expectedMatches kept its value; the row parked the collision.
    assertEquals(9, tier2row.getExpectedMatches(), "tier 2: admin edit must keep its value");
    assertTrue(
        !tier2row.isActive(), "tier 3: the non-seed-owned admin toggle must never be touched");
    org.mockito.Mockito.verify(parking)
        .parkFieldConflict(
            eq("show_type"),
            eq("show_type:premium-live-event-(ple)"),
            eq("Premium Live Event (PLE)"),
            eq("expectedMatches"),
            eq("9"),
            eq("7"),
            anyString());
    assertEquals(
        java.util.Set.of("show_type:premium-live-event-(ple)|expectedMatches"),
        parkedCoordinates,
        "exactly the one tier-2 collision parked; tier-1 and tier-3 fields never park");
  }

  @Test
  void normalBootBehaviorUnchanged() {
    // Normal boot pin: FULL-mode detection is inert (the FULL sync's own load does the writing),
    // and the PROTECT-mode gate returns early when the table is empty.
    ShowTypeSync fullSync = sync("false");
    when(showTypeService.count()).thenReturn(2L);

    fullSync.detectVersionChangeCollisions(); // FULL mode → inert

    org.mockito.Mockito.verifyNoInteractions(seedReviewService);
    verify(showTypeRepository, never()).findAll();
  }

  private static ShowType showType(
      String name, String description, int expectedMatches, int expectedPromos, ShowCategory cat) {
    ShowType st = new ShowType();
    st.setName(name);
    st.setDescription(description);
    st.setExpectedMatches(expectedMatches);
    st.setExpectedPromos(expectedPromos);
    st.setCategory(cat);
    return st;
  }
}
