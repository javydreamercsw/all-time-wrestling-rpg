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
    roundRepository.saveAndFlush(round2);
  }

  @Test
  void captureTournamentListView() {
    navigateTo("tournament-list");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));
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
    navigateTo("tournament-detail/" + scheduledId);
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.xpath("//*[contains(., 'Grand Prix Tournament')]"));
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
    navigateTo("tournament-detail/" + inProgressId);
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.xpath("//*[contains(., 'King of the Ring')]"));
    documentFeature(
        "Booker",
        "Tournament Detail — In Progress",
        "Live tournament view shows completed round results and the pending next round."
            + " The booker can book the upcoming round onto any scheduled show and record"
            + " match winners as each event is completed.",
        "booker-tournament-detail-in-progress");
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
