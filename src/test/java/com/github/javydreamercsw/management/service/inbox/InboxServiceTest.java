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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxEventTypeRegistry;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget;
import com.github.javydreamercsw.management.domain.inbox.InboxRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InboxServiceTest {

  @Mock private InboxRepository inboxRepository;
  @Mock private InboxEventTypeRegistry eventTypeRegistry;
  @Mock private SecurityUtils securityUtils;
  @Mock private AccountRepository accountRepository;

  @InjectMocks private InboxService inboxService;

  private InboxEventType testEventType;
  private InboxItem item1;
  private InboxItem item2;

  @BeforeEach
  void setUp() {
    testEventType = new InboxEventType("TEST_EVENT", "Test Event");

    item1 = new InboxItem();
    item1.setId(1L);

    item2 = new InboxItem();
    item2.setId(2L);

    when(inboxRepository.save(any(InboxItem.class))).thenAnswer(inv -> inv.getArgument(0));
    when(eventTypeRegistry.getEventTypes()).thenReturn(List.of(testEventType));
  }

  @Test
  void testDeleteSelected() {
    Set<InboxItem> items = Set.of(item1, item2);

    inboxService.deleteSelected(items);

    verify(inboxRepository, times(1)).deleteAll(items);
  }

  @Test
  void createInboxItem_singleTarget_savesItem() {
    InboxItem saved =
        inboxService.createInboxItem(
            testEventType, "Test message", "123", InboxItemTarget.TargetType.WRESTLER);

    verify(inboxRepository).save(any(InboxItem.class));
    assertThat(saved.getDescription()).isEqualTo("Test message");
    assertThat(saved.getEventType()).isEqualTo(testEventType);
  }

  @Test
  void createInboxItem_multipleTargets_savesItemWithTargets() {
    List<InboxService.TargetInfo> targets =
        List.of(
            new InboxService.TargetInfo("100", InboxItemTarget.TargetType.WRESTLER),
            new InboxService.TargetInfo("200", InboxItemTarget.TargetType.ACCOUNT));

    InboxItem saved = inboxService.createInboxItem(testEventType, "Multi target", targets);

    verify(inboxRepository).save(any(InboxItem.class));
    assertThat(saved.getDescription()).isEqualTo("Multi target");
    assertThat(saved.getTargets()).hasSize(2);
  }

  @Test
  void markSelectedAsRead_setsReadTrue() {
    item1.setRead(false);
    item2.setRead(false);
    Set<InboxItem> items = Set.of(item1, item2);

    inboxService.markSelectedAsRead(items);

    verify(inboxRepository).saveAll(items);
    assertThat(item1.isRead()).isTrue();
    assertThat(item2.isRead()).isTrue();
  }

  @Test
  void markSelectedAsUnread_setsReadFalse() {
    item1.setRead(true);
    item2.setRead(true);
    Set<InboxItem> items = Set.of(item1, item2);

    inboxService.markSelectedAsUnread(items);

    verify(inboxRepository).saveAll(items);
    assertThat(item1.isRead()).isFalse();
    assertThat(item2.isRead()).isFalse();
  }

  @Test
  void toggleReadStatus_fromFalseToTrue() {
    item1.setRead(false);

    InboxItem result = inboxService.toggleReadStatus(item1);

    verify(inboxRepository).save(item1);
    assertThat(result.isRead()).isTrue();
  }

  @Test
  void toggleReadStatus_fromTrueToFalse() {
    item1.setRead(true);

    InboxItem result = inboxService.toggleReadStatus(item1);

    assertThat(result.isRead()).isFalse();
  }

  @Test
  void findAll_delegatesToRepository() {
    when(inboxRepository.findAll(
            org.mockito.ArgumentMatchers
                .<org.springframework.data.jpa.domain.Specification<InboxItem>>any(),
            any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(item1)));

    List<InboxItem> result = inboxService.findAll(null, Pageable.unpaged());

    assertThat(result).hasSize(1);
  }

  @Test
  void list_delegatesToRepository() {
    when(inboxRepository.findAll(any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(item1, item2)));

    List<InboxItem> result = inboxService.list(Pageable.unpaged());

    assertThat(result).hasSize(2);
  }

  @Test
  void count_delegatesToRepository() {
    when(inboxRepository.count()).thenReturn(42L);

    assertThat(inboxService.count()).isEqualTo(42L);
  }

  @Test
  void countUnread_returnsNumberOfUnreadItemsForAccount() {
    when(securityUtils.isAdmin()).thenReturn(false);
    when(securityUtils.isBooker()).thenReturn(false);
    when(securityUtils.getCurrentAccountId()).thenReturn(java.util.Optional.of(1L));
    when(inboxRepository.findAll(
            any(Specification.class), any(org.springframework.data.domain.Sort.class)))
        .thenReturn(List.of(item1, item2));

    assertThat(inboxService.countUnread(1L)).isEqualTo(2L);
  }

  @Test
  void countUnread_withNoUnreadItems_returnsZero() {
    when(securityUtils.isAdmin()).thenReturn(false);
    when(securityUtils.isBooker()).thenReturn(false);
    when(securityUtils.getCurrentAccountId()).thenReturn(java.util.Optional.of(1L));
    when(inboxRepository.findAll(
            any(Specification.class), any(org.springframework.data.domain.Sort.class)))
        .thenReturn(List.of());

    assertThat(inboxService.countUnread(1L)).isEqualTo(0L);
  }

  @Test
  void addInboxItem_usesFirstEventTypeFromRegistry() {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(10L);

    inboxService.addInboxItem(wrestler, "Welcome message");

    verify(inboxRepository).save(any(InboxItem.class));
  }

  @Test
  void createInboxItem_emptyTargetList_savesItemWithNoTargets() {
    InboxItem saved = inboxService.createInboxItem(testEventType, "No targets", List.of());

    assertThat(saved.getTargets()).isEmpty();
    verify(inboxRepository).save(any(InboxItem.class));
  }

  @Test
  void search_asAdmin_noFilters_returnsAllItems() {
    when(securityUtils.isAdmin()).thenReturn(true);
    when(securityUtils.isBooker()).thenReturn(false);
    when(inboxRepository.findAll(
            any(Specification.class), any(org.springframework.data.domain.Sort.class)))
        .thenReturn(List.of(item1, item2));

    List<InboxItem> result = inboxService.search(null, null, null, null, null);

    assertThat(result).hasSize(2);
  }

  @Test
  void search_hideReadFilter_appliesFilter() {
    when(securityUtils.isAdmin()).thenReturn(true);
    when(inboxRepository.findAll(
            any(Specification.class), any(org.springframework.data.domain.Sort.class)))
        .thenReturn(List.of(item1));

    List<InboxItem> result = inboxService.search(null, null, null, true, null);

    assertThat(result).hasSize(1);
  }

  @Test
  void search_withReadStatus_read_appliesFilter() {
    when(securityUtils.isAdmin()).thenReturn(true);
    when(inboxRepository.findAll(
            any(Specification.class), any(org.springframework.data.domain.Sort.class)))
        .thenReturn(List.of(item1));

    List<InboxItem> result = inboxService.search(null, "Read", null, null, null);

    assertThat(result).hasSize(1);
  }

  @Test
  void search_withEventTypeFilter_matchingType() {
    when(securityUtils.isAdmin()).thenReturn(true);
    when(inboxRepository.findAll(
            any(Specification.class), any(org.springframework.data.domain.Sort.class)))
        .thenReturn(List.of(item1));

    List<InboxItem> result = inboxService.search(null, null, "Test Event", null, null);

    assertThat(result).hasSize(1);
  }

  @Test
  void search_withEventTypeFilter_noMatch_returnsAll() {
    when(securityUtils.isAdmin()).thenReturn(true);
    when(inboxRepository.findAll(
            any(Specification.class), any(org.springframework.data.domain.Sort.class)))
        .thenReturn(List.of(item1, item2));

    // "UnknownType" won't match any registered event type, so no extra filter is applied
    List<InboxItem> result = inboxService.search(null, null, "UnknownType", null, null);

    assertThat(result).hasSize(2);
  }

  @Test
  void getInboxItemsForWrestler_delegatesToRepository() {
    Wrestler wrestler = new Wrestler();
    wrestler.setId(5L);

    when(inboxRepository.findAll(
            any(Specification.class), any(org.springframework.data.domain.Sort.class)))
        .thenReturn(List.of(item1));

    List<InboxItem> result = inboxService.getInboxItemsForWrestler(wrestler, 10);

    assertThat(result).hasSize(1);
  }

  @Test
  void getAccountRepository_returnsInjectedRepository() {
    assertThat(inboxService.getAccountRepository()).isSameAs(accountRepository);
  }
}
