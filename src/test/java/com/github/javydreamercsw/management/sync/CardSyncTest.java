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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.card.CardSetService;
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CardSyncTest {

  @Mock private CardService cardService;
  @Mock private CardSetService cardSetService;
  @Mock private ResourcePatternResolver resourcePatternResolver;
  @Mock private SeedUpdateReviewService seedReviewService;

  private static final SeedProvenanceManifest EMPTY_MANIFEST = SeedProvenanceManifest.empty();

  @Test
  void sync_topUpInsertsOnlyMissingCardsWhenSkipFlagSet() throws Exception {
    // Simulates a flag-protected install where only some cards of the seeded catalog exist.
    // Top-up must insert exactly the missing cards and never re-save an existing one.
    PathMatchingResourcePatternResolver real = new PathMatchingResourcePatternResolver();
    Resource[] resources = real.getResources("classpath*:cards/*.json");
    when(resourcePatternResolver.getResources(any())).thenReturn(resources);

    // Every ATW card exists except #100 (Overhead Suplex, last in the file).
    CardSet existingSet = new CardSet();
    existingSet.setCode("ATW");
    Card existing = new Card();
    existing.setId(1L);
    existing.setName("Shoulder Tackle");
    existing.setNumber(60);
    existing.setSet(existingSet);
    when(cardService.count()).thenReturn(1L);
    when(cardService.findAll()).thenReturn(List.of(existing));
    when(cardSetService.findBySetCode("ATW")).thenReturn(Optional.of(existingSet));

    CardSync skipSync =
        new CardSync(
            "true",
            cardService,
            cardSetService,
            resourcePatternResolver,
            new ObjectMapper(),
            seedReviewService,
            EMPTY_MANIFEST);
    skipSync.sync();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Card>> captor = ArgumentCaptor.forClass(List.class);
    verify(cardService, atLeastOnce()).saveAll(captor.capture());
    List<Card> saved =
        captor.getAllValues().stream().flatMap(List::stream).collect(Collectors.toList());
    assertFalse(saved.stream().anyMatch(c -> c == existing), "existing card must not be re-saved");
    assertFalse(
        saved.stream().anyMatch(c -> "ATW".equals(c.getSet().getCode()) && c.getNumber() == 60));
    // ATW seeds 58 cards; all except #60 must be inserted
    assertEquals(57, saved.size(), "ATW seeds 58 cards; 1 exists → 57 must be inserted");
  }

  @Test
  void sync_fullModeUpdatesExistingCard() throws Exception {
    PathMatchingResourcePatternResolver real = new PathMatchingResourcePatternResolver();
    Resource[] resources = real.getResources("classpath*:cards/*.json");
    when(resourcePatternResolver.getResources(any())).thenReturn(resources);

    CardSet existingSet = new CardSet();
    existingSet.setCode("ATW");
    Card existing = new Card();
    existing.setId(1L);
    existing.setName("stale name");
    existing.setNumber(60);
    existing.setSet(existingSet);
    when(cardService.findAll()).thenReturn(List.of(existing));
    when(cardSetService.findBySetCode("ATW")).thenReturn(Optional.of(existingSet));

    CardSync fullSync =
        new CardSync(
            "false",
            cardService,
            cardSetService,
            resourcePatternResolver,
            new ObjectMapper(),
            seedReviewService,
            EMPTY_MANIFEST);
    fullSync.sync();

    verify(cardService, atLeastOnce()).saveAll(argThat(list -> list.contains(existing)));
    assertEquals("Shoulder Tackle", existing.getName(), "name refreshed from seed in full mode");
  }

  @Test
  void verifyMode_reconcilesCleanRowAndPreservesEditedRow() throws Exception {
    SeedSyncSupport support = new SeedSyncSupport("verify");
    PathMatchingResourcePatternResolver real = new PathMatchingResourcePatternResolver();
    Resource[] resources = real.getResources("classpath*:cards/*.json");
    when(resourcePatternResolver.getResources(any())).thenReturn(resources);

    CardSet atw = new CardSet();
    atw.setCode("ATW");
    when(cardService.count()).thenReturn(2L);
    when(cardSetService.findBySetCode("ATW")).thenReturn(Optional.of(atw));

    // Clean: ATW #60 Shoulder Tackle as an older release's seed write left it (its fields,
    // including the since-fixed name, hash to the stored fingerprint).
    Card clean = new Card();
    clean.setId(1L);
    clean.setName("stale name");
    clean.setNumber(60);
    clean.setSet(atw);
    clean.setDamage(1);
    clean.setStamina(1);
    clean.setTarget(2);
    clean.setMomentum(1);
    clean.setType("Strike");
    clean.setSignature(false);
    clean.setFinisher(false);
    clean.setTaunt(false);
    clean.setRecover(false);
    clean.setPin(false);
    clean.setSeedFingerprint(
        support.fingerprint("stale name", 1, 1, 2, 1, "Strike", false, false, false, false, false));

    // Edited: ATW #59 stamped at seed write, then hand-tuned (fields no longer hash to the stamp).
    Card edited = new Card();
    edited.setId(2L);
    edited.setName("hand-tuned");
    edited.setNumber(59);
    edited.setSet(atw);
    edited.setDamage(0);
    edited.setStamina(0);
    edited.setTarget(0);
    edited.setMomentum(0);
    edited.setType("Special");
    edited.setSignature(false);
    edited.setFinisher(false);
    edited.setTaunt(false);
    edited.setRecover(false);
    edited.setPin(false);
    edited.setSeedFingerprint(
        support.fingerprint("seed name", 9, 9, 9, 9, "Old", true, true, false, false, false));
    when(cardService.findAll()).thenReturn(List.of(clean, edited));

    CardSync verifySync =
        new CardSync(
            "verify",
            cardService,
            cardSetService,
            resourcePatternResolver,
            new ObjectMapper(),
            seedReviewService,
            EMPTY_MANIFEST);
    verifySync.sync();

    assertEquals(
        "Shoulder Tackle",
        clean.getName(),
        "clean-fingerprint card must be reconciled to the seed name");
    assertEquals("hand-tuned", edited.getName(), "edited card must be preserved");
  }

  // ── Version-change detection (seed-update review, PR 4 Step 4 three-tier pin) ──

  private static final SeedSyncSupport VERIFY_SUPPORT = new SeedSyncSupport("verify");

  /** Builds an ATW card mirroring the seed payload, with seed-owned fields set. */
  private Card seededCard(int number, String name, String type) {
    CardSet atw = new CardSet();
    atw.setCode("ATW");
    Card card = new Card();
    card.setName(name);
    card.setNumber(number);
    card.setSet(atw);
    card.setDamage(1);
    card.setStamina(1);
    card.setTarget(2);
    card.setMomentum(1);
    card.setType(type);
    card.setSignature(false);
    card.setFinisher(false);
    card.setTaunt(false);
    card.setRecover(false);
    card.setPin(false);
    return card;
  }

  private String cardFingerprint(Card card) {
    return VERIFY_SUPPORT.fingerprint(
        card.getName(),
        card.getDamage(),
        card.getStamina(),
        card.getTarget(),
        card.getMomentum(),
        card.getType(),
        card.getSignature(),
        card.getFinisher(),
        card.getTaunt(),
        card.getRecover(),
        card.getPin());
  }

  @Test
  void versionChangeBootsParkCollisions() throws Exception {
    // THREE-TIER pin, end-to-end through the wired pipeline (Task 5's classifier contract,
    // exercised via CardSync.detectVersionChangeCollisions + a recording parking sink):
    //   tier 1 — untouched field (name), seed changed → updated silently + re-stamped
    //   tier 2 — admin-edited field (momentum), seed changed → parked as PENDING decision, DB
    //            value unchanged
    //   tier 3 — seed unchanged → no-op whether or not the admin edited it (type edit,
    //            PRESERVE_EDIT — nothing parked, nothing written)
    //
    // The seed resource is a fixture copy of ATW.json with card #60's name AND #59's momentum
    // changed (the version upgrade's seed delta). The DB rows are stamped as the PREVIOUS
    // release's seed write left them, then the admin hand-edits #60's type (tier 3) and #59's
    // momentum (tier 2).
    Card tier1row = seededCard(60, "stale seed name", "Strike"); // seed renamed since
    tier1row.setSeedFingerprint(cardFingerprint(tier1row));
    tier1row.setType("Suplex"); // tier 3: admin edit AFTER the stamp, seed unchanged ("Strike")

    Card tier2row = seededCard(59, "Gori Special", "Throw");
    // Align the row's non-edited fields with the incoming seed (damage 3, stamina 3, target 4) so
    // the ONLY field differing from the incoming seed is momentum — the fail-safe parking path
    // parks every differing field, and the pin asserts exactly one.
    tier2row.setDamage(3);
    tier2row.setStamina(3);
    tier2row.setTarget(4);
    tier2row.setMomentum(4); // the seed's value
    tier2row.setSeedFingerprint(
        // The stamp carries the PREVIOUS release's momentum (2) while the row shows the admin's
        // edit (7) and the incoming seed ships 9 → genuine tier-2 collision.
        VERIFY_SUPPORT.fingerprint(
            tier2row.getName(),
            tier2row.getDamage(),
            tier2row.getStamina(),
            tier2row.getTarget(),
            2,
            tier2row.getType(),
            false,
            false,
            false,
            false,
            false));
    tier2row.setMomentum(7); // tier 2: admin edit AFTER the stamp

    when(cardService.count()).thenReturn(2L);
    when(cardService.findAll()).thenReturn(List.of(tier1row, tier2row));
    CardSet atw = new CardSet();
    atw.setCode("ATW");
    when(cardSetService.findBySetCode("ATW")).thenReturn(Optional.of(atw));

    // Seed fixture: #60's name changed (tier 1), #59's momentum bumped 4 → 9 (tier 2 target).
    com.fasterxml.jackson.databind.node.ArrayNode array =
        new com.fasterxml.jackson.databind.ObjectMapper().createArrayNode();
    com.fasterxml.jackson.databind.node.ObjectNode n60 = array.addObject();
    n60.put("name", "Shoulder Tackle Renamed");
    n60.put("type", "Strike");
    n60.put("damage", 1);
    n60.put("stamina", 1);
    n60.put("target", 2);
    n60.put("momentum", 1);
    n60.put("signature", false);
    n60.put("finisher", false);
    n60.put("taunt", false);
    n60.put("recover", false);
    n60.put("pin", false);
    n60.put("number", 60);
    n60.put("set", "ATW");
    com.fasterxml.jackson.databind.node.ObjectNode n59 = array.addObject();
    n59.put("name", "Gori Special");
    n59.put("type", "Throw");
    n59.put("damage", 3);
    n59.put("stamina", 3);
    n59.put("target", 4);
    n59.put("momentum", 9);
    n59.put("signature", false);
    n59.put("finisher", false);
    n59.put("taunt", false);
    n59.put("recover", false);
    n59.put("pin", false);
    n59.put("number", 59);
    n59.put("set", "ATW");
    Resource[] seedResources = {
      new org.springframework.core.io.ByteArrayResource(
          array.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))
    };
    when(resourcePatternResolver.getResources(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(seedResources);

    java.util.Set<String> parkedCoordinates = new java.util.HashSet<>();
    SeedUpdateReviewService parking = org.mockito.Mockito.mock(SeedUpdateReviewService.class);
    org.mockito.Mockito.doAnswer(
            inv -> {
              parkedCoordinates.add(inv.getArgument(1) + "|" + inv.getArgument(3));
              return null;
            })
        .when(parking)
        .parkFieldConflict(
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.anyString());

    CardSync detectionSync =
        new CardSync(
            "true",
            cardService,
            cardSetService,
            resourcePatternResolver,
            new ObjectMapper(),
            parking,
            EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    // Tier 1: the untouched name updated silently to the NEW seed value.
    assertEquals(
        "Shoulder Tackle Renamed",
        tier1row.getName(),
        "tier 1: untouched field with a changed seed value must update silently");
    // Tier 3: the seed-unchanged type edit was preserved with no parking.
    assertEquals(
        "Suplex",
        tier1row.getType(),
        "tier 3: admin edit on an unchanged seed field must be preserved silently");
    // Tier 2: the admin-edited momentum kept its value; the row parked the momentum collision.
    assertEquals(7, tier2row.getMomentum(), "tier 2: admin-edited field must keep its value");
    org.mockito.Mockito.verify(parking)
        .parkFieldConflict(
            org.mockito.ArgumentMatchers.eq("card"),
            org.mockito.ArgumentMatchers.eq("card:atw#59"),
            org.mockito.ArgumentMatchers.eq("Gori Special"),
            org.mockito.ArgumentMatchers.eq("momentum"),
            org.mockito.ArgumentMatchers.eq("7"),
            org.mockito.ArgumentMatchers.eq("9"),
            org.mockito.ArgumentMatchers.anyString());
    assertEquals(
        java.util.Set.of("card:atw#59|momentum"),
        parkedCoordinates,
        "exactly the one tier-2 collision parked; tier-1 fields never park");
  }

  @Test
  void normalBootBehaviorUnchanged() throws Exception {
    // Normal boot pin: FULL-mode detection is inert (nothing to collide — FULL applies the seed
    // everywhere), and the detection pass never parks anything when the mode is FULL.
    PathMatchingResourcePatternResolver real = new PathMatchingResourcePatternResolver();
    Resource[] resources = real.getResources("classpath*:cards/*.json");
    when(resourcePatternResolver.getResources(any())).thenReturn(resources);
    Card existing = seededCard(60, "Shoulder Tackle", "Strike");
    when(cardService.count()).thenReturn(1L);
    when(cardService.findAll()).thenReturn(List.of(existing));
    CardSet atw = new CardSet();
    atw.setCode("ATW");
    when(cardSetService.findBySetCode("ATW")).thenReturn(Optional.of(atw));

    SeedUpdateReviewService parking = org.mockito.Mockito.mock(SeedUpdateReviewService.class);
    CardSync fullModeSync =
        new CardSync(
            "false",
            cardService,
            cardSetService,
            resourcePatternResolver,
            new ObjectMapper(),
            parking,
            EMPTY_MANIFEST);
    fullModeSync.detectVersionChangeCollisions();
    org.mockito.Mockito.verifyNoInteractions(parking);
    assertEquals(
        "Shoulder Tackle",
        existing.getName(),
        "FULL-mode detection must not touch rows (the FULL sync's own load does that)");
  }
}
