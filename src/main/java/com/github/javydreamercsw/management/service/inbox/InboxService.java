package com.github.javydreamercsw.management.service.inbox;

import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxItemRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.event.AdjudicationCompletedEvent;
import com.github.javydreamercsw.management.event.ChampionshipChangeEvent;
import com.github.javydreamercsw.management.event.ChampionshipDefendedEvent;
import com.github.javydreamercsw.management.event.FactionHeatChangeEvent;
import com.github.javydreamercsw.management.event.FeudHeatChangeEvent;
import com.github.javydreamercsw.management.event.HeatChangeEvent;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InboxService {

  private final InboxItemRepository inboxItemRepository;
  private final TitleRepository titleRepository;
  private final WrestlerRepository wrestlerRepository;

  @EventListener
  public void handleHeatChangeEvent(HeatChangeEvent event) {
    InboxItem item = new InboxItem();
    item.setEventType("Rivalry Heat Change");
    item.setDescription(
        String.format(
            "Heat in rivalry involving %s changed from %d to %d. Reason: %s",
            event.getWrestlers().stream()
                .map(Wrestler::getName)
                .collect(Collectors.joining(" and ")),
            event.getOldHeat(),
            event.getNewHeat(),
            event.getReason()));
    item.setEventTimestamp(Instant.now());
    inboxItemRepository.save(item);
  }

  @EventListener
  public void handleFactionHeatChangeEvent(FactionHeatChangeEvent event) {
    InboxItem item = new InboxItem();
    item.setEventType("Faction Rivalry Heat Change");
    item.setDescription(
        String.format(
            "Heat in faction rivalry involving %s changed from %d to %d. Reason: %s",
            event.getWrestlers().stream()
                .map(Wrestler::getName)
                .collect(Collectors.joining(" and ")),
            event.getOldHeat(),
            event.getNewHeat(),
            event.getReason()));
    item.setEventTimestamp(Instant.now());
    inboxItemRepository.save(item);
  }

  @EventListener
  public void handleFeudHeatChangeEvent(FeudHeatChangeEvent event) {

    InboxItem item = new InboxItem();

    item.setEventType("Feud Heat Change");

    item.setDescription(
        String.format(
            "Heat in feud involving %s changed from %d to %d. Reason: %s",
            event.getWrestlers().stream().map(Wrestler::getName).collect(Collectors.joining(", ")),
            event.getOldHeat(),
            event.getNewHeat(),
            event.getReason()));

    item.setEventTimestamp(Instant.now());

    inboxItemRepository.save(item);
  }

  @EventListener
  public void handleAdjudicationCompletedEvent(AdjudicationCompletedEvent event) {

    InboxItem item = new InboxItem();

    item.setEventType("Adjudication Completed");

    item.setDescription(
        String.format("Adjudication completed for show: %s", event.getShow().getName()));

    item.setEventTimestamp(Instant.now());

    inboxItemRepository.save(item);
  }

  @EventListener
  public void handleChampionshipDefendedEvent(ChampionshipDefendedEvent event) {

    titleRepository
        .findById(event.getTitleId())
        .ifPresent(
            title -> {
              InboxItem item = new InboxItem();

              item.setEventType("Championship Defended");

              item.setDescription(
                  String.format(
                      "%s defended by %s.",
                      title.getName(),
                      event.getChampions().stream()
                          .map(Wrestler::getName)
                          .collect(Collectors.joining(", "))));

              item.setEventTimestamp(Instant.now());

              inboxItemRepository.save(item);
            });
  }

  @EventListener
  public void handleChampionshipChangeEvent(ChampionshipChangeEvent event) {

    titleRepository
        .findById(event.getTitleId())
        .ifPresent(
            title -> {
              InboxItem item = new InboxItem();

              item.setEventType("Championship Change");

              item.setDescription(
                  String.format(
                      "%s changed hands from %s to %s.",
                      title.getName(),
                      event.getOldChampions().stream()
                          .map(Wrestler::getName)
                          .collect(Collectors.joining(", ")),
                      event.getNewChampions().stream()
                          .map(Wrestler::getName)
                          .collect(Collectors.joining(", "))));

              item.setEventTimestamp(Instant.now());

              inboxItemRepository.save(item);
            });
  }

  public List<InboxItem> findAll() {
    return inboxItemRepository.findAll();
  }

  public void markAsRead(InboxItem item) {
    item.setRead(true);
    inboxItemRepository.save(item);
  }

  public void toggleReadStatus(InboxItem item) {
    item.setRead(!item.isRead());
    inboxItemRepository.save(item);
  }

  public void markSelectedAsRead(Collection<InboxItem> items) {
    items.forEach(item -> item.setRead(true));
    inboxItemRepository.saveAll(items);
  }

  public void markSelectedAsUnread(Collection<InboxItem> items) {
    items.forEach(item -> item.setRead(false));
    inboxItemRepository.saveAll(items);
  }

  public void deleteSelected(Collection<InboxItem> items) {
    inboxItemRepository.deleteAll(items);
  }

  public List<InboxItem> search(
      String filterText, String readStatus, String eventType, boolean hideRead) {
    Specification<InboxItem> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();

          if (filterText != null && !filterText.isEmpty()) {
            predicates.add(
                cb.like(cb.lower(root.get("description")), "%" + filterText.toLowerCase() + "%"));
          }

          if (hideRead) {
            predicates.add(cb.isFalse(root.get("isRead")));
          } else if (readStatus != null && !readStatus.equals("All")) {
            boolean isRead = readStatus.equals("Read");
            predicates.add(cb.equal(root.get("isRead"), isRead));
          }

          if (eventType != null && !eventType.equals("All")) {
            predicates.add(cb.equal(root.get("eventType"), eventType));
          }

          return cb.and(predicates.toArray(new Predicate[0]));
        };
    return inboxItemRepository.findAll(spec);
  }
}
