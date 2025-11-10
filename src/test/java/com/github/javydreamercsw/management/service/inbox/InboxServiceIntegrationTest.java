package com.github.javydreamercsw.management.service.inbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class InboxServiceIntegrationTest {

  @Autowired private InboxService inboxService;

  @Autowired private InboxRepository inboxRepository;

  @BeforeEach
  void setUp() {
    inboxRepository.deleteAll();
  }

  @Test
  void testSearchWithHideRead() {
    // Given
    InboxItem readItem = new InboxItem();
    readItem.setRead(true);
    readItem.setDescription("read");
    readItem.setEventTimestamp(Instant.now());
    readItem.setEventType("Test");
    inboxRepository.save(readItem);

    InboxItem unreadItem = new InboxItem();
    unreadItem.setRead(false);
    unreadItem.setDescription("unread");
    unreadItem.setEventTimestamp(Instant.now());
    unreadItem.setEventType("Test");
    inboxRepository.save(unreadItem);

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
    inboxRepository.save(readItem);

    InboxItem unreadItem = new InboxItem();
    unreadItem.setRead(false);
    unreadItem.setDescription("unread");
    unreadItem.setEventTimestamp(Instant.now());
    unreadItem.setEventType("Test");
    inboxRepository.save(unreadItem);

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
    inboxRepository.save(item1);

    InboxItem item2 = new InboxItem();
    item2.setDescription("item2");
    item2.setEventTimestamp(Instant.now());
    item2.setEventType("Test");
    inboxRepository.save(item2);

    // When
    inboxService.deleteSelected(Set.of(item1));

    // Then
    assertEquals(1, inboxRepository.count());
    assertEquals("item2", inboxRepository.findAll().get(0).getDescription());
  }

  @Test
  void testReferenceIdPersistence() {
    // Given
    InboxItem item = new InboxItem();
    item.setDescription("Item with reference ID");
    item.setEventTimestamp(Instant.now());
    item.setEventType("Test");
    item.setReferenceId("test-reference-id");
    inboxRepository.save(item);

    // When
    List<InboxItem> foundItems = inboxRepository.findAll();

    // Then
    assertEquals(1, foundItems.size());
    assertEquals("test-reference-id", foundItems.get(0).getReferenceId());
  }
}
