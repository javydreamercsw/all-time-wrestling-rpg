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
package com.github.javydreamercsw.management.ui.view.campaign;

import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCardRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.campaign.CampaignUpgradeService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CampaignDashboardViewTest extends AbstractViewTest {

  @Mock private CampaignRepository campaignRepository;
  @Mock private CampaignService campaignService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private CampaignAbilityCardRepository cardRepository;
  @Mock private CampaignUpgradeService upgradeService;
  @Mock private SecurityUtils securityUtils;

  @Mock
  private com.github.javydreamercsw.management.service.campaign.TournamentService tournamentService;

  private CustomUserDetails mockUser;
  private Account mockAccount;
  private Wrestler mockWrestler;
  private Campaign mockCampaign;
  private CampaignState mockState;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    mockAccount = new Account();
    mockAccount.setUsername("testuser");

    mockUser = mock(CustomUserDetails.class);
    when(mockUser.getUsername()).thenReturn("testuser");
    when(mockUser.getAccount()).thenReturn(mockAccount);

    mockWrestler = Wrestler.builder().name("Test Wrestler").account(mockAccount).build();

    mockState =
        CampaignState.builder().activeCards(new ArrayList<>()).upgrades(new ArrayList<>()).build();

    mockCampaign = Campaign.builder().id(1L).wrestler(mockWrestler).state(mockState).build();

    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(mockUser));
    when(wrestlerRepository.findByAccount(mockAccount)).thenReturn(Optional.of(mockWrestler));
    when(campaignRepository.findActiveByWrestler(mockWrestler))
        .thenReturn(Optional.of(mockCampaign));
    when(campaignService.isChapterComplete(mockCampaign)).thenReturn(false);
    when(campaignService.getCurrentChapter(any())).thenReturn(new CampaignChapterDTO());
  }

  @Test
  public void testDashboardLayout() {
    CampaignDashboardView view =
        new CampaignDashboardView(
            campaignRepository,
            campaignService,
            wrestlerRepository,
            cardRepository,
            upgradeService,
            securityUtils,
            tournamentService);

    UI.getCurrent().add(view);

    // Verify Main Headers
    _get(view, H2.class, spec -> spec.withText("Campaign: All or Nothing (Season 1)"));

    // Verify Split Layout

    HorizontalLayout mainLayout =
        _get(view, HorizontalLayout.class, spec -> spec.withClasses("gap-xl items-start"));

    // Verify Columns
    VerticalLayout leftColumn = (VerticalLayout) mainLayout.getComponentAt(0);
    VerticalLayout rightColumn = (VerticalLayout) mainLayout.getComponentAt(1);

    // Verify Left Column Content (Player Card)
    // Checking for class "player-card-container" which is on the PlayerCampaignCard component
    // Note: Composite components are wrapped, so we look for the internal Div
    // Actually, checking for H4 "My Ability Cards" which is definitely in left column
    _get(leftColumn, H4.class, spec -> spec.withText("My Ability Cards"));

    // Verify Right Column Content (Purchased Skills ABOVE Actions)
    H4 skillsHeader = _get(rightColumn, H4.class, spec -> spec.withText("Purchased Skills"));
    H4 actionsHeader = _get(rightColumn, H4.class, spec -> spec.withText("Actions"));

    // Verify Actions is below Purchased Skills (index check)
    int skillsIndex = rightColumn.indexOf(skillsHeader);
    int actionsIndex = rightColumn.indexOf(actionsHeader);

    // Actions header might be further down due to content in between, but index must be greater
    // Actually, we added "Purchased Skills", then "Skill Upgrades" (optional), then "Actions"
    // So index(Actions) > index(Skills)

    // NOTE: H4 might be wrapped in layout? No, added directly.

    assert (actionsIndex > skillsIndex);
  }

  @Test
  public void testCompleteChapterButtonVisibleWhenComplete() {
    when(campaignService.isChapterComplete(mockCampaign)).thenReturn(true);

    CampaignDashboardView view =
        new CampaignDashboardView(
            campaignRepository,
            campaignService,
            wrestlerRepository,
            cardRepository,
            upgradeService,
            securityUtils,
            tournamentService);

    UI.getCurrent().add(view);

    // Should find the button
    _get(view, Button.class, spec -> spec.withText("Complete Chapter & Advance"));
  }

  @Test
  public void testCompleteChapterButtonHiddenWhenNotComplete() {
    when(campaignService.isChapterComplete(mockCampaign)).thenReturn(false);

    CampaignDashboardView view =
        new CampaignDashboardView(
            campaignRepository,
            campaignService,
            wrestlerRepository,
            cardRepository,
            upgradeService,
            securityUtils,
            tournamentService);

    UI.getCurrent().add(view);

    // Should NOT find the button
    // Using Karibu _get throws error if not found, we expect that or verify list empty
    boolean found = false;
    try {
      _get(view, Button.class, spec -> spec.withText("Complete Chapter & Advance"));
      found = true;
    } catch (AssertionError e) {
      found = false;
    }
    assert (!found);
  }
}
