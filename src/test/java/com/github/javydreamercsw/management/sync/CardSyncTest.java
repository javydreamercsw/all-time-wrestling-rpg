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
import java.util.List;
import java.util.Optional;
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
            true, cardService, cardSetService, resourcePatternResolver, new ObjectMapper());
    skipSync.sync();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Card>> captor = ArgumentCaptor.forClass(List.class);
    verify(cardService, atLeastOnce()).saveAll(captor.capture());
    List<Card> saved =
        captor.getAllValues().stream()
            .flatMap(List::stream)
            .collect(java.util.stream.Collectors.toList());
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
            false, cardService, cardSetService, resourcePatternResolver, new ObjectMapper());
    fullSync.sync();

    verify(cardService, atLeastOnce()).saveAll(argThat(list -> list.contains(existing)));
    assertEquals("Shoulder Tackle", existing.getName(), "name refreshed from seed in full mode");
  }
}
