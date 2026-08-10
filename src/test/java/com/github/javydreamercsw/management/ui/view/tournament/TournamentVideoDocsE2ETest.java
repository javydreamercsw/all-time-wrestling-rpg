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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.springframework.beans.factory.annotation.Autowired;

@Tag("video")
class TournamentVideoDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private TournamentRepository tournamentRepository;
  @Autowired private TournamentEntryRepository entryRepository;
  @Autowired private TournamentRoundRepository roundRepository;
  @Autowired private TournamentMatchRepository matchRepository;
  @Autowired private WrestlerRepository wrestlerRepository;

  private Long tournamentId;

  @BeforeEach
  void seedTournament() {
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

    Tournament t = new Tournament();
    t.setName("King of the Ring");
    t.setFormatId(SingleEliminationFormat.FORMAT_ID);
    t.setStatus(TournamentStatus.IN_PROGRESS);
    t.setStartDate(LocalDate.now().minusDays(7));
    t.setEntries(new ArrayList<>());
    t.setRounds(new ArrayList<>());
    t = tournamentRepository.saveAndFlush(t);
    tournamentId = t.getId();

    List<TournamentEntry> entries = new ArrayList<>();
    for (int i = 0; i < 8; i++) {
      TournamentEntry e =
          TournamentEntry.builder().tournament(t).wrestler(wrestlers.get(i)).seed(i + 1).build();
      entries.add(entryRepository.saveAndFlush(e));
    }

    // Round 1: completed — seeds 1,2 advanced; seeds 3-8 eliminated
    TournamentRound round1 =
        TournamentRound.builder()
            .tournament(t)
            .roundNumber(1)
            .roundName("Quarter-Final")
            .status(TournamentRoundStatus.COMPLETE)
            .build();
    round1 = roundRepository.saveAndFlush(round1);

    for (int i = 0; i < 4; i++) {
      TournamentEntry winner = entries.get(i);
      TournamentEntry loser = entries.get(7 - i);
      matchRepository.saveAndFlush(
          TournamentMatch.builder()
              .round(round1)
              .entrant1(winner)
              .entrant2(loser)
              .winner(winner)
              .build());
    }

    // Round 2: pending — seeded 1v4 and 2v3
    TournamentRound round2 =
        TournamentRound.builder()
            .tournament(t)
            .roundNumber(2)
            .roundName("Semi-Final")
            .status(TournamentRoundStatus.PENDING)
            .build();
    round2 = roundRepository.saveAndFlush(round2);

    matchRepository.saveAndFlush(
        TournamentMatch.builder()
            .round(round2)
            .entrant1(entries.get(0))
            .entrant2(entries.get(3))
            .build());
    matchRepository.saveAndFlush(
        TournamentMatch.builder()
            .round(round2)
            .entrant1(entries.get(1))
            .entrant2(entries.get(2))
            .build());
  }

  @Test
  void recordTournamentWorkflow() {
    setVideoInfo("Booker", "Tournament Bracket Generator", "booker-tournament-bracket-workflow");

    navigateTo("tournament-list");
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.tagName("vaadin-grid"));
    captureCaption(
        "The Tournament List shows every active and completed tournament in your universe."
            + " Each row shows the format, status, entrant count, and start date."
            + " Click 'New Tournament' to launch the creation wizard.",
        3500);

    navigateTo("tournament-detail/" + tournamentId);
    waitForVaadinClientToLoad();
    waitForVaadinElement(driver, By.xpath("//*[contains(., 'King of the Ring')]"));
    captureCaption(
        "The Tournament Detail view is your bracket dashboard. At the top you see the"
            + " tournament name, format, status, and linked championship if any."
            + " Below are the seeded entrants and each round's match-up results.",
        4000);

    captureCaption(
        "The Quarter-Final round is complete — winners advanced, eliminated wrestlers are"
            + " greyed out. The Semi-Final round is pending and ready to be booked"
            + " onto an upcoming show using the 'Book Round on Show' button.",
        4500);

    captureCaption(
        "Once a round is booked, the matches appear on the selected show's segment list."
            + " As each match completes on the show, the booker records the winner here."
            + " The system automatically eliminates the loser and advances the winner.",
        4000);

    captureCaption(
        "The tournament engine supports Single Elimination and Round Robin formats."
            + " Single Elimination generates one round at a time, perfect for big"
            + " multi-week events. Round Robin pre-builds all match-ups so every"
            + " entrant faces every other entrant before a winner is determined.",
        5000);
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
