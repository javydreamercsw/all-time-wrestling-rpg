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
package com.github.javydreamercsw.management.service.inbox;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.DataInitializer;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.inbox.InboxEventTypeRegistry;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget;
import com.github.javydreamercsw.management.domain.inbox.InboxRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class InboxVisibilityIT extends ManagementIntegrationTest {

  @Autowired private InboxService inboxService;
  @Autowired private InboxRepository inboxRepository;
  @Autowired private InboxEventTypeRegistry eventTypeRegistry;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private DataInitializer dataInitializer;

  private Account userA;
  private Account userB;
  private Wrestler wrestlerA;
  private Wrestler wrestlerB;

  @BeforeEach
  public void setUp() {
    inboxRepository.deleteAll();

    // Clean up specifically for these tests
    accountRepository
        .findByUsername("testUserA")
        .ifPresent(
            a -> {
              wrestlerRepository.findByAccount(a).forEach(wrestlerRepository::delete);
              accountRepository.delete(a);
            });
    accountRepository
        .findByUsername("testUserB")
        .ifPresent(
            a -> {
              wrestlerRepository.findByAccount(a).forEach(wrestlerRepository::delete);
              accountRepository.delete(a);
            });

    userA = accountRepository.save(new Account("testUserA", "password123", "a@test.com"));
    userB = accountRepository.save(new Account("testUserB", "password123", "b@test.com"));

    wrestlerA = new Wrestler();
    wrestlerA.setName("Wrestler A");
    wrestlerA.setAccount(userA);
    wrestlerA = wrestlerRepository.save(wrestlerA);

    wrestlerB = new Wrestler();
    wrestlerB.setName("Wrestler B");
    wrestlerB.setAccount(userB);
    wrestlerB = wrestlerRepository.save(wrestlerB);
  }

  @Test
  void testVisibilityBetweenUsers() {
    // Create message for User A (Account target)
    inboxService.createInboxItem(
        eventTypeRegistry.getEventTypes().getFirst(),
        "For User A",
        userA.getId().toString(),
        InboxItemTarget.TargetType.ACCOUNT);

    // Create message for Wrestler B (Wrestler target)
    inboxService.createInboxItem(
        eventTypeRegistry.getEventTypes().getFirst(),
        "For Wrestler B",
        wrestlerB.getId().toString(),
        InboxItemTarget.TargetType.WRESTLER);

    // User A searches for their items
    loginAs("testUserA");
    List<InboxItem> resultsA =
        inboxService.search(Collections.singleton(wrestlerA), "All", "All", false, userA.getId());

    Assertions.assertEquals(1, resultsA.size(), "User A should see exactly one message");
    Assertions.assertEquals("For User A", resultsA.getFirst().getDescription());

    // User B searches for their items
    loginAs("testUserB");
    List<InboxItem> resultsB =
        inboxService.search(Collections.singleton(wrestlerB), "All", "All", false, userB.getId());
    Assertions.assertEquals(1, resultsB.size(), "User B should see exactly one message");
    Assertions.assertEquals("For Wrestler B", resultsB.getFirst().getDescription());
  }

  @Test
  void testUserCannotSeeOtherWrestlerMessages() {
    // Create message for Wrestler B
    inboxService.createInboxItem(
        eventTypeRegistry.getEventTypes().get(0),
        "For Wrestler B",
        wrestlerB.getId().toString(),
        InboxItemTarget.TargetType.WRESTLER);

    // Login as User A
    loginAs("testUserA");

    // User A tries to search for Wrestler B's items
    List<InboxItem> results =
        inboxService.search(Collections.singleton(wrestlerB), "All", "All", false, null);

    // Should return 0 because User A doesn't own Wrestler B
    Assertions.assertEquals(
        0, results.size(), "User A should not be able to see Wrestler B's messages");
  }

  @Test
  void testBugReproduction_NoFilters() {
    // Create a message for someone else
    inboxService.createInboxItem(
        eventTypeRegistry.getEventTypes().getFirst(),
        "Secret Message",
        "999",
        InboxItemTarget.TargetType.ACCOUNT);

    // Login as a regular user (User A)
    loginAs("testUserA");

    // If accountId is null and targets is empty, we should NOT see it if we are a regular user.
    List<InboxItem> results =
        inboxService.search(Collections.emptySet(), "All", "All", false, null);

    // This should now be 0
    Assertions.assertEquals(
        0, results.size(), "Should not see any messages if no target matches and not admin");
  }

  @Test
  void testAdminCanSeeEverything() {
    // Create a message for someone else
    inboxService.createInboxItem(
        eventTypeRegistry.getEventTypes().getFirst(),
        "Secret Message",
        "999",
        InboxItemTarget.TargetType.ACCOUNT);

    // Login as admin
    loginAs("admin");

    // Admin should see it even with no filters
    List<InboxItem> results =
        inboxService.search(Collections.emptySet(), "All", "All", false, null);

    Assertions.assertTrue(
        results.stream().anyMatch(item -> item.getDescription().equals("Secret Message")));
  }
}
