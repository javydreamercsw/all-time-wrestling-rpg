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
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.javydreamercsw.base.domain.account.Account;
import com.github.javydreamercsw.base.domain.account.AccountRepository;
import com.github.javydreamercsw.base.domain.account.RoleName;
import com.github.javydreamercsw.management.ManagementIntegrationTest;
import com.github.javydreamercsw.management.domain.AdjudicationStatus;
import com.github.javydreamercsw.management.domain.inbox.InboxItem;
import com.github.javydreamercsw.management.domain.league.Draft;
import com.github.javydreamercsw.management.domain.league.DraftRepository;
import com.github.javydreamercsw.management.domain.league.League;
import com.github.javydreamercsw.management.domain.league.LeagueRoster;
import com.github.javydreamercsw.management.domain.league.LeagueRosterRepository;
import com.github.javydreamercsw.management.domain.league.MatchFulfillment;
import com.github.javydreamercsw.management.domain.league.MatchFulfillmentRepository;
import com.github.javydreamercsw.management.domain.show.Show;
import com.github.javydreamercsw.management.domain.show.segment.Segment;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentType;
import com.github.javydreamercsw.management.domain.show.segment.type.SegmentTypeRepository;
import com.github.javydreamercsw.management.domain.show.type.ShowType;
import com.github.javydreamercsw.management.domain.show.type.ShowTypeRepository;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.inbox.InboxService;
import com.github.javydreamercsw.management.service.segment.SegmentService;
import com.github.javydreamercsw.management.service.show.ShowService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

class LeagueLifecycleIT extends ManagementIntegrationTest {

  @Autowired private LeagueService leagueService;
  @Autowired private DraftService draftService;
  @Autowired private DraftRepository draftRepository;
  @Autowired private ShowService showService;
  @Autowired private SegmentService segmentService;
  @Autowired private WrestlerService wrestlerService;
  @Autowired private WrestlerRepository wrestlerRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private LeagueRosterRepository leagueRosterRepository;
  @Autowired private MatchFulfillmentRepository matchFulfillmentRepository;
  @Autowired private MatchFulfillmentService matchFulfillmentService;
  @Autowired private InboxService inboxService;
  @Autowired private ShowTypeRepository showTypeRepository;
  @Autowired private SegmentTypeRepository segmentTypeRepository;
  @Autowired private EntityManager entityManager;

  @Autowired
  @Qualifier("managementAccountService") private com.github.javydreamercsw.management.service.AccountService accountService;

  @Test
  @Transactional
  void testFullLeagueLifecycle() {
    // 1. Setup Accounts
    Account commish = createLeagueTestAccount("commish", RoleName.ADMIN);
    Account player1 = createLeagueTestAccount("player1", RoleName.PLAYER);
    accountRepository.flush();

    // 2. Setup Wrestler Pool
    Wrestler w1 = createWrestler("Hulk Hogan");
    Wrestler w2 = createWrestler("Randy Savage");
    Wrestler w3 = createWrestler("The Undertaker");
    Wrestler w4 = createWrestler("Bret Hart");
    Wrestler w5 = createWrestler("Shawn Michaels");
    Wrestler w6 = createWrestler("Stone Cold");
    Wrestler w7 = createWrestler("The Rock");
    Wrestler w8 = createWrestler("John Cena");
    wrestlerRepository.flush();

    // 3. League Creation (Step 1 of Task List)
    // Commissioner also plays, max 2 picks per player, Randy Savage excluded
    League league =
        leagueService.createLeague("Hardening League 2026", commish, 2, Set.of(w2), true);
    leagueService.addPlayer(league, player1);

    assertThat(league.getStatus()).isEqualTo(League.LeagueStatus.PRE_DRAFT);
    assertThat(league.getMaxPicksPerPlayer()).isEqualTo(2);
    assertThat(league.getExcludedWrestlers()).contains(w2);

    // 4. Snake Draft (Step 2 of Task List)
    Draft draft = draftService.startDraft(league);
    assertThat(draft.getStatus()).isEqualTo(Draft.DraftStatus.ACTIVE);
    // commish is sorted by ID, but let's just check it's one of the players
    assertThat(draft.getCurrentTurnUser()).isNotNull();

    // Notifications sent? (Check if any notification exists for player1)
    assertThat(inboxService.getInboxItemsForWrestler(w1, 10)).isEmpty(); // Not yet drafted

    // Simulate picks based on whoever's turn it is
    int safetyCounter = 0;
    Set<Wrestler> locallyDrafted = new java.util.HashSet<>();
    final League finalLeague = league;
    final Draft finalDraft = draft;
    while (draft.getStatus() == Draft.DraftStatus.ACTIVE && safetyCounter < 10) {
      Account current = draft.getCurrentTurnUser();
      List<Wrestler> allWrestlers = wrestlerRepository.findAll();
      Wrestler nextAvailable =
          allWrestlers.stream()
              .filter(w -> w.getAccount() == null)
              .filter(w -> !finalLeague.getExcludedWrestlers().contains(w))
              .filter(w -> !locallyDrafted.contains(w))
              .findFirst()
              .orElseThrow(
                  () ->
                      new AssertionError(
                          "No wrestlers available for pick " + finalDraft.getCurrentPickNumber()));

      draftService.makePick(draft, current, nextAvailable);
      locallyDrafted.add(nextAvailable);
      safetyCounter++;
    }

    entityManager.flush();
    entityManager.clear();

    // Reload objects after clear
    league = leagueService.getLeagueById(league.getId()).orElseThrow();
    draft = draftRepository.findByLeague(league).orElseThrow();

    // Draft should complete
    assertThat(draft.getStatus()).isEqualTo(Draft.DraftStatus.COMPLETED);
    assertThat(league.getStatus()).isEqualTo(League.LeagueStatus.SEASON_ACTIVE);

    // Verify accounts have multiple wrestlers and activeWrestlerId is set
    commish = accountRepository.findById(commish.getId()).get();
    player1 = accountRepository.findById(player1.getId()).get();

    List<LeagueRoster> commishRoster = leagueRosterRepository.findByLeagueAndOwner(league, commish);
    assertThat(commishRoster).hasSize(2);
    assertThat(commish.getActiveWrestlerId()).isNotNull();

    List<LeagueRoster> player1Roster = leagueRosterRepository.findByLeagueAndOwner(league, player1);
    assertThat(player1Roster).hasSize(2);
    assertThat(player1.getActiveWrestlerId()).isNotNull();

    // 5. Booking League Match (Step 3)
    ShowType weekly =
        showTypeRepository
            .findByName("Weekly")
            .orElseGet(
                () -> {
                  ShowType st = new ShowType();
                  st.setName("Weekly");
                  st.setDescription("Weekly league show");
                  st.setExpectedMatches(3);
                  st.setExpectedPromos(2);
                  return showTypeRepository.save(st);
                });
    Show show =
        showService.createShow(
            "League Show #1",
            "Weekly league match",
            weekly.getId(),
            LocalDate.now(),
            null,
            null,
            league.getId(),
            null);
    show = showRepository.saveAndFlush(show);
    entityManager.clear();
    show = showRepository.findById(show.getId()).get();

    SegmentType matchType =
        segmentTypeRepository
            .findByName("One on One")
            .orElseGet(
                () -> {
                  SegmentType st = new SegmentType();
                  st.setName("One on One");
                  st.setDescription("One on One match");
                  return segmentTypeRepository.save(st);
                });
    segmentTypeRepository.flush();

    Segment match = segmentService.createSegment(show, matchType, java.time.Instant.now());
    match = segmentRepository.saveAndFlush(match);
    entityManager.clear();
    match = segmentRepository.findById(match.getId()).get();

    // Add player1's wrestler to match
    player1 = accountRepository.findById(player1.getId()).get();
    Wrestler p1Wrestler = wrestlerRepository.findByAccount(player1).get(0);
    segmentService.addParticipant(match, p1Wrestler);

    // Verify Notification (Step 4.3)
    java.util.List<InboxItem> p1Inbox = inboxService.getInboxItemsForWrestler(p1Wrestler, 10);
    assertThat(p1Inbox).isNotEmpty();
    assertThat(p1Inbox.get(0).getDescription()).contains("Pending match on show");

    // Add commish's wrestler
    commish = accountRepository.findById(commish.getId()).get();
    Wrestler cWrestler = wrestlerRepository.findByAccount(commish).get(0);
    segmentService.addParticipant(match, cWrestler);
    // Verify MatchFulfillment created
    MatchFulfillment fulfillment =
        matchFulfillmentRepository
            .findBySegment(match)
            .orElseThrow(() -> new AssertionError("Fulfillment not created"));
    assertThat(fulfillment.getStatus())
        .isEqualTo(MatchFulfillment.FulfillmentStatus.PENDING_RESULTS);

    // 6. Player Reporting (Step 4)
    // Player1 reports that their wrestler won
    matchFulfillmentService.submitResult(fulfillment, p1Wrestler, player1);

    matchFulfillmentRepository.flush();
    entityManager.clear();

    fulfillment = matchFulfillmentRepository.findById(fulfillment.getId()).orElseThrow();

    assertThat(fulfillment.getStatus()).isEqualTo(MatchFulfillment.FulfillmentStatus.SUBMITTED);
    assertThat(fulfillment.getReportedWinner()).isEqualTo(p1Wrestler);

    // 7. Finalization (Step 5)
    // Commissioner (or Admin) adjudicates the show
    showService.adjudicateShow(show.getId());

    // Verify results
    match = segmentRepository.findById(match.getId()).get();
    assertThat(match.getAdjudicationStatus()).isEqualTo(AdjudicationStatus.ADJUDICATED);
    assertThat(match.getWinners()).contains(p1Wrestler);

    fulfillment = matchFulfillmentRepository.findById(fulfillment.getId()).get();
    assertThat(fulfillment.getStatus()).isEqualTo(MatchFulfillment.FulfillmentStatus.FINALIZED);

    // 8. CRUD & Shows constraint
    final Long leagueId = league.getId();
    assertThrows(
        IllegalStateException.class,
        () -> leagueService.deleteLeague(leagueId),
        "Should not be able to delete league with shows");

    matchFulfillmentRepository.delete(fulfillment);
    matchFulfillmentRepository.flush();

    segmentRepository.delete(match);
    segmentRepository.flush();

    showService.deleteShow(show.getId());
    showRepository.flush();
    entityManager.clear();

    leagueService.deleteLeague(leagueId);
    assertThat(leagueService.getLeagueById(leagueId)).isEmpty();
  }

  private Account createLeagueTestAccount(String username, RoleName role) {
    return accountRepository
        .findByUsername(username)
        .orElseGet(
            () -> {
              Account a = new Account(username, "password123", username + "@test.com");
              a.addRole(roleRepository.findByName(role).get());
              return accountRepository.save(a);
            });
  }

  private Wrestler createWrestler(String name) {
    return wrestlerService.createWrestler(
        name,
        false,
        "Test",
        com.github.javydreamercsw.base.domain.wrestler.WrestlerTier.MIDCARDER,
        null);
  }
}
