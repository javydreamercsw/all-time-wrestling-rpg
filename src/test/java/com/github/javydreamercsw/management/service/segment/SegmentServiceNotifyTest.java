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
package com.github.javydreamercsw.management.service.segment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import com.github.javydreamercsw.management.domain.league.LeagueRoster;
import com.github.javydreamercsw.management.domain.league.LeagueRosterRepository;
import com.github.javydreamercsw.management.domain.league.MatchFulfillment;
import com.github.javydreamercsw.management.domain.league.MatchFulfillmentRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.SegmentRepository;
import com.github.javydreamercsw.management.domain.title.TitleRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.GameSettingService;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import com.github.javydreamercsw.management.service.news.NewsGenerationService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

/** Tests for the MATCH_REQUEST inbox notification path in SegmentService. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SegmentServiceNotifyTest {

  @Mock private SegmentRepository segmentRepository;
  @Mock private TitleRepository titleRepository;
  @Mock private WrestlerService wrestlerService;
  @Mock private GameSettingService gameSettingService;
  @Mock private SecurityUtils securityUtils;
  @Mock private CampaignRepository campaignRepository;
  @Mock private LeagueRepository leagueRepository;
  @Mock private LeagueRosterRepository leagueRosterRepository;
  @Mock private MatchFulfillmentRepository matchFulfillmentRepository;
  @Mock private InboxService inboxService;
  @Mock private NewsGenerationService newsGenerationService;
  @Mock private EntityManager entityManager;

  private final InboxEventType matchRequestEventType =
      new InboxEventType("MATCH_REQUEST", "Match Request");

  private SegmentService service;

  @BeforeEach
  void setUp() {
    service =
        new SegmentService(
            segmentRepository,
            titleRepository,
            wrestlerService,
            gameSettingService,
            securityUtils,
            campaignRepository,
            leagueRepository,
            leagueRosterRepository,
            matchFulfillmentRepository,
            inboxService,
            newsGenerationService,
            matchRequestEventType);
    ReflectionTestUtils.setField(service, "entityManager", entityManager);
  }

  /** Builds a standard test fixture and adds a participant via addParticipant(). */
  private Segment buildSegmentWithLeague(
      final League league, final Show show, final Wrestler wrestler) {
    Segment segment = new Segment();
    segment.setId(5L);
    segment.setShow(show);

    when(segmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(entityManager.find(eq(Show.class), anyLong())).thenReturn(show);

    service.addParticipant(segment, wrestler);
    return segment;
  }

  @Test
  void notifyLeagueParticipants_setsActionTypeAndPayloadOnInboxItem() {
    Account commissioner = new Account("commissioner", "pw", "c@test.com");
    commissioner.setId(1L);
    Account owner = new Account("player1", "pw", "p@test.com");
    owner.setId(2L);

    Wrestler wrestler = Wrestler.builder().id(10L).name("Test Wrestler").build();

    League league = new League();
    league.setCommissioner(commissioner);

    LeagueRoster roster = new LeagueRoster();
    roster.setOwner(owner);
    roster.setLeague(league);

    Show show = new Show();
    show.setId(1L);
    show.setName("Monday Night Show");
    show.setLeague(league);
    show.setUniverse(new com.github.javydreamercsw.management.domain.universe.Universe());

    MatchFulfillment fulfillment = new MatchFulfillment();
    fulfillment.setId(99L);

    InboxItem inboxItem = new InboxItem();
    inboxItem.setEventType(matchRequestEventType);

    when(leagueRosterRepository.findByLeagueAndWrestler(eq(league), eq(wrestler)))
        .thenReturn(Optional.of(roster));
    when(matchFulfillmentRepository.findBySegment(any())).thenReturn(Optional.empty());
    when(matchFulfillmentRepository.save(any())).thenReturn(fulfillment);
    when(inboxService.createInboxItem(eq(matchRequestEventType), any(), any()))
        .thenReturn(inboxItem);
    when(inboxService.save(any())).thenAnswer(inv -> inv.getArgument(0));

    buildSegmentWithLeague(league, show, wrestler);

    ArgumentCaptor<InboxItem> captor = ArgumentCaptor.forClass(InboxItem.class);
    verify(inboxService).save(captor.capture());
    InboxItem saved = captor.getValue();
    assertThat(saved.getActionType()).isEqualTo("MATCH_REPORT");
    assertThat(saved.getActionPayload()).contains("99");
  }

  @Test
  void notifyLeagueParticipants_doesNotNotifyWhenFulfillmentAlreadyExists() {
    Account commissioner = new Account("commissioner", "pw", "c@test.com");
    commissioner.setId(1L);
    Account owner = new Account("player1", "pw", "p@test.com");
    owner.setId(2L);

    Wrestler wrestler = Wrestler.builder().id(10L).name("Test Wrestler").build();

    League league = new League();
    league.setCommissioner(commissioner);

    LeagueRoster roster = new LeagueRoster();
    roster.setOwner(owner);
    roster.setLeague(league);

    Show show = new Show();
    show.setId(1L);
    show.setName("Monday Night Show");
    show.setLeague(league);
    show.setUniverse(new com.github.javydreamercsw.management.domain.universe.Universe());

    MatchFulfillment existing = new MatchFulfillment();
    existing.setId(77L);

    when(leagueRosterRepository.findByLeagueAndWrestler(eq(league), eq(wrestler)))
        .thenReturn(Optional.of(roster));
    when(matchFulfillmentRepository.findBySegment(any())).thenReturn(Optional.of(existing));

    buildSegmentWithLeague(league, show, wrestler);

    org.mockito.Mockito.verify(inboxService, org.mockito.Mockito.never())
        .createInboxItem(any(), any(String.class), any(List.class));
  }
}
