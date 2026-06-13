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

import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.SegmentNarrationService;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.service.NotificationService;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStateRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterDTO;
import com.github.javydreamercsw.management.dto.campaign.CampaignChapterMode;
import com.github.javydreamercsw.management.dto.campaign.CampaignEncounterResponseDTO;
import com.github.javydreamercsw.management.dto.campaign.StaticEncounterDTO;
import com.github.javydreamercsw.management.service.campaign.CampaignEncounterService;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.campaign.FeatureDataService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

class CampaignNarrativeViewTest extends AbstractViewTest {

  @Mock private WrestlerRepository wrestlerRepository;
  @Mock private CampaignEncounterService encounterService;
  @Mock private CampaignService campaignService;
  @Mock private SecurityUtils securityUtils;
  @Mock private SegmentNarrationServiceFactory aiFactory;
  @Mock private NotificationService notificationService;
  @Mock private FeatureDataService featureDataService;
  @Mock private CampaignStateRepository campaignStateRepository;

  private CampaignNarrativeView view;

  @BeforeEach
  void setup() {
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.empty());

    view =
        new CampaignNarrativeView(
            wrestlerRepository,
            encounterService,
            campaignService,
            securityUtils,
            aiFactory,
            notificationService,
            featureDataService,
            campaignStateRepository);
    UI.getCurrent().add(view);
  }

  @Test
  @DisplayName("Should render the Story Narrative heading when no campaign")
  void shouldRenderHeading() {
    H2 heading = _get(view, H2.class, spec -> spec.withText("Story Narrative"));
    assertTrue(heading.isVisible());
  }

  // ── Helpers ────────────────────────────────────────────────────────────────

  private CampaignNarrativeView viewWithCampaign(CampaignChapterDTO chapter) {
    Account account = new Account();
    account.setUsername("player");
    account.setActiveWrestlerId(1L);

    CustomUserDetails userDetails = Mockito.mock(CustomUserDetails.class);
    when(userDetails.getAccount()).thenReturn(account);

    Wrestler wrestler = Wrestler.builder().id(1L).name("Player").account(account).build();
    CampaignState state = CampaignState.builder().build();
    Campaign campaign = Campaign.builder().id(1L).wrestler(wrestler).state(state).build();

    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(userDetails));
    when(wrestlerRepository.findByAccount(account)).thenReturn(List.of(wrestler));
    when(campaignService.getCampaignForWrestler(wrestler)).thenReturn(Optional.of(campaign));
    when(campaignService.getCurrentChapter(campaign)).thenReturn(Optional.of(chapter));

    UI.getCurrent().removeAll();
    CampaignNarrativeView v =
        new CampaignNarrativeView(
            wrestlerRepository,
            encounterService,
            campaignService,
            securityUtils,
            aiFactory,
            notificationService,
            featureDataService,
            campaignStateRepository);
    UI.getCurrent().add(v);
    return v;
  }

  private static CampaignChapterDTO staticOnlyChapter(List<StaticEncounterDTO> encounters) {
    return CampaignChapterDTO.builder()
        .id("test_chapter")
        .title("Test Chapter")
        .mode(CampaignChapterMode.STATIC_ONLY)
        .staticEncounters(encounters)
        .build();
  }

  private static StaticEncounterDTO simpleEncounter(int index, String title) {
    StaticEncounterDTO.StaticChoiceDTO choice =
        StaticEncounterDTO.StaticChoiceDTO.builder().id("c1").label("Go").text("Proceed").build();
    return StaticEncounterDTO.builder()
        .id("e" + index)
        .title(title)
        .narrativeText("Narrative for " + title)
        .choices(List.of(choice))
        .build();
  }

  // ── Tests ──────────────────────────────────────────────────────────────────

  @Test
  @DisplayName("STATIC_ONLY chapter renders static encounter, not 'Story Director Offline'")
  void staticOnlyChapter_rendersStaticEncounter() {
    CampaignEncounterResponseDTO dto =
        CampaignEncounterResponseDTO.builder()
            .narrative("Step 0 title\n\nNarrative text here.")
            .choices(
                List.of(
                    CampaignEncounterResponseDTO.Choice.builder()
                        .label("Proceed")
                        .text("Go forward")
                        .build()))
            .build();

    CampaignChapterDTO chapter = staticOnlyChapter(List.of(simpleEncounter(0, "Step 0")));
    when(encounterService.generateStaticEncounter(any(), any())).thenReturn(dto);
    when(aiFactory.getAvailableServicesInPriorityOrder()).thenReturn(List.of());

    CampaignNarrativeView v = viewWithCampaign(chapter);

    // Should NOT show "Story Director Offline"
    List<H2> headings = _find(v, H2.class, spec -> spec.withText("Story Director Offline"));
    assertTrue(headings.isEmpty(), "Should not show 'Story Director Offline' for STATIC_ONLY");

    // Should show the narrative paragraph
    _get(v, Paragraph.class, spec -> spec.withText("Step 0 title\n\nNarrative text here."));
  }

  @Test
  @DisplayName("AI_WITH_FALLBACK chapter uses static when no AI available")
  void aiWithFallback_noAi_usesStatic() {
    CampaignEncounterResponseDTO dto =
        CampaignEncounterResponseDTO.builder()
            .narrative("Fallback narrative.")
            .choices(List.of())
            .build();

    CampaignChapterDTO chapter =
        CampaignChapterDTO.builder()
            .id("ch")
            .mode(CampaignChapterMode.AI_WITH_FALLBACK)
            .staticEncounters(List.of(simpleEncounter(0, "Intro")))
            .build();
    when(encounterService.generateStaticEncounter(any(), any())).thenReturn(dto);
    when(aiFactory.getAvailableServicesInPriorityOrder()).thenReturn(List.of());

    viewWithCampaign(chapter);

    verify(encounterService).generateStaticEncounter(any(), any());
    verify(encounterService, never()).generateEncounter(any());
  }

  @Test
  @DisplayName("AI_WITH_FALLBACK chapter uses AI when AI is available")
  void aiWithFallback_withAi_usesAi() {
    CampaignEncounterResponseDTO dto =
        CampaignEncounterResponseDTO.builder()
            .narrative("AI narrative.")
            .choices(List.of())
            .build();

    CampaignChapterDTO chapter =
        CampaignChapterDTO.builder()
            .id("ch")
            .mode(CampaignChapterMode.AI_WITH_FALLBACK)
            .staticEncounters(List.of(simpleEncounter(0, "Intro")))
            .build();

    SegmentNarrationService mockProvider = Mockito.mock(SegmentNarrationService.class);
    when(aiFactory.getAvailableServicesInPriorityOrder()).thenReturn(List.of(mockProvider));
    when(encounterService.generateEncounter(any())).thenReturn(dto);

    viewWithCampaign(chapter);

    verify(encounterService).generateEncounter(any());
    verify(encounterService, never()).generateStaticEncounter(any(), any());
  }

  @Test
  @DisplayName("AI_ONLY chapter with no AI shows 'Story Director Offline'")
  void aiOnly_noAi_showsOfflineMessage() {
    CampaignChapterDTO chapter =
        CampaignChapterDTO.builder().id("ai_only").mode(CampaignChapterMode.AI_ONLY).build();

    when(aiFactory.getAvailableServicesInPriorityOrder()).thenReturn(List.of());

    CampaignNarrativeView v = viewWithCampaign(chapter);

    _get(v, H2.class, spec -> spec.withText("Story Director Offline"));
  }

  @Test
  @DisplayName("All static steps consumed shows completion placeholder")
  void allStepsConsumed_showsCompletionPlaceholder() {
    CampaignChapterDTO chapter = staticOnlyChapter(List.of(simpleEncounter(0, "Intro")));
    when(encounterService.generateStaticEncounter(any(), any()))
        .thenThrow(new IllegalStateException("No more static encounters"));
    when(aiFactory.getAvailableServicesInPriorityOrder()).thenReturn(List.of());

    CampaignNarrativeView v = viewWithCampaign(chapter);

    _get(v, H2.class, spec -> spec.withText("All Story Events Complete"));
    _get(v, Button.class, spec -> spec.withText("Back to Dashboard"));
  }
}
