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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.service.card.CardSetService;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CardSetSyncTest {

  @Mock private CardSetService cardSetService;

  @Test
  void sync_topUpInsertsOnlyMissingCardSetsWhenSkipFlagSet() {
    // Simulates a flag-protected install where one seeded set (ATW) exists and every other
    // seeded set is missing. Top-up must insert exactly the missing sets and never re-save an
    // existing row.
    CardSetSync skipSync = new CardSetSync("true", cardSetService, new ObjectMapper());
    when(cardSetService.count()).thenReturn(1L);

    CardSet existing = new CardSet();
    existing.setCode("ATW");
    existing.setName("All Time Wrestling");
    when(cardSetService.findBySetCode("ATW")).thenReturn(Optional.of(existing));
    when(cardSetService.findBySetCode(argThat(code -> !"ATW".equals(code))))
        .thenReturn(Optional.empty());

    skipSync.sync();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<CardSet>> captor = ArgumentCaptor.forClass(List.class);
    verify(cardSetService, atLeastOnce()).saveAll(captor.capture());
    List<CardSet> saved =
        captor.getAllValues().stream().flatMap(List::stream).collect(Collectors.toList());
    assertFalse(saved.stream().anyMatch(cs -> cs == existing), "existing row must not be re-saved");
    assertFalse(saved.stream().anyMatch(cs -> "ATW".equals(cs.getCode())));
  }

  @Test
  void sync_topUpLeavesExistingCardSetUntouchedWhenSkipFlagSet() {
    CardSetSync skipSync = new CardSetSync("true", cardSetService, new ObjectMapper());
    when(cardSetService.count()).thenReturn(1L);
    CardSet existing = new CardSet();
    existing.setCode("ATW");
    existing.setName("stale name");
    when(cardSetService.findBySetCode("ATW")).thenReturn(Optional.of(existing));

    skipSync.sync();

    // ATW is seeded but already exists: under the skip gate its row must never be re-saved and
    // the stale name must survive.
    verify(cardSetService, Mockito.never())
        .saveAll(argThat(list -> list != null && list.stream().anyMatch(cs -> cs == existing)));
    assertEquals("stale name", existing.getName());
  }

  @Test
  void sync_fullModeUpdatesExistingCardSet() {
    // Without the skip flag, existing rows are updated to match the seed (original behavior).
    CardSetSync fullSync = new CardSetSync("false", cardSetService, new ObjectMapper());
    CardSet existing = new CardSet();
    existing.setCode("ATW");
    existing.setName("stale name");
    when(cardSetService.findBySetCode("ATW")).thenReturn(Optional.of(existing));

    fullSync.sync();

    verify(cardSetService, atLeastOnce()).saveAll(argThat(list -> list.contains(existing)));
    assertFalse("stale name".equals(existing.getName()), "name must be refreshed from seed");
  }

  @Test
  void verifyMode_reconcilesCleanRowAndPreservesEditedRow() {
    SeedSyncSupport support = new SeedSyncSupport("verify");
    CardSetSync verifySync = new CardSetSync("verify", cardSetService, new ObjectMapper());
    when(cardSetService.count()).thenReturn(2L);

    // ATW: stamped as an older release's seed write left it — the row's own (then-current) name
    // hashes to the stamp; the name has since gone stale only because the seed moved on.
    CardSet clean = new CardSet();
    clean.setCode("ATW");
    clean.setName("stale set name");
    clean.setSeedFingerprint(support.fingerprint("stale set name"));

    // Edited row: stamped at seed write, then hand-renamed (its current name no longer hashes
    // to the stamp).
    CardSet edited = new CardSet();
    edited.setCode("RVD");
    edited.setName("user-renamed set");
    edited.setSeedFingerprint(support.fingerprint("Rob Van Dam"));

    when(cardSetService.findBySetCode("ATW")).thenReturn(Optional.of(clean));
    when(cardSetService.findBySetCode("RVD")).thenReturn(Optional.of(edited));
    when(cardSetService.findBySetCode(argThat(code -> !"ATW".equals(code) && !"RVD".equals(code))))
        .thenReturn(Optional.empty());

    verifySync.sync();

    assertEquals(
        "All Time Wrestling",
        clean.getName(),
        "clean-fingerprint set must be reconciled to the seed name");
    assertEquals("user-renamed set", edited.getName(), "edited set must be preserved");
  }

  @Test
  void verifyMode_adoptsLegacyRowWhenFieldsMatchSeed() {
    SeedSyncSupport support = new SeedSyncSupport("verify");
    CardSetSync verifySync = new CardSetSync("verify", cardSetService, new ObjectMapper());
    when(cardSetService.count()).thenReturn(1L);
    CardSet legacy = new CardSet();
    legacy.setCode("ATW");
    legacy.setName("All Time Wrestling"); // matches seed, null fingerprint
    when(cardSetService.findBySetCode("ATW")).thenReturn(Optional.of(legacy));
    when(cardSetService.findBySetCode(argThat(code -> !"ATW".equals(code))))
        .thenReturn(Optional.empty());

    verifySync.sync();

    assertEquals(
        support.fingerprint("All Time Wrestling"),
        legacy.getSeedFingerprint(),
        "legacy row matching the seed must be adopted (fingerprint stamped)");
  }
}
