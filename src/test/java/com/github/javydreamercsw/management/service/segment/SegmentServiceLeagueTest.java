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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.security.SecurityUtils;
import com.github.javydreamercsw.management.domain.campaign.CampaignRepository;
import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.league.League;
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
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SegmentServiceLeagueTest {

  @Mock private SegmentRepository segmentRepository;
  @Mock private LeagueRosterRepository leagueRosterRepository;
  @Mock private MatchFulfillmentRepository matchFulfillmentRepository;
  @Mock private InboxService inboxService;
  @Mock private InboxEventType matchRequestEventType;

  // Mocks for other dependencies required by constructor
  @Mock private TitleRepository titleRepository;
  @Mock private WrestlerService wrestlerService;
  @Mock private GameSettingService gameSettingService;
  @Mock private SecurityUtils securityUtils;
  @Mock private CampaignRepository campaignRepository;

  @InjectMocks private SegmentService segmentService;

  @Test
  void testAddParticipant_CreatesMatchFulfillmentAndNotification() {
    Account commissioner = new Account();
    commissioner.setId(1L);
    commissioner.setUsername("commish");

    Account player = new Account();
    player.setId(2L);
    player.setUsername("player");

    League league = new League();
    league.setCommissioner(commissioner);

    Show show = new Show();
    show.setName("Test Show");
    show.setLeague(league);

    Segment segment = new Segment();
    segment.setShow(show);

    Wrestler wrestler = new Wrestler();
    wrestler.setName("The Rock");

    LeagueRoster roster = new LeagueRoster();
    roster.setLeague(league);
    roster.setWrestler(wrestler);
    roster.setOwner(player);

    when(leagueRosterRepository.findByLeagueAndWrestler(league, wrestler))
        .thenReturn(Optional.of(roster));

    when(matchFulfillmentRepository.findBySegment(segment)).thenReturn(Optional.empty());

    segmentService.addParticipant(segment, wrestler);

    verify(matchFulfillmentRepository).save(any(MatchFulfillment.class));
    verify(inboxService).createInboxItem(any(InboxEventType.class), anyString(), anyString());
  }
}
