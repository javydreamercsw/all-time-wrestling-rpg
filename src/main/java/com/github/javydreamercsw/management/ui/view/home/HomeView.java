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
package com.github.javydreamercsw.management.ui.view.home;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.news.NewsItem;
import com.github.javydreamercsw.management.service.home.LandingPageSummaryProvider;
import com.github.javydreamercsw.management.service.news.NewsService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Route(value = "", layout = com.github.javydreamercsw.management.ui.view.MainLayout.class)
@PageTitle("Home")
@PermitAll
@Slf4j
public class HomeView extends VerticalLayout {

  private static final int MAX_NEWS_ITEMS = 5;
  private static final DateTimeFormatter DATE_FMT =
      DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);

  public HomeView(
      final List<LandingPageSummaryProvider> summaryProviders,
      final NewsService newsService,
      final SecurityUtils securityUtils) {

    addClassName("home-view");
    setPadding(true);
    setSpacing(true);

    Optional<CustomUserDetails> userOpt = securityUtils.getAuthenticatedUser();
    if (userOpt.isEmpty()) {
      add(new Paragraph("Please log in to see your dashboard."));
      return;
    }

    Account account = userOpt.get().getAccount();

    add(buildWelcomeHeader(account));

    if (account.getPreviousLastLogin() != null) {
      add(buildSinceLastVisitSection(summaryProviders, account, securityUtils));
    }

    add(buildNewsSection(newsService));
  }

  private Component buildWelcomeHeader(final Account account) {
    VerticalLayout header = new VerticalLayout();
    header.setPadding(false);
    header.setSpacing(false);

    H2 welcome = new H2("Welcome back, " + account.getUsername() + "!");
    header.add(welcome);

    if (account.getPreviousLastLogin() != null) {
      String lastVisit =
          account.getPreviousLastLogin().atZone(ZoneId.systemDefault()).format(DATE_FMT);
      Span lastSeen = new Span("Last visit: " + lastVisit);
      lastSeen.getStyle().set("color", "var(--lumo-secondary-text-color)");
      header.add(lastSeen);
    }

    return header;
  }

  private Component buildSinceLastVisitSection(
      final List<LandingPageSummaryProvider> providers,
      final Account account,
      final SecurityUtils securityUtils) {

    VerticalLayout section = new VerticalLayout();
    section.setPadding(false);
    section.setSpacing(true);
    section.addClassName("since-last-visit-section");

    H3 heading = new H3("Since Your Last Visit");
    section.add(heading);

    Set<RoleName> userRoles = resolveRoles(securityUtils);

    boolean anyCard =
        providers.stream()
            .filter(p -> isApplicable(p, userRoles))
            .sorted((a, b) -> Integer.compare(a.getOrder(), b.getOrder()))
            .map(
                p -> {
                  try {
                    Component card = p.buildSummaryCard(account.getPreviousLastLogin(), account);
                    if (card != null) {
                      section.add(buildProviderCard(p.getTitle(), card));
                      return true;
                    }
                  } catch (Exception e) {
                    log.warn(
                        "Summary provider {} failed: {}",
                        p.getClass().getSimpleName(),
                        e.getMessage());
                  }
                  return false;
                })
            .reduce(false, Boolean::logicalOr);

    if (!anyCard) {
      section.add(new Paragraph("Nothing new since your last visit."));
    }

    return section;
  }

  private Component buildProviderCard(final String title, final Component content) {
    VerticalLayout card = new VerticalLayout();
    card.addClassName("summary-card");
    card.getStyle()
        .set("border", "1px solid var(--lumo-contrast-10pct)")
        .set("border-radius", "var(--lumo-border-radius-m)")
        .set("padding", "var(--lumo-space-m)");

    H4 cardTitle = new H4(title);
    cardTitle.getStyle().set("margin", "0 0 var(--lumo-space-xs) 0");
    card.add(cardTitle, content);
    return card;
  }

  private Component buildNewsSection(final NewsService newsService) {
    VerticalLayout section = new VerticalLayout();
    section.setPadding(false);
    section.setSpacing(true);
    section.addClassName("news-section");

    HorizontalLayout header = new HorizontalLayout();
    header.setWidthFull();
    header.setJustifyContentMode(JustifyContentMode.BETWEEN);
    header.setAlignItems(Alignment.CENTER);

    H3 heading = new H3("News & Rumors");
    heading.getStyle().set("margin", "0");

    Button seeAll = new Button("See All →", e -> UI.getCurrent().navigate("news"));
    seeAll.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
    header.add(heading, seeAll);
    section.add(header);

    List<NewsItem> news = newsService.getLatestNews();
    if (news.isEmpty()) {
      section.add(new Paragraph("No news yet. Play some shows to generate headlines!"));
    } else {
      news.stream().limit(MAX_NEWS_ITEMS).forEach(item -> section.add(buildNewsCard(item)));
    }

    return section;
  }

  private Component buildNewsCard(final NewsItem item) {
    Div card = new Div();
    card.addClassName("news-card");
    card.getStyle()
        .set("border-bottom", "1px solid var(--lumo-contrast-10pct)")
        .set("padding", "var(--lumo-space-s) 0");

    HorizontalLayout meta = new HorizontalLayout();
    meta.setSpacing(true);
    meta.setPadding(false);

    if (item.getCategory() != null) {
      Span category = new Span(item.getCategory().name());
      category
          .getElement()
          .getThemeList()
          .addAll(
              List.of("badge", item.getIsRumor() != null && item.getIsRumor() ? "contrast" : ""));
      meta.add(category);
    }

    if (item.getPublishDate() != null) {
      String date =
          item.getPublishDate().atZone(ZoneId.systemDefault()).toLocalDate().format(DATE_FMT);
      Span dateBadge = new Span(date);
      dateBadge
          .getStyle()
          .set("color", "var(--lumo-secondary-text-color)")
          .set("font-size", "small");
      meta.add(dateBadge);
    }

    Div headline = new Div();
    headline.getStyle().set("font-weight", "bold");
    headline.setText(item.getHeadline());

    card.add(meta, headline);
    return card;
  }

  private boolean isApplicable(
      final LandingPageSummaryProvider provider, final Set<RoleName> userRoles) {
    Set<RoleName> applicable = provider.applicableRoles();
    return applicable.isEmpty() || applicable.stream().anyMatch(userRoles::contains);
  }

  private Set<RoleName> resolveRoles(final SecurityUtils securityUtils) {
    java.util.EnumSet<RoleName> roles = java.util.EnumSet.noneOf(RoleName.class);
    if (securityUtils.isAdmin()) roles.add(RoleName.ADMIN);
    if (securityUtils.isBooker()) roles.add(RoleName.BOOKER);
    if (securityUtils.isPlayer()) roles.add(RoleName.PLAYER);
    if (securityUtils.isViewer()) roles.add(RoleName.VIEWER);
    return roles;
  }
}
