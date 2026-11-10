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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import com.github.javydreamercsw.management.sync.review.SeedProvenanceManifest;
import com.github.javydreamercsw.management.sync.review.SeedUpdateReviewService;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
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
  @Mock private SeedUpdateReviewService seedReviewService;

  private static final SeedProvenanceManifest EMPTY_MANIFEST = SeedProvenanceManifest.empty();

  private static final SeedSyncSupport VERIFY_SUPPORT = new SeedSyncSupport("verify");

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
            seedReviewService,
            EMPTY_MANIFEST);
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
            seedReviewService,
            EMPTY_MANIFEST);
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
            seedReviewService,
            EMPTY_MANIFEST);
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
            seedReviewService,
            EMPTY_MANIFEST);
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
            seedReviewService,
            EMPTY_MANIFEST);
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
            seedReviewService,
            EMPTY_MANIFEST);
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

  // ── Version-change detection (seed-update review, PR 4 Step 4 three-tier pin) ──

  @Test
  void versionChangeBootsParkCollisions() {
    // THREE-TIER pin, end-to-end through the wired pipeline. The domain's only seed-owned state is
    // the card composition (single-blob row, Task 5's per-row parking contract): the clean deck's
    // composition is seed-changed (tier 1 silent reconcile + re-stamp), the edited deck's
    // composition was hand-tuned by the user AND the seed changed too (tier 2 collision on the one
    // field — the field and the row are the same coordinate here). One deck per wrestler: the two
    // fixture decks belong to two seed wrestlers (both present in decks.json).
    Wrestler eddie = jeffHardy();
    Wrestler rvd = new Wrestler();
    rvd.setId(100L);
    rvd.setName("Rob Van Dam");
    when(wrestlerRepository.findAll()).thenReturn(List.of(eddie, rvd));

    CardSet set = eddieSet();
    Card card1 = eddieCard(1);
    Card card2 = eddieCard(2);
    when(cardSetRepository.findAll()).thenReturn(List.of(set));
    // Eddie's seed entry resolves EDDIE #1 and #2.
    when(cardService.findByNumberAndSet(1, "EDDIE")).thenReturn(Optional.of(card1));
    when(cardService.findByNumberAndSet(2, "EDDIE")).thenReturn(Optional.of(card2));
    // RVD's seed entry resolves the same two cards through the RVD set code (the stub catalog).
    when(cardService.findByNumberAndSet(1, "RVD")).thenReturn(Optional.of(card1));
    when(cardService.findByNumberAndSet(2, "RVD")).thenReturn(Optional.of(card2));

    // Tier 1: RVD's deck holds only EDDIE #1 — the previous release's seed write, stamped clean;
    // the seed's resolvable composition now includes #2 → silent update territory.
    Deck tier1deck = seededEddieDeck(set, card1);
    tier1deck.setWrestler(rvd);
    tier1deck.setSeedFingerprint(deckCompositionFingerprint(tier1deck));

    // Tier 2: Eddie's deck holds #1 x3 + #2 (the user's hand-tuned composition) while the stamp
    // carries the PREVIOUS release's amounts (EDDIE#1x2) — differing from BOTH the current
    // composition and the incoming seed's (EDDIE#1x1 + #2) → genuine tier-2 collision.
    Deck tier2deck = seededEddieDeck(set, card1);
    tier2deck.setWrestler(eddie);
    tier2deck.getCards().iterator().next().setAmount(3);
    DeckCard userAddition = new DeckCard();
    userAddition.setDeck(tier2deck);
    userAddition.setCard(card2);
    userAddition.setSet(set);
    userAddition.setAmount(1);
    tier2deck.getCards().add(userAddition);
    tier2deck.setSeedFingerprint(VERIFY_SUPPORT.fingerprint((Object[]) new String[] {"EDDIE#1x2"}));

    when(deckService.count()).thenReturn(2L);
    when(deckService.findByWrestlerWithCards(rvd)).thenReturn(List.of(tier1deck));
    when(deckService.findByWrestlerWithCards(eddie)).thenReturn(List.of(tier2deck));

    Set<String> parkedCoordinates = new HashSet<>();
    SeedUpdateReviewService parking = Mockito.mock(SeedUpdateReviewService.class);
    Mockito.doAnswer(
            inv -> {
              parkedCoordinates.add(inv.getArgument(1) + "|" + inv.getArgument(3));
              parkedCoordinates.add("#" + inv.getArgument(2));
              return null;
            })
        .when(parking)
        .parkFieldConflict(
            anyString(), anyString(), anyString(), anyString(), any(), any(), anyString());

    DeckSync detectionSync =
        new DeckSync(
            "true",
            deckService,
            wrestlerRepository,
            cardSetRepository,
            cardService,
            new ObjectMapper(),
            parking,
            EMPTY_MANIFEST);
    detectionSync.detectVersionChangeCollisions();

    // Tier 1: the clean deck's composition was reconciled to the seed in place.
    assertEquals(
        2,
        tier1deck.getCards().size(),
        "tier 1: untouched composition with a changed seed value must update silently");
    // Tier 2: the user-edited composition kept its value; the row parked the composition.
    assertEquals(
        2,
        tier2deck.getCards().size(),
        "tier 2: admin-edited composition must keep its value — only a PENDING decision is"
            + " created");
    Mockito.verify(parking)
        .parkFieldConflict(
            eq("deck"),
            eq("deck:eddie-guerrero"),
            eq("Eddie Guerrero"),
            eq("composition"),
            eq("EDDIE#1x3\nEDDIE#2x1"),
            eq("EDDIE#1x1\nEDDIE#2x1"),
            anyString());
    // Eddie's deck is the only tier-2 collision; RVD's tier-1 fields never park.
    assertEquals(
        Set.of("deck:eddie-guerrero|composition", "#Eddie Guerrero"),
        parkedCoordinates,
        "exactly the one tier-2 collision parked; tier-1 fields never park");
  }

  @Test
  void normalBootBehaviorUnchanged() {
    // Normal boot pin: FULL-mode detection is inert, and the pre-existing verify behavior still
    // holds with the new constructor dependencies.
    Wrestler eddie = jeffHardy();
    Deck existingDeck = new Deck();
    existingDeck.setWrestler(eddie);
    when(deckService.count()).thenReturn(1L);
    when(wrestlerRepository.findAll()).thenReturn(List.of(eddie));
    when(deckService.findByWrestlerWithCards(eddie)).thenReturn(List.of(existingDeck));

    SeedUpdateReviewService parking = Mockito.mock(SeedUpdateReviewService.class);
    DeckSync fullModeSync =
        new DeckSync(
            "false",
            deckService,
            wrestlerRepository,
            cardSetRepository,
            cardService,
            new ObjectMapper(),
            parking,
            EMPTY_MANIFEST);
    fullModeSync.detectVersionChangeCollisions(); // FULL mode → inert

    Mockito.verifyNoInteractions(parking);
    assertTrue(
        existingDeck.getCards().isEmpty(),
        "FULL-mode detection must not touch rows (the FULL sync's own load does that)");
  }
}
