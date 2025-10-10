package com.github.javydreamercsw.management.service.card;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import java.time.Clock;
import java.time.Instant;
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
class CardSetServiceTest {

  @Mock private CardSetRepository cardSetRepository;
  @Mock private Clock clock;

  @InjectMocks private CardSetService cardSetService;

  private CardSet cardSet;

  @BeforeEach
  void setUp() {
    cardSet = new CardSet();
    cardSet.setId(1L);
    cardSet.setName("Test Set");
    cardSet.setSetCode("TS");
    cardSet.setCreationDate(Instant.now());
  }

  @Test
  void testCreateCardSet() {
    Instant now = Instant.now();
    when(clock.instant()).thenReturn(now);
    when(cardSetRepository.saveAndFlush(any(CardSet.class)))
        .thenAnswer(
            invocation -> {
              CardSet cs = invocation.getArgument(0);
              cs.setId(1L);
              cs.setCreationDate(now);
              return cs;
            });

    CardSet newCardSet = cardSetService.createCardSet("New Set", "NS");

    assertEquals("New Set", newCardSet.getName());
    assertEquals("NS", newCardSet.getSetCode());
    assertEquals(now, newCardSet.getCreationDate());
  }

  @Test
  void testCreateCardSetNullName() {
    assertThrows(NullPointerException.class, () -> cardSetService.createCardSet(null, "NS"));
  }

  @Test
  void testCreateCardSetNullSetCode() {
    assertThrows(NullPointerException.class, () -> cardSetService.createCardSet("New Set", null));
  }

  @Test
  void testList() {
    List<CardSet> cardSets = List.of(cardSet);
    when(cardSetRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(cardSets));

    List<CardSet> result = cardSetService.list(Pageable.unpaged());

    assertEquals(1, result.size());
    assertEquals(cardSet, result.get(0));
  }

  @Test
  void testCount() {
    when(cardSetRepository.count()).thenReturn(1L);
    assertEquals(1L, cardSetService.count());
  }

  @Test
  void testSave() {
    Instant now = Instant.now();
    when(clock.instant()).thenReturn(now);
    when(cardSetRepository.saveAndFlush(any(CardSet.class))).thenReturn(cardSet);

    CardSet savedCardSet = cardSetService.save(cardSet);

    assertEquals(now, savedCardSet.getCreationDate());
    verify(cardSetRepository, times(1)).saveAndFlush(cardSet);
  }

  @Test
  void testFindAll() {
    List<CardSet> cardSets = List.of(cardSet);
    when(cardSetRepository.findAll()).thenReturn(cardSets);

    List<CardSet> result = cardSetService.findAll();

    assertEquals(1, result.size());
    assertEquals(cardSet, result.get(0));
  }

  @Test
  void testFindBySetCode() {
    when(cardSetRepository.findBySetCode("TS")).thenReturn(Optional.of(cardSet));

    Optional<CardSet> result = cardSetService.findBySetCode("TS");

    assertEquals(Optional.of(cardSet), result);
  }
}
