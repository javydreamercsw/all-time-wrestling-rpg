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

import com.github.javydreamercsw.management.domain.show.segment.rule.SegmentRule;
import com.github.javydreamercsw.management.domain.tournament.Tournament;
import com.github.javydreamercsw.management.domain.universe.Universe;
import com.github.javydreamercsw.management.service.segment.SegmentRuleService;
import com.github.javydreamercsw.management.service.tournament.TournamentService;
import com.github.javydreamercsw.management.service.universe.UniverseService;
import com.github.javydreamercsw.management.service.wrestler.WrestlerService;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Docs capture of the tournament bracket: a 16-entrant single-elimination tournament — 4 rounds
 * (Round 1, Quarter-Final, Semi-Final, Final) — with connector lines, advancing winners replacing
 * their "Winner of Match N" placeholders, the inline type·rule labels, and the Fit/100% zoom
 * controls. Seeds its own SCHEDULED tournament, starts it (round-1 bracket generates), records the
 * first two round-1 results (both feed the same quarter-final, so its card shows two real advancing
 * names), then screenshots the detail view.
 */
@Slf4j
class TournamentBracketDocsE2ETest extends AbstractDocsE2ETest {

  @Autowired private TournamentService tournamentService;
  @Autowired private WrestlerService wrestlerService;
  @Autowired private UniverseService universeService;
  @Autowired private SegmentRuleService segmentRuleService;

  @Test
  void captureTournamentBracket() throws InterruptedException {
    Universe universe = universeService.findAll().iterator().next();

    List<SegmentRule> pool =
        segmentRuleService.findAll().stream()
            .filter(r -> "No DQ".equals(r.getName()) || "Gauntlet".equals(r.getName()))
            .toList();

    Tournament tournament =
        tournamentService.createTournament(
            "Docs Bracket Showcase",
            "SINGLE_ELIMINATION",
            universe,
            null,
            LocalDate.now(),
            pool.isEmpty() ? null : pool);
    tournamentService.seedAuto(tournament, 16, universe.getId());
    // seedAuto persists entries through the repository — re-fetch so startTournament sees them
    // (the in-memory instance's entries collection is stale after the seed).
    tournament =
        tournamentService
            .findByIdWithDetails(tournament.getId())
            .orElseThrow(() -> new IllegalStateException("Tournament vanished"));
    tournament = tournamentService.startTournament(tournament);

    // Reload with the full graph, record the first two round-1 results — both winners advance
    // into the same quarter-final card in the projection.
    tournament =
        tournamentService
            .findByIdWithDetails(tournament.getId())
            .orElseThrow(() -> new IllegalStateException("Tournament vanished"));
    var round1Matches = tournament.getRounds().get(0).getMatches();
    tournamentService.recordMatchResult(round1Matches.get(0), round1Matches.get(0).getEntrant1());
    tournamentService.recordMatchResult(round1Matches.get(1), round1Matches.get(1).getEntrant1());

    navigateTo("tournament-detail/" + tournament.getId());
    waitForVaadinElement(driver, By.xpath("//*[contains(., 'Bracket')]"));
    // Round names prove the full 4-round projection rendered (lazy generation persists only
    // round 1 so far — the rest are projected client-side data).
    waitForVaadinElement(driver, By.xpath("//*[contains(., 'Semi-Final')]"));

    // The bracket renders below the entrants table — scroll its section heading into view, or
    // the viewport screenshot captures the table instead. Wait a beat so the connector overlay
    // redraws after the scroll (its ResizeObserver fires on the scrolled position).
    WebElement bracketHeading = driver.findElement(By.xpath("//h4[text()='Bracket']"));
    ((JavascriptExecutor) driver)
        .executeScript("arguments[0].scrollIntoView({block: 'start'});", bracketHeading);
    Thread.sleep(500);

    documentFeature(
        "Game Mechanics",
        "Tournament Bracket",
        "The full projected 4-round bracket (Round 1 through Quarter-Final, Semi-Final and"
            + " Final): connector lines between rounds, decided winners advancing (replacing"
            + " 'Winner of Match N' placeholders), losers struck through, and Fit/100% zoom"
            + " controls.",
        "mechanic-tournament-bracket");
  }
}
