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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.test.AbstractIntegrationTest;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class InboxServiceIntegrationTest extends AbstractIntegrationTest {

  @Autowired private InboxService inboxService;

  @Autowired private InboxRepository inboxRepository;

  @Autowired private WrestlerRepository wrestlerRepository;

  @Autowired private InboxEventType fanAdjudication;

  @BeforeEach
  void setUp() {
    inboxRepository.deleteAll();
    wrestlerRepository.deleteAll();
  }

  @Test
  void testSearchWithHideRead() {
    // Given
    InboxItem readItem = new InboxItem();
    readItem.setRead(true);
    readItem.setDescription("read");
    readItem.setEventTimestamp(Instant.now());
    readItem.setEventType(fanAdjudication);
    inboxRepository.save(readItem);

    InboxItem unreadItem = new InboxItem();
    unreadItem.setRead(false);
    unreadItem.setDescription("unread");
    unreadItem.setEventTimestamp(Instant.now());
    unreadItem.setEventType(fanAdjudication);
    inboxRepository.save(unreadItem);

    // When
    List<InboxItem> result = inboxService.search(null, "All", "All", true);

    // Then
    assertEquals(1, result.size());
    assertFalse(result.get(0).isRead());
  }

  @Test
  void testSearchWithReadStatus() {
    // Given
    InboxItem readItem = new InboxItem();
    readItem.setRead(true);
    readItem.setDescription("read");
    readItem.setEventTimestamp(Instant.now());
    readItem.setEventType(fanAdjudication);
    inboxRepository.save(readItem);

    InboxItem unreadItem = new InboxItem();
    unreadItem.setRead(false);
    unreadItem.setDescription("unread");
    unreadItem.setEventTimestamp(Instant.now());
    unreadItem.setEventType(fanAdjudication);
    inboxRepository.save(unreadItem);

    // When
    List<InboxItem> readResult = inboxService.search(null, "Read", "All", false);
    List<InboxItem> unreadResult = inboxService.search(null, "Unread", "All", false);

    // Then
    assertEquals(1, readResult.size());
    assertTrue(readResult.get(0).isRead());
    assertEquals(1, unreadResult.size());
    assertFalse(unreadResult.get(0).isRead());
  }

  @Test
  void testDeleteSelected() {
    // Given
    InboxItem item1 = new InboxItem();
    item1.setDescription("item1");
    item1.setEventTimestamp(Instant.now());
    item1.setEventType(fanAdjudication);
    inboxRepository.save(item1);

    InboxItem item2 = new InboxItem();
    item2.setDescription("item2");
    item2.setEventTimestamp(Instant.now());
    item2.setEventType(fanAdjudication);
    inboxRepository.save(item2);

    // When
    inboxService.deleteSelected(Set.of(item1));

    // Then
    assertEquals(1, inboxRepository.count());
    assertEquals("item2", inboxRepository.findAll().get(0).getDescription());
  }

  @Test
  void testTargetPersistence() {
    // Given
    InboxItem item = new InboxItem();
    item.setDescription("Item with targets");
    item.setEventTimestamp(Instant.now());
    item.setEventType(fanAdjudication);
    item.addTarget("target1");
    item.addTarget("target2");
    inboxRepository.save(item);

    // When
    List<InboxItem> foundItems = inboxRepository.findAll();

    // Then
    assertEquals(1, foundItems.size());
    assertEquals(2, foundItems.get(0).getTargets().size());
    assertTrue(
        foundItems.get(0).getTargets().stream()
            .anyMatch(target -> target.getTargetId().equals("target1")));
    assertTrue(
        foundItems.get(0).getTargets().stream()
            .anyMatch(target -> target.getTargetId().equals("target2")));
  }

  @Test
  void testSearchWithTargetFilter() {
    // Given
    Wrestler wrestler = createTestWrestler("wrestler1");
    wrestlerRepository.save(wrestler);

    InboxItem item1 = new InboxItem();
    item1.setRead(true);
    item1.setDescription("read");
    item1.setEventTimestamp(Instant.now());
    item1.setEventType(fanAdjudication);
    item1.addTarget(wrestler.getId().toString());
    inboxRepository.save(item1);

    InboxItem item2 = new InboxItem();
    item2.setRead(false);
    item2.setDescription("unread");
    item2.setEventTimestamp(Instant.now());
    item2.setEventType(fanAdjudication);
    inboxRepository.save(item2);

    // When
    List<InboxItem> result =
        inboxService.search(Collections.singleton(wrestler), "All", "All", false);

    // Then
    assertEquals(1, result.size());
    assertEquals(wrestler.getId().toString(), result.get(0).getTargets().get(0).getTargetId());
  }
}
