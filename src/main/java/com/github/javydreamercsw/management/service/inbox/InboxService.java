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

import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxEventTypeRegistry;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget;
import com.github.javydreamercsw.management.domain.inbox.InboxRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class InboxService {

  private final InboxRepository inboxRepository;
  private final InboxEventTypeRegistry eventTypeRegistry;
  @Getter private final AccountRepository accountRepository;

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public InboxItem createInboxItem(
      @NonNull InboxEventType eventType,
      @NonNull String message,
      @NonNull String referenceId,
      @NonNull InboxItemTarget.TargetType type) {
    return createInboxItem(eventType, message, List.of(new TargetInfo(referenceId, type)));
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public InboxItem createInboxItem(
      @NonNull InboxEventType eventType,
      @NonNull String message,
      @NonNull List<TargetInfo> targets) {
    InboxItem inboxItem = new InboxItem();
    inboxItem.setDescription(message);
    inboxItem.setEventType(eventType);
    targets.forEach(t -> inboxItem.addTarget(t.targetId(), t.type()));
    return inboxRepository.save(inboxItem);
  }

  public record TargetInfo(String targetId, InboxItemTarget.TargetType type) {}

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER') or @permissionService.isOwner(#inboxItems)")
  public void markSelectedAsRead(@NonNull Set<InboxItem> inboxItems) {
    inboxItems.forEach(item -> item.setRead(true));
    inboxRepository.saveAll(inboxItems);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER') or @permissionService.isOwner(#inboxItems)")
  public void markSelectedAsUnread(@NonNull Set<InboxItem> inboxItems) {
    inboxItems.forEach(item -> item.setRead(false));
    inboxRepository.saveAll(inboxItems);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER') or @permissionService.isOwner(#inboxItems)")
  public void deleteSelected(@NonNull Set<InboxItem> inboxItems) {
    inboxRepository.deleteAll(inboxItems);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER') or @permissionService.isOwner(#inboxItem)")
  public InboxItem toggleReadStatus(@NonNull InboxItem inboxItem) {
    inboxItem.setRead(!inboxItem.isRead());
    return inboxRepository.save(inboxItem);
  }

  @PreAuthorize("isAuthenticated()")
  public List<InboxItem> findAll(Specification<InboxItem> spec, Pageable pageable) {
    return inboxRepository.findAll(spec, pageable).getContent();
  }

  @PreAuthorize("isAuthenticated()")
  public List<InboxItem> search(
      Set<Wrestler> targets, String readStatus, String eventType, Boolean hideRead) {
    Specification<InboxItem> spec =
        (root, query, cb) -> {
          Predicate predicate = cb.conjunction();

          if (hideRead != null && hideRead) {
            predicate = cb.and(predicate, cb.isFalse(root.get("isRead")));
          }

          if (readStatus != null && !readStatus.equalsIgnoreCase("All")) {
            boolean isRead = readStatus.equalsIgnoreCase("Read");
            predicate = cb.and(predicate, cb.equal(root.get("isRead"), isRead));
          }

          if (targets != null && !targets.isEmpty()) {
            Join<Object, Object> join = root.join("targets", JoinType.INNER);
            predicate =
                cb.and(
                    predicate,
                    join.get("targetId")
                        .in(
                            targets.stream()
                                .map(wrestler -> wrestler.getId().toString())
                                .toList()));
          }

          if (eventType != null && !eventType.equalsIgnoreCase("All")) {
            // Find the enum by its friendly name
            InboxEventType foundEventType =
                eventTypeRegistry.getEventTypes().stream()
                    .filter(e -> e.getFriendlyName().equalsIgnoreCase(eventType))
                    .findFirst()
                    .orElse(null);
            if (foundEventType != null) {
              predicate = cb.and(predicate, cb.equal(root.get("eventType"), foundEventType));
            }
          }

          return predicate;
        };

    Sort sort = Sort.by(Sort.Direction.DESC, "eventTimestamp");

    return inboxRepository.findAll(spec, sort);
  }

  @PreAuthorize("isAuthenticated()")
  public List<InboxItem> list(@NonNull Pageable pageable) {
    return inboxRepository.findAll(pageable).toList();
  }

  @PreAuthorize("isAuthenticated()")
  public long count() {
    return inboxRepository.count();
  }

  @PreAuthorize("isAuthenticated()")
  public List<InboxItem> getInboxItemsForWrestler(@NonNull Wrestler wrestler, int limit) {
    Specification<InboxItem> spec =
        (root, query, cb) -> {
          Predicate predicate = cb.conjunction();
          predicate = cb.and(predicate, cb.isFalse(root.get("isRead")));
          Join<Object, Object> join = root.join("targets", JoinType.INNER);
          predicate =
              cb.and(predicate, join.get("targetId").in(List.of(wrestler.getId().toString())));
          return predicate;
        };

    Sort sort = Sort.by(Sort.Direction.DESC, "eventTimestamp");
    Pageable pageable = Pageable.ofSize(limit).withPage(0);

    return inboxRepository.findAll(spec, sort);
  }

  @PreAuthorize("hasAnyRole('ADMIN', 'BOOKER')")
  public InboxItem addInboxItem(@NonNull Wrestler wrestler, @NonNull String message) {
    InboxEventType eventType = eventTypeRegistry.getEventTypes().get(0);
    return createInboxItem(
        eventType, message, wrestler.getId().toString(), InboxItemTarget.TargetType.WRESTLER);
  }
}
