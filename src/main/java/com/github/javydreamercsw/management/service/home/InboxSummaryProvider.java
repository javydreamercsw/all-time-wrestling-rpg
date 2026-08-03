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
package com.github.javydreamercsw.management.service.home;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.inbox.InboxRepository;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InboxSummaryProvider implements LandingPageSummaryProvider {

  private static final int MAX_PREVIEW_ITEMS = 5;

  private final InboxRepository inboxRepository;
  private final WrestlerService wrestlerService;

  @Override
  public String getTitle() {
    return "Inbox";
  }

  @Override
  public int getOrder() {
    return 10;
  }

  @Override
  public Set<RoleName> applicableRoles() {
    return Collections.emptySet();
  }

  @Override
  public Component buildSummaryCard(final LocalDateTime since, final Account account) {
    List<InboxItem> items = fetchItems(since, account);
    if (items.isEmpty()) {
      return null;
    }

    VerticalLayout card = new VerticalLayout();
    card.setPadding(false);
    card.setSpacing(true);

    Span countLabel = new Span(items.size() + " unread message(s)");
    countLabel.getStyle().set("font-weight", "bold");
    card.add(countLabel);

    items.stream().limit(MAX_PREVIEW_ITEMS).forEach(item -> card.add(buildItemRow(item)));

    if (items.size() > MAX_PREVIEW_ITEMS) {
      Span more = new Span("+" + (items.size() - MAX_PREVIEW_ITEMS) + " more…");
      more.getStyle().set("color", "var(--lumo-secondary-text-color)").set("font-size", "small");
      card.add(more);
    }

    return card;
  }

  private List<InboxItem> fetchItems(final LocalDateTime since, final Account account) {
    Long wrestlerId = account.getActiveWrestlerId();
    if (wrestlerId == null) {
      return wrestlerService.findAllByAccount(account).stream()
          .findFirst()
          .map(
              w -> {
                if (since != null) {
                  Instant sinceInstant = since.toInstant(ZoneOffset.UTC);
                  return inboxRepository.findUnreadForWrestlerSince(
                      w.getId().toString(), sinceInstant);
                }
                return inboxRepository.findUnreadForWrestlerSince(
                    w.getId().toString(), Instant.EPOCH);
              })
          .orElse(Collections.emptyList());
    }

    Instant sinceInstant = since != null ? since.toInstant(ZoneOffset.UTC) : Instant.EPOCH;
    return inboxRepository.findUnreadForWrestlerSince(wrestlerId.toString(), sinceInstant);
  }

  private Div buildItemRow(final InboxItem item) {
    Div row = new Div();
    String subject =
        item.getSubject() != null && !item.getSubject().isBlank()
            ? item.getSubject()
            : (item.getDescription() != null && item.getDescription().length() > 60
                ? item.getDescription().substring(0, 60) + "…"
                : item.getDescription());
    row.setText("• " + subject);
    row.getStyle().set("font-size", "small");
    return row;
  }
}
