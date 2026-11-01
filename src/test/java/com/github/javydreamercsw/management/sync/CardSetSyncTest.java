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
    CardSetSync skipSync = new CardSetSync(true, cardSetService, new ObjectMapper());
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
    CardSetSync skipSync = new CardSetSync(true, cardSetService, new ObjectMapper());
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
    CardSetSync fullSync = new CardSetSync(false, cardSetService, new ObjectMapper());
    CardSet existing = new CardSet();
    existing.setCode("ATW");
    existing.setName("stale name");
    when(cardSetService.findBySetCode("ATW")).thenReturn(Optional.of(existing));

    fullSync.sync();

    verify(cardSetService, atLeastOnce()).saveAll(argThat(list -> list.contains(existing)));
    assertFalse("stale name".equals(existing.getName()), "name must be refreshed from seed");
  }
}
