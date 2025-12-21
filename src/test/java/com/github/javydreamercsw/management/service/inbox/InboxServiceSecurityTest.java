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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.javydreamercsw.base.test.AbstractSecurityTest;
import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxEventTypeRegistry;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithUserDetails;

@Import(InboxServiceSecurityTest.TestConfig.class)
class InboxServiceSecurityTest extends AbstractSecurityTest {

  @Autowired private InboxService inboxService;
  @Autowired private InboxRepository inboxRepository;
  @Autowired private InboxEventTypeRegistry eventTypeRegistry;

  @TestConfiguration
  static class TestConfig {
    @Bean
    public com.github.javydreamercsw.management.domain.inbox.InboxEventType testEventType() {
      return new com.github.javydreamercsw.management.domain.inbox.InboxEventType(
          "TEST_EVENT", "Test Event Type");
    }
  }

  private Wrestler getWrestler(String username) {
    return wrestlerRepository
        .findByAccountUsername(username)
        .orElseThrow(
            () -> new IllegalStateException("Wrestler for user " + username + " not found"));
  }

  private InboxEventType getOrCreateEventType() {
    return eventTypeRegistry.getEventTypes().stream()
        .filter(et -> et.getName().equals("TEST_EVENT"))
        .findFirst()
        .orElseThrow(
            () -> new IllegalStateException("TEST_EVENT not found in InboxEventTypeRegistry"));
  }

  private InboxItem getOrCreateInboxItem(Wrestler wrestler, String description) {
    InboxEventType eventType = getOrCreateEventType();
    List<InboxItem> items =
        inboxRepository.findByWrestlerIdAndDescription(wrestler.getId().toString(), description);
    if (items.isEmpty()) {
      InboxItem newItem = new InboxItem();
      newItem.setDescription(description);
      newItem.setEventType(eventType);
      newItem.addTarget(wrestler.getId().toString());
      return inboxRepository.save(newItem);
    }
    return items.get(0);
  }

  // --- Mark as Read/Unread ---

  @Test
  @WithUserDetails("admin")
  void testAdminCanMarkAsReadUnread() {
    InboxItem ownedInboxItem = getOrCreateInboxItem(getWrestler("owner"), "Owned Item");
    assertDoesNotThrow(() -> inboxService.markSelectedAsRead(Set.of(ownedInboxItem)));
    assertDoesNotThrow(() -> inboxService.markSelectedAsUnread(Set.of(ownedInboxItem)));
  }

  @Test
  @WithUserDetails("booker")
  void testBookerCanMarkAsReadUnread() {
    InboxItem ownedInboxItem = getOrCreateInboxItem(getWrestler("owner"), "Owned Item");
    assertDoesNotThrow(() -> inboxService.markSelectedAsRead(Set.of(ownedInboxItem)));
    assertDoesNotThrow(() -> inboxService.markSelectedAsUnread(Set.of(ownedInboxItem)));
  }

  @Test
  @WithUserDetails("owner")
  void testPlayerCanMarkOwnAsReadUnread() {
    InboxItem ownedInboxItem = getOrCreateInboxItem(getWrestler("owner"), "Owned Item");
    assertDoesNotThrow(() -> inboxService.markSelectedAsRead(Set.of(ownedInboxItem)));
    assertDoesNotThrow(() -> inboxService.markSelectedAsUnread(Set.of(ownedInboxItem)));
  }

  @Test
  @WithUserDetails("not_owner")
  void testPlayerCannotMarkOtherAsReadUnread() {
    InboxItem otherInboxItem = getOrCreateInboxItem(getWrestler("not_owner"), "Other Item");
    assertThrows(
        AccessDeniedException.class, () -> inboxService.markSelectedAsRead(Set.of(otherInboxItem)));
    assertThrows(
        AccessDeniedException.class,
        () -> inboxService.markSelectedAsUnread(Set.of(otherInboxItem)));
  }

  // --- Delete ---

  @Test
  @WithUserDetails("admin")
  void testAdminCanDelete() {
    InboxItem ownedInboxItem = getOrCreateInboxItem(getWrestler("owner"), "Owned Item");
    assertDoesNotThrow(() -> inboxService.deleteSelected(Set.of(ownedInboxItem)));
  }

  @Test
  @WithUserDetails("booker")
  void testBookerCanDelete() {
    InboxItem ownedInboxItem = getOrCreateInboxItem(getWrestler("owner"), "Owned Item");
    assertDoesNotThrow(() -> inboxService.deleteSelected(Set.of(ownedInboxItem)));
  }

  @Test
  @WithUserDetails("owner")
  void testPlayerCanDeleteOwn() {
    InboxItem ownedInboxItem = getOrCreateInboxItem(getWrestler("owner"), "Owned Item");
    assertDoesNotThrow(() -> inboxService.deleteSelected(Set.of(ownedInboxItem)));
  }

  @Test
  @WithUserDetails("not_owner")
  void testPlayerCannotDeleteOther() {
    InboxItem otherInboxItem = getOrCreateInboxItem(getWrestler("not_owner"), "Other Item");
    assertThrows(
        AccessDeniedException.class, () -> inboxService.deleteSelected(Set.of(otherInboxItem)));
  }

  // --- Toggle Read Status ---
  @Test
  @WithUserDetails("admin")
  void testAdminCanToggleReadStatus() {
    InboxItem ownedInboxItem = getOrCreateInboxItem(getWrestler("owner"), "Owned Item");
    assertDoesNotThrow(() -> inboxService.toggleReadStatus(ownedInboxItem));
  }

  @Test
  @WithUserDetails("booker")
  void testBookerCanToggleReadStatus() {
    InboxItem ownedInboxItem = getOrCreateInboxItem(getWrestler("owner"), "Owned Item");
    assertDoesNotThrow(() -> inboxService.toggleReadStatus(ownedInboxItem));
  }

  @Test
  @WithUserDetails("owner")
  void testPlayerCanToggleOwnReadStatus() {
    InboxItem ownedInboxItem = getOrCreateInboxItem(getWrestler("owner"), "Owned Item");
    assertDoesNotThrow(() -> inboxService.toggleReadStatus(ownedInboxItem));
  }

  @Test
  @WithUserDetails("not_owner")
  void testPlayerCannotToggleOtherReadStatus() {
    InboxItem otherInboxItem = getOrCreateInboxItem(getWrestler("not_owner"), "Other Item");
    assertThrows(AccessDeniedException.class, () -> inboxService.toggleReadStatus(otherInboxItem));
  }

  @Test
  @WithUserDetails("viewer")
  void testViewerCannotToggleReadStatus() {
    InboxItem ownedInboxItem = getOrCreateInboxItem(getWrestler("owner"), "Owned Item");
    assertThrows(AccessDeniedException.class, () -> inboxService.toggleReadStatus(ownedInboxItem));
  }

  // --- Create ---
  @Test
  @WithUserDetails("admin")
  void testAdminCanCreate() {
    InboxEventType testEventType = getOrCreateEventType();
    assertDoesNotThrow(() -> inboxService.createInboxItem(testEventType, "Admin message", "ref"));
  }

  @Test
  @WithUserDetails("booker")
  void testBookerCanCreate() {
    InboxEventType testEventType = getOrCreateEventType();
    assertDoesNotThrow(() -> inboxService.createInboxItem(testEventType, "Booker message", "ref"));
  }

  @Test
  @WithUserDetails("owner")
  void testPlayerCannotCreate() {
    InboxEventType testEventType = getOrCreateEventType();
    assertThrows(
        AccessDeniedException.class,
        () -> inboxService.createInboxItem(testEventType, "Player message", "ref"));
  }

  @Test
  @WithUserDetails("viewer")
  void testViewerCannotCreate() {
    InboxEventType testEventType = getOrCreateEventType();
    assertThrows(
        AccessDeniedException.class,
        () -> inboxService.createInboxItem(testEventType, "Viewer message", "ref"));
  }
}
