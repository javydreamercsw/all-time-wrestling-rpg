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
package com.github.javydreamercsw.management.service.card;

import com.github.javydreamercsw.base.security.WithCustomMockUser;
import com.github.javydreamercsw.management.domain.card.Card;
import com.github.javydreamercsw.management.domain.card.CardRepository;
import com.github.javydreamercsw.management.domain.card.CardSet;
import com.github.javydreamercsw.management.domain.card.CardSetRepository;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CardServiceTest {

  @Autowired private CardService cardService;
  @Autowired private CardSetRepository cardSetRepository;
  @Autowired private CardRepository cardRepository;

  private CardSet defaultCardSet;

  @BeforeEach
  void setUp() {
    // Ensure there is at least one CardSet for Card creation
    Optional<CardSet> existingSet = cardSetRepository.findByName("Default Test Set");
    if (existingSet.isEmpty()) {
      defaultCardSet = new CardSet();
      defaultCardSet.setName("Default Test Set");
      defaultCardSet.setCode("DTS"); // CardSet has setCode
      cardSetRepository.save(defaultCardSet);
    } else {
      defaultCardSet = existingSet.get();
    }
  }

  private Card createCardAsAdmin(String name) {
    Card card = new Card();
    card.setName(name);
    card.setType("Test");
    card.setSet(defaultCardSet);
    card.setTarget(1);
    card.setStamina(1);
    card.setDamage(1);
    card.setMomentum(1);
    card.setSignature(false);
    card.setFinisher(false);
    card.setTaunt(false);
    card.setRecover(false);
    card.setPin(false);
    card.setCreationDate(java.time.Instant.now());
    return cardRepository.save(card);
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanCreateCard() {
    Card card = cardService.createCard("Test Card Admin");
    Assertions.assertNotNull(card);
    Assertions.assertNotNull(card.getId());
  }

  @Test
  @WithCustomMockUser(
      username = "booker",
      roles = {"BOOKER", "PLAYER"})
  void testBookerCanCreateCard() {
    Card card = cardService.createCard("Test Card Booker");
    Assertions.assertNotNull(card);
    Assertions.assertNotNull(card.getId());
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotCreateCard() {
    Assertions.assertThrows(
        AccessDeniedException.class, () -> cardService.createCard("Test Card Player"));
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testViewerCannotCreateCard() {
    Assertions.assertThrows(
        AccessDeniedException.class, () -> cardService.createCard("Test Card Viewer"));
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanSaveCard() {
    Card card = createCardAsAdmin("Save Card Admin");
    card.setName("Updated by Admin"); // Use setName instead of setDescription
    Card savedCard = cardService.save(card);
    Assertions.assertEquals("Updated by Admin", savedCard.getName()); // Use getName
  }

  @Test
  @WithCustomMockUser(
      username = "booker",
      roles = {"BOOKER", "PLAYER"})
  void testBookerCanSaveCard() {
    Card card = createCardAsAdmin("Save Card Booker");
    card.setName("Updated by Booker"); // Use setName
    Card savedCard = cardService.save(card);
    Assertions.assertEquals("Updated by Booker", savedCard.getName()); // Use getName
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotSaveCard() {
    Card card = new Card();
    card.setName("Cannot Save Card Player");
    Assertions.assertThrows(AccessDeniedException.class, () -> cardService.save(card));
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testViewerCannotSaveCard() {
    Card card = new Card();
    card.setName("Cannot Save Card Viewer");
    Assertions.assertThrows(AccessDeniedException.class, () -> cardService.save(card));
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanDeleteCard() {
    Card card = createCardAsAdmin("Delete Card Admin");
    Long cardId = card.getId();
    cardService.delete(cardId);
    Optional<Card> deletedCard = cardService.findById(cardId);
    Assertions.assertTrue(deletedCard.isEmpty());
  }

  @Test
  @WithCustomMockUser(
      username = "booker",
      roles = {"BOOKER", "PLAYER"})
  void testBookerCanDeleteCard() {
    Card card = createCardAsAdmin("Delete Card Booker");
    Long cardId = card.getId();
    cardService.delete(cardId);
    Optional<Card> deletedCard = cardService.findById(cardId);
    Assertions.assertTrue(deletedCard.isEmpty());
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotDeleteCard() {
    Card card = createCardAsAdmin("Delete Card Player");
    Long cardId = card.getId();
    Assertions.assertThrows(AccessDeniedException.class, () -> cardService.delete(cardId));
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testViewerCannotDeleteCard() {
    Card card = createCardAsAdmin("Delete Card Viewer");
    Long cardId = card.getId();
    Assertions.assertThrows(AccessDeniedException.class, () -> cardService.delete(cardId));
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testAuthenticatedCanListCards() {
    cardService.list(Pageable.unpaged());
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testAuthenticatedCanCountCards() {
    cardService.count();
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testAuthenticatedCanFindAllCards() {
    cardService.findAll();
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testAuthenticatedCanFindByNumberAndSet() {
    // Requires existing card data, assume it exists for test setup
    cardService.findByNumberAndSet(1, "DTS");
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testAuthenticatedCanFindById() {
    // Requires existing card data, assume it exists for test setup
    cardService.findById(1L);
    // No exception means success
  }
}
