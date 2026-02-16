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
package com.github.javydreamercsw.management.service.league; /*
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

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.management.domain.inbox.InboxEventType;
import com.github.javydreamercsw.management.domain.inbox.InboxItemTarget;
import com.github.javydreamercsw.management.domain.league.Draft;
import com.github.javydreamercsw.management.domain.league.DraftPick;
import com.github.javydreamercsw.management.domain.league.DraftPickRepository;
import com.github.javydreamercsw.management.domain.league.DraftRepository;
import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.league.LeagueMembership;
import com.github.javydreamercsw.management.domain.league.LeagueMembershipRepository;
import com.github.javydreamercsw.management.domain.league.LeagueRepository;
import com.github.javydreamercsw.management.domain.league.LeagueRoster;
import com.github.javydreamercsw.management.domain.league.LeagueRosterRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.event.league.DraftBroadcaster;
import com.github.javydreamercsw.management.event.league.DraftUpdateEvent;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class DraftService {

  private final DraftRepository draftRepository;

  private final DraftPickRepository draftPickRepository;

  private final LeagueMembershipRepository leagueMembershipRepository;

  private final LeagueRosterRepository leagueRosterRepository;

  private final LeagueRepository leagueRepository;

  private final WrestlerRepository wrestlerRepository;
  private final AccountRepository accountRepository;
  private final DraftBroadcaster draftBroadcaster;
  private final InboxService inboxService;
  private final InboxEventType draftStartedEventType;

  public DraftService(
      DraftRepository draftRepository,
      DraftPickRepository draftPickRepository,
      LeagueMembershipRepository leagueMembershipRepository,
      LeagueRosterRepository leagueRosterRepository,
      LeagueRepository leagueRepository,
      WrestlerRepository wrestlerRepository,
      AccountRepository accountRepository,
      DraftBroadcaster draftBroadcaster,
      InboxService inboxService,
      @Qualifier("DRAFT_STARTED") InboxEventType draftStartedEventType) {
    this.draftRepository = draftRepository;
    this.draftPickRepository = draftPickRepository;
    this.leagueMembershipRepository = leagueMembershipRepository;
    this.leagueRosterRepository = leagueRosterRepository;
    this.leagueRepository = leagueRepository;
    this.wrestlerRepository = wrestlerRepository;
    this.accountRepository = accountRepository;
    this.draftBroadcaster = draftBroadcaster;
    this.inboxService = inboxService;
    this.draftStartedEventType = draftStartedEventType;
  }

  @Transactional
  public Draft startDraft(League league) {
    if (draftRepository.findByLeague(league).isPresent()) {
      throw new IllegalStateException("Draft already exists for this league.");
    }

    // Ensure we have players
    List<LeagueMembership> players =
        leagueMembershipRepository.findByLeagueAndRoleIn(
            league,
            List.of(
                LeagueMembership.LeagueRole.PLAYER,
                LeagueMembership.LeagueRole.COMMISSIONER_PLAYER));

    if (players.isEmpty()) {
      throw new IllegalStateException("League has no players.");
    }

    Draft draft = new Draft();
    draft.setLeague(league);
    draft.setStatus(Draft.DraftStatus.ACTIVE);
    draft.setCurrentRound(1);
    draft.setCurrentPickNumber(1);
    draft.setDirection(1); // Start going forward

    // Set first player (sorted by ID)
    players.sort(Comparator.comparing(m -> m.getMember().getId()));
    draft.setCurrentTurnUser(players.get(0).getMember());

    Draft saved = draftRepository.save(draft);

    // Update League status
    league.setStatus(League.LeagueStatus.DRAFTING);
    leagueRepository.save(league);

    // Send notifications to all participants (even viewers?)
    List<LeagueMembership> allMembers = leagueMembershipRepository.findByLeague(league);
    for (LeagueMembership member : allMembers) {
      inboxService.createInboxItem(
          draftStartedEventType,
          "The draft for league '" + league.getName() + "' has started!",
          member.getMember().getId().toString(),
          InboxItemTarget.TargetType.ACCOUNT);
    }

    draftBroadcaster.broadcast(new DraftUpdateEvent(saved.getId()));
    return saved;
  }

  @Transactional
  public DraftPick makePick(Draft draft, Account user, Wrestler wrestler) {
    if (draft.getStatus() != Draft.DraftStatus.ACTIVE) {
      throw new IllegalStateException("Draft is not active.");
    }

    if (draft.getCurrentTurnUser() == null || !draft.getCurrentTurnUser().equals(user)) {
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

    // Reload account to get fresh state including lazy collections and activeWrestlerId
    Account reloadedUser =
        accountRepository
            .findById(user.getId())
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + user.getId()));

    // Assign to account if user doesn't have one assigned (respecting unique constraint)
    if (wrestlerRepository.findByAccount(reloadedUser).isEmpty()) {
      wrestler.setAccount(reloadedUser);
    }
    wrestler.setIsPlayer(true);
    wrestlerRepository.saveAndFlush(wrestler);

    // Set as active wrestler if none set
    if (reloadedUser.getActiveWrestlerId() == null) {
      reloadedUser.setActiveWrestlerId(wrestler.getId());
      accountRepository.saveAndFlush(reloadedUser);
    }

    // Advance Turn
    advanceTurn(draft);

    draftBroadcaster.broadcast(new DraftUpdateEvent(draft.getId()));

    return pick;
  }

  private void advanceTurn(Draft draft) {
    League league = draft.getLeague();
    List<LeagueMembership> players =
        leagueMembershipRepository.findByLeagueAndRoleIn(
            league,
            List.of(
                LeagueMembership.LeagueRole.PLAYER,
                LeagueMembership.LeagueRole.COMMISSIONER_PLAYER));
    players.sort(Comparator.comparing(m -> m.getMember().getId()));

    long availableWrestlers = countAvailableWrestlers(league);

    // Check if draft is over (Total picks reached)
    int totalPicks = draft.getCurrentPickNumber();
    int maxTotalPicks = players.size() * league.getMaxPicksPerPlayer();

    log.info(
        "Checking draft progress: Pick {}/{}, Available Wrestlers: {}",
        totalPicks,
        maxTotalPicks,
        availableWrestlers);

    if (totalPicks >= maxTotalPicks || availableWrestlers == 0) {
      log.info(
          "Draft completing. Reason: totalPicks ({}) >= maxTotalPicks ({}) OR availableWrestlers"
              + " ({}) == 0",
          totalPicks,
          maxTotalPicks,
          availableWrestlers);
      completeDraft(draft);
      return;
    }

    int currentIndex = -1;
    for (int i = 0; i < players.size(); i++) {
      if (players.get(i).getMember().equals(draft.getCurrentTurnUser())) {
        currentIndex = i;
        break;
      }
    }

    int nextIndex = currentIndex + draft.getDirection();

    // Check boundaries for Snake Draft
    if (nextIndex >= players.size()) {
      // End of round, snake back
      draft.setDirection(-1);
      draft.setCurrentRound(draft.getCurrentRound() + 1);
      nextIndex = currentIndex;
    } else if (nextIndex < 0) {
      // Start of round, snake forward
      draft.setDirection(1);
      draft.setCurrentRound(draft.getCurrentRound() + 1);
      nextIndex = currentIndex;
    }

    draft.setCurrentTurnUser(players.get(nextIndex).getMember());
    draft.setCurrentPickNumber(draft.getCurrentPickNumber() + 1);
    draftRepository.save(draft);
  }

  private void completeDraft(Draft draft) {
    draft.setStatus(Draft.DraftStatus.COMPLETED);
    draft.setCurrentTurnUser(null);
    draftRepository.save(draft);

    League league = draft.getLeague();
    league.setStatus(League.LeagueStatus.SEASON_ACTIVE);
    leagueRepository.save(league);
  }

  private long countAvailableWrestlers(League league) {
    Set<Long> draftedWrestlerIds =
        leagueRosterRepository.findByLeague(league).stream()
            .map(r -> r.getWrestler().getId())
            .collect(Collectors.toSet());

    // Fetch league with excluded wrestlers to avoid LazyInitializationException
    League fullyLoaded =
        leagueRepository.findByIdWithExcludedWrestlers(league.getId()).orElse(league);

    Set<Long> excludedWrestlerIds =
        fullyLoaded.getExcludedWrestlers().stream()
            .map(Wrestler::getId)
            .collect(Collectors.toSet());

    return wrestlerRepository.findAll().stream()
        .filter(w -> !draftedWrestlerIds.contains(w.getId()))
        .filter(w -> !excludedWrestlerIds.contains(w.getId()))
        .count();
  }
}
