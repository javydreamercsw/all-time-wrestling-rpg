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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardRepository;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
class CardServiceTest {

  @Mock private CardRepository cardRepository;
  @Mock private CardSetRepository cardSetRepository;

  @Spy private Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

  @Mock private Validator validator;

  @InjectMocks private CardService cardService;

  private Card card;
  private CardSet cardSet;

  @BeforeEach
  void setUp() {
    cardSet = new CardSet();
    cardSet.setId(1L);
    cardSet.setName("Base Set");
    cardSet.setCode("BASE");

    card = new Card();
    card.setId(10L);
    card.setName("Suplex");
    card.setType("Strike");
    card.setDamage(2);
    card.setMomentum(1);
    card.setTarget(1);
    card.setStamina(1);
    card.setSignature(false);
    card.setFinisher(false);
    card.setSet(cardSet);
    card.setNumber(1);

    // Default: validator passes
    when(validator.validate(any(Card.class))).thenReturn(Collections.emptySet());
    // Default: repository returns the card unchanged
    when(cardRepository.saveAndFlush(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  // -------------------------------------------------------------------------
  // createCard
  // -------------------------------------------------------------------------

  @Test
  void createCard_success_whenCardSetExists() {
    when(cardSetRepository.findAll()).thenReturn(List.of(cardSet));
    when(cardRepository.findMaxCardNumberBySet(1L)).thenReturn(5);

    Card result = cardService.createCard("DDT");

    assertThat(result.getName()).isEqualTo("DDT");
    assertThat(result.getNumber()).isEqualTo(6); // maxCardNumber + 1
    assertThat(result.getSet()).isEqualTo(cardSet);
    verify(cardRepository).saveAndFlush(any(Card.class));
  }

  @Test
  void createCard_numbersFromOne_whenNoCardsInSet() {
    when(cardSetRepository.findAll()).thenReturn(List.of(cardSet));
    when(cardRepository.findMaxCardNumberBySet(1L)).thenReturn(null);

    Card result = cardService.createCard("Clothesline");

    assertThat(result.getNumber()).isEqualTo(1);
  }

  @Test
  void createCard_throwsIllegalState_whenNoCardSetAvailable() {
    when(cardSetRepository.findAll()).thenReturn(List.of());

    assertThatThrownBy(() -> cardService.createCard("RKO"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No CardSet available");
  }

  // -------------------------------------------------------------------------
  // list
  // -------------------------------------------------------------------------

  @Test
  void list_delegatesToRepositoryWithPageable() {
    Page<Card> page = new PageImpl<>(List.of(card));
    when(cardRepository.findAll(any(Pageable.class))).thenReturn(page);

    List<Card> result = cardService.list(Pageable.unpaged());

    assertThat(result).hasSize(1).contains(card);
    verify(cardRepository).findAll(any(Pageable.class));
  }

  @Test
  void list_returnsEmptyList_whenNoCards() {
    when(cardRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

    List<Card> result = cardService.list(Pageable.unpaged());

    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // count
  // -------------------------------------------------------------------------

  @Test
  void count_delegatesToRepository() {
    when(cardRepository.count()).thenReturn(42L);

    assertThat(cardService.count()).isEqualTo(42L);
    verify(cardRepository).count();
  }

  // -------------------------------------------------------------------------
  // save
  // -------------------------------------------------------------------------

  @Test
  void save_success_setsCreationDateAndPersists() {
    Card result = cardService.save(card);

    assertThat(result.getCreationDate()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    verify(cardRepository).saveAndFlush(card);
  }

  @Test
  @SuppressWarnings("unchecked")
  void save_throwsValidationException_whenViolationsPresent() {
    ConstraintViolation<Card> violation = org.mockito.Mockito.mock(ConstraintViolation.class);
    when(validator.validate(any(Card.class))).thenReturn(Set.of(violation));

    assertThatThrownBy(() -> cardService.save(card)).isInstanceOf(ValidationException.class);
  }

  // -------------------------------------------------------------------------
  // saveAll
  // -------------------------------------------------------------------------

  @Test
  void saveAll_delegatesToRepository() {
    when(cardRepository.saveAll(any())).thenReturn(List.of(card));

    List<Card> result = cardService.saveAll(List.of(card));

    assertThat(result).hasSize(1).contains(card);
    verify(cardRepository).saveAll(List.of(card));
  }

  @Test
  @SuppressWarnings("unchecked")
  void saveAll_throwsValidationException_onAnyViolation() {
    ConstraintViolation<Card> violation = org.mockito.Mockito.mock(ConstraintViolation.class);
    when(validator.validate(any(Card.class))).thenReturn(Set.of(violation));

    assertThatThrownBy(() -> cardService.saveAll(List.of(card)))
        .isInstanceOf(ValidationException.class);
  }

  // -------------------------------------------------------------------------
  // findAll
  // -------------------------------------------------------------------------

  @Test
  void findAll_returnsAllCards() {
    when(cardRepository.findAll()).thenReturn(List.of(card));

    List<Card> result = cardService.findAll();

    assertThat(result).hasSize(1).contains(card);
    verify(cardRepository).findAll();
  }

  // -------------------------------------------------------------------------
  // findByNumberAndSet
  // -------------------------------------------------------------------------

  @Test
  void findByNumberAndSet_found() {
    when(cardRepository.findByNumberAndSetCode(1, "BASE")).thenReturn(Optional.of(card));

    Optional<Card> result = cardService.findByNumberAndSet(1, "BASE");

    assertThat(result).isPresent().contains(card);
  }

  @Test
  void findByNumberAndSet_notFound_returnsEmpty() {
    when(cardRepository.findByNumberAndSetCode(99, "UNKNOWN")).thenReturn(Optional.empty());

    Optional<Card> result = cardService.findByNumberAndSet(99, "UNKNOWN");

    assertThat(result).isEmpty();
  }

  // -------------------------------------------------------------------------
  // delete
  // -------------------------------------------------------------------------

  @Test
  void delete_delegatesToRepository() {
    cardService.delete(10L);

    verify(cardRepository).deleteById(10L);
  }

  // -------------------------------------------------------------------------
  // findById
  // -------------------------------------------------------------------------

  @Test
  void findById_found() {
    when(cardRepository.findById(10L)).thenReturn(Optional.of(card));

    Optional<Card> result = cardService.findById(10L);

    assertThat(result).isPresent().contains(card);
  }

  @Test
  void findById_notFound_returnsEmpty() {
    when(cardRepository.findById(99L)).thenReturn(Optional.empty());

    Optional<Card> result = cardService.findById(99L);

    assertThat(result).isEmpty();
  }
}
