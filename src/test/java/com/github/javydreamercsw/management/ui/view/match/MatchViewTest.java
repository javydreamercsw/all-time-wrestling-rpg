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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.ai.SegmentNarrationServiceFactory;
import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.CustomUserDetails;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.base.ui.service.NotificationService;
import com.github.javydreamercsw.management.domain.campaign.Campaign;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.campaign.CampaignState;
import com.github.javydreamercsw.management.domain.campaign.CampaignStatus;
import com.github.javydreamercsw.management.domain.commentator.CommentaryTeamRepository;
import com.github.javydreamercsw.management.domain.league.MatchFulfillmentRepository;
import com.github.javydreamercsw.management.domain.npc.Npc;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentParticipant;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerState;
import com.github.javydreamercsw.management.service.campaign.CampaignService;
import com.github.javydreamercsw.management.service.injury.InjuryService;
import com.github.javydreamercsw.management.service.league.MatchFulfillmentService;
import com.github.javydreamercsw.management.service.match.SegmentAdjudicationService;
import com.github.javydreamercsw.management.service.npc.NpcService;
import com.github.javydreamercsw.management.service.ringside.RingsideActionDataService;
import com.github.javydreamercsw.management.service.ringside.RingsideActionService;
import com.github.javydreamercsw.management.service.ringside.RingsideAiService;
import com.github.javydreamercsw.management.service.segment.NarrationParserService;
import com.github.javydreamercsw.management.service.segment.PromoService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.team.TeamService;
import com.github.javydreamercsw.management.service.title.TitleScriptService;
import com.github.javydreamercsw.management.service.universe.UniverseContextService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerStatsService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class MatchViewTest extends AbstractViewTest {

  @Mock private SegmentService segmentService;
  @Mock private WrestlerService wrestlerService;
  @Mock private WrestlerStatsService wrestlerStatsService;
  @Mock private SecurityUtils securityUtils;
  @Mock private CampaignService campaignService;
  @Mock private CampaignRepository campaignRepository;
  @Mock private SegmentNarrationServiceFactory narrationServiceFactory;
  @Mock private NpcService npcService;
  @Mock private SegmentAdjudicationService segmentAdjudicationService;
  @Mock private MatchFulfillmentRepository matchFulfillmentRepository;
  @Mock private MatchFulfillmentService matchFulfillmentService;
  @Mock private PromoService promoService;
  @Mock private CommentaryTeamRepository commentaryTeamRepository;
  @Mock private NarrationParserService narrationParserService;

  @Mock private RingsideActionService ringsideActionService;
  @Mock private RingsideAiService ringsideAiService;
  @Mock private RingsideActionDataService ringsideActionDataService;
  @Mock private TeamService teamService;
  @Mock private NotificationService notificationService;
  @Mock private TitleScriptService titleScriptService;
  @Mock private InjuryService injuryService;

  @Mock private UniverseContextService universeContextService;

  private MatchView matchView;

  @BeforeEach
  public void setup() {
    lenient().when(universeContextService.getCurrentUniverseId()).thenReturn(1L);

    matchView =
        new MatchView(
            segmentService,
            wrestlerService,
            wrestlerStatsService,
            injuryService,
            universeContextService,
            securityUtils,
            campaignService,
            campaignRepository,
            narrationServiceFactory,
            npcService,
            segmentAdjudicationService,
            matchFulfillmentRepository,
            matchFulfillmentService,
            promoService,
            commentaryTeamRepository,
            narrationParserService,
            ringsideActionService,
            ringsideAiService,
            ringsideActionDataService,
            titleScriptService,
            notificationService);
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

    Universe universe = new Universe();
    universe.setId(1L);
    universe.setName("Default");

    Wrestler wrestler1 = new Wrestler();
    wrestler1.setName("Test Wrestler 1");
    wrestler1.setId(1L);
    wrestler1
        .getWrestlerStates()
        .add(WrestlerState.builder().wrestler(wrestler1).universe(universe).build());

    Wrestler wrestler2 = new Wrestler();
    wrestler2.setName("Test Wrestler 2");
    wrestler2.setId(2L);
    wrestler2
        .getWrestlerStates()
        .add(WrestlerState.builder().wrestler(wrestler2).universe(universe).build());
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
    when(securityUtils.isBooker()).thenReturn(true);
    when(userDetails.getWrestler()).thenReturn(wrestler1);
    when(segmentService.findByIdWithDetails(1L)).thenReturn(Optional.of(segment));
    lenient().when(wrestlerService.findById(1L)).thenReturn(Optional.of(wrestler1));
    lenient().when(wrestlerService.findById(2L)).thenReturn(Optional.of(wrestler2));
    when(wrestlerService.findByIdWithDetails(1L)).thenReturn(Optional.of(wrestler1));
    when(wrestlerService.findByIdWithDetails(2L)).thenReturn(Optional.of(wrestler2));
    lenient()
        .when(wrestlerService.getOrCreateState(anyLong(), eq(1L)))
        .thenAnswer(
            invocation -> {
              Long wrestlerId = invocation.getArgument(0);
              return (wrestlerId == 1L ? wrestler1 : wrestler2).getState(1L).orElseThrow();
            });

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

    Universe universe = new Universe();
    universe.setId(1L);
    universe.setName("Default");

    Account playerAccount = new Account();
    playerAccount.setId(42L);
    playerAccount.setUsername("player1");

    Wrestler wrestler1 = new Wrestler();
    wrestler1.setName("Test Wrestler 1");
    wrestler1.setId(1L);
    wrestler1.setAccount(playerAccount);
    wrestler1
        .getWrestlerStates()
        .add(WrestlerState.builder().wrestler(wrestler1).universe(universe).build());

    SegmentParticipant participant1 = new SegmentParticipant();
    participant1.setSegment(segment);
    participant1.setWrestler(wrestler1);
    segment.getParticipants().add(participant1);

    CustomUserDetails userDetails = mock(CustomUserDetails.class);
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(userDetails));
    when(securityUtils.isPlayer()).thenReturn(true);
    when(securityUtils.isBooker()).thenReturn(false);
    when(securityUtils.isAdmin()).thenReturn(false);
    when(securityUtils.getCurrentAccountId()).thenReturn(Optional.of(42L));
    when(userDetails.getWrestler()).thenReturn(wrestler1);
    when(segmentService.findByIdWithDetails(1L)).thenReturn(Optional.of(segment));

    BeforeEnterEvent event = mock(BeforeEnterEvent.class);
    when(event.getRouteParameters()).thenReturn(new RouteParameters("matchId", "1"));

    UI.getCurrent().add(matchView);
    matchView.beforeEnter(event); // Simulate navigation

    // Verify button text
    Button saveWinnersButton = _get(Button.class, spec -> spec.withId("save-winners-button"));
    assertEquals("Submit Result", saveWinnersButton.getText());

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

    // Verify adjudicateMatch was NOT called (neither ID nor entity overload)
    verify(segmentAdjudicationService, never()).adjudicateMatch(any(Long.class));
    // Verify updateSegment WAS called (to save winners)
    verify(segmentService, times(1)).updateSegment(any(Segment.class));
  }

  @Test
  void autoAssignsRefereeWhenNoneSet() {
    Segment segment = buildMinimalMatchSegment(1L, "Singles Match");

    Npc referee = new Npc();
    referee.setName("Earl Hebner");

    when(segmentService.findByIdWithDetails(1L)).thenReturn(Optional.of(segment));
    when(npcService.findAllByType("Referee")).thenReturn(List.of(referee));
    when(npcService.getAwareness(referee)).thenReturn(75);
    when(segmentService.updateSegment(any())).thenAnswer(inv -> inv.getArgument(0));
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.empty());

    BeforeEnterEvent event = mock(BeforeEnterEvent.class);
    when(event.getRouteParameters()).thenReturn(new RouteParameters("matchId", "1"));

    UI.getCurrent().add(matchView);
    matchView.beforeEnter(event);

    assertEquals(referee, segment.getReferee());
    assertEquals(75, segment.getRefereeAwarenessLevel());
    verify(segmentService).updateSegment(segment);
  }

  @Test
  void doesNotOverwriteExistingReferee() {
    Segment segment = buildMinimalMatchSegment(2L, "Tag Team Match");
    Npc existingRef = new Npc();
    existingRef.setName("Existing Ref");
    segment.setReferee(existingRef);

    Npc otherRef = new Npc();
    otherRef.setName("Other Ref");

    when(segmentService.findByIdWithDetails(2L)).thenReturn(Optional.of(segment));
    lenient().when(npcService.findAllByType("Referee")).thenReturn(List.of(otherRef));
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.empty());

    BeforeEnterEvent event = mock(BeforeEnterEvent.class);
    when(event.getRouteParameters()).thenReturn(new RouteParameters("matchId", "2"));

    UI.getCurrent().add(matchView);
    matchView.beforeEnter(event);

    assertEquals(existingRef, segment.getReferee());
    verify(npcService, never()).getAwareness(any());
  }

  @Test
  void doesNotAssignRefereeToPromoSegment() {
    Segment segment = buildMinimalMatchSegment(3L, "Promo");

    Npc referee = new Npc();
    referee.setName("Some Ref");

    when(segmentService.findByIdWithDetails(3L)).thenReturn(Optional.of(segment));
    lenient().when(npcService.findAllByType("Referee")).thenReturn(List.of(referee));
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.empty());

    BeforeEnterEvent event = mock(BeforeEnterEvent.class);
    when(event.getRouteParameters()).thenReturn(new RouteParameters("matchId", "3"));

    UI.getCurrent().add(matchView);
    matchView.beforeEnter(event);

    assertNull(segment.getReferee());
    verify(npcService, never()).findAllByType(any());
  }

  /**
   * Verifies that a PLAYER-role viewer still gets a referee auto-assigned and that {@code
   * segmentService.updateSegment} is called even when the security context only carries {@code
   * ROLE_PLAYER}. In production the call goes through {@link
   * com.github.javydreamercsw.base.security.GeneralSecurityUtils#runAsAdmin} which temporarily
   * elevates the context; in this unit test the service is mocked (no real {@code @PreAuthorize}
   * enforcement), so the test validates the code path and documenting the expected behaviour — the
   * companion integration test {@code MatchViewAutoAssignRefereeIT} exercises the real security
   * gate.
   */
  @Test
  void autoAssignsRefereeForPlayerRoleUser_persistsViaRunAsAdmin() {
    // Establish a PLAYER-only security context for the duration of this test.
    UsernamePasswordAuthenticationToken playerAuth =
        new UsernamePasswordAuthenticationToken(
            "player", "password", List.of(new SimpleGrantedAuthority("ROLE_PLAYER")));
    SecurityContext playerContext = SecurityContextHolder.createEmptyContext();
    playerContext.setAuthentication(playerAuth);
    SecurityContextHolder.setContext(playerContext);

    try {
      Segment segment = buildMinimalMatchSegment(4L, "Singles Match");

      Npc referee = new Npc();
      referee.setName("Mike Chioda");

      when(segmentService.findByIdWithDetails(4L)).thenReturn(Optional.of(segment));
      when(npcService.findAllByType("Referee")).thenReturn(List.of(referee));
      when(npcService.getAwareness(referee)).thenReturn(80);
      // Mock updateSegment to return the same segment (simulates a successful save)
      when(segmentService.updateSegment(any())).thenAnswer(inv -> inv.getArgument(0));
      when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.empty());

      BeforeEnterEvent event = mock(BeforeEnterEvent.class);
      when(event.getRouteParameters()).thenReturn(new RouteParameters("matchId", "4"));

      UI.getCurrent().add(matchView);
      matchView.beforeEnter(event);

      // Referee must be assigned on the in-memory segment regardless of viewer role.
      assertEquals(referee, segment.getReferee());
      assertEquals(80, segment.getRefereeAwarenessLevel());
      // updateSegment must be called — the auto-assign is a system op, not gated on role.
      verify(segmentService).updateSegment(segment);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  /**
   * Verifies that a PLAYER-role user can submit results for a campaign match. The two updateSegment
   * calls (save winners, then mark ADJUDICATED) both go through runAsAdmin() so the @PreAuthorize
   * gate is not a blocker. campaignService.processMatchResult() must be called with the correct
   * campaign and won flag, and the player should be routed back to "campaign".
   */
  @Test
  void playerCanSubmitCampaignMatchResult() {
    Segment segment = buildMinimalMatchSegment(5L, "One on One");
    segment.setId(5L);

    Universe universe = new Universe();
    universe.setId(1L);
    universe.setName("Default");

    // Account must be set so the winners card is visible (isParticipatingPlayer check at
    // MatchView line ~646 requires w.getAccount().getId() == currentAccountId).
    // The tutorial fix (CampaignTutorialDefinition.beforeStep) sets this for new campaigns.
    Account playerAccount = new Account();
    playerAccount.setId(42L);
    playerAccount.setUsername("player1");

    Wrestler playerWrestler = new Wrestler();
    playerWrestler.setId(10L);
    playerWrestler.setName("Player Wrestler");
    playerWrestler.setAccount(playerAccount);
    playerWrestler
        .getWrestlerStates()
        .add(WrestlerState.builder().wrestler(playerWrestler).universe(universe).build());

    Wrestler opponent = new Wrestler();
    opponent.setId(11L);
    opponent.setName("Opponent");
    opponent
        .getWrestlerStates()
        .add(WrestlerState.builder().wrestler(opponent).universe(universe).build());

    SegmentParticipant p1 = new SegmentParticipant();
    p1.setSegment(segment);
    p1.setWrestler(playerWrestler);
    segment.getParticipants().add(p1);

    SegmentParticipant p2 = new SegmentParticipant();
    p2.setSegment(segment);
    p2.setWrestler(opponent);
    segment.getParticipants().add(p2);

    CampaignState state = CampaignState.builder().build();
    state.setCurrentMatch(segment);

    Campaign campaign =
        Campaign.builder()
            .wrestler(playerWrestler)
            .status(CampaignStatus.ACTIVE)
            .state(state)
            .build();

    CustomUserDetails userDetails = mock(CustomUserDetails.class);
    when(securityUtils.getAuthenticatedUser()).thenReturn(Optional.of(userDetails));
    when(securityUtils.isPlayer()).thenReturn(true);
    when(securityUtils.isBooker()).thenReturn(false);
    when(securityUtils.isAdmin()).thenReturn(false);
    when(securityUtils.getCurrentAccountId()).thenReturn(Optional.of(42L));
    when(userDetails.getWrestler()).thenReturn(playerWrestler);
    when(segmentService.findByIdWithDetails(5L)).thenReturn(Optional.of(segment));
    when(segmentService.updateSegment(any())).thenAnswer(inv -> inv.getArgument(0));
    when(wrestlerService.findByIdWithDetails(10L)).thenReturn(Optional.of(playerWrestler));
    when(wrestlerService.findByIdWithDetails(11L)).thenReturn(Optional.of(opponent));
    lenient()
        .when(wrestlerService.getOrCreateState(anyLong(), eq(1L)))
        .thenAnswer(
            inv -> {
              Long wId = inv.getArgument(0);
              return (wId.equals(10L) ? playerWrestler : opponent).getState(1L).orElseThrow();
            });
    when(matchFulfillmentRepository.findBySegment(segment)).thenReturn(Optional.empty());
    when(campaignRepository.findActiveByWrestler(playerWrestler)).thenReturn(Optional.of(campaign));
    when(campaignRepository.findActiveByWrestler(opponent)).thenReturn(Optional.empty());

    BeforeEnterEvent event = mock(BeforeEnterEvent.class);
    when(event.getRouteParameters()).thenReturn(new RouteParameters("matchId", "5"));

    UI.getCurrent().add(matchView);
    matchView.beforeEnter(event);

    MultiSelectComboBox<Wrestler> winnersComboBox =
        _get(MultiSelectComboBox.class, spec -> spec.withId("winners-combobox"));
    winnersComboBox.setValue(new HashSet<>(List.of(playerWrestler)));

    _click(_get(Button.class, spec -> spec.withId("save-winners-button")));

    // processMatchResult must be called with won=true (player is in the winners set)
    verify(campaignService).processMatchResult(campaign, true);
    // updateSegment called twice: once to save winners, once to mark ADJUDICATED
    verify(segmentService, times(2)).updateSegment(any(Segment.class));
    // adjudicateMatch must NOT be called — that's booker/admin only
    verify(segmentAdjudicationService, never()).adjudicateMatch(any(Long.class));
  }

  private Segment buildMinimalMatchSegment(final long id, final String typeName) {
    Segment segment = new Segment();
    segment.setId(id);
    Show show = new Show();
    show.setName("Test Show");
    segment.setShow(show);
    SegmentType segmentType = new SegmentType();
    segmentType.setName(typeName);
    segment.setSegmentType(segmentType);
    return segment;
  }
}
