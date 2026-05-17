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
package com.github.javydreamercsw.management.service.card;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CardSetServiceTest {

  @Mock private CardSetRepository cardSetRepository;

  @Spy private Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

  @InjectMocks private CardSetService cardSetService;

  private CardSet cardSet;

  @BeforeEach
  void setUp() {
    cardSet = new CardSet();
    cardSet.setId(1L);
    cardSet.setName("Base Set");
    cardSet.setCode("BASE");

    when(cardSetRepository.saveAndFlush(any(CardSet.class))).thenAnswer(inv -> inv.getArgument(0));
    when(cardSetRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
  }

  // -------------------------------------------------------------------------
  // createCardSet
  // -------------------------------------------------------------------------

  @Test
  void createCardSet_setsNameCodeAndCreationDate() {
    CardSet result = cardSetService.createCardSet("Promo Set", "PROMO");

    assertThat(result.getName()).isEqualTo("Promo Set");
    assertThat(result.getCode()).isEqualTo("PROMO");
    assertThat(result.getCreationDate()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    verify(cardSetRepository).saveAndFlush(any(CardSet.class));
  }

  @Test
  void createCardSet_delegatesToSave() {
    cardSetService.createCardSet("Legends", "LEG");

    verify(cardSetRepository).saveAndFlush(any(CardSet.class));
  }

  // -------------------------------------------------------------------------
  // list (pageable)
  // -------------------------------------------------------------------------

  @Test
  void list_returnsPageContents() {
    Page<CardSet> page = new PageImpl<>(List.of(cardSet));
    when(cardSetRepository.findAll(any(Pageable.class))).thenReturn(page);

    List<CardSet> result = cardSetService.list(Pageable.unpaged());

    assertThat(result).hasSize(1).contains(cardSet);
    verify(cardSetRepository).findAll(any(Pageable.class));
  }

  @Test
  void list_returnsEmptyList_whenNoCardSets() {
    when(cardSetRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

    List<CardSet> result = cardSetService.list(Pageable.unpaged());

    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // count
  // -------------------------------------------------------------------------

  @Test
  void count_delegatesToRepository() {
    when(cardSetRepository.count()).thenReturn(7L);

    assertThat(cardSetService.count()).isEqualTo(7L);
    verify(cardSetRepository).count();
  }

  // -------------------------------------------------------------------------
  // save
  // -------------------------------------------------------------------------

  @Test
  void save_setsCreationDateFromClockAndPersists() {
    CardSet toSave = new CardSet();
    toSave.setName("New Set");
    toSave.setCode("NEW");

    CardSet result = cardSetService.save(toSave);

    assertThat(result.getCreationDate()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    verify(cardSetRepository).saveAndFlush(toSave);
  }

  // -------------------------------------------------------------------------
  // saveAll
  // -------------------------------------------------------------------------

  @Test
  void saveAll_setsCreationDateOnEachAndPersists() {
    CardSet cs1 = new CardSet();
    cs1.setName("Set A");
    cs1.setCode("A");
    CardSet cs2 = new CardSet();
    cs2.setName("Set B");
    cs2.setCode("B");
    when(cardSetRepository.saveAll(anyList())).thenReturn(List.of(cs1, cs2));

    List<CardSet> result = cardSetService.saveAll(List.of(cs1, cs2));

    assertThat(cs1.getCreationDate()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    assertThat(cs2.getCreationDate()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    assertThat(result).hasSize(2);
    verify(cardSetRepository).saveAll(anyList());
  }

  // -------------------------------------------------------------------------
  // findAll
  // -------------------------------------------------------------------------

  @Test
  void findAll_returnsAllCardSets() {
    when(cardSetRepository.findAll()).thenReturn(List.of(cardSet));

    List<CardSet> result = cardSetService.findAll();

    assertThat(result).hasSize(1).contains(cardSet);
    verify(cardSetRepository).findAll();
  }

  @Test
  void findAll_returnsEmptyList_whenNone() {
    when(cardSetRepository.findAll()).thenReturn(List.of());

    List<CardSet> result = cardSetService.findAll();

    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // findBySetCode
  // -------------------------------------------------------------------------

  @Test
  void findBySetCode_found() {
    when(cardSetRepository.findByCode("BASE")).thenReturn(Optional.of(cardSet));

    Optional<CardSet> result = cardSetService.findBySetCode("BASE");

    assertThat(result).isPresent().contains(cardSet);
    verify(cardSetRepository).findByCode("BASE");
  }

  @Test
  void findBySetCode_notFound_returnsEmpty() {
    when(cardSetRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

    Optional<CardSet> result = cardSetService.findBySetCode("UNKNOWN");

    assertThat(result).isEmpty();
  }
}
