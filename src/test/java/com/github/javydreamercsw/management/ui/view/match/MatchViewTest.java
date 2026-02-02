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
package com.github.javydreamercsw.management.ui.view.match;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.LocalAIStatusService;
import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.league.MatchFulfillmentRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentParticipant;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.league.MatchFulfillmentService;
import com.github.javydreamercsw.management.service.match.SegmentAdjudicationService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.segment.PromoService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.ui.view.AbstractViewTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.RouteParameters;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchViewTest extends AbstractViewTest {

  @Mock private SegmentService segmentService;
  @Mock private WrestlerService wrestlerService;
  @Mock private SecurityUtils securityUtils;
  @Mock private CampaignService campaignService;
  @Mock private CampaignRepository campaignRepository;
  @Mock private SegmentNarrationServiceFactory narrationServiceFactory;
  @Mock private NpcService npcService;
  @Mock private SegmentAdjudicationService segmentAdjudicationService;
  @Mock private MatchFulfillmentRepository matchFulfillmentRepository;
  @Mock private MatchFulfillmentService matchFulfillmentService;
  @Mock private LocalAIStatusService localAIStatus;
  @Mock private PromoService promoService;
  private MatchView matchView;

  @BeforeEach
  public void setup() {
    matchView =
        new MatchView(
            segmentService,
            wrestlerService,
            securityUtils,
            campaignService,
            campaignRepository,
            narrationServiceFactory,
            npcService,
            segmentAdjudicationService,
            matchFulfillmentRepository,
            matchFulfillmentService,
            localAIStatus,
            promoService);
  }

  @Test
  void testSaveNarrationAndWinners() {
    Segment segment = new Segment();
    segment.setId(1L);
    segment.setNarration("Initial narration");
    Show show = new Show();
    show.setName("Test Show");
    segment.setShow(show);
    SegmentType segmentType = new SegmentType();
    segmentType.setName("Test Match");
    segment.setSegmentType(segmentType);

    Wrestler wrestler1 = new Wrestler();
    wrestler1.setName("Test Wrestler 1");
    wrestler1.setId(1L);

    Wrestler wrestler2 = new Wrestler();
    wrestler2.setName("Test Wrestler 2");
    wrestler2.setId(2L);

    SegmentParticipant participant1 = new SegmentParticipant();
    participant1.setSegment(segment);
    participant1.setWrestler(wrestler1);
    participant1.setIsWinner(false);
    segment.getParticipants().add(participant1);

    SegmentParticipant participant2 = new SegmentParticipant();
    participant2.setSegment(segment);
    participant2.setWrestler(wrestler2);
    participant2.setIsWinner(false);
    segment.getParticipants().add(participant2);

    CustomUserDetails userDetails = mock(CustomUserDetails.class);
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(userDetails));
    when(userDetails.getWrestler()).thenReturn(wrestler1);
    when(segmentService.findByIdWithShow(1L)).thenReturn(Optional.of(segment));
    when(wrestlerService.findByIdWithInjuries(1L)).thenReturn(Optional.of(wrestler1));
    when(wrestlerService.findByIdWithInjuries(2L)).thenReturn(Optional.of(wrestler2));

    BeforeEnterEvent event = mock(BeforeEnterEvent.class);
    when(event.getRouteParameters()).thenReturn(new RouteParameters("matchId", "1"));

    UI.getCurrent().add(matchView);
    matchView.beforeEnter(event); // Simulate navigation

    TextArea narrationArea = _get(TextArea.class, spec -> spec.withId("narration-area"));
    assertEquals("Initial narration", narrationArea.getValue());

    narrationArea.setValue("Updated narration");

    _click(_get(Button.class, spec -> spec.withId("save-narration-button")));

    assertEquals("Updated narration", segment.getNarration());

    MultiSelectComboBox<Wrestler> winnersComboBox =
        _get(MultiSelectComboBox.class, spec -> spec.withId("winners-combobox"));
    assertEquals(
        2, winnersComboBox.getDataProvider().size(new com.vaadin.flow.data.provider.Query<>()));

    winnersComboBox.setValue(new HashSet<>(List.of(wrestler1)));

    _click(_get(Button.class, spec -> spec.withId("save-winners-button")));

    assertEquals(1, segment.getWinners().size());
    assertEquals(wrestler1, segment.getWinners().get(0));
  }

  @Test
  void testPlayerRestrictionsForNonCampaignMatch() {
    Segment segment = new Segment();
    segment.setId(1L);
    Show show = new Show();
    show.setName("Test Show");
    segment.setShow(show);
    SegmentType segmentType = new SegmentType();
    segmentType.setName("Test Match");
    segment.setSegmentType(segmentType);

    Wrestler wrestler1 = new Wrestler();
    wrestler1.setName("Test Wrestler 1");
    wrestler1.setId(1L);

    CustomUserDetails userDetails = mock(CustomUserDetails.class);
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(userDetails));
    when(securityUtils.isPlayer()).thenReturn(true);
    when(securityUtils.isBooker()).thenReturn(false);
    when(securityUtils.isAdmin()).thenReturn(false);
    when(userDetails.getWrestler()).thenReturn(wrestler1);
    when(segmentService.findByIdWithShow(1L)).thenReturn(Optional.of(segment));

    BeforeEnterEvent event = mock(BeforeEnterEvent.class);
    when(event.getRouteParameters()).thenReturn(new RouteParameters("matchId", "1"));

    UI.getCurrent().add(matchView);
    matchView.beforeEnter(event); // Simulate navigation

    // Verify button text
    Button saveWinnersButton = _get(Button.class, spec -> spec.withId("save-winners-button"));
    assertEquals("Save Results", saveWinnersButton.getText());

    // Verify AI button hidden
    assertTrue(
        UI.getCurrent()
            .getChildren()
            .noneMatch(
                c ->
                    c instanceof Button
                        && "ai-generate-narration-button".equals(c.getId().orElse(""))));

    // Simulate clicking save winners
    _click(saveWinnersButton);

    // Verify adjudicateMatch was NOT called
    verify(segmentAdjudicationService, never()).adjudicateMatch(any());
    // Verify updateSegment WAS called (to save winners)
    verify(segmentService, times(1)).updateSegment(any(Segment.class));
  }
}
