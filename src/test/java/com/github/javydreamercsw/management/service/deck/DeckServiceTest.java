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
package com.github.javydreamercsw.management.service.deck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeckServiceTest {

  @Mock private DeckRepository deckRepository;

  private Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

  private DeckService deckService;

  private Deck deck;

  @BeforeEach
  void setUp() {
    // DeckService uses constructor injection, so we create it manually
    deckService = new DeckService(deckRepository, clock);

    deck = new Deck();
    deck.setId(1L);

    when(deckRepository.saveAndFlush(any(Deck.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  // -------------------------------------------------------------------------
  // count
  // -------------------------------------------------------------------------

  @Test
  void count_delegatesToRepository() {
    when(deckRepository.count()).thenReturn(7L);

    assertThat(deckService.count()).isEqualTo(7L);
    verify(deckRepository).count();
  }

  // -------------------------------------------------------------------------
  // delete
  // -------------------------------------------------------------------------

  @Test
  void delete_delegatesToRepository() {
    deckService.delete(deck);

    verify(deckRepository).delete(deck);
  }

  // -------------------------------------------------------------------------
  // findAll
  // -------------------------------------------------------------------------

  @Test
  void findAll_returnsAllDecks() {
    when(deckRepository.findAll()).thenReturn(List.of(deck));

    List<Deck> result = deckService.findAll();

    assertThat(result).hasSize(1).contains(deck);
    verify(deckRepository).findAll();
  }

  @Test
  void findAll_returnsEmptyList_whenNone() {
    when(deckRepository.findAll()).thenReturn(List.of());

    List<Deck> result = deckService.findAll();

    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // findById
  // -------------------------------------------------------------------------

  @Test
  void findById_found_returnsDeck() {
    when(deckRepository.findById(1L)).thenReturn(Optional.of(deck));

    Deck result = deckService.findById(1L);

    assertThat(result).isSameAs(deck);
  }

  @Test
  void findById_notFound_throwsEntityNotFoundException() {
    when(deckRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> deckService.findById(99L))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageContaining("99");
  }

  // -------------------------------------------------------------------------
  // list
  // -------------------------------------------------------------------------

  @Test
  void list_delegatesToRepositoryWithPageable() {
    Page<Deck> page = new PageImpl<>(List.of(deck));
    when(deckRepository.findAll(any(Pageable.class))).thenReturn(page);

    List<Deck> result = deckService.list(Pageable.unpaged());

    assertThat(result).hasSize(1).contains(deck);
    verify(deckRepository).findAll(any(Pageable.class));
  }

  @Test
  void list_returnsEmptyList_whenNone() {
    when(deckRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

    List<Deck> result = deckService.list(Pageable.unpaged());

    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // save
  // -------------------------------------------------------------------------

  @Test
  void save_setsCreationDateAndDelegates() {
    Deck result = deckService.save(deck);

    assertThat(result.getCreationDate()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    verify(deckRepository).saveAndFlush(deck);
  }

  @Test
  void save_returnsPersistedDeck() {
    Deck saved = new Deck();
    saved.setId(42L);
    when(deckRepository.saveAndFlush(deck)).thenReturn(saved);

    Deck result = deckService.save(deck);

    assertThat(result).isSameAs(saved);
  }
}
