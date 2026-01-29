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
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DraftService {

  private final DraftRepository draftRepository;
  private final DraftPickRepository draftPickRepository;
  private final LeagueMembershipRepository leagueMembershipRepository;
  private final LeagueRosterRepository leagueRosterRepository;

  @Transactional
  public Draft startDraft(League league) {
    if (draftRepository.findByLeague(league).isPresent()) {
      throw new IllegalStateException("Draft already exists for this league.");
    }

    // Ensure we have players
    List<LeagueMembership> members = leagueMembershipRepository.findByLeague(league);
    if (members.isEmpty()) {
      throw new IllegalStateException("League has no members.");
    }

    Draft draft = new Draft();
    draft.setLeague(league);
    draft.setStatus(Draft.DraftStatus.ACTIVE);
    draft.setCurrentRound(1);
    draft.setCurrentPickNumber(1);
    draft.setDirection(1); // Start going forward

    // Set first player (sorted by ID or join date? Let's assume ID for deterministic order for now)
    members.sort(Comparator.comparing(m -> m.getMember().getId()));
    draft.setCurrentTurnUser(members.get(0).getMember());

    return draftRepository.save(draft);
  }

  @Transactional
  public DraftPick makePick(Draft draft, Account user, Wrestler wrestler) {
    if (draft.getStatus() != Draft.DraftStatus.ACTIVE) {
      throw new IllegalStateException("Draft is not active.");
    }

    if (!draft.getCurrentTurnUser().equals(user)) {
      throw new IllegalStateException("It is not " + user.getUsername() + "'s turn.");
    }

    // Create Pick Record
    DraftPick pick = new DraftPick();
    pick.setDraft(draft);
    pick.setUser(user);
    pick.setWrestler(wrestler);
    pick.setRound(draft.getCurrentRound());
    pick.setPickNumber(draft.getCurrentPickNumber());
    draftPickRepository.save(pick);

    // Add to League Roster
    LeagueRoster roster = new LeagueRoster();
    roster.setLeague(draft.getLeague());
    roster.setOwner(user);
    roster.setWrestler(wrestler);
    leagueRosterRepository.save(roster);

    // Advance Turn
    advanceTurn(draft);

    return pick;
  }

  private void advanceTurn(Draft draft) {
    List<LeagueMembership> members = leagueMembershipRepository.findByLeague(draft.getLeague());
    members.sort(Comparator.comparing(m -> m.getMember().getId()));

    int currentIndex = -1;
    for (int i = 0; i < members.size(); i++) {
      if (members.get(i).getMember().equals(draft.getCurrentTurnUser())) {
        currentIndex = i;
        break;
      }
    }

    int nextIndex = currentIndex + draft.getDirection();

    // Check boundaries for Snake Draft
    if (nextIndex >= members.size()) {
      // End of round, snake back
      draft.setDirection(-1);
      draft.setCurrentRound(draft.getCurrentRound() + 1);
      // In snake draft, the last person picks again immediately
      // e.g., 1, 2, 3, 3, 2, 1
      // So if index was 2 (size 3), next is still 2.
      nextIndex = currentIndex;
    } else if (nextIndex < 0) {
      // Start of round, snake forward
      draft.setDirection(1);
      draft.setCurrentRound(draft.getCurrentRound() + 1);
      // In snake draft, first person picks again
      // 3, 2, 1, 1, 2, 3
      nextIndex = currentIndex;
    }

    draft.setCurrentTurnUser(members.get(nextIndex).getMember());
    draft.setCurrentPickNumber(draft.getCurrentPickNumber() + 1);
    draftRepository.save(draft);
  }
}
