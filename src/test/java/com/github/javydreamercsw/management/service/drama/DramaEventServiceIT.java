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
package com.github.javydreamercsw.management.service.drama;

import com.github.javydreamercsw.base.AccountInitializer;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.security.WithCustomMockUser;
import com.github.javydreamercsw.management.DatabaseCleaner;
import com.github.javydreamercsw.management.domain.drama.DramaEvent;
import com.github.javydreamercsw.management.domain.drama.DramaEventSeverity;
import com.github.javydreamercsw.management.domain.drama.DramaEventType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
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
class DramaEventServiceIT {

  @Autowired private DramaEventService dramaEventService;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private AccountInitializer accountInitializer;
  @Autowired private DatabaseCleaner cleaner;

  private Wrestler bookerWrestler;
  private Wrestler playerWrestler;

  @BeforeEach
  void setUp() {
    cleaner.clearRepositories();
    accountInitializer.init();
    if (bookerWrestler == null) {
      Account booker = accountRepository.findByUsername("booker").get();
      bookerWrestler = new Wrestler();
      bookerWrestler.setName("Booker T");
      bookerWrestler.setAccount(booker);
      wrestlerRepository.save(bookerWrestler);
    }

    if (playerWrestler == null) {
      Account player = accountRepository.findByUsername("player").orElseThrow();
      playerWrestler = new Wrestler();
      playerWrestler.setName("Player One");
      playerWrestler.setAccount(player);
      wrestlerRepository.save(playerWrestler);
    }
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanCreateDramaEvent() {
    Optional<DramaEvent> event =
        dramaEventService.createDramaEvent(
            bookerWrestler.getId(),
            playerWrestler.getId(),
            DramaEventType.BACKSTAGE_INCIDENT,
            DramaEventSeverity.NEUTRAL,
            "Test Title",
            "Test Description");
    Assertions.assertTrue(event.isPresent());
  }

  @Test
  @WithCustomMockUser(
      username = "booker",
      roles = {"BOOKER", "PLAYER"})
  void testBookerCanCreateDramaEvent() {
    Optional<DramaEvent> event =
        dramaEventService.createDramaEvent(
            bookerWrestler.getId(),
            playerWrestler.getId(),
            DramaEventType.BACKSTAGE_INCIDENT,
            DramaEventSeverity.NEUTRAL,
            "Test Title",
            "Test Description");
    Assertions.assertTrue(event.isPresent());
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotCreateDramaEvent() {
    Assertions.assertThrows(
        AccessDeniedException.class,
        () ->
            dramaEventService.createDramaEvent(
                bookerWrestler.getId(),
                playerWrestler.getId(),
                DramaEventType.BACKSTAGE_INCIDENT,
                DramaEventSeverity.NEUTRAL,
                "Test Title",
                "Test Description"));
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testViewerCannotCreateDramaEvent() {
    Assertions.assertThrows(
        AccessDeniedException.class,
        () ->
            dramaEventService.createDramaEvent(
                bookerWrestler.getId(),
                playerWrestler.getId(),
                DramaEventType.BACKSTAGE_INCIDENT,
                DramaEventSeverity.NEUTRAL,
                "Test Title",
                "Test Description"));
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanGenerateRandomDramaEvent() {
    Optional<DramaEvent> event = dramaEventService.generateRandomDramaEvent(bookerWrestler.getId());
    Assertions.assertTrue(event.isPresent());
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotGenerateRandomDramaEvent() {
    Assertions.assertThrows(
        AccessDeniedException.class,
        () -> dramaEventService.generateRandomDramaEvent(bookerWrestler.getId()));
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanProcessUnprocessedEvents() {
    dramaEventService.createDramaEvent(
        bookerWrestler.getId(),
        playerWrestler.getId(),
        DramaEventType.BACKSTAGE_INCIDENT,
        DramaEventSeverity.NEUTRAL,
        "Test Title",
        "Test Description");
    dramaEventService.processUnprocessedEvents();
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotProcessUnprocessedEvents() {
    Assertions.assertThrows(
        AccessDeniedException.class, () -> dramaEventService.processUnprocessedEvents());
  }

  @Test
  @WithCustomMockUser(
      username = "admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanProcessEvent() {
    Optional<DramaEvent> event =
        dramaEventService.createDramaEvent(
            bookerWrestler.getId(),
            playerWrestler.getId(),
            DramaEventType.BACKSTAGE_INCIDENT,
            DramaEventSeverity.NEUTRAL,
            "Test Title",
            "Test Description");
    Assertions.assertTrue(event.isPresent());
    dramaEventService.processEvent(event.get());
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testPlayerCannotProcessEvent() {
    DramaEvent event = new DramaEvent();
    Assertions.assertThrows(
        AccessDeniedException.class, () -> dramaEventService.processEvent(event));
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testAuthenticatedCanGetEventsForWrestler() {
    dramaEventService.getEventsForWrestler(bookerWrestler.getId());
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testAuthenticatedCanGetEventsForWrestlerWithPageable() {
    dramaEventService.getEventsForWrestler(bookerWrestler.getId(), Pageable.unpaged());
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "player", roles = "PLAYER")
  void testAuthenticatedCanGetRecentEvents() {
    dramaEventService.getRecentEvents();
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "viewer", roles = "VIEWER")
  void testAuthenticatedCanGetEventsBetweenWrestlers() {
    dramaEventService.getEventsBetweenWrestlers(bookerWrestler.getId(), playerWrestler.getId());
    // No exception means success
  }
}
