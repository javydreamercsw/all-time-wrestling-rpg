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
package com.github.javydreamercsw.management.ui.view.tournament;

import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntry;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntryRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentEntryStatus;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatch;
import com.github.javydreamercsw.management.domain.tournament.TournamentMatchRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRound;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundRepository;
import com.github.javydreamercsw.management.domain.tournament.TournamentRoundStatus;
import com.github.javydreamercsw.management.domain.tournament.TournamentStatus;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.domain.wrestler.WrestlerRepository;
import com.github.javydreamercsw.management.service.tournament.RoundRobinFormat;
import com.github.javydreamercsw.management.service.tournament.SingleEliminationFormat;
import com.github.javydreamercsw.management.ui.view.AbstractDocsE2ETest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;

class TournamentDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private TournamentRepository tournamentRepository;
  @Autowired private TournamentEntryRepository entryRepository;
  @Autowired private TournamentRoundRepository roundRepository;
  @Autowired private TournamentMatchRepository matchRepository;
  @Autowired private WrestlerRepository wrestlerRepository;

  private Long scheduledId;
  private Long inProgressId;
  private Long roundRobinId;

  @BeforeEach
  void seedTournaments() {
    List<Wrestler> wrestlers =
        seedWrestlers(
            "Apollo Vega",
            "Tormenta",
            "Johnny All Time",
            "El Diablo",
            "Lady Storm",
            "Rex Power",
            "The Phoenix",
            "Dark Omen");

    // Scheduled tournament (just created, no bracket yet)
    Tournament scheduled = new Tournament();
    scheduled.setName("Grand Prix Tournament");
    scheduled.setFormatId(SingleEliminationFormat.FORMAT_ID);
    scheduled.setStatus(TournamentStatus.SCHEDULED);
    scheduled.setStartDate(LocalDate.now().plusDays(7));
    scheduled.setEntries(new ArrayList<>());
    scheduled.setRounds(new ArrayList<>());
    scheduled = tournamentRepository.saveAndFlush(scheduled);
    scheduledId = scheduled.getId();

    for (int i = 0; i < 8; i++) {
      TournamentEntry e =
          TournamentEntry.builder()
              .tournament(scheduled)
              .wrestler(wrestlers.get(i))
              .seed(i + 1)
              .build();
      entryRepository.save(e);
    }
    entryRepository.flush();

    // In-progress tournament with Round 1 results
    Tournament inProgress = new Tournament();
    inProgress.setName("King of the Ring");
    inProgress.setFormatId(SingleEliminationFormat.FORMAT_ID);
    inProgress.setStatus(TournamentStatus.IN_PROGRESS);
    inProgress.setStartDate(LocalDate.now().minusDays(3));
    inProgress.setEntries(new ArrayList<>());
    inProgress.setRounds(new ArrayList<>());
    inProgress = tournamentRepository.saveAndFlush(inProgress);
    inProgressId = inProgress.getId();

    List<TournamentEntry> entries = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      TournamentEntry e =
          TournamentEntry.builder()
              .tournament(inProgress)
              .wrestler(wrestlers.get(i))
              .seed(i + 1)
              .status(i < 2 ? TournamentEntryStatus.ACTIVE : TournamentEntryStatus.ELIMINATED)
              .build();
      entries.add(entryRepository.saveAndFlush(e));
    }

    TournamentRound round1 =
        TournamentRound.builder()
            .tournament(inProgress)
            .roundNumber(1)
            .roundName("Quarter-Final")
            .status(TournamentRoundStatus.COMPLETE)
            .build();
    round1 = roundRepository.saveAndFlush(round1);

    TournamentMatch m1 =
        TournamentMatch.builder()
            .round(round1)
            .entrant1(entries.get(0))
            .entrant2(entries.get(2))
            .winner(entries.get(0))
            .build();
    TournamentMatch m2 =
        TournamentMatch.builder()
            .round(round1)
            .entrant1(entries.get(1))
            .entrant2(entries.get(3))
            .winner(entries.get(1))
            .build();
    matchRepository.saveAndFlush(m1);
    matchRepository.saveAndFlush(m2);

    TournamentRound round2 =
        TournamentRound.builder()
            .tournament(inProgress)
            .roundNumber(2)
            .roundName("Semi-Final")
            .status(TournamentRoundStatus.PENDING)
            .build();
    round2 = roundRepository.saveAndFlush(round2);

    // The open semi-final match — the standalone runner's Run Round / Run Tournament buttons
    // render only when a winnerless, unbooked match exists.
    TournamentMatch openSemi =
        TournamentMatch.builder()
            .round(round2)
            .entrant1(entries.get(0))
            .entrant2(entries.get(1))
            .build();
    matchRepository.saveAndFlush(openSemi);

    // Round-robin tournament (ATW-2aq3) with a partial table: A 2-0 leads, B/C/D follow.
    Tournament roundRobin = new Tournament();
    roundRobin.setName("Continental Cup");
    roundRobin.setFormatId(RoundRobinFormat.FORMAT_ID);
    roundRobin.setStatus(TournamentStatus.IN_PROGRESS);
    roundRobin.setStartDate(LocalDate.now().minusDays(3));
    roundRobin.setEntries(new ArrayList<>());
    roundRobin.setRounds(new ArrayList<>());
    roundRobin = tournamentRepository.saveAndFlush(roundRobin);
    roundRobinId = roundRobin.getId();

    List<TournamentEntry> rrEntries = new ArrayList<>();
    for (int i = 0; i < 4; i++) {
      TournamentEntry e =
          TournamentEntry.builder()
              .tournament(roundRobin)
              .wrestler(wrestlers.get(i))
              .seed(i + 1)
              .status(TournamentEntryStatus.ACTIVE)
              .build();
      rrEntries.add(entryRepository.saveAndFlush(e));
    }

    TournamentRound rrRound1 =
        TournamentRound.builder()
            .tournament(roundRobin)
            .roundNumber(1)
            .roundName("Round 1")
            .status(TournamentRoundStatus.COMPLETE)
            .build();
    rrRound1 = roundRepository.saveAndFlush(rrRound1);
    matchRepository.saveAndFlush(
        TournamentMatch.builder()
            .round(rrRound1)
            .entrant1(rrEntries.get(0))
            .entrant2(rrEntries.get(3))
            .winner(rrEntries.get(0))
            .build());
    matchRepository.saveAndFlush(
        TournamentMatch.builder()
            .round(rrRound1)
            .entrant1(rrEntries.get(1))
            .entrant2(rrEntries.get(2))
            .winner(rrEntries.get(1))
            .build());

    TournamentRound rrRound2 =
        TournamentRound.builder()
            .tournament(roundRobin)
            .roundNumber(2)
            .roundName("Round 2")
            .status(TournamentRoundStatus.PENDING)
            .build();
    roundRepository.saveAndFlush(rrRound2);
    matchRepository.saveAndFlush(
        TournamentMatch.builder()
            .round(rrRound2)
            .entrant1(rrEntries.get(0))
            .entrant2(rrEntries.get(1))
            .build());
    matchRepository.saveAndFlush(
        TournamentMatch.builder()
            .round(rrRound2)
            .entrant1(rrEntries.get(2))
            .entrant2(rrEntries.get(3))
            .build());
  }

  @Test
  void captureTournamentListView() {
    navigateToAndWaitForElement("tournament-list", By.tagName("vaadin-grid"));
    documentFeature(
        "Booker",
        "Tournament Bracket Generator",
        "Plan and run tournaments directly from the Booker dashboard. Choose from"
            + " Single Elimination or Round Robin formats, auto-seed wrestlers by fan count"
            + " or pick them manually, and book each round across your scheduled shows.",
        "booker-tournament-list");
  }

  @Test
  void captureTournamentDetailScheduled() {
    navigateToAndWaitForElement(
        "tournament-detail/" + scheduledId, By.xpath("//*[contains(., 'Grand Prix Tournament')]"));
    documentFeature(
        "Booker",
        "Tournament Detail — Scheduled",
        "A freshly created tournament shows all entrants seeded and ready."
            + " The 'Start Tournament' button generates the first-round bracket."
            + " Entrants are ranked by seed — 1 vs 8, 2 vs 7, and so on.",
        "booker-tournament-detail-scheduled");
  }

  @Test
  void captureTournamentDetailInProgress() {
    navigateToAndWaitForElement(
        "tournament-detail/" + inProgressId, By.xpath("//*[contains(., 'King of the Ring')]"));
    documentFeature(
        "Booker",
        "Tournament Detail — In Progress",
        "Live tournament view shows completed round results and the pending next round."
            + " The booker can book the upcoming round onto any scheduled show and record"
            + " match winners as each event is completed.",
        "booker-tournament-detail-in-progress");
  }

  @Test
  void captureTournamentCreationWizard() {
    navigateToAndWaitForElement("tournament-list", By.tagName("vaadin-grid"));
    clickButtonByText("New Tournament");
    waitForVaadinElement(driver, By.xpath("//*[contains(text(), 'New Tournament')]"));

    documentFeature(
        "Booker",
        "Tournament Creation Wizard",
        "Create a tournament in two steps. Step 1 names the tournament and picks the format"
            + " (Single Elimination or Round Robin), an optional linked championship, start"
            + " date, and allowed rules — the seeding step stays locked until these are set."
            + " Linking a championship also unlocks the contender-deciding option: the"
            + " tournament winner becomes the title's #1 contender instead of challenging."
            + " Step 2 chooses how entrants are seeded: automatically by fan count (with a"
            + " live match-up preview of what Create will build), manual wrestler picking,"
            + " or deferred so a paired show can seed it on approval.",
        "booker-tournament-creation-wizard");
  }

  @Test
  void captureTournamentSeedEditing() {
    navigateToAndWaitForElement(
        "tournament-detail/" + scheduledId, By.xpath("//*[contains(., 'Grand Prix Tournament')]"));
    // The reorder/swap columns only render on SCHEDULED, unstarted tournaments.
    waitForVaadinElement(driver, By.xpath("//*[contains(text(), 'Reorder')]"));
    waitForVaadinElement(driver, By.xpath("//*[contains(text(), 'Swap')]"));

    documentFeature(
        "Booker",
        "Editing Tournament Seeds",
        "Before the bracket is generated, seeds stay editable. Move an entrant up or down"
            + " to change its seed — the round-1 pairing is 1 vs last, 2 vs second-to-last,"
            + " so reordering directly reshapes the match-ups — or swap in a different"
            + " wrestler while keeping the seed. When the tournament is linked to a"
            + " championship, the current titleholder is excluded from seeding and from the"
            + " swap picker: the champion cannot win the belt from themselves. Seeds lock"
            + " once the tournament starts.",
        "booker-tournament-seed-editing");
  }

  @Test
  void captureArrangeMatchupsDialog() {
    navigateToAndWaitForElement(
        "tournament-detail/" + scheduledId, By.xpath("//*[contains(., 'Grand Prix Tournament')]"));
    // The arrange button only renders on SCHEDULED single-elimination tournaments.
    waitForVaadinElement(driver, By.xpath("//*[contains(text(), 'Arrange Match-ups')]"));
    clickButtonByText("Arrange Match-ups");
    waitForVaadinElement(driver, By.xpath("//*[contains(text(), 'Round 1 match-ups')]"));

    documentFeature(
        "Booker",
        "Tournament Bracket Generator — Arrange Match-ups",
        "Beyond dragging rows to reorder seeds, the round-1 opponents can be hand-picked:"
            + " each match pairs its wrestlers at the outer seed positions (1 vs last,"
            + " 2 vs second-to-last), so Start Tournament generates exactly the arranged"
            + " match-ups. Odd-sized brackets keep a bye at the middle seed.",
        "booker-tournament-arrange-matchups");
  }

  @Test
  void captureTournamentCreationWizardHostShow() {
    navigateToAndWaitForElement("tournament-list", By.tagName("vaadin-grid"));
    clickButtonByText("New Tournament");
    waitForVaadinElement(driver, By.xpath("//*[contains(text(), 'Host Show')]"));

    documentFeature(
        "Booker",
        "One-Time Tournament on a Host Show",
        "For a one-off tournament, attach it directly to a show instead of pairing it with"
            + " a PLE template (that stays reserved for recurring tournaments). Pick a host"
            + " show and the payoff books there exactly once — rounds pace automatically onto"
            + " the weekly shows before it. The payoff match type and rule default to"
            + " One-on-One but can be anything, e.g. a Free-for-All TLC match for a title.",
        "booker-tournament-host-show");
  }

  @Test
  void captureTournamentStandaloneRunner() {
    navigateToAndWaitForElement(
        "tournament-detail/" + inProgressId, By.xpath("//*[contains(., 'King of the Ring')]"));
    // The interactive run section renders for the earliest round holding open matches.
    waitForVaadinElement(driver, By.xpath("//*[contains(text(), 'Record Results')]"));

    documentFeature(
        "Booker",
        "Tournament — Standalone Runner",
        "Run a tournament without booking it on a show: pick each open match's winner"
            + " yourself, or use 'Decide by Engine' for a single match — full health,"
            + " stamina, fans and injury consequences, no AI provider needed. The bracket"
            + " advances automatically when a round's last match is decided, and payoffs"
            + " (trophy, briefcase, title opportunity) are granted at completion. No shows"
            + " yet? A '<code>&lt;tournament&gt; Night</code>' show is created automatically"
            + " to host the matches, or pick one from the Host show box.",
        "booker-tournament-standalone-runner");
  }

  @Test
  void captureRoundRobinStandings() {
    navigateToAndWaitForElement(
        "tournament-detail/" + roundRobinId, By.xpath("//*[contains(., 'Continental Cup')]"));
    waitForVaadinElement(driver, By.xpath("//*[contains(text(), 'Standings')]"));

    documentFeature(
        "Booker",
        "Tournament — Round Robin Standings",
        "Round robin tournaments score every match: a win earns 2 points, a draw 1, a loss"
            + " 0 — nobody is eliminated, everyone plays everyone. The live standings table"
            + " ranks entrants by points, breaking ties head-to-head (results among the tied"
            + " group) and then by seed. When the final round completes, the points leader"
            + " is crowned the tournament winner automatically.",
        "booker-tournament-round-robin-standings");
  }

  private List<Wrestler> seedWrestlers(String... names) {
    List<Wrestler> result = new ArrayList<>();
    for (String name : names) {
      Wrestler w =
          wrestlerRepository
              .findByName(name)
              .orElseGet(
                  () -> {
                    Wrestler newW = new Wrestler();
                    newW.setName(name);
                    return wrestlerRepository.saveAndFlush(newW);
                  });
      result.add(w);
    }
    return result;
  }
}
