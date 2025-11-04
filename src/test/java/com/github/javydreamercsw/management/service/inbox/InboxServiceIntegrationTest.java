package com.github.javydreamercsw.management.service.inbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxItemRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class InboxServiceIntegrationTest {

  @Autowired private InboxService inboxService;

  @Autowired private InboxItemRepository inboxItemRepository;

  @BeforeEach
  void setUp() {
    inboxItemRepository.deleteAll();
  }

  @Test
  void testSearchWithHideRead() {
    // Given
    InboxItem readItem = new InboxItem();
    readItem.setRead(true);
    readItem.setDescription("read");
    readItem.setEventTimestamp(Instant.now());
    readItem.setEventType("Test");
    inboxItemRepository.save(readItem);

    InboxItem unreadItem = new InboxItem();
    unreadItem.setRead(false);
    unreadItem.setDescription("unread");
    unreadItem.setEventTimestamp(Instant.now());
    unreadItem.setEventType("Test");
    inboxItemRepository.save(unreadItem);

    // When
    List<InboxItem> result = inboxService.search("", "All", "All", true);

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
    readItem.setEventType("Test");
    inboxItemRepository.save(readItem);

    InboxItem unreadItem = new InboxItem();
    unreadItem.setRead(false);
    unreadItem.setDescription("unread");
    unreadItem.setEventTimestamp(Instant.now());
    unreadItem.setEventType("Test");
    inboxItemRepository.save(unreadItem);

    // When
    List<InboxItem> readResult = inboxService.search("", "Read", "All", false);
    List<InboxItem> unreadResult = inboxService.search("", "Unread", "All", false);

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
    item1.setEventType("Test");
    inboxItemRepository.save(item1);

    InboxItem item2 = new InboxItem();
    item2.setDescription("item2");
    item2.setEventTimestamp(Instant.now());
    item2.setEventType("Test");
    inboxItemRepository.save(item2);

    // When
    inboxService.deleteSelected(List.of(item1));

    // Then
    assertEquals(1, inboxItemRepository.count());
    assertEquals("item2", inboxItemRepository.findAll().get(0).getDescription());
  }
}
