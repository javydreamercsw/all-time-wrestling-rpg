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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.campaign.StatusCard;
import com.github.javydreamercsw.management.domain.campaign.StatusCardRepository;
import com.github.javydreamercsw.management.service.campaign.StatusCardService;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Unit tests for the status_cards.json seed sync, including the version-change detection pass. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StatusCardSyncTest {

  @Mock private StatusCardService statusCardService;
  @Mock private StatusCardRepository statusCardRepository;
  @Mock private SeedUpdateReviewService seedReviewService;

  private static final SeedProvenanceManifest EMPTY_MANIFEST = SeedProvenanceManifest.empty();

  private static final SeedSyncSupport VERIFY_SUPPORT = new SeedSyncSupport("verify");

  private StatusCardSync sync;

  @BeforeEach
  void setUp() {
    sync = sync("false");
    when(statusCardService.findAll()).thenReturn(List.of());
    when(statusCardRepository.count()).thenReturn(0L);
  }

  private StatusCardSync sync(String mode) {
    return new StatusCardSync(
        mode,
        statusCardService,
        statusCardRepository,
        new ObjectMapper(),
        seedReviewService,
        EMPTY_MANIFEST);
  }

  @Test
  void sync_seedsCatalogAndStampsFreshRows() {
    when(statusCardService.findAll()).thenReturn(List.of());
    when(statusCardService.createOrUpdateCard(
            anyString(),
            any(),
            any(),
            any(),
            ArgumentMatchers.anyBoolean(),
            any(),
            any(),
            any(),
            any(),
            any()))
        .thenAnswer(
            inv -> {
              StatusCard card = new StatusCard();
              card.setKey(inv.getArgument(0));
              return card;
            });
    when(statusCardService.save(any())).thenAnswer(inv -> inv.getArgument(0));

    sync.sync();

    verify(statusCardService, atLeastOnce())
        .save(argThat(card -> card.getSeedFingerprint() != null));
  }

  // ── Version-change detection (seed-update review, PR 4 Step 4 three-tier pin) ──

  @Test
  void versionChangeBootsParkCollisions() {
    // THREE-TIER pin, end-to-end through the wired pipeline. Rows are stamped as the previous
    // release's seed write left them; the incoming seed (status_cards.json) supplies the new
    // values through the sync's normal resource path. "status_champion" is the tier-1 row (its
    // description stale vs the stamp, seed current); "status_injury" the tier-2 row (an
    // admin-tuned level1Effect vs the stamp's seed value); "status_custom" has no seed entry.
    StatusCard tier1row = seedCard("status_draw");
    tier1row.setDescription("stale description from an older release");
    tier1row.setSeedFingerprint(fingerprintOf(tier1row));

    // Tier-2 row: the stamp holds the PREVIOUS seed's level1Effect while the row carries the
    // admin's hand-tuned replacement.
    StatusCard tier2fixed = seedCard("status_lost_confidence");
    tier2fixed.setLevel1Effect("admin tuned effect"); // admin edit AFTER the stamp → dirty row

    StatusCard customRow = new StatusCard();
    customRow.setKey("status_custom");
    customRow.setLevel1Name("Custom");
    customRow.setDescription("user-created card");
    customRow.setPositive(false);

    when(statusCardRepository.count()).thenReturn(3L);
    when(statusCardRepository.findByKey("status_draw")).thenReturn(Optional.of(tier1row));
    when(statusCardRepository.findByKey("status_lost_confidence"))
        .thenReturn(Optional.of(tier2fixed));
    when(statusCardRepository.findByKey(
            argThat(k -> !"status_draw".equals(k) && !"status_lost_confidence".equals(k))))
        .thenReturn(Optional.empty());

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

    StatusCardSync detectionSync =
        new StatusCardSync(
            "true",
            statusCardService,
            statusCardRepository,
            new ObjectMapper(),
            parking,
            EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    // Tier 1: the untouched description updated silently to the NEW seed value.
    assertEquals(
        "Represents the wrestler's ability to draw a crowd.",
        tier1row.getDescription(),
        "tier 1: untouched field with a changed seed value must update silently");
    // Tier 2: the admin's effect kept its value; the row parked the collision.
    assertEquals(
        "admin tuned effect",
        tier2fixed.getLevel1Effect(),
        "tier 2: admin edit must keep its value");
    assertEquals(1, parkedCoordinates.size(), "exactly one tier-2 collision parked");
    assertEquals(
        StatusCardSync.entityKeyFor("status_lost_confidence") + "|level1Effect",
        parkedCoordinates.iterator().next(),
        "the tier-2 park must be keyed by the entity key and the level1Effect field");
  }

  @Test
  void normalBootBehaviorUnchanged() {
    // Normal boot pin: FULL-mode detection is inert (the FULL sync's own load does the writing).
    StatusCardSync fullSync = sync("false");
    when(statusCardRepository.count()).thenReturn(4L);

    fullSync.detectVersionChangeCollisions(); // FULL mode → inert

    Mockito.verifyNoInteractions(seedReviewService);
    verify(statusCardRepository, never()).findByKey(any());
  }

  private StatusCard seedCard(String key) {
    StatusCard card = new StatusCard();
    card.setKey(key);
    if ("status_draw".equals(key)) {
      card.setLevel1Name("Draw");
      card.setLevel2Name("Main Eventer");
      card.setDescription("Represents the wrestler's ability to draw a crowd.");
      card.setPositive(true);
      card.setLevel1Effect("momentum: +4");
      card.setLevel2Effect("momentum: +4, mainEvent: true");
      card.setFlipUpCondition("momentum >= 5");
      card.setFlipDownCondition("momentum < 3");
      card.setDiscardCondition("false");
    } else { // status_lost_confidence
      card.setLevel1Name("Lost Confidence");
      card.setLevel2Name("Humiliated");
      card.setDescription("Represents a temporary loss of self-assurance.");
      card.setPositive(false);
      card.setLevel1Effect("handSize: -2");
      card.setLevel2Effect("momentum: -7, loseFocusOnLowMomentum: true");
      card.setFlipUpCondition("loss == true");
      card.setFlipDownCondition("momentum >= 5");
      card.setDiscardCondition("false");
    }
    return card;
  }

  private String fingerprintOf(StatusCard card) {
    return VERIFY_SUPPORT.fingerprint(
        card.getLevel1Name(),
        card.getLevel2Name(),
        card.getDescription(),
        card.isPositive(),
        card.getLevel1Effect(),
        card.getLevel2Effect(),
        card.getFlipUpCondition(),
        card.getFlipDownCondition(),
        card.getDiscardCondition());
  }
}
