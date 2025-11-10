package com.github.javydreamercsw.management.service.inbox;

import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.List;
import java.util.Set;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class InboxService {

  @Autowired private InboxRepository inboxRepository;

  public InboxItem createInboxItem(@NonNull String message, @NonNull String referenceId) {
    InboxItem inboxItem = new InboxItem();
    inboxItem.setDescription(message);
    inboxItem.setEventType("FanAdjudication"); // Default event type for now
    inboxItem.setReferenceId(referenceId);
    return inboxRepository.save(inboxItem);
  }

  public void markSelectedAsRead(@NonNull Set<InboxItem> inboxItems) {
    inboxItems.forEach(item -> item.setRead(true));
    inboxRepository.saveAll(inboxItems);
  }

  public void markSelectedAsUnread(@NonNull Set<InboxItem> inboxItems) {
    inboxItems.forEach(item -> item.setRead(false));
    inboxRepository.saveAll(inboxItems);
  }

  public void deleteSelected(@NonNull Set<InboxItem> inboxItems) {
    inboxRepository.deleteAll(inboxItems);
  }

  public InboxItem toggleReadStatus(@NonNull InboxItem inboxItem) {
    inboxItem.setRead(!inboxItem.isRead());
    return inboxRepository.save(inboxItem);
  }

  public List<InboxItem> findAll(Specification<InboxItem> spec, Pageable pageable) {
    return inboxRepository.findAll(spec, pageable).getContent();
  }

  public List<InboxItem> search(
      String filter, String readStatus, String eventType, Boolean hideRead) {
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

          if (filter != null && !filter.isEmpty()) {
            predicate =
                cb.and(
                    predicate,
                    cb.like(cb.lower(root.get("description")), "%" + filter.toLowerCase() + "%"));
          }

          if (eventType != null && !eventType.equalsIgnoreCase("All")) {
            predicate = cb.and(predicate, cb.equal(root.get("eventType"), eventType));
          }

          return predicate;
        };

    Sort sort = Sort.by(Sort.Direction.DESC, "eventTimestamp");

    return inboxRepository.findAll(spec, sort);
  }

  public List<InboxItem> list(@NonNull Pageable pageable) {
    return inboxRepository.findAll(pageable).toList();
  }

  public long count() {
    return inboxRepository.count();
  }
}
