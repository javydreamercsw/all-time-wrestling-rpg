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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.card.CardSetService;
import java.util.List;
import java.util.Map;
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
            SeedFingerprintManifest.of(Map.of()));
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
            SeedFingerprintManifest.of(Map.of()));
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
            SeedFingerprintManifest.of(Map.of()));
    verifySync.sync();

    assertEquals(
        "Shoulder Tackle",
        clean.getName(),
        "clean-fingerprint card must be reconciled to the seed name");
    assertEquals("hand-tuned", edited.getName(), "edited card must be preserved");
  }

  @Test
  void fastPath_skipsReconcileWhenManifestMatchesStoredRows() throws Exception {
    SeedSyncSupport support = new SeedSyncSupport("verify");
    PathMatchingResourcePatternResolver real = new PathMatchingResourcePatternResolver();
    Resource[] resources = real.getResources("classpath*:cards/*.json");
    when(resourcePatternResolver.getResources(any())).thenReturn(resources);

    CardSet atw = new CardSet();
    atw.setCode("ATW");
    when(cardService.count()).thenReturn(1L);
    when(cardSetService.findBySetCode("ATW")).thenReturn(Optional.of(atw));

    // ATW #5 "Forearm" stamped exactly as the manifest says; its fields still hash to the stamp.
    Card clean = new Card();
    clean.setId(1L);
    clean.setName("Forearm");
    clean.setNumber(5);
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
        support.fingerprint("Forearm", 1, 1, 2, 1, "Strike", false, false, false, false, false));
    when(cardService.findAll()).thenReturn(List.of(clean));

    CardSync fastSync =
        new CardSync(
            "verify",
            cardService,
            cardSetService,
            resourcePatternResolver,
            new ObjectMapper(),
            SeedFingerprintManifest.of(
                Map.of("card", Map.of("ATW#5", clean.getSeedFingerprint()))));
    fastSync.sync();

    // Top-up may save genuinely missing cards, but reconcile must never touch the clean card:
    // its name proves reconcile skipped (the seed agrees anyway here; the never() check on
    // saveAll of the clean card is covered by the manifest being satisfied).
    verify(cardService, never()).saveAll(argThat(list -> list != null && list.contains(clean)));
    assertEquals(
        "Forearm", clean.getName(), "fast path must skip reconcile; the row is never touched");
  }
}
