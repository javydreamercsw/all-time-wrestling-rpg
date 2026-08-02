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

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.news.NewsCategory;
import com.github.javydreamercsw.management.domain.news.NewsItem;
import com.github.javydreamercsw.management.service.home.LandingPageSummaryProvider;
import com.github.javydreamercsw.management.service.news.NewsService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class HomeViewTest extends AbstractViewTest {

  @Mock private SecurityUtils securityUtils;
  @Mock private NewsService newsService;

  private Account testAccount;

  @BeforeEach
  void setup() {
    testAccount = new Account("testuser", "pass", "test@test.com");
    testAccount.setId(99L);
    CustomUserDetails userDetails = new CustomUserDetails(testAccount, null);

    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(userDetails));
    when(securityUtils.isAdmin()).thenReturn(true);
    when(securityUtils.isBooker()).thenReturn(false);
    when(securityUtils.isPlayer()).thenReturn(false);
    when(securityUtils.isViewer()).thenReturn(false);
    when(newsService.getLatestNews()).thenReturn(Collections.emptyList());
  }

  private HomeView buildView(final List<LandingPageSummaryProvider> providers) {
    HomeView view = new HomeView(providers, newsService, securityUtils);
    UI.getCurrent().add(view);
    return view;
  }

  @Test
  @DisplayName("Unauthenticated user sees login prompt instead of dashboard")
  void unauthenticatedUserSeesLoginPrompt() {
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.empty());
    HomeView view = buildView(Collections.emptyList());

    Paragraph prompt = _get(view, Paragraph.class);
    assertTrue(prompt.getText().contains("Please log in"));
  }

  @Test
  @DisplayName("Welcome header shows correct username")
  void welcomeHeaderShowsUsername() {
    HomeView view = buildView(Collections.emptyList());

    H2 heading = _get(view, H2.class);
    assertTrue(heading.getText().contains("testuser"));
  }

  @Test
  @DisplayName("First login: no Since Your Last Visit section when previousLastLogin is null")
  void firstLoginHidesLastVisitSection() {
    HomeView view = buildView(Collections.emptyList());

    List<H3> h3s = _find(view, H3.class);
    boolean hasLastVisit =
        h3s.stream().anyMatch(h -> h.getText().contains("Since Your Last Visit"));
    assertFalse(hasLastVisit, "Should not show Since Your Last Visit on first login");
  }

  @Test
  @DisplayName("Return login: Since Your Last Visit section appears when previousLastLogin is set")
  void returnLoginShowsLastVisitSection() {
    testAccount.setPreviousLastLogin(LocalDateTime.now().minusDays(1));
    HomeView view = buildView(Collections.emptyList());

    List<H3> h3s = _find(view, H3.class);
    boolean hasLastVisit =
        h3s.stream().anyMatch(h -> h.getText().contains("Since Your Last Visit"));
    assertTrue(hasLastVisit, "Should show Since Your Last Visit on return login");
  }

  @Test
  @DisplayName("Return login: last visit date appears in welcome header")
  void returnLoginShowsLastVisitDate() {
    testAccount.setPreviousLastLogin(LocalDateTime.now().minusDays(3));
    HomeView view = buildView(Collections.emptyList());

    List<Span> spans = _find(view, Span.class);
    boolean hasLastVisit = spans.stream().anyMatch(s -> s.getText().startsWith("Last visit:"));
    assertTrue(hasLastVisit, "Should show last visit date in welcome header");
  }

  @Test
  @DisplayName("Provider returning null shows Nothing new placeholder")
  void providerReturningNullShowsPlaceholder() {
    testAccount.setPreviousLastLogin(LocalDateTime.now().minusDays(1));

    LandingPageSummaryProvider nullProvider = mock(LandingPageSummaryProvider.class);
    when(nullProvider.getTitle()).thenReturn("Test Provider");
    when(nullProvider.getOrder()).thenReturn(1);
    when(nullProvider.applicableRoles()).thenReturn(Collections.emptySet());
    when(nullProvider.buildSummaryCard(any(), any())).thenReturn(null);

    HomeView view = buildView(List.of(nullProvider));

    List<Paragraph> paragraphs = _find(view, Paragraph.class);
    boolean hasNothingNew =
        paragraphs.stream()
            .anyMatch(p -> p.getText().contains("Nothing new since your last visit"));
    assertTrue(hasNothingNew);
  }

  @Test
  @DisplayName("Provider returning a component shows card with its title")
  void providerReturningComponentShowsCard() {
    testAccount.setPreviousLastLogin(LocalDateTime.now().minusDays(1));

    LandingPageSummaryProvider provider = mock(LandingPageSummaryProvider.class);
    when(provider.getTitle()).thenReturn("Inbox Summary");
    when(provider.getOrder()).thenReturn(1);
    when(provider.applicableRoles()).thenReturn(Collections.emptySet());
    when(provider.buildSummaryCard(any(), any())).thenReturn(new Span("3 new inbox items"));

    HomeView view = buildView(List.of(provider));

    List<H4> cardTitles = _find(view, H4.class);
    boolean hasCardTitle = cardTitles.stream().anyMatch(h -> h.getText().equals("Inbox Summary"));
    assertTrue(hasCardTitle, "Should render provider card with its title");
  }

  @Test
  @DisplayName("Role-filtered provider is not shown to user without required role")
  void roleFilteredProviderHiddenForWrongRole() {
    testAccount.setPreviousLastLogin(LocalDateTime.now().minusDays(1));
    when(securityUtils.isAdmin()).thenReturn(false);
    when(securityUtils.isPlayer()).thenReturn(false);

    LandingPageSummaryProvider playerOnlyProvider = mock(LandingPageSummaryProvider.class);
    when(playerOnlyProvider.getTitle()).thenReturn("Player Stats");
    when(playerOnlyProvider.getOrder()).thenReturn(1);
    when(playerOnlyProvider.applicableRoles()).thenReturn(Set.of(RoleName.PLAYER));
    when(playerOnlyProvider.buildSummaryCard(any(), any())).thenReturn(new Span("player content"));

    HomeView view = buildView(List.of(playerOnlyProvider));

    List<H4> cardTitles = _find(view, H4.class);
    boolean hasPlayerCard = cardTitles.stream().anyMatch(h -> h.getText().equals("Player Stats"));
    assertFalse(hasPlayerCard, "Player-only card should not appear for non-player user");
  }

  @Test
  @DisplayName("News section shows placeholder when there are no news items")
  void newsSectionShowsPlaceholderWhenEmpty() {
    when(newsService.getLatestNews()).thenReturn(Collections.emptyList());
    HomeView view = buildView(Collections.emptyList());

    H3 newsHeading =
        _find(view, H3.class).stream()
            .filter(h -> h.getText().contains("News"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("News section heading not found"));
    assertTrue(newsHeading.isVisible());

    List<Paragraph> paragraphs = _find(view, Paragraph.class);
    boolean hasNoNews = paragraphs.stream().anyMatch(p -> p.getText().contains("No news yet"));
    assertTrue(hasNoNews, "Should show No news yet placeholder when list is empty");
  }

  @Test
  @DisplayName("News section shows at most 5 items even when service returns more")
  void newsSectionCapsAtFiveItems() {
    when(newsService.getLatestNews()).thenReturn(buildNewsItems(7));
    HomeView view = buildView(Collections.emptyList());

    List<Div> newsCards = _find(view, Div.class, spec -> spec.withClasses("news-card"));
    assertEquals(5, newsCards.size(), "Should show at most 5 news items");
  }

  private List<NewsItem> buildNewsItems(final int count) {
    return IntStream.range(0, count)
        .mapToObj(
            i ->
                NewsItem.builder()
                    .headline("Headline " + i)
                    .category(NewsCategory.BREAKING)
                    .isRumor(false)
                    .publishDate(Instant.now())
                    .build())
        .toList();
  }
}
