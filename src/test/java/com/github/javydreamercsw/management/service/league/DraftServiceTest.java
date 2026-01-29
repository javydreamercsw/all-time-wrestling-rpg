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
package com.github.javydreamercsw.management.service.league;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.management.domain.league.Draft;
import com.github.javydreamercsw.management.domain.league.DraftPick;
import com.github.javydreamercsw.management.domain.league.DraftPickRepository;
import com.github.javydreamercsw.management.domain.league.DraftRepository;
import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.league.LeagueMembership;
import com.github.javydreamercsw.management.domain.league.LeagueMembershipRepository;
import com.github.javydreamercsw.management.domain.league.LeagueRoster;
import com.github.javydreamercsw.management.domain.league.LeagueRosterRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DraftServiceTest {

  @Mock private DraftRepository draftRepository;
  @Mock private DraftPickRepository draftPickRepository;
  @Mock private LeagueMembershipRepository leagueMembershipRepository;
  @Mock private LeagueRosterRepository leagueRosterRepository;

  @InjectMocks private DraftService draftService;

  @Test
  void testStartDraft() {
    League league = new League();
    Account p1 = new Account();
    p1.setId(1L);
    Account p2 = new Account();
    p2.setId(2L);

    LeagueMembership m1 = new LeagueMembership();
    m1.setMember(p1);
    LeagueMembership m2 = new LeagueMembership();
    m2.setMember(p2);

    when(draftRepository.findByLeague(league)).thenReturn(Optional.empty());
    when(leagueMembershipRepository.findByLeague(league))
        .thenReturn(new ArrayList<>(List.of(m2, m1))); // Unordered
    when(draftRepository.save(any(Draft.class))).thenAnswer(i -> i.getArgument(0));

    Draft draft = draftService.startDraft(league);

    assertThat(draft.getCurrentTurnUser()).isEqualTo(p1); // Should sort by ID
    assertThat(draft.getStatus()).isEqualTo(Draft.DraftStatus.ACTIVE);
  }

  @Test
  void testMakePick_SnakeDraftLogic() {
    League league = new League();
    Account p1 = new Account();
    p1.setId(1L);
    p1.setUsername("P1");
    Account p2 = new Account();
    p2.setId(2L);
    p2.setUsername("P2");

    LeagueMembership m1 = new LeagueMembership();
    m1.setMember(p1);
    LeagueMembership m2 = new LeagueMembership();
    m2.setMember(p2);

    Draft draft = new Draft();
    draft.setLeague(league);
    draft.setStatus(Draft.DraftStatus.ACTIVE);
    draft.setCurrentTurnUser(p1);
    draft.setDirection(1);
    draft.setCurrentRound(1);
    draft.setCurrentPickNumber(1);

    when(leagueMembershipRepository.findByLeague(league))
        .thenReturn(new ArrayList<>(List.of(m1, m2)));
    when(draftRepository.save(any(Draft.class))).thenAnswer(i -> i.getArgument(0));

    // Pick 1: P1
    draftService.makePick(draft, p1, new Wrestler());
    assertThat(draft.getCurrentTurnUser()).isEqualTo(p2);
    assertThat(draft.getCurrentRound()).isEqualTo(1);

    // Pick 2: P2 (End of Round 1, Snake back)
    draftService.makePick(draft, p2, new Wrestler());
    assertThat(draft.getCurrentTurnUser()).isEqualTo(p2); // P2 picks again!
    assertThat(draft.getCurrentRound()).isEqualTo(2);
    assertThat(draft.getDirection()).isEqualTo(-1);

    // Pick 3: P2 (Round 2 start)
    draftService.makePick(draft, p2, new Wrestler());
    assertThat(draft.getCurrentTurnUser()).isEqualTo(p1);
    assertThat(draft.getCurrentRound()).isEqualTo(2);

    // Pick 4: P1 (End of Round 2, Snake forward)
    draftService.makePick(draft, p1, new Wrestler());
    assertThat(draft.getCurrentTurnUser()).isEqualTo(p1); // P1 picks again
    assertThat(draft.getCurrentRound()).isEqualTo(3);
    assertThat(draft.getDirection()).isEqualTo(1);

    verify(leagueRosterRepository, times(4)).save(any(LeagueRoster.class));
    verify(draftPickRepository, times(4)).save(any(DraftPick.class));
  }
}
