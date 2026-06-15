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

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignAbilityCardRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStoryline;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterMode;
import com.github.javydreamercsw.management.service.campaign.CampaignChapterService;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.campaign.CampaignUpgradeService;
import com.github.javydreamercsw.management.service.campaign.StorylineExportService;
import com.github.javydreamercsw.management.service.campaign.TournamentService;
import com.github.javydreamercsw.management.service.title.TitleService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class CampaignDashboardViewTest extends AbstractViewTest {

  @Mock private CampaignRepository campaignRepository;
  @Mock private CampaignService campaignService;
  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private com.github.javydreamercsw.base.domain.account.AccountRepository accountRepository;
  @Mock private CampaignAbilityCardRepository cardRepository;
  @Mock private CampaignUpgradeService upgradeService;
  @Mock private SecurityUtils securityUtils;
  @Mock private StorylineExportService storylineExportService;
  @Mock private TournamentService tournamentService;
  @Mock private CampaignChapterService chapterService;
  @Mock private TitleService titleService;
  @Mock private TitleRepository titleRepository;
  @Mock private WrestlerService wrestlerService;

  private ObjectMapper objectMapper = new ObjectMapper();

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
    mockAccount.setActiveWrestlerId(1L);

    mockUser = mock(CustomUserDetails.class);
    when(mockUser.getUsername()).thenReturn("testuser");
    when(mockUser.getAccount()).thenReturn(mockAccount);
    when(mockUser.getId()).thenReturn(42L);

    mockWrestler = Wrestler.builder().id(1L).name("Test Wrestler").account(mockAccount).build();

    mockState =
        CampaignState.builder().activeCards(new ArrayList<>()).upgrades(new ArrayList<>()).build();

    mockCampaign = Campaign.builder().id(1L).wrestler(mockWrestler).state(mockState).build();

    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(mockUser));
    when(accountRepository.findById(42L)).thenReturn(Optional.of(mockAccount));
    when(wrestlerRepository.findByAccountId(42L)).thenReturn(List.of(mockWrestler));
    when(wrestlerRepository.findByAccountWithDetails(mockAccount))
        .thenReturn(List.of(mockWrestler));
    when(campaignRepository.findActiveByWrestler(mockWrestler))
        .thenReturn(Optional.of(mockCampaign));
    when(campaignService.getCampaignForWrestler(mockWrestler))
        .thenReturn(Optional.of(mockCampaign));
    when(campaignService.isChapterComplete(mockCampaign)).thenReturn(false);
    when(campaignService.getCurrentChapter(any()))
        .thenReturn(Optional.of(new CampaignChapterDTO()));
    when(titleRepository.findByName(any())).thenReturn(Optional.empty());
    when(wrestlerService.resolveWrestlerImage(any()))
        .thenReturn(new com.github.javydreamercsw.base.image.ImageResolution(null, true));
  }

  @Test
  public void testDashboardLayout() {
    CampaignDashboardView view =
        new CampaignDashboardView(
            campaignRepository,
            campaignService,
            wrestlerRepository,
            accountRepository,
            cardRepository,
            upgradeService,
            securityUtils,
            tournamentService,
            objectMapper,
            chapterService,
            titleService,
            titleRepository,
            storylineExportService,
            wrestlerService);

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
            accountRepository,
            cardRepository,
            upgradeService,
            securityUtils,
            tournamentService,
            objectMapper,
            chapterService,
            titleService,
            titleRepository,
            storylineExportService,
            wrestlerService);

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
            accountRepository,
            cardRepository,
            upgradeService,
            securityUtils,
            tournamentService,
            objectMapper,
            chapterService,
            titleService,
            titleRepository,
            storylineExportService,
            wrestlerService);

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

  private CampaignDashboardView buildView() {
    return new CampaignDashboardView(
        campaignRepository,
        campaignService,
        wrestlerRepository,
        accountRepository,
        cardRepository,
        upgradeService,
        securityUtils,
        tournamentService,
        objectMapper,
        chapterService,
        titleService,
        titleRepository,
        storylineExportService,
        wrestlerService);
  }

  @Test
  public void testAdvanceButton_singleChapter_callsAdvanceToChapter() {
    when(campaignService.isChapterComplete(mockCampaign)).thenReturn(true);
    CampaignChapterDTO next =
        CampaignChapterDTO.builder()
            .id("tournament")
            .title("The Tournament")
            .mode(CampaignChapterMode.AI_ONLY)
            .build();
    when(campaignService.getAvailableNextChapters(mockCampaign)).thenReturn(List.of(next));
    when(campaignService.advanceToChapter(mockCampaign, "tournament")).thenReturn(Optional.empty());

    CampaignDashboardView view = buildView();
    UI.getCurrent().add(view);

    _click(_get(view, Button.class, spec -> spec.withText("Complete Chapter & Advance")));

    verify(campaignService).advanceToChapter(mockCampaign, "tournament");
  }

  @Test
  public void testAdvanceButton_multipleChapters_opensSelectionDialog() {
    when(campaignService.isChapterComplete(mockCampaign)).thenReturn(true);
    CampaignChapterDTO ch1 =
        CampaignChapterDTO.builder()
            .id("tournament")
            .title("The Tournament")
            .mode(CampaignChapterMode.AI_ONLY)
            .build();
    CampaignChapterDTO ch2 =
        CampaignChapterDTO.builder()
            .id("tag_team")
            .title("Tag Team Redemption")
            .mode(CampaignChapterMode.AI_ONLY)
            .build();
    when(campaignService.getAvailableNextChapters(mockCampaign)).thenReturn(List.of(ch1, ch2));

    CampaignDashboardView view = buildView();
    UI.getCurrent().add(view);

    _click(_get(view, Button.class, spec -> spec.withText("Complete Chapter & Advance")));

    Dialog dialog = _get(Dialog.class);
    assertThat(dialog).isNotNull();
    _get(H3.class, spec -> spec.withText("The Tournament"));
    _get(H3.class, spec -> spec.withText("Tag Team Redemption"));
  }

  @Test
  public void testAdvanceButton_noChapters_callsAdvanceChapter() {
    when(campaignService.isChapterComplete(mockCampaign)).thenReturn(true);
    when(campaignService.getAvailableNextChapters(mockCampaign)).thenReturn(List.of());
    when(campaignService.advanceChapter(mockCampaign)).thenReturn(Optional.empty());

    CampaignDashboardView view = buildView();
    UI.getCurrent().add(view);

    _click(_get(view, Button.class, spec -> spec.withText("Complete Chapter & Advance")));

    verify(campaignService).advanceChapter(mockCampaign);
  }

  @Test
  public void testStoryJournalDownloadLink() {
    CampaignStoryline storyline = new CampaignStoryline();
    storyline.setTitle("AI Epic Arc");
    storyline.setDescription("Deep narrative.");
    storyline.setStatus(CampaignStoryline.StorylineStatus.ACTIVE);

    when(campaignService.getStorylineHistory(mockCampaign)).thenReturn(List.of(storyline));

    CampaignDashboardView view =
        new CampaignDashboardView(
            campaignRepository,
            campaignService,
            wrestlerRepository,
            accountRepository,
            cardRepository,
            upgradeService,
            securityUtils,
            tournamentService,
            objectMapper,
            chapterService,
            titleService,
            titleRepository,
            storylineExportService,
            wrestlerService);

    UI.getCurrent().add(view);

    // Verify Story Journal contains the storyline and a download link
    _get(view, Anchor.class, spec -> spec.withId("download-json-anchor-null"));
    _get(view, Button.class, spec -> spec.withId("download-json-button-null"));
  }
}
