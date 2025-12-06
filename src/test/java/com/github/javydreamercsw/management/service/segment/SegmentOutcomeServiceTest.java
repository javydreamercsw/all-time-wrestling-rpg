/*
* Copyright (C) 2025 Software Consulting Dreams LLC
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
package com.github.javydreamercsw.management.service.segment;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckCard;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerTier;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SegmentOutcomeServiceTest {

  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private Random random; // Mock the Random object
  @InjectMocks private SegmentOutcomeService segmentOutcomeService;

  @BeforeEach
  void setUp() {
    when(random.nextInt(anyInt())).thenReturn(0); // Make random predictable

    CardSet testCardSet = new CardSet();
    testCardSet.setName("TestSet");

    // Rob Van Dam setup
    Wrestler robVanDam = Wrestler.builder().build();
    robVanDam.setName("Rob Van Dam");
    robVanDam.setFans(100L);
    robVanDam.setTier(WrestlerTier.ICON);
    robVanDam.setStartingStamina(10);
    robVanDam.setLowStamina(2);
    robVanDam.setStartingHealth(10);
    robVanDam.setLowHealth(2);
    robVanDam.setDeckSize(10);
    robVanDam.setCreationDate(Instant.now());

    Card rvdFinisher = new Card();
    rvdFinisher.setName("Five-Star Frog Splash");
    rvdFinisher.setFinisher(true);
    rvdFinisher.setSet(testCardSet);
    rvdFinisher.setDamage(5);
    rvdFinisher.setTarget(1);
    rvdFinisher.setStamina(1);
    rvdFinisher.setMomentum(1);
    rvdFinisher.setType("Finisher");
    rvdFinisher.setCreationDate(Instant.now());

    Deck rvdDeck = new Deck();
    rvdDeck.setWrestler(robVanDam);
    rvdDeck.setCreationDate(Instant.now());
    Set<DeckCard> rvdDeckCards = new HashSet<>();
    DeckCard rvdDeckCard = new DeckCard();
    rvdDeckCard.setCard(rvdFinisher);
    rvdDeckCard.setSet(testCardSet);
    rvdDeckCard.setAmount(1);
    rvdDeckCard.setDeck(rvdDeck);
    rvdDeckCards.add(rvdDeckCard);
    rvdDeck.setCards(rvdDeckCards);
    robVanDam.setDecks(List.of(rvdDeck));

    // Kurt Angle setup
    Wrestler kurtAngle = Wrestler.builder().build();
    kurtAngle.setName("Kurt Angle");
    kurtAngle.setFans(90L);
    kurtAngle.setTier(WrestlerTier.MAIN_EVENTER);
    kurtAngle.setStartingStamina(10);
    kurtAngle.setLowStamina(2);
    kurtAngle.setStartingHealth(10);
    kurtAngle.setLowHealth(2);
    kurtAngle.setDeckSize(10);
    kurtAngle.setCreationDate(Instant.now());

    Card angleFinisher = new Card();
    angleFinisher.setName("Angle Slam");
    angleFinisher.setFinisher(true);
    angleFinisher.setSet(testCardSet);
    angleFinisher.setDamage(5);
    angleFinisher.setTarget(1);
    angleFinisher.setStamina(1);
    angleFinisher.setMomentum(1);
    angleFinisher.setType("Finisher");
    angleFinisher.setCreationDate(Instant.now());

    Deck angleDeck = new Deck();
    angleDeck.setWrestler(kurtAngle);
    angleDeck.setCreationDate(Instant.now());
    Set<DeckCard> angleDeckCards = new HashSet<>();
    DeckCard angleDeckCard = new DeckCard();
    angleDeckCard.setCard(angleFinisher);
    angleDeckCard.setSet(testCardSet);
    angleDeckCard.setAmount(1);
    angleDeckCard.setDeck(angleDeck);
    angleDeckCards.add(angleDeckCard);
    angleDeck.setCards(angleDeckCards);
    kurtAngle.setDecks(List.of(angleDeck));

    // Generic Wrestler setup
    Wrestler genericWrestler = Wrestler.builder().build();
    genericWrestler.setName("Generic Wrestler");
    genericWrestler.setFans(50L);
    genericWrestler.setTier(WrestlerTier.MIDCARDER);
    genericWrestler.setStartingStamina(8);
    genericWrestler.setLowStamina(2);
    genericWrestler.setStartingHealth(8);
    genericWrestler.setLowHealth(2);
    genericWrestler.setDeckSize(10);
    genericWrestler.setCreationDate(Instant.now());

    Card genericFinisher = new Card();
    genericFinisher.setName("Generic Finisher");
    genericFinisher.setFinisher(true);
    genericFinisher.setSet(testCardSet);
    genericFinisher.setDamage(4);
    genericFinisher.setTarget(1);
    genericFinisher.setStamina(1);
    genericFinisher.setMomentum(1);
    genericFinisher.setType("Finisher");
    genericFinisher.setCreationDate(Instant.now());

    Deck genericDeck = new Deck();
    genericDeck.setWrestler(genericWrestler);
    genericDeck.setCreationDate(Instant.now());
    Set<DeckCard> genericDeckCards = new HashSet<>();
    DeckCard genericDeckCard = new DeckCard();
    genericDeckCard.setCard(genericFinisher);
    genericDeckCard.setSet(testCardSet);
    genericDeckCard.setAmount(1);
    genericDeckCard.setDeck(genericDeck);
    genericDeckCards.add(genericDeckCard);
    genericDeck.setCards(genericDeckCards);
    genericWrestler.setDecks(List.of(genericDeck));

    when(wrestlerRepository.findByName("Rob Van Dam")).thenReturn(Optional.of(robVanDam));
    when(wrestlerRepository.findByName("Kurt Angle")).thenReturn(Optional.of(kurtAngle));
    lenient()
        .when(wrestlerRepository.findByName("Generic Wrestler"))
        .thenReturn(Optional.of(genericWrestler));
  }

  @Test
  void testDetermineTwoWrestlerOutcomeWithSpecificFinisher() {
    // Fix 2: WrestlerContext constructor
    SegmentNarrationService.WrestlerContext rvdContext =
        new SegmentNarrationService.WrestlerContext();
    rvdContext.setName("Rob Van Dam");
    SegmentNarrationService.WrestlerContext angleContext =
        new SegmentNarrationService.WrestlerContext();
    angleContext.setName("Kurt Angle");

    SegmentNarrationService.SegmentNarrationContext context =
        new SegmentNarrationService.SegmentNarrationContext();
    context.setWrestlers(List.of(rvdContext, angleContext));

    // Fix 3: SegmentType
    SegmentNarrationService.SegmentTypeContext segmentType =
        new SegmentNarrationService.SegmentTypeContext();
    segmentType.setSegmentType("Match");
    context.setSegmentType(segmentType);

    SegmentNarrationService.SegmentNarrationContext result =
        segmentOutcomeService.determineOutcomeIfNeeded(context);

    assertNotNull(result.getDeterminedOutcome());

    // With a deterministic random mock, Rob Van Dam should always win.
    assertTrue(
        result.getDeterminedOutcome().contains("Rob Van Dam"), result.getDeterminedOutcome());
    assertTrue(
        result.getDeterminedOutcome().contains("Five-Star Frog Splash"),
        result.getDeterminedOutcome());
  }

  @Test
  void testDetermineMultiWrestlerOutcomeWithSpecificFinisher() {
    // Fix 2: WrestlerContext constructor
    SegmentNarrationService.WrestlerContext rvdContext =
        new SegmentNarrationService.WrestlerContext();
    rvdContext.setName("Rob Van Dam");
    SegmentNarrationService.WrestlerContext angleContext =
        new SegmentNarrationService.WrestlerContext();
    angleContext.setName("Kurt Angle");
    SegmentNarrationService.WrestlerContext genericWrestlerContext =
        new SegmentNarrationService.WrestlerContext();
    genericWrestlerContext.setName("Generic Wrestler");

    SegmentNarrationService.SegmentNarrationContext context =
        new SegmentNarrationService.SegmentNarrationContext();
    context.setWrestlers(List.of(rvdContext, angleContext, genericWrestlerContext));

    // Fix 3: SegmentType
    SegmentNarrationService.SegmentTypeContext segmentType =
        new SegmentNarrationService.SegmentTypeContext();
    segmentType.setSegmentType("Match");
    context.setSegmentType(segmentType);

    SegmentNarrationService.SegmentNarrationContext result =
        segmentOutcomeService.determineOutcomeIfNeeded(context);

    assertNotNull(result.getDeterminedOutcome());
    // With full data, Rob Van Dam should win.
    assertTrue(
        result.getDeterminedOutcome().contains("Rob Van Dam"), result.getDeterminedOutcome());
    assertTrue(
        result.getDeterminedOutcome().contains("Five-Star Frog Splash"),
        result.getDeterminedOutcome());
  }
}
