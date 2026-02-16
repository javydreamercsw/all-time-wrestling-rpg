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
package com.github.javydreamercsw.management.service.inbox;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.DataInitializer;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.inbox.InboxEventTypeRegistry;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class InboxServiceIT extends ManagementIntegrationTest {

  @Autowired private InboxService inboxService;
  @Autowired private InboxRepository inboxRepository;
  @Autowired private InboxEventTypeRegistry eventTypeRegistry;
  @Autowired private WrestlerService wrestlerService;
  @Autowired private DataInitializer dataInitializer;

  @BeforeEach
  public void setUp() {
    inboxRepository.deleteAll();
    dataInitializer.init();
  }

  @Test
  void testSearch() {
    Wrestler wrestler = wrestlerService.findAll().get(0);
    // Create an inbox item with a target
    InboxItem anObject = new InboxItem();
    anObject.setDescription("Filter Me Item");
    anObject.setRead(false);
    anObject.setEventType(eventTypeRegistry.getEventTypes().get(0));
    anObject.addTarget(wrestler.getId().toString());
    inboxRepository.save(anObject);

    // Create an inbox item without a target
    InboxItem noTarget = new InboxItem();
    noTarget.setDescription("No Target Item");
    noTarget.setRead(false);
    noTarget.setEventType(eventTypeRegistry.getEventTypes().get(0));
    inboxRepository.save(noTarget);

    // Search for the item with the target
    List<InboxItem> results = inboxService.search(Set.of(wrestler), null, null, false, null);
    Assertions.assertEquals(1, results.size());
    Assertions.assertEquals("Filter Me Item", results.get(0).getDescription());
  }

  @Test
  void testSearchByAccount() {
    Wrestler wrestler = wrestlerService.findAll().get(0);
    Account account = wrestler.getAccount();
    if (account == null) {
      // In case dataInitializer didn't link them
      account =
          inboxService
              .getAccountRepository()
              .findByUsername("admin")
              .orElseGet(
                  () -> {
                    Account a = new Account("admin", "password", "admin@test.com");
                    return inboxService.getAccountRepository().save(a);
                  });
    }

    // Create an inbox item targeted at account
    inboxService.createInboxItem(
        eventTypeRegistry.getEventTypes().get(0),
        "Account Target Item",
        account.getId().toString(),
        com.github.javydreamercsw.management.domain.inbox.InboxItemTarget.TargetType.ACCOUNT);

    // Search for items for this account
    List<InboxItem> results = inboxService.search(null, null, null, false, account.getId());
    Assertions.assertTrue(
        results.stream().anyMatch(item -> item.getDescription().equals("Account Target Item")));
  }
}
