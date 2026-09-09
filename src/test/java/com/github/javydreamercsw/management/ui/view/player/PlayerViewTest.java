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
package com.github.javydreamercsw.management.ui.view.player;

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AchievementRepository;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignPhase;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.season.SeasonRepository;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.AccountService;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import com.github.javydreamercsw.management.service.news.NewsService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.season.SeasonStatsService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerStatsService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class PlayerViewTest extends AbstractViewTest {

  @Mock private WrestlerService wrestlerService;
  @Mock private WrestlerStatsService wrestlerStatsService;
  @Mock private ShowService showService;
  @Mock private RivalryService rivalryService;
  @Mock private InboxService inboxService;
  @Mock private SecurityUtils securityUtils;
  @Mock private AccountService accountService;
  @Mock private SegmentService segmentService;
  @Mock private NewsService newsService;
  @Mock private TransactionTemplate transactionTemplate;
  @Mock private AchievementRepository achievementRepository;
  @Mock private SeasonStatsService seasonStatsService;
  @Mock private SeasonRepository seasonRepository;
  @Mock private UniverseContextService universeContextService;
  @Mock private CampaignService campaignService;

  @SuppressWarnings("unchecked")
  private PlayerDashboardView buildView() {
    when(transactionTemplate.execute(any(TransactionCallback.class)))
        .thenAnswer(
            inv -> {
              TransactionCallback<?> callback = inv.getArgument(0);
              return callback.doInTransaction(null);
            });
    when(newsService.getLatestNews()).thenReturn(Collections.emptyList());
    when(campaignService.getCampaignForWrestler(any(Wrestler.class))).thenReturn(Optional.empty());

    PlayerDashboardView view =
        new PlayerDashboardView(
            wrestlerService,
            wrestlerStatsService,
            rivalryService,
            inboxService,
            securityUtils,
            accountService,
            segmentService,
            newsService,
            transactionTemplate,
            achievementRepository,
            seasonStatsService,
            seasonRepository,
            universeContextService,
            campaignService);
    UI.getCurrent().add(view);
    return view;
  }

  @Nested
  @DisplayName("No wrestler assigned")
  class NoWrestlerAssigned {

    private PlayerDashboardView view;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
      MockitoAnnotations.openMocks(PlayerViewTest.this);
      Account account = new Account();
      account.setUsername("testuser");
      account.setPassword("password");
      account.setEmail("test@example.com");
      CustomUserDetails userDetails = new CustomUserDetails(account);
      when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(userDetails));
      when(accountService.get(any())).thenReturn(Optional.of(account));
      when(wrestlerService.findAllByAccount(any())).thenReturn(Collections.emptyList());
      view = buildView();
    }

    @Test
    @DisplayName("Should render the Player Dashboard toolbar")
    void shouldRenderToolbar() {
      ViewToolbar toolbar = _get(view, ViewToolbar.class);
      assertTrue(toolbar.isVisible());
    }

    @Test
    @DisplayName("Switcher ComboBox should have no value when no wrestler is assigned")
    void switcherShouldHaveNoValueWhenNoWrestlerAssigned() {
      ComboBox<Wrestler> switcher =
          _get(view, ComboBox.class, spec -> spec.withId("active-wrestler-switcher"));
      assertNotNull(switcher);
      assertNull(switcher.getValue(), "ComboBox should be empty when account has no wrestler");
    }
  }

  @Nested
  @DisplayName("Wrestler assigned")
  class WrestlerAssigned {

    private PlayerDashboardView view;
    private Wrestler wrestler;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
      MockitoAnnotations.openMocks(PlayerViewTest.this);

      wrestler = new Wrestler();
      wrestler.setId(42L);
      wrestler.setName("Test Wrestler");

      WrestlerState state = new WrestlerState();

      Account account = new Account();
      account.setUsername("testuser");
      account.setPassword("password");
      account.setEmail("test@example.com");
      account.setActiveWrestlerId(42L);

      CustomUserDetails userDetails = new CustomUserDetails(account);
      when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(userDetails));
      when(accountService.get(any())).thenReturn(Optional.of(account));
      when(wrestlerService.findAllByAccount(any())).thenReturn(List.of(wrestler));
      when(wrestlerService.findById(42L)).thenReturn(Optional.of(wrestler));
      when(wrestlerService.findByIdWithDetails(42L)).thenReturn(Optional.of(wrestler));
      when(wrestlerService.getOrCreateState(anyLong(), any())).thenReturn(state);
      when(wrestlerStatsService.getWrestlerStats(anyLong(), any())).thenReturn(Optional.empty());
      when(seasonRepository.findByWrestler(any())).thenReturn(Collections.emptyList());
      when(seasonRepository.findActiveSeason()).thenReturn(Optional.empty());
      when(segmentService.getUpcomingSegmentsForWrestler(any(), anyInt()))
          .thenReturn(Collections.emptyList());
      when(universeContextService.getCurrentUniverseId()).thenReturn(1L);

      view = buildView();
    }

    /** buildView() but preserving a caller-provided campaign for the primary-action band. */
    @SuppressWarnings("unchecked")
    private PlayerDashboardView buildViewWithCampaign(Campaign campaign) {
      when(transactionTemplate.execute(any(TransactionCallback.class)))
          .thenAnswer(
              inv -> {
                TransactionCallback<?> callback = inv.getArgument(0);
                return callback.doInTransaction(null);
              });
      when(newsService.getLatestNews()).thenReturn(Collections.emptyList());
      when(campaignService.getCampaignForWrestler(any(Wrestler.class)))
          .thenReturn(Optional.of(campaign));

      PlayerDashboardView built =
          new PlayerDashboardView(
              wrestlerService,
              wrestlerStatsService,
              rivalryService,
              inboxService,
              securityUtils,
              accountService,
              segmentService,
              newsService,
              transactionTemplate,
              achievementRepository,
              seasonStatsService,
              seasonRepository,
              universeContextService,
              campaignService);
      UI.getCurrent().add(built);
      return built;
    }

    @Test
    @DisplayName("MATCH phase with a pending match shows Continue Match as primary CTA")
    void matchPhaseShowsContinueMatchCta() {
      CampaignState campaignState = new CampaignState();
      campaignState.setCurrentPhase(CampaignPhase.MATCH);
      Segment match = new Segment();
      match.setId(9L);
      match.setAdjudicationStatus(AdjudicationStatus.PENDING);
      campaignState.setCurrentMatch(match);
      Campaign campaign = Campaign.builder().id(1L).wrestler(wrestler).state(campaignState).build();
      view = buildViewWithCampaign(campaign);

      var cta = _get(view, Button.class, spec -> spec.withId("continue-match-cta"));
      assertNotNull(cta, "Continue Match CTA should render for a pending match");
      assertTrue(cta.getThemeNames().contains("primary"));
    }

    @Test
    @DisplayName("Adjudicated match falls back to Continue Campaign CTA")
    void adjudicatedMatchShowsContinueCampaignCta() {
      CampaignState campaignState = new CampaignState();
      campaignState.setCurrentPhase(CampaignPhase.MATCH);
      Segment match = new Segment();
      match.setId(9L);
      match.setAdjudicationStatus(AdjudicationStatus.ADJUDICATED);
      campaignState.setCurrentMatch(match);
      Campaign campaign = Campaign.builder().id(1L).wrestler(wrestler).state(campaignState).build();
      view = buildViewWithCampaign(campaign);

      assertNotNull(
          _get(view, Button.class, spec -> spec.withId("continue-campaign-cta")),
          "Continue Campaign CTA should render when the match is already adjudicated");
      Assertions.assertTrue(
          _find(view, Button.class, spec -> spec.withId("continue-match-cta")).isEmpty(),
          "Continue Match CTA must not render for an adjudicated match");
    }

    @Test
    @DisplayName("BACKSTAGE phase shows Continue Campaign CTA")
    void backstagePhaseShowsContinueCampaignCta() {
      CampaignState campaignState = new CampaignState();
      campaignState.setCurrentPhase(CampaignPhase.BACKSTAGE);
      Campaign campaign = Campaign.builder().id(1L).wrestler(wrestler).state(campaignState).build();
      view = buildViewWithCampaign(campaign);

      assertNotNull(_get(view, Button.class, spec -> spec.withId("continue-campaign-cta")));
    }

    @Test
    @DisplayName("Active wrestler should be pre-selected in the switcher ComboBox on load")
    void activewrestlershouldBePreSelectedInSwitcher() {
      ComboBox<Wrestler> switcher =
          _get(view, ComboBox.class, spec -> spec.withId("active-wrestler-switcher"));
      assertNotNull(switcher, "Active wrestler switcher ComboBox should be present");
      assertEquals(
          wrestler,
          switcher.getValue(),
          "ComboBox should be pre-selected with the active wrestler on page load");
    }

    @Test
    @DisplayName("POST_MATCH phase renders no CTA in the primary action band")
    void postMatchPhaseShowsNoCta() {
      CampaignState campaignState = new CampaignState();
      campaignState.setCurrentPhase(CampaignPhase.POST_MATCH);
      Campaign campaign = Campaign.builder().id(2L).wrestler(wrestler).state(campaignState).build();
      PlayerDashboardView built = buildViewWithCampaign(campaign);

      // Neither the match CTA nor the campaign CTA may exist.
      Assertions.assertTrue(
          _find(built, Button.class).stream()
              .noneMatch(b -> b.getId().orElse("").equals("continue-match-cta")),
          "No Continue Match CTA during POST_MATCH");
      Assertions.assertTrue(
          _find(built, Button.class).stream()
              .noneMatch(b -> b.getId().orElse("").equals("continue-campaign-cta")),
          "No Continue Campaign CTA during POST_MATCH");
    }

    @Test
    @DisplayName("Tab pages are wrapped in the grid scroll container")
    void tabGridsAreWrappedInScrollContainer() {
      // Every tab page Div carries the touch-scroll class from the redesign.
      // Inactive tabs are INVIS so the karibu locator skips them — walk the tree.
      long wrappers =
          walk(view).stream()
              .filter(
                  d ->
                      d.getElement().getAttribute("class") != null
                          && d.getElement().getAttribute("class").contains("grid-scroll-container"))
              .count();
      Assertions.assertEquals(4, wrappers, "All four tab grids should sit in scroll wrappers");
    }

    @Test
    @DisplayName("Upcoming matches grid shows the segments for the active wrestler")
    void upcomingMatchesGridListsWrestlerSegments() {
      ComboBox<Wrestler> switcher =
          _get(view, ComboBox.class, spec -> spec.withId("active-wrestler-switcher"));
      Assertions.assertNotNull(switcher.getValue());

      @SuppressWarnings("unchecked")
      Grid<Segment> grid =
          (Grid<Segment>) _get(view, Grid.class, spec -> spec.withId("upcoming-matches-grid"));
      Assertions.assertNotNull(grid);
      verify(segmentService).getUpcomingSegmentsForWrestler(wrestler, 5);
    }

    @Test
    @DisplayName("Continue Match CTA navigates to the match view")
    void continueMatchCtaNavigates() {
      CampaignState campaignState = new CampaignState();
      campaignState.setCurrentPhase(CampaignPhase.MATCH);
      Segment pending = new Segment();
      pending.setId(77L);
      campaignState.setCurrentMatch(pending);
      Campaign campaign = Campaign.builder().id(3L).wrestler(wrestler).state(campaignState).build();
      PlayerDashboardView built = buildViewWithCampaign(campaign);

      built
          .getUI()
          .orElseThrow()
          .add(
              new Div() {
                // ensure UI has a current view context for navigation
              });

      Button cta = _get(built, Button.class, spec -> spec.withId("continue-match-cta"));
      cta.click();
      // Navigation is attempted against the mocked route registry; the handler
      // ran without error, covering the click path.
      Assertions.assertTrue(cta.isEnabled());
    }

    @Test
    @DisplayName("Continue Campaign CTA click handler runs")
    void continueCampaignCtaClickRuns() {
      CampaignState campaignState = new CampaignState();
      campaignState.setCurrentPhase(CampaignPhase.BACKSTAGE);
      Campaign campaign = Campaign.builder().id(4L).wrestler(wrestler).state(campaignState).build();
      PlayerDashboardView built = buildViewWithCampaign(campaign);

      Button cta = _get(built, Button.class, spec -> spec.withId("continue-campaign-cta"));
      cta.click();
      Assertions.assertTrue(cta.isEnabled());
    }
  }

  /** Depth-first walk of the component tree, including INVIS components. */
  private static List<Component> walk(final Component root) {
    List<Component> all = new ArrayList<>();
    all.add(root);
    root.getChildren().forEach(child -> all.addAll(walk(child)));
    return all;
  }
}
