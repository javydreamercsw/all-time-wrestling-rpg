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
package com.github.javydreamercsw.management.ui.view.wrestler;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.image.ImageStorageService;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.service.account.AccountService;
import com.github.javydreamercsw.base.ui.component.ViewToolbar;
import com.github.javydreamercsw.management.domain.title.TitleOpportunity;
import com.github.javydreamercsw.management.domain.title.TitleOpportunityStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerAbilityRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerStateRepository;
import com.github.javydreamercsw.management.service.campaign.AlignmentService;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.campaign.StatusCardService;
import com.github.javydreamercsw.management.service.campaign.WrestlerStatusService;
import com.github.javydreamercsw.management.service.feud.MultiWrestlerFeudService;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.injury.InjuryTypeService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.ranking.RankingService;
import com.github.javydreamercsw.management.service.relationship.WrestlerRelationshipService;
import com.github.javydreamercsw.management.service.rivalry.RivalryService;
import com.github.javydreamercsw.management.service.season.SeasonService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.show.ShowFacade;
import com.github.javydreamercsw.management.service.title.TitleOpportunityService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerFacade;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerStatsService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.accordion.Accordion;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.data.domain.Page;

class WrestlerProfileViewTest extends AbstractViewTest {

  @Mock private WrestlerService wrestlerService;
  @Mock private WrestlerStatsService wrestlerStatsService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private TitleService titleService;
  @Mock private RankingService rankingService;
  @Mock private SegmentService segmentService;
  @Mock private MultiWrestlerFeudService multiWrestlerFeudService;
  @Mock private RivalryService rivalryService;
  @Mock private SeasonService seasonService;
  @Mock private InjuryService injuryService;
  @Mock private InjuryTypeService injuryTypeService;
  @Mock private NpcService npcService;
  @Mock private AccountService accountService;
  @Mock private CampaignService campaignService;
  @Mock private ImageStorageService imageStorageService;
  @Mock private UniverseContextService universeContextService;
  @Mock private WrestlerRelationshipService relationshipService;
  @Mock private WrestlerStatusService wrestlerStatusService;
  @Mock private StatusCardService statusCardService;
  @Mock private WrestlerStateRepository wrestlerStateRepository;

  @Mock private WrestlerAbilityRepository wrestlerAbilityRepository;

  @Mock private AlignmentService alignmentService;
  @Mock private SecurityUtils securityUtils;
  @Mock private WrestlerFacade wrestlerFacade;
  @Mock private ShowFacade showFacade;
  @Mock private TitleOpportunityService titleOpportunityService;

  private WrestlerProfileView view;
  private Wrestler wrestler;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setup() {
    when(seasonService.getAllSeasons(any())).thenReturn(Page.empty());

    wrestler = new Wrestler();
    wrestler.setId(1L);
    wrestler.setName("Test Wrestler");
    WrestlerState state = new WrestlerState();
    state.setFans(5000L);

    // Facade wiring for the briefcase panel (ATW-312z).
    when(wrestlerFacade.getTitleOpportunityService()).thenReturn(titleOpportunityService);
    when(titleOpportunityService.findByWrestler(anyLong())).thenReturn(List.of());
    com.github.javydreamercsw.management.service.show.ShowService careerShowService =
        mock(com.github.javydreamercsw.management.service.show.ShowService.class);
    when(showFacade.getShowService()).thenReturn(careerShowService);
    when(careerShowService.getUpcomingShows(50)).thenReturn(List.of());

    view =
        new WrestlerProfileView(
            wrestlerService,
            wrestlerStatsService,
            wrestlerRepository,
            titleService,
            rankingService,
            segmentService,
            multiWrestlerFeudService,
            rivalryService,
            seasonService,
            injuryService,
            injuryTypeService,
            npcService,
            accountService,
            campaignService,
            imageStorageService,
            universeContextService,
            relationshipService,
            wrestlerStatusService,
            statusCardService,
            wrestlerStateRepository,
            wrestlerAbilityRepository,
            alignmentService,
            wrestlerFacade,
            showFacade);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the wrestler profile toolbar")
  void shouldRenderToolbar() {
    ViewToolbar toolbar = _get(view, ViewToolbar.class);
    assertTrue(toolbar.isVisible());
  }

  // ── Briefcase panel + hero link (ATW-312z) ───────────────────────────────

  @Test
  @DisplayName("Profile has a Briefcase accordion panel next to Championships")
  void briefcasePanel_existsNextToChampionships() {
    Accordion accordion = _get(view, Accordion.class);
    // AccordionPanel extends Details; the summary text is the panel title.
    List<String> summaries =
        accordion
            .getChildren()
            .map(
                panel ->
                    ((com.vaadin.flow.component.accordion.AccordionPanel) panel).getSummaryText())
            .toList();
    String panels = String.join(" | ", summaries);
    assertTrue(
        panels.contains("Briefcase"),
        "A Briefcase panel should exist in the profile accordion; got: " + panels);
    assertTrue(
        panels.indexOf("Championships") < panels.indexOf("Briefcase"),
        "Briefcase should sit right after Championships; got: " + panels);
  }

  @Test
  @DisplayName("Held briefcase renders inside the profile with Cash In button")
  void briefcasePanel_heldCase_showsCashInButton() {
    TitleOpportunity held = new TitleOpportunity();
    held.setId(30L);
    held.setName("Time Vault briefcase");
    held.setStatus(TitleOpportunityStatus.HELD);
    held.setWrestler(wrestler);
    held.setEarnedAt(LocalDate.now().minusDays(10));
    when(titleOpportunityService.findByWrestler(anyLong())).thenReturn(List.of(held));

    buildViewFor(wrestler);

    // The button sits deep in the accordion (view → accordion → panel → content → section row).
    List<Component> cashIns =
        allDescendants(view)
            .filter(c -> c instanceof com.vaadin.flow.component.button.Button)
            .map(com.vaadin.flow.component.button.Button.class::cast)
            .filter(b -> "Cash In".equals(b.getText()))
            .map(Component.class::cast)
            .toList();
    assertEquals(1, cashIns.size(), "Cash In button must render inside the profile view");
  }

  @Test
  @DisplayName("Career Dashboard link lives in the hero section, outside the accordion")
  void careerLink_inHeroSection_notInsideAccordion() {
    buildViewFor(wrestler);

    Accordion accordion = _get(view, Accordion.class);
    boolean linkInsideAccordion =
        accordion
            .getChildren()
            .flatMap(c -> c.getChildren())
            .filter(c -> c instanceof com.vaadin.flow.router.RouterLink)
            .map(l -> ((com.vaadin.flow.router.RouterLink) l).getText())
            .anyMatch(t -> t != null && t.contains("Career"));
    assertTrue(
        !linkInsideAccordion,
        "The career link must not be buried inside the collapsed accordion panels");

    List<Component> heroLinks =
        view.getChildren()
            .flatMap(this::allDescendants)
            .filter(c -> c instanceof com.vaadin.flow.router.RouterLink)
            .map(com.vaadin.flow.router.RouterLink.class::cast)
            .filter(l -> l.getText() != null && l.getText().contains("Career Dashboard"))
            .map(Component.class::cast)
            .toList();
    assertEquals(1, heroLinks.size(), "Exactly one career-dashboard link should be visible");
  }

  private void buildViewFor(Wrestler w) {
    when(wrestlerService.findByIdWithDetails(w.getId())).thenReturn(Optional.of(w));
    when(wrestlerService.getOrCreateState(anyLong(), anyLong()))
        .thenReturn(new com.github.javydreamercsw.management.domain.wrestler.WrestlerState());
    when(wrestlerService.resolveWrestlerImage(any()))
        .thenReturn(
            new com.github.javydreamercsw.base.image.ImageResolution("test://img.png", true));
    // securityUtils is field-injected (@Autowired), not constructor-injected; the action menu
    // needs it, so set it reflectively like WrestlerProfileViewUpdateTest does.
    org.springframework.test.util.ReflectionTestUtils.setField(
        view, "securityUtils", securityUtils);
    view.updateViewForTest(w);
  }

  private java.util.stream.Stream<Component> allDescendants(Component root) {
    return java.util.stream.Stream.concat(
        java.util.stream.Stream.of(root), root.getChildren().flatMap(this::allDescendants));
  }
}
