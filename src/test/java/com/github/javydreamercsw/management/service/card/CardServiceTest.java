package com.github.javydreamercsw.management.service.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardRepository;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

  @Mock private CardRepository cardRepository;
  @Mock private CardSetRepository cardSetRepository;
  @Mock private Clock clock;

  @InjectMocks private CardService cardService;

  private CardSet cardSet;
  private Card card;

  @BeforeEach
  void setUp() {
    cardSet = new CardSet();
    cardSet.setId(1L);
    cardSet.setName("Test Set");
    cardSet.setSetCode("TS");
    cardSet.setCreationDate(Instant.now());

    card = new Card();
    card.setId(1L);
    card.setName("Test Card");
    card.setSet(cardSet);
    card.setNumber(1);
    card.setCreationDate(Instant.now());
  }

  @Test
  void testCreateCard() {
    when(cardSetRepository.findAll()).thenReturn(List.of(cardSet));
    when(cardRepository.findMaxCardNumberBySet(cardSet.getId())).thenReturn(1);
    when(cardRepository.saveAndFlush(any(Card.class)))
        .thenAnswer(
            invocation -> {
              Card card = invocation.getArgument(0);
              card.setId(1L);
              return card;
            });

    Card newCard = cardService.createCard("New Card");

    assertNotNull(newCard.getId());
    assertEquals("New Card", newCard.getName());
    assertEquals(2, newCard.getNumber());
    assertEquals(cardSet, newCard.getSet());
  }

  @Test
  void testCreateCardFirstInSet() {
    when(cardSetRepository.findAll()).thenReturn(List.of(cardSet));
    when(cardRepository.findMaxCardNumberBySet(cardSet.getId())).thenReturn(null);
    when(cardRepository.saveAndFlush(any(Card.class)))
        .thenAnswer(
            invocation -> {
              Card card = invocation.getArgument(0);
              card.setId(1L);
              return card;
            });

    Card newCard = cardService.createCard("New Card");

    assertNotNull(newCard.getId());
    assertEquals("New Card", newCard.getName());
    assertEquals(1, newCard.getNumber());
    assertEquals(cardSet, newCard.getSet());
  }

  @Test
  void testCreateCardNoSet() {
    when(cardSetRepository.findAll()).thenReturn(new ArrayList<>());
    assertThrows(IllegalStateException.class, () -> cardService.createCard("New Card"));
  }

  @Test
  void testList() {
    List<Card> cards = List.of(card);
    when(cardRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(cards));

    List<Card> result = cardService.list(Pageable.unpaged());

    assertEquals(1, result.size());
    assertEquals(card, result.get(0));
  }

  @Test
  void testCount() {
    when(cardRepository.count()).thenReturn(1L);
    assertEquals(1L, cardService.count());
  }

  @Test
  void testSave() {
    Instant now = Instant.now();
    when(clock.instant()).thenReturn(now);
    when(cardRepository.saveAndFlush(any(Card.class))).thenReturn(card);

    Card savedCard = cardService.save(card);

    assertEquals(now, savedCard.getCreationDate());
    verify(cardRepository, times(1)).saveAndFlush(card);
  }

  @Test
  void testFindAll() {
    List<Card> cards = List.of(card);
    when(cardRepository.findAll()).thenReturn(cards);

    List<Card> result = cardService.findAll();

    assertEquals(1, result.size());
    assertEquals(card, result.get(0));
  }

  @Test
  void testFindByNumberAndSet() {
    when(cardRepository.findByNumberAndSetSetCode(1, "TS")).thenReturn(Optional.of(card));

    Optional<Card> result = cardService.findByNumberAndSet(1, "TS");

    assertEquals(Optional.of(card), result);
  }

  @Test
  void testDelete() {
    cardService.delete(1L);
    verify(cardRepository, times(1)).deleteById(1L);
  }

  @Test
  void testFindById() {
    when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

    Optional<Card> result = cardService.findById(1L);

    assertEquals(Optional.of(card), result);
  }
}
