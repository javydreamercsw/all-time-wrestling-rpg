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
package com.github.javydreamercsw.management.ui.view;

import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.domain.wrestler.Wrestler;
import com.github.javydreamercsw.management.service.title.TitleOpportunityService;
import com.github.javydreamercsw.management.service.tournament.TournamentService;
import com.github.javydreamercsw.management.service.universe.UniverseService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Docs capture of the Money in the Bank-style briefcase (ATW-8p72): seeds a briefcase-deciding
 * tournament, completes it manually (its winner earns the held briefcase), then screenshots the
 * tournament detail view (payoff line + briefcase status) and the winner's career view with the
 * HELD badge and Cash In action.
 */
@Slf4j
class BriefcaseDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private TournamentService tournamentService;
  @Autowired private WrestlerService wrestlerService;
  @Autowired private UniverseService universeService;
  @Autowired private TitleOpportunityService titleOpportunityService;

  @Test
  void captureBriefcase() throws InterruptedException {
    Universe universe = universeService.findAll().iterator().next();

    Tournament tournament =
        tournamentService.createTournament(
            "Docs Time Vault",
            "SINGLE_ELIMINATION",
            universe,
            null,
            LocalDate.now(),
            null,
            null,
            null,
            null,
            null,
            false,
            true);
    tournamentService.seedAuto(tournament, 4, universe.getId());
    tournament =
        tournamentService
            .findByIdWithDetails(tournament.getId())
            .orElseThrow(() -> new IllegalStateException("Tournament vanished"));
    tournament = tournamentService.startTournament(tournament);

    // Complete the bracket by hand: no segments were booked, so the manual-completion path
    // grants the briefcase directly (adjudication only fires for booked finals).
    tournament =
        tournamentService
            .findByIdWithDetails(tournament.getId())
            .orElseThrow(() -> new IllegalStateException("Tournament vanished"));
    var round1Matches = tournament.getRounds().get(0).getMatches();
    round1Matches.forEach(match -> tournamentService.recordMatchResult(match, match.getEntrant1()));
    // Advance generates the final (the bracket isn't complete until it is decided); record it,
    // then advance again — that call flips COMPLETE and grants the briefcase.
    var finalMatches = tournamentService.advanceToNextRound(tournament);
    finalMatches.forEach(match -> tournamentService.recordMatchResult(match, match.getEntrant1()));
    tournamentService.advanceToNextRound(tournament);
    Wrestler winner =
        tournamentService
            .findByIdWithDetails(tournament.getId())
            .orElseThrow()
            .getEntries()
            .stream()
            .filter(
                e ->
                    e.getStatus()
                        == com.github.javydreamercsw.management.domain.tournament
                            .TournamentEntryStatus.WINNER)
            .map(e -> e.getWrestler())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No winner recorded"));

    // Tournament detail: the payoff line and the winner's briefcase status.
    navigateTo("tournament-detail/" + tournament.getId());
    waitForVaadinElement(driver, By.xpath("//*[contains(., 'Briefcase')]"));
    documentFeature(
        "Game Mechanics",
        "Money in the Bank Briefcase",
        "A briefcase-deciding tournament (like the annual Time Vault) pays off with a cashable"
            + " briefcase instead of a championship: the detail view shows the payoff mode and"
            + " the winner's briefcase status (held with its cashable-until date, cashed in, or"
            + " expired).",
        "mechanic-briefcase-tournament");

    // Career view: the HELD badge and the Cash In action.
    navigateTo("wrestler-career/" + winner.getId());
    waitForVaadinElement(driver, By.xpath("//*[contains(., 'Briefcase')]"));
    WebElement section = driver.findElement(By.xpath("//h3[text()='Briefcase']"));
    ((JavascriptExecutor) driver)
        .executeScript("arguments[0].scrollIntoView({block: 'start'});", section);
    Thread.sleep(500);
    documentFeature(
        "Game Mechanics",
        "Cash In the Briefcase",
        "The holder's career view shows the held briefcase with its expiry and a Cash In action:"
            + " pick any active championship with a reigning champion and a show, and the title"
            + " match books on the spot. The briefcase is spent whether the match is won or"
            + " lost.",
        "mechanic-briefcase-cash-in");
  }
}
