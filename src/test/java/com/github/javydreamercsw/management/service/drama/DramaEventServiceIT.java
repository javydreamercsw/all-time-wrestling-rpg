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

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.Role;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.domain.account.RoleRepository;
import com.github.javydreamercsw.base.security.WithCustomMockUser;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.drama.DramaEvent;
import com.github.javydreamercsw.management.domain.drama.DramaEventSeverity;
import com.github.javydreamercsw.management.domain.drama.DramaEventType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

class DramaEventServiceIT extends ManagementIntegrationTest {

  @Autowired private DramaEventService dramaEventService;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private RoleRepository roleRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  private Wrestler testWrestler1;
  private Wrestler testWrestler2;
  private Wrestler playerWrestler;

  @BeforeEach
  void setUp() {
    clearAllRepositories();
    // Roles should be present in the DB from migrations or a general test data setup.
    Role playerRole =
        roleRepository
            .findByName(RoleName.PLAYER)
            .orElseGet(() -> roleRepository.save(new Role(RoleName.PLAYER, "Player role")));

    createTestAccount("drama_viewer", RoleName.VIEWER);
    createTestAccount("drama_admin", RoleName.ADMIN);
    createTestAccount("drama_booker", RoleName.BOOKER);
    Account playerAccount = createTestAccount("drama_player", RoleName.PLAYER);

    // Ensure accounts are flushed to DB so PermissionService can find them
    accountRepository.flush();

    // Ensure no wrestler is associated with this account from previous tests
    wrestlerRepository
        .findByAccount(playerAccount)
        .ifPresent(
            w -> {
              wrestlerRepository.delete(w);
              wrestlerRepository.flush();
            });

    String uuid1 = UUID.randomUUID().toString();
    Account testAccount1 =
        new Account(
            "tw1-" + uuid1, passwordEncoder.encode("password"), "test1-" + uuid1 + "@test.com");
    testAccount1.setRoles(Set.of(playerRole));
    accountRepository.save(testAccount1);

    String uuid2 = UUID.randomUUID().toString();
    Account testAccount2 =
        new Account(
            "tw2-" + uuid2, passwordEncoder.encode("password"), "test2-" + uuid2 + "@test.com");
    testAccount2.setRoles(Set.of(playerRole));
    accountRepository.save(testAccount2);

    testWrestler1 = new Wrestler();
    testWrestler1.setName("Test Wrestler 1");
    testWrestler1.setAccount(testAccount1);
    wrestlerRepository.save(testWrestler1);

    testWrestler2 = new Wrestler();
    testWrestler2.setName("Test Wrestler 2");
    testWrestler2.setAccount(testAccount2);
    wrestlerRepository.save(testWrestler2);

    playerWrestler = new Wrestler();
    playerWrestler.setName("Drama Player Wrestler");
    playerWrestler.setAccount(playerAccount);
    wrestlerRepository.save(playerWrestler);
  }

  /*
   * @AfterEach
   *
   * @Override public void tearDown() { wrestlerRepository.delete(testWrestler1);
   * wrestlerRepository.delete(testWrestler2); accountRepository.delete(testAccount1);
   * accountRepository.delete(testAccount2); super.tearDown(); }
   */

  @Test
  @WithCustomMockUser(
      username = "drama_admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanCreateDramaEvent() {
    Optional<DramaEvent> event =
        dramaEventService.createDramaEvent(
            testWrestler1.getId(),
            testWrestler2.getId(),
            DramaEventType.BACKSTAGE_INCIDENT,
            DramaEventSeverity.NEUTRAL,
            "Test Title",
            "Test Description");
    Assertions.assertTrue(event.isPresent());
  }

  @Test
  @WithCustomMockUser(
      username = "drama_booker",
      roles = {"BOOKER", "PLAYER"})
  void testBookerCanCreateDramaEvent() {
    Optional<DramaEvent> event =
        dramaEventService.createDramaEvent(
            testWrestler1.getId(),
            testWrestler2.getId(),
            DramaEventType.BACKSTAGE_INCIDENT,
            DramaEventSeverity.NEUTRAL,
            "Test Title",
            "Test Description");
    Assertions.assertTrue(event.isPresent());
  }

  @Test
  @WithCustomMockUser(username = "drama_player", roles = "PLAYER")
  void testPlayerCannotCreateDramaEvent() {
    Assertions.assertThrows(
        AccessDeniedException.class,
        () ->
            dramaEventService.createDramaEvent(
                testWrestler1.getId(),
                testWrestler2.getId(),
                DramaEventType.BACKSTAGE_INCIDENT,
                DramaEventSeverity.NEUTRAL,
                "Test Title",
                "Test Description"));
  }

  @Test
  @WithCustomMockUser(username = "drama_viewer", roles = "VIEWER")
  void testViewerCannotCreateDramaEvent() {
    Assertions.assertThrows(
        AccessDeniedException.class,
        () ->
            dramaEventService.createDramaEvent(
                testWrestler1.getId(),
                testWrestler2.getId(),
                DramaEventType.BACKSTAGE_INCIDENT,
                DramaEventSeverity.NEUTRAL,
                "Test Title",
                "Test Description"));
  }

  @Test
  @WithCustomMockUser(
      username = "drama_admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanGenerateRandomDramaEvent() {
    Optional<DramaEvent> event = dramaEventService.generateRandomDramaEvent(testWrestler1.getId());
    Assertions.assertTrue(event.isPresent());
  }

  @Test
  @WithCustomMockUser(username = "drama_player", roles = "PLAYER")
  void testPlayerCannotGenerateRandomDramaEvent() {
    Assertions.assertThrows(
        AccessDeniedException.class,
        () -> dramaEventService.generateRandomDramaEvent(testWrestler1.getId()));
  }

  @Test
  @WithCustomMockUser(
      username = "drama_admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanProcessUnprocessedEvents() {
    dramaEventService.createDramaEvent(
        testWrestler1.getId(),
        testWrestler2.getId(),
        DramaEventType.BACKSTAGE_INCIDENT,
        DramaEventSeverity.NEUTRAL,
        "Test Title",
        "Test Description");
    dramaEventService.processUnprocessedEvents();
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "drama_player", roles = "PLAYER")
  void testPlayerCannotProcessUnprocessedEvents() {
    Assertions.assertThrows(
        AccessDeniedException.class, () -> dramaEventService.processUnprocessedEvents());
  }

  @Test
  @WithCustomMockUser(
      username = "drama_admin",
      roles = {"ADMIN", "PLAYER"})
  void testAdminCanProcessEvent() {
    Optional<DramaEvent> event =
        dramaEventService.createDramaEvent(
            testWrestler1.getId(),
            testWrestler2.getId(),
            DramaEventType.BACKSTAGE_INCIDENT,
            DramaEventSeverity.NEUTRAL,
            "Test Title",
            "Test Description");
    Assertions.assertTrue(event.isPresent());
    dramaEventService.processEvent(event.get());
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "drama_player", roles = "PLAYER")
  void testPlayerCannotProcessEvent() {
    DramaEvent event = new DramaEvent();
    Assertions.assertThrows(
        AccessDeniedException.class, () -> dramaEventService.processEvent(event));
  }

  @Test
  @WithCustomMockUser(username = "drama_player", roles = "PLAYER")
  void testAuthenticatedCanGetEventsForWrestler() {
    dramaEventService.getEventsForWrestler(playerWrestler.getId());
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "drama_viewer", roles = "VIEWER")
  void testAuthenticatedCanGetEventsForWrestlerWithPageable() {
    dramaEventService.getEventsForWrestler(testWrestler1.getId(), Pageable.unpaged());
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "drama_player", roles = "PLAYER")
  void testAuthenticatedCanGetRecentEvents() {
    dramaEventService.getRecentEvents();
    // No exception means success
  }

  @Test
  @WithCustomMockUser(username = "drama_viewer", roles = "VIEWER")
  void testAuthenticatedCanGetEventsBetweenWrestlers() {
    dramaEventService.getEventsBetweenWrestlers(testWrestler1.getId(), testWrestler2.getId());
    // No exception means success
  }
}
