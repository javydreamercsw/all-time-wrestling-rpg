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
import com.github.javydreamercsw.management.domain.card.CardSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CardSetServiceTest {

  @Autowired private CardSetService cardSetService;

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanCreateCardSet() {
    CardSet cs = cardSetService.createCardSet("Test Card Set Admin", "TCSA");
    Assertions.assertNotNull(cs);
    Assertions.assertNotNull(cs.getId());
  }

  @Test
  @WithCustomMockUser(
      username = "booker",
      roles = {"BOOKER", "PLAYER"})
  void testBookerCanCreateCardSet() {
    CardSet cs = cardSetService.createCardSet("Test Card Set Booker", "TCSB");
    Assertions.assertNotNull(cs);
    Assertions.assertNotNull(cs.getId());
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotCreateCardSet() {
    Assertions.assertThrows(
        AccessDeniedException.class,
        () -> cardSetService.createCardSet("Test Card Set Player", "TCSP"));
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testViewerCannotCreateCardSet() {
    Assertions.assertThrows(
        AccessDeniedException.class,
        () -> cardSetService.createCardSet("Test Card Set Viewer", "TCSV"));
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanSaveCardSet() {
    CardSet cs = cardSetService.createCardSet("Save Card Set Admin", "SCSA");
    cs.setName("Updated by Admin");
    CardSet savedCs = cardSetService.save(cs);
    Assertions.assertEquals("Updated by Admin", savedCs.getName());
  }

  @Test
  @WithCustomMockUser(
      username = "booker",
      roles = {"BOOKOKER", "PLAYER"})
  void testBookerCanSaveCardSet() {
    CardSet cs = cardSetService.createCardSet("Save Card Set Booker", "SCSB");
    cs.setName("Updated by Booker");
    CardSet savedCs = cardSetService.save(cs);
    Assertions.assertEquals("Updated by Booker", savedCs.getName());
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotSaveCardSet() {
    CardSet cs = new CardSet();
    cs.setName("Cannot Save Card Player");
    Assertions.assertThrows(AccessDeniedException.class, () -> cardSetService.save(cs));
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testViewerCannotSaveCardSet() {
    CardSet cs = new CardSet();
    cs.setName("Cannot Save Card Viewer");
    Assertions.assertThrows(AccessDeniedException.class, () -> cardSetService.save(cs));
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testAuthenticatedCanListCardSets() {
    cardSetService.list(Pageable.unpaged());
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testAuthenticatedCanCountCardSets() {
    cardSetService.count();
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testAuthenticatedCanFindAllCardSets() {
    cardSetService.findAll();
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testAuthenticatedCanFindBySetCode() {
    cardSetService.findBySetCode("TCSA");
    // No exception means success
  }
}
