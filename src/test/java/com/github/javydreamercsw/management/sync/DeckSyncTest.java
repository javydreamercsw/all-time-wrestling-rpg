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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import com.github.javydreamercsw.management.domain.deck.Deck;
import com.github.javydreamercsw.management.domain.deck.DeckCard;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.card.CardService;
import com.github.javydreamercsw.management.service.deck.DeckService;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeckSyncTest {

  @Mock private DeckService deckService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private CardSetRepository cardSetRepository;
  @Mock private CardService cardService;

  private DeckSync skipSync;
  private DeckSync fullSync;

  private Wrestler jeffHardy() {
    Wrestler w = new Wrestler();
    w.setId(99L);
    w.setName("Eddie Guerrero");
    return w;
  }

  private CardSet eddieSet() {
    CardSet set = new CardSet();
    set.setId(1L);
    set.setCode("EDDIE");
    return set;
  }

  private Card eddieCard(int number) {
    CardSet set = eddieSet();
    Card c = new Card();
    c.setId((long) number);
    c.setName("EDDIE card " + number);
    c.setNumber(number);
    c.setSet(set);
    return c;
  }

  @Test
  void sync_topUpCreatesDeckForWrestlerWithoutOneWhenSkipFlagSet() {
    // Simulates a flag-protected install: decks exist for everyone except Eddie Guerrero. Top-up
    // must build exactly his deck from decks.json.
    skipSync =
        new DeckSync(
            "true",
            deckService,
            wrestlerRepository,
            cardSetRepository,
            cardService,
            new ObjectMapper(),
            SeedFingerprintManifest.of(Map.of()));
    when(deckService.count()).thenReturn(35L);

    Wrestler jeff = jeffHardy();
    when(wrestlerRepository.findAll()).thenReturn(List.of(jeff));

    // Jeff has no deck yet.
    when(deckService.findByWrestlerWithCards(jeff)).thenReturn(List.of());
    Deck newDeck = new Deck();
    newDeck.setWrestler(jeff);
    when(deckService.createDeck(jeff)).thenReturn(newDeck);

    // EDDIE cards exist in the catalog (CardSync top-up ran first in production ordering).
    CardSet set = eddieSet();
    when(cardSetRepository.findAll()).thenReturn(List.of(set));
    when(cardService.findByNumberAndSet(1, "EDDIE")).thenReturn(Optional.of(eddieCard(1)));

    skipSync.sync();

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Deck>> captor = ArgumentCaptor.forClass(List.class);
    verify(deckService, atLeastOnce()).saveAll(captor.capture());
    List<Deck> saved =
        captor.getAllValues().stream().flatMap(List::stream).collect(Collectors.toList());
    assertFalse(saved.isEmpty(), "Eddie's new deck must be saved");
  }

  @Test
  void sync_topUpNeverModifiesExistingDeckWhenSkipFlagSet() {
    skipSync =
        new DeckSync(
            "true",
            deckService,
            wrestlerRepository,
            cardSetRepository,
            cardService,
            new ObjectMapper(),
            SeedFingerprintManifest.of(Map.of()));
    when(deckService.count()).thenReturn(1L);

    Wrestler jeff = jeffHardy();
    when(wrestlerRepository.findAll()).thenReturn(List.of(jeff));

    // Eddie already has a deck: under the skip gate it must never be re-saved, even if the seed
    // file lists different cards.
    Deck existingDeck = new Deck();
    existingDeck.setWrestler(jeff);
    when(deckService.findByWrestlerWithCards(jeff)).thenReturn(List.of(existingDeck));

    skipSync.sync();

    verify(deckService, never()).saveAll(any());
  }

  @Test
  void sync_fullModeReconcilesExistingDeck() {
    fullSync =
        new DeckSync(
            "false",
            deckService,
            wrestlerRepository,
            cardSetRepository,
            cardService,
            new ObjectMapper(),
            SeedFingerprintManifest.of(Map.of()));
    when(deckService.count()).thenReturn(1L);

    Wrestler jeff = jeffHardy();
    when(wrestlerRepository.findAll()).thenReturn(List.of(jeff));

    Deck existingDeck = new Deck();
    existingDeck.setWrestler(jeff);
    when(deckService.findByWrestlerWithCards(jeff)).thenReturn(List.of(existingDeck));

    CardSet set = eddieSet();
    Card card = eddieCard(1);
    when(cardSetRepository.findAll()).thenReturn(List.of(set));
    when(cardService.findByNumberAndSet(1, "EDDIE")).thenReturn(Optional.of(card));

    fullSync.sync();

    // Full mode reconciles: Eddie's deck gains the seeded JFH #1 card.
    verify(deckService, atLeastOnce()).saveAll(any());
    assertFalse(existingDeck.getCards().isEmpty());
    assertEquals(1, existingDeck.getCards().size());
  }

  private DeckSync verifySync;

  private Deck seededEddieDeck(CardSet set, Card card1) {
    Deck deck = new Deck();
    deck.setWrestler(jeffHardy());
    DeckCard dc = new DeckCard();
    dc.setDeck(deck);
    dc.setCard(card1);
    dc.setSet(set);
    dc.setAmount(1);
    deck.getCards().add(dc);
    return deck;
  }

  @Test
  void verifyMode_reconcilesCleanDeckToSeedComposition() {
    verifySync =
        new DeckSync(
            "verify",
            deckService,
            wrestlerRepository,
            cardSetRepository,
            cardService,
            new ObjectMapper(),
            SeedFingerprintManifest.of(Map.of()));
    when(deckService.count()).thenReturn(1L);

    Wrestler eddie = jeffHardy();
    when(wrestlerRepository.findAll()).thenReturn(List.of(eddie));

    CardSet set = eddieSet();
    Card card1 = eddieCard(1);
    Card card2 = eddieCard(2);
    when(cardSetRepository.findAll()).thenReturn(List.of(set));
    when(cardService.findByNumberAndSet(1, "EDDIE")).thenReturn(Optional.of(card1));
    when(cardService.findByNumberAndSet(2, "EDDIE")).thenReturn(Optional.of(card2));

    // Clean: the deck holds only EDDIE #1 — exactly what an older release's seed write left,
    // with the stamp hashing that composition.
    Deck deck = seededEddieDeck(set, card1);
    deck.setSeedFingerprint(deckCompositionFingerprint(deck));
    when(deckService.findByWrestlerWithCards(eddie)).thenReturn(List.of(deck));

    verifySync.sync();

    // Seed lists EDDIE #1 and #2 (plus ATW cards not stubbed here): the clean deck gains #2.
    assertEquals(
        2, deck.getCards().size(), "clean-fingerprint deck must adopt the seed's new card entries");
    verify(deckService, atLeastOnce()).saveAll(any());
  }

  @Test
  void verifyMode_preservesUserEditedComposition() {
    verifySync =
        new DeckSync(
            "verify",
            deckService,
            wrestlerRepository,
            cardSetRepository,
            cardService,
            new ObjectMapper(),
            SeedFingerprintManifest.of(Map.of()));
    when(deckService.count()).thenReturn(1L);

    Wrestler eddie = jeffHardy();
    when(wrestlerRepository.findAll()).thenReturn(List.of(eddie));

    CardSet set = eddieSet();
    Card card1 = eddieCard(1);
    Card card2 = eddieCard(2);
    when(cardSetRepository.findAll()).thenReturn(List.of(set));
    when(cardService.findByNumberAndSet(1, "EDDIE")).thenReturn(Optional.of(card1));
    when(cardService.findByNumberAndSet(2, "EDDIE")).thenReturn(Optional.of(card2));

    // Edited: stamped at seed write (over EDDIE #1 alone), then the user added EDDIE #2 —
    // the composition no longer hashes to the stamp.
    Deck deck = seededEddieDeck(set, card1);
    deck.setSeedFingerprint(deckCompositionFingerprint(deck));
    DeckCard userAddition = new DeckCard();
    userAddition.setDeck(deck);
    userAddition.setCard(card2);
    userAddition.setSet(set);
    userAddition.setAmount(1);
    deck.getCards().add(userAddition);

    when(deckService.findByWrestlerWithCards(eddie)).thenReturn(List.of(deck));

    verifySync.sync();

    assertEquals(
        2, deck.getCards().size(), "user-edited composition must be preserved, not re-synced");
    verify(deckService, never()).saveAll(any());
  }

  @Test
  void verifyMode_adoptsLegacyDeckWhenCompositionMatchesSeed() {
    verifySync =
        new DeckSync(
            "verify",
            deckService,
            wrestlerRepository,
            cardSetRepository,
            cardService,
            new ObjectMapper(),
            SeedFingerprintManifest.of(Map.of()));
    when(deckService.count()).thenReturn(1L);

    Wrestler eddie = jeffHardy();
    when(wrestlerRepository.findAll()).thenReturn(List.of(eddie));

    CardSet set = eddieSet();
    Card card1 = eddieCard(1);
    Card card2 = eddieCard(2);
    when(cardSetRepository.findAll()).thenReturn(List.of(set));
    when(cardService.findByNumberAndSet(1, "EDDIE")).thenReturn(Optional.of(card1));
    when(cardService.findByNumberAndSet(2, "EDDIE")).thenReturn(Optional.of(card2));

    // Legacy: no stamp, but the deck's composition equals the seed's resolvable entries
    // (EDDIE #1 and #2, the only cards stubbed into the catalog).
    Deck deck = seededEddieDeck(set, card1);
    DeckCard seededTwo = new DeckCard();
    seededTwo.setDeck(deck);
    seededTwo.setCard(card2);
    seededTwo.setSet(set);
    seededTwo.setAmount(1);
    deck.getCards().add(seededTwo);
    when(deckService.findByWrestlerWithCards(eddie)).thenReturn(List.of(deck));

    verifySync.sync();

    assertEquals(
        deckCompositionFingerprint(deck),
        deck.getSeedFingerprint(),
        "legacy deck whose composition matches the seed must be adopted (stamped)");
    verify(deckService, atLeastOnce()).saveAll(any());
  }

  private String deckCompositionFingerprint(Deck deck) {
    String entries =
        deck.getCards().stream()
            .map(
                dc -> dc.getSet().getCode() + "#" + dc.getCard().getNumber() + "x" + dc.getAmount())
            .sorted()
            .collect(Collectors.joining("\n"));
    return new SeedSyncSupport("verify").fingerprint((Object[]) entries.split("\n"));
  }

  @Test
  void fastPath_topUpStillRunsButReconcileIsSkipped() {
    // The manifest carries exactly the fingerprint the DB deck stores (and its composition still
    // hashes to it): the deck fast path must skip reconcileVerifiedDecks but still run the
    // top-up, which creates decks for wrestlers without one.
    when(deckService.count()).thenReturn(2L);

    Wrestler eddie = jeffHardy();
    Wrestler rvd = new Wrestler();
    rvd.setId(7L);
    rvd.setName("Rob Van Dam");
    when(wrestlerRepository.findAll()).thenReturn(List.of(eddie, rvd));

    CardSet set = eddieSet();
    Card card1 = eddieCard(1);
    Card card2 = eddieCard(2);
    CardSet rvdSet = new CardSet();
    rvdSet.setId(2L);
    rvdSet.setCode("RVD");
    Card rvdCard5 = new Card();
    rvdCard5.setId(5L);
    rvdCard5.setName("Five-Star");
    rvdCard5.setNumber(5);
    rvdCard5.setSet(rvdSet);
    when(cardSetRepository.findAll()).thenReturn(List.of(set, rvdSet));
    when(cardService.findByNumberAndSet(1, "EDDIE")).thenReturn(Optional.of(card1));
    when(cardService.findByNumberAndSet(2, "EDDIE")).thenReturn(Optional.of(card2));
    when(cardService.findByNumberAndSet(5, "RVD")).thenReturn(Optional.of(rvdCard5));

    // Eddie's deck is clean and stamped exactly as the manifest says (composition still hashes
    // to it); RVD has no deck yet, so top-up must still create his.
    Deck deck = seededEddieDeck(set, card1);
    deck.setSeedFingerprint(deckCompositionFingerprint(deck));
    // findAllWithCards() is the join-fetched fast-path batch loader; findByWrestlerWithCards
    // feeds top-up/reconcile.
    when(deckService.findAllWithCards()).thenReturn(List.of(deck));
    when(deckService.findByWrestlerWithCards(eddie)).thenReturn(List.of(deck));
    when(deckService.findByWrestlerWithCards(rvd)).thenReturn(List.of());
    Deck rvdNewDeck = new Deck();
    rvdNewDeck.setWrestler(rvd);
    when(deckService.createDeck(rvd)).thenReturn(rvdNewDeck);
    when(deckService.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

    DeckSync sync =
        new DeckSync(
            "verify",
            deckService,
            wrestlerRepository,
            cardSetRepository,
            cardService,
            new ObjectMapper(),
            SeedFingerprintManifest.of(
                Map.of("deck", Map.of("Eddie Guerrero", deck.getSeedFingerprint()))));

    sync.sync();

    // Top-up still ran: RVD's missing deck was created and saved.
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<Deck>> captor = ArgumentCaptor.forClass(List.class);
    verify(deckService, atLeastOnce()).saveAll(captor.capture());
    List<Deck> saved =
        captor.getAllValues().stream().flatMap(List::stream).collect(Collectors.toList());
    assertTrue(
        saved.stream().anyMatch(d -> d == rvdNewDeck),
        "top-up must still create the missing deck for RVD");
    // Reconcile never re-saves the already-correct deck.
    assertFalse(saved.stream().anyMatch(d -> d == deck), "reconcile must be skipped");
  }
}
